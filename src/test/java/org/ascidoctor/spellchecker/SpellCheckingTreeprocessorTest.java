package org.ascidoctor.spellchecker;

import java.io.File;
import java.util.Arrays;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.AttributesBuilder;
import org.asciidoctor.Options;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.SafeMode;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class SpellCheckingTreeprocessorTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testSpellChecking() {

        //thrown.expect(RuntimeException.class);
        //thrown.expectMessage(StringContains.containsString("Spelling mistakes are found"));
        
        Asciidoctor asciidoctor = Asciidoctor.Factory.create();
        File file = new File("target/test-classes/asciidoc/example-manual.adoc");

        Options opts = OptionsBuilder.options().
                backend("docbook5").
                option("sourcemap", "true").
                docType("book").
                attributes(AttributesBuilder.attributes().
                        attribute(SpellCheckingTreeprocessor.WORDS_TO_IGNORE_ATTR, Arrays.asList("statusExplanation", "statusExplation"))).
                safe(SafeMode.UNSAFE).get();

        asciidoctor.renderFile(file, opts);
        
        asciidoctor.shutdown();
    }

}
