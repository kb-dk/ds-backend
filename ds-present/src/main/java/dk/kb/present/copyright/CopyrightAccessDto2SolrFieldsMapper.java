package dk.kb.present.copyright;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.present.copyright.CopyrightAccessDto.AccessCondition;

public class CopyrightAccessDto2SolrFieldsMapper {

    private static final Logger log = LoggerFactory.getLogger(CopyrightAccessDto2SolrFieldsMapper.class);

    
    private boolean blokeret = false;
    private String accessNote= "";
    private Integer ophavsPersonDoedsAar= null;
    private Integer skabelsesAar= null;
    private String searligeVisningsVilkaar=null; //Fra 'rullelisten'
    private boolean ejerMaerke;
    private boolean vandMaerke;
    
    private String imageUrl=null; 
    public CopyrightAccessDto2SolrFieldsMapper(CopyrightAccessDto accessDto) {
        
        blokeret = handleBlokeret(accessDto);        
        searligeVisningsVilkaar= getSearligeVisningsVilkaar(accessDto);
        ophavsPersonDoedsAar = accessDto.getOphavsPersonDoedsAar();                
        skabelsesAar=accessDto.getSkabelsesAar();
        
        accessNote=getAccessNote(accessDto);   
        //maybe return if blokkeret

        //Ejermærke, vandmærke
        ejerMaerke=getEjermaerke(accessDto);
        vandMaerke=getVandmaerke(accessDto);
        
        imageUrl=accessDto.getImageUrl();
        

    }

    
    public void setVandMaerke(boolean vandMaerke) {
        this.vandMaerke = vandMaerke;
    }


    public boolean isEjerMaerke() {
        return ejerMaerke;
    }

    public boolean isBlokeret() {
        return blokeret;
    }


    public String getImageUrl() {
        return imageUrl;
    }



    public Integer getSkabelsesAar() {
        return skabelsesAar;
    }



    public Integer getLastDeathYearForPerson() {
        return ophavsPersonDoedsAar;
    }


    public String getAccessNote() {
        return accessNote;
    }

    public String getSearligevisningsVilkaar() {
        return searligeVisningsVilkaar;
    }


    public void setAccessNote(String accessNote) {
        this.accessNote = accessNote;
    }




    private  boolean handleBlokeret(CopyrightAccessDto accessDto) {

        for (AccessCondition ac : accessDto.getAccessConditionsList()) {
            if ( ac.getType() != null && ac.getType().equals(CopyrightAccessDto.TYPE_RESTRICTION_ON_ACCESS) 
                    && ac.getDisplayLabel().equals(CopyrightAccessDto.DISPLAY_LABEL_ACCESS_STATUS)
                    && ac.getValue().equals(CopyrightAccessDto.SPECIAL_RESTRICTION_BLOKERET)
                    ){
                return true;
            }

        }
        return false;

    }


    private String getAccessNote(CopyrightAccessDto accessDto) {
        for (AccessCondition ac : accessDto.getAccessConditionsList()) {
            if (CopyrightAccessDto.TYPE_RESTRICTION_ON_ACCESS_NOTE.equals(ac.getType()))
            {
                return ac.getValue();
            }
        }
        return null;
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
    
    
    
    //<mods:accessCondition type="restriction on access" displayLabel="Access Status">Visning kun på stedet</mods:accessCondition>    
    //Type and DisplayLabel must match above
    //A record can only have one of these.
    private  String getSearligeVisningsVilkaar(CopyrightAccessDto accessDto) {

        for (AccessCondition ac: accessDto.getAccessConditionsList()) {
            if (CopyrightAccessDto.TYPE_RESTRICTION_ON_ACCESS.equals(ac.getType()) && 
                CopyrightAccessDto.DISPLAY_LABEL_ACCESS_STATUS.equals(ac.getDisplayLabel())){           
                String value =ac.getValue();              
                return value;                                                   
            }            
        }
        return null;
    }
    


    // <mods:accessCondition type="use and reproduction" displayLabel="Restricted">Ejermærke</mods:accessCondition>    
    private  boolean getEjermaerke(CopyrightAccessDto accessDto) {
        for (AccessCondition ac: accessDto.getAccessConditionsList()) {
            if (CopyrightAccessDto.TYPE_USE_AND_REPRODUCTION.equals(ac.getType()) && 
                CopyrightAccessDto.DISPLAY_LABEL_RESTRICTED.equals(ac.getDisplayLabel()) &&
                "Ejermærke".equals(ac.getValue())){                                         
                return true;                                                   
            }            
        }
        return false;
    }
    
 // <mods:accessCondition type="use and reproduction" displayLabel="Restricted">Ejermærke</mods:accessCondition>    
    private  boolean getVandmaerke(CopyrightAccessDto accessDto) {
        for (AccessCondition ac: accessDto.getAccessConditionsList()) {
            if (CopyrightAccessDto.TYPE_USE_AND_REPRODUCTION.equals(ac.getType()) && 
                CopyrightAccessDto.DISPLAY_LABEL_RESTRICTED.equals(ac.getDisplayLabel()) &&
                CopyrightAccessDto.USE_AND_REPRODUCTION_EJERMAERKE.equals(ac.getValue())){                                         
                return true;                                                   
            }            
        }
        return false;
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
