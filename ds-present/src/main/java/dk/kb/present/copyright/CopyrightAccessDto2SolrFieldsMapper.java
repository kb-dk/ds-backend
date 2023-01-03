package dk.kb.present.copyright;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.present.copyright.CopyrightAccessDto.AccessCondition;
import dk.kb.present.copyright.CopyrightAccessDto.CreatorCorporate;
import dk.kb.present.copyright.CopyrightAccessDto.CreatorPerson;

public class CopyrightAccessDto2SolrFieldsMapper {

    private static final Logger log = LoggerFactory.getLogger(CopyrightAccessDto2SolrFieldsMapper.class);

    
    private boolean blokkeret = false;
    private String accessNote= "";
    private Integer lastDeathYearForPerson= null;
    //private Integer lastEndedYearForCorporate= null;
    private String specialPresentationRestriction=null; //Fra 'rullelisten'

    
    private String imageUrl=null; 
    public CopyrightAccessDto2SolrFieldsMapper(CopyrightAccessDto accessDto) {

        imageUrl=accessDto.getImageUrl();
        //Trin 1
        handleBlokkeret( accessDto);
        
        specialPresentationRestriction= extractSpecialPresentationRestriction(accessDto);
        lastDeathYearForPerson = accessDto.getCreatorPersonDeathYear();
                
        setAccessNote(accessDto);   
        //maybe return if blokkeret



    }

    

    public boolean isBlokkeret() {
        return blokkeret;
    }


    public String getImageUrl() {
        return imageUrl;
    }


   

    public Integer getLastDeathYearForPerson() {
        return lastDeathYearForPerson;
    }


    public String getAccessNote() {
        return accessNote;
    }

    public String getSpecialPresentationRestriction() {
        return specialPresentationRestriction;
    }


    public void setAccessNote(String accessNote) {
        this.accessNote = accessNote;
    }




    private  void handleBlokkeret(CopyrightAccessDto accessDto) {

        for (AccessCondition ac : accessDto.getAccessConditionsList()) {
            if ( ac.getType() != null && ac.getType().equals(CopyrightAccessDto.TYPE_RESTRICTION_ON_ACCESS) 
                    && ac.getDisplayLabel().equals(CopyrightAccessDto.DISPLAY_LABEL_ACCESS_STATUS)
                    && ac.getValue().equals(CopyrightAccessDto.SPECIAL_RESTRICTION_BLOKKERET)
                    ){
                this.blokkeret=true;                                 
                return;

            }


        }

    }


    private void setAccessNote(CopyrightAccessDto accessDto) {
        for (AccessCondition ac : accessDto.getAccessConditionsList()) {
            if ( ac.getType() != null && ac.getType().equals(CopyrightAccessDto.TYPE_RESTRICTION_ON_ACCESS_NOTE)) 
            {
                this.accessNote=ac.getValue();
            }
        }
    }








   
  
    //Has been decided not to use anyway....
    /*
    private  Integer getLastEndedYearForCorporate(CopyrightAccessDto accessDto) {

        Integer highestYear = null;
        for (AccessCondition ac: accessDto.getAccessConditionsList()) {

            for (CreatorCorporate cor : ac.getCreatorCorporateList()) {


                Integer year= extractYear(cor.getYearEnded());

                if (year == null) { //one coorporate not 'dead' yet. TODO check logic
                    return null;
                }                   
                if( highestYear==null || year > highestYear) {
                }
                highestYear=year;
            }

        }

        return highestYear;
    }
*/
    

    private  String extractSpecialPresentationRestriction(CopyrightAccessDto accessDto) {


        for (AccessCondition ac: accessDto.getAccessConditionsList()) {
               String value =ac.getValue();
               if (value != null) { //TODO. If there are more it will not be detected, but it is a meta data error according to model
                 return value;                    
               }
        }
        return null;
    }
    

    /*
     * Every special restriction type has a custom danish text for presentation layer 
     * TODO? What is text
     * 
     * 
     */
    /*
    private String generateJuridiskTekst(CopyrightAccessDto acDto) {
        
        for (AccessCondition ac :acDto.getAccessConditionsList()) {
            
            switch (ac.getValue()) {
            
             case SPECIAL_RESTRICTION_BLOKKERET_TEXT:
                
                break;
            case SPECIAL_RESTRICTION_CCBY_TEXT:
                break;
            case SPECIAL_RESTRICTION_EJERMAERKE_TEXT:
                break;
            case SPECIAL_RESTRICTION_PLIGTAFLEVERET_TEXT:
                break;
            case SPECIAL_RESTRICTION_VANDMAERKE_TEXT:
                break;
            case SPECIAL_RESTRICTION_VISNING_KUN_AF_METADATA_TEXT:
                break;
            case SPECIAL_RESTRICTION_VISNING_KUN_PAA_STEDET_TEXT:
                break;
                
            }            
            
        }        
    }
    */
    

   
}
