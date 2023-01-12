package dk.kb.present.copyright;

import java.util.HashMap;
/**
 *
 * @author teg
 * 
 * This class is used to enrich the Solr document with additional fields in the XSLT transformation of Mods/Mets into a Solr document 
 *
 * As a temporary fix, this will also add the imageUrl to solr document. This logic must later be moved into the XSLT mapping.
 *
 */
public class XsltCopyrightMapper {
        
    //These fields must be define in the Solr schema.xml (and temporary also 'imageurl')
    public static final String ACCESS_BLOKERET_FIELD="access_blokeret";
    public static final String ACCESS_PLIGTAFLEVERET_FIELD="access_pligtafleveret";
    public static final String ACCESS_EJERMAERKE_FIELD="access_ejermaerke";
    public static final String ACCESS_NOTE_FIELD="access_note";
    public static final String ACCESS_SKABELSESAAR_FIELD="access_skabelsesaar";
    public static final String ACCESS_OPHAVSPERSON_DOEDSAAR_FIELD="access_ophavsperson_doedsaar";
    public static final String ACCESS_SEARLIGE_VISNINGSVILKAAR_FIELD="access_searlige_visningsvilkaar";
    public static final String ACCESS_MATERIALE_TYPE="access_materiale_type";
     
    
    public static HashMap<String,String> xsltCopyrightTransformer(String modMedsXML) throws Exception{
                        
        HashMap<String,String> solrFieldsMap = new HashMap<String,String>(); 
        
        CopyrightAccessDto copyrightAccessDto = CopyrightAccessExtractor.extractCopyrightFields(modMedsXML);
        CopyrightAccessDto2SolrFieldsMapper mapper= new CopyrightAccessDto2SolrFieldsMapper(copyrightAccessDto);
        
        solrFieldsMap.put(ACCESS_MATERIALE_TYPE, mapper.getMaterialeType());
                
        solrFieldsMap.put(ACCESS_BLOKERET_FIELD,""+mapper.isBlokeret()); //true or false
        solrFieldsMap.put(ACCESS_EJERMAERKE_FIELD,""+mapper.isEjerMaerke()); //true or false
        solrFieldsMap.put(ACCESS_PLIGTAFLEVERET_FIELD,""+mapper.isPligtAfleveret()); //true or false
        
        
        if (mapper.getAccessNote() != null) {
          solrFieldsMap.put(ACCESS_NOTE_FIELD,mapper.getAccessNote()); //String
        }
          
        if (copyrightAccessDto.getOphavsPersonDoedsAar() != null) {
            solrFieldsMap.put(ACCESS_OPHAVSPERSON_DOEDSAAR_FIELD,""+copyrightAccessDto.getOphavsPersonDoedsAar());    
        }
        
        solrFieldsMap.put(ACCESS_SKABELSESAAR_FIELD,""+copyrightAccessDto.getSkabelsesAar()); // will always be there.        
        
        if (mapper.getSearligevisningsVilkaar() != null) {
           solrFieldsMap.put(ACCESS_SEARLIGE_VISNINGSVILKAAR_FIELD,mapper.getSearligevisningsVilkaar());
        }        
        
        if (copyrightAccessDto.getImageUrl() != null) {
          solrFieldsMap.put("imageurl",mapper.getImageUrl());
        }
        
        return solrFieldsMap;
    }

}
