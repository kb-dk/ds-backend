package dk.kb.present.copyright;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 *
 * @author teg
 * 
 * This class is used to enrich the Solr document with additional fields in the XSLT transformation of Mods/Mets into a Solr document 
 *
 * As a temporary fix, this will also add the imageUrl to solr document. This logic must later be moved into the XSLT mapping.
 *
 * The following fields must be defined in the solr schema.xml
 * 
 * field                          | type
 * ----------------------------------------------
 * access_blokeret                | boolean
 * access_pligtafleveret          | boolean
 * access_ejermaerke              | boolean
 * access_note                    | String (only if value)
 * access_skabelsesaar            | int (only if value)
 * access_ophavsperson_doedsaar   | int (only if value)  
 * access_ophavsret_tekst         | string   (temporary!)
 * access_searlige_visningsvilkaar| String (only if value)
 * access_materiale_type          | String (always)   
 * access_foto_aftale             | boolean
 * access_billede_aftale          | boolean
 * imageurl                       | String (temporary hack) 
 */
public class XsltCopyrightMapper {
        
    private static final Logger log = LoggerFactory.getLogger(XsltCopyrightMapper.class);
	
    //These fields must be define in the Solr schema.xml (and temporary also 'imageurl')
    public static final String ACCESS_BLOKERET_FIELD="access_blokeret";
    public static final String ACCESS_PLIGTAFLEVERET_FIELD="access_pligtafleveret";
    public static final String ACCESS_EJERMAERKE_FIELD="access_ejermaerke";
    public static final String ACCESS_NOTE_FIELD="access_note";
    public static final String ACCESS_SKABELSESAAR_FIELD="access_skabelsesaar";
    public static final String ACCESS_OPHAVSPERSON_DOEDSAAR_FIELD="access_ophavsperson_doedsaar";
    public static final String ACCESS_OPHAVSRET_TEKST_FIELD="access_ophavsret_tekst";    
    public static final String ACCESS_SEARLIGE_VISNINGSVILKAAR_FIELD="access_searlige_visningsvilkaar";
    public static final String ACCESS_MATERIALE_TYPE="access_materiale_type";
    public static final String ACCESS_FOTO_AFTALE_FIELD="access_foto_aftale";
    public static final String ACCESS_BILLEDE_AFTALE_FIELD="access_billede_aftale";
     
            
    /**
     * Will return a map of key value pair to enrich the XSLT. These additional field all starts with 'access_'. See final ACCESS values in class.
     * The extracted values are not just a field extract from a single field, but depend on various copyright logic. These values
     * are not used for presentation, but for all access validation when searching/viewing the records/images. The fields in solr are
     * used in licensemodule. 
     * 
     * @param modMedsXML The med/mods record in XML format
     * @return
     * @throws Exception
     */
    
    public static HashMap<String,String> applyXsltCopyrightTransformer (String modMedsXML) throws Exception{
                        
        HashMap<String,String> solrFieldsMap = new HashMap<String,String>(); 
        CopyrightAccessDto copyrightAccessDto = null;
        try {
        copyrightAccessDto = CopyrightAccessExtractor.buildCopyrightFields(modMedsXML);        
        CopyrightAccessDto2SolrFieldsMapper mapper= new CopyrightAccessDto2SolrFieldsMapper(copyrightAccessDto);
        
        solrFieldsMap.put(ACCESS_MATERIALE_TYPE, mapper.getMaterialeType());
                
        solrFieldsMap.put(ACCESS_BLOKERET_FIELD,""+mapper.isBlokeret()); //true or false
        solrFieldsMap.put(ACCESS_EJERMAERKE_FIELD,""+mapper.isEjerMaerke()); //true or false
        solrFieldsMap.put(ACCESS_PLIGTAFLEVERET_FIELD,""+mapper.isPligtAfleveret()); //true or false
        solrFieldsMap.put(ACCESS_OPHAVSRET_TEKST_FIELD,mapper.getOphavsretTekst());        
        solrFieldsMap.put(ACCESS_BILLEDE_AFTALE_FIELD,""+mapper.isBilledeAftale()); //true or false
        solrFieldsMap.put(ACCESS_FOTO_AFTALE_FIELD,""+mapper.isFotoAftale()); //true or false
        
        if (mapper.getAccessNote() != null) {
          solrFieldsMap.put(ACCESS_NOTE_FIELD,mapper.getAccessNote()); //String
        }
          
        if (copyrightAccessDto.getOphavsPersonDoedsAar() != null) {
            solrFieldsMap.put(ACCESS_OPHAVSPERSON_DOEDSAAR_FIELD,""+copyrightAccessDto.getOphavsPersonDoedsAar());    
        }
        
        if (copyrightAccessDto.getSkabelsesAar() != null ) {
        	solrFieldsMap.put(ACCESS_SKABELSESAAR_FIELD,""+copyrightAccessDto.getSkabelsesAar()); // maybe only use this    
        }        
     
        if (mapper.getSearligevisningsVilkaar() != null) {
           solrFieldsMap.put(ACCESS_SEARLIGE_VISNINGSVILKAAR_FIELD,mapper.getSearligevisningsVilkaar());
        }        
        
        if (copyrightAccessDto.getImageUrl() != null) {
          solrFieldsMap.put("imageurl",mapper.getImageUrl());
        }
        }
        catch(Exception e) {
        	//Data error! will be fixed
            log.error("Error transforming... Probably data error:"+e.getMessage());
            solrFieldsMap.put(ACCESS_SKABELSESAAR_FIELD,"9999");       
        	
        }
        
        return solrFieldsMap;
    }

}
