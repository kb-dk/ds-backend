package dk.kb.license.audit;


/**
 * All these will be defined in the yaml config
 * 
 */
public class AuditUtil {

    enum ChangeType {
        CREATE,
        DELETE,        
        UPDATE
      }

    enum ObjectType  {
        CLAUSE_RESTRICTED_ID, 
        CLAUSE_PRODUCTION_CODE,
        CLAUSE_STRICT_TITLE,
        PRESENTATION_TYPE, 
        LICENSE,
        GROUP_TYPE,
        HOLDBACK
    }
    
    
}
