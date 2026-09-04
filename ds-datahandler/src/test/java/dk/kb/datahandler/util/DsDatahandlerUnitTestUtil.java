package dk.kb.datahandler.util;

import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.datahandler.storage.JobStorage;
import dk.kb.shared.util.DatabaseUnitTestUtil;

import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Locale;

/**
 * Setup for the environment for unittest the same way as done in the InitialContext loader in the web container.
 * 1) Create a Postgres database for unittests with schema defined
 * 2) Load the Yaml property files.
 */
@Testcontainers
public abstract class DsDatahandlerUnitTestUtil {
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
    protected static final String MODULE = "ds-datahandler";

    protected static void setupDatabaseForClass(Class<?> clazz) throws Exception {
        schemaName = clazz.getSimpleName().toLowerCase(Locale.ROOT);
        URL = getJdbcUrlForSchema(schemaName);

        ServiceConfig.initialize("conf/ds-datahandler-behaviour.yaml");
        DatabaseUnitTestUtil.initializeFlyway(URL, USERNAME, PASSWORD, schemaName, MODULE);
        JobStorage.initialize(DRIVER, URL, USERNAME, PASSWORD);
    }
}
