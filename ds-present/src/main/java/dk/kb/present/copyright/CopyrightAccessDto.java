package dk.kb.present.copyright;

import java.util.ArrayList;

/**
 * This class is a Dto version of the XML extracted from the 'mets:rightsMD' tag.
 *  
 * @author teg@kb.dk 
 *
 */


public class CopyrightAccessDto {
    private Copyright copyright;
    private ArrayList<CreatorPerson> creatorPersonList;
            
    public  CopyrightAccessDto() {                    
    }




   // String publication_status = copyrightList.item(0).getAttributes().getNamedItem("publication.status").getNodeValue();
    //String copyright_status = copyrightList.item(0).getAttributes().getNamedItem("copyright.status").getNodeValue();

    public Copyright getCopyright() {
        return copyright;
    }



    public void setCopyright(Copyright copyright) {
        this.copyright = copyright;
    }



   




    public ArrayList<CreatorPerson> getCreatorPersonList() {
        return creatorPersonList;
    }




    public void setCreatorPersonList(ArrayList<CreatorPerson> creatorPersonList) {
        this.creatorPersonList = creatorPersonList;
    }








    public class Copyright{
        
        
        String pulicationStatus;
        String copyRightStatus;
        public String getPulicationStatus() {
            return pulicationStatus;
        }
        public void setPulicationStatus(String pulicationStatus) {
            this.pulicationStatus = pulicationStatus;
        }
        public String getCopyRightStatus() {
            return copyRightStatus;
        }
        public void setCopyRightStatus(String copyRightStatus) {
            this.copyRightStatus = copyRightStatus;
        }
        
    }



    public class CreatorPerson {     
        String name;
        String yearBirth;
        String yearDeath;
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public String getYearBirth() {
            return yearBirth;
        }
        public void setYearBirth(String yearBirth) {
            this.yearBirth = yearBirth;
        }
        public String getYearDeath() {
            return yearDeath;
        }
        public void setYearDeath(String yearDeath) {
            this.yearDeath = yearDeath;
        }



    }

}
