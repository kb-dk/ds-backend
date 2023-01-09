package dk.kb.present.copyright;

import java.util.HashMap;

public class XsltCopyrightMapper {
    
    
    public static HashMap<String,String> xsltCopyrightTransformer(String modMedsXML) throws Exception{
                        
        HashMap<String,String> solrFieldsMap = new HashMap<String,String>(); 
        
        CopyrightAccessDto copyrightAccessDto = CopyrightAccessExtractor.extractCopyrightFields(modMedsXML);
        CopyrightAccessDto2SolrFieldsMapper mapper= new CopyrightAccessDto2SolrFieldsMapper(copyrightAccessDto);
        
        //Above has all as a DTO with setter and getter. But the XSLT wants a map
        solrFieldsMap.put("access_blokkeret",""+mapper.isBlokkeret());
        solrFieldsMap.put("access_note",mapper.getAccessNote());                               
        solrFieldsMap.put("access_skabelsesaar","");        
        solrFieldsMap.put("access_ophavsperson_doedsaar","");
        solrFieldsMap.put("access_searlige_visningsvilkaar","");
                                
        if (copyrightAccessDto.getImageUrl() != null) {
          solrFieldsMap.put("imageUrl",mapper.getImageUrl());
        }
        
        return solrFieldsMap;
    }

}
