package dk.kb.kaltura.client;

import com.kaltura.client.types.APIException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DsKalturaClientDeleteEntryTest {

    @Test
    void deleteEntry_whenKalturaDeletes_thenDoesNotThrow() throws Exception {
        // Arrange
        EntryStatusStubKalturaClient client = new EntryStatusStubKalturaClient(Map.of());

        // Act and Assert
        assertDoesNotThrow(() -> client.deleteEntry("0_a"));
    }

    @Test
    void deleteEntry_whenEntryNotFound_thenCountsAsDeleted() throws Exception {
        // Arrange
        EntryStatusStubKalturaClient client = new EntryStatusStubKalturaClient(Map.of());
        client.setDeleteError(new APIException(APIException.FailureStep.OnResponse, "Entry id not found",
                DsKalturaClient.ENTRY_ID_NOT_FOUND));

        // Act and Assert
        assertDoesNotThrow(() -> client.deleteEntry("0_a"));
    }

    @Test
    void deleteEntry_whenOtherApiError_thenThrows() throws Exception {
        // Arrange
        EntryStatusStubKalturaClient client = new EntryStatusStubKalturaClient(Map.of());
        APIException forbidden = new APIException(APIException.FailureStep.OnResponse, "No permission",
                "SERVICE_FORBIDDEN");
        client.setDeleteError(forbidden);

        // Act
        APIException thrown = assertThrows(APIException.class, () -> client.deleteEntry("0_a"));

        // Assert
        assertSame(forbidden, thrown);
    }
}
