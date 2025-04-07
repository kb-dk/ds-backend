package dk.kb.present;

import dk.kb.present.config.ServiceConfig;
import dk.kb.present.dr.restrictions.ProductionIdLookup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration")
public class ProductionIdLookupTest {
    private static final Logger log = LoggerFactory.getLogger(ProductionIdLookupTest.class);


    @BeforeEach
    public void setup() throws IOException {
        ServiceConfig.initialize("conf/ds-present-behaviour.yaml");
    }

    @Test
    public void testProductionIdLookup() {
        ProductionIdLookup.init();

        // ID we know is present in list.
        assertTrue(ProductionIdLookup.getInstance().doLookup("9220232600"));
    }

    @Test
    public void testProductionIdLookupCorrectNumbers() {
        ProductionIdLookup.init();

        // new ID we know is present in list.
        assertTrue(ProductionIdLookup.getInstance().doLookup("9514310600"));
    }

}


