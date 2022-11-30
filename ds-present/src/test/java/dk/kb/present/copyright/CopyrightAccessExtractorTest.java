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
        
        assertEquals(CopyrightAccessDto.VALUE_EJERMAERKE,accessCondition1.getValue());
        assertEquals("use and reproduction",accessCondition1.getType());
        assertEquals(CopyrightAccessDto.DISPLAY_LABEL_RESTRICTED,accessCondition1.getDisplayLabel()); 
        
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
        
        assertEquals(CopyrightAccessDto.VALUE_BLOKKERET,accessCondition1.getValue());
        assertEquals(CopyrightAccessDto.TYPE_RESTRICTION_ON_ACCESS,accessCondition1.getType());
        assertEquals(CopyrightAccessDto.DISPLAY_LABEL_ACCESS_STATUS,accessCondition1.getDisplayLabel()); 
        
        assertEquals("Kurators beslutning. Se journal nr. 897697",accessCondition2.getValue());
        assertEquals(CopyrightAccessDto.TYPE_RESTRICTION_ON_ACCESS_NOTE,accessCondition2.getType());
        assertEquals(null,accessCondition2.getDisplayLabel()); 
                
        assertEquals(1,accessCondition3.getCreatorPersonList().size());
        assertEquals(1,accessCondition3.getCreatorCorporateList().size());
    
        CreatorCorporate creatorCorporate = accessCondition3.getCreatorCorporateList().get(0);
        
        assertEquals("Georg E. Hansen & Co.",creatorCorporate.getName());
        assertEquals(null,creatorCorporate.getYearStarted());
        assertEquals(null,creatorCorporate.getYearEnded());
    }
    
    @Test
    void testVisningKunPaaStedet() throws Exception {
        String mods = Resolver.resolveUTF8String("xml/copyright_extraction/DT005031.tif.xml");
        
        //Copyright statuses
        CopyrightAccessDto copyright = CopyrightAccessExtractor.extractCopyrightFields(mods);
        assertEquals(3,copyright.getAccessConditionsList().size());
        
        AccessCondition accessCondition1 = copyright.getAccessConditionsList().get(0);
        AccessCondition accessCondition2 = copyright.getAccessConditionsList().get(1);
                       
        assertEquals("Visning kun på stedet",accessCondition1.getValue());
        assertEquals(CopyrightAccessDto.TYPE_RESTRICTION_ON_ACCESS,accessCondition1.getType());
        assertEquals(CopyrightAccessDto.DISPLAY_LABEL_ACCESS_STATUS,accessCondition1.getDisplayLabel()); 
                
        assertEquals("Kurators beslutning",accessCondition2.getValue());
        assertEquals("use and reproduction note",accessCondition2.getType());
        assertEquals("Restricted",accessCondition2.getDisplayLabel());                  
    }
    
    
    
    /*     
         hvorfor har den først ikke type og displaylabel?
         <mods:accessCondition>Visning kun af metadata</mods:accessCondition>
         <mods:accessCondition type="use and reproduction note" displayLabel="Restricted">Materialet må kun vises efter aftale</mods:accessCondition>
         <mods:accessCondition>
           <cdl:copyright publication.status="unknown" copyright.status="copyrighted" xsi:schemaLocation="http://www.cdlib.org/inside/diglib/copyrightMD /usr/local/ginnungagap/current/script/xsd/copyright-md.xsd"/>
         </mods:accessCondition>
     */   
 
    @Test
    void testVisningKunAfMetaData() throws Exception {
        String mods = Resolver.resolveUTF8String("xml/copyright_extraction/DT013769.tif.xml");
        
        //Copyright statuses
        CopyrightAccessDto copyright = CopyrightAccessExtractor.extractCopyrightFields(mods);
        assertEquals(3,copyright.getAccessConditionsList().size());
        
        AccessCondition accessCondition1 = copyright.getAccessConditionsList().get(0);
        AccessCondition accessCondition2 = copyright.getAccessConditionsList().get(1);
                       
        assertEquals(CopyrightAccessDto.VALUE_VISNING_KUN_AF_METADATA,accessCondition1.getValue());
        assertEquals(null,accessCondition1.getType());
        assertEquals(null,accessCondition1.getDisplayLabel()); 
                
        assertEquals("Materialet må kun vises efter aftale",accessCondition2.getValue());
        assertEquals("use and reproduction note",accessCondition2.getType());
        assertEquals(CopyrightAccessDto.DISPLAY_LABEL_RESTRICTED.trim(),accessCondition2.getDisplayLabel()); //no error whitespace error here                  
    }
        
    
    
    /*
     * <mets:rightsMD CREATED="2022-11-14T07:42:19.915+01:00" ID="ModsRights1">
            <mets:mdWrap MDTYPE="MODS">
                <mets:xmlData>
                    <mods:mods xmlns:dk="/usr/local/ginnungagap/current/script/xsd" xmlns:cdl="http://www.cdlib.org/inside/diglib/copyrightMD" xmlns:md="http://www.loc.gov/mods/v3" version="3.7" xsi:schemaLocation="http://www.loc.gov/mods/v3 http://www.loc.gov/standards/mods/v3/mods-3-7.xsd">
                        <mods:accessCondition type="use and reproduction" displayLabel="Restricted ">Ejermærke</mods:accessCondition>
                        <mods:accessCondition type="use and reproduction note" displayLabel="Restricted">Se journalnr: 205068</mods:accessCondition>
                        <mods:accessCondition type="pligtaflevering">Pligtafleveret</mods:accessCondition>
                        <mods:accessCondition>
                            <cdl:copyright publication.status="unknown" copyright.status="copyrighted" xsi:schemaLocation="http://www.cdlib.org/inside/diglib/copyrightMD /usr/local/ginnungagap/current/script/xsd/copyright-md.xsd">
                                <cdl:creator>
                                    <cdl:creator.person>
                                        <cdl:name>Helmer-Petersen, Keld</cdl:name>
                                        <cdl:year.birth>1920-8-23</cdl:year.birth>
                                        <cdl:year.death>2013-3-6</cdl:year.death>
                                    </cdl:creator.person>
                                </cdl:creator>
                            </cdl:copyright>
                        </mods:accessCondition>
                    </mods:mods>
     * 
     */
    
    
    /*     
     * So this has two different access modifiers
     */
    @Test
    void testPligtAfleveret_Ejermærke_Restricted() throws Exception {
        String mods = Resolver.resolveUTF8String("xml/copyright_extraction/KHP0001-001.tif.xml");
        
        //Copyright statuses
        CopyrightAccessDto copyright = CopyrightAccessExtractor.extractCopyrightFields(mods);
        assertEquals(4,copyright.getAccessConditionsList().size());
        
        AccessCondition accessCondition1 = copyright.getAccessConditionsList().get(0);                
        AccessCondition accessCondition3 = copyright.getAccessConditionsList().get(2);
        
        assertEquals("use and reproduction",accessCondition1.getType());
        assertEquals(CopyrightAccessDto.DISPLAY_LABEL_RESTRICTED,accessCondition1.getDisplayLabel()); 
        assertEquals(CopyrightAccessDto.VALUE_EJERMAERKE,accessCondition1.getValue()); 
                       

        assertEquals("pligtaflevering",accessCondition3.getType());
        assertEquals(CopyrightAccessDto.VALUE_PLIGTAFLEVERET,accessCondition3.getValue()); 
                         
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

