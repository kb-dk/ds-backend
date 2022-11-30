package dk.kb.present.copyright;

import dk.kb.present.copyright.CopyrightAccessDto.AccessCondition;

public class CopyrightAccessDto2SolrFieldsMapper {


    private boolean blokkeret = false;
    private String blokkeret_note = "";
    //ACCESS_NOTE Måske dække alle notes
    
    
    
    public CopyrightAccessDto2SolrFieldsMapper(CopyrightAccessDto accessDto) {
        
        
        
    }

    private void handleBlokkeret(CopyrightAccessDto accessDto) {
        
       if (accessDto.getAccessConditionsList().size() >0) {           
           AccessCondition ac = accessDto.getAccessConditionsList().get(0);
     
           String displayLabel = ac.getDisplayLabel();
           
           /*      
           <mods:accessCondition type="restriction on access" displayLabel="Access Status">Blokeret</mods:accessCondition>
           if (ac.getType()
    */      
           
           
       }
        
        
    }
    
    
}
