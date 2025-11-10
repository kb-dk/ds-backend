package dk.kb.license.facade;

import dk.kb.license.config.ServiceConfig;
import dk.kb.license.model.v1.*;
import dk.kb.license.solr.SolrServerClient;
import dk.kb.license.storage.BaseModuleStorage;
import dk.kb.license.storage.RightsModuleStorageForUnitTest;
import dk.kb.license.storage.UnitTestUtil;
import dk.kb.license.util.H2DbUtil;
import dk.kb.license.webservice.KBAuthorizationInterceptor;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import dk.kb.util.webservice.exception.NotFoundServiceException;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.MessageImpl;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.keycloak.representations.AccessToken;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RightsModuleFacadeTest extends UnitTestUtil {
    protected static RightsModuleStorageForUnitTest storage = null;
    static MockedStatic<JAXRSUtils> mocked;
    final String drHoldBackValue = "2.02";
    final String drHoldBackName = "Aktualitet og Debat";
    final int drHoldbackDays = 2190;
    final String userName = "mockedName";
    DrHoldbackRuleInputDto drHoldbackRuleInputDto = new DrHoldbackRuleInputDto();
    DrHoldbackRangeDto drHoldbackRangeDtoOne = new DrHoldbackRangeDto();
    DrHoldbackRangeDto drHoldbackRangeDtoTwo = new DrHoldbackRangeDto();

    List<AuditLogEntryOutputDto> auditLogEntriesForObject;

    @BeforeAll
    public static void beforeClass() throws IOException, SQLException {
        ServiceConfig.initialize("conf/ds-license*.yaml");
        H2DbUtil.createEmptyH2DBFromDDL(URL, DRIVER, USERNAME, PASSWORD, List.of("ddl/rightsmodule_create_h2_unittest.ddl", "ddl/audit_log_module_create_h2_unittest.ddl"));
        BaseModuleStorage.initialize(DRIVER, URL, USERNAME, PASSWORD);

        storage = new RightsModuleStorageForUnitTest();

        MessageImpl message = new MessageImpl();
        AccessToken mockedToken = mock(AccessToken.class);
        when(mockedToken.getName()).thenReturn("mockedName");
        message.put(KBAuthorizationInterceptor.ACCESS_TOKEN, mockedToken);

        mocked = mockStatic(JAXRSUtils.class);
        mocked.when(JAXRSUtils::getCurrentMessage).thenReturn(message);
    }

    /**
     * Close the MockedStatic JAXRSUtils.class when the tests are done, so it don't interfere with other test classes
     */
    @AfterAll
    public static void afterClass() {
        mocked.close();
    }

    /*
     * Delete all records between each unittest. The clearTableRecords is only called from here.
     * The facade class is responsible for committing transactions. So clean up between unittests.
     */
    @BeforeEach
    public void beforeEach() throws SQLException {
        ArrayList<String> tables = new ArrayList<>();
        tables.add("RESTRICTED_IDS");
        tables.add("DR_HOLDBACK_RANGES");
        tables.add("DR_HOLDBACK_RULES");
        tables.add("AUDITLOG");
        storage.clearTableRecords(tables);

        drHoldbackRuleInputDto.setDays(drHoldbackDays);
        drHoldbackRuleInputDto.setDrHoldbackValue(drHoldBackValue);
        drHoldbackRuleInputDto.setName(drHoldBackName);

        drHoldbackRangeDtoOne.setContentRangeFrom(1000);
        drHoldbackRangeDtoOne.setContentRangeTo(1900);
        drHoldbackRangeDtoOne.setFormRangeFrom(1000);
        drHoldbackRangeDtoOne.setFormRangeTo(1000);

        drHoldbackRangeDtoTwo.setContentRangeFrom(1000);
        drHoldbackRangeDtoTwo.setContentRangeTo(1900);
        drHoldbackRangeDtoTwo.setFormRangeFrom(1200);
        drHoldbackRangeDtoTwo.setFormRangeTo(1500);
    }

    /**
     * Transforms a String to a Date object
     *
     * @param dateString
     * @return Date
     * @throws ParseException
     */
    private Date parseStringToDate(String dateString) throws ParseException {
        // The format date Solr client from dependency returns
        final String parseDateFormat = "EEEE MMM dd HH:mm:ss z yyyy";
        return new SimpleDateFormat(parseDateFormat, Locale.ROOT).parse(dateString);
    }

    @Test
    public void createRestrictedId_whenInvalidDsId_thenThrowInvalidArgumentServiceException() throws SQLException {
        // Arrange
        String invalidDsId = "4b35ee6f-b7d3-4fee-8936-a067b42eb9ef";
        RestrictedIdInputDto restrictedIdInputDto = new RestrictedIdInputDto();
        restrictedIdInputDto.setIdValue(invalidDsId);
        restrictedIdInputDto.setIdType(IdTypeEnumDto.DS_ID);
        restrictedIdInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        restrictedIdInputDto.setTitle("Test title");
        restrictedIdInputDto.setComment("Brugeren har trukket deres samtykke tilbage");

        String expectedMessage = "Invalid dsId: " + invalidDsId;

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> RightsModuleFacade.createRestrictedId(false, restrictedIdInputDto));
        auditLogEntriesForObject = storage.getAllAudit();

        // Assert
        assertEquals(expectedMessage, exception.getMessage());

        // Only valid RestrictedIdInputDto objects is in the audit log
        assertEquals(0, auditLogEntriesForObject.size());
    }

    @Test
    public void createRestrictedId_whenInvalidDrProductionId_thenThrowInvalidArgumentServiceException() throws SQLException {
        // Arrange
        String invalidDrProductionId = "1234567";
        RestrictedIdInputDto restrictedIdInputDto = new RestrictedIdInputDto();
        restrictedIdInputDto.setIdValue(invalidDrProductionId);
        restrictedIdInputDto.setIdType(IdTypeEnumDto.DR_PRODUCTION_ID);
        restrictedIdInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        restrictedIdInputDto.setTitle("Test title");
        restrictedIdInputDto.setComment("Brugeren har trukket deres samtykke tilbage");

        String expectedMessage = "drProductionId: " + invalidDrProductionId + " should be at least 8 digits";

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> RightsModuleFacade.createRestrictedId(false, restrictedIdInputDto));
        auditLogEntriesForObject = storage.getAllAudit();

        // Assert
        assertEquals(expectedMessage, exception.getMessage());

        // Only valid RestrictedIdInputDto objects is in the audit log
        assertEquals(0, auditLogEntriesForObject.size());
    }

    @Test
    public void createRestrictedId_whenBlankStrictTitle_thenThrowInvalidArgumentServiceException() throws SQLException {
        // Arrange
        String invalidStrictTitle = " ";
        RestrictedIdInputDto restrictedIdInputDto = new RestrictedIdInputDto();
        restrictedIdInputDto.setIdValue(invalidStrictTitle);
        restrictedIdInputDto.setIdType(IdTypeEnumDto.STRICT_TITLE);
        restrictedIdInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        restrictedIdInputDto.setTitle("Test title");
        restrictedIdInputDto.setComment("Brugeren har trukket deres samtykke tilbage");

        String expectedMessage = "strictTitle cannot be empty";

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> RightsModuleFacade.createRestrictedId(false, restrictedIdInputDto));
        auditLogEntriesForObject = storage.getAllAudit();

        // Assert
        assertEquals(expectedMessage, exception.getMessage());

        // Only valid RestrictedIdInputDto objects is in the audit log
        assertEquals(0, auditLogEntriesForObject.size());
    }

    @Test
    public void createRestrictedId_whenBlankOwnProductionCode_thenThrowInvalidArgumentServiceException() throws SQLException {
        // Arrange
        String invalidStrictTitle = " ";
        RestrictedIdInputDto restrictedIdInputDto = new RestrictedIdInputDto();
        restrictedIdInputDto.setIdValue(invalidStrictTitle);
        restrictedIdInputDto.setIdType(IdTypeEnumDto.OWNPRODUCTION_CODE);
        restrictedIdInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        restrictedIdInputDto.setTitle("Test title");
        restrictedIdInputDto.setComment("Brugeren har trukket deres samtykke tilbage");

        String expectedMessage = "ownProductionCode cannot be empty";

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> RightsModuleFacade.createRestrictedId(false, restrictedIdInputDto));
        auditLogEntriesForObject = storage.getAllAudit();

        // Assert
        assertEquals(expectedMessage, exception.getMessage());

        // Only valid RestrictedIdInputDto objects is in the audit log
        assertEquals(0, auditLogEntriesForObject.size());
    }

    @Test
    public void createRestrictedId_whenInvalidOwnProductionCode_thenThrowInvalidArgumentServiceException() throws SQLException {
        // Arrange
        String invalidStrictTitle = "12ab";
        RestrictedIdInputDto restrictedIdInputDto = new RestrictedIdInputDto();
        restrictedIdInputDto.setIdValue(invalidStrictTitle);
        restrictedIdInputDto.setIdType(IdTypeEnumDto.OWNPRODUCTION_CODE);
        restrictedIdInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        restrictedIdInputDto.setTitle("Test title");
        restrictedIdInputDto.setComment("Brugeren har trukket deres samtykke tilbage");

        String expectedMessage = "ownProductionCode: 12ab should only contain digits";

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> RightsModuleFacade.createRestrictedId(false, restrictedIdInputDto));
        auditLogEntriesForObject = storage.getAllAudit();

        // Assert
        assertEquals(expectedMessage, exception.getMessage());

        // Only valid RestrictedIdInputDto objects is in the audit log
        assertEquals(0, auditLogEntriesForObject.size());
    }

    @Test
    public void createRestrictedId_whenNullTitle_thenThrowInvalidArgumentServiceException() throws SQLException {
        // Arrange
        RestrictedIdInputDto restrictedIdInputDto = new RestrictedIdInputDto();
        restrictedIdInputDto.setIdValue("12345678");
        restrictedIdInputDto.setIdType(IdTypeEnumDto.DR_PRODUCTION_ID);
        restrictedIdInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        restrictedIdInputDto.setTitle(null);
        restrictedIdInputDto.setComment("Brugeren har trukket deres samtykke tilbage");

        String expectedMessage = "Title cannot be empty";

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> RightsModuleFacade.createRestrictedId(false, restrictedIdInputDto));
        auditLogEntriesForObject = storage.getAllAudit();

        // Assert
        assertEquals(expectedMessage, exception.getMessage());

        // Only valid RestrictedIdInputDto objects is in the audit log
        assertEquals(0, auditLogEntriesForObject.size());
    }

    @Test
    public void createRestrictedId_whenNullComment_thenThrowInvalidArgumentServiceException() throws SQLException {
        // Arrange
        RestrictedIdInputDto restrictedIdInputDto = new RestrictedIdInputDto();
        restrictedIdInputDto.setIdValue("12345678");
        restrictedIdInputDto.setIdType(IdTypeEnumDto.DR_PRODUCTION_ID);
        restrictedIdInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        restrictedIdInputDto.setTitle("Test title");
        restrictedIdInputDto.setComment(null);

        String expectedMessage = "Comment cannot be empty";

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> RightsModuleFacade.createRestrictedId(false, restrictedIdInputDto));
        auditLogEntriesForObject = storage.getAllAudit();

        // Assert
        assertEquals(expectedMessage, exception.getMessage());

        // Only valid RestrictedIdInputDto objects is in the audit log
        assertEquals(0, auditLogEntriesForObject.size());
    }

    @Test
    public void createRestrictedId_whenValidRestrictedIdInputDto_thenReturnRestrictedIdOutputDto() throws SQLException {
        // Arrange
        String drProductionId = "12345678";
        String title = "Test title";
        String comment = "Brugeren har trukket deres samtykke tilbage";

        RestrictedIdInputDto restrictedIdInputDto = new RestrictedIdInputDto();
        restrictedIdInputDto.setIdValue(drProductionId);
        restrictedIdInputDto.setIdType(IdTypeEnumDto.DR_PRODUCTION_ID);
        restrictedIdInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        restrictedIdInputDto.setTitle(title);
        restrictedIdInputDto.setComment(comment);

        // Act
        RestrictedIdOutputDto restrictedIdOutputDto = RightsModuleFacade.createRestrictedId(false, restrictedIdInputDto);
        auditLogEntriesForObject = storage.getAllAudit();

        // Assert
        assertNotNull(restrictedIdOutputDto);
        assertNotNull(restrictedIdOutputDto.getId());
        assertEquals(drProductionId, restrictedIdOutputDto.getIdValue());
        assertEquals(IdTypeEnumDto.DR_PRODUCTION_ID, restrictedIdOutputDto.getIdType());
        assertEquals(PlatformEnumDto.DRARKIV, restrictedIdOutputDto.getPlatform());
        assertEquals(title, restrictedIdOutputDto.getTitle());
        assertEquals(comment, restrictedIdOutputDto.getComment());

        // Only valid RestrictedIdInputDto objects is in the audit log
        AuditLogEntryOutputDto auditLogEntryOutputDto = auditLogEntriesForObject.get(0);

        assertTrue(auditLogEntryOutputDto.getId() > 0L);
        assertEquals(restrictedIdOutputDto.getId(), auditLogEntryOutputDto.getObjectId());
        assertTrue(auditLogEntryOutputDto.getModifiedTime() > 0L); //modifiedtime has been set
        assertEquals(userName, auditLogEntryOutputDto.getUserName());
        assertEquals(ChangeTypeEnumDto.CREATE, auditLogEntryOutputDto.getChangeType());
        assertEquals(ObjectTypeEnumDto.DR_PRODUCTION_ID, auditLogEntryOutputDto.getChangeName());
        assertEquals(drProductionId, auditLogEntryOutputDto.getIdentifier());
        assertNull(auditLogEntryOutputDto.getChangeComment());
        assertNull(auditLogEntryOutputDto.getTextBefore());
        assertEquals(restrictedIdOutputDto.toString(), auditLogEntryOutputDto.getTextAfter());
    }

    @Test
    public void updateRestrictedId_whenInvalidId_thenThrowInvalidArgumentServiceException() throws SQLException {
        // Arrange
        Long invalidId = 1234567890L;
        RestrictedIdInputDto restrictedIdInputDto = new RestrictedIdInputDto();

        String expectedMessage = "id: " + invalidId + " should be at least 11 digits";

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> RightsModuleFacade.updateRestrictedId(invalidId, false, restrictedIdInputDto));
        auditLogEntriesForObject = storage.getAllAudit();

        // Assert
        assertEquals(expectedMessage, exception.getMessage());

        // Only valid RestrictedIdInputDto objects is in the audit log
        assertEquals(0, auditLogEntriesForObject.size());
    }

    @Test
    public void updateRestrictedId_whenInvalidDsId_thenThrowInvalidArgumentServiceException() throws SQLException {
        // Arrange
        String invalidDsId = "4b35ee6f-b7d3-4fee-8936-a067b42eb9ef";
        RestrictedIdInputDto restrictedIdInputDto = new RestrictedIdInputDto();
        restrictedIdInputDto.setIdValue(invalidDsId);
        restrictedIdInputDto.setIdType(IdTypeEnumDto.DS_ID);
        restrictedIdInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        restrictedIdInputDto.setTitle("Test title");
        restrictedIdInputDto.setComment("Brugeren har trukket deres samtykke tilbage");

        String expectedMessage = "Invalid dsId: " + invalidDsId;

        long id = storage.createRestrictedId(restrictedIdInputDto.getIdValue(), restrictedIdInputDto.getIdType().name(), restrictedIdInputDto.getPlatform().name(), restrictedIdInputDto.getTitle(), restrictedIdInputDto.getComment());

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> RightsModuleFacade.updateRestrictedId(id, false, restrictedIdInputDto));
        auditLogEntriesForObject = storage.getAllAudit();

        // Assert
        assertEquals(expectedMessage, exception.getMessage());

        // Only valid RestrictedIdInputDto objects is in the audit log
        assertEquals(0, auditLogEntriesForObject.size());
    }

    @Test
    public void updateRestrictedId_whenInvalidDrProductionId_thenThrowInvalidArgumentServiceException() throws SQLException {
        // Arrange
        String invalidDrProductionId = "1234567";
        RestrictedIdInputDto restrictedIdInputDto = new RestrictedIdInputDto();
        restrictedIdInputDto.setIdValue(invalidDrProductionId);
        restrictedIdInputDto.setIdType(IdTypeEnumDto.DR_PRODUCTION_ID);
        restrictedIdInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        restrictedIdInputDto.setTitle("Test title");
        restrictedIdInputDto.setComment("Brugeren har trukket deres samtykke tilbage");

        String expectedMessage = "drProductionId: " + invalidDrProductionId + " should be at least 8 digits";

        long id = storage.createRestrictedId(restrictedIdInputDto.getIdValue(), restrictedIdInputDto.getIdType().name(), restrictedIdInputDto.getPlatform().name(), restrictedIdInputDto.getTitle(), restrictedIdInputDto.getComment());

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> RightsModuleFacade.updateRestrictedId(id, false, restrictedIdInputDto));
        auditLogEntriesForObject = storage.getAllAudit();

        // Assert
        assertEquals(expectedMessage, exception.getMessage());

        // Only valid RestrictedIdInputDto objects is in the audit log
        assertEquals(0, auditLogEntriesForObject.size());
    }

    @Test
    public void updateRestrictedId_whenBlankStrictTitle_thenThrowInvalidArgumentServiceException() throws SQLException {
        // Arrange
        String invalidStrictTitle = " ";
        RestrictedIdInputDto restrictedIdInputDto = new RestrictedIdInputDto();
        restrictedIdInputDto.setIdValue(invalidStrictTitle);
        restrictedIdInputDto.setIdType(IdTypeEnumDto.STRICT_TITLE);
        restrictedIdInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        restrictedIdInputDto.setTitle("Test title");
        restrictedIdInputDto.setComment("Brugeren har trukket deres samtykke tilbage");

        String expectedMessage = "strictTitle cannot be empty";

        long id = storage.createRestrictedId(restrictedIdInputDto.getIdValue(), restrictedIdInputDto.getIdType().name(), restrictedIdInputDto.getPlatform().name(), restrictedIdInputDto.getTitle(), restrictedIdInputDto.getComment());

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> RightsModuleFacade.updateRestrictedId(id, false, restrictedIdInputDto));
        auditLogEntriesForObject = storage.getAllAudit();

        // Assert
        assertEquals(expectedMessage, exception.getMessage());

        // Only valid RestrictedIdInputDto objects is in the audit log
        assertEquals(0, auditLogEntriesForObject.size());
    }

    @Test
    public void updateRestrictedId_whenBlankOwnProductionCode_thenThrowInvalidArgumentServiceException() throws SQLException {
        // Arrange
        String invalidStrictTitle = " ";
        RestrictedIdInputDto restrictedIdInputDto = new RestrictedIdInputDto();
        restrictedIdInputDto.setIdValue(invalidStrictTitle);
        restrictedIdInputDto.setIdType(IdTypeEnumDto.OWNPRODUCTION_CODE);
        restrictedIdInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        restrictedIdInputDto.setTitle("Test title");
        restrictedIdInputDto.setComment("Brugeren har trukket deres samtykke tilbage");

        String expectedMessage = "ownProductionCode cannot be empty";

        long id = storage.createRestrictedId(restrictedIdInputDto.getIdValue(), restrictedIdInputDto.getIdType().name(), restrictedIdInputDto.getPlatform().name(), restrictedIdInputDto.getTitle(), restrictedIdInputDto.getComment());

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> RightsModuleFacade.updateRestrictedId(id, false, restrictedIdInputDto));
        auditLogEntriesForObject = storage.getAllAudit();

        // Assert
        assertEquals(expectedMessage, exception.getMessage());

        // Only valid RestrictedIdInputDto objects is in the audit log
        assertEquals(0, auditLogEntriesForObject.size());
    }

    @Test
    public void updateRestrictedId_whenInvalidOwnProductionCode_thenThrowInvalidArgumentServiceException() throws SQLException {
        // Arrange
        String invalidStrictTitle = "12ab";
        RestrictedIdInputDto restrictedIdInputDto = new RestrictedIdInputDto();
        restrictedIdInputDto.setIdValue(invalidStrictTitle);
        restrictedIdInputDto.setIdType(IdTypeEnumDto.OWNPRODUCTION_CODE);
        restrictedIdInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        restrictedIdInputDto.setTitle("Test title");
        restrictedIdInputDto.setComment("Brugeren har trukket deres samtykke tilbage");

        String expectedMessage = "ownProductionCode: 12ab should only contain digits";

        long id = storage.createRestrictedId(restrictedIdInputDto.getIdValue(), restrictedIdInputDto.getIdType().name(), restrictedIdInputDto.getPlatform().name(), restrictedIdInputDto.getTitle(), restrictedIdInputDto.getComment());

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> RightsModuleFacade.updateRestrictedId(id, false, restrictedIdInputDto));
        auditLogEntriesForObject = storage.getAllAudit();

        // Assert
        assertEquals(expectedMessage, exception.getMessage());

        // Only valid RestrictedIdInputDto objects is in the audit log
        assertEquals(0, auditLogEntriesForObject.size());
    }

    @Test
    public void updateRestrictedId_whenNullTitle_thenThrowInvalidArgumentServiceException() throws SQLException {
        // Arrange
        RestrictedIdInputDto restrictedIdInputDto = new RestrictedIdInputDto();
        restrictedIdInputDto.setIdValue("12345678");
        restrictedIdInputDto.setIdType(IdTypeEnumDto.DR_PRODUCTION_ID);
        restrictedIdInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        restrictedIdInputDto.setTitle(null);
        restrictedIdInputDto.setComment("Brugeren har trukket deres samtykke tilbage");

        String expectedMessage = "Title cannot be empty";

        long id = storage.createRestrictedId(restrictedIdInputDto.getIdValue(), restrictedIdInputDto.getIdType().name(), restrictedIdInputDto.getPlatform().name(), restrictedIdInputDto.getTitle(), restrictedIdInputDto.getComment());

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> RightsModuleFacade.updateRestrictedId(id, false, restrictedIdInputDto));
        auditLogEntriesForObject = storage.getAllAudit();

        // Assert
        assertEquals(expectedMessage, exception.getMessage());

        // Only valid RestrictedIdInputDto objects is in the audit log
        assertEquals(0, auditLogEntriesForObject.size());
    }

    @Test
    public void updateRestrictedId_whenNullComment_thenThrowInvalidArgumentServiceException() throws SQLException {
        // Arrange
        String drProductId = "12345678";
        String title = "Test title";

        RestrictedIdInputDto createRestrictedIdInputDto = new RestrictedIdInputDto();
        createRestrictedIdInputDto.setIdValue(drProductId);
        createRestrictedIdInputDto.setIdType(IdTypeEnumDto.DR_PRODUCTION_ID);
        createRestrictedIdInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        createRestrictedIdInputDto.setTitle(title);
        createRestrictedIdInputDto.setComment("Brugeren har trukket deres samtykke tilbage");

        String expectedMessage = "Comment cannot be empty";

        long id = storage.createRestrictedId(createRestrictedIdInputDto.getIdValue(), createRestrictedIdInputDto.getIdType().name(), createRestrictedIdInputDto.getPlatform().name(), createRestrictedIdInputDto.getTitle(), createRestrictedIdInputDto.getComment());

        RestrictedIdInputDto updateRestrictedIdInputDto = new RestrictedIdInputDto();
        updateRestrictedIdInputDto.setIdValue(drProductId);
        updateRestrictedIdInputDto.setIdType(IdTypeEnumDto.DR_PRODUCTION_ID);
        updateRestrictedIdInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        updateRestrictedIdInputDto.setTitle(title);
        updateRestrictedIdInputDto.setComment(null);

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> RightsModuleFacade.updateRestrictedId(id, false, updateRestrictedIdInputDto));
        auditLogEntriesForObject = storage.getAllAudit();

        // Assert
        assertEquals(expectedMessage, exception.getMessage());

        // Only valid RestrictedIdInputDto objects is in the audit log
        assertEquals(0, auditLogEntriesForObject.size());
    }

    @Test
    public void updateRestrictedId_whenRestrictedIdDoesNotExists_thenReturnRestrictedIdOutputDto() throws SQLException {
        // Arrange
        Long notExistingId = 12345678901L;
        String drProductionId = "12345678";
        String updatedTitle = "Updated title";
        String updatedComment = "Updated comment";

        RestrictedIdInputDto updateRestrictedIdInputDto = new RestrictedIdInputDto();
        updateRestrictedIdInputDto.setIdValue(drProductionId);
        updateRestrictedIdInputDto.setIdType(IdTypeEnumDto.DR_PRODUCTION_ID);
        updateRestrictedIdInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        updateRestrictedIdInputDto.setTitle(updatedTitle);
        updateRestrictedIdInputDto.setComment(updatedComment);

        String expectedMessage = "id: " + notExistingId + " not found";

        // Act
        Exception exception = assertThrows(InternalServiceException.class, () -> RightsModuleFacade.updateRestrictedId(notExistingId, false, updateRestrictedIdInputDto));
        auditLogEntriesForObject = storage.getAllAudit();

        // Assert
        assertTrue(exception.getMessage().contains(expectedMessage));

        // Only valid RestrictedIdInputDto objects is in the audit log
        assertEquals(0, auditLogEntriesForObject.size());
    }

    @Test
    public void updateRestrictedId_whenValidRestrictedIdInputDto_thenReturnRestrictedIdOutputDto() throws SQLException {
        // Arrange
        String drProductionId = "12345678";
        String title = "Test title";
        String updatedTitle = "Updated title";
        String comment = "Brugeren har trukket deres samtykke tilbage";
        String updatedComment = "Updated comment";

        RestrictedIdInputDto createRestrictedIdInputDto = new RestrictedIdInputDto();
        createRestrictedIdInputDto.setIdValue(drProductionId);
        createRestrictedIdInputDto.setIdType(IdTypeEnumDto.DR_PRODUCTION_ID);
        createRestrictedIdInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        createRestrictedIdInputDto.setTitle(title);
        createRestrictedIdInputDto.setComment(comment);

        RestrictedIdOutputDto createdRestrictedIdOutputDto = RightsModuleFacade.createRestrictedId(false, createRestrictedIdInputDto);

        RestrictedIdInputDto updateRestrictedIdInputDto = new RestrictedIdInputDto();
        updateRestrictedIdInputDto.setIdValue(drProductionId);
        updateRestrictedIdInputDto.setIdType(IdTypeEnumDto.DR_PRODUCTION_ID);
        updateRestrictedIdInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        updateRestrictedIdInputDto.setTitle(updatedTitle);
        updateRestrictedIdInputDto.setComment(updatedComment);

        // Act
        RestrictedIdOutputDto updatedRestrictedIdOutputDto = RightsModuleFacade.updateRestrictedId(createdRestrictedIdOutputDto.getId(), false, updateRestrictedIdInputDto);
        auditLogEntriesForObject = storage.getAllAudit();

        // Assert
        assertNotNull(updatedRestrictedIdOutputDto);
        assertNotNull(updatedRestrictedIdOutputDto.getId());
        assertEquals(createdRestrictedIdOutputDto.getId(), updatedRestrictedIdOutputDto.getId());
        assertEquals(drProductionId, updatedRestrictedIdOutputDto.getIdValue());
        assertEquals(IdTypeEnumDto.DR_PRODUCTION_ID, updatedRestrictedIdOutputDto.getIdType());
        assertEquals(PlatformEnumDto.DRARKIV, updatedRestrictedIdOutputDto.getPlatform());
        assertEquals(updatedTitle, updatedRestrictedIdOutputDto.getTitle());
        assertEquals(updatedComment, updatedRestrictedIdOutputDto.getComment());

        // Only valid RestrictedIdInputDto objects is in the audit log
        assertEquals(2, auditLogEntriesForObject.size());

        AuditLogEntryOutputDto createdAuditLogEntryOutputDto = auditLogEntriesForObject.get(1);

        assertTrue(createdAuditLogEntryOutputDto.getId() > 0L);
        assertEquals(createdRestrictedIdOutputDto.getId(), createdAuditLogEntryOutputDto.getObjectId());
        assertTrue(createdAuditLogEntryOutputDto.getModifiedTime() > 0L); //modifiedtime has been set
        assertEquals(userName, createdAuditLogEntryOutputDto.getUserName());
        assertEquals(ChangeTypeEnumDto.CREATE, createdAuditLogEntryOutputDto.getChangeType());
        assertEquals(ObjectTypeEnumDto.DR_PRODUCTION_ID, createdAuditLogEntryOutputDto.getChangeName());
        assertEquals(drProductionId, createdAuditLogEntryOutputDto.getIdentifier());
        assertNull(createdAuditLogEntryOutputDto.getChangeComment());
        assertNull(createdAuditLogEntryOutputDto.getTextBefore());
        assertEquals(createdRestrictedIdOutputDto.toString(), createdAuditLogEntryOutputDto.getTextAfter());

        AuditLogEntryOutputDto updatedAuditLogEntryOutputDto = auditLogEntriesForObject.get(0);

        assertTrue(updatedAuditLogEntryOutputDto.getId() > 0L);
        assertEquals(updatedRestrictedIdOutputDto.getId(), updatedAuditLogEntryOutputDto.getObjectId());
        assertTrue(updatedAuditLogEntryOutputDto.getModifiedTime() > 0L); //modifiedtime has been set
        assertEquals(userName, updatedAuditLogEntryOutputDto.getUserName());
        assertEquals(ChangeTypeEnumDto.UPDATE, updatedAuditLogEntryOutputDto.getChangeType());
        assertEquals(ObjectTypeEnumDto.DR_PRODUCTION_ID, updatedAuditLogEntryOutputDto.getChangeName());
        assertEquals(drProductionId, updatedAuditLogEntryOutputDto.getIdentifier());
        assertNull(updatedAuditLogEntryOutputDto.getChangeComment());
        assertEquals(createdAuditLogEntryOutputDto.getTextAfter(), updatedAuditLogEntryOutputDto.getTextBefore());
        assertEquals(updatedRestrictedIdOutputDto.toString(), updatedAuditLogEntryOutputDto.getTextAfter());
    }

    @Test
    public void createOrUpdateRestrictedIds_whenInvalidDsId_thenProcessStatusDtoIsFailedAndFailedIdsList() throws SQLException {
        // Arrange
        RestrictedIdInputDto restrictedIdInputDto = new RestrictedIdInputDto();
        restrictedIdInputDto.setIdValue("4b35ee6f-b7d3-4fee-8936-a067b42eb9ef");
        restrictedIdInputDto.setIdType(IdTypeEnumDto.DS_ID);
        restrictedIdInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        restrictedIdInputDto.setTitle("Test title");
        restrictedIdInputDto.setComment("Brugeren har trukket deres samtykke tilbage");

        List<RestrictedIdInputDto> restrictedIds = new ArrayList<>();
        restrictedIds.add(restrictedIdInputDto);

        // Act
        ProcessedRestrictedIdsOutputDto processedRestrictedIdsOutputDto = RightsModuleFacade.createOrUpdateRestrictedIds(false, restrictedIds);
        auditLogEntriesForObject = storage.getAllAudit();

        // Assert
        assertNotNull(processedRestrictedIdsOutputDto);
        assertEquals(ProcessStatusDto.FAILED, processedRestrictedIdsOutputDto.getProcessStatus());
        assertEquals(0, processedRestrictedIdsOutputDto.getProcessedSuccessfully());
        assertEquals(1, processedRestrictedIdsOutputDto.getFailedRestrictedIds().size());
        assertEquals(restrictedIdInputDto.getIdValue(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getIdValue());
        assertEquals(restrictedIdInputDto.getIdType(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getIdType());
        assertEquals(restrictedIdInputDto.getPlatform(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getPlatform());
        assertEquals(restrictedIdInputDto.getTitle(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getTitle());
        assertEquals(restrictedIdInputDto.getComment(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getComment());
        assertEquals("InvalidArgumentServiceException", processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getException());
        assertEquals("Invalid dsId: 4b35ee6f-b7d3-4fee-8936-a067b42eb9ef", processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getErrorMessage());

        // Only valid RestrictedIdInputDto objects is in the audit log
        assertEquals(0, auditLogEntriesForObject.size());
    }

    @Test
    public void createOrUpdateRestrictedIds_whenInvalidDrProductionId_thenProcessStatusDtoIsFailedAndFailedIdsList() throws SQLException {
        // Arrange
        String drProductionId = "1234567";
        RestrictedIdInputDto restrictedIdInputDto = new RestrictedIdInputDto();
        restrictedIdInputDto.setIdValue(drProductionId);
        restrictedIdInputDto.setIdType(IdTypeEnumDto.DR_PRODUCTION_ID);
        restrictedIdInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        restrictedIdInputDto.setTitle("Test title");
        restrictedIdInputDto.setComment("Brugeren har trukket deres samtykke tilbage");

        List<RestrictedIdInputDto> restrictedIds = new ArrayList<>();
        restrictedIds.add(restrictedIdInputDto);

        // Act
        ProcessedRestrictedIdsOutputDto processedRestrictedIdsOutputDto = RightsModuleFacade.createOrUpdateRestrictedIds(false, restrictedIds);
        auditLogEntriesForObject = storage.getAllAudit();

        // Assert
        assertNotNull(processedRestrictedIdsOutputDto);
        assertEquals(ProcessStatusDto.FAILED, processedRestrictedIdsOutputDto.getProcessStatus());
        assertEquals(0, processedRestrictedIdsOutputDto.getProcessedSuccessfully());
        assertEquals(1, processedRestrictedIdsOutputDto.getFailedRestrictedIds().size());
        assertEquals(restrictedIdInputDto.getIdValue(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getIdValue());
        assertEquals(restrictedIdInputDto.getIdType(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getIdType());
        assertEquals(restrictedIdInputDto.getPlatform(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getPlatform());
        assertEquals(restrictedIdInputDto.getTitle(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getTitle());
        assertEquals(restrictedIdInputDto.getComment(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getComment());
        assertEquals("InvalidArgumentServiceException", processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getException());
        assertEquals("drProductionId: " + drProductionId + " should be at least 8 digits", processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getErrorMessage());

        // Only valid RestrictedIdInputDto objects is in the audit log
        assertEquals(0, auditLogEntriesForObject.size());
    }

    @Test
    public void createOrUpdateRestrictedIds_whenBlankStrictTitle_thenProcessStatusDtoIsFailedAndFailedIdsList() throws SQLException {
        // Arrange
        RestrictedIdInputDto restrictedIdInputDto = new RestrictedIdInputDto();
        restrictedIdInputDto.setIdValue("");
        restrictedIdInputDto.setIdType(IdTypeEnumDto.STRICT_TITLE);
        restrictedIdInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        restrictedIdInputDto.setTitle("Test title");
        restrictedIdInputDto.setComment("Brugeren har trukket deres samtykke tilbage");

        List<RestrictedIdInputDto> restrictedIds = new ArrayList<>();
        restrictedIds.add(restrictedIdInputDto);

        // Act
        ProcessedRestrictedIdsOutputDto processedRestrictedIdsOutputDto = RightsModuleFacade.createOrUpdateRestrictedIds(false, restrictedIds);
        auditLogEntriesForObject = storage.getAllAudit();

        // Assert
        assertNotNull(processedRestrictedIdsOutputDto);
        assertEquals(ProcessStatusDto.FAILED, processedRestrictedIdsOutputDto.getProcessStatus());
        assertEquals(0, processedRestrictedIdsOutputDto.getProcessedSuccessfully());
        assertEquals(1, processedRestrictedIdsOutputDto.getFailedRestrictedIds().size());
        assertEquals(restrictedIdInputDto.getIdValue(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getIdValue());
        assertEquals(restrictedIdInputDto.getIdType(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getIdType());
        assertEquals(restrictedIdInputDto.getPlatform(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getPlatform());
        assertEquals(restrictedIdInputDto.getTitle(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getTitle());
        assertEquals(restrictedIdInputDto.getComment(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getComment());
        assertEquals("InvalidArgumentServiceException", processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getException());
        assertEquals("strictTitle cannot be empty", processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getErrorMessage());

        // Only valid RestrictedIdInputDto objects is in the audit log
        assertEquals(0, auditLogEntriesForObject.size());
    }

    @Test
    public void createOrUpdateRestrictedIds_whenBlankOwnProductionCode_thenProcessStatusDtoIsFailedAndFailedIdsList() throws SQLException {
        // Arrange
        RestrictedIdInputDto restrictedIdInputDto = new RestrictedIdInputDto();
        restrictedIdInputDto.setIdValue(" ");
        restrictedIdInputDto.setIdType(IdTypeEnumDto.OWNPRODUCTION_CODE);
        restrictedIdInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        restrictedIdInputDto.setTitle("Test title");
        restrictedIdInputDto.setComment("Brugeren har trukket deres samtykke tilbage");

        List<RestrictedIdInputDto> restrictedIds = new ArrayList<>();
        restrictedIds.add(restrictedIdInputDto);

        // Act
        ProcessedRestrictedIdsOutputDto processedRestrictedIdsOutputDto = RightsModuleFacade.createOrUpdateRestrictedIds(false, restrictedIds);
        auditLogEntriesForObject = storage.getAllAudit();

        // Assert
        assertNotNull(processedRestrictedIdsOutputDto);
        assertEquals(ProcessStatusDto.FAILED, processedRestrictedIdsOutputDto.getProcessStatus());
        assertEquals(0, processedRestrictedIdsOutputDto.getProcessedSuccessfully());
        assertEquals(1, processedRestrictedIdsOutputDto.getFailedRestrictedIds().size());
        assertEquals(restrictedIdInputDto.getIdValue(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getIdValue());
        assertEquals(restrictedIdInputDto.getIdType(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getIdType());
        assertEquals(restrictedIdInputDto.getPlatform(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getPlatform());
        assertEquals(restrictedIdInputDto.getTitle(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getTitle());
        assertEquals(restrictedIdInputDto.getComment(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getComment());
        assertEquals("InvalidArgumentServiceException", processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getException());
        assertEquals("ownProductionCode cannot be empty", processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getErrorMessage());

        // Only valid RestrictedIdInputDto objects is in the audit log
        assertEquals(0, auditLogEntriesForObject.size());
    }

    @Test
    public void createOrUpdateRestrictedIds_whenInvalidOwnProductionCode_thenProcessStatusDtoIsFailedAndFailedIdsList() throws SQLException {
        // Arrange
        RestrictedIdInputDto restrictedIdInputDto = new RestrictedIdInputDto();
        restrictedIdInputDto.setIdValue("12ab");
        restrictedIdInputDto.setIdType(IdTypeEnumDto.OWNPRODUCTION_CODE);
        restrictedIdInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        restrictedIdInputDto.setTitle("Test title");
        restrictedIdInputDto.setComment("Brugeren har trukket deres samtykke tilbage");

        List<RestrictedIdInputDto> restrictedIds = new ArrayList<>();
        restrictedIds.add(restrictedIdInputDto);

        // Act
        ProcessedRestrictedIdsOutputDto processedRestrictedIdsOutputDto = RightsModuleFacade.createOrUpdateRestrictedIds(false, restrictedIds);
        auditLogEntriesForObject = storage.getAllAudit();

        // Assert
        assertNotNull(processedRestrictedIdsOutputDto);
        assertEquals(ProcessStatusDto.FAILED, processedRestrictedIdsOutputDto.getProcessStatus());
        assertEquals(0, processedRestrictedIdsOutputDto.getProcessedSuccessfully());
        assertEquals(1, processedRestrictedIdsOutputDto.getFailedRestrictedIds().size());
        assertEquals(restrictedIdInputDto.getIdValue(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getIdValue());
        assertEquals(restrictedIdInputDto.getIdType(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getIdType());
        assertEquals(restrictedIdInputDto.getPlatform(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getPlatform());
        assertEquals(restrictedIdInputDto.getTitle(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getTitle());
        assertEquals(restrictedIdInputDto.getComment(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getComment());
        assertEquals("InvalidArgumentServiceException", processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getException());
        assertEquals("ownProductionCode: 12ab should only contain digits", processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getErrorMessage());

        // Only valid RestrictedIdInputDto objects is in the audit log
        assertEquals(0, auditLogEntriesForObject.size());
    }

    @Test
    public void createOrUpdateRestrictedIds_whenNullTitle_thenProcessStatusDtoIsFailedAndFailedIdsList() throws SQLException {
        // Arrange
        RestrictedIdInputDto restrictedIdInputDto = new RestrictedIdInputDto();
        restrictedIdInputDto.setIdValue("ds.tv:oai:io:4b35ee6f-b7d3-4fee-8936-a067b42eb9ef");
        restrictedIdInputDto.setIdType(IdTypeEnumDto.DS_ID);
        restrictedIdInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        restrictedIdInputDto.setTitle(null);
        restrictedIdInputDto.setComment("Brugeren har trukket deres samtykke tilbage");

        List<RestrictedIdInputDto> restrictedIds = new ArrayList<>();
        restrictedIds.add(restrictedIdInputDto);

        // Act
        ProcessedRestrictedIdsOutputDto processedRestrictedIdsOutputDto = RightsModuleFacade.createOrUpdateRestrictedIds(false, restrictedIds);
        auditLogEntriesForObject = storage.getAllAudit();

        // Assert
        assertNotNull(processedRestrictedIdsOutputDto);
        assertEquals(ProcessStatusDto.FAILED, processedRestrictedIdsOutputDto.getProcessStatus());
        assertEquals(0, processedRestrictedIdsOutputDto.getProcessedSuccessfully());
        assertEquals(1, processedRestrictedIdsOutputDto.getFailedRestrictedIds().size());
        assertEquals(restrictedIdInputDto.getIdValue(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getIdValue());
        assertEquals(restrictedIdInputDto.getIdType(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getIdType());
        assertEquals(restrictedIdInputDto.getPlatform(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getPlatform());
        assertEquals(restrictedIdInputDto.getTitle(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getTitle());
        assertEquals(restrictedIdInputDto.getComment(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getComment());
        assertEquals("InvalidArgumentServiceException", processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getException());
        assertEquals("Title cannot be empty", processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getErrorMessage());

        // Only valid RestrictedIdInputDto objects is in the audit log
        assertEquals(0, auditLogEntriesForObject.size());
    }

    @Test
    public void createOrUpdateRestrictedIds_whenNullComment_thenProcessStatusDtoIsFailedAndFailedIdsList() throws SQLException {
        // Arrange
        RestrictedIdInputDto restrictedIdInputDto = new RestrictedIdInputDto();
        restrictedIdInputDto.setIdValue("ds.tv:oai:io:4b35ee6f-b7d3-4fee-8936-a067b42eb9ef");
        restrictedIdInputDto.setIdType(IdTypeEnumDto.DS_ID);
        restrictedIdInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        restrictedIdInputDto.setTitle("Test title");
        restrictedIdInputDto.setComment(null);

        List<RestrictedIdInputDto> restrictedIds = new ArrayList<>();
        restrictedIds.add(restrictedIdInputDto);

        // Act
        ProcessedRestrictedIdsOutputDto processedRestrictedIdsOutputDto = RightsModuleFacade.createOrUpdateRestrictedIds(false, restrictedIds);
        auditLogEntriesForObject = storage.getAllAudit();

        // Assert
        assertNotNull(processedRestrictedIdsOutputDto);
        assertEquals(ProcessStatusDto.FAILED, processedRestrictedIdsOutputDto.getProcessStatus());
        assertEquals(0, processedRestrictedIdsOutputDto.getProcessedSuccessfully());
        assertEquals(1, processedRestrictedIdsOutputDto.getFailedRestrictedIds().size());
        assertEquals(restrictedIdInputDto.getIdValue(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getIdValue());
        assertEquals(restrictedIdInputDto.getIdType(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getIdType());
        assertEquals(restrictedIdInputDto.getPlatform(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getPlatform());
        assertEquals(restrictedIdInputDto.getTitle(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getTitle());
        assertEquals(restrictedIdInputDto.getComment(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getComment());
        assertEquals("InvalidArgumentServiceException", processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getException());
        assertEquals("Comment cannot be empty", processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getErrorMessage());

        // Only valid RestrictedIdInputDto objects is in the audit log
        assertEquals(0, auditLogEntriesForObject.size());
    }

    @Test
    public void createOrUpdateRestrictedIds_whenValidRestrictedIdsList_thenProcessStatusDtoIsSuccessAndEmptyFailedIdsList() throws SQLException {
        // Arrange
        String dsId = "ds.tv:oai:io:ea440a12-d14b-46cd-b6b9-53b16ee56111";
        String drProductionId = "12345678";
        String title = "Test title";
        String comment = "Brugeren har trukket deres samtykke tilbage";

        RestrictedIdInputDto dsIdRestrictedIdInputDto = new RestrictedIdInputDto();
        dsIdRestrictedIdInputDto.setIdValue(dsId);
        dsIdRestrictedIdInputDto.setIdType(IdTypeEnumDto.DS_ID);
        dsIdRestrictedIdInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        dsIdRestrictedIdInputDto.setTitle(title);
        dsIdRestrictedIdInputDto.setComment(comment);

        RestrictedIdInputDto drProductionIdRestrictedIdInputDto = new RestrictedIdInputDto();
        drProductionIdRestrictedIdInputDto.setIdValue(drProductionId);
        drProductionIdRestrictedIdInputDto.setIdType(IdTypeEnumDto.DR_PRODUCTION_ID);
        drProductionIdRestrictedIdInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        drProductionIdRestrictedIdInputDto.setTitle(title);
        drProductionIdRestrictedIdInputDto.setComment(comment);

        List<RestrictedIdInputDto> restrictedIds = new ArrayList<>();
        restrictedIds.add(dsIdRestrictedIdInputDto);
        restrictedIds.add(drProductionIdRestrictedIdInputDto);

        // Act
        ProcessedRestrictedIdsOutputDto processedRestrictedIdsOutputDto = RightsModuleFacade.createOrUpdateRestrictedIds(false, restrictedIds);
        auditLogEntriesForObject = storage.getAllAudit();

        // Assert
        assertNotNull(processedRestrictedIdsOutputDto);
        assertEquals(ProcessStatusDto.SUCCESS, processedRestrictedIdsOutputDto.getProcessStatus());
        assertEquals(2, processedRestrictedIdsOutputDto.getProcessedSuccessfully());
        assertTrue(processedRestrictedIdsOutputDto.getFailedRestrictedIds().isEmpty());

        // Only valid RestrictedIdInputDto objects is in the audit log
        assertEquals(restrictedIds.size(), auditLogEntriesForObject.size());

        AuditLogEntryOutputDto dsIdAuditLogEntryOutputDto = auditLogEntriesForObject.get(1);

        assertTrue(dsIdAuditLogEntryOutputDto.getId() > 0L);
        assertTrue(dsIdAuditLogEntryOutputDto.getObjectId() > 0L);
        assertTrue(dsIdAuditLogEntryOutputDto.getModifiedTime() > 0L); //modifiedtime has been set
        assertEquals(userName, dsIdAuditLogEntryOutputDto.getUserName());
        assertEquals(ChangeTypeEnumDto.CREATE, dsIdAuditLogEntryOutputDto.getChangeType());
        assertEquals(ObjectTypeEnumDto.DS_ID, dsIdAuditLogEntryOutputDto.getChangeName());
        assertEquals(dsId, dsIdAuditLogEntryOutputDto.getIdentifier());
        assertNull(dsIdAuditLogEntryOutputDto.getChangeComment());
        assertNull(dsIdAuditLogEntryOutputDto.getTextBefore());
        assertNotNull(dsIdAuditLogEntryOutputDto.getTextAfter());

        AuditLogEntryOutputDto drProductionIdAuditLogEntryOutputDto = auditLogEntriesForObject.get(0);

        assertTrue(drProductionIdAuditLogEntryOutputDto.getId() > 0L);
        assertTrue(drProductionIdAuditLogEntryOutputDto.getObjectId() > 0L);
        assertTrue(drProductionIdAuditLogEntryOutputDto.getModifiedTime() > 0L); //modifiedtime has been set
        assertEquals(userName, drProductionIdAuditLogEntryOutputDto.getUserName());
        assertEquals(ChangeTypeEnumDto.CREATE, drProductionIdAuditLogEntryOutputDto.getChangeType());
        assertEquals(ObjectTypeEnumDto.DR_PRODUCTION_ID, drProductionIdAuditLogEntryOutputDto.getChangeName());
        assertEquals(drProductionId, drProductionIdAuditLogEntryOutputDto.getIdentifier());
        assertNull(drProductionIdAuditLogEntryOutputDto.getChangeComment());
        assertNull(drProductionIdAuditLogEntryOutputDto.getTextBefore());
        assertNotNull(drProductionIdAuditLogEntryOutputDto.getTextAfter());
    }

    @Test
    public void createOrUpdateRestrictedIds_whenValidAndInvalidDsId_thenProcessStatusDtoIsPartialProcessedAndFailedIdsList() throws SQLException {
        // Arrange
        String validDsId = "ds.tv:oai:io:ea440a12-d14b-46cd-b6b9-53b16ee56111";
        String title = "Test title";
        String comment = "Brugeren har trukket deres samtykke tilbage";

        RestrictedIdInputDto validRestrictedIdInputDto = new RestrictedIdInputDto();
        validRestrictedIdInputDto.setIdValue(validDsId);
        validRestrictedIdInputDto.setIdType(IdTypeEnumDto.DS_ID);
        validRestrictedIdInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        validRestrictedIdInputDto.setTitle(title);
        validRestrictedIdInputDto.setComment(comment);

        RestrictedIdInputDto invalidRestrictedIdInputDto = new RestrictedIdInputDto();
        invalidRestrictedIdInputDto.setIdValue("4b35ee6f-b7d3-4fee-8936-a067b42eb9ef");
        invalidRestrictedIdInputDto.setIdType(IdTypeEnumDto.DS_ID);
        invalidRestrictedIdInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        invalidRestrictedIdInputDto.setTitle(title);
        invalidRestrictedIdInputDto.setComment(comment);

        List<RestrictedIdInputDto> restrictedIds = new ArrayList<>();
        restrictedIds.add(validRestrictedIdInputDto);
        restrictedIds.add(invalidRestrictedIdInputDto);

        // Act
        ProcessedRestrictedIdsOutputDto processedRestrictedIdsOutputDto = RightsModuleFacade.createOrUpdateRestrictedIds(false, restrictedIds);
        auditLogEntriesForObject = storage.getAllAudit();

        // Assert
        assertNotNull(processedRestrictedIdsOutputDto);
        assertEquals(ProcessStatusDto.PARTIAL_PROCESSED, processedRestrictedIdsOutputDto.getProcessStatus());
        assertEquals(1, processedRestrictedIdsOutputDto.getProcessedSuccessfully());
        assertEquals(1, processedRestrictedIdsOutputDto.getFailedRestrictedIds().size());
        assertEquals(invalidRestrictedIdInputDto.getIdValue(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getIdValue());
        assertEquals(invalidRestrictedIdInputDto.getIdType(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getIdType());
        assertEquals(invalidRestrictedIdInputDto.getPlatform(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getPlatform());
        assertEquals(invalidRestrictedIdInputDto.getTitle(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getTitle());
        assertEquals(invalidRestrictedIdInputDto.getComment(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getComment());
        assertEquals("InvalidArgumentServiceException", processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getException());
        assertEquals("Invalid dsId: 4b35ee6f-b7d3-4fee-8936-a067b42eb9ef", processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getErrorMessage());

        // Only valid RestrictedIdInputDto objects is in the audit log
        assertEquals(1, auditLogEntriesForObject.size());
        AuditLogEntryOutputDto auditLogEntryOutputDto = auditLogEntriesForObject.get(0);

        assertTrue(auditLogEntryOutputDto.getId() > 0L);
        assertTrue(auditLogEntryOutputDto.getObjectId() > 0L);
        assertTrue(auditLogEntryOutputDto.getModifiedTime() > 0L); //modifiedtime has been set
        assertEquals(userName, auditLogEntryOutputDto.getUserName());
        assertEquals(ChangeTypeEnumDto.CREATE, auditLogEntryOutputDto.getChangeType());
        assertEquals(ObjectTypeEnumDto.DS_ID, auditLogEntryOutputDto.getChangeName());
        assertEquals(validDsId, auditLogEntryOutputDto.getIdentifier());
        assertNull(auditLogEntryOutputDto.getChangeComment());
        assertNull(auditLogEntryOutputDto.getTextBefore());
        assertNotNull(auditLogEntryOutputDto.getTextAfter());
    }

    @Test
    public void createOrUpdateRestrictedIds_whenAlreadyExistingRestrictedId_thenUpdateRestrictedIdAndProcessStatusDtoIsSuccessAndEmptyFailedIdsList() throws SQLException {
        // Arrange
        String validDsId = "ds.tv:oai:io:ea440a12-d14b-46cd-b6b9-53b16ee56111";
        String title = "Test title";
        String comment = "Brugeren har trukket deres samtykke tilbage";

        RestrictedIdInputDto restrictedIdInputDto = new RestrictedIdInputDto();
        restrictedIdInputDto.setIdValue(validDsId);
        restrictedIdInputDto.setIdType(IdTypeEnumDto.DS_ID);
        restrictedIdInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        restrictedIdInputDto.setTitle(title);
        restrictedIdInputDto.setComment(comment);

        RestrictedIdInputDto duplicatedRestrictedIdInputDto = new RestrictedIdInputDto();
        duplicatedRestrictedIdInputDto.setIdValue(validDsId);
        duplicatedRestrictedIdInputDto.setIdType(IdTypeEnumDto.DS_ID);
        duplicatedRestrictedIdInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        duplicatedRestrictedIdInputDto.setTitle(title);
        duplicatedRestrictedIdInputDto.setComment("Opdateret kommentar");

        List<RestrictedIdInputDto> restrictedIds = new ArrayList<>();
        restrictedIds.add(restrictedIdInputDto);
        restrictedIds.add(duplicatedRestrictedIdInputDto);

        // Act
        ProcessedRestrictedIdsOutputDto processedRestrictedIdsOutputDto = RightsModuleFacade.createOrUpdateRestrictedIds(false, restrictedIds);
        auditLogEntriesForObject = storage.getAllAudit();

        // Assert
        assertNotNull(processedRestrictedIdsOutputDto);
        assertEquals(ProcessStatusDto.SUCCESS, processedRestrictedIdsOutputDto.getProcessStatus());
        assertEquals(2, processedRestrictedIdsOutputDto.getProcessedSuccessfully());
        assertEquals(0, processedRestrictedIdsOutputDto.getFailedRestrictedIds().size());

        // Only valid RestrictedIdInputDto objects is in the audit log
        assertEquals(2, auditLogEntriesForObject.size());

        AuditLogEntryOutputDto createdAuditLogEntryOutputDto = auditLogEntriesForObject.get(1);

        assertTrue(createdAuditLogEntryOutputDto.getId() > 0L);
        assertTrue(createdAuditLogEntryOutputDto.getObjectId() > 0L);
        assertTrue(createdAuditLogEntryOutputDto.getModifiedTime() > 0L); //modifiedtime has been set
        assertEquals(userName, createdAuditLogEntryOutputDto.getUserName());
        assertEquals(ChangeTypeEnumDto.CREATE, createdAuditLogEntryOutputDto.getChangeType());
        assertEquals(ObjectTypeEnumDto.DS_ID, createdAuditLogEntryOutputDto.getChangeName());
        assertEquals(validDsId, createdAuditLogEntryOutputDto.getIdentifier());
        assertNull(createdAuditLogEntryOutputDto.getChangeComment());
        assertNull(createdAuditLogEntryOutputDto.getTextBefore());
        assertNotNull(createdAuditLogEntryOutputDto.getTextAfter());

        AuditLogEntryOutputDto updatedAuditLogEntryOutputDto = auditLogEntriesForObject.get(0);

        assertTrue(updatedAuditLogEntryOutputDto.getId() > 0L);
        assertTrue(updatedAuditLogEntryOutputDto.getObjectId() > 0L);
        assertTrue(updatedAuditLogEntryOutputDto.getModifiedTime() > 0L); //modifiedtime has been set
        assertEquals(userName, updatedAuditLogEntryOutputDto.getUserName());
        assertEquals(ChangeTypeEnumDto.UPDATE, updatedAuditLogEntryOutputDto.getChangeType());
        assertEquals(ObjectTypeEnumDto.DS_ID, updatedAuditLogEntryOutputDto.getChangeName());
        assertEquals(validDsId, updatedAuditLogEntryOutputDto.getIdentifier());
        assertNull(updatedAuditLogEntryOutputDto.getChangeComment());
        assertEquals(createdAuditLogEntryOutputDto.getTextAfter(), updatedAuditLogEntryOutputDto.getTextBefore());
        assertNotNull(updatedAuditLogEntryOutputDto.getTextAfter());
    }

    @Test
    public void deleteRestrictedId_whenInvalidId_thenThrowNotFoundServiceException() {
        // Arrange
        Long invalidId = 12345678910L;
        String changeComment = "Udsendelse må vises efter nye aftaler";
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        deleteReasonDto.setChangeComment(changeComment);

        // Act
        Exception exception = assertThrows(InternalServiceException.class, () -> RightsModuleFacade.deleteRestrictedId(invalidId, false, deleteReasonDto));

        // Assert
        assertTrue(exception.getMessage().contains("id: " + invalidId + " not found"));
    }

    @Test
    public void deleteRestrictedId_whenValidId_thenDeleteRestrictedId() throws SQLException {
        // Arrange
        String drProductionId = "12345678";
        String title = "Test title";
        String comment = "Brugeren har trukket deres samtykke tilbage";
        String changeComment = "Udsendelse må vises efter nye aftaler";

        RestrictedIdInputDto restrictedIdInputDto = new RestrictedIdInputDto();
        restrictedIdInputDto.setIdValue(drProductionId);
        restrictedIdInputDto.setIdType(IdTypeEnumDto.DR_PRODUCTION_ID);
        restrictedIdInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        restrictedIdInputDto.setTitle(title);
        restrictedIdInputDto.setComment(comment);

        RestrictedIdOutputDto restrictedIdOutputDto = RightsModuleFacade.createRestrictedId(false, restrictedIdInputDto);

        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        deleteReasonDto.setChangeComment(changeComment);

        // Act
        RecordsCountDto recordsCountDto = RightsModuleFacade.deleteRestrictedId(restrictedIdOutputDto.getId(), false, deleteReasonDto);

        // Make sure that the restricted id is deleted
        Exception exception = assertThrows(InternalServiceException.class, () -> RightsModuleFacade.getRestrictedIdById(restrictedIdOutputDto.getId()));

        auditLogEntriesForObject = storage.getAuditLogByObjectId(restrictedIdOutputDto.getId());

        // Assert
        assertTrue(exception.getMessage().contains("id: " + restrictedIdOutputDto.getId() + " not found"));

        assertEquals(1, recordsCountDto.getCount());

        assertEquals(2, auditLogEntriesForObject.size());
        AuditLogEntryOutputDto auditLogEntryOutputDto = auditLogEntriesForObject.get(0);

        assertTrue(auditLogEntryOutputDto.getId() > 0L);
        assertEquals(restrictedIdOutputDto.getId(), auditLogEntryOutputDto.getObjectId());
        assertTrue(auditLogEntryOutputDto.getModifiedTime() > 0L);
        assertEquals(userName, auditLogEntryOutputDto.getUserName());
        assertEquals(ChangeTypeEnumDto.DELETE, auditLogEntryOutputDto.getChangeType());
        assertEquals(ObjectTypeEnumDto.DR_PRODUCTION_ID, auditLogEntryOutputDto.getChangeName());
        assertEquals(drProductionId, auditLogEntryOutputDto.getIdentifier());
        assertEquals(changeComment, auditLogEntryOutputDto.getChangeComment());
        assertEquals(restrictedIdOutputDto.toString(), auditLogEntryOutputDto.getTextBefore());
        assertNull(auditLogEntryOutputDto.getTextAfter());
    }

    @Test
    public void matchingDrProductionIdBroadcasts_whenNullDsId_thenThrowInvalidArgumentServiceException() {
        // Arrange
        String expectedMessage = "dsId cannot be empty";
        // We need to mock the call to solr,
        SolrServerClient mockedSolrServerClient = mock(SolrServerClient.class);

        try (MockedStatic<RightsModuleFacade> mockedRightsModuleFacade = mockStatic(RightsModuleFacade.class)) {
            mockedRightsModuleFacade.when(RightsModuleFacade::getSolrServerClient).thenReturn(mockedSolrServerClient);
            // Because we have mocked RightsModuleFacade, we need to tell Mockito to use the real method want to test on
            mockedRightsModuleFacade.when(() -> RightsModuleFacade.matchingDrProductionIdBroadcasts(null)).thenCallRealMethod();

            // Act
            Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> RightsModuleFacade.matchingDrProductionIdBroadcasts(null));

            // Assert
            assertEquals(expectedMessage, exception.getMessage());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            " "
    })
    public void matchingDrProductionIdBroadcasts_whenEmptyOrBlankDsId_thenThrowInvalidArgumentServiceException(String dsId) {
        // Arrange
        String expectedMessage = "dsId cannot be empty";
        // We need to mock the call to solr,
        SolrServerClient mockedSolrServerClient = mock(SolrServerClient.class);

        try (MockedStatic<RightsModuleFacade> mockedRightsModuleFacade = mockStatic(RightsModuleFacade.class)) {
            mockedRightsModuleFacade.when(RightsModuleFacade::getSolrServerClient).thenReturn(mockedSolrServerClient);
            // Because we have mocked RightsModuleFacade, we need to tell Mockito to use the real method want to test on
            mockedRightsModuleFacade.when(() -> RightsModuleFacade.matchingDrProductionIdBroadcasts(dsId)).thenCallRealMethod();

            // Act
            Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> RightsModuleFacade.matchingDrProductionIdBroadcasts(dsId));

            // Assert
            assertEquals(expectedMessage, exception.getMessage());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "1",
            "0bdf8656-4a96-400d-b3d8-e4695328688e",
            ":0bdf8656-4a96-400d-b3d8-e4695328688e",
            "ds.tv:oai:io0bdf8656-4a96-400d-b3d8-e4695328688e",
            "\"ds.tv:oai:io:0bdf8656-4a96-400d-b3d8-e4695328688e\""
    })
    public void matchingDrProductionIdBroadcasts_whenInvalidDsId_thenThrowInvalidArgumentServiceException(String dsId) {
        // Arrange
        String expectedMessage = "Invalid dsId: " + dsId;
        // We need to mock the call to solr,
        SolrServerClient mockedSolrServerClient = mock(SolrServerClient.class);

        try (MockedStatic<RightsModuleFacade> mockedRightsModuleFacade = mockStatic(RightsModuleFacade.class)) {
            mockedRightsModuleFacade.when(RightsModuleFacade::getSolrServerClient).thenReturn(mockedSolrServerClient);
            // Because we have mocked RightsModuleFacade, we need to tell Mockito to use the real method want to test on
            mockedRightsModuleFacade.when(() -> RightsModuleFacade.matchingDrProductionIdBroadcasts(dsId)).thenCallRealMethod();

            // Act
            Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> RightsModuleFacade.matchingDrProductionIdBroadcasts(dsId));

            // Assert
            assertEquals(expectedMessage, exception.getMessage());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ds.tv:oai:io:",
            "ds.radio:oai:io:",
            "ds.tv:oai:io:0bdf8656-4a96-400d-b3d8-e4695328688",
            "ds.tv:oai:io:0bdf8656-4a96-400d-b3d8-e4695328688e"
    })
    public void matchingDrProductionIdBroadcasts_whenNotFoundDsId_thenThrowNotFoundServiceException(String dsId) throws SolrServerException, IOException {
        // Arrange
        String expectedMessage = "dsId: " + dsId + " not found";
        SolrDocumentList solrDocumentList = new SolrDocumentList();
        solrDocumentList.setNumFound(0);

        // We need to mock the call to solr,
        SolrServerClient mockedSolrServerClient = mock(SolrServerClient.class);
        when(mockedSolrServerClient.callSolr(anyString(), anyString())).thenReturn(solrDocumentList);

        try (MockedStatic<RightsModuleFacade> mockedRightsModuleFacade = mockStatic(RightsModuleFacade.class)) {
            mockedRightsModuleFacade.when(RightsModuleFacade::getSolrServerClient).thenReturn(mockedSolrServerClient);
            // Because we have mocked RightsModuleFacade, we need to tell Mockito to use the real method want to test on
            mockedRightsModuleFacade.when(() -> RightsModuleFacade.matchingDrProductionIdBroadcasts(dsId)).thenCallRealMethod();

            // Act
            Exception exception = assertThrows(NotFoundServiceException.class, () -> RightsModuleFacade.matchingDrProductionIdBroadcasts(dsId));

            // Assert
            assertEquals(expectedMessage, exception.getMessage());
        }
    }

    @Test
    public void matchingDrProductionIdBroadcasts_whenValidDsIdTWithDrProductionIdButThereIsNoMatchOnDrProductionId_thenThrowNotFoundServiceException() throws ParseException, SolrServerException, IOException {
        // Arrange
        String drProductionId = "9213163000";
        String expectedMessage = "No DR broadcasts found with drProductionId: " + drProductionId;

        String dsId = "ds.tv:oai:io:baafb0d9-691f-409d-8c34-97051cf79b93";
        String title = "TV-Avisen.";
        String startTime = "Thu Sep 29 21:55:00 CET 1966";
        String endTime = "Thu Sep 29 22:05:00 CET 1966";

        String queryDsId = "id:\"" + dsId + "\"";
        String fieldListDsId = "dr_production_id, id, title, startTime, endTime";
        String queryDrProductionId = "dr_production_id:\"" + drProductionId + "\"";
        String fieldListDrProductionId = "id, title, startTime, endTime";

        Date startTimeDate = parseStringToDate(startTime);
        Date endTimeDate = parseStringToDate(endTime);

        SolrDocument solrDocumentDsId = new SolrDocument();
        solrDocumentDsId.put("dr_production_id", drProductionId);
        solrDocumentDsId.put("id", dsId);
        solrDocumentDsId.put("title", title);
        solrDocumentDsId.put("startTime", startTimeDate);
        solrDocumentDsId.put("endTime", endTimeDate);

        SolrDocumentList solrDocumentListDsId = new SolrDocumentList();
        solrDocumentListDsId.setNumFound(1);
        solrDocumentListDsId.add(solrDocumentDsId);

        SolrDocument solrDocumentDrProductionId = new SolrDocument();

        SolrDocumentList solrDocumentListDrProductionId = new SolrDocumentList();
        solrDocumentListDrProductionId.setNumFound(0);
        solrDocumentListDrProductionId.add(solrDocumentDrProductionId);

        // We need to mock the call to solr,
        SolrServerClient mockedSolrServerClient = mock(SolrServerClient.class);
        when(mockedSolrServerClient.callSolr(queryDsId, fieldListDsId)).thenReturn(solrDocumentListDsId);
        when(mockedSolrServerClient.callSolr(queryDrProductionId, fieldListDrProductionId)).thenReturn(solrDocumentListDrProductionId);

        try (MockedStatic<RightsModuleFacade> mockedRightsModuleFacade = mockStatic(RightsModuleFacade.class)) {
            mockedRightsModuleFacade.when(RightsModuleFacade::getSolrServerClient).thenReturn(mockedSolrServerClient);
            // Because we have mocked RightsModuleFacade, we need to tell Mockito to use the real method want to test on
            mockedRightsModuleFacade.when(() -> RightsModuleFacade.matchingDrProductionIdBroadcasts(dsId)).thenCallRealMethod();

            // Act
            Exception exception = assertThrows(NotFoundServiceException.class, () -> RightsModuleFacade.matchingDrProductionIdBroadcasts(dsId));

            // Assert
            assertEquals(expectedMessage, exception.getMessage());
        }
    }

    @Test
    public void matchingDrProductionIdBroadcasts_whenValidDsIdWithNoDrProductionId_thenReturnDrBroadcastDto() throws ParseException, SolrServerException, IOException {
        // Arrange
        String dsId = "ds.tv:oai:io:baafb0d9-691f-409d-8c34-97051cf79b93";
        String title = "TV-Avisen.";
        String startTime = "Thu Sep 29 21:55:00 CET 1966";
        String endTime = "Thu Sep 29 22:05:00 CET 1966";

        Date startTimeDate = parseStringToDate(startTime);
        Date endTimeDate = parseStringToDate(endTime);

        SolrDocument solrDocument = new SolrDocument();
        solrDocument.put("id", dsId);
        solrDocument.put("title", title);
        solrDocument.put("startTime", startTimeDate);
        solrDocument.put("endTime", endTimeDate);

        SolrDocumentList solrDocumentList = new SolrDocumentList();
        solrDocumentList.setNumFound(1);
        solrDocumentList.add(solrDocument);

        // We need to mock the call to solr,
        SolrServerClient mockedSolrServerClient = mock(SolrServerClient.class);
        when(mockedSolrServerClient.callSolr(anyString(), anyString())).thenReturn(solrDocumentList);

        try (MockedStatic<RightsModuleFacade> mockedRightsModuleFacade = mockStatic(RightsModuleFacade.class)) {
            mockedRightsModuleFacade.when(RightsModuleFacade::getSolrServerClient).thenReturn(mockedSolrServerClient);
            // Because we have mocked RightsModuleFacade, we need to tell Mockito to use the real method want to test on
            mockedRightsModuleFacade.when(() -> RightsModuleFacade.matchingDrProductionIdBroadcasts(dsId)).thenCallRealMethod();

            // Act
            DrBroadcastDto actualDrBroadcastDto = RightsModuleFacade.matchingDrProductionIdBroadcasts(dsId);

            // Assert
            assertNotNull(actualDrBroadcastDto);
            assertNull(actualDrBroadcastDto.getDrProductionId());

            assertNotNull(actualDrBroadcastDto.getBroadcast());
            assertEquals(1, actualDrBroadcastDto.getBroadcast().size());

            assertEquals(dsId, actualDrBroadcastDto.getBroadcast().get(0).getDsId());
            assertEquals(title, actualDrBroadcastDto.getBroadcast().get(0).getTitle());
            assertEquals(OffsetDateTime.parse("1966-09-29T20:55Z"), actualDrBroadcastDto.getBroadcast().get(0).getStartTime());
            assertEquals(OffsetDateTime.class, actualDrBroadcastDto.getBroadcast().get(0).getEndTime().getClass());
            assertEquals(OffsetDateTime.parse("1966-09-29T21:05Z"), actualDrBroadcastDto.getBroadcast().get(0).getEndTime());
            assertEquals(false, actualDrBroadcastDto.getBroadcast().get(0).getRestricted());
            assertNull(actualDrBroadcastDto.getBroadcast().get(0).getRestrictedComment());
        }
    }

    @Test
    public void matchingDrProductionIdBroadcasts_whenValidDsIdWithNoDrProductionIdAndRestrictedComment_thenReturnDrBroadcastDto() throws ParseException, SolrServerException, IOException {
        // Arrange
        String dsId = "ds.tv:oai:io:baafb0d9-691f-409d-8c34-97051cf79b93";
        String title = "TV-Avisen.";
        String startTime = "Thu Sep 29 21:55:00 CET 1966";
        String endTime = "Thu Sep 29 22:05:00 CET 1966";
        String restrictedCommentOne = "Brugeren har trukket deres samtykke tilbage";

        Date startTimeDate = parseStringToDate(startTime);
        Date endTimeDate = parseStringToDate(endTime);

        SolrDocument solrDocument = new SolrDocument();
        solrDocument.put("id", dsId);
        solrDocument.put("title", title);
        solrDocument.put("startTime", startTimeDate);
        solrDocument.put("endTime", endTimeDate);

        SolrDocumentList solrDocumentList = new SolrDocumentList();
        solrDocumentList.setNumFound(1);
        solrDocumentList.add(solrDocument);

        // We need to mock the call to solr,
        SolrServerClient mockedSolrServerClient = mock(SolrServerClient.class);
        when(mockedSolrServerClient.callSolr(anyString(), anyString())).thenReturn(solrDocumentList);

        try (MockedStatic<RightsModuleFacade> mockedRightsModuleFacade = mockStatic(RightsModuleFacade.class)) {
            mockedRightsModuleFacade.when(RightsModuleFacade::getSolrServerClient).thenReturn(mockedSolrServerClient);
            mockedRightsModuleFacade.when(() -> RightsModuleFacade.getRestrictedIdCommentByIdValue(dsId)).thenReturn(restrictedCommentOne);
            // Because we have mocked RightsModuleFacade, we need to tell Mockito to use the real method want to test on
            mockedRightsModuleFacade.when(() -> RightsModuleFacade.matchingDrProductionIdBroadcasts(dsId)).thenCallRealMethod();

            // Act
            DrBroadcastDto actualDrBroadcastDto = RightsModuleFacade.matchingDrProductionIdBroadcasts(dsId);

            // Assert
            assertNotNull(actualDrBroadcastDto);
            assertNull(actualDrBroadcastDto.getDrProductionId());

            assertNotNull(actualDrBroadcastDto.getBroadcast());
            assertEquals(1, actualDrBroadcastDto.getBroadcast().size());

            assertEquals(dsId, actualDrBroadcastDto.getBroadcast().get(0).getDsId());
            assertEquals(title, actualDrBroadcastDto.getBroadcast().get(0).getTitle());
            assertEquals(OffsetDateTime.class, actualDrBroadcastDto.getBroadcast().get(0).getStartTime().getClass());
            assertEquals(OffsetDateTime.parse("1966-09-29T20:55Z"), actualDrBroadcastDto.getBroadcast().get(0).getStartTime());
            assertEquals(OffsetDateTime.class, actualDrBroadcastDto.getBroadcast().get(0).getEndTime().getClass());
            assertEquals(OffsetDateTime.parse("1966-09-29T21:05Z"), actualDrBroadcastDto.getBroadcast().get(0).getEndTime());
            assertEquals(true, actualDrBroadcastDto.getBroadcast().get(0).getRestricted());
            assertNotNull(actualDrBroadcastDto.getBroadcast().get(0).getRestrictedComment());
            assertEquals(restrictedCommentOne, actualDrBroadcastDto.getBroadcast().get(0).getRestrictedComment());
        }
    }

    @Test
    public void matchingDrProductionIdBroadcasts_whenValidDsIdWithDrProductionId_thenReturnDrBroadcastDto() throws ParseException, SolrServerException, IOException {
        // Arrange
        String drProductionId = "9213163000";

        String dsIdOne = "ds.tv:oai:io:5c6ef540-9aa6-47cd-837e-7c488f8176f0";
        String titleOne = "P2 Radioavis";
        String startTimeOne = "Thu Apr 04 08:00:00 CEST 2018";
        String endTimeOne = "Thu Apr 04 08:06:00 CEST 2018";
        String restrictedCommentOne = "Brugeren har trukket deres samtykke tilbage";

        String dsIdTwo = "ds.tv:oai:io:d5ec7b20-c1f2-491e-a2cb-f143683a40f8";
        String titleTwo = "P2 Radioavis";
        String startTimeTwo = "Thu Apr 05 08:00:00 CEST 2018";
        String endTimeTwo = "Thu Apr 05 08:06:00 CEST 2018";

        Date startTimeDateOne = parseStringToDate(startTimeOne);
        Date endTimeDateOne = parseStringToDate(endTimeOne);
        Date startTimeDateTwo = parseStringToDate(startTimeTwo);
        Date endTimeDateTwo = parseStringToDate(endTimeTwo);

        SolrDocument solrDocumentOne = new SolrDocument();
        solrDocumentOne.put("dr_production_id", drProductionId);
        solrDocumentOne.put("id", dsIdOne);
        solrDocumentOne.put("title", titleOne);
        solrDocumentOne.put("startTime", startTimeDateOne);
        solrDocumentOne.put("endTime", endTimeDateOne);

        SolrDocument solrDocumentTwo = new SolrDocument();
        solrDocumentTwo.put("dr_production_id", drProductionId);
        solrDocumentTwo.put("id", dsIdTwo);
        solrDocumentTwo.put("title", titleTwo);
        solrDocumentTwo.put("startTime", startTimeDateTwo);
        solrDocumentTwo.put("endTime", endTimeDateTwo);

        SolrDocumentList solrDocumentList = new SolrDocumentList();
        solrDocumentList.setNumFound(1);
        solrDocumentList.addAll(List.of(solrDocumentOne, solrDocumentTwo));

        // We mock the call to
        SolrServerClient mockedSolrServerClient = mock(SolrServerClient.class);
        when(mockedSolrServerClient.callSolr(anyString(), anyString())).thenReturn(solrDocumentList);

        try (MockedStatic<RightsModuleFacade> mockedRightsModuleFacade = mockStatic(RightsModuleFacade.class)) {
            mockedRightsModuleFacade.when(RightsModuleFacade::getSolrServerClient).thenReturn(mockedSolrServerClient);
            // Because we have mocked RightsModuleFacade, we need to tell Mockito to use the real method want to test on
            mockedRightsModuleFacade.when(() -> RightsModuleFacade.matchingDrProductionIdBroadcasts(anyString())).thenCallRealMethod();
            // It is only the first object in the list that has a restrictedComment
            mockedRightsModuleFacade.when(() -> RightsModuleFacade.getRestrictedIdCommentByIdValue(dsIdOne)).thenReturn(restrictedCommentOne);

            // Act
            DrBroadcastDto actualDrBroadcastDto = RightsModuleFacade.matchingDrProductionIdBroadcasts(dsIdOne);

            // Assert
            assertNotNull(actualDrBroadcastDto);
            assertEquals(drProductionId, actualDrBroadcastDto.getDrProductionId());

            assertNotNull(actualDrBroadcastDto.getBroadcast());
            assertEquals(2, actualDrBroadcastDto.getBroadcast().size());

            assertEquals(dsIdOne, actualDrBroadcastDto.getBroadcast().get(0).getDsId());
            assertEquals(titleOne, actualDrBroadcastDto.getBroadcast().get(0).getTitle());
            assertEquals(OffsetDateTime.class, actualDrBroadcastDto.getBroadcast().get(0).getStartTime().getClass());
            assertEquals(OffsetDateTime.parse("2018-04-04T06:00Z"), actualDrBroadcastDto.getBroadcast().get(0).getStartTime());
            assertEquals(OffsetDateTime.class, actualDrBroadcastDto.getBroadcast().get(0).getEndTime().getClass());
            assertEquals(OffsetDateTime.parse("2018-04-04T06:06Z"), actualDrBroadcastDto.getBroadcast().get(0).getEndTime());
            assertEquals(true, actualDrBroadcastDto.getBroadcast().get(0).getRestricted());
            assertNotNull(actualDrBroadcastDto.getBroadcast().get(0).getRestrictedComment());
            assertEquals(restrictedCommentOne, actualDrBroadcastDto.getBroadcast().get(0).getRestrictedComment());

            assertEquals(dsIdTwo, actualDrBroadcastDto.getBroadcast().get(1).getDsId());
            assertEquals(titleTwo, actualDrBroadcastDto.getBroadcast().get(1).getTitle());
            assertEquals(OffsetDateTime.class, actualDrBroadcastDto.getBroadcast().get(1).getStartTime().getClass());
            assertEquals(OffsetDateTime.parse("2018-04-05T06:00Z"), actualDrBroadcastDto.getBroadcast().get(1).getStartTime());
            assertEquals(OffsetDateTime.class, actualDrBroadcastDto.getBroadcast().get(1).getStartTime().getClass());
            assertEquals(OffsetDateTime.parse("2018-04-05T06:06Z"), actualDrBroadcastDto.getBroadcast().get(1).getEndTime());
            assertEquals(false, actualDrBroadcastDto.getBroadcast().get(1).getRestricted());
            assertNull(actualDrBroadcastDto.getBroadcast().get(1).getRestrictedComment());
        }
    }

    @Test
    public void createDrHoldbackRule_whenValidDrHoldbackRuleInputDto_thenCreateRule() throws SQLException {
        // Act
        DrHoldbackRuleOutputDto drHoldbackRuleOutputDto = RightsModuleFacade.createDrHoldbackRule(drHoldbackRuleInputDto);
        RightsModuleFacade.getDrHoldbackRuleByDrHoldbackValue(drHoldBackValue);
        auditLogEntriesForObject = storage.getAuditLogByObjectId(drHoldbackRuleOutputDto.getId());

        // Assert
        assertTrue(drHoldbackRuleOutputDto.getId() > 0L);
        assertEquals(drHoldBackValue, drHoldbackRuleOutputDto.getDrHoldbackValue());
        assertEquals(drHoldBackName, drHoldbackRuleOutputDto.getName());
        assertEquals(drHoldbackDays, drHoldbackRuleOutputDto.getDays());

        assertEquals(1, auditLogEntriesForObject.size());
        AuditLogEntryOutputDto auditLogEntryOutputDto = auditLogEntriesForObject.get(0);

        assertTrue(auditLogEntryOutputDto.getId() > 0L);
        assertEquals(drHoldbackRuleOutputDto.getId(), auditLogEntryOutputDto.getObjectId());
        assertTrue(auditLogEntryOutputDto.getModifiedTime() > 0L); //modifiedtime has been set
        assertEquals(userName, auditLogEntryOutputDto.getUserName());
        assertEquals(ChangeTypeEnumDto.CREATE, auditLogEntryOutputDto.getChangeType());
        assertEquals(ObjectTypeEnumDto.HOLDBACK_RULE, auditLogEntryOutputDto.getChangeName());
        assertEquals(drHoldBackValue, auditLogEntryOutputDto.getIdentifier());
        assertNull(auditLogEntryOutputDto.getChangeComment());
        assertNull(auditLogEntryOutputDto.getTextBefore());
        assertEquals(drHoldbackRuleOutputDto.toString(), auditLogEntryOutputDto.getTextAfter());
    }

    @Test
    public void updateDrHoldbackRule_whenValidDrHoldBackValue_thenUpdateRuleForRule() throws SQLException {
        // Arrange
        String changeComment = "DR holdback rule skal ikke længere bruges";
        DrHoldbackRuleInputDto updateDrHoldbackRuleInputDto = new DrHoldbackRuleInputDto();

        updateDrHoldbackRuleInputDto.setDrHoldbackValue(drHoldBackValue);
        updateDrHoldbackRuleInputDto.setName(drHoldBackName);
        updateDrHoldbackRuleInputDto.setDays(10);
        updateDrHoldbackRuleInputDto.setChangeComment(changeComment);

        DrHoldbackRuleOutputDto createdDrHoldbackRuleOutputDto = RightsModuleFacade.createDrHoldbackRule(drHoldbackRuleInputDto);

        // Act
        DrHoldbackRuleOutputDto updatedDrHoldbackRuleOutputDto = RightsModuleFacade.updateDrHoldbackRule(createdDrHoldbackRuleOutputDto.getId(), updateDrHoldbackRuleInputDto);
        auditLogEntriesForObject = storage.getAuditLogByObjectId(updatedDrHoldbackRuleOutputDto.getId());

        // Assert
        assertTrue(updatedDrHoldbackRuleOutputDto.getId() > 0L);
        assertEquals(updateDrHoldbackRuleInputDto.getDrHoldbackValue(), updatedDrHoldbackRuleOutputDto.getDrHoldbackValue());
        assertEquals(updateDrHoldbackRuleInputDto.getName(), updatedDrHoldbackRuleOutputDto.getName());
        assertEquals(updateDrHoldbackRuleInputDto.getDays(), updatedDrHoldbackRuleOutputDto.getDays());

        assertEquals(2, auditLogEntriesForObject.size());
        AuditLogEntryOutputDto auditLogEntryOutputDto = auditLogEntriesForObject.get(0);

        assertTrue(auditLogEntryOutputDto.getId() > 0L);
        assertEquals(updatedDrHoldbackRuleOutputDto.getId(), auditLogEntryOutputDto.getObjectId());
        assertTrue(auditLogEntryOutputDto.getModifiedTime() > 0L); //modifiedtime has been set
        assertEquals(userName, auditLogEntryOutputDto.getUserName());
        assertEquals(ChangeTypeEnumDto.UPDATE, auditLogEntryOutputDto.getChangeType());
        assertEquals(ObjectTypeEnumDto.HOLDBACK_DAY, auditLogEntryOutputDto.getChangeName());
        assertEquals(drHoldBackValue, auditLogEntryOutputDto.getIdentifier());
        assertEquals(changeComment, auditLogEntryOutputDto.getChangeComment());
        assertEquals(createdDrHoldbackRuleOutputDto.toString(), auditLogEntryOutputDto.getTextBefore());
        assertEquals(updatedDrHoldbackRuleOutputDto.toString(), auditLogEntryOutputDto.getTextAfter());
    }

    @Test
    public void deleteDrHoldbackRule_whenValidDrHoldbackValue_thenDeleteRule() throws SQLException {
        // Arrange
        String changeComment = "DR holdback rule skal ikke længere bruges";
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        deleteReasonDto.setChangeComment(changeComment);

        DrHoldbackRuleOutputDto drHoldbackRuleOutputDto = RightsModuleFacade.createDrHoldbackRule(drHoldbackRuleInputDto);

        // Act
        RecordsCountDto recordsCountDto = RightsModuleFacade.deleteDrHoldbackRule(drHoldbackRuleOutputDto.getId(), deleteReasonDto);

        // Make sure that the DR holdback rule is deleted
        Exception exception = assertThrows(InternalServiceException.class, () -> RightsModuleFacade.getDrHoldbackRuleByDrHoldbackValue(drHoldBackValue));

        auditLogEntriesForObject = storage.getAuditLogByObjectId(drHoldbackRuleOutputDto.getId());

        // Assert
        assertTrue(exception.getMessage().contains("DR holdback rule not found for drHoldbackValue: " + drHoldBackValue));

        assertEquals(1, recordsCountDto.getCount());

        assertEquals(2, auditLogEntriesForObject.size());
        AuditLogEntryOutputDto auditLogEntryOutputDto = auditLogEntriesForObject.get(0);

        assertTrue(auditLogEntryOutputDto.getId() > 0L);
        assertEquals(drHoldbackRuleOutputDto.getId(), auditLogEntryOutputDto.getObjectId());
        assertTrue(auditLogEntryOutputDto.getModifiedTime() > 0L); //modifiedtime has been set
        assertEquals(userName, auditLogEntryOutputDto.getUserName());
        assertEquals(ChangeTypeEnumDto.DELETE, auditLogEntryOutputDto.getChangeType());
        assertEquals(ObjectTypeEnumDto.HOLDBACK_RULE, auditLogEntryOutputDto.getChangeName());
        assertEquals(drHoldBackValue, auditLogEntryOutputDto.getIdentifier());
        assertEquals(changeComment, auditLogEntryOutputDto.getChangeComment());
        assertEquals(drHoldbackRuleOutputDto.toString(), auditLogEntryOutputDto.getTextBefore());
        assertNull(auditLogEntryOutputDto.getTextAfter());
    }

    @Test
    public void createDrHoldbackRanges_whenInvalidDrHoldBackValue_thenThrowNotFoundServiceException() {
        // Arrange
        String invalidDrHoldbackValue = "invalid";
        DrHoldbackRangeInputDto drHoldbackRangeInputDto = new DrHoldbackRangeInputDto();
        drHoldbackRangeInputDto.setDrHoldbackValue(invalidDrHoldbackValue);

        List<DrHoldbackRangeDto> ranges = List.of(drHoldbackRangeDtoOne, drHoldbackRangeDtoTwo);

        drHoldbackRangeInputDto.setRanges(ranges);

        RightsModuleFacade.createDrHoldbackRule(drHoldbackRuleInputDto);

        // Act
        Exception exception = assertThrows(InternalServiceException.class, () -> RightsModuleFacade.createDrHoldbackRanges(drHoldbackRangeInputDto));

        // Assert
        assertTrue(exception.getMessage().contains("DR holdback rule not found for drHoldbackValue: " + invalidDrHoldbackValue));
    }

    @Test
    public void createDrHoldbackRanges_whenDrHoldbackRangeInputDto_thenCreatesRanges() throws SQLException {
        // Arrange
        DrHoldbackRangeInputDto drHoldbackRangeInputDto = new DrHoldbackRangeInputDto();
        drHoldbackRangeInputDto.setDrHoldbackValue(drHoldBackValue);

        List<DrHoldbackRangeDto> ranges = List.of(drHoldbackRangeDtoOne, drHoldbackRangeDtoTwo);

        drHoldbackRangeInputDto.setRanges(ranges);

        RightsModuleFacade.createDrHoldbackRule(drHoldbackRuleInputDto);

        // Act
        List<DrHoldbackRangeOutputDto> drHoldbackRangeOutputDtoList = RightsModuleFacade.createDrHoldbackRanges(drHoldbackRangeInputDto);

        // Assert
        assertEquals(drHoldbackRangeInputDto.getRanges().size(), drHoldbackRangeOutputDtoList.size());

        for (int i = 0; i < drHoldbackRangeOutputDtoList.size(); i++) {
            assertEquals(drHoldbackRangeInputDto.getDrHoldbackValue(), drHoldbackRangeOutputDtoList.get(i).getDrHoldbackValue());
            assertEquals(drHoldbackRangeInputDto.getRanges().get(i).getContentRangeFrom(), drHoldbackRangeOutputDtoList.get(i).getContentRangeFrom());
            assertEquals(drHoldbackRangeInputDto.getRanges().get(i).getContentRangeTo(), drHoldbackRangeOutputDtoList.get(i).getContentRangeTo());
            assertEquals(drHoldbackRangeInputDto.getRanges().get(i).getFormRangeFrom(), drHoldbackRangeOutputDtoList.get(i).getFormRangeFrom());
            assertEquals(drHoldbackRangeInputDto.getRanges().get(i).getFormRangeTo(), drHoldbackRangeOutputDtoList.get(i).getFormRangeTo());

            auditLogEntriesForObject = storage.getAuditLogByObjectId(drHoldbackRangeOutputDtoList.get(i).getId());
            AuditLogEntryOutputDto auditLogEntryOutputDto = auditLogEntriesForObject.get(0);

            assertTrue(auditLogEntryOutputDto.getId() > 0L);
            assertEquals(drHoldbackRangeOutputDtoList.get(i).getId(), auditLogEntryOutputDto.getObjectId());
            assertTrue(auditLogEntryOutputDto.getModifiedTime() > 0L); //modifiedtime has been set
            assertEquals(userName, auditLogEntryOutputDto.getUserName());
            assertEquals(ChangeTypeEnumDto.CREATE, auditLogEntryOutputDto.getChangeType());
            assertEquals(ObjectTypeEnumDto.HOLDBACK_RANGE, auditLogEntryOutputDto.getChangeName());
            assertEquals(drHoldBackValue, auditLogEntryOutputDto.getIdentifier());
            assertNull(auditLogEntryOutputDto.getChangeComment());
            assertNull(auditLogEntryOutputDto.getTextBefore());
            assertEquals(ranges.get(i).toString(), auditLogEntryOutputDto.getTextAfter());
        }
    }

    @Test
    public void deleteRangesForDrHoldbackValue_whenInvalidDrHoldBackValue_thenThrowNotFoundServiceException() throws SQLException {
        // Arrange
        String invalidDrHoldbackValue = "invalid";
        String changeComment = "DR holdback ranges skal ikke længere bruges";
        DrHoldbackRangeInputDto drHoldbackRangeInputDto = new DrHoldbackRangeInputDto();
        drHoldbackRangeInputDto.setDrHoldbackValue(drHoldBackValue);
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        deleteReasonDto.setChangeComment(changeComment);

        List<DrHoldbackRangeDto> ranges = List.of(drHoldbackRangeDtoOne, drHoldbackRangeDtoTwo);

        drHoldbackRangeInputDto.setRanges(ranges);

        RightsModuleFacade.createDrHoldbackRule(drHoldbackRuleInputDto);

        RightsModuleFacade.createDrHoldbackRanges(drHoldbackRangeInputDto);

        // Act
        Exception exception = assertThrows(InternalServiceException.class, () -> RightsModuleFacade.deleteDrHoldbackRanges(invalidDrHoldbackValue, deleteReasonDto));

        // Assert
        assertTrue(exception.getMessage().contains("DR holdback ranges not found for drHoldbackValue: " + invalidDrHoldbackValue));
    }

    @Test
    public void deleteRangesForDrHoldbackValue_whenValidDrHoldBackValue_thenDeletesAllRanges() throws SQLException {
        // Arrange
        String changeComment = "DR holdback ranges skal ikke længere bruges";
        DrHoldbackRangeInputDto drHoldbackRangeInputDto = new DrHoldbackRangeInputDto();
        drHoldbackRangeInputDto.setDrHoldbackValue(drHoldBackValue);
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        deleteReasonDto.setChangeComment(changeComment);

        List<DrHoldbackRangeDto> ranges = List.of(drHoldbackRangeDtoOne, drHoldbackRangeDtoTwo);

        drHoldbackRangeInputDto.setRanges(ranges);

        DrHoldbackRuleOutputDto drHoldbackRuleOutputDto = RightsModuleFacade.createDrHoldbackRule(drHoldbackRuleInputDto);

        List<DrHoldbackRangeOutputDto> drHoldbackRangeOutputDtoList = RightsModuleFacade.createDrHoldbackRanges(drHoldbackRangeInputDto);

        // Act
        RecordsCountDto recordsCountDto = RightsModuleFacade.deleteDrHoldbackRanges(drHoldBackValue, deleteReasonDto);

        // Make sure that all DR holdback ranges is deleted
        Exception exception = assertThrows(InternalServiceException.class, () -> RightsModuleFacade.getDrHoldbackRanges(drHoldBackValue));
        auditLogEntriesForObject = storage.getAuditLogByObjectId(drHoldbackRuleOutputDto.getId());

        // Assert
        assertTrue(exception.getMessage().contains("DR holdback ranges not found for drHoldbackValue: " + drHoldBackValue));

        assertEquals(2, recordsCountDto.getCount());

        assertEquals(1, auditLogEntriesForObject.size());

        for (DrHoldbackRangeOutputDto drHoldbackRangeOutputDto : drHoldbackRangeOutputDtoList) {
            auditLogEntriesForObject = storage.getAuditLogByObjectId(drHoldbackRangeOutputDto.getId());
            assertEquals(2, auditLogEntriesForObject.size());

            AuditLogEntryOutputDto auditLogEntryOutputDto = auditLogEntriesForObject.get(0);

            assertTrue(auditLogEntryOutputDto.getId() > 0L);
            assertEquals(drHoldbackRangeOutputDto.getId(), auditLogEntryOutputDto.getObjectId());
            assertTrue(auditLogEntriesForObject.get(1).getModifiedTime() < auditLogEntryOutputDto.getModifiedTime());
            assertEquals(userName, auditLogEntryOutputDto.getUserName());
            assertEquals(ChangeTypeEnumDto.DELETE, auditLogEntryOutputDto.getChangeType());
            assertEquals(ObjectTypeEnumDto.HOLDBACK_RANGE, auditLogEntryOutputDto.getChangeName());
            assertEquals(drHoldbackRangeInputDto.getDrHoldbackValue(), auditLogEntryOutputDto.getIdentifier());
            assertEquals(changeComment, auditLogEntryOutputDto.getChangeComment());
            assertEquals(drHoldbackRangeOutputDto.toString(), auditLogEntryOutputDto.getTextBefore());
            assertNull(auditLogEntryOutputDto.getTextAfter());
        }
    }
}
