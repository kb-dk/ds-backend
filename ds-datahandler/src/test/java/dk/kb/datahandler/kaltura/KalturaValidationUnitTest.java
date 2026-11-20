package dk.kb.datahandler.kaltura;

import com.kaltura.client.enums.EntryStatus;
import com.kaltura.client.types.APIException;
import dk.kb.kaltura.client.DsKalturaClient;
import dk.kb.storage.model.v1.DsRecordMinimalDto;
import dk.kb.storage.model.v1.StreamErrorTypeDto;
import dk.kb.util.webservice.exception.InternalServiceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KalturaValidationUnitTest {
    static MockedStatic<KalturaValidationJob> service;
    private static final String ID = "record-456";
    private static final String FILE_ID = "file-123";
    private static final String KALTURA_ID = "0_test";

    @BeforeEach
    void initSetup() {
        // initKalturaClient now throws when the client can not be created, and a stub of it would call the real
        // method while stubbing (CALLS_REAL_METHODS). Setting the client makes initKalturaClient return early instead.
        KalturaValidationJob.kalturaClient = mock(DsKalturaClient.class);
        service = mockStatic(KalturaValidationJob.class, CALLS_REAL_METHODS);
    }

    @AfterEach
    void tearDown() {
        service.close();
        KalturaValidationJob.kalturaClient = null;
    }

    // ─── validateKalturaIds ────────────────────────────────────────────────────

    @Test
    void testValidateKalturaIds_whenStorageHasNoRecords_thenNoRecordIsCleared() {
        // Arrange
        List<DsRecordMinimalDto> emptyRecords = List.of();

        service.when(() -> KalturaValidationJob.fetchStorageRecords(any(), anyString(), anyLong(), anyInt()))
                .thenReturn(emptyRecords);

        // Act
        int result = KalturaValidationJob.validateKalturaIds(false);

        // Assert
        service.verify(() -> KalturaValidationJob.getEntryStatuses(any()), never());
        assertEquals(0, result);
    }

    @Test
    void testValidateKalturaIds_whenEntryIsReady_thenRecordIsNotCleared() {
        // Arrange
        List<DsRecordMinimalDto> records = List.of(buildRecord());
        List<DsRecordMinimalDto> emptyRecords = List.of();

        service.when(() -> KalturaValidationJob.fetchStorageRecords(any(), anyString(), anyLong(), anyInt()))
                .thenReturn(records, emptyRecords);
        service.when(() -> KalturaValidationJob.getEntryStatuses(List.of(KALTURA_ID)))
                .thenReturn(Map.of(KALTURA_ID, EntryStatus.READY));

        // Act
        int result = KalturaValidationJob.validateKalturaIds(false);

        // Assert
        assertEquals(0, result);
        service.verify(() -> KalturaValidationJob.getEntryStatus(any()), never());
        service.verify(() -> KalturaValidationJob.deleteStream(any()), never());
        service.verify(() -> KalturaValidationJob.clearKalturaIdForRecord(any(), any()), never());
    }

    @Test
    void testValidateKalturaIds_whenEntryHasNoContent_thenEntryIsDeletedAndKalturaIdIsCleared() {
        // Arrange
        List<DsRecordMinimalDto> records = List.of(buildRecord());
        List<DsRecordMinimalDto> emptyRecords = List.of();

        service.when(() -> KalturaValidationJob.fetchStorageRecords(any(), anyString(), anyLong(), anyInt()))
                .thenReturn(records, emptyRecords);
        service.when(() -> KalturaValidationJob.getEntryStatuses(anyList()))
                .thenReturn(Map.of(KALTURA_ID, EntryStatus.NO_CONTENT));
        service.when(() -> KalturaValidationJob.deleteStream(any())).thenAnswer(inv -> null);
        service.when(() -> KalturaValidationJob.clearKalturaIdForRecord(any(), any())).thenAnswer(inv -> null);

        // Act
        int result = KalturaValidationJob.validateKalturaIds(false);

        // Assert
        assertEquals(1, result);
        service.verify(() -> KalturaValidationJob.deleteStream(eq(KALTURA_ID)), times(1));
        service.verify(() -> KalturaValidationJob.clearKalturaIdForRecord(any(), eq(FILE_ID)), times(1));
    }

    @Test
    void testValidateKalturaIds_whenEntryErrorConverting_thenKalturaIdIsMarkedWithTranscodingError() {
        assertKalturaErrorIsMarked(EntryStatus.ERROR_CONVERTING, StreamErrorTypeDto.KALTURA_TRANSCODING);
    }

    @Test
    void testValidateKalturaIds_whenEntryErrorImporting_thenKalturaIdIsMarkedWithImportError() {
        assertKalturaErrorIsMarked(EntryStatus.ERROR_IMPORTING, StreamErrorTypeDto.KALTURA_IMPORT);
    }

    @Test
    void testValidateKalturaIds_whenEntryBlocked_thenKalturaIdIsMarkedWithBlockedError() {
        assertKalturaErrorIsMarked(EntryStatus.BLOCKED, StreamErrorTypeDto.KALTURA_BLOCKED);
    }

    @Test
    void testValidateKalturaIds_whenEntryScanFailure_thenKalturaIdIsMarkedWithScanError() {
        assertKalturaErrorIsMarked(EntryStatus.SCAN_FAILURE, StreamErrorTypeDto.KALTURA_SCAN);
    }

    @Test
    void testValidateKalturaIds_whenEntryInfected_thenKalturaIdIsMarkedWithScanError() {
        assertKalturaErrorIsMarked(EntryStatus.INFECTED, StreamErrorTypeDto.KALTURA_SCAN);
    }

    @Test
    void testValidateKalturaIds_whenEntryImporting_thenRecordIsSkipped() {
        assertStatusIsSkipped(EntryStatus.IMPORT);
    }

    @Test
    void testValidateKalturaIds_whenEntryPreconvert_thenRecordIsSkipped() {
        assertStatusIsSkipped(EntryStatus.PRECONVERT);
    }

    @Test
    void testValidateKalturaIds_whenEntryPending_thenRecordIsSkipped() {
        assertStatusIsSkipped(EntryStatus.PENDING);
    }

    @Test
    void testValidateKalturaIds_whenEntryWaitingForModeration_thenRecordIsSkipped() {
        assertStatusIsSkipped(EntryStatus.MODERATE);
    }

    @Test
    void testValidateKalturaIds_whenEntryHasUnhandledStatus_thenRecordIsSkipped() {
        // DELETED is the only status without an action. Kaltura should not return it, as deleted entries are
        // filtered out, but any status the job does not handle must never cause a delete.
        assertStatusIsSkipped(EntryStatus.DELETED);
    }

    private void assertStatusIsSkipped(EntryStatus status) {
        // Arrange
        List<DsRecordMinimalDto> records = List.of(buildRecord());
        List<DsRecordMinimalDto> emptyRecords = List.of();

        service.when(() -> KalturaValidationJob.fetchStorageRecords(any(), anyString(), anyLong(), anyInt()))
                .thenReturn(records, emptyRecords);
        service.when(() -> KalturaValidationJob.getEntryStatuses(anyList()))
                .thenReturn(Map.of(KALTURA_ID, status));

        // Act
        int result = KalturaValidationJob.validateKalturaIds(false);

        // Assert
        assertEquals(0, result);
        service.verify(() -> KalturaValidationJob.deleteStream(any()), never());
        service.verify(() -> KalturaValidationJob.clearKalturaIdForRecord(any(), any()), never());
        service.verify(() -> KalturaValidationJob.updateKalturaIdForRecordWithError(any(), any(), any()), never());
    }

    private void assertKalturaErrorIsMarked(EntryStatus status, StreamErrorTypeDto expectedError) {
        // Arrange
        List<DsRecordMinimalDto> records = List.of(buildRecord());
        List<DsRecordMinimalDto> emptyRecords = List.of();

        service.when(() -> KalturaValidationJob.fetchStorageRecords(any(), anyString(), anyLong(), anyInt()))
                .thenReturn(records, emptyRecords);
        service.when(() -> KalturaValidationJob.getEntryStatuses(anyList()))
                .thenReturn(Map.of(KALTURA_ID, status));
        service.when(() -> KalturaValidationJob.updateKalturaIdForRecordWithError(any(), any(), any()))
                .thenAnswer(inv -> null);

        // Act
        int result = KalturaValidationJob.validateKalturaIds(false);

        // Assert
        assertEquals(1, result);
        service.verify(() -> KalturaValidationJob.updateKalturaIdForRecordWithError(any(), eq(FILE_ID), eq(expectedError)), times(1));
        service.verify(() -> KalturaValidationJob.deleteStream(any()), never());
        service.verify(() -> KalturaValidationJob.clearKalturaIdForRecord(any(), any()), never());
    }

    @Test
    void testValidateKalturaIds_whenEntryDoesNotExistInKaltura_thenKalturaIdIsClearedWithoutDelete() {
        // Arrange
        List<DsRecordMinimalDto> records = List.of(buildRecord());
        List<DsRecordMinimalDto> emptyRecords = List.of();

        service.when(() -> KalturaValidationJob.fetchStorageRecords(any(), anyString(), anyLong(), anyInt()))
                .thenReturn(records, emptyRecords);
        service.when(() -> KalturaValidationJob.getEntryStatuses(anyList())).thenReturn(Map.of());
        service.when(() -> KalturaValidationJob.getEntryStatus(KALTURA_ID)).thenReturn(null);
        service.when(() -> KalturaValidationJob.clearKalturaIdForRecord(any(), any())).thenAnswer(inv -> null);

        // Act
        int result = KalturaValidationJob.validateKalturaIds(false);

        // Assert
        assertEquals(1, result);
        service.verify(() -> KalturaValidationJob.deleteStream(any()), never());
        service.verify(() -> KalturaValidationJob.clearKalturaIdForRecord(any(), eq(FILE_ID)), times(1));
    }

    @Test
    void testValidateKalturaIds_whenMultipleDocuments_thenAccumulatesCount() {
        // Arrange
        List<DsRecordMinimalDto> records = List.of(
                buildRecord("id-1", "file-1", "kaltura-1"),
                buildRecord("id-2", "file-2", "kaltura-2"));
        List<DsRecordMinimalDto> emptyRecords = List.of();

        service.when(() -> KalturaValidationJob.fetchStorageRecords(any(), anyString(), anyLong(), anyInt()))
                .thenReturn(records, emptyRecords);
        service.when(() -> KalturaValidationJob.getEntryStatuses(anyList()))
                .thenReturn(Map.of("kaltura-1", EntryStatus.NO_CONTENT, "kaltura-2", EntryStatus.NO_CONTENT));
        service.when(() -> KalturaValidationJob.deleteStream(any())).thenAnswer(inv -> null);
        service.when(() -> KalturaValidationJob.clearKalturaIdForRecord(any(), any())).thenAnswer(inv -> null);

        // Act
        int result = KalturaValidationJob.validateKalturaIds(false);

        // Assert
        assertEquals(2, result);
        service.verify(() -> KalturaValidationJob.getEntryStatuses(List.of("kaltura-1", "kaltura-2")), times(1));
        service.verify(() -> KalturaValidationJob.deleteStream(eq("kaltura-1")), times(1));
        service.verify(() -> KalturaValidationJob.deleteStream(eq("kaltura-2")), times(1));
        service.verify(() -> KalturaValidationJob.clearKalturaIdForRecord(any(), eq("file-1")), times(1));
        service.verify(() -> KalturaValidationJob.clearKalturaIdForRecord(any(), eq("file-2")), times(1));
    }

    @Test
    void testValidateKalturaIds_whenFetchStorageRecordsThrows_thenThrowsInternalServiceException() {
        // Arrange
        String expectedMessage = "Storage is down";
        service.when(() -> KalturaValidationJob.fetchStorageRecords(any(), anyString(), anyLong(), anyInt()))
                .thenThrow(new RuntimeException(expectedMessage));

        // Act and Assert
        Exception exception = assertThrows(InternalServiceException.class, () -> KalturaValidationJob.validateKalturaIds(false));
        assertEquals(expectedMessage, exception.getCause().getMessage());
    }

    @Test
    void testValidateKalturaIds_whenGetEntryStatusesThrows_thenThrowsInternalServiceException() {
        // Arrange
        List<DsRecordMinimalDto> records = List.of(buildRecord());

        service.when(() -> KalturaValidationJob.fetchStorageRecords(any(), anyString(), anyLong(), anyInt()))
                .thenReturn(records);
        service.when(() -> KalturaValidationJob.getEntryStatuses(anyList()))
                .thenThrow(new RuntimeException("Kaltura API error"));

        // Act and Assert
        assertThrows(InternalServiceException.class, () -> KalturaValidationJob.validateKalturaIds(false));
        service.verify(() -> KalturaValidationJob.clearKalturaIdForRecord(any(), any()), never());
    }

    @Test
    void testValidateKalturaIds_whenGetEntryStatusesThrows_thenMessageNamesMTimeTheBatchWasFetchedFrom() {
        // Arrange
        DsRecordMinimalDto first = buildRecord("record-1", "file-1", "kaltura-1");
        first.setmTime(100L);
        DsRecordMinimalDto second = buildRecord("record-2", "file-2", "kaltura-2");
        second.setmTime(200L);

        service.when(() -> KalturaValidationJob.fetchStorageRecords(any(), eq("ds.tv"), eq(0L), anyInt()))
                .thenReturn(List.of(first));
        service.when(() -> KalturaValidationJob.fetchStorageRecords(any(), eq("ds.tv"), eq(100L), anyInt()))
                .thenReturn(List.of(second));
        service.when(() -> KalturaValidationJob.getEntryStatuses(List.of("kaltura-1")))
                .thenReturn(Map.of("kaltura-1", EntryStatus.READY));
        service.when(() -> KalturaValidationJob.getEntryStatuses(List.of("kaltura-2")))
                .thenThrow(new RuntimeException("Kaltura API error"));

        // Act
        Exception exception = assertThrows(InternalServiceException.class, () -> KalturaValidationJob.validateKalturaIds(false));

        // Assert
        assertTrue(exception.getMessage().endsWith("batch after mTime=100"), exception.getMessage());
    }

    @Test
    void testValidateKalturaIds_whenKalturaIdIsMissingBlankOrErrorMarker_thenRecordIsSkipped() {
        // Arrange
        // Storage returns every record of the origin, so the job must filter out the ones it can not validate.
        DsRecordMinimalDto blank = buildRecord("record-1", "file-1", "");
        DsRecordMinimalDto valid = buildRecord("record-2", "file-2", "kaltura-2");
        DsRecordMinimalDto noKalturaId = buildRecord("record-3", "file-3", null);
        DsRecordMinimalDto uploadError = buildRecord("record-4", "file-4", "ERROR_FILE_MISSING");

        service.when(() -> KalturaValidationJob.fetchStorageRecords(any(), anyString(), anyLong(), anyInt()))
                .thenReturn(List.of(blank, valid, noKalturaId, uploadError), List.of());
        service.when(() -> KalturaValidationJob.getEntryStatuses(anyList()))
                .thenReturn(Map.of("kaltura-2", EntryStatus.READY));

        // Act
        int result = KalturaValidationJob.validateKalturaIds(false);

        // Assert
        assertEquals(0, result);
        service.verify(() -> KalturaValidationJob.getEntryStatuses(List.of("kaltura-2")), times(1));
        service.verify(() -> KalturaValidationJob.getEntryStatus(any()), never());
    }

    @Test
    void testValidateKalturaIds_whenEntryMissingFromBatchButReady_thenRecordIsNotCleared() {
        // Since Kaltura media.list can not be trusted to always return all ID's, each missing entry must be double
        // checked separately with media.get.

        // Arrange
        List<DsRecordMinimalDto> records = List.of(buildRecord());
        List<DsRecordMinimalDto> emptyRecords = List.of();

        service.when(() -> KalturaValidationJob.fetchStorageRecords(any(), anyString(), anyLong(), anyInt()))
                .thenReturn(records, emptyRecords);
        service.when(() -> KalturaValidationJob.getEntryStatuses(anyList())).thenReturn(Map.of());
        service.when(() -> KalturaValidationJob.getEntryStatus(KALTURA_ID)).thenReturn(EntryStatus.READY);

        // Act
        int result = KalturaValidationJob.validateKalturaIds(false);

        // Assert
        assertEquals(0, result);
        service.verify(() -> KalturaValidationJob.deleteStream(any()), never());
        service.verify(() -> KalturaValidationJob.clearKalturaIdForRecord(any(), any()), never());
    }

    @Test
    void testValidateKalturaIds_whenFallbackGetEntryStatusThrows_thenThrowsInternalServiceException() {
        // Arrange
        List<DsRecordMinimalDto> records = List.of(buildRecord());

        service.when(() -> KalturaValidationJob.fetchStorageRecords(any(), anyString(), anyLong(), anyInt()))
                .thenReturn(records);
        service.when(() -> KalturaValidationJob.getEntryStatuses(anyList())).thenReturn(Map.of());
        service.when(() -> KalturaValidationJob.getEntryStatus(KALTURA_ID))
                .thenThrow(new RuntimeException("Kaltura API error"));

        // Act and Assert
        assertThrows(InternalServiceException.class, () -> KalturaValidationJob.validateKalturaIds(false));
        service.verify(() -> KalturaValidationJob.clearKalturaIdForRecord(any(), any()), never());
    }

    @Test
    void testValidateKalturaIds_whenDryRun_thenNothingIsDeletedOrCleared() {
        // Arrange
        DsRecordMinimalDto notReady = buildRecord("record-1", "file-1", "kaltura-1");
        DsRecordMinimalDto notFound = buildRecord("record-2", "file-2", "kaltura-2");
        List<DsRecordMinimalDto> records = List.of(notReady, notFound);
        List<DsRecordMinimalDto> emptyRecords = List.of();

        service.when(() -> KalturaValidationJob.fetchStorageRecords(any(), anyString(), anyLong(), anyInt()))
                .thenReturn(records, emptyRecords);
        service.when(() -> KalturaValidationJob.getEntryStatuses(anyList()))
                .thenReturn(Map.of("kaltura-1", EntryStatus.ERROR_CONVERTING));
        service.when(() -> KalturaValidationJob.getEntryStatus("kaltura-2")).thenReturn(null);

        // Act
        int result = KalturaValidationJob.validateKalturaIds(true);

        // Assert
        assertEquals(2, result);
        service.verify(() -> KalturaValidationJob.deleteStream(any()), never());
        service.verify(() -> KalturaValidationJob.clearKalturaIdForRecord(any(), any()), never());
        service.verify(() -> KalturaValidationJob.updateKalturaIdForRecordWithError(any(), any(), any()), never());
    }

    @Test
    void testValidateKalturaIds_whenDryRunAndEntryReady_thenNothingWouldBeCleared() {
        // Arrange
        List<DsRecordMinimalDto> records = List.of(buildRecord());
        List<DsRecordMinimalDto> emptyRecords = List.of();

        service.when(() -> KalturaValidationJob.fetchStorageRecords(any(), anyString(), anyLong(), anyInt()))
                .thenReturn(records, emptyRecords);
        service.when(() -> KalturaValidationJob.getEntryStatuses(anyList()))
                .thenReturn(Map.of(KALTURA_ID, EntryStatus.READY));

        // Act
        int result = KalturaValidationJob.validateKalturaIds(true);

        // Assert
        assertEquals(0, result);
        service.verify(() -> KalturaValidationJob.deleteStream(any()), never());
        service.verify(() -> KalturaValidationJob.clearKalturaIdForRecord(any(), any()), never());
    }

    @Test
    void testValidateKalturaIds_thenPagesEachOriginFromLastMTime() {
        // Arrange
        DsRecordMinimalDto first = buildRecord("record-1", "file-1", "kaltura-1");
        first.setmTime(100L);
        DsRecordMinimalDto second = buildRecord("record-2", "file-2", "kaltura-2");
        second.setmTime(200L);

        service.when(() -> KalturaValidationJob.fetchStorageRecords(any(), eq("ds.tv"), eq(0L), anyInt()))
                .thenReturn(List.of(first));
        service.when(() -> KalturaValidationJob.fetchStorageRecords(any(), eq("ds.tv"), eq(100L), anyInt()))
                .thenReturn(List.of(second));
        service.when(() -> KalturaValidationJob.fetchStorageRecords(any(), eq("ds.tv"), eq(200L), anyInt()))
                .thenReturn(List.of());
        service.when(() -> KalturaValidationJob.fetchStorageRecords(any(), eq("ds.radio"), eq(0L), anyInt()))
                .thenReturn(List.of());
        service.when(() -> KalturaValidationJob.getEntryStatuses(anyList()))
                .thenReturn(Map.of("kaltura-1", EntryStatus.READY, "kaltura-2", EntryStatus.READY));

        // Act
        int result = KalturaValidationJob.validateKalturaIds(false);

        // Assert
        assertEquals(0, result);
        service.verify(() -> KalturaValidationJob.getEntryStatuses(List.of("kaltura-1")), times(1));
        service.verify(() -> KalturaValidationJob.getEntryStatuses(List.of("kaltura-2")), times(1));
        service.verify(() -> KalturaValidationJob.fetchStorageRecords(any(), eq("ds.radio"), eq(0L), anyInt()), times(1));
    }

    // ─── validateRecord ──────────────────────────────────────────────────────

    @Test
    void testValidateRecord_whenEntryMissingFromBatch_thenFallbackLookupIsCounted() {
        // Arrange
        ValidationSummary summary = new ValidationSummary();
        service.when(() -> KalturaValidationJob.getEntryStatus(KALTURA_ID)).thenReturn(EntryStatus.READY);

        // Act
        KalturaValidationJob.validateRecord(null, ID, FILE_ID, KALTURA_ID, null, false, summary);

        // Assert
        assertEquals(1, summary.getFallbackLookups());
        assertEquals(0, summary.size());
    }

    @Test
    void testValidateRecord_whenEntryFoundInBatch_thenNoFallbackLookup() {
        // Arrange
        ValidationSummary summary = new ValidationSummary();

        // Act
        KalturaValidationJob.validateRecord(null, ID, FILE_ID, KALTURA_ID, EntryStatus.READY, false, summary);

        // Assert
        assertEquals(0, summary.getFallbackLookups());
        service.verify(() -> KalturaValidationJob.getEntryStatus(any()), never());
    }

    @Test
    void testValidateRecord_whenNoContentDeleteFails_thenKalturaIdIsNotCleared() throws Exception {
        // Arrange
        // Clearing kaltura_id after a failed delete would leave the entry in Kaltura with no record pointing at it.
        ValidationSummary summary = new ValidationSummary();
        APIException permissionError = new APIException("No permission");
        permissionError.setCode("SERVICE_FORBIDDEN");
        doThrow(permissionError).when(KalturaValidationJob.kalturaClient).deleteEntry(KALTURA_ID);

        // Act
        InternalServiceException thrown = assertThrows(InternalServiceException.class, () ->
                KalturaValidationJob.validateRecord(null, ID, FILE_ID, KALTURA_ID, EntryStatus.NO_CONTENT, false, summary));

        // Assert
        assertSame(permissionError, thrown.getCause());
        service.verify(() -> KalturaValidationJob.clearKalturaIdForRecord(any(), any()), never());
        assertEquals(0, summary.size());
    }

    // ─── initKalturaClient ───────────────────────────────────────────────────

    @Test
    void testInitKalturaClient_whenClientCanNotBeCreated_thenThrowsInternalServiceException() {
        // Arrange
        // No Kaltura configuration is loaded in this unit test, so creating the client fails.
        KalturaValidationJob.kalturaClient = null;

        // Act and Assert
        assertThrows(InternalServiceException.class, KalturaValidationJob::initKalturaClient);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private DsRecordMinimalDto buildRecord() {
        return buildRecord(ID, FILE_ID, KALTURA_ID);
    }

    private DsRecordMinimalDto buildRecord(String id, String referenceId, String kalturaId) {
        DsRecordMinimalDto record = new DsRecordMinimalDto();
        record.setId(id);
        record.setReferenceId(referenceId);
        record.setKalturaId(kalturaId);
        record.setmTime(1_700_000_000L);
        return record;
    }
}
