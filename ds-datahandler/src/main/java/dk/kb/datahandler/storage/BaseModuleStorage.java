package dk.kb.datahandler.storage;

import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import org.apache.commons.dbcp2.BasicDataSource;

import java.sql.Connection;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseModuleStorage implements AutoCloseable {
    private static Logger log = LoggerFactory.getLogger(BaseModuleStorage.class);

    protected Connection connection;
    private static BasicDataSource dataSource;

    public static void initialize(String driverName, String driverUrl, String userName, String password) {
        int connectionPoolSize = ServiceConfig.getConnectionPoolSize();

        dataSource = new BasicDataSource();
        dataSource.setDriverClassName(driverName);
        dataSource.setUsername(userName);
        dataSource.setPassword(password);
        dataSource.setUrl(driverUrl);
        dataSource.setDefaultReadOnly(false);
        dataSource.setDefaultAutoCommit(false);
        dataSource.setMaxOpenPreparedStatements(connectionPoolSize);

        log.info("DsStorage initialized with driverName='{}', driverURL='{}', connectionPoolSize='{}'", driverName, driverUrl,connectionPoolSize);
    }

    public BaseModuleStorage() throws SQLException {
        connection = dataSource.getConnection();
    }

    public void commit() throws SQLException {
        connection.commit();
    }

    public void rollback() {
        try {
            connection.rollback();
        } catch (Exception e) {
            // nothing to do here
        }
    }

    @Override
    public void close() {
        // Make sure connection is closed
        try {
            connection.close();
        } catch (Exception e) {
            //Nothing to do
        }
    }

    /**
     * Start a storage transaction and performs the given action on it, returning the result from the action.
     * If the action throws an exception, a {@link BaseModuleStorage#rollback()} is performed.
     * If the action passes without exceptions, a {@link BaseModuleStorage#commit()} is performed.
     *
     * @param actionID     a debug-oriented ID for the action, typically the name of the calling method.
     * @param storageClass what Storage class triggered the method
     * @param action       the action to perform on the storage.
     * @return return value from the action.
     * @throws InternalServiceException if anything goes wrong.
     */
    public static <T> T performStorageAction(String actionID,
                                             Class<? extends BaseModuleStorage> storageClass,
                                             BaseModuleStorage.StorageAction<T> action) {
        long start = System.currentTimeMillis();
        try (
            BaseModuleStorage storage = storageClass.getDeclaredConstructor().newInstance()) {
            T result;
            try {
                result = action.process(storage);
            } catch (InvalidArgumentServiceException e) {
                log.warn("Exception performing action '{}'. Initiating rollback", actionID, e.getMessage());
                storage.rollback();
                throw new InvalidArgumentServiceException(e);
            } catch (Exception e) {
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

            log.debug("ds-datahandler method '{}' SQL time in millis: {} ", actionID, (System.currentTimeMillis() - start));
            return result;
        } catch (Exception e) {
            log.error("Exception performing action '{}'", actionID, e);
            throw new InternalServiceException(e);
        }
    }

    /**
     * Callback used with {@link #performStorageAction(String, Class, StorageAction)}.
     *
     * @param <T> the object returned from the {@link StorageAction#process(BaseModuleStorage)} method.
     */
    @FunctionalInterface
    public interface StorageAction<T> {
        /**
         * Access or modify the given storage inside a transaction.
         * If the method throws an exception, it will be logged, a {@link BaseModuleStorage#rollback()} will be performed and
         * a wrapping {@link dk.kb.util.webservice.exception.ServiceException} will be thrown.
         *
         * @param storage a storage ready for requests and updates.
         * @return custom return value.
         * @throws Exception if something went wrong.
         */
        T process(BaseModuleStorage storage) throws Exception;
    }
}
