package dk.kb.datahandler.oai;

import dk.kb.datahandler.model.v1.OaiTargetDto;
import dk.kb.util.webservice.exception.InternalServiceException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OaiHarvestClientTest {
    private static final OaiHarvestClient CLIENT = new OaiHarvestClient(new OaiTargetDto(), null);

    @Test
    public void parseGetRecord_whenRecord_thenParsedLikeListRecords() {
        String xml = """
                <OAI-PMH xmlns="http://www.openarchives.org/OAI/2.0/">
                  <GetRecord>
                    <record>
                      <header>
                        <identifier>oai:io:1111</identifier>
                        <datestamp>2025-04-23T15:23:23.602Z</datestamp>
                      </header>
                      <metadata><XIP xmlns="http://preservica.com/XIP/v7.6"><InformationObject/></XIP></metadata>
                    </record>
                  </GetRecord>
                </OAI-PMH>""";

        OaiRecord record = CLIENT.parseGetRecord(xml, "test");

        assertEquals("oai:io:1111", record.getId());
        assertEquals("2025-04-23T15:23:23.602Z", record.getDateStamp());
        assertFalse(record.isDeleted());
        assertTrue(record.getMetadata().contains("<XIP><InformationObject/></XIP>"), record.getMetadata());
    }

    @Test
    public void parseGetRecord_whenIdDoesNotExist_thenNull() {
        String xml = """
                <OAI-PMH xmlns="http://www.openarchives.org/OAI/2.0/">
                  <error code="idDoesNotExist">No matching identifier</error>
                </OAI-PMH>""";

        assertNull(CLIENT.parseGetRecord(xml, "test"));
    }

    @Test
    public void parseGetRecord_whenOtherError_thenFails() {
        String xml = """
                <OAI-PMH xmlns="http://www.openarchives.org/OAI/2.0/">
                  <error code="cannotDisseminateFormat">Unknown metadataPrefix</error>
                </OAI-PMH>""";

        assertThrows(InternalServiceException.class, () -> CLIENT.parseGetRecord(xml, "test"));
    }
}
