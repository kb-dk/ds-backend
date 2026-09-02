package dk.kb.license.util;

import java.util.Locale;

import dk.kb.license.config.ServiceConfig;
import dk.kb.license.storage.*;
import dk.kb.shared.util.DbUtil;
import org.junit.jupiter.api.AfterAll;

import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Setup for the environment for unittest the same way as done in the InitialContext loader in the web container.
 * 1) Create aPostgres database for unittests with schema defined
 * 2) Load the Yaml property files.
 */

@Testcontainers
public abstract class DsLicenseUnitTestUtil {
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

    protected static final String DRIVER = postgres.getDriverClassName();
    protected static final String USERNAME = postgres.getUsername();
    protected static final String PASSWORD = postgres.getPassword();
    protected static final String MODULE = "ds-license";

    protected static AuditLogModuleStorageForUnitTest auditStorage = null;
    protected static LicenseModuleStorageForUnitTest licenseStorage = null;
    protected static RightsModuleStorageForUnitTest rightsStorage = null;

    protected static void setupDatabaseForClass(Class<?> clazz) throws Exception {
        schemaName = clazz.getSimpleName().toLowerCase(Locale.ROOT);
        URL = getJdbcUrlForSchema(schemaName);

        ServiceConfig.initialize("conf/ds-license*.yaml", "src/test/resources/ds-license-integration-test.yaml");
        BaseModuleStorage.initialize(DRIVER, URL, USERNAME, PASSWORD);
        DbUtil.runFlywayMigrations(URL, DRIVER, USERNAME, PASSWORD, schemaName, MODULE);

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
