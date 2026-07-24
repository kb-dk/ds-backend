package dk.kb.license.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpSession;

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

import dk.kb.license.config.ServiceConfig;
import dk.kb.license.model.v1.AuditLogEntryOutputDto;
import dk.kb.license.model.v1.ChangeTypeEnumDto;
import dk.kb.license.model.v1.DeleteReasonDto;
import dk.kb.license.model.v1.ObjectTypeEnumDto;
import dk.kb.license.storage.AuditLogModuleStorageForUnitTest;
import dk.kb.license.storage.BaseModuleStorage;
import dk.kb.license.storage.UnitTestUtil;
import dk.kb.license.util.H2DbUtil;
import dk.kb.license.webservice.KBAuthorizationInterceptor;


public class AuditLogModuleFacadeTest extends UnitTestUtil {


    protected static AuditLogModuleStorageForUnitTest storage = null;
    private static final Logger log = LoggerFactory.getLogger(AuditLogModuleFacadeTest.class);
    static MockedStatic<JAXRSUtils> mocked;

    @BeforeAll
    public static void beforeClass() throws IOException, SQLException {
        ServiceConfig.initialize("conf/ds-license*.yaml", "ds-license-integration-test.yaml");
        BaseModuleStorage.initialize(DRIVER, URL, USERNAME, PASSWORD);
        
        H2DbUtil.createEmptyH2DBFromDDL(URL, DRIVER, USERNAME, PASSWORD, List.of("ddl/licensemodule_create_h2_unittest.ddl","ddl/audit_log_module_create_h2_unittest.ddl"));
        storage = new  AuditLogModuleStorageForUnitTest();               
    }
    
    /*
     * Delete all records between each unittest. The clearTableRecords is only called from here.
     * The facade class is responsible for committing transactions. So clean up between unittests.
     */
    @BeforeEach
    public void beforeEach() throws SQLException {
        ArrayList<String> tables = new ArrayList<>();
        tables.add("PRESENTATIONTYPES");
        tables.add("GROUPTYPES");
        tables.add("ATTRIBUTETYPES");
        tables.add("LICENSE");
        tables.add("ATTRIBUTEGROUP");
        tables.add("ATTRIBUTE");
        tables.add("VALUE_ORG");
        tables.add("LICENSECONTENT");    
        tables.add("PRESENTATION");
        tables.add("AUDITLOG");    
        storage.clearTableRecords(tables);
    }
    
    
    @Test
    public void testAuditLogList() throws SQLException {
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
            String changeComment = "changeComment";
            DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
            deleteReasonDto.setChangeComment(changeComment);

            long presentationTypeId = LicenseModuleFacade.persistLicensePresentationType(key, value, valueEnglish, mockedSession);
            long after=System.currentTimeMillis()+1;
            //No type defined, get all.
            List<AuditLogEntryOutputDto> auditLogAll= AuditLogModuleFacade.getAuditLogOlderThanModifiedTime(after,null);            
            assertEquals(1, auditLogAll.size());
            
            //The correct type just added
            List<AuditLogEntryOutputDto> auditPresentationType= AuditLogModuleFacade.getAuditLogOlderThanModifiedTime(after,ObjectTypeEnumDto.PRESENTATION_TYPE);            
            assertEquals(1, auditPresentationType.size()); //Found

            //Not the type added
            List<AuditLogEntryOutputDto> dsIDType= AuditLogModuleFacade.getAuditLogOlderThanModifiedTime(after,ObjectTypeEnumDto.DS_ID);           
            assertEquals(0, dsIDType.size()); //Not found            
        }
    }    
}
