package dk.kb.license.facade;

import java.sql.SQLException;
import java.util.ArrayList;

import javax.servlet.http.HttpSession;

import dk.kb.license.storage.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.license.util.ChangeDifferenceText;
import dk.kb.license.util.LicenseChangelogGenerator;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;

/**
 * The LicenseModuleFacade exposes all methods that can be called on a LicenceModule. This includes both persistence logic and business logic resolving licence access.
 * <p>
 * This facade class is also responsible for the transactional integrity of a storage. The storage model will never commit or rollback. All storage
 * transactional logic is controlled by this class. This makes it possible to use multiple storage methods as building blocks and rollback
 * everything if one of the steps fails.
 * <p>
 * The method {@link #performStorageAction} is used for all storage calls and responsible for commit or rollback.
 * @author Thomas Egense
 */

public class LicenseModuleFacade {

    private static final Logger log = LoggerFactory.getLogger(LicenseModuleFacade.class);
    
    /**
     * Create a new {@link PresentationType} which can then be added to licences. A new created PresentationType will
     * not be added the any existing licences. Licences can be edited and the new {@link PresentationType} can be added.
     *   
     * @param key Unique identifier for the {@link PresentationType} that is used when requesting access.
     *            Use alphanumeric values with no white spaces. Examples: Thumbnails, Search, Stream
     * @param value_dk This text (danish) will be shown to end users. Keep it short.
     * @param value_en This text (english) will be shown to end users. Keep it short.
     */
    public static void persistLicensePresentationType(String key, String value_dk, String value_en, HttpSession session) {
        
        BaseModuleStorage.performStorageAction("persistLicensePresentationType(" + key + ","+value_dk +","+value_en+")", storage -> {
        
            PresentationType newType = new PresentationType(0, key, value_dk, value_en);
            storage.persistLicensePresentationType(key, value_dk, value_en);
            ChangeDifferenceText changes = LicenseChangelogGenerator.getPresentationTypeChanges(null, newType);
            AuditLog auditLog = new AuditLog(System.currentTimeMillis(),(String) session.getAttribute("oauth_user"),"Create presentationtype", key, changes.getBefore(), changes.getAfter());
            storage.persistAuditLog(auditLog);
            
            return null;
        
        });
        LicenseCache.reloadCache(); // Database changed, so reload cache
    }

    /**
     * Get a list of all defined Presentation types.
     * <p>
     * This is only intended to be called from the admin GUI when creating or updating a license
     * to show all possible presentation types that can be added to the licence.
     * @return List of presentationtype DTO's
     */
    public static ArrayList<PresentationType> getLicensePresentationTypes() {
       return BaseModuleStorage.performStorageAction("persistLicensePresentationType()", storage -> {
            return storage.getLicensePresentationTypes();                   
        });
                
    }

    
    /**
     * Retrieve a specific audit log by id.
     * <p>
     * The id is milliseconds at the time for the change.
     * Call the getAllAuditLogs {@link #getAllAuditLogs()} to see all entries with their ids
     * @param millis The ID of the log. 
     * @return AuditLog The auditlog with this id
     */
    public static AuditLog getAuditLog(Long millis) {
        return BaseModuleStorage.performStorageAction("getAuditLog()", storage -> {
             return storage.getAuditLog(millis);
         });                
     }
     
    /**
     * Returns a list of all auditlogs.
     * <p>
     * @return List of all auditlogs.
     */    
    public static ArrayList<AuditLog> getAllAuditLogs() {
       return BaseModuleStorage.performStorageAction("getAllAuditLogs()", storage -> {
            return storage.getAllAudit();                   
        });                
    }
    
    
    
