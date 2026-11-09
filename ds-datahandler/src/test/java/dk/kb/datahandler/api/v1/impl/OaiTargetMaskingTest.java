package dk.kb.datahandler.api.v1.impl;

import dk.kb.datahandler.model.v1.OaiTargetDto;
import dk.kb.datahandler.model.v1.OaiTargetSearchDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class OaiTargetMaskingTest {

    @Test
    public void withMaskedPasswords_whenPasswords_thenMaskedInCopyOnly() {
        OaiTargetSearchDto search = new OaiTargetSearchDto();
        search.setApiUrl("https://example.com");
        search.setUsername("api-user");
        search.setPassword("api-secret");
        OaiTargetDto target = new OaiTargetDto();
        target.setName("target");
        target.setUsername("oai-user");
        target.setPassword("oai-secret");
        target.setSearch(search);

        OaiTargetDto masked = DsDatahandlerApiServiceImpl.withMaskedPasswords(target);

        assertEquals("********", masked.getPassword());
        assertEquals("********", masked.getSearch().getPassword());
        assertEquals("oai-user", masked.getUsername());
        assertEquals("api-user", masked.getSearch().getUsername());
        assertEquals("oai-secret", target.getPassword());
        assertEquals("api-secret", target.getSearch().getPassword());
    }

    @Test
    public void withMaskedPasswords_whenNoPasswordOrSearch_thenUnchanged() {
        OaiTargetDto target = new OaiTargetDto();
        target.setName("target");

        OaiTargetDto masked = DsDatahandlerApiServiceImpl.withMaskedPasswords(target);

        assertNull(masked.getPassword());
        assertNull(masked.getSearch());
        assertEquals("target", masked.getName());
    }
}
