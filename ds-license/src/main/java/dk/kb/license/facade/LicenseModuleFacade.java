package dk.kb.license.facade;

import java.util.ArrayList;

import javax.servlet.http.HttpSession;

import dk.kb.license.model.v1.AuditEntryOutputDto;
import dk.kb.license.model.v1.ChangeTypeEnumDto;
import dk.kb.license.model.v1.ObjectTypeEnumDto;
import dk.kb.license.storage.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.license.util.ChangeDifferenceText;
import dk.kb.license.util.LicenseChangelogGenerator;

/**
 * The LicenseModuleFacade exposes all methods that can be called on a LicenceModule. This includes both persistence logic and business logic resolving licence access.
 * <p>
 * This facade class is also responsible for the transactional integrity of a storage. The storage model will never commit or rollback. All storage
 * transactional logic is controlled by this class. This makes it possible to use multiple storage methods as building blocks and rollback
 * everything if one of the steps fails.

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



        BaseModuleStorage.performStorageAction("persistLicensePresentationType(" + key + ","+value_dk +","+value_en+")", LicenseModuleStorage.class, storage -> {

            PresentationType newType = new PresentationType(0, key, value_dk, value_en);
            ((LicenseModuleStorage) storage).persistLicensePresentationType(key, value_dk, value_en);
            ChangeDifferenceText changes = LicenseChangelogGenerator.getPresentationTypeChanges(null, newType);
            
            AuditLogEntry auditLog = new AuditLogEntry(newType.getId(),(String) session.getAttribute("oauth_user"), ChangeTypeEnumDto.CREATE,  ObjectTypeEnumDto.PRESENTATION_TYPE, "", changes.getBefore(), changes.getAfter());

            ((LicenseModuleStorage) storage).persistAuditLog(auditLog);

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
       return BaseModuleStorage.performStorageAction("persistLicensePresentationType()", LicenseModuleStorage.class, storage -> {
            return ((LicenseModuleStorage) storage).getLicensePresentationTypes();
        });
                
    }

    
    /**
     * Delete a license completely.
     * Instead of deleting a license it is also an option to disable it by changing the valid-to attribute.
     * @param licenseId The unique id for the license. Instead of deleting a license, you can also change valid to/from for the license to disable it instead.
     */    
    public static void deleteLicense(long licenseId, HttpSession session) {
        BaseModuleStorage.performStorageAction("deleteLicense(" + licenseId + ")", LicenseModuleStorage.class, storage -> {

            License license = ((LicenseModuleStorage) storage).getLicense(licenseId);
            ChangeDifferenceText changes = LicenseChangelogGenerator.getLicenseChanges(license,null);              

            AuditLogEntry auditLog = new AuditLogEntry(licenseId,(String) session.getAttribute("oauth_user"),ChangeTypeEnumDto.DELETE, ObjectTypeEnumDto.LICENSE,"", changes.getBefore(), changes.getAfter());

            ((LicenseModuleStorage) storage).deleteLicense(licenseId);
            ((LicenseModuleStorage) storage).persistAuditLog(auditLog);
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
 
        BaseModuleStorage.performStorageAction("persistLicenseGroupType(" + key+","+value+","+value_en +","+description +","+description_en +","+query+","+ isRestriction+")", LicenseModuleStorage.class, storage -> {
        GroupType g = new GroupType(0L,key,value,value_en,description,description_en,query, isRestriction);

        ChangeDifferenceText changes = LicenseChangelogGenerator.getGroupTypeChanges(null, g);
        AuditLogEntry auditLog = new AuditLogEntry(g.getId(), (String) session.getAttribute("oauth_user"),ChangeTypeEnumDto.CREATE, ObjectTypeEnumDto.GROUP_TYPE, "", changes.getBefore(), changes.getAfter());
        ((LicenseModuleStorage) storage).persistLicenseGroupType(key, value, value_en, description, description_en, query,  isRestriction);
        ((LicenseModuleStorage) storage).persistAuditLog(auditLog);
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
      
        BaseModuleStorage.performStorageAction("updateLicenseGroupType(" + id+","+value_dk+","+value_en +","+description +","+description_en +","+query+","+ isRestriction+")", LicenseModuleStorage.class, storage -> {
           GroupType oldGroupType = ((LicenseModuleStorage) storage).getGroupTypeById(id);
            
            GroupType updateGroupType = new GroupType(0L,oldGroupType.getKey(),value_dk,value_en,description,description_en,query, isRestriction);
            ChangeDifferenceText changes = LicenseChangelogGenerator.getGroupTypeChanges(oldGroupType, updateGroupType);
            AuditLogEntry auditLog = new AuditLogEntry(id, (String) session.getAttribute("oauth_user"),ChangeTypeEnumDto.UPDATE, ObjectTypeEnumDto.GROUP_TYPE, "", changes.getBefore(), changes.getAfter());
                      
            ((LicenseModuleStorage) storage).updateLicenseGroupType(id, value_dk, value_en, description, description_en, query,  isRestriction);
            ((LicenseModuleStorage) storage).persistAuditLog(auditLog);
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
       
        BaseModuleStorage.performStorageAction("updateLicenseGroupType(" + id+","+value_dk+","+value_en +")", LicenseModuleStorage.class, storage -> {

           PresentationType oldType = ((LicenseModuleStorage) storage).getPresentationTypeById(id);
           PresentationType newType = new PresentationType(id,oldType.getKey(),value_dk,value_en);
           ((LicenseModuleStorage) storage).updatePresentationType(id, value_dk, value_en);

           ChangeDifferenceText changes = LicenseChangelogGenerator.getPresentationTypeChanges(oldType, newType);
           AuditLogEntry auditLog = new AuditLogEntry(id,(String) session.getAttribute("oauth_user"), ChangeTypeEnumDto.UPDATE,  ObjectTypeEnumDto.PRESENTATION_TYPE, "", changes.getBefore(), changes.getAfter());
           ((LicenseModuleStorage) storage).persistAuditLog(auditLog);
           return null;        
        });
        LicenseCache.reloadCache(); // Database changed, so reload cache
    }
    
    
    /**
     * Delete a {@link GroupType}. If a group type is used by any license, it can not be deleted.
     * @param groupName The unique name of the grouptype
     */   
    public static void deleteLicenseGroupType(String groupName,HttpSession session) {

        BaseModuleStorage.performStorageAction("deleteLicenseGroupType(" + groupName +")", LicenseModuleStorage.class, storage -> {
            long id = 0;
            ChangeDifferenceText changes;
            ArrayList<GroupType> groups = ((LicenseModuleStorage) storage).getLicenseGroupTypes();
            for (GroupType group : groups) {
                if (group.getKey().equals(groupName)) {
                    id = group.getId();
                    changes = LicenseChangelogGenerator.getGroupTypeChanges(group, null);
                    break;
                }
            }

            AuditLogEntry auditLog = new AuditLogEntry(id, (String) session.getAttribute("oauth_user"),ChangeTypeEnumDto.DELETE, ObjectTypeEnumDto.GROUP_TYPE, "", "", "");
            ((LicenseModuleStorage) storage).persistAuditLog(auditLog);
            ((LicenseModuleStorage) storage).deleteLicenseGroupType(groupName);
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
        BaseModuleStorage.performStorageAction("deletePresentationType(" + presentationName +")", LicenseModuleStorage.class, storage -> {
            PresentationType oldType = ((LicenseModuleStorage) storage).getPresentationTypeByKey(presentationName);
            ((LicenseModuleStorage) storage).deletePresentationType(presentationName);

           ChangeDifferenceText changes = LicenseChangelogGenerator.getPresentationTypeChanges(oldType, null);
           AuditLogEntry auditLog = new AuditLogEntry(oldType.getId(),(String) session.getAttribute("oauth_user"), ChangeTypeEnumDto.DELETE,  ObjectTypeEnumDto.PRESENTATION_TYPE, "", changes.getBefore(), changes.getAfter());
           ((LicenseModuleStorage) storage).persistAuditLog(auditLog);
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
    public static long persistLicense(License license,HttpSession session) {
        
        BaseModuleStorage.performStorageAction("persistLicense(description_dk=" + license.getDescription_dk() +")", LicenseModuleStorage.class, storage -> {
            AuditLogEntry auditLog = null;
            //audit log
            if (license.getId() == 0 ) {
               ChangeDifferenceText changes = LicenseChangelogGenerator.getLicenseChanges(null,license);              
               auditLog = new AuditLogEntry(0,(String) session.getAttribute("oauth_user"),ChangeTypeEnumDto.CREATE, ObjectTypeEnumDto.LICENSE,"", changes.getBefore(), changes.getAfter());

            }
            else {
               License oldLicense = ((LicenseModuleStorage) storage).getLicense(license.getId());
               ChangeDifferenceText changes = LicenseChangelogGenerator.getLicenseChanges(oldLicense, license);
               auditLog = new AuditLogEntry(System.currentTimeMillis(),(String) session.getAttribute("oauth_user"),"Update License", license.getLicenseName(), changes.getBefore(), changes.getAfter());
            }
            
            ((LicenseModuleStorage) storage).persistAuditLog(auditLog);
            ((LicenseModuleStorage) storage).persistLicense(license);
            return null;        
        });
        LicenseCache.reloadCache(); // Database changed, so reload cache        
    }
    

    /**
     * Get a list of all defined {@link GroupType}s.
     * @return List of all GroupTypes define in this instance of LicenseModule
     */
    public static ArrayList<GroupType> getLicenseGroupTypes() {
   
        return BaseModuleStorage.performStorageAction("getLicenseGroupTypes()", LicenseModuleStorage.class, storage -> {
            return ((LicenseModuleStorage) storage).getLicenseGroupTypes();
        });                               
    }
    
    /**
     * Persist a new attribute name that can be used by licenses to identify users.
     * @param attributeTypeName The new attribute. Example: wayf.mail
     */
    public static void persistAttributeType(String attributeTypeName,HttpSession session) {
        BaseModuleStorage.performStorageAction("persistAttributeType("+attributeTypeName+")", LicenseModuleStorage.class, storage -> {
            AuditLogEntry auditLog = new AuditLogEntry(System.currentTimeMillis(),(String) session.getAttribute("oauth_user"),"Create attribute", attributeTypeName,"",attributeTypeName);
            ((LicenseModuleStorage) storage).persistAttributeType(attributeTypeName);
            ((LicenseModuleStorage) storage).persistAuditLog(auditLog);
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
        BaseModuleStorage.performStorageAction("deleteAttributeType("+attributeTypeName+")", LicenseModuleStorage.class, storage -> {
            AuditLogEntry auditLog = new AuditLogEntry(System.currentTimeMillis(),(String) session.getAttribute("oauth_user"),"Delete attribute", attributeTypeName,attributeTypeName,"");
            ((LicenseModuleStorage) storage).deleteAttributeType(attributeTypeName);
            ((LicenseModuleStorage) storage).persistAuditLog(auditLog);
            return null;        
        });
        LicenseCache.reloadCache(); // Database changed, so reload cache              
    }
     
    /**
    * Retrieve a list of all defined {@link AttributeType}s.
    * @return List of all AttributeTypes.
    */

    public static ArrayList<AttributeType> getAttributeTypes() {
        return BaseModuleStorage.performStorageAction("getAttributeTypes()", LicenseModuleStorage.class, storage -> {
            return ((LicenseModuleStorage) storage).getAttributeTypes();
        });                                       
    }
    

    /**
     * Retrieve a list of all licenses for overview.
     * The licenses will only have the name field and valid to/from date loaded.
     * @return List of licenses that will have name and valid date to/from loaded.
     */   
    public static ArrayList<License> getAllLicenseNames() {
        return BaseModuleStorage.performStorageAction("getAllLicenseNames()", LicenseModuleStorage.class, storage -> {
            return ((LicenseModuleStorage) storage).getAllLicenseNames();
        });                    
    }
    

    
    /**
     * Get a specific license.
     * @param licenseId The unique id of the license.
     * @return License The unique license with this id. 
     */
    public static License getLicense(long licenseId) {
        return BaseModuleStorage.performStorageAction("getLicense("+licenseId+")", LicenseModuleStorage.class, storage -> {
            return ((LicenseModuleStorage) storage).getLicense(licenseId);
        });           
    }
}
