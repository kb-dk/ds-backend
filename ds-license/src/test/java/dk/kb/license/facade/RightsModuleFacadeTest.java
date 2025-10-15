package dk.kb.license.facade;

import dk.kb.license.config.ServiceConfig;
import dk.kb.license.model.v1.*;
import dk.kb.license.solr.SolrServerClient;
import dk.kb.license.storage.BaseModuleStorage;
import dk.kb.license.storage.UnitTestUtil;
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

public class RightsModuleFacadeTest extends UnitTestUtil {
    protected static RightsModuleStorageForUnitTest storage = null;

    final String drHoldBackValue = "2.02";
    final String drHoldBackName = "Aktualitet og Debat";
    final int drHoldbackDays = 2190;
    final String userName = "mockedName";

    DrHoldbackRuleInputDto drHoldbackRuleInputDto = new DrHoldbackRuleInputDto();
    DrHoldbackRangesDto drHoldbackRangesDtoOne = new DrHoldbackRangesDto();
    DrHoldbackRangesDto drHoldbackRangesDtoTwo = new DrHoldbackRangesDto();

    ArrayList<AuditEntryOutputDto> auditLogEntriesForObject;

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
        auditLogEntriesForObject = storage.getAuditLogByObjectId(drHoldbackRuleId);

        assertEquals(1, auditLogEntriesForObject.size());
        AuditEntryOutputDto createDrHoldbackRuleAuditLog = auditLogEntriesForObject.get(0);
        assertEquals(ChangeTypeEnumDto.CREATE, createDrHoldbackRuleAuditLog.getChangeType());
        assertEquals(ObjectTypeEnumDto.HOLDBACK_RULE, createDrHoldbackRuleAuditLog.getChangeName());
        assertNull(createDrHoldbackRuleAuditLog.getTextBefore());
        assertEquals(drHoldbackRuleInputDto.toString(), createDrHoldbackRuleAuditLog.getTextAfter());
        assertEquals(userName, createDrHoldbackRuleAuditLog.getUserName());
        assertEquals(drHoldbackRuleInputDto.getDrHoldbackValue(), createDrHoldbackRuleAuditLog.getIdentifier());
        assertNull(createDrHoldbackRuleAuditLog.getChangeComment());
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

        auditLogEntriesForObject = storage.getAuditLogByObjectId(drHoldbackRuleId);

        assertEquals(2, drHoldBackRangesIds.size());

