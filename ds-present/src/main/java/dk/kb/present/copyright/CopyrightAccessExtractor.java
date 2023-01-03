package dk.kb.present.copyright;

import java.io.StringReader;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import dk.kb.present.copyright.CopyrightAccessDto.AccessCondition;

import dk.kb.present.copyright.CopyrightAccessDto.CreatorCorporate;
import dk.kb.present.copyright.CopyrightAccessDto.CreatorPerson;
import dk.kb.present.webservice.exception.InvalidArgumentServiceException;
import dk.kb.util.xml.XMLEscapeSanitiser;



public class CopyrightAccessExtractor {

    private static final Logger log = LoggerFactory.getLogger(CopyrightAccessExtractor.class);


    public static CopyrightAccessDto extractCopyrightFields(String xml) throws Exception {

        CopyrightAccessDto copyrightDto= new CopyrightAccessDto();

        Document document = createDocFromXml(xml);
        
        copyrightDto.setMaterialeType(getMaterialType(document));
        copyrightDto.setCreatedYear(getCreatedYear(document));

        

        
        
        //TEMORARY SOLUTION TO SET IMAGE LINK!
        copyrightDto.setImageUrl(getImageLink(document));
        
        NodeList nList = document.getElementsByTagName("mets:rightsMD");
        if (nList.getLength()==0) {
            log.info("No rightsMD found");
            copyrightDto.setAccessConditionsList(new ArrayList<AccessCondition>()); //Set empty list
            return copyrightDto;
        }
        
        String version=nList.item(0).getAttributes().getNamedItem("ID").getNodeValue();
        if (!"ModsRights1".equals(version)) { //Failed hard if we are not parsing the implemented version
            throw new InvalidArgumentServiceException("'Mets:rightsMD' Version not supported:"+version);          
        }

        Element rightsMD = (Element) nList.item(0);

        NodeList accessConditions = rightsMD.getElementsByTagName("mods:accessCondition");        
        ArrayList<AccessCondition> accessConditionList = buildAccessCondition(accessConditions);
        copyrightDto.setAccessConditionsList(accessConditionList);        
        
        Integer lastDeathYearForPerson = getLastDeathYearForPerson(accessConditionList);
        if (lastDeathYearForPerson !=  null) {
          copyrightDto.setCreatorPersonDeathYear(lastDeathYearForPerson);
        }
        
        return  copyrightDto;
    }


    /*
   <mods:accessCondition type="use and reproduction" displayLabel="Restricted ">Ejermærke</mods:accessCondition>    
     <cdl:copyright publication.status="unknown" copyright.status="copyrighted" xsi:schemaLocation="http://www.cdlib.org/inside/diglib/copyrightMD /usr/local/ginnungagap/current/script/xsd/copyright-md.xsd">
       <cdl:creator>
         <cdl:creator.person>
           <cdl:name>Clemens, Johann Friderich</cdl:name>
           <cdl:year.birth>1748-11-29</cdl:year.birth>
           <cdl:year.death>1831-11-5</cdl:year.death>
         </cdl:creator.person>
       </cdl:creator>
     </cdl:copyright>   
     */
    public static ArrayList<AccessCondition> buildAccessCondition(  NodeList accessCondititions){       
        ArrayList<AccessCondition> accessConditionList = new  ArrayList<AccessCondition>();
        for (int i =0;i<accessCondititions.getLength();i++) {

            AccessCondition accessCondition = new CopyrightAccessDto().new AccessCondition();
            Element accessConditionElement = (Element) accessCondititions.item(i);
           
            Node typeNode = accessConditionElement.getAttributes().getNamedItem("type");
            if (typeNode != null) {
                accessCondition.setType(typeNode.getNodeValue());     
            }
            
            Node displayNode = accessConditionElement.getAttributes().getNamedItem("displayLabel");
            if (displayNode != null) {
                accessCondition.setDisplayLabel(displayNode.getNodeValue());                
            }

            NodeList copyrightList = accessConditionElement.getElementsByTagName("cdl:copyright");  
           
            
            if (copyrightList.getLength()==0) { //Text!               
                String accessContentText= accessConditionElement.getTextContent();
                if (accessContentText != null) {
                    accessContentText=accessContentText.trim();
                }
                accessCondition.setValue(accessContentText);                
            }            
            if (copyrightList.getLength() ==1) { //XML      
                String publication_status = copyrightList.item(0).getAttributes().getNamedItem("publication.status").getNodeValue();
                String copyright_status = copyrightList.item(0).getAttributes().getNamedItem("copyright.status").getNodeValue();
                accessCondition.setCopyrightPublicationStatus(publication_status);
                accessCondition.setCopyrightStatus(copyright_status);
            }
            else if (copyrightList.getLength() >1) { //TODO deleted later. Just sanity check for new
                throw new InvalidArgumentServiceException("UPS... Need to handle multiple elements for 'cdl:copyright'");
            }


            //Can be empty list
            ArrayList<CreatorPerson> creatorPersons = buildPersons(accessConditionElement);
            accessCondition.setCreatorPersonList(creatorPersons);

           //Can be empty list
            ArrayList<CreatorCorporate> creatorCorporate = buildCooperate(accessConditionElement);
            
            accessCondition.setCreatorCorporateList(creatorCorporate);
            
            accessConditionList.add(accessCondition);                     
        }

        return accessConditionList ;

    }


