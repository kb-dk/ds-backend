package dk.kb.license.facade;

import dk.kb.license.model.v1.AuditLogEntryOutputDto;
import dk.kb.license.model.v1.ObjectTypeEnumDto;
import dk.kb.license.storage.AuditLogEntry;
import dk.kb.license.storage.AuditLogModuleStorage;
import dk.kb.license.storage.BaseModuleStorage;

import java.util.ArrayList;
import java.util.List;

public class AuditLogModuleFacade {

    /**
     * Retrieves a {@link AuditLogEntry} by id
     * This method does not have an immediate use case.
     *
     * @param auditLogId The ID of the log.
     * @return the AuditLogEntry with the given id
     */
    public static AuditLogEntryOutputDto getAuditEntryById(Long auditLogId) {
        return BaseModuleStorage.performStorageAction("getAuditEntryById()", AuditLogModuleStorage.class, storage -> {
            return ((AuditLogModuleStorage) storage).getAuditLogById(auditLogId);
        });
    }

    /**
     * Retrieves List of {@link AuditLogEntry} . Maximum elements is 100. Use modifiedTimeStart for pagination.
     * AuditLogEntries in the list are sorted by modifiedTimeStart desc, so latest will come first in the list.     
     *      
     * @param modifiedTimeStart Will extract AuditLogEntries with modifiedTimeStart less than this value
     * @param changeName list only AuditLogEntries of this type. Will list all types if type is null.
     * 
     * @return List<AuditLogEntryOutputDto> with a maximum of 100 elements in the list. 
     */
    public static List<AuditLogEntryOutputDto> getAuditLogOlderThanModifiedTime(Long modifiedTimeStart, ObjectTypeEnumDto changeName) {                                  
        return BaseModuleStorage.performStorageAction("getAuditLogOlderThanModifiedTime()", AuditLogModuleStorage.class, storage -> {
         if (changeName == null) {
             return ((AuditLogModuleStorage) storage).getAuditLogOlderThanModifiedTimeListAll(modifiedTimeStart);     
         }
         else {
             return ((AuditLogModuleStorage) storage).getAuditLogOlderThanModifiedTimeListByType(modifiedTimeStart,changeName);
         }        
        });
    }
    
    /**
     * Retrieves a list of all {@link AuditLogEntry} related to a given object.
     *
     * @param objectId The ID of the object for which to retrieve audit entries.
     * @return A list of AuditLogEntry related to
     */
    public static List<AuditLogEntryOutputDto> getAuditEntriesByObjectId(Long objectId) {
        return BaseModuleStorage.performStorageAction("getAuditLogByObjectId()", AuditLogModuleStorage.class, storage -> {
            return ((AuditLogModuleStorage) storage).getAuditLogByObjectId(objectId);
        });
    }

    /**
     * Get a list of all {@link AuditLogEntry}.
     *
     * @return List of all audit log entries
     */
    public static ArrayList<AuditLogEntryOutputDto> getAllAuditLogs() {
        return BaseModuleStorage.performStorageAction("getAllAuditLogs()", AuditLogModuleStorage.class, storage -> {
            return ((AuditLogModuleStorage) storage).getAllAudit();
        });
    }
}