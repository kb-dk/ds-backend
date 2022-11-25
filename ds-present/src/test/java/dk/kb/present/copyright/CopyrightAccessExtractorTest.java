package dk.kb.present.copyright;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.CALLS_REAL_METHODS;

import org.junit.jupiter.api.Test;

import dk.kb.present.copyright.CopyrightAccessDto.AccessCondition;
import dk.kb.present.copyright.CopyrightAccessDto.CreatorCorporate;
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
        
        //Corporate
        assertEquals(0,accessCondition.getCreatorCorporateList().size());
        
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
        
        assertEquals(null,accessCondition2.getValue());  
        
        assertEquals("unknown",accessCondition2.getCopyrightPublicationStatus());
        assertEquals("copyrighted",accessCondition2.getCopyrightStatus());

        
    
    }
    
    @Test
    void testBlokkeret() throws Exception {
        String mods = Resolver.resolveUTF8String("xml/copyright_extraction/006940.tif.xml");
        
        //Copyright statuses
        CopyrightAccessDto copyright = CopyrightAccessExtractor.extractCopyrightFields(mods);
        assertEquals(3,copyright.getAccessConditionsList().size());
        
        AccessCondition accessCondition1 = copyright.getAccessConditionsList().get(0);
        AccessCondition accessCondition2 = copyright.getAccessConditionsList().get(1);
        AccessCondition accessCondition3 = copyright.getAccessConditionsList().get(2);
        
        assertEquals("Blokeret",accessCondition1.getValue());
        assertEquals("restriction on access",accessCondition1.getType());
        assertEquals("Access Status",accessCondition1.getDisplayLabel()); 
        
        assertEquals("Kurators beslutning. Se journal nr. 897697",accessCondition2.getValue());
        assertEquals("restriction on access note",accessCondition2.getType());
        assertEquals(null,accessCondition2.getDisplayLabel()); 
                
        assertEquals(1,accessCondition3.getCreatorPersonList().size());
        assertEquals(1,accessCondition3.getCreatorCorporateList().size());
    
        CreatorCorporate creatorCorporate = accessCondition3.getCreatorCorporateList().get(0);
        
        assertEquals("Georg E. Hansen & Co.",creatorCorporate.getName());
        assertEquals(null,creatorCorporate.getYearStarted());
        assertEquals(null,creatorCorporate.getYearEnded());
    }
    
    
    
    @Test
    void testThreeAccessConditionsWith1Person() throws Exception {
        String mods = Resolver.resolveUTF8String("xml/copyright_extraction/KHP0001-049.tif.xml");
        
        CopyrightAccessDto copyright = CopyrightAccessExtractor.extractCopyrightFields(mods);
        assertEquals(3,copyright.getAccessConditionsList().size()); 
        
        AccessCondition accessCondition3 = copyright.getAccessConditionsList().get(2);//last one has the person
        assertEquals(1,accessCondition3.getCreatorPersonList().size());                    
    }
    
    @Test
    void testAccessConditionwith3Persons1Corporate() throws Exception {
        String mods = Resolver.resolveUTF8String("xml/copyright_extraction/000332.tif.xml");
        
        CopyrightAccessDto copyright = CopyrightAccessExtractor.extractCopyrightFields(mods);
        assertEquals(3,copyright.getAccessConditionsList().get(0).getCreatorPersonList().size());                                   
        assertEquals(1,copyright.getAccessConditionsList().get(0).getCreatorCorporateList().size());
        CreatorCorporate creatorCorporate = copyright.getAccessConditionsList().get(0).getCreatorCorporateList().get(0);
        assertEquals("Em. Bærentzen & Co. lith. Inst.",creatorCorporate.getName()); //notice xml encoding : &amp
        assertEquals("1837",creatorCorporate.getYearStarted());
        assertEquals("1874",creatorCorporate.getYearEnded());
      
        
        
    }
    
    
}

