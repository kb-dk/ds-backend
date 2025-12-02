package dk.kb.license.storage;

import dk.kb.license.config.ServiceConfig;
import dk.kb.license.model.v1.AuditLogEntryOutputDto;
import dk.kb.license.model.v1.ChangeTypeEnumDto;
import dk.kb.license.model.v1.ObjectTypeEnumDto;
import dk.kb.license.util.H2DbUtil;
import dk.kb.license.webservice.KBAuthorizationInterceptor;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.MessageImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.AccessToken;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

/**
 * Unittest class for the H2Storage.
 * All tests create and use H2 database in the directory: target/h2
 * The directory will be deleted before the first test-method is called.
 * Each test-method will delete all entries in the database, but keep the database tables.
 * Currently, the directory is not deleted after the tests have run. This is useful as you can
 * open and open the database and see what the unit-tests did.
 */
public class AuditLogModuleStorageTest extends UnitTestUtil {
    private static final Logger log = LoggerFactory.getLogger(AuditLogModuleStorageTest.class);

    protected static AuditLogModuleStorageForUnitTest storage = null;

    @BeforeAll
    public static void beforeClass() throws IOException, SQLException {
        ServiceConfig.initialize("conf/ds-license*.yaml");
        BaseModuleStorage.initialize(DRIVER, URL, USERNAME, PASSWORD);
        H2DbUtil.createEmptyH2DBFromDDL(URL, DRIVER, USERNAME, PASSWORD, List.of("ddl/audit_log_module_create_h2_unittest.ddl"));
        storage = new AuditLogModuleStorageForUnitTest();
    }

    /**
     * Delete all records between each unittest. The clearTableRecords is only defined on the unittest extension of the storage module
     * The facade class is responsible for committing transactions. So clean up between unittests.
     */
    @BeforeEach
    public void beforeEach() throws SQLException {
        List<String> tables = new ArrayList<>();
        tables.add("AUDITLOG");
        storage.clearTableRecords(tables);
    }

    @Test
    public void persistAuditLog_whenPersistAuditLogEntry_thenReturnAuditLogEntry() throws SQLException, IllegalArgumentException {
        // Arrange
        String userName = "mockedName";
        MessageImpl message = new MessageImpl();
        AccessToken mockedToken = Mockito.mock(AccessToken.class);
        Mockito.when(mockedToken.getName()).thenReturn(userName);
        message.put(KBAuthorizationInterceptor.ACCESS_TOKEN, mockedToken);

        try (MockedStatic<JAXRSUtils> mocked = mockStatic(JAXRSUtils.class)) {
            mocked.when(JAXRSUtils::getCurrentMessage).thenReturn(message);

            Long objectId = 123456789L;
            ChangeTypeEnumDto changeType = ChangeTypeEnumDto.UPDATE;
            ObjectTypeEnumDto changeName = ObjectTypeEnumDto.DR_PRODUCTION_ID;
            String identifier = "1234";
            String changeComment = "changeComment";
            String textBefore = "before";
            String textAfter = "after";

            AuditLogEntry auditLog = new AuditLogEntry(objectId, "", changeType, changeName, identifier, changeComment, textBefore, textAfter);

            // Act
            long auditLogId = storage.persistAuditLog(auditLog);

            // Assert
            AuditLogEntryOutputDto auditFromStorage = storage.getAuditLogById(auditLogId);

            assertEquals(auditLogId, auditFromStorage.getId());
            assertEquals(objectId, auditFromStorage.getObjectId());
            assertTrue(auditFromStorage.getModifiedTime() > 0); //modifiedtime has been set
            assertEquals(userName, auditFromStorage.getUserName());
            assertEquals(changeType, auditFromStorage.getChangeType());
            assertEquals(changeName, auditFromStorage.getChangeName());
            assertEquals(identifier, auditFromStorage.getIdentifier());
            assertEquals(changeComment, auditFromStorage.getChangeComment());
            assertEquals(textBefore, auditFromStorage.getTextBefore());
            assertEquals(textAfter, auditFromStorage.getTextAfter());
        }
    }
}
