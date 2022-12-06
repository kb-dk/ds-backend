package dk.kb.present.copyright;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.present.copyright.CopyrightAccessDto.AccessCondition;
import dk.kb.present.copyright.CopyrightAccessDto.CreatorPerson;

public class CopyrightAccessDto2SolrFieldsMapper {

    private static final Logger log = LoggerFactory.getLogger(CopyrightAccessDto2SolrFieldsMapper.class);
    
    private boolean blokkeret = false;
    private String accessNote= "";
    private Integer lastDeathYearForPersonWithFamiliyName= null;



    public  CopyrightAccessDto2SolrFieldsMapper(CopyrightAccessDto accessDto) {

        //Trin 1
        handleBlokkeret( accessDto);
        setAccessNote(accessDto);   
        //maybe return if blokkeret
        handleStep1(accessDto);
        //maybe return
        
    }




    private  void handleBlokkeret(CopyrightAccessDto accessDto) {

        for (AccessCondition ac : accessDto.getAccessConditionsList()) {
            if ( ac.getType() != null && ac.getType().equals(CopyrightAccessDto.TYPE_RESTRICTION_ON_ACCESS) 
                    && ac.getDisplayLabel().equals(CopyrightAccessDto.DISPLAY_LABEL_ACCESS_STATUS)
                    && ac.getValue().equals(CopyrightAccessDto.VALUE_BLOKKERET)
                    ){
                this.blokkeret=true;                                 
                return;

            }


        }

    }
    
    private  void handleStep1(CopyrightAccessDto accessDto) {
        lastDeathYearForPersonWithFamiliyName =  getLastDeathYearForPersonWithFamilyName(accessDto);
    }

    
    

    private void setAccessNote(CopyrightAccessDto accessDto) {
        for (AccessCondition ac : accessDto.getAccessConditionsList()) {
            if ( ac.getType() != null && ac.getType().equals(CopyrightAccessDto.TYPE_RESTRICTION_ON_ACCESS_NOTE)) 
            {
                this.accessNote=ac.getValue();
            }
        }
    }


    public boolean isBlokkeret() {
        return blokkeret;
    }


    public String getAccessNote() {
        return accessNote;
    }



    
     public Integer getLastDeathYearForPersonWithFamiliyName() {
        return lastDeathYearForPersonWithFamiliyName;
    }





    /* Lastname can not be read in accesscondition. Assume a comma (,) in name means he has a familyname (last name)
      * Will read all creator.persons and find last death year for person without familyname
      *  
      *  
      * Will return null if there is no year. (ie. person not death etc.)
      */       
    private  Integer getLastDeathYearForPersonWithFamilyName(CopyrightAccessDto accessDto) {
        
        Integer highestYear = null;
        for (AccessCondition ac: accessDto.getAccessConditionsList()) {
        
            for (CreatorPerson p : ac.getCreatorPersonList()) {
                boolean hasLastName = p.getName().indexOf(",")>0;
                if (hasLastName) {
                Integer year= extractYear(p.getYearDeath());
          System.out.println("parsed year:"+year);
                if (highestYear == null || year > highestYear) {
                    highestYear=year;
                }
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
    private Integer extractYear(String yearString) {
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
    
}