    /*
   <cdl:creator.person>
     <cdl:name>Clemens, Johann Friderich</cdl:name>
     <cdl:year.birth>1748-11-29</cdl:year.birth>
     <cdl:year.death>1831-11-5</cdl:year.death>
   </cdl:creator.person>        
     */     
    public static ArrayList<CreatorPerson> buildPersons( Element accessConditionElement){       
        ArrayList<CreatorPerson> personList = new  ArrayList<CreatorPerson>();

        NodeList creatorPerson = accessConditionElement.getElementsByTagName("cdl:creator.person");
        if (creatorPerson != null) {

            for (int i =0;i<creatorPerson.getLength();i++) {
                CreatorPerson person=new CopyrightAccessDto().new CreatorPerson();
                
                Element creatorElement = (Element) creatorPerson.item(i);
                
                if (creatorElement.getElementsByTagName("cdl:name").getLength() >0) {
                  String personName= creatorElement.getElementsByTagName("cdl:name").item(0).getTextContent();                    
                  person.setName(personName);                    
                }

                
                if (creatorElement.getElementsByTagName("cdl:year.birth").getLength() >0) {
                    String personYearBirth= creatorElement.getElementsByTagName("cdl:year.birth").item(0).getTextContent();
                    person.setYearBirth(personYearBirth);                    
                    
                }

                
                if (creatorElement.getElementsByTagName("cdl:year.death").getLength() >0) {
                    String personYearDeath= creatorElement.getElementsByTagName("cdl:year.death").item(0).getTextContent();                    
                    person.setYearDeath(personYearDeath);
                }

                personList.add(person);
            }
        }

        return personList;
    }
    
