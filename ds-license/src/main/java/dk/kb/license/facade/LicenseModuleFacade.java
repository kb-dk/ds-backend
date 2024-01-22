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
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;

/**
 * @author Thomas Egense
 * 
 * The LicenseModuleFacade expose all methods that can be called on LicenceModule. This incluces both persistence logic and business logic resolving licence access. 
 * 
 * This facade class is also responsible for the storage transactional integrity. The storage model will never commit or rollback. All storage
 * transactional logic is controlled by this class. This makes it possible to use several storage method as building blocks and rollback 
 * everything if one of the steps fails. The method {@link #performStorageAction) performStorageAction} is used for all storage call and responsible for commit or rollback.
 *  
 */

public class LicenseModuleFacade {

    private static final Logger log = LoggerFactory.getLogger(LicenseModuleFacade.class);
    
    /**
     * Create a new PresentationType that will be storage and available to be added to licences. A newly created
     * presentationtype will be added the any existing licences.
     *   
     * @param key Unique identifier for the presentation type that is used when requesting access. Use alphanumeric value with no white spaces. Examples: Thumbnails, Search, Stream 
     * @param value_dk This text (danish) will be shown when selected the presentation type and adding it to a license. Keep it short.
     * @param value_en This text (english) will be shown when selected the presentation type and adding it to a license. Keep it short.
     */
    public static void persistLicensePresentationType(String key, String value_dk, String value_en) {
        
        performStorageAction("persistLicensePresentationType(" + key + ","+value_dk +","+value_en+")", storage -> {
        
            PresentationType newType = new PresentationType(0, key, value_dk, value_en);
            storage.persistLicensePresentationType(key, value_dk, value_en);
            ChangeDifferenceText changes = LicenseChangelogGenerator.getPresentationTypeChanges(null, newType);
            AuditLog auditLog = new AuditLog(System.currentTimeMillis(),"anonymous","Create presentationtype", key, changes.getBefore(), changes.getAfter());
            storage.persistAuditLog(auditLog);
            
            return null;
        
        });
        LicenseCache.reloadCache(); // Database changed, so reload cache
    }

    /**
     * Get a list of all defined Presentation types. This is only intended to be called from the admin GUI frontend for when creating/updating a license
     * to show all posible presentation types that can be added to the licence.
     * 
     * 
     * @return List of presentationtype DTO's
     */
    public static ArrayList<PresentationType> getLicensePresentationTypes() {
       return performStorageAction("persistLicensePresentationType()", storage -> {
            return storage.getLicensePresentationTypes();                   
        });
                
    }

    
    /**
     * Retrieve a specific audit log change by id. The id is millis at the time for the change.
     * Call the getAllAuditLogs {@link #getAllAuditLogs() getAllAuditLogs} to see all entries with their ids
     *  
     * @param millis The ID of the log. 
     * @return
     */
    public static AuditLog getAuditLog(Long millis) {
        return performStorageAction("getAuditLog()", storage -> {
             return storage.getAuditLog(millis);
         });                
     }
     
    /**
     * Returns a list of all auditlogs.
     *   
     * @return List of all auditlogs
     */    
    public static ArrayList<AuditLog> getAllAuditLogs() {
       return performStorageAction("getAllAuditLogs()", storage -> {
            return storage.getAllAudit();                   
        });                
    }
    
    
    
    /**
     * Delete a license completely. Instead of deleting a license it is also a option to disable it by changing valid-to attribute.
     *  
     * @param licenseId
     */    
    public static void deleteLicense(long licenseId) {
        performStorageAction("deleteLicense(" + licenseId + ")", storage -> {

            License license = storage.getLicense(licenseId);
            ChangeDifferenceText changes = LicenseChangelogGenerator.getLicenseChanges(license,null);              

            AuditLog auditLog = new AuditLog(System.currentTimeMillis(),"anonymous","Delete License", license.getLicenseName(), changes.getBefore(), changes.getAfter());               
            
            storage.deleteLicense(licenseId);
            storage.persistAuditLog(auditLog);
            return null;
        
        });
        LicenseCache.reloadCache(); // Database changed, so reload cache
     
    }

    
    /**
     * 
     * 
     */
    public static void persistLicenseGroupType(String key, String value, String value_en, String description, String description_en, String query, boolean isRestriction) {
 
        performStorageAction("persistLicenseGroupType(" + key+","+value+","+value_en +","+description +","+description_en +","+query+","+ isRestriction+")", storage -> {                    
        GroupType g = new GroupType(0L,key,value,value_en,description,description_en,query, isRestriction);

        ChangeDifferenceText changes = LicenseChangelogGenerator.getGroupTypeChanges(null, g);
        AuditLog auditLog = new AuditLog(System.currentTimeMillis(),"anonymous","Create grouptype", key, changes.getBefore(), changes.getAfter());        
        storage.persistLicenseGroupType(key, value, value_en, description, description_en, query,  isRestriction);        
        storage.persistAuditLog(auditLog);
        return null;
        
        });
        LicenseCache.reloadCache(); // Database changed, so reload cache        
    }

