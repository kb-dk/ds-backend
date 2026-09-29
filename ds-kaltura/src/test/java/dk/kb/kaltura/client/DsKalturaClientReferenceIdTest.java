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
        ReferenceIdStubKalturaClient client = new ReferenceIdStubKalturaClient();

        assertNull(client.getEntryIdByReferenceId(REFERENCE_ID));
    }

    @Test
    void getEntryIdByReferenceId_whenOneEntry_thenReturnsEntryId() throws Exception {
        ReferenceIdStubKalturaClient client = new ReferenceIdStubKalturaClient("0_entry");

        assertEquals("0_entry", client.getEntryIdByReferenceId(REFERENCE_ID));
    }

    @Test
    void getEntryIdByReferenceId_whenMultipleEntries_thenThrowsIOException() throws Exception {
        ReferenceIdStubKalturaClient client = new ReferenceIdStubKalturaClient("0_a", "0_b");

        assertThrows(IOException.class, () -> client.getEntryIdByReferenceId(REFERENCE_ID));
    }

    @Test
    void getEntryIdByReferenceId_thenFiltersOnReferenceIdAndExcludesDeleted() throws Exception {
        ReferenceIdStubKalturaClient client = new ReferenceIdStubKalturaClient();

        client.getEntryIdByReferenceId(REFERENCE_ID);

        MediaEntryFilter filter = client.getLastFilter();
        assertEquals(REFERENCE_ID, filter.getReferenceIdEqual());
        assertEquals(EntryStatus.DELETED, filter.getStatusNotEqual());
        assertEquals(EntryModerationStatus.DELETED, filter.getModerationStatusNotEqual());
    }
}
