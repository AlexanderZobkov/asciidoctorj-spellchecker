require 'asciidoctor/extensions' unless RUBY_ENGINE == 'opal'
require 'java'

java_import org.languagetool.JLanguageTool
java_import org.languagetool.language.AmericanEnglish
java_import org.languagetool.rules.Rule
java_import org.languagetool.rules.RuleMatch
java_import org.languagetool.rules.spelling.SpellingCheckRule

include ::Asciidoctor

# TODO: separate extension registration and the extension itself?
Extensions.register do
  preprocessor SourceMapEnablingPreprocessor.new
  treeprocessor SpellCheckingTreeprocessor.new
end

# XXX: A workaround to enable access to line numbers and file names while the maven plugin does not have the corresponding option.
class SourceMapEnablingPreprocessor < Extensions::Preprocessor

  def process document, reader
    document.instance_variable_set :@sourcemap, true
    nil
  end

end

class SpellCheckingTreeprocessor < Extensions::Treeprocessor

  class NullSourceLocation

    @@reply="Not available"

    def file
      return @@reply
    end

    def lineno
      return @@reply
    end

  end


  def initialize config = {}
    super config

    @potential_spelling_mistakes = []

    # TODO: Make configurable via document attributes
    @potential_spelling_mistakes_limit = 10;

    @langTool = JLanguageTool.new(AmericanEnglish.new)
    @langTool.getAllActiveRules.each do |rule|
      @langTool.disableRule(rule.getId) if !rule.isDictionaryBasedSpellingRule
    end
  end

  def process document
    return unless document.blocks?

    potential_spelling_mistakes_report_file="#{document.attributes.fetch('docname','unnamed')}_spelling_mistakes_report.txt"

    check_spelling document.doctitle, document.source_location

    process_blocks document

    if !@potential_spelling_mistakes.empty?

      report = File.open(potential_spelling_mistakes_report_file,'w')

      @potential_spelling_mistakes.each do |mistake|

        text = mistake[:text]
        match = mistake[:match]
        source_location = mistake[:source_location]

        potential_spelling_mistake_error_msg = [
            "#{source_location.file}:#{source_location.lineno}: #{match.getMessage()}",
            "Details:",
            "---> #{text[match.getFromPos()..-1]}",
            "Suggested correction(s): #{match.getSuggestedReplacements()}"
        ].join "\n"

        puts potential_spelling_mistake_error_msg
        report.write("#{potential_spelling_mistake_error_msg}\n")
      end

      raise 'Potential spelling mistakes are found!'
    end

    nil
  end

  def process_blocks node

    return if @potential_spelling_mistakes.size > @potential_spelling_mistakes_limit

    node.blocks.each do |block|
      puts block

      case block
        when Block
          post_processed_lines = block.apply_subs (block.lines.join(EOL))
          check_spelling(post_processed_lines, node.source_location)
        when List
          process_blocks block if block.blocks?
        when ListItem
          check_spelling block.text, node.source_location
        when Section
          check_spelling block.title, node.source_location
        when Table
          block.rows.body.each { |row|
            row.each { |cell| check_spelling cell.text, node.source_location }
          }
        else
          puts "Unsupported node #{block}"
      end

      process_blocks block if block.respond_to?(:blocks) && block.blocks?

    end

  end

  def check_spelling text, source_location
    return if text.nil?

    source_location =  source_location == nil ? NullSourceLocation.new : source_location

    puts "Checking: #{text} #{source_location}"

    matches = @langTool.check(text)
    matches.each do |match|
      mistake = {text: text,
                 match: match,
                 source_location: source_location}
      @potential_spelling_mistakes.push mistake
    end

  end

end