        for (Long drHoldBackRangesId : drHoldBackRangesIds) {

            auditLogEntriesForObject = storage.getAuditLogByObjectId(drHoldBackRangesId);
            assertEquals(1, auditLogEntriesForObject.size());

            AuditEntryOutputDto createDrHoldbackRangeAuditLog = auditLogEntriesForObject.get(0);

            assertEquals(drHoldBackRangesId, createDrHoldbackRangeAuditLog.getObjectId());
            assertEquals(ChangeTypeEnumDto.CREATE, createDrHoldbackRangeAuditLog.getChangeType());
            assertEquals(ObjectTypeEnumDto.HOLDBACK_RANGE, createDrHoldbackRangeAuditLog.getChangeName());
            assertEquals(userName, createDrHoldbackRangeAuditLog.getUserName());
            assertEquals(drHoldbackRangeInputDto.getDrHoldbackValue(), createDrHoldbackRangeAuditLog.getIdentifier());
            assertNull(createDrHoldbackRangeAuditLog.getChangeComment());
            assertNull(createDrHoldbackRangeAuditLog.getTextBefore());
            assertEquals(ranges.toString(), createDrHoldbackRangeAuditLog.getTextAfter());
        }
    }

    @Test
    public void updateDrHoldbackDaysFromDrHoldbackValue_whenDrHoldBackValue_thenUpdateDaysForRule() throws SQLException {
        int newDrHoldbackDays = 10;

        long drHoldbackRuleId = RightsModuleFacade.createDrHoldbackRule(drHoldbackRuleInputDto);

        auditLogEntriesForObject = storage.getAuditLogByObjectId(drHoldbackRuleId);

        RightsModuleFacade.updateDrHoldbackDaysFromDrHoldbackValue(drHoldBackValue, newDrHoldbackDays);
        auditLogEntriesForObject = storage.getAuditLogByObjectId(drHoldbackRuleId);
        assertEquals(2, auditLogEntriesForObject.size());

        AuditEntryOutputDto updateDrHoldbackRuleAuditLogFromValue = auditLogEntriesForObject.get(0);

        assertEquals(drHoldbackRuleId, updateDrHoldbackRuleAuditLogFromValue.getObjectId());
        assertEquals(userName, updateDrHoldbackRuleAuditLogFromValue.getUserName());
        assertEquals(ChangeTypeEnumDto.UPDATE, updateDrHoldbackRuleAuditLogFromValue.getChangeType());
        assertEquals(ObjectTypeEnumDto.HOLDBACK_DAY, updateDrHoldbackRuleAuditLogFromValue.getChangeName());
        assertEquals(drHoldbackRuleInputDto.getDrHoldbackValue(), updateDrHoldbackRuleAuditLogFromValue.getIdentifier());
        assertNull(updateDrHoldbackRuleAuditLogFromValue.getChangeComment());
        assertEquals("Days before: " + drHoldbackDays, updateDrHoldbackRuleAuditLogFromValue.getTextBefore());
        assertEquals("Days after: " + newDrHoldbackDays, updateDrHoldbackRuleAuditLogFromValue.getTextAfter());
    }

    @Test
    public void updateDrHoldbackDaysFromName_whenDrHoldBackName_thenUpdateDaysForRule() throws SQLException {
        int newDrHoldbackDays = 10;

        long drHoldbackRuleId = RightsModuleFacade.createDrHoldbackRule(drHoldbackRuleInputDto);

        auditLogEntriesForObject = storage.getAuditLogByObjectId(drHoldbackRuleId);

        RightsModuleFacade.updateDrHoldbackDaysFromName(drHoldBackName, newDrHoldbackDays);
        auditLogEntriesForObject = storage.getAuditLogByObjectId(drHoldbackRuleId);
        assertEquals(2, auditLogEntriesForObject.size());

        AuditEntryOutputDto updateDrHoldbackRuleAuditLogFromName = auditLogEntriesForObject.get(0);

        assertEquals(drHoldbackRuleId, updateDrHoldbackRuleAuditLogFromName.getObjectId());
        assertEquals(userName, updateDrHoldbackRuleAuditLogFromName.getUserName());
        assertEquals(ChangeTypeEnumDto.UPDATE, updateDrHoldbackRuleAuditLogFromName.getChangeType());
        assertEquals(ObjectTypeEnumDto.HOLDBACK_DAY, updateDrHoldbackRuleAuditLogFromName.getChangeName());
        assertEquals(drHoldbackRuleInputDto.getName(), updateDrHoldbackRuleAuditLogFromName.getIdentifier());
        assertNull(updateDrHoldbackRuleAuditLogFromName.getChangeComment());
        assertEquals("Days before: " + drHoldbackDays, updateDrHoldbackRuleAuditLogFromName.getTextBefore());
        assertEquals("Days after: " + newDrHoldbackDays, updateDrHoldbackRuleAuditLogFromName.getTextAfter());
    }

    @Test
    public void deleteRangesForDrHoldbackValue_whenDrHoldBackValue_thenDeletesAllRanges() throws SQLException {
        DrHoldbackRangeInputDto drHoldbackRangeInputDto = new DrHoldbackRangeInputDto();
        drHoldbackRangeInputDto.setDrHoldbackValue(drHoldBackValue);

        List<DrHoldbackRangesDto> ranges = List.of(drHoldbackRangesDtoOne, drHoldbackRangesDtoTwo);

        drHoldbackRangeInputDto.setRanges(ranges);

        long drHoldbackRuleId = RightsModuleFacade.createDrHoldbackRule(drHoldbackRuleInputDto);

        List<Long> drHoldBackRangesIds = RightsModuleFacade.createDrHoldbackRanges(drHoldbackRangeInputDto);

        auditLogEntriesForObject = storage.getAuditLogByObjectId(drHoldbackRuleId);

        RightsModuleFacade.deleteRangesForDrHoldbackValue(drHoldBackValue);

        for (Long drHoldBackRangesId : drHoldBackRangesIds) {

            auditLogEntriesForObject = storage.getAuditLogByObjectId(drHoldBackRangesId);
            assertEquals(2, auditLogEntriesForObject.size());

            AuditEntryOutputDto deleteDrHoldbackRangeAuditLog = auditLogEntriesForObject.get(0);

            assertEquals(drHoldBackRangesId, deleteDrHoldbackRangeAuditLog.getObjectId());
            assertTrue(auditLogEntriesForObject.get(1).getModifiedTime() < deleteDrHoldbackRangeAuditLog.getModifiedTime());
            assertEquals(userName, deleteDrHoldbackRangeAuditLog.getUserName());
            assertEquals(ChangeTypeEnumDto.DELETE, deleteDrHoldbackRangeAuditLog.getChangeType());
            assertEquals(ObjectTypeEnumDto.HOLDBACK_RANGE, deleteDrHoldbackRangeAuditLog.getChangeName());
            assertEquals(drHoldbackRangeInputDto.getDrHoldbackValue(), deleteDrHoldbackRangeAuditLog.getIdentifier());
            assertNull(deleteDrHoldbackRangeAuditLog.getChangeComment());
            // assertEquals(ranges.toString(), deleteDrHoldbackRangeAuditLog.getTextBefore()); TODO: This should be fixed together with: DRA-2085
            assertNull(deleteDrHoldbackRangeAuditLog.getTextAfter());
        }
    }

    @Test
    public void deleteDrHoldbackRule_whenDrHoldbackRuleId_thenDeleteRule() throws SQLException {
        long drHoldbackRuleId = RightsModuleFacade.createDrHoldbackRule(drHoldbackRuleInputDto);

        auditLogEntriesForObject = storage.getAuditLogByObjectId(drHoldbackRuleId);

        RightsModuleFacade.deleteDrHoldbackRule(drHoldBackValue);
        auditLogEntriesForObject = storage.getAuditLogByObjectId(drHoldbackRuleId);
        assertEquals(2, auditLogEntriesForObject.size());

        AuditEntryOutputDto deleteDrHoldbackRuleAuditLog = auditLogEntriesForObject.get(0);

        assertEquals(drHoldbackRuleId, deleteDrHoldbackRuleAuditLog.getObjectId());
        assertEquals(userName, deleteDrHoldbackRuleAuditLog.getUserName());
        assertEquals(ChangeTypeEnumDto.DELETE, deleteDrHoldbackRuleAuditLog.getChangeType());
        assertEquals(ObjectTypeEnumDto.HOLDBACK_RULE, deleteDrHoldbackRuleAuditLog.getChangeName());
        assertEquals(drHoldbackRuleInputDto.getDrHoldbackValue(), deleteDrHoldbackRuleAuditLog.getIdentifier());
        assertNull(deleteDrHoldbackRuleAuditLog.getChangeComment());
        //assertEquals(drHoldbackRuleInputDto.toString(), deleteDrHoldbackRuleAuditLog.getTextBefore()); TODO: This should be fixed together with: DRA-2085
        assertNull(deleteDrHoldbackRuleAuditLog.getTextAfter());
    }
}
