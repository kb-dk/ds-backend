package dk.kb.shared.util;

import java.sql.SQLException;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for resetting and running Flyway database migrations
 * for unit testing against a database instance.
 */
public class DbUtil {

    private static final Logger log = LoggerFactory.getLogger(DbUtil.class);
    private static final String MIGRATION_BASE_PATH = "classpath:db/migration/";
    private static final String TEST_DATA_BASE_PATH = "classpath:db/testdata/";

    /**
     * Cleans (wipes) the target database and executes Flyway migrations
     * to provide a fresh baseline for unit tests.
     */
    public static void runFlywayMigrations(String url, String driver, String username, String password, String schema, String moduleName) throws Exception {

        String migrationLocation = MIGRATION_BASE_PATH + moduleName;
        String testDataLocation = TEST_DATA_BASE_PATH + moduleName;

        log.info("Initializing fresh test database via Flyway for target: {}", url);

        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Failed to load database driver: " + driver, e);
        }

        try {
            // Programmatically invoke Flyway against the Postgres target
            Flyway flyway = Flyway.configure()
                    .dataSource(url, username, password)
                    .locations(migrationLocation, testDataLocation)
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
        } catch (Exception e) {
            log.error("Failed to execute Flyway database cleanup/migration", e);
            throw e;
        }
    }
}