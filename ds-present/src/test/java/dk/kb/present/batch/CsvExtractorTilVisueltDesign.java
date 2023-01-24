package dk.kb.present.batch;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.ContextAction;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import dk.kb.present.copyright.CopyrightAccessDto;
import dk.kb.present.copyright.CopyrightAccessExtractor;
import dk.kb.present.copyright.CopyrightAccessDto.AccessCondition;
import dk.kb.present.copyright.CopyrightAccessDto.CreatorCorporate;
import dk.kb.present.copyright.CopyrightAccessDto.CreatorPerson;


/**
 * Designteam har brug for eksemple på data/felter samt værdier til deres wireframes. 
 * Dette job udtrækker de udvalgte felter i en csv-fil.
 * 
 * Den kræver 500+ xml med nyeste records i en mappe, derfor ikke checket ind i repository da de også skal opdateres hver gang.
 * 
 */
public class CsvExtractorTilVisueltDesign {
    
    private static String TAB="\t";
    private static String NEWLINE="\n";
    private static String SEPERATOR=";";
    private static String testDataDir="/home/teg/testdata/";
    
  
    /**
     * Call with argument to custom folder
     * 
     * @param path to folder with record XML files
     */
    public static void main(String[] args) {

        if (args != null && args.length ==1) {
          testDataDir = args[0];            
        }
    
        Set<String> files = getFilesNamesInDir(testDataDir);
        System.out.println(files.size());
        
        StringBuffer csvFileBuffer = new StringBuffer();
        
        csvFileBuffer.append("file;titel;subTitle;creator;dateOfOrigin;dateNotBefore;dateNotAfter;copyright;resourceDescription; materialDescription;description");
        csvFileBuffer.append(NEWLINE);
        for (String file: files) {
            
            
            try {            
             String xml = new String(Files.readAllBytes(Paths.get(testDataDir+file)),"UTF-8");
             CsvExtract csvLine = getCvsExtractromDoc(xml);
            
             csvFileBuffer.append(file);
             csvFileBuffer.append(SEPERATOR);
            csvFileBuffer.append(csvLine.getTitel());
            csvFileBuffer.append(SEPERATOR);
            csvFileBuffer.append(csvLine.getSubTitle());
            csvFileBuffer.append(SEPERATOR);
            csvFileBuffer.append(csvLine.getCreator());
            csvFileBuffer.append(SEPERATOR);
            csvFileBuffer.append(csvLine.getDateOfOrigin());
            csvFileBuffer.append(SEPERATOR);
            csvFileBuffer.append(csvLine.getDateNotBefore());
            csvFileBuffer.append(SEPERATOR);
            csvFileBuffer.append(csvLine.getDateNotAfter());
            csvFileBuffer.append(SEPERATOR);
            csvFileBuffer.append(csvLine.getCopyright());
            csvFileBuffer.append(SEPERATOR);
            csvFileBuffer.append(csvLine.getResourceDescription());
            csvFileBuffer.append(SEPERATOR);
            csvFileBuffer.append(csvLine.getMaterialDescription());
            csvFileBuffer.append(SEPERATOR);
            csvFileBuffer.append(csvLine.getDescription());
            
            csvFileBuffer.append(NEWLINE);
            
            }
            catch(Exception e) {
                e.printStackTrace();
                System.out.println("Failed parsing:"+file);
                
            }
           
            
            
        }
        System.out.println(csvFileBuffer.toString());
        //String mods = Resolver.resolveUTF8String("xml/copyright_extraction/DT005031.tif.xml");
        
        
    }
    
