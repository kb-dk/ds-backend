package dk.kb.license.facade;

import java.sql.SQLException;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.license.storage.AttributeType;
import dk.kb.license.storage.AuditLog;
import dk.kb.license.storage.GroupType;
import dk.kb.license.storage.PresentationType;
import dk.kb.license.util.ChangeDifferenceText;
import dk.kb.license.util.LicenseChangelogGenerator;
import dk.kb.license.storage.License;
import dk.kb.license.storage.LicenseCache;
import dk.kb.license.storage.LicenseModuleStorage;
import dk.kb.license.webservice.exception.InternalServiceException;
import dk.kb.license.webservice.exception.InvalidArgumentServiceException;



public class LicenseModuleFacade {

    private static final Logger log = LoggerFactory.getLogger(LicenseModuleFacade.class);

    public static void persistDomLicensePresentationType(String key, String value_dk, String value_en) throws Exception {    
        
        performStorageAction("persistDomLicensePresentationType(" + key + ","+value_dk +","+value_en+")", storage -> {
        
            PresentationType newType = new PresentationType(0, key, value_dk, value_en);
            storage.persistLicensePresentationType(key, value_dk, value_en);
            ChangeDifferenceText changes = LicenseChangelogGenerator.getPresentationTypeChanges(null, newType);
            AuditLog auditLog = new AuditLog(System.currentTimeMillis(),"anonymous","Create presentationtype", key, changes.getBefore(), changes.getAfter());
            storage.persistAuditLog(auditLog);
            
            return null;
        
        });
        LicenseCache.reloadCache(); // Database changed, so reload cache
    }

    public static ArrayList<PresentationType> getDomLicensePresentationTypes() throws Exception {
       return performStorageAction("persistDomLicensePresentationType()", storage -> {
            return storage.getLicensePresentationTypes();                   
        });
                
    }

    public static void deleteLicense(long licenseId) throws Exception { 
        performStorageAction("deleteLicense(" + licenseId + ")", storage -> {
            storage.deleteLicense(licenseId);
            return null;
        
        });
        LicenseCache.reloadCache(); // Database changed, so reload cache
     
    }

    public static void persistDomLicenseGroupType(String key, String value, String value_en, String description, String description_en, String query, boolean denyGroup) throws Exception {
 
        performStorageAction("persistDomLicenseGroupType(" + key+","+value+","+value_en +","+description +","+description_en +","+query+","+denyGroup+")", storage -> {                    
            storage.persistLicenseGroupType(key, value, value_en, description, description_en, query, denyGroup);        
            return null;
        
        });
        LicenseCache.reloadCache(); // Database changed, so reload cache
        
    }

    public static void updateDomLicenseGroupType(long id, String value_dk, String value_en, String description, String description_en, String query, boolean denyGroup) throws Exception {
      
        performStorageAction("updateDomLicenseGroupType(" + id+","+value_dk+","+value_en +","+description +","+description_en +","+query+","+denyGroup+")", storage -> {
        storage.updateLicenseGroupType(id, value_dk, value_en, description, description_en, query, denyGroup);
            
            return null;
        
        });
        LicenseCache.reloadCache(); // Database changed, so reload cache
        
       

    }

    public static void updateDomPresentationType(long id, String value_dk, String value_en) throws Exception {
       
        performStorageAction("updateDomLicenseGroupType(" + id+","+value_dk+","+value_en +")", storage -> {

           PresentationType oldType = storage.getPresentationTypeById(id);
           PresentationType newType = new PresentationType(id,oldType.getKey(),value_dk,value_en);
           storage.updatePresentationType(id, value_dk, value_en);

           ChangeDifferenceText changes = LicenseChangelogGenerator.getPresentationTypeChanges(oldType, newType);
           AuditLog auditLog = new AuditLog(System.currentTimeMillis(),"anonymous","Update presentationtype", oldType.getKey(), changes.getBefore(), changes.getAfter());
           storage.persistAuditLog(auditLog);
           return null;        
        });
        LicenseCache.reloadCache(); // Database changed, so reload cache

    }
    
    public static void deleteDomLicenseGroupType(String groupName) throws Exception {
   
        performStorageAction("deleteDomLicenseGroupType(" + groupName +")", storage -> {
            storage.deleteLicenseGroupType(groupName);
            return null;        
        });
        LicenseCache.reloadCache(); // Database changed, so reload cache  
    }
    
    
    public static void deleteDomPresentationType(String presentationName) throws Exception {
        performStorageAction("deleteDomPresentationType(" + presentationName +")", storage -> {
            PresentationType oldType = storage.getPresentationTypeByKey(presentationName);
            storage.deletePresentationType(presentationName);

           ChangeDifferenceText changes = LicenseChangelogGenerator.getPresentationTypeChanges(oldType, null);
           AuditLog auditLog = new AuditLog(System.currentTimeMillis(),"anonymous","Delete presentationtype", oldType.getKey(), changes.getBefore(), changes.getAfter());                   
           storage.persistAuditLog(auditLog);            
            return null;        
        });
        LicenseCache.reloadCache(); // Database changed, so reload cache  
    }
        
      

    /**
     * If license id=0 a new will be created. Else it will update the license with the id
     * 
     * @param license
     * @throws Exception
     */
    