    /**
     * Delete a license completely.
     * Instead of deleting a license it is also an option to disable it by changing the valid-to attribute.
     * @param licenseId The unique id for the license. Instead of deleting a license, you can also change valid to/from for the license to disable it instead.
     */    
    public static void deleteLicense(long licenseId, HttpSession session) {
        BaseModuleStorage.performStorageAction("deleteLicense(" + licenseId + ")", storage -> {

            License license = storage.getLicense(licenseId);
            ChangeDifferenceText changes = LicenseChangelogGenerator.getLicenseChanges(license,null);              

            AuditLog auditLog = new AuditLog(System.currentTimeMillis(),(String) session.getAttribute("oauth_user"),"Delete License", license.getLicenseName(), changes.getBefore(), changes.getAfter());               
            
            storage.deleteLicense(licenseId);
            storage.persistAuditLog(auditLog);
            return null;
        
        });
        LicenseCache.reloadCache(); // Database changed, so reload cache     
    }

    
    /**

     * Create a new {@link GroupType}. The new GroupType will not be attached to any existing licenses.
     * <p>
     * A group type has to be one of the two types:  access-giving(pakke) or restriction(klausulering).
     * The group type must define a solr query string. In case of access-giving(pakke) the query will include
     * additional content. In case of a restriction, the query will remove content unless the user has a license
     * which removes the restriction.
     * <p>
     * After creation of the GroupType, it is not added to any licences,
     * but can be used when creating or editing an existing license.
     *  @param key This is the name seen from the license page of the GUI when selecting among {@link GroupType}s.
     *              Example:  'Access to DR-arkiv'
     *  @param value Small description in danish. Often this description equals the key-param.
     *  @param value_en Small description in english. Often this description equals the key-param.
     *  @param description A longer and more detailed description in danish
     *                     which will only be shown on the create/edit page for GroupTypes.
     *  @param description_en A longer and more detailed description in english
     *                        which will only be shown on the create/edit page for GroupTypes.
     *  @param query A solr query which defines the material covered by this {@link GroupType} at access-giving or
     *              restriction. Make sure the query is correct and has all parentheses matched.
     *  @param isRestriction if false the type is access-giving(pakke). If true it will be a restriction(klausulering).
     */
    public static void persistLicenseGroupType(String key, String value, String value_en, String description, String description_en, String query, boolean isRestriction,HttpSession session) {
 
        BaseModuleStorage.performStorageAction("persistLicenseGroupType(" + key+","+value+","+value_en +","+description +","+description_en +","+query+","+ isRestriction+")", storage -> {
        GroupType g = new GroupType(0L,key,value,value_en,description,description_en,query, isRestriction);

        ChangeDifferenceText changes = LicenseChangelogGenerator.getGroupTypeChanges(null, g);
        AuditLog auditLog = new AuditLog(System.currentTimeMillis(),(String) session.getAttribute("oauth_user"),"Create grouptype", key, changes.getBefore(), changes.getAfter());        
        storage.persistLicenseGroupType(key, value, value_en, description, description_en, query,  isRestriction);        
        storage.persistAuditLog(auditLog);
        return null;
        
        });
        LicenseCache.reloadCache(); // Database changed, so reload cache        
    }


       
    /**
     * Update an existing {@link GroupType}.
     * If query is changed it will have influence on all licenses giving access with this group.
     * @param id The unique ID for the grouptype.
     * @param value_dk Name (Danish)
     * @param value_en Name (English)
     * @param description Description (danish)
     * @param description_en Description (english)
     * @param query A solr query which defines the material covered by this {@link GroupType} at access-giving or
     *              restriction. Make sure the query is correct and has all parentheses matched. Example: collection:dr
     * @param isRestriction Is package (includes) or restriction(excludes)  
     */

