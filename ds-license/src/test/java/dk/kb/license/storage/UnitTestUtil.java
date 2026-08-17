package dk.kb.license.storage;

import java.io.File;
import java.sql.SQLException;

import dk.kb.license.config.ServiceConfig;
import dk.kb.license.util.DbUtil;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;


import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.apache.solr.common.util.IOUtils.closeQuietly;

/**
 * Setup for the environment for unittest the same way as done in the InitialContext loader in the web container.
 * 1) Create a h2 database for unittests with schema defined
 * 2) Load the Yaml property files.
 */
public abstract class UnitTestUtil {
    private static final PostgreSQLContainer<?> postgres;

    static {
        System.setProperty("DOCKER_HOST", "unix:///var/run/docker.sock");
        System.setProperty("api.version", "1.40");

        postgres = new PostgreSQLContainer<>("postgres:13.23-alpine3.21")
                .withDatabaseName("digisam")
                .withUsername("digisam")
                .withPassword("p0stgr3s")
                .withCommand("postgres", "-c", "datestyle=ISO,DMY");

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

    protected static AuditLogModuleStorageForUnitTest auditStorage = null;
    protected static LicenseModuleStorageForUnitTest licenseStorage = null;
    protected static RightsModuleStorageForUnitTest rightsStorage = null;
    protected static BaseModuleStorage baseModuleStorage = null;

    @BeforeAll
    public static void beforeClass() throws Exception {
        ServiceConfig.initialize("conf/ds-license*.yaml", "src/test/resources/ds-license-integration-test.yaml");
        BaseModuleStorage.initialize(DRIVER, URL, USERNAME, PASSWORD);
        DbUtil.runFlywayMigrations(URL,DRIVER,USERNAME,PASSWORD,"public");
        auditStorage = new AuditLogModuleStorageForUnitTest();
        licenseStorage = new LicenseModuleStorageForUnitTest();
        rightsStorage = new RightsModuleStorageForUnitTest();
    }

    @AfterAll
    public static void afterClass() {
        // No reason to delete DB data file after test, since we clear table it before each test.
        // This way you can open the DB in a DB-browser after an unittest and see the result.
        // Just run that single test and look in the DB
        AuditLogModuleStorage.shutdown();
        LicenseModuleStorage.shutdown();
        RightsModuleStorage.shutdown();
    }
}
