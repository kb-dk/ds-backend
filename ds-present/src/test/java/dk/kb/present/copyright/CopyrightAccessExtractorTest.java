package dk.kb.present.copyright;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import dk.kb.util.Resolver;

public class CopyrightAccessExtractorTest {

    

    @Test
    void test() throws Exception {
        String mods = Resolver.resolveUTF8String("xml/copyright_extraction/096c9090-717f-11e0-82d7-002185371280.xml");
    CopyrightAccessExtractor.extractCopyrightFields(mods);
        
        //System.out.println(mods);
        
    }
    
    
}

