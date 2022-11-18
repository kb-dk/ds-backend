package dk.kb.present.copyright;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import dk.kb.present.copyright.CopyrightAccessDto.AccessCondition;
import dk.kb.present.copyright.CopyrightAccessDto.CreatorPerson;
import dk.kb.util.Resolver;

public class CopyrightAccessExtractorTest {

    

    @Test
    void testSinglePerson() throws Exception {
        String mods = Resolver.resolveUTF8String("xml/copyright_extraction/096c9090-717f-11e0-82d7-002185371280.xml");
        
        //Copyright statuses
        CopyrightAccessDto copyright = CopyrightAccessExtractor.extractCopyrightFields(mods);
        assertEquals(1,copyright.getAccessConditionsList().size());
        
        AccessCondition accessCondition = copyright.getAccessConditionsList().get(0);
        
        
        
        assertEquals("copyrighted",accessCondition.getCopyrightStatus());        
        assertEquals("unknown",accessCondition.getCopyrightPublicationStatus());
        
        //Persons
        assertEquals(1,accessCondition.getCreatorPersonList().size());
        CreatorPerson person= accessCondition.getCreatorPersonList().get(0);
        assertEquals("Clemens, Johann Friderich",person.getName());
        assertEquals("1748-11-29",person.getYearBirth());
        assertEquals("1831-11-5",person.getYearDeath());       
    }
    
    
    @Test
    void testNoAccessConditions() throws Exception {
        String mods = Resolver.resolveUTF8String("xml/copyright_extraction/DPK000107.tif.xml");
        CopyrightAccessDto copyright = CopyrightAccessExtractor.extractCopyrightFields(mods);
        assertEquals(0,copyright.getAccessConditionsList().size());
    
    }
    
    @Test
    void testTwoAccessConditions() throws Exception {
        String mods = Resolver.resolveUTF8String("xml/copyright_extraction/524438.tif.xml");
        
        //Copyright statuses
        CopyrightAccessDto copyright = CopyrightAccessExtractor.extractCopyrightFields(mods);
        assertEquals(2,copyright.getAccessConditionsList().size());
        
        AccessCondition accessCondition1 = copyright.getAccessConditionsList().get(0);
        AccessCondition accessCondition2 = copyright.getAccessConditionsList().get(1);
        
        assertEquals("Ejermærke",accessCondition1.getValue());
        assertEquals("use and reproduction",accessCondition1.getType());
        assertEquals("Restricted ",accessCondition1.getDisplayLabel()); //white space
        
        System.out.println("'"+accessCondition2.getValue()+"'");
        assertEquals("",accessCondition2.getValue().trim()); // XML can both have text or xml tag... 
        
        assertEquals("unknown",accessCondition2.getCopyrightPublicationStatus());
        assertEquals("copyrighted",accessCondition2.getCopyrightStatus());

        
    
    }
    
    
    @Test
    void testTwoAccessConditionsWith1Person() throws Exception {
        String mods = Resolver.resolveUTF8String("xml/copyright_extraction/524438.tif.xml");
        
        //Copyright statuses
        CopyrightAccessDto copyright = CopyrightAccessExtractor.extractCopyrightFields(mods);
        assertEquals(2,copyright.getAccessConditionsList().size());
        
        AccessCondition accessCondition1 = copyright.getAccessConditionsList().get(0);
        AccessCondition accessCondition2 = copyright.getAccessConditionsList().get(1);
        
        assertEquals("Ejermærke",accessCondition1.getValue());
        assertEquals("use and reproduction",accessCondition1.getType());
        assertEquals("Restricted ",accessCondition1.getDisplayLabel()); //white space
        
        /*  
        assertEquals("copyrighted",accessCondition.getCopyrightStatus());        
        assertEquals("unknown",accessCondition.getCopyrightPublicationStatus());
        
        //Persons
        assertEquals(1,accessCondition.getCreatorPersonList().size());
        CreatorPerson person= accessCondition.getCreatorPersonList().get(0);
        assertEquals("Clemens, Johann Friderich",person.getName());
        assertEquals("1748-11-29",person.getYearBirth());
        assertEquals("1831-11-5",person.getYearDeath());
        */
    
    }
    
}

