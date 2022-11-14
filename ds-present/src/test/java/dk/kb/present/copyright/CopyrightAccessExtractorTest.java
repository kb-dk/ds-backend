package dk.kb.present.copyright;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import dk.kb.present.copyright.CopyrightAccessDto.CreatorPerson;
import dk.kb.util.Resolver;

public class CopyrightAccessExtractorTest {

    

    @Test
    void testSinglePerson() throws Exception {
        String mods = Resolver.resolveUTF8String("xml/copyright_extraction/096c9090-717f-11e0-82d7-002185371280.xml");
        
        //Copyright statuses
        CopyrightAccessDto copyright = CopyrightAccessExtractor.extractCopyrightFields(mods);
        assertEquals("copyrighted",copyright.getCopyright().getCopyRightStatus());        
        assertEquals("unknown",copyright.getCopyright().getPulicationStatus());
        
        //Persons
        assertEquals(1,copyright.getCreatorPersonList().size());
        CreatorPerson person= copyright.getCreatorPersonList().get(0);
        assertEquals("Clemens, Johann Friderich",person.getName());
        assertEquals("1748-11-29",person.getYearBirth());
        assertEquals("1831-11-5",person.getYearDeath());
    }
    
    
}

