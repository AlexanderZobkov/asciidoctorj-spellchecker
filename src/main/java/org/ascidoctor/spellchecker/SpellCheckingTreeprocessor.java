package org.ascidoctor.spellchecker;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.asciidoctor.ast.Block;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.ListItem;
import org.asciidoctor.ast.Section;
import org.asciidoctor.ast.AbstractBlock;
import org.asciidoctor.ast.ListNode;
import org.asciidoctor.ast.Table;
import org.asciidoctor.extension.Treeprocessor;
import org.languagetool.JLanguageTool;
import org.languagetool.language.AmericanEnglish;
import org.languagetool.rules.Rule;
import org.languagetool.rules.RuleMatch;
import org.languagetool.rules.spelling.SpellingCheckRule;

// XXX: StringConverter could also be used but it's available in 1.6 only
// TODO: How to ingore words including underscores, like operation_context, alarm_object? subdevice? Add to common ignore list?
// TODO: How to ingore words in camelCase, like targetName? Add to common ignore list?
// TODO: Why do I get <xfer linkend... in table? 
// TODO: How to deal with text that include formatting? like, _Italic_ Text: AnnotetedText?
// TODO: Add a list of ignored phrases, for cases like: Downloadable documentation?
public class SpellCheckingTreeprocessor extends Treeprocessor {

    // XXX: Why ascidoctor(j) does not like attributes in camel case?
    // TODO: read words to ignore from a file
    public static final String WORDS_TO_IGNORE_ATTR = "spellchecker-words-to-ignore";

    private final JLanguageTool langTool;

    private Set<String> skipBlockTypes = new HashSet<>();
    
    /**
     * Creates an instance. Constructor is required by API.
     *
     * @param config ???
     */
    public SpellCheckingTreeprocessor(Map<String, Object> config) throws IOException {
        super(config);
        langTool = new JLanguageTool(new AmericanEnglish());
        
        // This tool is so powerfull (supports grammar and writing style checking) 
        // so let's enable only spellcheking so far
        for (Rule rule : langTool.getAllActiveRules()) {
            if (!rule.isDictionaryBasedSpellingRule()) {
                langTool.disableRule(rule.getId());
            }
        }
       
        skipBlockTypes.add("listing");
    }

    @Override
    public Document process(Document document) {

        for (Rule rule : langTool.getAllActiveRules()) {
            if (rule instanceof SpellingCheckRule) {
                List<String> wordsToIgnore = (List<String>) document.getAttr(WORDS_TO_IGNORE_ATTR, Collections.EMPTY_LIST);
                ((SpellingCheckRule) rule).addIgnoreTokens(wordsToIgnore);
            }
        }
        
        String title = document.doctitle();
        checkSpelling(title);

        final List<AbstractBlock> blocks = document.blocks();
        if (blocks != null) {
            visitBlocks(blocks);
        }

        return document;
    }

    // TODO: Consider to decouple traverser from processor 
    private void visitBlocks(List<AbstractBlock> nodes) {
        for (AbstractBlock currentNode : nodes) {
            System.out.println(currentNode.getClass());
            // XXX: Seems can't get line number and source file when using the existing plugin as required option is not passed and seems can't be passed
            //System.out.println(currentNode.getSourceLocation().getLineNumber());

            if (currentNode instanceof Block) {
                Block block = (Block) currentNode;
                
                String blockType = block.getNodeName();
                if (!skipBlockTypes.contains(blockType)) {
                    String text = block.lines().stream().collect(Collectors.joining("\n"));
                    checkSpelling(text);
                }
                
            } else if (currentNode instanceof ListNode) {
                ListNode list = (ListNode) currentNode;
                visitBlocks(list.getItems());
            } else if (currentNode instanceof ListItem) {
               ListItem listItem = (ListItem) currentNode;
               String text = listItem.getText();
               checkSpelling(text);
            } else if (currentNode instanceof Section){
                Section section = (Section) currentNode;
                String text = section.sectname();
                checkSpelling(text);
            } else if (currentNode instanceof Table) {
                Table table = (Table) currentNode;
                table.getBody().stream().forEach((row) -> {
                    row.getCells().stream().map((cell) -> cell.getText()).forEach((text) -> {
                        checkSpelling(text);
                    });
                });
            } else {
                throw new IllegalArgumentException("Unsupported node type:" + currentNode.getClass());
            }           
            
            List moreBlocks = currentNode.getBlocks();
            if (moreBlocks != null && !moreBlocks.isEmpty()) {
                visitBlocks(moreBlocks);
            }
        }
    }

    // TODO; Consider to decouple spellchecker from processor
    private void checkSpelling(String text) {

        try {

            List<RuleMatch> matches = langTool.check(text);
            if (!matches.isEmpty()) {
                matches.forEach(match -> {

                    System.out.println("Potential error at column "
                            + match.getFromPos()+ ": " + match.getMessage());

                    System.out.println("Text with potential error:" + text);
                    System.out.println("Error starts here:"
                            + text.substring(match.getFromPos()));

                    System.out.println("Suggested correction: " + match.getSuggestedReplacements());
                    System.out.println();

                });
                //throw new RuntimeException("Spelling mistakes are found!");
            }

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

    }

}
