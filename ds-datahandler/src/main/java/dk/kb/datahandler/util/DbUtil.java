package dk.kb.datahandler.util;

import java.sql.SQLException;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for initializing and running Flyway database migrations
 * against a PostgreSQL instance (e.g. Jetty local dev mode or embedded setups).
 */
public class DbUtil {

    private static final Logger log = LoggerFactory.getLogger(DbUtil.class);

    /**
     * Executes Flyway migrations against the configured PostgreSQL database.
     * Replaces the legacy H2 RUNSCRIPT execution.
     */
    public static void runFlywayMigrations(String url, String driver, String username, String password) throws Exception {
        log.info("Initializing database migrations via Flyway for target: {}", url);

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
                    .load();

            flyway.migrate();
            log.info("Flyway database migration completed successfully.");
        } catch (Exception e) {
            log.error("Failed to execute Flyway database migrations", e);
            throw e;
        }
    }
}
