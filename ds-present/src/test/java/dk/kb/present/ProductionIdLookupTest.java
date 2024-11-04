package dk.kb.present;

import dk.kb.present.config.ServiceConfig;
import dk.kb.present.dr.restrictions.ProductionIdLookup;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProductionIdLookupTest {

    @Test
    public void testProductionIdLookup() throws IOException {
        ServiceConfig.initialize("conf/ds-present-behaviour.yaml");
        ProductionIdLookup idLookup = new ProductionIdLookup();

        // ID we know is present in list.
        assertTrue(idLookup.doLookup("00922023260"));
    }

}


