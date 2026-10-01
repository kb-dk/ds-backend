package dk.kb.datahandler.kaltura;

import com.kaltura.client.enums.EntryStatus;
import dk.kb.storage.model.v1.DsRecordKalturaDto;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        service = mockStatic(KalturaValidationJob.class, CALLS_REAL_METHODS);
        service.when(() -> KalturaValidationJob.initKalturaClient()).then(inv -> null);
    }

    @AfterEach
    void tearDown() {
        service.close();
    }

    // ─── validateKalturaIds ────────────────────────────────────────────────────

    @Test
    void testValidateKalturaIds_whenStorageHasNoRecords_thenNoRecordIsCleared() {
        // Arrange
        List<DsRecordKalturaDto> emptyRecords = List.of();

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
        List<DsRecordKalturaDto> records = List.of(buildRecord());
        List<DsRecordKalturaDto> emptyRecords = List.of();

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
    void testValidateKalturaIds_whenEntryExistsButNotReady_thenEntryIsDeletedAndKalturaIdIsCleared() {
        // Arrange
        List<DsRecordKalturaDto> records = List.of(buildRecord());
        List<DsRecordKalturaDto> emptyRecords = List.of();

        service.when(() -> KalturaValidationJob.fetchStorageRecords(any(), anyString(), anyLong(), anyInt()))
                .thenReturn(records, emptyRecords);
        service.when(() -> KalturaValidationJob.getEntryStatuses(anyList()))
                .thenReturn(Map.of(KALTURA_ID, EntryStatus.PENDING));
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

    private void assertKalturaErrorIsMarked(EntryStatus status, StreamErrorTypeDto expectedError) {
        // Arrange
        List<DsRecordKalturaDto> records = List.of(buildRecord());
        List<DsRecordKalturaDto> emptyRecords = List.of();

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
        List<DsRecordKalturaDto> records = List.of(buildRecord());
        List<DsRecordKalturaDto> emptyRecords = List.of();

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
        List<DsRecordKalturaDto> records = List.of(
                buildRecord("id-1", "file-1", "kaltura-1"),
                buildRecord("id-2", "file-2", "kaltura-2"));
        List<DsRecordKalturaDto> emptyRecords = List.of();

        service.when(() -> KalturaValidationJob.fetchStorageRecords(any(), anyString(), anyLong(), anyInt()))
                .thenReturn(records, emptyRecords);
        service.when(() -> KalturaValidationJob.getEntryStatuses(anyList()))
                .thenReturn(Map.of("kaltura-1", EntryStatus.PENDING, "kaltura-2", EntryStatus.PENDING));
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
        List<DsRecordKalturaDto> records = List.of(buildRecord());

        service.when(() -> KalturaValidationJob.fetchStorageRecords(any(), anyString(), anyLong(), anyInt()))
                .thenReturn(records);
        service.when(() -> KalturaValidationJob.getEntryStatuses(anyList()))
                .thenThrow(new RuntimeException("Kaltura API error"));

        // Act and Assert
        assertThrows(InternalServiceException.class, () -> KalturaValidationJob.validateKalturaIds(false));
        service.verify(() -> KalturaValidationJob.clearKalturaIdForRecord(any(), any()), never());
    }

    @Test
    void testValidateKalturaIds_whenEntryMissingFromBatchButReady_thenRecordIsNotCleared() {
        // Arrange
        List<DsRecordKalturaDto> records = List.of(buildRecord());
        List<DsRecordKalturaDto> emptyRecords = List.of();

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
        List<DsRecordKalturaDto> records = List.of(buildRecord());

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
        DsRecordKalturaDto notReady = buildRecord("record-1", "file-1", "kaltura-1");
        DsRecordKalturaDto notFound = buildRecord("record-2", "file-2", "kaltura-2");
        List<DsRecordKalturaDto> records = List.of(notReady, notFound);
        List<DsRecordKalturaDto> emptyRecords = List.of();

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
        List<DsRecordKalturaDto> records = List.of(buildRecord());
        List<DsRecordKalturaDto> emptyRecords = List.of();

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
    void testValidateKalturaIds_whenRecordMarkedForDelete_thenRecordIsSkipped() {
        // Arrange
        DsRecordKalturaDto deleted = buildRecord("record-1", "file-1", "kaltura-1");
        deleted.setDeleted(true);
        DsRecordKalturaDto active = buildRecord("record-2", "file-2", "kaltura-2");

        service.when(() -> KalturaValidationJob.fetchStorageRecords(any(), anyString(), anyLong(), anyInt()))
                .thenReturn(List.of(deleted, active), List.of());
        service.when(() -> KalturaValidationJob.getEntryStatuses(anyList()))
                .thenReturn(Map.of("kaltura-2", EntryStatus.PENDING));
        service.when(() -> KalturaValidationJob.deleteStream(any())).thenAnswer(inv -> null);
        service.when(() -> KalturaValidationJob.clearKalturaIdForRecord(any(), any())).thenAnswer(inv -> null);

        // Act
        int result = KalturaValidationJob.validateKalturaIds(false);

        // Assert
        assertEquals(1, result);
        service.verify(() -> KalturaValidationJob.getEntryStatuses(List.of("kaltura-2")), times(1));
        service.verify(() -> KalturaValidationJob.clearKalturaIdForRecord(any(), eq("file-1")), never());
        service.verify(() -> KalturaValidationJob.clearKalturaIdForRecord(any(), eq("file-2")), times(1));
    }

    @Test
    void testValidateKalturaIds_thenPagesEachOriginFromLastMTime() {
        // Arrange
        DsRecordKalturaDto first = buildRecord("record-1", "file-1", "kaltura-1");
        first.setmTime(100L);
        DsRecordKalturaDto second = buildRecord("record-2", "file-2", "kaltura-2");
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

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private DsRecordKalturaDto buildRecord() {
        return buildRecord(ID, FILE_ID, KALTURA_ID);
    }

    private DsRecordKalturaDto buildRecord(String id, String referenceId, String kalturaId) {
        DsRecordKalturaDto record = new DsRecordKalturaDto();
        record.setId(id);
        record.setReferenceId(referenceId);
        record.setKalturaId(kalturaId);
        record.setmTime(1_700_000_000L);
        record.setDeleted(false);
        return record;
    }
}
