package dk.kb.present.copyright;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.present.copyright.CopyrightAccessDto.AccessCondition;

public class CopyrightAccessDto2SolrFieldsMapper {

    private static final Logger log = LoggerFactory.getLogger(CopyrightAccessDto2SolrFieldsMapper.class);

    
    private boolean blokeret = false;
    private String accessNote;
    private Integer ophavsPersonDoedsAar;
    private Integer skabelsesAar;
    private String searligeVisningsVilkaar; //Fra 'rullelisten'. (Blokeret er på rullelisten med håndteres specielt)
    private String materialeType;
    private boolean ejerMaerke;
    private boolean pligtAfleveret;
    private boolean fotoAftale;
    private boolean billedeAftale;
    private String ophavsretTekst; //Temporary field for jura
    private String filNavn;//used to log which records has errors
    private String imageUrl=null; 
    public CopyrightAccessDto2SolrFieldsMapper(CopyrightAccessDto accessDto) {
        
        blokeret = getBlokeret(accessDto);        
        searligeVisningsVilkaar= getSearligeVisningsVilkaar(accessDto);
        ophavsPersonDoedsAar = accessDto.getOphavsPersonDoedsAar();                
        skabelsesAar=accessDto.getSkabelsesAar();
        filNavn= accessDto.getFilNavn();
        accessNote=getAccessNote(accessDto);   
        
        //Vandmærke benyttes ikke længere
        ejerMaerke=getEjermaerke(accessDto);        
        pligtAfleveret=getPligtAfleveret(accessDto);
        materialeType=accessDto.getMaterialeType();  
        imageUrl=accessDto.getImageUrl();
        fotoAftale=accessDto.isFotoAftale();
        billedeAftale=accessDto.isBilledeAftale();
        System.out.println(billedeAftale);
     
        //Very important TEMPORARY logic with hardcoded data so 'jura' can test.
        if ((ophavsPersonDoedsAar!= null && ophavsPersonDoedsAar <=  1952)  || (skabelsesAar!= null && skabelsesAar <=  1882)) {
            ophavsretTekst="Fri af ophavsret";            
        }
        else {
            ophavsretTekst="Beskyttet af ophavsret"; 
           // Some material types can have further restrictions, and they not always set correct. 
            // It was too much manual work to do this by hand for the curators and too many errors.
            if (!fotoAftale && !billedeAftale) {
                 searligeVisningsVilkaar=CopyrightAccessDto.SPECIAL_RESTRICTION_VISNING_KUN_PAA_STEDET;              
            }                        
        }        
    }
    public String getFilnavn() {
       return filNavn;        
    }    
    public String getOphavsretTekst() {
        return ophavsretTekst;
    }
    
    public boolean isPligtAfleveret() {
        return pligtAfleveret;
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


    public String getMaterialeType() {
        return materialeType;
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

    public boolean isFotoAftale() {
        return fotoAftale;
    }

    public boolean isBilledeAftale() {
        return billedeAftale;
    }
    
    private boolean getBlokeret(CopyrightAccessDto accessDto) {

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
   
    
    //<mods:accessCondition type="pligtaflevering">Pligtafleveret</mods:accessCondition>    
    private  boolean getPligtAfleveret(CopyrightAccessDto accessDto) {
        for (AccessCondition ac: accessDto.getAccessConditionsList()) {
            if (CopyrightAccessDto.TYPE_PLIGTAFLEVERING.equals(ac.getType()) &&                
                "Pligtafleveret".equals(ac.getValue())){                                         
                return true;                                                   
            }            
        }
        return false;
    }
       
   
}
