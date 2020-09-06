package org.ascidoctor.spellchecker;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.extension.JavaExtensionRegistry;
import org.asciidoctor.extension.spi.ExtensionRegistry;

public class SpellCheckerExtension implements ExtensionRegistry {

    @Override
    public void register(Asciidoctor asciidoctor) {
        //JavaExtensionRegistry extensionRegistry = asciidoctor.javaExtensionRegistry();
        //extensionRegistry.treeprocessor(SpellCheckingTreeprocessor.class);
    }
    
}
