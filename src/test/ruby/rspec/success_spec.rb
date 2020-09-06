require 'asciidoctor'
require 'asciidoctor/extensions'
require 'spell_checking_treeprocessor'

describe "succeeding rspec" do

  it "should succeed" do

    src = <<-EOS
text {attr} _italic_ treee  Company(R)

|===
| Name of column 1 | Name of column 2

| XXX1(C)
| YYY1(R)

| XXX2
| {attr}
|===
EOS

    opts = {:attributes => {"attr" => "value"}}

    doc = Asciidoctor::Document.new src.lines.entries, opts

    puts doc.convert

  end

end
