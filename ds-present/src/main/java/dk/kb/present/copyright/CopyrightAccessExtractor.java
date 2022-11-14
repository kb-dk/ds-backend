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

import dk.kb.present.copyright.CopyrightAccessDto.Copyright;
import dk.kb.present.copyright.CopyrightAccessDto.CreatorPerson;
import dk.kb.present.webservice.exception.InvalidArgumentServiceException;
import dk.kb.util.xml.XMLEscapeSanitiser;



public class CopyrightAccessExtractor {

    private static final Logger log = LoggerFactory.getLogger(CopyrightAccessExtractor.class);
    
    
   public static CopyrightAccessDto extractCopyrightFields( String xml ) throws Exception {
        
       CopyrightAccessDto copyrightDto= new CopyrightAccessDto();
       
       Document document = createDocFromXml(xml);
       
       System.out.println(document.getChildNodes().item(0));
       
       NodeList nList = document.getElementsByTagName("mets:rightsMD");
       
       String version=nList.item(0).getAttributes().getNamedItem("ID").getNodeValue();
       if (!"ModsRights1".equals(version)) { //Failed hard if we are not parsing the implemented version
           throw new InvalidArgumentServiceException("'Mets:rightsMD' Version not supported:"+version);          
       }
       

        Element accessElement = (Element) nList.item(0);
       
        copyrightDto.setCopyright(buildCopyright(accessElement));
        copyrightDto.setCreatorPersonList(buildPersons(accessElement));


        return  copyrightDto;
    }
   
   
   /*    
  <cdl:copyright publication.status="unknown" copyright.status="copyrighted"   
  */
   public static Copyright buildCopyright( Element accessElement){       
       Copyright copyright = new CopyrightAccessDto().new Copyright();
       NodeList copyrightList =  accessElement.getElementsByTagName("cdl:copyright");  
       String publication_status = copyrightList.item(0).getAttributes().getNamedItem("publication.status").getNodeValue();
       String copyright_status = copyrightList.item(0).getAttributes().getNamedItem("copyright.status").getNodeValue();
       copyright.setPulicationStatus(publication_status);
       copyright.setCopyRightStatus(copyright_status);
       return copyright;
       
   }
   
   
   
  //Extract creator.person. Not sure it is always there.
   
/*
   <cdl:name>Clemens, Johann Friderich</cdl:name>
   <cdl:year.birth>1748-11-29</cdl:year.birth>
   <cdl:year.death>1831-11-5</cdl:year.death>
*        
*/
     

   public static ArrayList<CreatorPerson> buildPersons( Element accessElement){       
       ArrayList<CreatorPerson> personList = new  ArrayList<CreatorPerson>();
              
       NodeList creatorPerson =  accessElement.getElementsByTagName("cdl:creator.person");
       if (creatorPerson != null) {
           
           for (int i =0;i<creatorPerson.getLength();i++) {
           
           Element creatorElement = (Element) creatorPerson.item(i);
           
           String personName= creatorElement.getElementsByTagName("cdl:name").item(0).getTextContent();
           String personYearBirth= creatorElement.getElementsByTagName("cdl:year.birth").item(0).getTextContent();
           String personYearDeath= creatorElement.getElementsByTagName("cdl:year.death").item(0).getTextContent();
           
           System.out.println(personYearBirth);
           System.out.println(personName);
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
