package dk.kb.present;

import dk.kb.present.config.ServiceConfig;
import dk.kb.present.dr.restrictions.ProductionIdLookup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProductionIdLookupTest {

    @BeforeEach
    public void setup() throws IOException {
        ServiceConfig.initialize("conf/ds-present-behaviour.yaml");
    }

    @Test
    public void testProductionIdLookup() throws IOException {
        ProductionIdLookup.init();
        ServiceConfig.initialize("conf/ds-present-behaviour.yaml");

        // ID we know is present in list.
        assertTrue(ProductionIdLookup.getInstance().doLookup("9220232600"));
    }

}


