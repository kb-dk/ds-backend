package dk.kb.present.copyright;

import java.util.ArrayList;

/**
 * This class is a Dto version of the XML extracted from the 'mets:rightsMD' tag.
 *  
 * @author teg@kb.dk 
 *
 */


public class CopyrightAccessDto {
    private ArrayList<AccessCondition> accessConditionsList;

            
    public  CopyrightAccessDto() {                    
    }




    public ArrayList<AccessCondition> getAccessConditionsList() {
        return accessConditionsList;
    }



    public void setAccessConditionsList(ArrayList<AccessCondition> accessConditionsList) {
        this.accessConditionsList = accessConditionsList;
    }




    public class AccessCondition{
              
        private String type;
        private String displayLabel;    
        private String copyrightPublicationStatus;
        private String copyrightStatus;
        private String value;
        
        public String getType() {
            return type;
        }
        public void setType(String type) {
            this.type = type;
        }
        public String getDisplayLabel() {
            return displayLabel;
        }
        public void setDisplayLabel(String displayLabel) {
            this.displayLabel = displayLabel;
        }
            
        public String getValue() {
            return value;
        }
        public void setValue(String value) {
            this.value = value;
        }


        private ArrayList<CreatorPerson> creatorPersonList;
        
        public ArrayList<CreatorPerson> getCreatorPersonList() {
            return creatorPersonList;
        }
        public void setCreatorPersonList(ArrayList<CreatorPerson> creatorPersonList) {
            this.creatorPersonList = creatorPersonList;
        }
      
        public String getCopyrightPublicationStatus() {
            return copyrightPublicationStatus;
        }
        public void setCopyrightPublicationStatus(String copyrightPublicationStatus) {
            this.copyrightPublicationStatus = copyrightPublicationStatus;
        }
        public String getCopyrightStatus() {
            return copyrightStatus;
        }
        public void setCopyrightStatus(String copyrightStatus) {
            this.copyrightStatus = copyrightStatus;
        }
        
    }



    public class CreatorPerson {     
        private String name;
        private String yearBirth;
        private String yearDeath;
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
