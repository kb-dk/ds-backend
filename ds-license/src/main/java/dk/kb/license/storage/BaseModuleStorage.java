package dk.kb.license.storage;

import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;

/**
 * The BaseModuleStorage, which sets up the connection to the database which is then used by  {@link LicenseModuleStorage} and {@link RightsModuleStorage}.
 * This class only sets up the connection, while the other two are responsible for implementing the interactions with the database.
 */
public abstract class BaseModuleStorage implements AutoCloseable  {
    private static final Logger log = LoggerFactory.getLogger(BaseModuleStorage.class);

    // statistics shown on monitor.jsp page
    public static Date INITDATE = null;

    protected Connection connection = null; // private
    protected static BasicDataSource dataSource = null; // shared

    /**
     * Close the connection to the database. You should probably perform a commit or rollback before closing the connection.
     */
    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            // nothing to do here
        }
    }

    /**
     * Commit all changes made since the last rollback or commit
     */
    public void commit() throws SQLException {
        connection.commit();
    }

    /**
     * Rollback all changes in the current transaction.
     */
    public void rollback() {
        try {
            connection.rollback();
        } catch (SQLException e) {
            // nothing to do here
        }
    }

    /**
     * Initialize the connection to a database
     * @param driverName name of the database driver
     * @param dbUrl url of the database
     * @param username database username
     * @param password password for the user
     */
    public static void initialize(String driverName, String dbUrl, String username, String password) {
        dataSource = new BasicDataSource();
        dataSource.setDriverClassName(driverName);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setUrl(dbUrl);

        dataSource.setDefaultReadOnly(false);
        dataSource.setDefaultAutoCommit(false);

        // TODO maybe set some datasource options.
        // enable detection and logging of connection leaks
        /*
         * dataSource.setRemoveAbandonedOnBorrow(
         * AlmaPickupNumbersPropertiesHolder.PICKUPNUMBERS_DATABASE_TIME_BEFORE_RECLAIM
         * > 0); dataSource.setRemoveAbandonedOnMaintenance(
         * AlmaPickupNumbersPropertiesHolder.PICKUPNUMBERS_DATABASE_TIME_BEFORE_RECLAIM
         * > 0); dataSource.setRemoveAbandonedTimeout(AlmaPickupNumbersPropertiesHolder.
         * PICKUPNUMBERS_DATABASE_TIME_BEFORE_RECLAIM); //1 hour
         * dataSource.setLogAbandoned(AlmaPickupNumbersPropertiesHolder.
         * PICKUPNUMBERS_DATABASE_TIME_BEFORE_RECLAIM > 0);
         * dataSource.setMaxWaitMillis(AlmaPickupNumbersPropertiesHolder.
         * PICKUPNUMBERS_DATABASE_POOL_CONNECT_TIMEOUT);
         */
        dataSource.setMaxTotal(10); //

        INITDATE = new Date();

        log.info("DsLicence storage initialized");
    }

    // This is called by from InialialziationContextListener by the Web-container
    // when server is shutdown,
    // Just to be sure the DB lock file is free.
    public static void shutdown() {
        log.info("Shutdown ds-license");
        try {
            if (dataSource != null) {
                dataSource.close();
            }
        } catch (SQLException e) {
            // ignore errors during shutdown, we cant do anything about it anyway
            log.error("shutdown failed", e);
        }
    }
}
