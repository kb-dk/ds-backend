package dk.kb.kaltura.client;

import com.kaltura.client.enums.EntryStatus;
import com.kaltura.client.types.APIException;
import com.kaltura.client.types.FilterPager;
import com.kaltura.client.types.MediaEntryFilter;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DsKalturaClientEntryStatusesTest {

    @Test
    void getEntryStatuses_thenFiltersOnIdsWithPageSizeOfBatch() throws Exception {
        // Arrange
        EntryStatusStubKalturaClient client = new EntryStatusStubKalturaClient(Map.of());

        // Act
        client.getEntryStatuses(List.of("0_a", "0_b"));
        MediaEntryFilter filter = client.getLastFilter();
        FilterPager pager = client.getLastPager();

        // Assert
        assertEquals("0_a,0_b", filter.getIdIn());
        assertNull(filter.getStatusIn());
        assertEquals(2, pager.getPageSize());
    }

    @Test
    void getEntryStatuses_whenEmpty_thenReturnsEmptyWithoutRequest() throws Exception {
        // Arrange
        EntryStatusStubKalturaClient client = new EntryStatusStubKalturaClient(Map.of("0_a", EntryStatus.READY));

        // Act
        Map<String, EntryStatus> result = client.getEntryStatuses(List.of());
        MediaEntryFilter filter = client.getLastFilter();

        // Assert
        assertTrue(result.isEmpty());
        assertNull(filter);
    }

    @Test
    void getEntryStatuses_whenMoreIdsThanBatchSize_thenThrows() throws Exception {
        // Arrange
        EntryStatusStubKalturaClient client = new EntryStatusStubKalturaClient(Map.of());
        List<String> ids = Collections.nCopies(client.getBatchSize() + 1, "0_a");

        // Act and Assert
        assertThrows(IllegalArgumentException.class, () -> client.getEntryStatuses(ids));
    }

    @Test
    void getEntryStatus_whenEntryExists_thenReturnsStatus() throws Exception {
        // Arrange
        EntryStatusStubKalturaClient client = new EntryStatusStubKalturaClient(Map.of("0_a", EntryStatus.PENDING));

        // Act
        EntryStatus result = client.getEntryStatus("0_a");

        // Assert
        assertEquals(EntryStatus.PENDING, result);
    }

    @Test
    void getEntryStatus_whenEntryNotFound_thenReturnsNull() throws Exception {
        // Arrange
        EntryStatusStubKalturaClient client = new EntryStatusStubKalturaClient(Map.of());

        // Act
        EntryStatus result = client.getEntryStatus("0_missing");

        // Assert
        assertNull(result);
    }

    @Test
    void getEntryStatus_whenEntryDeleted_thenReturnsNull() throws Exception {
        // Arrange
        EntryStatusStubKalturaClient client = new EntryStatusStubKalturaClient(Map.of("0_a", EntryStatus.DELETED));

        // Act
        EntryStatus result = client.getEntryStatus("0_a");

        // Assert
        assertNull(result);
    }

    @Test
    void getEntryStatus_whenOtherApiError_thenThrows() throws Exception {
        // Arrange
        EntryStatusStubKalturaClient client = new EntryStatusStubKalturaClient(Map.of());
        client.setGetError(new APIException(APIException.FailureStep.OnResponse, "Service unavailable", "SERVICE_UNAVAILABLE"));

        // Act and Assert
        assertThrows(APIException.class, () -> client.getEntryStatus("0_a"));
    }
}