    public static void updateLicenseGroupType(long id, String value_dk, String value_en, String description, String description_en, String query, boolean  isRestriction,HttpSession session) {
      
        BaseModuleStorage.performStorageAction("updateLicenseGroupType(" + id+","+value_dk+","+value_en +","+description +","+description_en +","+query+","+ isRestriction+")", storage -> {
           GroupType oldGroupType = storage.getGroupTypeById(id);
            
            GroupType updateGroupType = new GroupType(0L,oldGroupType.getKey(),value_dk,value_en,description,description_en,query, isRestriction);
            ChangeDifferenceText changes = LicenseChangelogGenerator.getGroupTypeChanges(oldGroupType, updateGroupType);
            AuditLog auditLog = new AuditLog(System.currentTimeMillis(),(String) session.getAttribute("oauth_user"),"Update grouptype", value_dk, changes.getBefore(), changes.getAfter());        
                      
            storage.updateLicenseGroupType(id, value_dk, value_en, description, description_en, query,  isRestriction);
            storage.persistAuditLog(auditLog);
            return null;
        
        });
        LicenseCache.reloadCache(); // Database changed, so reload cache       
    }


    /**
     * Update a {@link PresentationType}. It is possible to update a PresentationType which is already used by licences.
     * It is not possible to change the 'key' field, only the descriptions.
     * @param id the for presentationtype
     * @param value_dk the danish short description
     * @param value_en the english short description
     */
    public static void updatePresentationType(long id, String value_dk, String value_en,HttpSession session) {
       
        BaseModuleStorage.performStorageAction("updateLicenseGroupType(" + id+","+value_dk+","+value_en +")", storage -> {

           PresentationType oldType = storage.getPresentationTypeById(id);
           PresentationType newType = new PresentationType(id,oldType.getKey(),value_dk,value_en);
           storage.updatePresentationType(id, value_dk, value_en);

           ChangeDifferenceText changes = LicenseChangelogGenerator.getPresentationTypeChanges(oldType, newType);
           AuditLog auditLog = new AuditLog(System.currentTimeMillis(),(String) session.getAttribute("oauth_user"),"Update presentationtype", oldType.getKey(), changes.getBefore(), changes.getAfter());
           storage.persistAuditLog(auditLog);
           return null;        
        });
        LicenseCache.reloadCache(); // Database changed, so reload cache
    }
    
    
    /**
     * Delete a {@link GroupType}. If a group type is used by any license, it can not be deleted.
     * @param groupName The unique name of the grouptype
     */   
    public static void deleteLicenseGroupType(String groupName,HttpSession session) {
   
        BaseModuleStorage.performStorageAction("deleteLicenseGroupType(" + groupName +")", storage -> {
           AuditLog auditLog = new AuditLog(System.currentTimeMillis(),(String) session.getAttribute("oauth_user"),"Delete grouptype",groupName,groupName,"");
           storage.persistAuditLog(auditLog);
            storage.deleteLicenseGroupType(groupName);
            return null;        
        });
        LicenseCache.reloadCache(); // Database changed, so reload cache  
    }
    
    

    /**
     * Delete a {@link PresentationType}.
     * It is not possible to delete a Presentation type that is being used by any licenses.
     * Licences using a PresentationType must remove them before the PresentationType can be deleted.
     * @param presentationName The given identifier for the PresentationType to delete.
     */
    public static void deletePresentationType(String presentationName,HttpSession session) {
        BaseModuleStorage.performStorageAction("deletePresentationType(" + presentationName +")", storage -> {
            PresentationType oldType = storage.getPresentationTypeByKey(presentationName);
            storage.deletePresentationType(presentationName);

           ChangeDifferenceText changes = LicenseChangelogGenerator.getPresentationTypeChanges(oldType, null);
           AuditLog auditLog = new AuditLog(System.currentTimeMillis(),(String) session.getAttribute("oauth_user"),"Delete presentationtype", oldType.getKey(), changes.getBefore(), changes.getAfter());                   
           storage.persistAuditLog(auditLog);            
            return null;        
        });
        LicenseCache.reloadCache(); // Database changed, so reload cache  
    }
        
      