    public static void persistLicense(License license)  throws Exception {
        
        performStorageAction("persistLicense(description_dk=" + license.getDescription_dk() +")", storage -> {
            AuditLog auditLog = null;
            //audit log
            if (license.getId() == 0 ) {
               ChangeDifferenceText changes = LicenseChangelogGenerator.getLicenseChanges(null,license);              
               auditLog = new AuditLog(System.currentTimeMillis(),"anonymous","Create New License", license.getLicenseName(), changes.getBefore(), changes.getAfter());               

            }
            else {
               License oldLicense = storage.getLicense(license.getId());
               ChangeDifferenceText changes = LicenseChangelogGenerator.getLicenseChanges(oldLicense, license);
               auditLog = new AuditLog(System.currentTimeMillis(),"anonymous","Update License", license.getLicenseName(), changes.getBefore(), changes.getAfter());                                               
            }
            
            storage.persistAuditLog(auditLog);            
            storage.persistLicense(license);
            return null;        
        });
        LicenseCache.reloadCache(); // Database changed, so reload cache        
    }
    

    
    public static ArrayList<GroupType> getDomLicenseGroupTypes() throws Exception {
   
        return performStorageAction("getDomLicenseGroupTypes()", storage -> {
            return storage.getLicenseGroupTypes();
                    
        });                        
       
    }
    
    public static void persistDomAttributeType(String attributeTypeName) throws Exception {
        performStorageAction("persistDomAttributeType("+attributeTypeName+")", storage -> {
            AuditLog auditLog = new AuditLog(System.currentTimeMillis(),"anonymous","Create attribute", attributeTypeName,"",attributeTypeName);
            storage.persistAttributeType(attributeTypeName);
            storage.persistAuditLog(auditLog);
            return null;        
        });
        LicenseCache.reloadCache(); // Database changed, so reload cache        
    }
    
    public static void deleteDomAttributeType(String attributeTypeName) throws Exception {     
        performStorageAction(" deleteDomAttributeType("+attributeTypeName+")", storage -> {
            AuditLog auditLog = new AuditLog(System.currentTimeMillis(),"anonymous","Delete attribute", attributeTypeName,attributeTypeName,"");
            storage.deleteAttributeType(attributeTypeName);
            storage.persistAuditLog(auditLog);
            return null;        
        });
        LicenseCache.reloadCache(); // Database changed, so reload cache              
    }
     
    
    public static ArrayList<AttributeType> getDomAttributeTypes() throws Exception {
        return performStorageAction("getDomAttributeTypes()", storage -> {
            return storage.getAttributeTypes();                    
        });                                       
    }
    
    
    
    
    public static ArrayList<License> getAllLicenseNames() throws Exception {
        return performStorageAction("getAllLicenseNames()", storage -> {
            return storage.getAllLicenseNames();                    
        });            
        

    }
    
    public static License getLicense(long licenseId)throws Exception {
        return performStorageAction("getLicense("+licenseId+")", storage -> {
            return storage.getLicense(licenseId);                    
        });           

            }
    
    /**
     * Starts a storage transaction and performs the given action on it, returning the result from the action.
     *
     * If the action throws an exception, a {@link LicenseModuleStorage#rollback()} is performed.
     * If the action passes without exceptions, a {@link LicenseModuleStorage#commit()} is performed.
     * @param actionID a debug-oriented ID for the action, typically the name of the calling method.
     * @param action the action to perform on the storage.
     * @return return value from the action.
     * @throws InternalServiceException if anything goes wrong.
     */
    private static <T> T performStorageAction(String actionID, StorageAction<T> action) {
        try (LicenseModuleStorage storage = new LicenseModuleStorage()) {
            T result;
            try {
                result = action.process(storage);
            }
            catch(InvalidArgumentServiceException e) {
                log.warn("Exception performing action '{}'. Initiating rollback", actionID, e.getMessage());
                storage.rollback();
                throw new InvalidArgumentServiceException(e);                
            }            
            catch (Exception e) {
                log.warn("Exception performing action '{}'. Initiating rollback", actionID, e);
                storage.rollback();
                throw new InternalServiceException(e);
            }

            try {
                storage.commit();
            } catch (SQLException e) {
                log.error("Exception committing after action '{}'", actionID, e);
                throw new InternalServiceException(e);
            }

            return result;
        } catch (SQLException e) { //Connecting to storage failed
            log.error("SQLException performing action '{}'", actionID, e);
            throw new InternalServiceException(e);
        }
    }

    /**
     * Callback used with {@link #performStorageAction(String, StorageAction)}.
     * @param <T> the object returned from the {@link StorageAction#process(DsStorage)} method.
     */
    @FunctionalInterface
    private interface StorageAction<T> {
        /**
         * Access or modify the given storage inside of a transaction.
         * If the method throws an exception, it will be logged, a {@link LicenseModuleStorage#rollback()} will be performed and
         * a wrapping {@link dk.kb.storage.webservice.exception.ServiceException} will be thrown.
         * @param storage a storage ready for requests and updates.
         * @return custom return value.
         * @throws Exception if something went wrong.
         */
        T process(LicenseModuleStorage storage) throws Exception;
    }
    
    
        
}