    public static void updateLicenseGroupType(long id, String value_dk, String value_en, String description, String description_en, String query, boolean  isRestriction) {
      
        performStorageAction("updateLicenseGroupType(" + id+","+value_dk+","+value_en +","+description +","+description_en +","+query+","+ isRestriction+")", storage -> {
           GroupType oldGroupType = storage.getGroupTypeById(id);
            
            GroupType updateGroupType = new GroupType(0L,oldGroupType.getKey(),value_dk,value_en,description,description_en,query, isRestriction);
            ChangeDifferenceText changes = LicenseChangelogGenerator.getGroupTypeChanges(oldGroupType, updateGroupType);
            AuditLog auditLog = new AuditLog(System.currentTimeMillis(),"anonymous","Update grouptype", value_dk, changes.getBefore(), changes.getAfter());        
                      
            storage.updateLicenseGroupType(id, value_dk, value_en, description, description_en, query,  isRestriction);
            storage.persistAuditLog(auditLog);
            return null;
        
        });
        LicenseCache.reloadCache(); // Database changed, so reload cache
        
       

    }

    public static void updatePresentationType(long id, String value_dk, String value_en) {
       
        performStorageAction("updateLicenseGroupType(" + id+","+value_dk+","+value_en +")", storage -> {

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
    
    public static void deleteLicenseGroupType(String groupName) {
   
        performStorageAction("deleteLicenseGroupType(" + groupName +")", storage -> {
           AuditLog auditLog = new AuditLog(System.currentTimeMillis(),"anonymous","Delete grouptype",groupName,groupName,"");
           storage.persistAuditLog(auditLog);
            storage.deleteLicenseGroupType(groupName);
            return null;        
        });
        LicenseCache.reloadCache(); // Database changed, so reload cache  
    }
    
    
    public static void deletePresentationType(String presentationName) {
        performStorageAction("deletePresentationType(" + presentationName +")", storage -> {
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
     */
    
    public static void persistLicense(License license) {
        
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
    

    
    public static ArrayList<GroupType> getLicenseGroupTypes() {
   
        return performStorageAction("getLicenseGroupTypes()", storage -> {
            return storage.getLicenseGroupTypes();
                    
        });                        
       
    }
    
    public static void persistAttributeType(String attributeTypeName) {
        performStorageAction("persistAttributeType("+attributeTypeName+")", storage -> {
            AuditLog auditLog = new AuditLog(System.currentTimeMillis(),"anonymous","Create attribute", attributeTypeName,"",attributeTypeName);
            storage.persistAttributeType(attributeTypeName);
            storage.persistAuditLog(auditLog);
            return null;        
        });
        LicenseCache.reloadCache(); // Database changed, so reload cache        
    }
    
    public static void deleteAttributeType(String attributeTypeName) {
        performStorageAction("deleteAttributeType("+attributeTypeName+")", storage -> {
            AuditLog auditLog = new AuditLog(System.currentTimeMillis(),"anonymous","Delete attribute", attributeTypeName,attributeTypeName,"");
            storage.deleteAttributeType(attributeTypeName);
            storage.persistAuditLog(auditLog);
            return null;        
        });
        LicenseCache.reloadCache(); // Database changed, so reload cache              
    }
     
    
    public static ArrayList<AttributeType> getAttributeTypes() {
        return performStorageAction("getAttributeTypes()", storage -> {
            return storage.getAttributeTypes();                    
        });                                       
    }
    
    
    
    
    public static ArrayList<License> getAllLicenseNames() {
        return performStorageAction("getAllLicenseNames()", storage -> {
            return storage.getAllLicenseNames();                    
        });            
        

    }
    
    public static License getLicense(long licenseId) {
        return performStorageAction("getLicense("+licenseId+")", storage -> {
            return storage.getLicense(licenseId);                    
        });           

            }
    
    /**
     * Starts a storage transaction and performs the given action on it, returning the result from the action.
     * <p>
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
     * @param <T> the object returned from the {@link StorageAction#process(LicenseModuleStorage)} method.
     */
    @FunctionalInterface
    private interface StorageAction<T> {
        /**
         * Access or modify the given storage inside of a transaction.
         * If the method throws an exception, it will be logged, a {@link LicenseModuleStorage#rollback()} will be performed and
         * a wrapping {@link dk.kb.util.webservice.exception.InternalServiceException} will be thrown.
         * @param storage a storage ready for requests and updates.
         * @return custom return value.
         * @throws Exception if something went wrong.
         */
        T process(LicenseModuleStorage storage) throws Exception;
    }
    
    
        
}
