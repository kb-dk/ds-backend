package dk.kb.datahandler.oai;

import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.model.v1.RecordTypeDto;
import dk.kb.storage.util.DsStorageClient;
import dk.kb.util.webservice.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class OaiResponseFilterDrArchiveTest {
    // The DR filter derives origin (ds.tv/ds.radio) from the record metadata, so the datasource is not used
    private static final String IGNORED_DATASOURCE = "ignored.datasource";

    @Test
    public void oaiDrFilter_whenOaiContainsDeletedRecords_thenTheyAreMarkedDeletedAndAllRecordsProcessed() throws ServiceException {
        DsStorageClient storage = mock(DsStorageClient.class);
        OaiResponseFilterDrArchive filter = new OaiResponseFilterDrArchive(IGNORED_DATASOURCE, storage);

        OaiRecord deletedRecord1 = new OaiRecord();
        deletedRecord1.setId("oai:io:1111");
        deletedRecord1.setDeleted(true);

        OaiRecord tvRecord = new OaiRecord();
        tvRecord.setId("oai:io:4444");
        tvRecord.setMetadata("<Metadata schemaUri=\"http://www.pbcore.org/PBCore/PBCoreNamespace.html\">" +
                             "<publisher>dr1</publisher><formatMediaType>Moving Image</formatMediaType></Metadata>");

        OaiRecord radioRecord = new OaiRecord();
        radioRecord.setId("oai:io:2222");
        radioRecord.setMetadata("<Metadata schemaUri=\"http://www.pbcore.org/PBCore/PBCoreNamespace.html\">" +
                                "<publisher>drp1</publisher><formatMediaType>Sound</formatMediaType></Metadata>");

        OaiRecord deletedRecord2 = new OaiRecord();
        deletedRecord2.setId("oai:io:3333");
        deletedRecord2.setDeleted(true);

        OaiResponse response = new OaiResponse();
        response.setRecords(new ArrayList<>(List.of(deletedRecord1, tvRecord, radioRecord, deletedRecord2)));

        filter.addToStorage(response);

        // Deleted records have no metadata, so both tv and radio IDs are marked for delete
        verify(storage).markRecordForDelete("ds.tv:oai:io:1111");
        verify(storage).markRecordForDelete("ds.radio:oai:io:1111");
        verify(storage).recordPost(argThat(record -> "ds.tv:oai:io:4444".equals(record.getId())));
        verify(storage).recordPost(argThat(record -> "ds.radio:oai:io:2222".equals(record.getId())));
        verify(storage).markRecordForDelete("ds.tv:oai:io:3333");
        verify(storage).markRecordForDelete("ds.radio:oai:io:3333");
        verifyNoMoreInteractions(storage);
    }

    @Test
    public void oaiDrFilter_whenOaiContainsTvRecord_thenItIsStoredAsTvRecord() throws ServiceException {
        DsStorageClient storage = mock(DsStorageClient.class);
        OaiResponseFilterDrArchive filter = new OaiResponseFilterDrArchive(IGNORED_DATASOURCE, storage);

        String metadata = "<Metadata schemaUri=\"http://www.pbcore.org/PBCore/PBCoreNamespace.html\">" +
                          "<publisher>dr1</publisher><formatMediaType>Moving Image</formatMediaType></Metadata>";
        OaiRecord tvRecord = new OaiRecord();
        tvRecord.setId("oai:io:4444");
        tvRecord.setMetadata(metadata);

        OaiResponse response = new OaiResponse();
        response.setRecords(new ArrayList<>(List.of(tvRecord)));

        filter.addToStorage(response);

        ArgumentCaptor<DsRecordDto> stored = ArgumentCaptor.forClass(DsRecordDto.class);
        verify(storage).recordPost(stored.capture());
        verifyNoMoreInteractions(storage);
        assertEquals("ds.tv:oai:io:4444", stored.getValue().getId());
        assertEquals("ds.tv", stored.getValue().getOrigin());
        assertEquals(metadata, stored.getValue().getData());
        assertEquals(RecordTypeDto.DELIVERABLEUNIT, stored.getValue().getRecordType());
        assertNull(stored.getValue().getParentId());
        assertNull(stored.getValue().getReferenceId());
        assertEquals(1, filter.getProcessed());
    }

    @Test
    public void oaiDrFilter_whenOaiContainsRadioRecord_thenItIsStoredAsRadioRecord() throws ServiceException {
        DsStorageClient storage = mock(DsStorageClient.class);
        OaiResponseFilterDrArchive filter = new OaiResponseFilterDrArchive(IGNORED_DATASOURCE, storage);

        String metadata = "<Metadata schemaUri=\"http://www.pbcore.org/PBCore/PBCoreNamespace.html\">" +
                          "<publisher>drp1</publisher><formatMediaType>Sound</formatMediaType></Metadata>";
        OaiRecord radioRecord = new OaiRecord();
        radioRecord.setId("oai:io:2222");
        radioRecord.setMetadata(metadata);

        OaiResponse response = new OaiResponse();
        response.setRecords(new ArrayList<>(List.of(radioRecord)));

        filter.addToStorage(response);

        ArgumentCaptor<DsRecordDto> stored = ArgumentCaptor.forClass(DsRecordDto.class);
        verify(storage).recordPost(stored.capture());
        verifyNoMoreInteractions(storage);
        assertEquals("ds.radio:oai:io:2222", stored.getValue().getId());
        assertEquals("ds.radio", stored.getValue().getOrigin());
        assertEquals(metadata, stored.getValue().getData());
        assertEquals(RecordTypeDto.DELIVERABLEUNIT, stored.getValue().getRecordType());
        assertNull(stored.getValue().getParentId());
        assertNull(stored.getValue().getReferenceId());
        assertEquals(1, filter.getProcessed());
    }
}
