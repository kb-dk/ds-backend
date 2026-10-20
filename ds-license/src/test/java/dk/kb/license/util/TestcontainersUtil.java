package dk.kb.license.util;

import java.util.Locale;

import dk.kb.license.config.ServiceConfig;
import dk.kb.license.storage.*;
import dk.kb.shared.util.DatabaseUnitTestUtil;
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
public abstract class TestcontainersUtil {
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

    protected static void setupDatabaseForClass(Class<?> clazz) throws Exception {
        schemaName = clazz.getSimpleName().toLowerCase(Locale.ROOT);
        URL = getJdbcUrlForSchema(schemaName);

        // Note: previously "conf/ds-license*.yaml" (a glob, resolved by kb-util's YAML.resolveLayeredConfigs)
        // plus a second file "src/test/resources/ds-license-integration-test.yaml" that does not actually exist
        // in this repository - so in practice this only ever loaded ds-license-behaviour.yaml. SmallRye Config's
        // Resolver.resolveURL(...) does not support globs, so this now names that single file explicitly. The
        // database itself is set up below via Testcontainers (DRIVER/URL/USERNAME/PASSWORD), bypassing
        // ServiceConfig's db.* getters entirely, exactly as before.
        ServiceConfig.initialize("conf/ds-license-behaviour.yaml");
        BaseModuleStorage.initialize(DRIVER, URL, USERNAME, PASSWORD);
        DatabaseUnitTestUtil.initializeFlyway(URL, USERNAME, PASSWORD, schemaName, MODULE);
    }
}
