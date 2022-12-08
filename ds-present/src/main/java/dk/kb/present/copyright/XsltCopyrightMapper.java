package dk.kb.present.copyright;

import java.util.HashMap;

public class XsltCopyrightMapper {
    
    
    public static HashMap<String,String> xsltCopyrightTransformer(String modMedsXML) throws Exception{
                        
        HashMap<String,String> solrFieldsMap = new HashMap<String,String>(); 
        
        CopyrightAccessDto copyrightAccessDto = CopyrightAccessExtractor.extractCopyrightFields(modMedsXML);
        CopyrightAccessDto2SolrFieldsMapper mapper= new CopyrightAccessDto2SolrFieldsMapper(copyrightAccessDto);
        
        //Above has all as a DTO with setter and getter. But the XSLT wants a map
        solrFieldsMap.put("blocked",""+mapper.isBlokkeret());
        solrFieldsMap.put("accessNote",mapper.getAccessNote());        
        
        return solrFieldsMap;
    }

}