    public static CsvExtract getCvsExtractromDoc(String xml) throws Exception{
        
        CsvExtract extract = new CsvExtractorTilVisueltDesign().new CsvExtract();

       CopyrightAccessDto copyDto = CopyrightAccessExtractor.buildCopyrightFields(xml);
       Document doc= CopyrightAccessExtractor.createDocFromXml(xml);
        /*
        <mods:typeOfResource displayLabel="Generel materialebetegnelse">Billede, Todimensionalt billedmateriale</mods:typeOfResource>
        <mods:typeOfResource displayLabel="Materialebetegnelse">Grafik</mods:typeOfResource>
       */
        NodeList resNodes = doc.getElementsByTagName("mods:typeOfResource");
        Element res1= (Element)  resNodes.item(0);
        Element res2= (Element)  resNodes.item(1);
        if (res1 != null) {
           String mat_type =res1.getTextContent();    

            extract.setMaterialDescription(mat_type);
        }
        if (res2 != null) {
            String  mat_desc=res2.getTextContent();
            extract.setResourceDescription(mat_desc);

        }

     
        NodeList titleNodes = doc.getElementsByTagName("mods:title");

        extractTitles(titleNodes, extract);
        
        NodeList dateNodes = doc.getElementsByTagName("mods:dateCreated");

        if (dateNodes.getLength() >0) {
            Element e = (Element) dateNodes.item(0);
            extract.setDateOfOrigin(e.getTextContent());
            
        }
        
        ArrayList<AccessCondition> accessConditionsList = copyDto.getAccessConditionsList();
       StringBuffer values = new StringBuffer();
        for (AccessCondition ac : accessConditionsList) {
           String value = ac.getValue();
           if (value != null) {
               values.append(value+TAB);
           }
            
            
           
            
        }
        extract.setCopyright(values.toString());
        
        String creators = extractCreators(accessConditionsList);        
        extract.setCreator(creators);
        
        
        
     return extract;
 }
    
    private static String extractCreators( ArrayList<AccessCondition>  acList) {

        StringBuffer creators= new StringBuffer();
        for (AccessCondition ac :acList) {
            
            for (CreatorPerson cp : ac.getCreatorPersonList()) {
                creators.append(cp.getName());                
                creators.append(TAB);
            }
            
            for (CreatorCorporate cp : ac.getCreatorCorporateList()) {
                creators.append(cp.getName());                
                creators.append(TAB);
            }
            

        }
        
        
        return creators.toString();
        
        
        
    }
   
 
 private static void extractTitles(NodeList titleNodes,CsvExtract extract) {

     
     if (titleNodes.getLength() >0) {
         Element titel = (Element) titleNodes.item(0);   
         extract.setTitel(titel.getTextContent());
     }
     

     if (titleNodes.getLength() >1) {
         Element subTitel = (Element) titleNodes.item(1);   
         extract.setSubTitle(subTitel.getTextContent());
     }

     
 }
 
    
    private static Set<String> getFilesNamesInDir(String dir) {
        return Stream.of(new File(dir).listFiles())
          .filter(file -> !file.isDirectory())
          .map(File::getName)
          .collect(Collectors.toSet());
    }

    

    public class CsvExtract {     

        private String titel;
        private String subTitle;
        private String creator;
        private String dateOfOrigin;
        private String dateNotBefore;
        private String dateNotAfter;
        private String copyright;
        private String resourceDescription;
        private String materialDescription;
        private String description;
        public String getTitel() {
            return titel;
        }
        public void setTitel(String titel) {
            this.titel = titel;
        }
        public String getSubTitle() {
            return subTitle;
        }
        public void setSubTitle(String subTitle) {
            this.subTitle = subTitle;
        }
        public String getCreator() {
            return creator;
        }
        public void setCreator(String creator) {
            this.creator = creator;
        }
        public String getDateOfOrigin() {
            return dateOfOrigin;
        }
        public void setDateOfOrigin(String dateOfOrigin) {
            this.dateOfOrigin = dateOfOrigin;
        }
        public String getDateNotBefore() {
            return dateNotBefore;
        }
        public void setDateNotBefore(String dateNotBefore) {
            this.dateNotBefore = dateNotBefore;
        }
        public String getDateNotAfter() {
            return dateNotAfter;
        }
        public void setDateNotAfter(String dateNotAfter) {
            this.dateNotAfter = dateNotAfter;
        }
        public String getCopyright() {
            return copyright;
        }
        public void setCopyright(String copyright) {
            this.copyright = copyright;
        }
        public String getResourceDescription() {
            return resourceDescription;
        }
        public void setResourceDescription(String resourceDescription) {
            this.resourceDescription = resourceDescription;
        }
        public String getMaterialDescription() {
            return materialDescription;
        }
        public void setMaterialDescription(String materialDescription) {
            this.materialDescription = materialDescription;
        }
        public String getDescription() {
            return description;
        }
        public void setDescription(String description) {
            this.description = description;
        }
        @Override
        public String toString() {
            return "CsvExtract [titel=" + titel + ", subTitle=" + subTitle + ", creator=" + creator + ", dateOfOrigin="
                    + dateOfOrigin + ", dateNotBefore=" + dateNotBefore + ", dateNotAfter=" + dateNotAfter
                    + ", copyright=" + copyright + ", resourceDescription=" + resourceDescription
                    + ", materialDescription=" + materialDescription + ", description=" + description + "]";
        }
        

    } 
    
}
