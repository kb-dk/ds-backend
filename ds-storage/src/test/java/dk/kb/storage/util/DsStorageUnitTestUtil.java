package dk.kb.storage.util;

import java.io.File;
import java.util.Locale;

import dk.kb.storage.storage.DsStorage;
import dk.kb.storage.storage.DsStorageForUnitTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;

import dk.kb.storage.config.ServiceConfig;
import dk.kb.shared.util.DbUtil;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Setup for the environment for unittest the same way as done in the InitialContext loader in the web container.
 * 1) Create a Postgres database for unittests with schema defined
 * 2) Load the Yaml property files.
 */
@Testcontainers
public abstract class DsStorageUnitTestUtil {
    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:13.23")
            .withDatabaseName("digisam")
            .withEnv("PGDATESTYLE", "ISO,DMY");

    public static String getJdbcUrlForSchema(String schemaName) {
        String baseUrl = postgres.getJdbcUrl();
        String delimiter = baseUrl.contains("?") ? "&" : "?";
        return baseUrl + delimiter + "currentSchema=" + schemaName;
    }

    protected static String URL;
    protected static String schemaName;

    protected static final String DRIVER = "org.postgresql.Driver";
    protected static final String USERNAME = postgres.getUsername();
    protected static final String PASSWORD = postgres.getPassword();

    protected static final String TEST_CLASSES_PATH = new File(
            Thread.currentThread().getContextClassLoader().getResource("logback-test.xml").getPath()
    ).getParentFile().getAbsolutePath();

    protected static DsStorageForUnitTest storage = null;

    protected static void setupDatabaseForClass(Class<?> clazz) throws Exception {
        schemaName = clazz.getSimpleName().toLowerCase(Locale.ROOT);
        URL = getJdbcUrlForSchema(schemaName);

        ServiceConfig.initialize("conf/ds-storage*.yaml");
        DbUtil.runFlywayMigrations(URL, DRIVER, USERNAME, PASSWORD, schemaName);
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