    /**
     * Create or update a license.  
     * If license id=0 a new license will be created. Else it will update the license with the id
     * @param license A licenseDTO having all information about date to/from, PresentationTypes,
     *                attribute groups and GroupTypes.
     */    
    public static void persistLicense(License license,HttpSession session) {
        
        BaseModuleStorage.performStorageAction("persistLicense(description_dk=" + license.getDescription_dk() +")", storage -> {
            AuditLog auditLog = null;
            //audit log
            if (license.getId() == 0 ) {
               ChangeDifferenceText changes = LicenseChangelogGenerator.getLicenseChanges(null,license);              
               auditLog = new AuditLog(System.currentTimeMillis(),(String) session.getAttribute("oauth_user"),"Create New License", license.getLicenseName(), changes.getBefore(), changes.getAfter());               

            }
            else {
               License oldLicense = storage.getLicense(license.getId());
               ChangeDifferenceText changes = LicenseChangelogGenerator.getLicenseChanges(oldLicense, license);
               auditLog = new AuditLog(System.currentTimeMillis(),(String) session.getAttribute("oauth_user"),"Update License", license.getLicenseName(), changes.getBefore(), changes.getAfter());                                               
            }
            
            storage.persistAuditLog(auditLog);            
            storage.persistLicense(license);
            return null;        
        });
        LicenseCache.reloadCache(); // Database changed, so reload cache        
    }
    

    /**
     * Get a list of all defined {@link GroupType}s.
     * @return List of all GroupTypes define in this instance of LicenseModule
     */
    public static ArrayList<GroupType> getLicenseGroupTypes() {
   
        return BaseModuleStorage.performStorageAction("getLicenseGroupTypes()", storage -> {
            return storage.getLicenseGroupTypes();                    
        });                               
    }
    
    /**
     * Persist a new attribute name that can be used by licenses to identify users.
     * @param attributeTypeName The new attribute. Example: wayf.mail
     */
    public static void persistAttributeType(String attributeTypeName,HttpSession session) {
        BaseModuleStorage.performStorageAction("persistAttributeType("+attributeTypeName+")", storage -> {
            AuditLog auditLog = new AuditLog(System.currentTimeMillis(),(String) session.getAttribute("oauth_user"),"Create attribute", attributeTypeName,"",attributeTypeName);
            storage.persistAttributeType(attributeTypeName);
            storage.persistAuditLog(auditLog);
            return null;        
        });
        LicenseCache.reloadCache(); // Database changed, so reload cache        
    }
    
    

    /**
     * Delete an {@link AttributeType}.
     * 
     * @param attributeTypeName The unique name of the AttributeType.
     *                          If the AttributeType is actively used by any licenses, it can not be deleted.
     */    
    public static void deleteAttributeType(String attributeTypeName,HttpSession session) {
        BaseModuleStorage.performStorageAction("deleteAttributeType("+attributeTypeName+")", storage -> {
            AuditLog auditLog = new AuditLog(System.currentTimeMillis(),(String) session.getAttribute("oauth_user"),"Delete attribute", attributeTypeName,attributeTypeName,"");
            storage.deleteAttributeType(attributeTypeName);
            storage.persistAuditLog(auditLog);
            return null;        
        });
        LicenseCache.reloadCache(); // Database changed, so reload cache              
    }
     
    /**
    * Retrieve a list of all defined {@link AttributeType}s.
    * @return List of all AttributeTypes.
    */

    public static ArrayList<AttributeType> getAttributeTypes() {
        return BaseModuleStorage.performStorageAction("getAttributeTypes()", storage -> {
            return storage.getAttributeTypes();                    
        });                                       
    }
    

    /**
     * Retrieve a list of all licenses for overview.
     * The licenses will only have the name field and valid to/from date loaded.
     * @return List of licenses that will have name and valid date to/from loaded.
     */   
    public static ArrayList<License> getAllLicenseNames() {
        return BaseModuleStorage.performStorageAction("getAllLicenseNames()", storage -> {
            return storage.getAllLicenseNames();                    
        });                    
    }
    

    
    /**
     * Get a specific license.
     * @param licenseId The unique id of the license.
     * @return License The unique license with this id. 
     */
    public static License getLicense(long licenseId) {
        return BaseModuleStorage.performStorageAction("getLicense("+licenseId+")", storage -> {
            return storage.getLicense(licenseId);                    
        });           
    }
}
