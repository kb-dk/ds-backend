package dk.kb.license.facade;

import java.sql.SQLException;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.license.storage.ConfiguredAttributeType;
import dk.kb.license.storage.ConfiguredLicenseGroupType;
import dk.kb.license.storage.ConfiguredLicensePresentationType;
import dk.kb.license.storage.License;
import dk.kb.license.storage.LicenseCache;
import dk.kb.license.storage.LicenseModuleStorage;
import dk.kb.license.webservice.exception.InternalServiceException;
import dk.kb.license.webservice.exception.InvalidArgumentServiceException;



public class LicenseModuleFacade {

    private static final Logger log = LoggerFactory.getLogger(LicenseModuleFacade.class);

    public static void persistDomLicensePresentationType(String key, String value_dk, String value_en) throws Exception {    
         performStorageAction("persistDomLicensePresentationType(" + key + ","+value_dk +","+value_en+")", storage -> {
            storage.persistDomLicensePresentationType(key, value_dk, value_en);
            return null;
        
        });
        LicenseCache.reloadCache(); // Database changed, so reload cache
    }

    public static ArrayList<ConfiguredLicensePresentationType> getDomLicensePresentationTypes() throws Exception {
       return performStorageAction("persistDomLicensePresentationType()", storage -> {
            return storage.getDomLicensePresentationTypes();                   
        });
                
    }

    public static void deleteLicense(long licenseId) throws Exception { 
        performStorageAction("deleteLicense(" + licenseId + ")", storage -> {
            storage.deleteLicense(licenseId);
            return null;
        
        });
        LicenseCache.reloadCache(); // Database changed, so reload cache
     
    }

    public static void persistDomLicenseGroupType(String key, String value, String value_en, String description, String description_en, String query, boolean mustGroup) throws Exception {
 
        performStorageAction("persistDomLicenseGroupType(" + key+","+value+","+value_en +","+description +","+description_en +","+query+","+mustGroup+")", storage -> {
            storage.persistDomLicenseGroupType(key, value, value_en, description, description_en, query, mustGroup);
            return null;
        
        });
        LicenseCache.reloadCache(); // Database changed, so reload cache
        
    }

    public static void updateDomLicenseGroupType(long id, String value_dk, String value_en, String description, String description_en, String query, boolean mustGroup) throws Exception {
      
        performStorageAction("updateDomLicenseGroupType(" + id+","+value_dk+","+value_en +","+description +","+description_en +","+query+","+mustGroup+")", storage -> {
            storage.updateDomLicenseGroupType(id, value_dk, value_en, description, description_en, query, mustGroup);
            return null;
        
        });
        LicenseCache.reloadCache(); // Database changed, so reload cache
        
       

    }

    public static void updateDomPresentationType(long id, String value_dk, String value_en) throws Exception {
       
        performStorageAction("updateDomLicenseGroupType(" + id+","+value_dk+","+value_en +")", storage -> {
            storage.updateDomPresentationType(id, value_dk, value_en);
            return null;        
        });
        LicenseCache.reloadCache(); // Database changed, so reload cache

    }
    
    public static void deleteDomLicenseGroupType(String groupName) throws Exception {
   
        performStorageAction("deleteDomLicenseGroupType(" + groupName +")", storage -> {
            storage.deleteDomLicenseGroupType(groupName);
            return null;        
        });
        LicenseCache.reloadCache(); // Database changed, so reload cache  
    }
    
    
    public static void deleteDomPresentationType(String presentationName) throws Exception {
        performStorageAction("deleteDomPresentationType(" + presentationName +")", storage -> {
            storage.deleteDomPresentationType(presentationName);
            return null;        
        });
        LicenseCache.reloadCache(); // Database changed, so reload cache  
    }
        
      

    
    
    public static void persistLicense(License license)  throws Exception {
      
        performStorageAction("persistLicense(description_dk=" + license.getDescription_dk() +")", storage -> {
            storage.persistLicense(license);
            return null;        
        });
        LicenseCache.reloadCache(); // Database changed, so reload cache        
    }
    

    
    public static ArrayList<ConfiguredLicenseGroupType> getDomLicenseGroupTypes() throws Exception {
   
        return performStorageAction("getDomLicenseGroupTypes()", storage -> {
            return storage.getDomLicenseGroupTypes();
                    
        });                        
       
    }
    
    public static void persistDomAttributeType(String value) throws Exception {
        performStorageAction("persistDomAttributeType("+value+")", storage -> {
            storage.persistDomAttributeType(value);
            return null;        
        });
        LicenseCache.reloadCache(); // Database changed, so reload cache        
    }
    
    public static void deleteDomAttributeType(String attributeTypeName) throws Exception {
     
        performStorageAction(" deleteDomAttributeType("+attributeTypeName+")", storage -> {
            storage.deleteDomAttributeType(attributeTypeName);
            return null;        
        });
        LicenseCache.reloadCache(); // Database changed, so reload cache        
       

    }
     
    
    public static ArrayList<ConfiguredAttributeType> getDomAttributeTypes() throws Exception {
        return performStorageAction("getDomAttributeTypes()", storage -> {
            return storage.getDomAttributeTypes();                    
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
