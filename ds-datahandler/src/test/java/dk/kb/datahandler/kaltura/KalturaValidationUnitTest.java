package dk.kb.datahandler.kaltura;

import com.kaltura.client.enums.EntryStatus;
import dk.kb.util.webservice.exception.InternalServiceException;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Arrays;

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
    void testValidateKalturaIds_whenSolrHasNoDocuments_thenNoRecordIsCleared() {
        // Arrange
        SolrDocumentList emptySolrDocumentList = new SolrDocumentList(); // numFound = 0

        service.when(() -> KalturaValidationJob.fetchSolrRecords(anyLong(), anyInt()))
                .thenReturn(emptySolrDocumentList);

        // Act
        int result = KalturaValidationJob.validateKalturaIds();

        // Assert
        service.verify(() -> KalturaValidationJob.getEntryStatus(any()), never());
        assertEquals(0, result);
    }

    @Test
    void testValidateKalturaIds_whenEntryIsReady_thenRecordIsNotCleared() {
        // Arrange
        SolrDocumentList solrDocumentList = buildSolrDocumentList(buildSolrDocument());
        SolrDocumentList emptySolrDocumentList = new SolrDocumentList();

        service.when(() -> KalturaValidationJob.fetchSolrRecords(anyLong(), anyInt()))
                .thenReturn(solrDocumentList, emptySolrDocumentList);
        service.when(() -> KalturaValidationJob.getEntryStatus(KALTURA_ID)).thenReturn(EntryStatus.READY);

        // Act
        int result = KalturaValidationJob.validateKalturaIds();

        // Assert
        assertEquals(0, result);
        service.verify(() -> KalturaValidationJob.deleteStream(any()), never());
        service.verify(() -> KalturaValidationJob.clearKalturaIdForRecord(any(), any()), never());
    }

    @Test
    void testValidateKalturaIds_whenEntryExistsButNotReady_thenEntryIsDeletedAndKalturaIdIsCleared() {
        // Arrange
        SolrDocumentList solrDocumentList = buildSolrDocumentList(buildSolrDocument());
        SolrDocumentList emptySolrDocumentList = new SolrDocumentList();

        service.when(() -> KalturaValidationJob.fetchSolrRecords(anyLong(), anyInt()))
                .thenReturn(solrDocumentList, emptySolrDocumentList);
        service.when(() -> KalturaValidationJob.getEntryStatus(KALTURA_ID)).thenReturn(EntryStatus.PENDING);
        service.when(() -> KalturaValidationJob.deleteStream(any())).thenAnswer(inv -> null);
        service.when(() -> KalturaValidationJob.clearKalturaIdForRecord(any(), any())).thenAnswer(inv -> null);

        // Act
        int result = KalturaValidationJob.validateKalturaIds();

        // Assert
        assertEquals(1, result);
        service.verify(() -> KalturaValidationJob.deleteStream(eq(KALTURA_ID)), times(1));
        service.verify(() -> KalturaValidationJob.clearKalturaIdForRecord(any(), eq(FILE_ID)), times(1));
    }

    @Test
    void testValidateKalturaIds_whenEntryDoesNotExistInKaltura_thenKalturaIdIsClearedWithoutDelete() {
        // Arrange
        SolrDocumentList solrDocumentList = buildSolrDocumentList(buildSolrDocument());
        SolrDocumentList emptySolrDocumentList = new SolrDocumentList();

        service.when(() -> KalturaValidationJob.fetchSolrRecords(anyLong(), anyInt()))
                .thenReturn(solrDocumentList, emptySolrDocumentList);
        service.when(() -> KalturaValidationJob.getEntryStatus(KALTURA_ID)).thenReturn(null);
        service.when(() -> KalturaValidationJob.clearKalturaIdForRecord(any(), any())).thenAnswer(inv -> null);

        // Act
        int result = KalturaValidationJob.validateKalturaIds();

        // Assert
        assertEquals(1, result);
        service.verify(() -> KalturaValidationJob.deleteStream(any()), never());
        service.verify(() -> KalturaValidationJob.clearKalturaIdForRecord(any(), eq(FILE_ID)), times(1));
    }

    @Test
    void testValidateKalturaIds_whenMultipleDocuments_thenAccumulatesCount() {
        // Arrange
        SolrDocumentList solrDocumentList = buildSolrDocumentList(
                buildSolrDocument("id-1", "file-1", "kaltura-1"),
                buildSolrDocument("id-2", "file-2", "kaltura-2"));
        SolrDocumentList emptySolrDocumentList = new SolrDocumentList();

        service.when(() -> KalturaValidationJob.fetchSolrRecords(anyLong(), anyInt()))
                .thenReturn(solrDocumentList, emptySolrDocumentList);
        service.when(() -> KalturaValidationJob.getEntryStatus(anyString())).thenReturn(EntryStatus.PENDING);
        service.when(() -> KalturaValidationJob.deleteStream(any())).thenAnswer(inv -> null);
        service.when(() -> KalturaValidationJob.clearKalturaIdForRecord(any(), any())).thenAnswer(inv -> null);

        // Act
        int result = KalturaValidationJob.validateKalturaIds();

        // Assert
        assertEquals(2, result);
        service.verify(() -> KalturaValidationJob.deleteStream(eq("kaltura-1")), times(1));
        service.verify(() -> KalturaValidationJob.deleteStream(eq("kaltura-2")), times(1));
        service.verify(() -> KalturaValidationJob.clearKalturaIdForRecord(any(), eq("file-1")), times(1));
        service.verify(() -> KalturaValidationJob.clearKalturaIdForRecord(any(), eq("file-2")), times(1));
    }

    @Test
    void testValidateKalturaIds_whenFetchSolrRecordsThrowsSolrServerException_thenThrowsInternalServiceException() {
        // Arrange
        String expectedMessage = "Solr is down";
        service.when(() -> KalturaValidationJob.fetchSolrRecords(anyLong(), anyInt()))
                .thenThrow(new SolrServerException(expectedMessage));

        // Act and Assert
        Exception exception = assertThrows(InternalServiceException.class, KalturaValidationJob::validateKalturaIds);
        assertEquals(expectedMessage, exception.getCause().getMessage());
    }

    @Test
    void testValidateKalturaIds_whenFetchSolrRecordsThrowsIOException_thenThrowsInternalServiceException() {
        // Arrange
        String expectedMessage = "Network failure";
        service.when(() -> KalturaValidationJob.fetchSolrRecords(anyLong(), anyInt()))
                .thenThrow(new IOException(expectedMessage));

        // Act and Assert
        Exception exception = assertThrows(InternalServiceException.class, KalturaValidationJob::validateKalturaIds);
        assertEquals(expectedMessage, exception.getCause().getMessage());
    }

    @Test
    void testValidateKalturaIds_whenGetEntryStatusThrows_thenThrowsInternalServiceException() {
        // Arrange
        SolrDocumentList solrDocumentList = buildSolrDocumentList(buildSolrDocument());

        service.when(() -> KalturaValidationJob.fetchSolrRecords(anyLong(), anyInt()))
                .thenReturn(solrDocumentList);
        service.when(() -> KalturaValidationJob.getEntryStatus(KALTURA_ID))
                .thenThrow(new RuntimeException("Kaltura API error"));

        // Act and Assert
        assertThrows(InternalServiceException.class, KalturaValidationJob::validateKalturaIds);
        service.verify(() -> KalturaValidationJob.clearKalturaIdForRecord(any(), any()), never());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private SolrDocument buildSolrDocument() {
        return buildSolrDocument(ID, FILE_ID, KALTURA_ID);
    }

    private SolrDocument buildSolrDocument(String id, String fileId, String kalturaId) {
        SolrDocument solrDocument = new SolrDocument();
        solrDocument.setField("id", id);
        solrDocument.setField("file_id", fileId);
        solrDocument.setField("kaltura_id", kalturaId);
        solrDocument.setField("internal_storage_mTime", 1_700_000_000L);
        return solrDocument;
    }

    private SolrDocumentList buildSolrDocumentList(SolrDocument... documents) {
        SolrDocumentList solrDocumentList = new SolrDocumentList();
        solrDocumentList.addAll(Arrays.asList(documents));
        solrDocumentList.setNumFound(documents.length);
        return solrDocumentList;
    }
}
