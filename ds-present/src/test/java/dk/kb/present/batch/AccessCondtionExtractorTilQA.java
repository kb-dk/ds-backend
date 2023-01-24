package dk.kb.present.batch;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import dk.kb.present.copyright.CopyrightAccessDto;
import dk.kb.present.copyright.CopyrightAccessExtractor;
import dk.kb.present.copyright.XsltCopyrightMapper;
import dk.kb.present.copyright.CopyrightAccessDto2SolrFieldsMapper;

/**
 * Manuelt kørt batch job for at udtrække "rettigheders" oversætter data til jura QA
 *  
 * Den kræver enkelt XML record filer i den mappe der læses fra. (579 records og senere 40K records) 
 */
public class AccessCondtionExtractorTilQA {

    private static String testDataDir="/home/teg/testdata/";

    //private static String testDataDir="/home/teg/workspace/ds-present/src/test/resources/xml/copyright_extraction/";
    public static void main(String[] args) {

        Set<String> files = getFilesNamesInDir(testDataDir);
        System.out.println(files.size());

        for (String file: files) {


            try {            
                String xml = new String(Files.readAllBytes(Paths.get(testDataDir+file)),"UTF-8");
                CopyrightAccessDto extractCopyrightFields = CopyrightAccessExtractor.buildCopyrightFields(xml);
                CopyrightAccessDto2SolrFieldsMapper mapper = new CopyrightAccessDto2SolrFieldsMapper(extractCopyrightFields); 

                System.out.println("ID:"+getId(xml));
                System.out.println(XsltCopyrightMapper.ACCESS_MATERIALE_TYPE+":"+mapper.getMaterialeType());
                System.out.println(XsltCopyrightMapper.ACCESS_BLOKERET_FIELD+":"+mapper.isBlokeret());                
                
                if (mapper.getLastDeathYearForPerson()== null) {
                                        
                   System.out.println(XsltCopyrightMapper.ACCESS_OPHAVSPERSON_DOEDSAAR_FIELD+":");
                }
                else {
                    System.out.println(XsltCopyrightMapper.ACCESS_OPHAVSPERSON_DOEDSAAR_FIELD+":"+mapper.getLastDeathYearForPerson());
                }
                
                if (mapper.getSkabelsesAar()== null) {
                    System.out.println(XsltCopyrightMapper.ACCESS_SKABELSESAAR_FIELD+":"+"DATAFEJL");                
                }
                else {
                    System.out.println(XsltCopyrightMapper.ACCESS_SKABELSESAAR_FIELD+":"+mapper.getSkabelsesAar());
                }
                if (mapper.getSearligevisningsVilkaar() != null) {
                    System.out.println(XsltCopyrightMapper.ACCESS_SEARLIGE_VISNINGSVILKAAR_FIELD+":"+ mapper.getSearligevisningsVilkaar());   
                }

                if (mapper.getAccessNote() != null) {
                    System.out.println(XsltCopyrightMapper.ACCESS_NOTE_FIELD+":"+mapper.getAccessNote());    
                }

                System.out.println(XsltCopyrightMapper.ACCESS_PLIGTAFLEVERET_FIELD+":"+mapper.isPligtAfleveret());

                String ophavsretTekst="Beskyttet af ophavsret";
                if (   (mapper.getLastDeathYearForPerson() != null && mapper.getLastDeathYearForPerson() <=1952) ||
                       (mapper.getSkabelsesAar() != null && mapper.getSkabelsesAar() <=1882)){
                        ophavsretTekst="Fri af ophavsret";                      
                }
                                
                System.out.println("Ophavsret:"+ophavsretTekst);

                System.out.println("---------------------------------------");
            }
            catch(Exception e) {
                e.printStackTrace();
                System.out.println("Failed parsing:"+file);

            }



        }        

    }


    private static Set<String> getFilesNamesInDir(String dir) {
        return Stream.of(new File(dir).listFiles())
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .collect(Collectors.toSet());
    }



    private static String getId(String xml) throws Exception {

        Document document =CopyrightAccessExtractor.createDocFromXml(xml);

        NodeList identifiers = document.getElementsByTagName("mods:identifier");

        for (int i =0;i<identifiers.getLength();i++) {

            Element e = (Element) identifiers.item(i);        
            String type= e.getAttribute("type");

            if ("local".equals(type)) {
                String ref = e.getTextContent();
                return ref;  
            }
        }
        return null;
    }
}
