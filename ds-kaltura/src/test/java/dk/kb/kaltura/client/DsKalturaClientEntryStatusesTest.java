package dk.kb.kaltura.client;

import com.kaltura.client.enums.EntryStatus;
import com.kaltura.client.types.APIException;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DsKalturaClientEntryStatusesTest {

    @Test
    void getEntryStatuses_thenReturnsStatusForAllStatuses() throws Exception {
        EntryStatusStubKalturaClient client = new EntryStatusStubKalturaClient(
                Map.of("0_ready", EntryStatus.READY, "0_error", EntryStatus.ERROR_CONVERTING));

        Map<String, EntryStatus> statuses = client.getEntryStatuses(List.of("0_ready", "0_error", "0_missing"));

        assertEquals(EntryStatus.READY, statuses.get("0_ready"));
        assertEquals(EntryStatus.ERROR_CONVERTING, statuses.get("0_error"));
        assertNull(statuses.get("0_missing"));
    }

    @Test
    void getEntryStatuses_thenFiltersOnIdsWithPageSizeOfBatch() throws Exception {
        EntryStatusStubKalturaClient client = new EntryStatusStubKalturaClient(Map.of());

        client.getEntryStatuses(List.of("0_a", "0_b"));

        assertEquals("0_a,0_b", client.getLastFilter().getIdIn());
        assertNull(client.getLastFilter().getStatusIn());
        assertEquals(2, client.getLastPager().getPageSize());
    }

    @Test
    void getEntryStatuses_whenEmpty_thenReturnsEmptyWithoutRequest() throws Exception {
        EntryStatusStubKalturaClient client = new EntryStatusStubKalturaClient(Map.of("0_a", EntryStatus.READY));

        assertTrue(client.getEntryStatuses(List.of()).isEmpty());
        assertNull(client.getLastFilter());
    }

    @Test
    void getEntryStatuses_whenMoreIdsThanBatchSize_thenThrows() throws Exception {
        EntryStatusStubKalturaClient client = new EntryStatusStubKalturaClient(Map.of());
        List<String> ids = Collections.nCopies(client.getBatchSize() + 1, "0_a");

        assertThrows(IllegalArgumentException.class, () -> client.getEntryStatuses(ids));
    }

    @Test
    void getEntryStatus_whenEntryExists_thenReturnsStatus() throws Exception {
        EntryStatusStubKalturaClient client = new EntryStatusStubKalturaClient(Map.of("0_a", EntryStatus.PENDING));

        assertEquals(EntryStatus.PENDING, client.getEntryStatus("0_a"));
    }

    @Test
    void getEntryStatus_whenEntryNotFound_thenReturnsNull() throws Exception {
        EntryStatusStubKalturaClient client = new EntryStatusStubKalturaClient(Map.of());

        assertNull(client.getEntryStatus("0_missing"));
    }

    @Test
    void getEntryStatus_whenEntryDeleted_thenReturnsNull() throws Exception {
        EntryStatusStubKalturaClient client = new EntryStatusStubKalturaClient(Map.of("0_a", EntryStatus.DELETED));

        assertNull(client.getEntryStatus("0_a"));
    }

    @Test
    void getEntryStatus_whenOtherApiError_thenThrows() throws Exception {
        EntryStatusStubKalturaClient client = new EntryStatusStubKalturaClient(Map.of());
        client.setGetError(new APIException(APIException.FailureStep.OnResponse, "Service unavailable", "SERVICE_UNAVAILABLE"));

        assertThrows(APIException.class, () -> client.getEntryStatus("0_a"));
    }
}
