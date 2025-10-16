package dk.kb.license.facade;

import dk.kb.license.config.ServiceConfig;
import dk.kb.license.model.v1.AuditLogEntryOutputDto;
import dk.kb.license.model.v1.ChangeTypeEnumDto;
import dk.kb.license.model.v1.ObjectTypeEnumDto;
import dk.kb.license.storage.*;
import dk.kb.license.util.H2DbUtil;
import dk.kb.license.webservice.KBAuthorizationInterceptor;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.MessageImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.AccessToken;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

public class LicenseModuleFacadeTest extends UnitTestUtil {
    protected static LicenseModuleStorageForUnitTest storage = null;
    private static final Logger log = LoggerFactory.getLogger(LicenseModuleFacadeTest.class);

    @BeforeAll
    public static void beforeClass() throws IOException, SQLException {

        ServiceConfig.initialize("conf/ds-license*.yaml", "ds-license-integration-test.yaml");
        BaseModuleStorage.initialize(DRIVER, URL, USERNAME, PASSWORD);

        H2DbUtil.createEmptyH2DBFromDDL(URL, DRIVER, USERNAME, PASSWORD, List.of("ddl/licensemodule_create_h2_unittest.ddl", "ddl/audit_log_module_create_h2_unittest.ddl"));
        storage = new LicenseModuleStorageForUnitTest();
    }

    @Test
    public void testAuditLog() throws SQLException {
        HttpSession mockedSession = Mockito.mock(HttpSession.class);
        Mockito.when(mockedSession.getAttribute("oauth_user")).thenReturn("mockedName");

        MessageImpl message = new MessageImpl();
        AccessToken mockedToken = Mockito.mock(AccessToken.class);
        Mockito.when(mockedToken.getName()).thenReturn("mockedName");
        message.put(KBAuthorizationInterceptor.ACCESS_TOKEN, mockedToken);

        try (MockedStatic<JAXRSUtils> mocked = mockStatic(JAXRSUtils.class)) {
            mocked.when(JAXRSUtils::getCurrentMessage).thenReturn(message);

            String key = "keyAuditTest";
            String value = "unit_test_value";
            String valueEnglish = "unit_test_value_en";
            String valueUpdated = "unit_test_value_updated";
            String valueEnglishUpdated = "unit_test_value_en_updated";

            long presentationTypeId = LicenseModuleFacade.persistLicensePresentationType(key, value, valueEnglish, mockedSession);
            LicenseModuleFacade.updatePresentationType(presentationTypeId, valueUpdated, valueEnglishUpdated, mockedSession);
            LicenseModuleFacade.deletePresentationType(key, mockedSession);

            ArrayList<AuditLogEntryOutputDto> auditLogEntriesForObject = storage.getAuditLogByObjectId(presentationTypeId);

            assertEquals(3, auditLogEntriesForObject.size());
            AuditLogEntryOutputDto createAuditLog = auditLogEntriesForObject.get(2);
            AuditLogEntryOutputDto updateAuditLog = auditLogEntriesForObject.get(1);
            AuditLogEntryOutputDto deleteAuditLog = auditLogEntriesForObject.get(0);

            assertEquals(presentationTypeId, createAuditLog.getObjectId());
            assertEquals(presentationTypeId, updateAuditLog.getObjectId());
            assertEquals(presentationTypeId, deleteAuditLog.getObjectId());

            assertTrue(createAuditLog.getModifiedTime() < updateAuditLog.getModifiedTime());
            assertTrue(updateAuditLog.getModifiedTime() < deleteAuditLog.getModifiedTime());

            assertEquals("mockedName", createAuditLog.getUserName());
            assertEquals("mockedName", updateAuditLog.getUserName());
            assertEquals("mockedName", deleteAuditLog.getUserName());

            assertEquals(ChangeTypeEnumDto.CREATE, createAuditLog.getChangeType());
            assertEquals(ChangeTypeEnumDto.UPDATE, updateAuditLog.getChangeType());
            assertEquals(ChangeTypeEnumDto.DELETE, deleteAuditLog.getChangeType());

            assertEquals(ObjectTypeEnumDto.PRESENTATION_TYPE, createAuditLog.getChangeName());
            assertEquals(ObjectTypeEnumDto.PRESENTATION_TYPE, updateAuditLog.getChangeName());
            assertEquals(ObjectTypeEnumDto.PRESENTATION_TYPE, deleteAuditLog.getChangeName());

            assertEquals("keyAuditTest", createAuditLog.getIdentifier());
            assertEquals("keyAuditTest", updateAuditLog.getIdentifier());
            assertEquals("keyAuditTest", deleteAuditLog.getIdentifier());

            assertNull(createAuditLog.getChangeComment());
            assertNull(updateAuditLog.getChangeComment());
            assertNull(deleteAuditLog.getChangeComment());

            assertNull(createAuditLog.getTextBefore());
            //TODO: This should be fixed together with: DRA-2085
            assertEquals("keyAuditTestPresentationType value DK/En:unit_test_value / unit_test_value_en\n", updateAuditLog.getTextBefore());
            assertEquals("PresentationType value DK/En:unit_test_value_updated / unit_test_value_en_updated\n", deleteAuditLog.getTextBefore());

            //TODO: This should be fixed together with: DRA-2085
            assertEquals("PresentationType value DK/En:unit_test_value / unit_test_value_en\n", createAuditLog.getTextAfter());
            assertEquals("keyAuditTestPresentationType value DK/En:unit_test_value_updated / unit_test_value_en_updated\n", updateAuditLog.getTextAfter()); //Tjek op på det
            assertNull(deleteAuditLog.getTextAfter());
        }
    }
}
