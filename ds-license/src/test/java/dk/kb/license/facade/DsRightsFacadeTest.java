package dk.kb.license.facade;

import dk.kb.license.config.ServiceConfig;
import dk.kb.license.model.v1.*;
import dk.kb.license.solr.SolrServerClient;
import dk.kb.license.storage.BaseModuleStorage;
import dk.kb.license.storage.DsLicenseUnitTestUtil;
import dk.kb.license.storage.RightsModuleStorageForUnitTest;
import dk.kb.license.util.H2DbUtil;
import dk.kb.license.webservice.KBAuthorizationInterceptor;
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

public class DsRightsFacadeTest extends DsLicenseUnitTestUtil {
    protected static RightsModuleStorageForUnitTest storage = null;

    final String drHoldBackValue = "2.02";
    final String drHoldBackName = "Aktualitet og Debat";
    final int drHoldbackDays = 2190;
    final String userName = "mockedName";

    DrHoldbackRuleInputDto drHoldbackRuleInputDto = new DrHoldbackRuleInputDto();
    DrHoldbackRangesDto drHoldbackRangesDtoOne = new DrHoldbackRangesDto();
    DrHoldbackRangesDto drHoldbackRangesDtoTwo = new DrHoldbackRangesDto();

    List<AuditEntryOutputDto> auditEntryOutputDtoList;

    static MockedStatic<JAXRSUtils> mocked;

    @BeforeAll
    public static void beforeClass() throws IOException, SQLException {
        ServiceConfig.initialize("conf/ds-license*.yaml");
        H2DbUtil.createEmptyH2DBFromDDL(URL, DRIVER, USERNAME, PASSWORD, List.of("ddl/rightsmodule_create_h2_unittest.ddl"));
        BaseModuleStorage.initialize(DRIVER, URL, USERNAME, PASSWORD);

        storage = new RightsModuleStorageForUnitTest();

        MessageImpl message = new MessageImpl();
        AccessToken mockedToken = mock(AccessToken.class);
        when(mockedToken.getName()).thenReturn("mockedName");
        message.put(KBAuthorizationInterceptor.ACCESS_TOKEN, mockedToken);

        mocked = mockStatic(JAXRSUtils.class);
        mocked.when(JAXRSUtils::getCurrentMessage).thenReturn(message);
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

        drHoldbackRangesDtoOne.setContentRangeFrom(1000);
        drHoldbackRangesDtoOne.setContentRangeTo(1900);
        drHoldbackRangesDtoOne.setFormRangeFrom(1000);
        drHoldbackRangesDtoOne.setFormRangeTo(1000);

        drHoldbackRangesDtoTwo.setContentRangeFrom(1000);
        drHoldbackRangesDtoTwo.setContentRangeTo(1900);
        drHoldbackRangesDtoTwo.setFormRangeFrom(1200);
        drHoldbackRangesDtoTwo.setFormRangeTo(1500);
    }

    /**
     * Close the MockedStatic JAXRSUtils.class when the tests are done, so it don't interfere with other test classes
     */
    @AfterAll
    public static void afterClass() {
        mocked.close();
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
        RestrictedIdInputDto restrictedIdInputDto = new RestrictedIdInputDto();
        restrictedIdInputDto.setIdValue("4b35ee6f-b7d3-4fee-8936-a067b42eb9ef");
        restrictedIdInputDto.setIdType(IdTypeEnumDto.DS_ID);
        restrictedIdInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        restrictedIdInputDto.setComment("Brugeren har trukket deres samtykke tilbage");

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> RightsModuleFacade.createRestrictedId(restrictedIdInputDto, false));
        auditEntryOutputDtoList = storage.getAllAudit();

        // Assert
        assertEquals("Invalid dsId: 4b35ee6f-b7d3-4fee-8936-a067b42eb9ef", exception.getMessage());

