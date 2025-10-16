package dk.kb.license.facade;

import java.util.ArrayList;
import java.util.List;

import dk.kb.license.model.v1.AuditLogEntryOutputDto;
import dk.kb.license.storage.*;

public class AuditLogModuleFacade {

    /**
     * Retrieves a {@link AuditLogEntry} by id
     * This method does not have an immediate usecase.
     *
     * @param auditLogId The ID of the log.
     * @return the AuditLogEntry with the given id
     */
    public static AuditLogEntryOutputDto getAuditEntryById(Long auditLogId) {
        return BaseModuleStorage.performStorageAction("getAuditEntries()", AuditLogModuleStorage.class, storage -> {
             return ((AuditLogModuleStorage) storage).getAuditLogById(auditLogId);
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
     * Get a list of all defined {@link GroupType}s.
     * @return List of all GroupTypes define in this instance of LicenseModule
     */
    public static ArrayList<AuditLogEntryOutputDto> getAllAuditLogs() {
        return BaseModuleStorage.performStorageAction("getAllAuditLogs()", AuditLogModuleStorage.class, storage -> {
            return ((AuditLogModuleStorage) storage).getAllAudit();
        });
    }
}