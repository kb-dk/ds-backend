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

    /**
     * Cleans (wipes) the target database and executes Flyway migrations
     * to provide a fresh baseline for unit tests.
     */
    public static void runFlywayMigrations(String url, String driver, String username, String password, String schema) throws Exception {
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
                    .locations("classpath:db/migration")
                    .connectRetries(30)
                    .schemas(schema)
                    .createSchemas(true)
                    .load();

            log.info("Applying Flyway migrations...");
            flyway.migrate();

            log.info("Flyway database cleanup and migration completed successfully.");
        } catch (Exception e) {
            log.error("Failed to execute Flyway database cleanup/migration", e);
            throw e;
        }
    }
}