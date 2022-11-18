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

import dk.kb.present.copyright.CopyrightAccessDto.CreatorPerson;
import dk.kb.present.webservice.exception.InvalidArgumentServiceException;
import dk.kb.util.xml.XMLEscapeSanitiser;



public class CopyrightAccessExtractor {

    private static final Logger log = LoggerFactory.getLogger(CopyrightAccessExtractor.class);


    public static CopyrightAccessDto extractCopyrightFields( String xml ) throws Exception {

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

            accessCondition.setValue(accessConditionElement.getTextContent());
            Node typeNode = accessConditionElement.getAttributes().getNamedItem("type");
            if (typeNode != null) {
                accessCondition.setType(typeNode.getNodeValue());     
            }
            
            Node displayNode = accessConditionElement.getAttributes().getNamedItem("displayLabel");
            if (displayNode != null) {
                accessCondition.setDisplayLabel(displayNode.getNodeValue());                
            }

            NodeList copyrightList = accessConditionElement.getElementsByTagName("cdl:copyright");  
            if (copyrightList.getLength() ==1) {       
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

                String personName= creatorElement.getElementsByTagName("cdl:name").item(0).getTextContent();
                String personYearBirth= creatorElement.getElementsByTagName("cdl:year.birth").item(0).getTextContent();
                String personYearDeath= creatorElement.getElementsByTagName("cdl:year.death").item(0).getTextContent();
                person.setName(personName);
                person.setYearBirth(personYearBirth);           
                person.setYearDeath(personYearDeath);
                personList.add(person);
            }
        }

        return personList;

    }


    private static   Document createDocFromXml(String xml) throws Exception{

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


}
