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
        copyrightDto.setAccessConditionsList(buildAccessCondition(accessConditions));
        
        //TEMORARY SOLUTION TO SET IMAGE LINK!
        copyrightDto.setImageUrl(getImageLink(document));
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


   
    public static   Document createDocFromXml(String xml) throws Exception{

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




}