        // Only valid RestrictedIdInputDto objects is in the audit log
        assertEquals(0, auditEntryOutputDtoList.size());
    }

    @Test
    public void createRestrictedId_whenInvalidDrProductionId_thenThrowInvalidArgumentServiceException() throws SQLException {
        // Arrange
        RestrictedIdInputDto restrictedIdInputDto = new RestrictedIdInputDto();
        restrictedIdInputDto.setIdValue("1234567");
        restrictedIdInputDto.setIdType(IdTypeEnumDto.DR_PRODUCTION_ID);
        restrictedIdInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        restrictedIdInputDto.setComment("Brugeren har trukket deres samtykke tilbage");

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> RightsModuleFacade.createRestrictedId(restrictedIdInputDto, false));
        auditEntryOutputDtoList = storage.getAllAudit();

        // Assert
        assertEquals("The input DR production ID: 1234567 should be at least 8 digits", exception.getMessage());

        // Only valid RestrictedIdInputDto objects is in the audit log
        assertEquals(0, auditEntryOutputDtoList.size());
    }

    @Test
    public void createRestrictedId_whenInvalidComment_thenThrowInvalidArgumentServiceException() throws SQLException {
        // Arrange
        RestrictedIdInputDto restrictedIdInputDto = new RestrictedIdInputDto();
        restrictedIdInputDto.setIdValue("12345678");
        restrictedIdInputDto.setIdType(IdTypeEnumDto.DR_PRODUCTION_ID);
        restrictedIdInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        restrictedIdInputDto.setComment(null);

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> RightsModuleFacade.createRestrictedId(restrictedIdInputDto, false));
        auditEntryOutputDtoList = storage.getAllAudit();

        // Assert
        assertEquals("Comment cannot be empty", exception.getMessage());

        // Only valid RestrictedIdInputDto objects is in the audit log
        assertEquals(0, auditEntryOutputDtoList.size());
    }

    @Test
    public void createRestrictedId_whenValidRestrictedIdInputDto_thenReturnRestrictedIdOutputDto() throws SQLException {
        // Arrange
        String drProductionId = "12345678";
        String comment = "Brugeren har trukket deres samtykke tilbage";

        RestrictedIdInputDto restrictedIdInputDto = new RestrictedIdInputDto();
        restrictedIdInputDto.setIdValue(drProductionId);
        restrictedIdInputDto.setIdType(IdTypeEnumDto.DR_PRODUCTION_ID);
        restrictedIdInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        restrictedIdInputDto.setComment(comment);

        // Act
        RestrictedIdOutputDto restrictedIdOutputDto = RightsModuleFacade.createRestrictedId(restrictedIdInputDto, false);
        auditEntryOutputDtoList = storage.getAllAudit();

        // Assert
        assertNotNull(restrictedIdOutputDto);
        assertNotNull(restrictedIdOutputDto.getId());
        assertEquals(drProductionId, restrictedIdOutputDto.getIdValue());
        assertEquals(IdTypeEnumDto.DR_PRODUCTION_ID, restrictedIdOutputDto.getIdType());
        assertEquals(PlatformEnumDto.DRARKIV, restrictedIdOutputDto.getPlatform());
        assertEquals(comment, restrictedIdOutputDto.getComment());

        // Only valid RestrictedIdInputDto objects is in the audit log
        AuditEntryOutputDto drProductionIdAuditEntryOutputDto = auditEntryOutputDtoList.get(0);
        assertEquals(ChangeTypeEnumDto.CREATE, drProductionIdAuditEntryOutputDto.getChangeType());
        assertEquals(ObjectTypeEnumDto.DR_PRODUCTION_ID, drProductionIdAuditEntryOutputDto.getChangeName());
        assertNull(drProductionIdAuditEntryOutputDto.getTextBefore());
        assertEquals(restrictedIdInputDto.toString(), drProductionIdAuditEntryOutputDto.getTextAfter());
        assertEquals(userName, drProductionIdAuditEntryOutputDto.getUserName());
        assertEquals(drProductionId, drProductionIdAuditEntryOutputDto.getChangeComment());
    }

    @Test
    public void createRestrictedIds_whenAllRestrictedIdInputDtoHasErrors_thenCreationStatusDtoIsFailedAndFailedIdsList() throws SQLException {
        // Arrange
        RestrictedIdInputDto restrictedIdInputDto = new RestrictedIdInputDto();
        restrictedIdInputDto.setIdValue("4b35ee6f-b7d3-4fee-8936-a067b42eb9ef");
        restrictedIdInputDto.setIdType(IdTypeEnumDto.DS_ID);
        restrictedIdInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        restrictedIdInputDto.setComment("Brugeren har trukket deres samtykke tilbage");

        List<RestrictedIdInputDto> restrictedIds = new ArrayList<>();
        restrictedIds.add(restrictedIdInputDto);

        // Act
        ProcessedRestrictedIdsOutputDto processedRestrictedIdsOutputDto = RightsModuleFacade.createRestrictedIds(restrictedIds, false);
        auditEntryOutputDtoList = storage.getAllAudit();

        // Assert
        assertNotNull(processedRestrictedIdsOutputDto);
        assertEquals(CreationStatusDto.FAILED, processedRestrictedIdsOutputDto.getCreationStatus());
        assertEquals(0, processedRestrictedIdsOutputDto.getCreatedSuccessfully());
        assertEquals(1, processedRestrictedIdsOutputDto.getFailedRestrictedIds().size());
        assertEquals(restrictedIdInputDto.getIdValue(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getIdValue());
        assertEquals(restrictedIdInputDto.getIdType(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getIdType());
        assertEquals(restrictedIdInputDto.getPlatform(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getPlatform());
        assertEquals(restrictedIdInputDto.getComment(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getComment());
        assertEquals("InvalidArgumentServiceException", processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getException());
        assertEquals("Invalid dsId: 4b35ee6f-b7d3-4fee-8936-a067b42eb9ef", processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getErrorMessage());

        // Only valid RestrictedIdInputDto objects is in the audit log
        assertEquals(0, auditEntryOutputDtoList.size());
    }

    @Test
    public void createRestrictedIds_whenInvalidDrProductionId_thenCreationStatusDtoIsFailedAndFailedIdsList() throws SQLException {
        // Arrange
        RestrictedIdInputDto restrictedIdInputDto = new RestrictedIdInputDto();
        restrictedIdInputDto.setIdValue("1234567");
        restrictedIdInputDto.setIdType(IdTypeEnumDto.DR_PRODUCTION_ID);
        restrictedIdInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        restrictedIdInputDto.setComment("Brugeren har trukket deres samtykke tilbage");

        List<RestrictedIdInputDto> restrictedIds = new ArrayList<>();
        restrictedIds.add(restrictedIdInputDto);

        // Act
        ProcessedRestrictedIdsOutputDto processedRestrictedIdsOutputDto = RightsModuleFacade.createRestrictedIds(restrictedIds, false);
        auditEntryOutputDtoList = storage.getAllAudit();

        // Assert
        assertNotNull(processedRestrictedIdsOutputDto);
        assertEquals(CreationStatusDto.FAILED, processedRestrictedIdsOutputDto.getCreationStatus());
        assertEquals(0, processedRestrictedIdsOutputDto.getCreatedSuccessfully());
        assertEquals(1, processedRestrictedIdsOutputDto.getFailedRestrictedIds().size());
        assertEquals(restrictedIdInputDto.getIdValue(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getIdValue());
        assertEquals(restrictedIdInputDto.getIdType(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getIdType());
        assertEquals(restrictedIdInputDto.getPlatform(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getPlatform());
        assertEquals(restrictedIdInputDto.getComment(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getComment());
        assertEquals("InvalidArgumentServiceException", processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getException());
        assertEquals("The input DR production ID: 1234567 should be at least 8 digits", processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getErrorMessage());

        // Only valid RestrictedIdInputDto objects is in the audit log
        assertEquals(0, auditEntryOutputDtoList.size());
    }

    @Test
    public void createRestrictedIds_whenCommentIsNull_thenCreationStatusDtoIsFailedAndFailedIdsList() throws SQLException {
        // Arrange
        RestrictedIdInputDto restrictedIdInputDto = new RestrictedIdInputDto();
        restrictedIdInputDto.setIdValue("ds.tv:oai:io:4b35ee6f-b7d3-4fee-8936-a067b42eb9ef");
        restrictedIdInputDto.setIdType(IdTypeEnumDto.DS_ID);
        restrictedIdInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        restrictedIdInputDto.setComment(null);

        List<RestrictedIdInputDto> restrictedIds = new ArrayList<>();
        restrictedIds.add(restrictedIdInputDto);

        // Act
        ProcessedRestrictedIdsOutputDto processedRestrictedIdsOutputDto = RightsModuleFacade.createRestrictedIds(restrictedIds, false);
        auditEntryOutputDtoList = storage.getAllAudit();

        // Assert
        assertNotNull(processedRestrictedIdsOutputDto);
        assertEquals(CreationStatusDto.FAILED, processedRestrictedIdsOutputDto.getCreationStatus());
        assertEquals(0, processedRestrictedIdsOutputDto.getCreatedSuccessfully());
        assertEquals(1, processedRestrictedIdsOutputDto.getFailedRestrictedIds().size());
        assertEquals(restrictedIdInputDto.getIdValue(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getIdValue());
        assertEquals(restrictedIdInputDto.getIdType(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getIdType());
        assertEquals(restrictedIdInputDto.getPlatform(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getPlatform());
        assertEquals(restrictedIdInputDto.getComment(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getComment());
        assertEquals("InvalidArgumentServiceException", processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getException());
        assertEquals("Comment cannot be empty", processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getErrorMessage());

        // Only valid RestrictedIdInputDto objects is in the audit log
        assertEquals(0, auditEntryOutputDtoList.size());
    }

    @Test
    public void createRestrictedIds_whenValidRestrictedIdsList_thenCreationStatusDtoIsSuccessAndEmptyFailedIdsList() throws SQLException {
        // Arrange
        String dsId = "ds.tv:oai:io:ea440a12-d14b-46cd-b6b9-53b16ee56111";
        String drProductionId = "12345678";
        String comment = "Brugeren har trukket deres samtykke tilbage";

        RestrictedIdInputDto dsIdRestrictedIdInputDto = new RestrictedIdInputDto();
        dsIdRestrictedIdInputDto.setIdValue(dsId);
        dsIdRestrictedIdInputDto.setIdType(IdTypeEnumDto.DS_ID);
        dsIdRestrictedIdInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        dsIdRestrictedIdInputDto.setComment(comment);

        RestrictedIdInputDto drProductionIdRestrictedIdInputDto = new RestrictedIdInputDto();
        drProductionIdRestrictedIdInputDto.setIdValue(drProductionId);
        drProductionIdRestrictedIdInputDto.setIdType(IdTypeEnumDto.DR_PRODUCTION_ID);
        drProductionIdRestrictedIdInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        drProductionIdRestrictedIdInputDto.setComment(comment);

        List<RestrictedIdInputDto> restrictedIds = new ArrayList<>();
        restrictedIds.add(dsIdRestrictedIdInputDto);
        restrictedIds.add(drProductionIdRestrictedIdInputDto);

        // Act
        ProcessedRestrictedIdsOutputDto processedRestrictedIdsOutputDto = RightsModuleFacade.createRestrictedIds(restrictedIds, false);
        auditEntryOutputDtoList = storage.getAllAudit();

        // Assert
        assertNotNull(processedRestrictedIdsOutputDto);
        assertEquals(CreationStatusDto.SUCCESS, processedRestrictedIdsOutputDto.getCreationStatus());
        assertEquals(2, processedRestrictedIdsOutputDto.getCreatedSuccessfully());
        assertTrue(processedRestrictedIdsOutputDto.getFailedRestrictedIds().isEmpty());

        // Only valid RestrictedIdInputDto objects is in the audit log
        assertEquals(restrictedIds.size(), auditEntryOutputDtoList.size());

        AuditEntryOutputDto drProductionIdAuditEntryOutputDto = auditEntryOutputDtoList.get(0);
        assertEquals(ChangeTypeEnumDto.CREATE, drProductionIdAuditEntryOutputDto.getChangeType());
        assertEquals(ObjectTypeEnumDto.DR_PRODUCTION_ID, drProductionIdAuditEntryOutputDto.getChangeName());
        assertNull(drProductionIdAuditEntryOutputDto.getTextBefore());
        assertEquals(drProductionIdRestrictedIdInputDto.toString(), drProductionIdAuditEntryOutputDto.getTextAfter());
        assertEquals(userName, drProductionIdAuditEntryOutputDto.getUserName());
        assertEquals(drProductionId, drProductionIdAuditEntryOutputDto.getChangeComment());

        AuditEntryOutputDto dsIdAuditEntryOutputDto = auditEntryOutputDtoList.get(1);
        assertEquals(ChangeTypeEnumDto.CREATE, dsIdAuditEntryOutputDto.getChangeType());
        assertEquals(ObjectTypeEnumDto.DS_ID, dsIdAuditEntryOutputDto.getChangeName());
        assertNull(dsIdAuditEntryOutputDto.getTextBefore());
        assertEquals(dsIdRestrictedIdInputDto.toString(), dsIdAuditEntryOutputDto.getTextAfter());
        assertEquals(userName, dsIdAuditEntryOutputDto.getUserName());
        assertEquals(dsId, dsIdAuditEntryOutputDto.getChangeComment());
    }

    @Test
    public void createRestrictedIds_whenValidAndInvalidDsId_thenCreationStatusDtoIsPartialProcessedAndFailedIdsList() throws SQLException {
        // Arrange
        String validDsId = "ds.tv:oai:io:ea440a12-d14b-46cd-b6b9-53b16ee56111";
        String comment = "Brugeren har trukket deres samtykke tilbage";

        RestrictedIdInputDto validRestrictedIdInputDto = new RestrictedIdInputDto();
        validRestrictedIdInputDto.setIdValue(validDsId);
        validRestrictedIdInputDto.setIdType(IdTypeEnumDto.DS_ID);
        validRestrictedIdInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        validRestrictedIdInputDto.setComment(comment);

        RestrictedIdInputDto invalidRestrictedIdInputDto = new RestrictedIdInputDto();
        invalidRestrictedIdInputDto.setIdValue("4b35ee6f-b7d3-4fee-8936-a067b42eb9ef");
        invalidRestrictedIdInputDto.setIdType(IdTypeEnumDto.DS_ID);
        invalidRestrictedIdInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        invalidRestrictedIdInputDto.setComment(comment);

        List<RestrictedIdInputDto> restrictedIds = new ArrayList<>();
        restrictedIds.add(validRestrictedIdInputDto);
        restrictedIds.add(invalidRestrictedIdInputDto);

        // Act
        ProcessedRestrictedIdsOutputDto processedRestrictedIdsOutputDto = RightsModuleFacade.createRestrictedIds(restrictedIds, false);
        auditEntryOutputDtoList = storage.getAllAudit();

        // Assert
        assertNotNull(processedRestrictedIdsOutputDto);
        assertEquals(CreationStatusDto.PARTIAL_PROCESSED, processedRestrictedIdsOutputDto.getCreationStatus());
        assertEquals(1, processedRestrictedIdsOutputDto.getCreatedSuccessfully());
        assertEquals(1, processedRestrictedIdsOutputDto.getFailedRestrictedIds().size());
        assertEquals(invalidRestrictedIdInputDto.getIdValue(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getIdValue());
        assertEquals(invalidRestrictedIdInputDto.getIdType(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getIdType());
        assertEquals(invalidRestrictedIdInputDto.getPlatform(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getPlatform());
        assertEquals(invalidRestrictedIdInputDto.getComment(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getComment());
        assertEquals("InvalidArgumentServiceException", processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getException());
        assertEquals("Invalid dsId: 4b35ee6f-b7d3-4fee-8936-a067b42eb9ef", processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getErrorMessage());

        // Only valid RestrictedIdInputDto objects is in the audit log
        assertEquals(1, auditEntryOutputDtoList.size());
        AuditEntryOutputDto auditEntryOutputDto = auditEntryOutputDtoList.get(0);
        assertEquals(ChangeTypeEnumDto.CREATE, auditEntryOutputDto.getChangeType());
        assertEquals(ObjectTypeEnumDto.DS_ID, auditEntryOutputDto.getChangeName());
        assertNull(auditEntryOutputDto.getTextBefore());
        assertEquals(validRestrictedIdInputDto.toString(), auditEntryOutputDto.getTextAfter());
        assertEquals(userName, auditEntryOutputDto.getUserName());
        assertEquals(validDsId, auditEntryOutputDto.getChangeComment());
    }

    @Test
    public void createRestrictedIds_whenAlreadyExistingRestrictedId_thenCreationStatusDtoIsPartialProcessedAndFailedIdsList() throws SQLException {
        // Arrange
        String validDsId = "ds.tv:oai:io:ea440a12-d14b-46cd-b6b9-53b16ee56111";
        String comment = "Brugeren har trukket deres samtykke tilbage";

        RestrictedIdInputDto restrictedIdInputDto = new RestrictedIdInputDto();
        restrictedIdInputDto.setIdValue(validDsId);
        restrictedIdInputDto.setIdType(IdTypeEnumDto.DS_ID);
        restrictedIdInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        restrictedIdInputDto.setComment(comment);

        RestrictedIdInputDto duplicatedRestrictedIdInputDto = new RestrictedIdInputDto();
        duplicatedRestrictedIdInputDto.setIdValue(validDsId);
        duplicatedRestrictedIdInputDto.setIdType(IdTypeEnumDto.DS_ID);
        duplicatedRestrictedIdInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        duplicatedRestrictedIdInputDto.setComment(comment);

        List<RestrictedIdInputDto> restrictedIds = new ArrayList<>();
        restrictedIds.add(restrictedIdInputDto);
        restrictedIds.add(duplicatedRestrictedIdInputDto);

        // Act
        ProcessedRestrictedIdsOutputDto processedRestrictedIdsOutputDto = RightsModuleFacade.createRestrictedIds(restrictedIds, false);
        auditEntryOutputDtoList = storage.getAllAudit();

        // Assert
        assertNotNull(processedRestrictedIdsOutputDto);
        assertEquals(CreationStatusDto.PARTIAL_PROCESSED, processedRestrictedIdsOutputDto.getCreationStatus());
        assertEquals(1, processedRestrictedIdsOutputDto.getCreatedSuccessfully());
        assertEquals(1, processedRestrictedIdsOutputDto.getFailedRestrictedIds().size());
        assertEquals(duplicatedRestrictedIdInputDto.getIdValue(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getIdValue());
        assertEquals(duplicatedRestrictedIdInputDto.getIdType(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getIdType());
        assertEquals(duplicatedRestrictedIdInputDto.getPlatform(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getPlatform());
        assertEquals(duplicatedRestrictedIdInputDto.getComment(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getComment());
        assertEquals("InternalServiceException", processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getException());
        assertTrue(processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getErrorMessage().startsWith("dk.kb.util.webservice.exception.InternalServiceException: org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException: Unique index or primary key violation"));

        // Only valid RestrictedIdInputDto objects is in the audit log
        assertEquals(1, auditEntryOutputDtoList.size());
        AuditEntryOutputDto auditEntryOutputDto = auditEntryOutputDtoList.get(0);
        assertEquals(ChangeTypeEnumDto.CREATE, auditEntryOutputDto.getChangeType());
        assertEquals(ObjectTypeEnumDto.DS_ID, auditEntryOutputDto.getChangeName());
        assertNull(auditEntryOutputDto.getTextBefore());
        assertEquals(restrictedIdInputDto.toString(), auditEntryOutputDto.getTextAfter());
        assertEquals(userName, auditEntryOutputDto.getUserName());
        assertEquals(validDsId, auditEntryOutputDto.getChangeComment());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
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
    public void matchingDrProductionIdBroadcasts_whenDsIdTWithDrProductionIdButThereIsNoMatchOnDrProductionId_thenThrowNotFoundServiceException() throws ParseException, SolrServerException, IOException {
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
    public void matchingDrProductionIdBroadcasts_whenDsIdWithNoDrProductionId_thenReturnDrBroadcastDto() throws ParseException, SolrServerException, IOException {
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
    public void matchingDrProductionIdBroadcasts_whenDsIdWithNoDrProductionIdAndRestrictedComment_thenReturnDrBroadcastDto() throws ParseException, SolrServerException, IOException {
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
    public void matchingDrProductionIdBroadcasts_whenDsIdWithDrProductionId_thenReturnDrBroadcastDto() throws ParseException, SolrServerException, IOException {
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
    public void createDrHoldbackRule_whenDrHoldbackRuleInputDto_thenCreateRule() throws SQLException {
        long drHoldbackRuleId = RightsModuleFacade.createDrHoldbackRule(drHoldbackRuleInputDto);
        auditEntryOutputDtoList = storage.getAuditLogByObjectId(drHoldbackRuleId);

        assertEquals(1, auditEntryOutputDtoList.size());
        AuditEntryOutputDto createDrHoldbackRuleAuditLog = auditEntryOutputDtoList.get(0);
        assertEquals(ChangeTypeEnumDto.CREATE, createDrHoldbackRuleAuditLog.getChangeType());
        assertEquals(ObjectTypeEnumDto.HOLDBACK_RULE, createDrHoldbackRuleAuditLog.getChangeName());
        assertNull(createDrHoldbackRuleAuditLog.getTextBefore());
        assertEquals(drHoldbackRuleInputDto.toString(), createDrHoldbackRuleAuditLog.getTextAfter());
        assertEquals(userName, createDrHoldbackRuleAuditLog.getUserName());
        assertEquals(drHoldbackRuleInputDto.getDrHoldbackValue(), createDrHoldbackRuleAuditLog.getChangeComment());
        assertEquals(drHoldbackRuleId, createDrHoldbackRuleAuditLog.getObjectId());
    }

    @Test
    public void createDrHoldbackRanges_whenDrHoldbackRangeInputDto_thenCreatesRanges() throws SQLException {
        DrHoldbackRangeInputDto drHoldbackRangeInputDto = new DrHoldbackRangeInputDto();
        drHoldbackRangeInputDto.setDrHoldbackValue(drHoldBackValue);

        List<DrHoldbackRangesDto> ranges = List.of(drHoldbackRangesDtoOne, drHoldbackRangesDtoTwo);

        drHoldbackRangeInputDto.setRanges(ranges);

        long drHoldbackRuleId = RightsModuleFacade.createDrHoldbackRule(drHoldbackRuleInputDto);

        List<Long> drHoldBackRangesIds = RightsModuleFacade.createDrHoldbackRanges(drHoldbackRangeInputDto);

        auditEntryOutputDtoList = storage.getAuditLogByObjectId(drHoldbackRuleId);

        assertEquals(2, drHoldBackRangesIds.size());

        for (Long drHoldBackRangesId : drHoldBackRangesIds) {

            auditEntryOutputDtoList = storage.getAuditLogByObjectId(drHoldBackRangesId);
            assertEquals(1, auditEntryOutputDtoList.size());
            AuditEntryOutputDto createDrHoldbackRangeAuditLog = auditEntryOutputDtoList.get(0);

            assertEquals(ChangeTypeEnumDto.CREATE, createDrHoldbackRangeAuditLog.getChangeType());
            assertEquals(ObjectTypeEnumDto.HOLDBACK_RANGE, createDrHoldbackRangeAuditLog.getChangeName());
            assertNull(createDrHoldbackRangeAuditLog.getTextBefore());
            assertEquals(ranges.toString(), createDrHoldbackRangeAuditLog.getTextAfter());
            assertEquals(userName, createDrHoldbackRangeAuditLog.getUserName());
            assertEquals(drHoldbackRangeInputDto.getDrHoldbackValue(), createDrHoldbackRangeAuditLog.getChangeComment());
            assertEquals(drHoldBackRangesId, createDrHoldbackRangeAuditLog.getObjectId());
        }
    }

    @Test
    public void updateDrHoldbackDaysFromDrHoldbackValue_whenDrHoldBackValue_thenUpdateDaysForRule() throws SQLException {
        int newDrHoldbackDays = 10;

        long drHoldbackRuleId = RightsModuleFacade.createDrHoldbackRule(drHoldbackRuleInputDto);

        auditEntryOutputDtoList = storage.getAuditLogByObjectId(drHoldbackRuleId);

        RightsModuleFacade.updateDrHoldbackDaysFromDrHoldbackValue(drHoldBackValue, newDrHoldbackDays);
        auditEntryOutputDtoList = storage.getAuditLogByObjectId(drHoldbackRuleId);
        assertEquals(2, auditEntryOutputDtoList.size());
        AuditEntryOutputDto updateDrHoldbackRuleAuditLogFromValue = auditEntryOutputDtoList.get(0);
        assertEquals(ChangeTypeEnumDto.UPDATE, updateDrHoldbackRuleAuditLogFromValue.getChangeType());
        assertEquals(ObjectTypeEnumDto.HOLDBACK_DAY, updateDrHoldbackRuleAuditLogFromValue.getChangeName());
        assertEquals("Days before: " + drHoldbackDays, updateDrHoldbackRuleAuditLogFromValue.getTextBefore());
        assertEquals("Days after: " + newDrHoldbackDays, updateDrHoldbackRuleAuditLogFromValue.getTextAfter());
        assertEquals(userName, updateDrHoldbackRuleAuditLogFromValue.getUserName());
        assertEquals(drHoldbackRuleInputDto.getDrHoldbackValue(), updateDrHoldbackRuleAuditLogFromValue.getChangeComment());
        assertEquals(drHoldbackRuleId, updateDrHoldbackRuleAuditLogFromValue.getObjectId());
    }

    @Test
    public void updateDrHoldbackDaysFromName_whenDrHoldBackName_thenUpdateDaysForRule() throws SQLException {
        int newDrHoldbackDays = 10;

        long drHoldbackRuleId = RightsModuleFacade.createDrHoldbackRule(drHoldbackRuleInputDto);

        auditEntryOutputDtoList = storage.getAuditLogByObjectId(drHoldbackRuleId);

        RightsModuleFacade.updateDrHoldbackDaysFromName(drHoldBackName, newDrHoldbackDays);
        auditEntryOutputDtoList = storage.getAuditLogByObjectId(drHoldbackRuleId);
        assertEquals(2, auditEntryOutputDtoList.size());
        AuditEntryOutputDto updateDrHoldbackRuleAuditLogFromName = auditEntryOutputDtoList.get(0);
        assertEquals(ChangeTypeEnumDto.UPDATE, updateDrHoldbackRuleAuditLogFromName.getChangeType());
        assertEquals("Days before: " + drHoldbackDays, updateDrHoldbackRuleAuditLogFromName.getTextBefore());
        assertEquals(ObjectTypeEnumDto.HOLDBACK_DAY, updateDrHoldbackRuleAuditLogFromName.getChangeName());
        assertEquals("Days after: " + newDrHoldbackDays, updateDrHoldbackRuleAuditLogFromName.getTextAfter());
        assertEquals(userName, updateDrHoldbackRuleAuditLogFromName.getUserName());
        assertEquals(drHoldbackRuleInputDto.getName(), updateDrHoldbackRuleAuditLogFromName.getChangeComment());
        assertEquals(drHoldbackRuleId, updateDrHoldbackRuleAuditLogFromName.getObjectId());
    }

    @Test
    public void deleteRangesForDrHoldbackValue_whenDrHoldBackValue_thenDeletesAllRanges() throws SQLException {
        DrHoldbackRangeInputDto drHoldbackRangeInputDto = new DrHoldbackRangeInputDto();
        drHoldbackRangeInputDto.setDrHoldbackValue(drHoldBackValue);

        List<DrHoldbackRangesDto> ranges = List.of(drHoldbackRangesDtoOne, drHoldbackRangesDtoTwo);

        drHoldbackRangeInputDto.setRanges(ranges);

        long drHoldbackRuleId = RightsModuleFacade.createDrHoldbackRule(drHoldbackRuleInputDto);

        List<Long> drHoldBackRangesIds = RightsModuleFacade.createDrHoldbackRanges(drHoldbackRangeInputDto);

        auditEntryOutputDtoList = storage.getAuditLogByObjectId(drHoldbackRuleId);

        RightsModuleFacade.deleteRangesForDrHoldbackValue(drHoldBackValue);

        for (Long drHoldBackRangesId : drHoldBackRangesIds) {
            auditEntryOutputDtoList = storage.getAuditLogByObjectId(drHoldBackRangesId);
            assertEquals(2, auditEntryOutputDtoList.size());
            AuditEntryOutputDto deleteDrHoldbackRangeAuditLog = auditEntryOutputDtoList.get(0);

            assertEquals(ChangeTypeEnumDto.DELETE, deleteDrHoldbackRangeAuditLog.getChangeType());
            assertEquals(ObjectTypeEnumDto.HOLDBACK_RANGE, deleteDrHoldbackRangeAuditLog.getChangeName());
            // assertEquals(ranges.toString(), deleteDrHoldbackRangeAuditLog.getTextBefore()); TODO: This should be fixed together with: DRA-2085
            assertNull(deleteDrHoldbackRangeAuditLog.getTextAfter());
            assertEquals(userName, deleteDrHoldbackRangeAuditLog.getUserName());
            assertEquals(drHoldbackRangeInputDto.getDrHoldbackValue(), deleteDrHoldbackRangeAuditLog.getChangeComment());
            assertEquals(drHoldBackRangesId, deleteDrHoldbackRangeAuditLog.getObjectId());

            assertTrue(auditEntryOutputDtoList.get(1).getModifiedTime() < deleteDrHoldbackRangeAuditLog.getModifiedTime());
        }
    }

    @Test
    public void deleteDrHoldbackRule_whenDrHoldbackRuleId_thenDeleteRule() throws SQLException {
        long drHoldbackRuleId = RightsModuleFacade.createDrHoldbackRule(drHoldbackRuleInputDto);

        auditEntryOutputDtoList = storage.getAuditLogByObjectId(drHoldbackRuleId);

        RightsModuleFacade.deleteDrHoldbackRule(drHoldBackValue);
        auditEntryOutputDtoList = storage.getAuditLogByObjectId(drHoldbackRuleId);

        assertEquals(2, auditEntryOutputDtoList.size());
        AuditEntryOutputDto deleteDrHoldbackRuleAuditLog = auditEntryOutputDtoList.get(0);
        assertEquals(ChangeTypeEnumDto.DELETE, deleteDrHoldbackRuleAuditLog.getChangeType());
        assertEquals(ObjectTypeEnumDto.HOLDBACK_RULE, deleteDrHoldbackRuleAuditLog.getChangeName());
        //assertEquals(drHoldbackRuleInputDto.toString(), deleteDrHoldbackRuleAuditLog.getTextBefore()); TODO: This should be fixed together with: DRA-2085
        assertNull(deleteDrHoldbackRuleAuditLog.getTextAfter());
        assertEquals(userName, deleteDrHoldbackRuleAuditLog.getUserName());
        assertEquals(drHoldbackRuleInputDto.getDrHoldbackValue(), deleteDrHoldbackRuleAuditLog.getChangeComment());
        assertEquals(drHoldbackRuleId, deleteDrHoldbackRuleAuditLog.getObjectId());
    }
}
