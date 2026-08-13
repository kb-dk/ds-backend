package dk.kb.storage.storage;

import java.io.File;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.storage.config.ServiceConfig;
import dk.kb.storage.util.H2DbUtil;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Setup for the environment for unittest the same way as done in the InitialContext loader in the web container.
 * 1) Create a h2 database for unittests with schema defined
 * 2) Load the Yaml property files.
 */
public abstract class DsStorageUnitTestUtil {
    private static final PostgreSQLContainer<?> postgres;

    static {
        System.setProperty("DOCKER_HOST", "unix:///var/run/docker.sock");
        System.setProperty("api.version", "1.40");

        postgres = new PostgreSQLContainer<>("postgres:13.23-alpine3.21")
                .withDatabaseName("digisam")
                .withUsername("digisam")
                .withPassword("p0stgr3s");

        postgres.start();

        // Execute Flyway migrations against the container
        Flyway flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("filesystem:src/main/resources/db/migration")
                .load();

        flyway.migrate();
    }

    protected static final String DRIVER = "org.postgresql.Driver";
    protected static final String URL = postgres.getJdbcUrl();
    protected static final String USERNAME = postgres.getUsername();
    protected static final String PASSWORD = postgres.getPassword();

    protected static final String TEST_CLASSES_PATH = new File(
            Thread.currentThread().getContextClassLoader().getResource("logback-test.xml").getPath()
    ).getParentFile().getAbsolutePath();

    protected static DsStorageForUnitTest storage = null;

    @BeforeAll
    public static void beforeClass() throws Exception {
        ServiceConfig.initialize("conf/ds-storage*.yaml");
        H2DbUtil.createEmptyH2DBFromDDL(URL,DRIVER,USERNAME,PASSWORD);
        DsStorage.initialize(DRIVER, URL, USERNAME, PASSWORD);
        storage = new DsStorageForUnitTest();
    }

    /**
     * Delete all records between each unittest. The clearTableRecords is only called from here. 
     * The facade class is responsible for committing transactions. So clean up between unittests.
     */
    @BeforeEach
    public void beforeEach() throws Exception {                     
        storage.clearMappingAndRecordTable();
        storage.commit();
    }

    @AfterAll
    public static void afterClass() {
        // No reason to delete DB data file after test, since we clear table it before each test.
        // This way you can open the DB in a DB-browser after the unittest and see the result.
        // Just run that single test and look in the DB
        DsStorage.shutdown();
    }
}
