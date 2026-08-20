package dk.kb.license.storage;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.Locale;

import dk.kb.license.config.ServiceConfig;
import dk.kb.license.util.DbUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import org.testcontainers.containers.PostgreSQLContainer;

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

    }

    public static String getJdbcUrlForSchema(String schemaName) {
        String baseUrl = postgres.getJdbcUrl();
        String delimiter = baseUrl.contains("?") ? "&" : "?";
        return baseUrl + delimiter + "currentSchema=" + schemaName;
    }

    protected static String schemaName = MethodHandles.lookup().lookupClass().getSimpleName().toLowerCase(Locale.ROOT);

    protected static final String DRIVER = "org.postgresql.Driver";
    protected static final String URL = getJdbcUrlForSchema(schemaName);
    protected static final String USERNAME = postgres.getUsername();
    protected static final String PASSWORD = postgres.getPassword();

    protected static AuditLogModuleStorageForUnitTest auditStorage = null;
    protected static LicenseModuleStorageForUnitTest licenseStorage = null;
    protected static RightsModuleStorageForUnitTest rightsStorage = null;

    @BeforeAll
    public static void beforeClass() throws Exception {
        ServiceConfig.initialize("conf/ds-license*.yaml", "src/test/resources/ds-license-integration-test.yaml");
        BaseModuleStorage.initialize(DRIVER, URL, USERNAME, PASSWORD);
        DbUtil.runFlywayMigrations(URL, DRIVER, USERNAME, PASSWORD, schemaName);
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
