package dk.kb.present.copyright;

import java.util.ArrayList;

/**
 * This class is a Dto version of the XML extracted from the 'mets:rightsMD' tag.
 *  
 * @author teg@kb.dk 
 *
 */


public class CopyrightAccessDto {
    
    public static final String TYPE_RESTRICTION_ON_ACCESS= "restriction on access";
    public static final String TYPE_RESTRICTION_ON_ACCESS_NOTE="restriction on access note";
    public static final String TYPE_USE_AND_REPRODUCTION = "use and reproduction";
    public static final String TYPE_PLIGTAFLEVERING = "pligtaflevering";
        
    public static final String DISPLAY_LABEL_ACCESS_STATUS= "Access Status";
    public static final String DISPLAY_LABEL_RESTRICTED="Restricted "; //notice white space error 
    
    
    public static final String SPECIAL_RESTRICTION_BLOKKERET= "Blokeret";
    public static final String SPECIAL_RESTRICTION_EJERMAERKE= "Ejermærke";
    public static final String SPECIAL_RESTRICTION_VANDMAERKE=   "Vandmærke"; 
    public static final String SPECIAL_RESTRICTION_VISNING_KUN_AF_METADATA ="Visning kun af metadata";
    public static final String SPECIAL_RESTRICTION_VISNING_KUN_PAA_STEDET ="Visning kun på stedet";
    public static final String SPECIAL_RESTRICTION_PLIGTAFLEVERET ="Pligtafleveret";
    public static final String SPECIAL_RESTRICTION_CCBY="CC-BY";
    
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
        private ArrayList<CreatorPerson> creatorPersonList = new  ArrayList<CreatorPerson>(); 
        private ArrayList<CreatorCorporate> creatorCorporateList =  new ArrayList<CreatorCorporate>();
        
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
       
        public ArrayList<CreatorPerson> getCreatorPersonList() {
            return creatorPersonList;
        }
        public void setCreatorPersonList(ArrayList<CreatorPerson> creatorPersonList) {
            this.creatorPersonList = creatorPersonList;
        }
              
        public ArrayList<CreatorCorporate> getCreatorCorporateList() {
            return creatorCorporateList;
        }
        public void setCreatorCorporateList(ArrayList<CreatorCorporate> creatorCorporateList) {
            this.creatorCorporateList = creatorCorporateList;
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
    

    public class CreatorCorporate {     
        
        private String name;
        private String yearStarted;
        private String yearEnded;
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public String getYearStarted() {
            return yearStarted;
        }
        public void setYearStarted(String yearStarted) {
            this.yearStarted = yearStarted;
        }
        public String getYearEnded() {
            return yearEnded;
        }
        public void setYearEnded(String yearEnded) {
            this.yearEnded = yearEnded;
        }
      

    }

    

}
