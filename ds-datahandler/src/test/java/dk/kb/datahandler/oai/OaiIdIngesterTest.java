package dk.kb.datahandler.oai;

import dk.kb.util.webservice.exception.InternalServiceException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class OaiIdIngesterTest {

    private static class RecordingFilter extends OaiResponseFilter {
        final List<String> stored = new ArrayList<>();

        RecordingFilter() {
            super("test", null);
        }

        @Override
        public void addToStorage(OaiResponse response) {
            response.getRecords().forEach(record -> stored.add(record.getId()));
            processed += response.getRecords().size();
        }
    }

    private static OaiRecord record(String identifier) {
        OaiRecord record = new OaiRecord();
        record.setId(identifier);
        return record;
    }

    private interface Fetch {
        OaiRecord getRecord(String identifier) throws IOException;
    }

    private static OaiHarvestClient client(Fetch fetch) {
        return new OaiHarvestClient(null, null) {
            @Override
            public OaiRecord getRecord(String identifier) throws IOException {
                return fetch.getRecord(identifier);
            }
        };
    }

    private static List<String> identifiers(int count) {
        return IntStream.range(0, count).mapToObj(i -> "oai:io:" + i).toList();
    }

    @Test
    public void ingest_whenSeveralRecords_thenStoredInOrder() {
        RecordingFilter filter = new RecordingFilter();
        List<String> identifiers = identifiers(50);

        try (OaiIdIngester ingester = new OaiIdIngester(client(OaiIdIngesterTest::record), filter, 4)) {
            ingester.ingest(identifiers);

            assertEquals(identifiers.size(), ingester.getProcessed());
            assertEquals(0, ingester.getNotFound());
        }
        assertEquals(identifiers, filter.stored);
    }

    @Test
    public void ingest_whenRecordDoesNotExist_thenCountedAndSkipped() {
        RecordingFilter filter = new RecordingFilter();

        try (OaiIdIngester ingester = new OaiIdIngester(
                client(identifier -> identifier.equals("oai:io:1") ? null : record(identifier)), filter, 2)) {
            ingester.ingest(identifiers(3));
            ingester.ingest(identifiers(2));

            assertEquals(3, ingester.getProcessed());
            assertEquals(2, ingester.getNotFound());
        }
        assertEquals(List.of("oai:io:0", "oai:io:2", "oai:io:0"), filter.stored);
    }

    @Test
    public void ingest_whenFetchFails_thenThrowsAfterStoringEarlierRecords() {
        RecordingFilter filter = new RecordingFilter();
        OaiHarvestClient fetcher = client(identifier -> {
            if (identifier.equals("oai:io:3")) {
                throw new IOException("Server unavailable");
            }
            return record(identifier);
        });

        try (OaiIdIngester ingester = new OaiIdIngester(fetcher, filter, 2)) {
            InternalServiceException e = assertThrows(InternalServiceException.class,
                                                      () -> ingester.ingest(identifiers(10)));
            assertEquals(IOException.class, e.getCause().getClass());
        }
        assertEquals(List.of("oai:io:0", "oai:io:1", "oai:io:2"), filter.stored);
    }

    @Test
    public void ingest_whenFetchThrowsServiceException_thenRethrownAsIs() {
        RecordingFilter filter = new RecordingFilter();

        try (OaiIdIngester ingester = new OaiIdIngester(client(identifier -> {
            throw new InternalServiceException("OAI GetRecord failed");
        }), filter, 2)) {
            InternalServiceException e = assertThrows(InternalServiceException.class,
                                                      () -> ingester.ingest(identifiers(1)));
            assertEquals("OAI GetRecord failed", e.getMessage());
        }
    }
}
