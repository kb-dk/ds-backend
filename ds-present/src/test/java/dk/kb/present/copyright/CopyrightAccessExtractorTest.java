package dk.kb.present.copyright;


import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertEquals("Grafik",copyright.getMaterialeType()); 

        assertEquals(2,copyright.getAccessConditionsList().size());
        assertEquals(1831,copyright.getSkabelsesAar());



        AccessCondition accessCondition1 = copyright.getAccessConditionsList().get(0);
        AccessCondition accessCondition2 = copyright.getAccessConditionsList().get(1);                

        //TODO test ejermærke

        assertEquals("copyrighted",accessCondition2.getCopyrightStatus());        
        assertEquals("unknown",accessCondition2.getCopyrightPublicationStatus());

        //Persons
        assertEquals(1,accessCondition2.getCreatorPersonList().size());
        CreatorPerson person= accessCondition2.getCreatorPersonList().get(0);
        assertEquals("Clemens, Johann Friderich",person.getName());
        assertEquals("1748-11-29",person.getYearBirth());
        assertEquals("1831-11-5",person.getYearDeath());

        //Corporate
        assertEquals(0,accessCondition2.getCreatorCorporateList().size());


        //Test field mapping       
        CopyrightAccessDto2SolrFieldsMapper mapper = new  CopyrightAccessDto2SolrFieldsMapper(copyright);

        //has familiy name and year
        assertEquals(1831, mapper.getLastDeathYearForPerson());        
        assertEquals(true, mapper.isEjerMaerke());
        assertEquals(1831, mapper.getSkabelsesAar());

    }


    @Test
    void testDateCaptured() throws Exception {

        // Rare situation where dateCaptured field is used. This is only used if there is no createdDate field         
        String mods = Resolver.resolveUTF8String("xml/copyright_extraction/DPK000107.tif.xml");
        CopyrightAccessDto copyright = CopyrightAccessExtractor.extractCopyrightFields(mods);
        assertEquals("Postkort",copyright.getMaterialeType());  
        assertEquals(2016,copyright.getSkabelsesAar());
        assertEquals(2,copyright.getAccessConditionsList().size());

        AccessCondition accessCondition1 = copyright.getAccessConditionsList().get(0);
        assertEquals(CopyrightAccessDto.USE_AND_REPRODUCTION_EJERMAERKE,accessCondition1.getValue());


        //Test field mapping       
        CopyrightAccessDto2SolrFieldsMapper mapper = new  CopyrightAccessDto2SolrFieldsMapper(copyright);

        //TODO test this for all unittest methods
        assertEquals(null, mapper.getLastDeathYearForPerson());        
        assertEquals(true, mapper.isEjerMaerke());
        assertEquals(2016, mapper.getSkabelsesAar());  


    }

    @Test
    void testNoDeathYearForPerson() throws Exception {
        String mods = Resolver.resolveUTF8String("xml/copyright_extraction/524438.tif.xml");

        //Copyright statuses
        CopyrightAccessDto copyright = CopyrightAccessExtractor.extractCopyrightFields(mods);
        assertEquals("Fotografi",copyright.getMaterialeType()); 

        assertEquals(1,copyright.getAccessConditionsList().size());
        assertEquals(1964,copyright.getSkabelsesAar());

        AccessCondition accessCondition1 = copyright.getAccessConditionsList().get(0);


        /* Ejermærke was removed from this post.
        assertEquals(CopyrightAccessDto.SPECIAL_RESTRICTION_EJERMAERKE,accessCondition1.getValue());
        assertEquals(CopyrightAccessDto.TYPE_USE_AND_REPRODUCTION,accessCondition1.getType());
        assertEquals(CopyrightAccessDto.DISPLAY_LABEL_RESTRICTED,accessCondition1.getDisplayLabel()); 

         */  

        //Test field mapping       
        CopyrightAccessDto2SolrFieldsMapper mapper = new  CopyrightAccessDto2SolrFieldsMapper(copyright);
        assertEquals(null, mapper.getLastDeathYearForPerson());//The record is a person, not art by a person.
        assertEquals(1964, mapper.getSkabelsesAar()); 





    }

    @Test
    void testBlokeret() throws Exception {
        String mods = Resolver.resolveUTF8String("xml/copyright_extraction/006940.tif.xml");


        String access_note="Kurators beslutning. Se journal nr. 897697";

        //Copyright statuses
        CopyrightAccessDto copyright = CopyrightAccessExtractor.extractCopyrightFields(mods);
        assertEquals(4,copyright.getAccessConditionsList().size());

        assertEquals(1865,copyright.getSkabelsesAar());
        assertEquals("Fotografi",copyright.getMaterialeType());

        AccessCondition accessCondition1 = copyright.getAccessConditionsList().get(0);
        AccessCondition accessCondition2 = copyright.getAccessConditionsList().get(1);
        AccessCondition accessCondition3 = copyright.getAccessConditionsList().get(2); //TODO test ejermærke
        AccessCondition accessCondition4 = copyright.getAccessConditionsList().get(3);

        assertEquals(CopyrightAccessDto.SPECIAL_RESTRICTION_BLOKERET,accessCondition1.getValue());
        assertEquals(CopyrightAccessDto.TYPE_RESTRICTION_ON_ACCESS,accessCondition1.getType());
        assertEquals(CopyrightAccessDto.DISPLAY_LABEL_ACCESS_STATUS,accessCondition1.getDisplayLabel()); 

        assertEquals(access_note,accessCondition2.getValue());
        assertEquals(CopyrightAccessDto.TYPE_RESTRICTION_ON_ACCESS_NOTE,accessCondition2.getType());
        assertEquals(null,accessCondition2.getDisplayLabel()); 

        assertEquals(1,accessCondition4.getCreatorPersonList().size());
        assertEquals(1,accessCondition4.getCreatorCorporateList().size());

        CreatorCorporate creatorCorporate = accessCondition4.getCreatorCorporateList().get(0);

        assertEquals("Georg E. Hansen & Co.",creatorCorporate.getName());
        assertEquals(null,creatorCorporate.getYearStarted());
        assertEquals(null,creatorCorporate.getYearEnded());

        //Test field mapping

        CopyrightAccessDto2SolrFieldsMapper mapper = new  CopyrightAccessDto2SolrFieldsMapper(copyright);
        assertEquals(true,mapper.isBlokeret());
        assertEquals(access_note,mapper.getAccessNote());
        assertEquals(1865, mapper.getSkabelsesAar());
        assertEquals(1891, mapper.getLastDeathYearForPerson()); //Person not defined in accessconditin. Looks like a data error



    }

    @Test
    void testVisningKunPaaStedet() throws Exception {
        String mods = Resolver.resolveUTF8String("xml/copyright_extraction/DT005031.tif.xml");

        //Copyright statuses
        CopyrightAccessDto copyright = CopyrightAccessExtractor.extractCopyrightFields(mods);
        assertEquals("Tegning",copyright.getMaterialeType()); 
        assertEquals(3,copyright.getAccessConditionsList().size());
        assertEquals(1987,copyright.getSkabelsesAar());

        AccessCondition accessCondition1 = copyright.getAccessConditionsList().get(0);
        AccessCondition accessCondition2 = copyright.getAccessConditionsList().get(1);

        assertEquals(CopyrightAccessDto.SPECIAL_RESTRICTION_VISNING_KUN_PAA_STEDET,accessCondition1.getValue());
        assertEquals(CopyrightAccessDto.TYPE_RESTRICTION_ON_ACCESS,accessCondition1.getType());
        assertEquals(CopyrightAccessDto.DISPLAY_LABEL_ACCESS_STATUS,accessCondition1.getDisplayLabel()); 

        assertEquals("Kurators beslutning",accessCondition2.getValue());
        assertEquals("use and reproduction note",accessCondition2.getType());
        assertEquals(CopyrightAccessDto.DISPLAY_LABEL_RESTRICTED.trim(),accessCondition2.getDisplayLabel()); //no white space error here  TODO                


        //TEMPORARY TEST, FIELD WILL BE REMOVED
        assertEquals("http://kb-images.kb.dk/?FIF=/DAMJP2/DAM/Samlingsbilleder/0000/388/116/DT005031",copyright.getImageUrl());

        //Test field mapping       
        CopyrightAccessDto2SolrFieldsMapper mapper = new  CopyrightAccessDto2SolrFieldsMapper(copyright);

        assertEquals(1987, mapper.getSkabelsesAar());               
        assertEquals(1998, mapper.getLastDeathYearForPerson()); 

        //TEMPORARY TEST, FIELD WILL BE REMOVED
        assertEquals("http://kb-images.kb.dk/?FIF=/DAMJP2/DAM/Samlingsbilleder/0000/388/116/DT005031",mapper.getImageUrl());

    }


    @Test
    void testEjermaerke() throws Exception {
        String mods = Resolver.resolveUTF8String("xml/copyright_extraction/db_hans_lollesgaard_00039.tif.xml");

        //Copyright statuses
        CopyrightAccessDto copyright = CopyrightAccessExtractor.extractCopyrightFields(mods);
        assertEquals("Bladtegning",copyright.getMaterialeType()); 
        assertEquals(2,copyright.getAccessConditionsList().size());
        assertEquals(1977,copyright.getSkabelsesAar());


        AccessCondition accessCondition1 = copyright.getAccessConditionsList().get(0);

        assertEquals(CopyrightAccessDto.USE_AND_REPRODUCTION_EJERMAERKE,accessCondition1.getValue());
        assertEquals(CopyrightAccessDto.TYPE_USE_AND_REPRODUCTION,accessCondition1.getType());
        assertEquals(CopyrightAccessDto.DISPLAY_LABEL_RESTRICTED,accessCondition1.getDisplayLabel()); 




        //Test field mapping       
        CopyrightAccessDto2SolrFieldsMapper mapper = new  CopyrightAccessDto2SolrFieldsMapper(copyright);

        assertEquals(1993, mapper.getLastDeathYearForPerson());
        assertEquals(true,mapper.isEjerMaerke());        
        assertEquals(1977, mapper.getSkabelsesAar());
    }





    @Test
    void testVisningKunAfMetaDataOgPersonIkkeDoed() throws Exception {
        String mods = Resolver.resolveUTF8String("xml/copyright_extraction/DT013769.tif.xml");

        //Copyright statuses
        CopyrightAccessDto copyright = CopyrightAccessExtractor.extractCopyrightFields(mods);
        assertEquals("Tegning",copyright.getMaterialeType()); 
        assertEquals(3,copyright.getAccessConditionsList().size());
        assertEquals(1971,copyright.getSkabelsesAar());


        AccessCondition accessCondition1 = copyright.getAccessConditionsList().get(0);
        AccessCondition accessCondition2 = copyright.getAccessConditionsList().get(1);

        assertEquals(CopyrightAccessDto.TYPE_RESTRICTION_ON_ACCESS,accessCondition1.getType());
        assertEquals(CopyrightAccessDto.DISPLAY_LABEL_ACCESS_STATUS,accessCondition1.getDisplayLabel());
        assertEquals(CopyrightAccessDto.SPECIAL_RESTRICTION_VISNING_KUN_AF_METADATA,accessCondition1.getValue());


        assertEquals("Materialet må kun vises efter aftale",accessCondition2.getValue());
        assertEquals("use and reproduction note",accessCondition2.getType());
        assertEquals(CopyrightAccessDto.DISPLAY_LABEL_RESTRICTED.trim(),accessCondition2.getDisplayLabel()); //no error whitespace error here                  


        //Test field mapping       
        CopyrightAccessDto2SolrFieldsMapper mapper = new  CopyrightAccessDto2SolrFieldsMapper(copyright);

        //Person not dead.
        assertEquals(null, mapper.getLastDeathYearForPerson());
        assertEquals(1971, mapper.getSkabelsesAar());
        assertEquals(CopyrightAccessDto.SPECIAL_RESTRICTION_VISNING_KUN_AF_METADATA, mapper.getSearligevisningsVilkaar());
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
        assertEquals("Dia",copyright.getMaterialeType()); 
        assertEquals(4,copyright.getAccessConditionsList().size());
        assertEquals(1942,copyright.getSkabelsesAar());


        AccessCondition accessCondition1 = copyright.getAccessConditionsList().get(0);                
        AccessCondition accessCondition3 = copyright.getAccessConditionsList().get(2);

        assertEquals(CopyrightAccessDto.TYPE_USE_AND_REPRODUCTION,accessCondition1.getType());
        assertEquals(CopyrightAccessDto.DISPLAY_LABEL_RESTRICTED,accessCondition1.getDisplayLabel()); 
        assertEquals(CopyrightAccessDto.USE_AND_REPRODUCTION_EJERMAERKE,accessCondition1.getValue()); 



        assertEquals(CopyrightAccessDto.TYPE_PLIGTAFLEVERING,accessCondition3.getType());
        assertEquals(CopyrightAccessDto.SPECIAL_RESTRICTION_PLIGTAFLEVERET,accessCondition3.getValue()); 

        CopyrightAccessDto2SolrFieldsMapper mapper = new  CopyrightAccessDto2SolrFieldsMapper(copyright);
        assertEquals(true, mapper.isEjerMaerke());

        assertEquals(2013, mapper.getLastDeathYearForPerson());
        assertEquals(1942, mapper.getSkabelsesAar());


    }



    @Test
    void testThreeAccessConditionsWith1Person() throws Exception {
        String mods = Resolver.resolveUTF8String("xml/copyright_extraction/KHP0001-049.tif.xml");

        CopyrightAccessDto copyright = CopyrightAccessExtractor.extractCopyrightFields(mods);
        assertEquals("Dia",copyright.getMaterialeType()); 
        assertEquals(3,copyright.getAccessConditionsList().size());         
        assertEquals(1942,copyright.getSkabelsesAar());

        AccessCondition accessCondition3 = copyright.getAccessConditionsList().get(2);//last one has the person
        assertEquals(1,accessCondition3.getCreatorPersonList().size());                    

        CopyrightAccessDto2SolrFieldsMapper mapper = new  CopyrightAccessDto2SolrFieldsMapper(copyright);
        assertEquals(2013, mapper.getLastDeathYearForPerson());
        assertEquals(1942, mapper.getSkabelsesAar());

    }

    @Test
    void testAccessConditionwith3Persons1Corporate() throws Exception {
        String mods = Resolver.resolveUTF8String("xml/copyright_extraction/000332.tif.xml");

        CopyrightAccessDto copyright = CopyrightAccessExtractor.extractCopyrightFields(mods);
        assertEquals("Grafik",copyright.getMaterialeType()); 
        assertEquals(1899,copyright.getSkabelsesAar());

        assertEquals(3,copyright.getAccessConditionsList().get(1).getCreatorPersonList().size());                                   
        assertEquals(1,copyright.getAccessConditionsList().get(1).getCreatorCorporateList().size());


        CreatorCorporate creatorCorporate = copyright.getAccessConditionsList().get(1).getCreatorCorporateList().get(0);
        assertEquals("Em. Bærentzen & Co. lith. Inst.",creatorCorporate.getName()); //notice xml encoding : &amp
        assertEquals("1837",creatorCorporate.getYearStarted());
        assertEquals("1874",creatorCorporate.getYearEnded());              



        //Test field mapping       
        CopyrightAccessDto2SolrFieldsMapper mapper = new  CopyrightAccessDto2SolrFieldsMapper(copyright);

        //3 persons, find last death with family name
        assertEquals(1895, mapper.getLastDeathYearForPerson());   
        assertEquals(1899, mapper.getSkabelsesAar());


    }


    @Test
    void testInvalidRecord() throws Exception {
        String mods = Resolver.resolveUTF8String("xml/copyright_extraction/SKF_f_0137.tif.xml");
        CopyrightAccessDto copyright = CopyrightAccessExtractor.extractCopyrightFields(mods);
        assertEquals("Postkort",copyright.getMaterialeType());  
        assertEquals(2016,copyright.getSkabelsesAar());
        assertEquals(2,copyright.getAccessConditionsList().size());

        AccessCondition accessCondition1 = copyright.getAccessConditionsList().get(0);
        assertEquals(CopyrightAccessDto.USE_AND_REPRODUCTION_EJERMAERKE,accessCondition1.getValue());


        //Test field mapping       
        CopyrightAccessDto2SolrFieldsMapper mapper = new  CopyrightAccessDto2SolrFieldsMapper(copyright);

        //TODO test this for all unittest methods
        assertEquals(null, mapper.getLastDeathYearForPerson());        
        assertEquals(true, mapper.isEjerMaerke());
        assertEquals(1831, mapper.getSkabelsesAar());  //DATAFEJL! Den mangler i record


    }
    

}

