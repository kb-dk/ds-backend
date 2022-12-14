package dk.kb.present.batch;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import dk.kb.present.copyright.CopyrightAccessExtractor;



/*
 * This is a manual started class to extract some data for "visuelt Design"
 * 
 * 
 */
public class BilledeLocationMapper {
    
    private static String NEWLINE="\n";
    private static String testDataDir="/home/teg/testdata/";
    
    //private static String testDataDir="/home/teg/workspace/ds-present/src/test/resources/xml/copyright_extraction/";
    public static void main(String[] args) {
        
        Set<String> files = getFilesNamesInDir(testDataDir);
        System.out.println(files.size());
        
        StringBuffer csvFileBuffer = new StringBuffer();
        
        csvFileBuffer.append(NEWLINE);
        for (String file: files) {
            
            
            try {            
             String xml = new String(Files.readAllBytes(Paths.get(testDataDir+file)),"UTF-8");
             Document doc= CopyrightAccessExtractor.createDocFromXml(xml);
             
             NodeList identifiers = doc.getElementsByTagName("mods:identifier");
             
             String id = getId(identifiers);
             String link = getAssetReferenceLink(identifiers);
       
             if (id != null && link != null) {
             System.out.println(id +" "+link);
             }
             else {
                 //System.out.println("no info for file:"+file);
             }
            
            }
            catch(Exception e) {
                e.printStackTrace();
                System.out.println("Failed parsing:"+file);
                
            }
           
            
            
        }
   //     System.out.println(csvFileBuffer.toString());
        //String mods = Resolver.resolveUTF8String("xml/copyright_extraction/DT005031.tif.xml");
        
        
    }
 
 
 
private static String getAssetReferenceLink(NodeList identifiers) {

    for (int i =0;i<identifiers.getLength();i++) {
      
        Element e = (Element) identifiers.item(i);        
        String type= e.getAttribute("type");
         
        if ("Asset Reference".equals(type)) {
        String ref = e.getTextContent();

            ref = ref.replaceAll("cumulus-core-01:/Depot", "");
            ref = ref.replaceAll(".tif", "");
            if (ref.startsWith("cumulus")) {
                continue;
            }
            
            
            
            //http://kb-images.kb.dk/?FIF=/DAMJP2/DAM/Samlingsbilleder/0000/388/116/DT005031&CVT=jpeg  

            String link = "http://kb-images.kb.dk/?FIF=/DAMJP2"+ref+"&CVT=jpeg";            
            return link;
            
     }
    }
     return null;
 }
 

private static String getId(NodeList identifiers) {

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



    private static Set<String> getFilesNamesInDir(String dir) {
        return Stream.of(new File(dir).listFiles())
          .filter(file -> !file.isDirectory())
          .map(File::getName)
          .collect(Collectors.toSet());
    }

    

    
}
