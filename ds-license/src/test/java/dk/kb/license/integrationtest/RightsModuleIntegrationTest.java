package dk.kb.license.integrationtest;

import dk.kb.license.config.ServiceConfig;
import dk.kb.license.storage.BaseModuleStorage;
import dk.kb.license.storage.DsLicenseUnitTestUtil;
import dk.kb.license.storage.RightsModuleStorage;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class RightsModuleIntegrationTest extends DsLicenseUnitTestUtil {
    private static final Logger log = LoggerFactory.getLogger( RightsModuleIntegrationTest.class);

    private static RightsModuleStorage storage;

    @BeforeAll
    static void setup() {
        try {
            ServiceConfig.initialize("conf/ds-license-behaviour.yaml","ds-license-integration-test.yaml");

            BaseModuleStorage.initialize(DRIVER, URL, USERNAME, PASSWORD);
            // Instantiate the RightsModuleStorage without it being able to touch records in a backing DS-storage
            storage = new RightsModuleStorage(false);
        } catch (IOException | SQLException e) {
            log.error("Integration yaml 'ds-license-integration-test.yaml' file most be present. Call 'kb init'");
            fail();
        }
    }

    @Test
    @Tag("integration")
    public void testQueryLookupForProductionId() {
        int touchedRecords = storage.touchRelatedStorageRecords("9213145700", "dr_produktions_id");

        // Currently there are five records for this productionId in solr
        assertTrue(touchedRecords > 4);
    }

    @Test
    @Tag("integration")
    public void testQueryLookupForId() {
        int touchedRecords = storage.touchRelatedStorageRecords("ds.tv:oai:io:5a888d7d-3c0d-4375-9e67-343d88d1dbd9", "ds_id");

        assertEquals(1, touchedRecords);
    }

    @Test
    @Tag("integration")
    public void testQueryLookupForTitle() {
        int touchedRecords = storage.touchRelatedStorageRecords("Øen", "strict_title");
        log.info("touched '{}' records", touchedRecords);


        assertTrue(touchedRecords > 20);
    }

    @Test
    @Tag("integration")
    public void testQueryLookupForProductionCode() {
        int touchedRecords = storage.touchRelatedStorageRecords("3200", "egenproduktions_kode");

        assertTrue(touchedRecords > 2500);
    }

    @Test
    @Tag("integration")
    public void testTouchNonExistingRecordInStorage() throws SQLException {
        RightsModuleStorage trueStorage = new RightsModuleStorage(true);

        assertThrows(InvalidArgumentServiceException.class,  () -> {
            trueStorage.touchRelatedStorageRecords("some-non-existing-ds-id", "ds_id");
        });
    }
}
