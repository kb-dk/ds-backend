package dk.kb.license.facade;

import dk.kb.license.config.ServiceConfig;
import dk.kb.license.model.v1.AuditEntryOutputDto;
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

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;


public class DsLicenseFacadeTest  extends DsLicenseUnitTestUtil{
    protected static LicenseModuleStorage storage = null;
    private static final Logger log = LoggerFactory.getLogger(LicenseModuleStorageTest.class);

    @BeforeAll
    public static void beforeClass() throws IOException, SQLException {

        ServiceConfig.initialize("conf/ds-license*.yaml", "ds-license-integration-test.yaml");
        BaseModuleStorage.initialize(DRIVER, URL, USERNAME, PASSWORD);

        H2DbUtil.createEmptyH2DBFromDDL(URL, DRIVER, USERNAME, PASSWORD, List.of("ddl/licensemodule_create_h2_unittest.ddl"));
        storage = new LicenseModuleStorage();
    }

    @Test
    public void testAuditLog() throws SQLException {

        MessageImpl message = new MessageImpl();
        AccessToken mockedToken = Mockito.mock(AccessToken.class);
        Mockito.when(mockedToken.getName()).thenReturn("mockedName");
        message.put(KBAuthorizationInterceptor.ACCESS_TOKEN, mockedToken);

        MockedStatic<JAXRSUtils> mocked = mockStatic(JAXRSUtils.class);
        mocked.when(JAXRSUtils::getCurrentMessage).thenReturn(message);

        String key = "keyAuditTest";
        String value = "unit_test_value";
        String valueEnglish = "unit_test_value_en";
        String valueUpdated = "unit_test_value_updated";
        String valueEnglishUpdated = "unit_test_value_en_updated";

        long presentationTypeId = LicenseModuleFacade.persistLicensePresentationType(key, value, valueEnglish);
        LicenseModuleFacade.updatePresentationType(presentationTypeId, valueUpdated,valueEnglishUpdated);
        LicenseModuleFacade.deletePresentationType(key);

        ArrayList<AuditEntryOutputDto> auditLogEntriesForObject = storage.getAuditLogByObjectId(presentationTypeId);

        assertEquals(3, auditLogEntriesForObject.size());
        AuditEntryOutputDto createAuditLog = auditLogEntriesForObject.get(2);
        AuditEntryOutputDto updateAuditLog = auditLogEntriesForObject.get(1);
        AuditEntryOutputDto deleteAuditLog = auditLogEntriesForObject.get(0);

        assertEquals(ChangeTypeEnumDto.CREATE, createAuditLog.getChangeType());
        assertEquals(ChangeTypeEnumDto.UPDATE, updateAuditLog.getChangeType());
        assertEquals(ChangeTypeEnumDto.DELETE, deleteAuditLog.getChangeType());

        assertEquals(ObjectTypeEnumDto.PRESENTATION_TYPE, createAuditLog.getChangeName());
        assertEquals(ObjectTypeEnumDto.PRESENTATION_TYPE, updateAuditLog.getChangeName());
        assertEquals(ObjectTypeEnumDto.PRESENTATION_TYPE, deleteAuditLog.getChangeName());

        assertNull(createAuditLog.getTextBefore());
        //TODO: This should be fixed together with: DRA-2085
        assertEquals("keyAuditTestPresentationType value DK/En:unit_test_value / unit_test_value_en\n", updateAuditLog.getTextBefore());
        assertEquals("PresentationType value DK/En:unit_test_value_updated / unit_test_value_en_updated\n", deleteAuditLog.getTextBefore());

      //TODO: This should be fixed together with: DRA-2085
        assertEquals("PresentationType value DK/En:unit_test_value / unit_test_value_en\n", createAuditLog.getTextAfter());
        assertEquals("keyAuditTestPresentationType value DK/En:unit_test_value_updated / unit_test_value_en_updated\n", updateAuditLog.getTextAfter()); //Tjek op på det
        assertNull(deleteAuditLog.getTextAfter());

        assertEquals("mockedName", createAuditLog.getUserName());
        assertEquals("mockedName", updateAuditLog.getUserName());
        assertEquals("mockedName", deleteAuditLog.getUserName());

        assertEquals("keyAuditTest", createAuditLog.getChangeComment());
        assertEquals("keyAuditTest", updateAuditLog.getChangeComment());
        assertEquals("keyAuditTest", deleteAuditLog.getChangeComment());

        assertEquals(presentationTypeId, createAuditLog.getObjectId());
        assertEquals(presentationTypeId, updateAuditLog.getObjectId());
        assertEquals(presentationTypeId, deleteAuditLog.getObjectId());

        assertTrue(createAuditLog.getModifiedTime()<updateAuditLog.getModifiedTime());
        assertTrue(updateAuditLog.getModifiedTime()<deleteAuditLog.getModifiedTime());

        // Close the MockedStatic JAXRSUtils.class when the test is done, so it don't interfere with other test classes
        mocked.close();
    }
}