  /*
  <cdl:creator>
    <dk:creator.corporate>
      <cdl:name>Em. Bærentzen &amp; Co. lith. Inst.</cdl:name>
      <dk:year.started>1837</dk:year.started>
      <dk:year.ended>1874</dk:year.ended>
    </dk:creator.corporate>
  </cdl:creator>
  */     
     public static ArrayList<CreatorCorporate> buildCooperate( Element accessConditionElement){       
         ArrayList<CreatorCorporate> cooperateList = new  ArrayList<CreatorCorporate>();

         NodeList creatorCorporate = accessConditionElement.getElementsByTagName("dk:creator.corporate");
         
         if (creatorCorporate != null) {

             for (int i =0;i<creatorCorporate.getLength();i++) {
                 CreatorCorporate coorporate=new CopyrightAccessDto().new CreatorCorporate();
                 Element creatorElement = (Element) creatorCorporate.item(i);

                 String name= creatorElement.getElementsByTagName("cdl:name").item(0).getTextContent();
                 coorporate.setName(name);
                 
                 NodeList yearStartedNode = creatorElement.getElementsByTagName("dk:year.started");
                 NodeList yearEndedNode = creatorElement.getElementsByTagName("dk:year.ended");

                 if (yearStartedNode.getLength() >0  ){
                     String yearStarted= yearStartedNode.item(0).getTextContent();                         
                     coorporate.setYearStarted(yearStarted);
                 }
                 if (yearEndedNode.getLength() > 0){
                     String yearEnded= yearEndedNode.item(0).getTextContent();                         
                     coorporate.setYearEnded(yearEnded);
                 }                                  
                 
                 cooperateList.add(coorporate);
             }
         }
         return cooperateList;
     }


   
    public static  Document createDocFromXml(String xml) throws Exception{

        //System.out.println(response);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        XMLEscapeSanitiser sanitiser = new XMLEscapeSanitiser(""); //Do not replace with anything
        String responseSanitized  =  sanitiser.apply(xml);

        Document document = null;
        try {
            document = builder.parse(new InputSource(new StringReader(responseSanitized)));
            document.getDocumentElement().normalize();
        }
        catch(Exception e) {                       
            log.error("Invalid XML",e);                                                
            throw new Exception("invalid xml",e);            

        }

        return document;
    }
    
    
    private static String getImageLink(Document doc) {

        NodeList identifiers = doc.getElementsByTagName("mods:identifier");
        for (int i =0;i<identifiers.getLength();i++) {
          
            Element e = (Element) identifiers.item(i);        
            String type= e.getAttribute("type");
             
            if ("Asset Reference".equals(type)) {
            String ref = e.getTextContent();

                ref = ref.replaceAll("cumulus-core-01:/Depot", "");
                ref = ref.replaceAll(".tif", "");
                                                
                
                //http://kb-images.kb.dk/?FIF=/DAMJP2/DAM/Samlingsbilleder/0000/388/116/DT005031
                //Add last parameters such as size and format: 
                //http://kb-images.kb.dk/?FIF=/DAMJP2/DAM/Samlingsbilleder/0000/388/116/DT005031&CVT=jpeg
                String link = "http://kb-images.kb.dk/?FIF=/DAMJP2"+ref;            
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

    
    

    // See documentation. Can be 3 different fields, one MUST always be there.
    //<mods:dateCreated>1868</mods:dateCreated>  (notice no point attibute)
    //<mods:dateCreated point="end">1900</mods:dateCreated> 
    //<mods:dateCaptured>2014-04-04T11:52:16.000+02:00</mods:dateCaptured> 
        
    private static int getCreatedYear(Document doc) {

        NodeList dateCreated= doc.getElementsByTagName("mods:dateCreated");
                        
        for (int i =0;i<dateCreated.getLength();i++) {
          
            Element e = (Element) dateCreated.item(i);        
            String point= e.getAttribute("point");
            if (point == null || "".equals(point)){
                String unknownDateFormat=e.getTextContent();
                ///format is YYYY or YYYY-YYYY or '1977.1.14'
                if (unknownDateFormat.indexOf(".")>1) {
                  return Integer.parseInt(unknownDateFormat.substring(0,4));
                }                
                return Integer.parseInt(unknownDateFormat.substring(unknownDateFormat.length()-4));
            }
            else if ("end".equals(point)) {
                return Integer.parseInt(e.getTextContent());
            }                                        
        }


        NodeList dateCaptured = doc.getElementsByTagName("mods:dateCaptured");                                
        
       Element e = (Element)  dateCaptured .item(0);               
       return Integer.parseInt(e.getTextContent().substring(0,4));                                        
        
     }


    

    /* Find person with most recent death year. Return null if a person has no death year.   
     *  Notice last name logic is no longer in use
     */       
    private static Integer getLastDeathYearForPerson(ArrayList<AccessCondition> accessConditionList) {

        Integer highestYear = null;
        for (AccessCondition ac: accessConditionList) {

            for (CreatorPerson p : ac.getCreatorPersonList()) {
                    Integer year= extractYear(p.getYearDeath());
                    //System.out.println("parsed year:"+year);
                    if (year == null) {
                        return null;//one person not dead yet. TODO! Check if this is correct logic
                    }

                    if  (highestYear==null || year > highestYear) {
                        highestYear=year;
                    }
                }


        }                
        return highestYear;
    }


    /*
     * Always assume first 4 letters are year.
     *       
     * Format can me 
     * YYYY or YYYY-M-DD or  YYYY-MM-DD or...
     * 
     */
    private static  Integer extractYear(String yearString) {
        if (yearString==null || yearString.length() <4) {            
            return null;
        }

        try {
            int year = Integer.parseInt(yearString.substring(0,4));
            return year;
        }
        catch(Exception e) {
            log.warn("Could not parse year from:"+yearString);
            return 9999;
        }                        
    }

    

    private static String getMaterialType(Document doc) {
        NodeList types = doc.getElementsByTagName("mods:typeOfResource");
        for (int i =0;i<types.getLength();i++) {
          
            Element e = (Element) types.item(i);        
            String type= e.getAttribute("displayLabel");    
            if ("Resource Description".equals(type)) { 
            String ref = e.getTextContent();
                System.out.println(ref);
              return ref;  
                
           }
        }
        log.warn("No material type found");
        return null;
         
     }

    
    
    

}
