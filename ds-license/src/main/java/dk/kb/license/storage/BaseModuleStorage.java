package dk.kb.license.storage;

import dk.kb.license.facade.LicenseModuleFacade;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
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

    // Used from unittests. Create tables DDL etc.
    protected synchronized void runDDLScript(File file) throws SQLException {
        log.info("Running DDL script:" + file.getAbsolutePath());

        if (!file.exists()) {
            log.error("DDL script not found:" + file.getAbsolutePath());
            throw new RuntimeException("DDLscript file not found:" + file.getAbsolutePath());
        }

        String scriptStatement = "RUNSCRIPT FROM '" + file.getAbsolutePath() + "'";

        connection.prepareStatement(scriptStatement).execute();
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

    /**
     * Start a storage transaction and performs the given action on it, returning the result from the action.
     * <p>
     * If the action throws an exception, a {@link LicenseModuleStorage#rollback()} is performed.
     * If the action passes without exceptions, a {@link LicenseModuleStorage#commit()} is performed.
     * @param actionID a debug-oriented ID for the action, typically the name of the calling method.
     * @param action the action to perform on the storage.
     * @return return value from the action.
     * @throws InternalServiceException if anything goes wrong.
     */
    public static <T> T performStorageAction(String actionID, BaseModuleStorage.StorageAction<T> action) {
        try (LicenseModuleStorage storage = new LicenseModuleStorage()) {
            T result;
            try {
                result = action.process(storage);
            }
            catch(InvalidArgumentServiceException e) {
                log.warn("Exception performing action '{}'. Initiating rollback", actionID, e.getMessage());
                storage.rollback();
                throw new InvalidArgumentServiceException(e);
            }
            catch (Exception e) {
                log.warn("Exception performing action '{}'. Initiating rollback", actionID, e);
                storage.rollback();
                throw new InternalServiceException(e);
            }

            try {
                storage.commit();
            } catch (SQLException e) {
                log.error("Exception committing after action '{}'", actionID, e);
                throw new InternalServiceException(e);
            }

            return result;
        } catch (SQLException e) { //Connecting to storage failed
            log.error("SQLException performing action '{}'", actionID, e);
            throw new InternalServiceException(e);
        }
    }

    /**
     * Callback used with {@link #performStorageAction(String, BaseModuleStorage.StorageAction)}.
     * @param <T> the object returned from the {@link BaseModuleStorage.StorageAction#process(LicenseModuleStorage)} method.
     */
    @FunctionalInterface
    public interface StorageAction<T> {
        /**
         * Access or modify the given storage inside of a transaction.
         * If the method throws an exception, it will be logged, a {@link LicenseModuleStorage#rollback()} will be performed and
         * a wrapping {@link dk.kb.util.webservice.exception.InternalServiceException} will be thrown.
         * @param storage a storage ready for requests and updates.
         * @return custom return value.
         * @throws Exception if something went wrong.
         */
        T process(LicenseModuleStorage storage) throws Exception;
    }
}
