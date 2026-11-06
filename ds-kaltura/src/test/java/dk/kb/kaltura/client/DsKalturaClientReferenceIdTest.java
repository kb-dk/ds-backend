package dk.kb.kaltura.client;

import com.kaltura.client.enums.EntryModerationStatus;
import com.kaltura.client.enums.EntryStatus;
import com.kaltura.client.types.MediaEntryFilter;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DsKalturaClientReferenceIdTest {
    private static final String REFERENCE_ID = "b16bc5cb-1ea9-48d4-8e3c-2a94abae501b";

    @Test
    void getEntryIdByReferenceId_whenNoEntry_thenReturnsNull() throws Exception {
        // Arrange
        ReferenceIdStubKalturaClient client = new ReferenceIdStubKalturaClient();

        // Act
        String result = client.getEntryIdByReferenceId(REFERENCE_ID);

        // Assert
        assertNull(result);
    }

    @Test
    void getEntryIdByReferenceId_whenOneEntry_thenReturnsEntryId() throws Exception {
        // Arrange
        ReferenceIdStubKalturaClient client = new ReferenceIdStubKalturaClient("0_entry");

        // Act
        String result = client.getEntryIdByReferenceId(REFERENCE_ID);

        // Assert
        assertEquals("0_entry", result);
    }

    @Test
    void getEntryIdByReferenceId_whenMultipleEntries_thenThrowsIOException() throws Exception {
        // Arrange
        ReferenceIdStubKalturaClient client = new ReferenceIdStubKalturaClient("0_a", "0_b");

        // Act and Assert
        assertThrows(IOException.class, () -> client.getEntryIdByReferenceId(REFERENCE_ID));
    }

    @Test
    void getEntryIdByReferenceId_thenFiltersOnReferenceIdAndExcludesDeleted() throws Exception {
        // Arrange
        ReferenceIdStubKalturaClient client = new ReferenceIdStubKalturaClient();

        // Act
        client.getEntryIdByReferenceId(REFERENCE_ID);
        MediaEntryFilter filter = client.getLastFilter();

        // Assert
        assertEquals(REFERENCE_ID, filter.getReferenceIdEqual());
        assertEquals(EntryStatus.DELETED, filter.getStatusNotEqual());
        assertEquals(EntryModerationStatus.DELETED, filter.getModerationStatusNotEqual());
    }
}
