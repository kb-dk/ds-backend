package dk.kb.shared.util;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for resetting and running Flyway database migrations
 * for unit testing against a database instance.
 */
public class DatabaseUnitTestUtil {
    private static final Logger log = LoggerFactory.getLogger(DatabaseUnitTestUtil.class);
    private static final String MIGRATION_BASE_PATH = "classpath:db/migration/";
    private static final String TEST_DATA_BASE_PATH = "classpath:db/testdata/";

    /**
     * Cleans (wipes) the target database and executes Flyway migrations to provide a fresh baseline
     * for unit tests.
     *
     * @param url
     * @param username
     * @param password
     * @param schema
     * @param moduleName
     */
    public static void initializeFlyway(String url, String username,
                                        String password, String schema, String moduleName) {

        String migrationPath = MIGRATION_BASE_PATH + moduleName;
        String testDataPath = TEST_DATA_BASE_PATH + moduleName;

        log.info("Initializing fresh test database via Flyway for target: {}", url);

        try {
            // Programmatically invoke Flyway against the database target
            Flyway flyway = Flyway.configure()
                    .dataSource(url, username, password)
                    .locations(migrationPath, testDataPath)
                    .connectRetries(30)
                    .schemas(schema)
                    .createSchemas(true)
                    .cleanDisabled(false)
                    .load();

            log.info("Wiping existing schema: {}", schema);
            flyway.clean();
            log.info("Applying Flyway migrations...");
            flyway.migrate();

            log.info("Flyway database cleanup and migration completed successfully.");

        } catch (Exception exception) {
            log.error("Failed to execute Flyway database cleanup/migration", exception);
            throw exception;
        }
    }
}