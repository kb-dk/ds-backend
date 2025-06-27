package dk.kb.license.facade;

import java.util.ArrayList;

import dk.kb.license.model.v1.AuditEntryOutputDto;
import dk.kb.license.storage.BaseModuleStorage;
import dk.kb.license.storage.LicenseModuleStorage;


public class AuditFacade {

    
    /**
     * Retrieve a specific audit log by id.
     * <p>
     * @param millis The ID of the log. 
     * @return AuditLog The auditlog with this id
     */
    public static AuditEntryOutputDto getAuditEntry(Long auditLogId) {
        return BaseModuleStorage.performStorageAction("getAuditEntries()", LicenseModuleStorage.class, storage -> {
             return ((LicenseModuleStorage) storage).getAuditLogById(auditLogId);
         });                
     }
     

    
    /**
     * A list of audit entries for a given object id
     * <p>
     * @param objectId The ID of the object to retrieve audit entries for. 
     * @return AuditLog The auditlog with this id
     */
    public static ArrayList<AuditEntryOutputDto> getAuditEntries(Long objectId) {
        return BaseModuleStorage.performStorageAction("getAuditLogByObjectId()", LicenseModuleStorage.class, storage -> {
             return ((LicenseModuleStorage) storage).getAuditLogByObjectId(objectId);
         });                
     }

    

}
