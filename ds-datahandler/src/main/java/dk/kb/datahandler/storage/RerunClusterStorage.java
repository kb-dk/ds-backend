package dk.kb.datahandler.storage;

import dk.kb.datahandler.mapper.RerunClusterDtoMapper;
import dk.kb.storage.model.v1.RerunClusterDto;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RerunClusterStorage implements AutoCloseable {
  private static final Logger log = LoggerFactory.getLogger(RerunClusterStorage.class);

  private final static RerunClusterDtoMapper rerunClusterDtoMapper = new RerunClusterDtoMapper();
  private static final String getRerunClustersStatement = """
      SELECT DISTINCT ON (c.file_id) -- there can be multiple of the same file_id (history) and we want the newest inserted file_id
          c.id,
          c.file_id,
          c.rerun_cluster_id,
          c.created,
          c.job_id
      FROM
          clusters c
      WHERE
          (
              CAST(? AS TIMESTAMP WITH TIME ZONE) IS NULL -- takes care if the rerun_clusters table is empty
              OR c.created > ?
          )
      ORDER BY
          c.file_id ASC,
          c.created DESC
      """;

  private static BasicDataSource dataSource;
  protected Connection connection;

  public RerunClusterStorage() throws SQLException {
    connection = dataSource.getConnection();
  }

  public static void initialize(String driver, String url, String username, String password,
                                int connectionPoolSize) {

    dataSource = new BasicDataSource();
    dataSource.setDriverClassName(driver);
    dataSource.setUsername(username);
    dataSource.setPassword(password);
    dataSource.setUrl(url);
    // Needs to be false because else unittests fails
    dataSource.setDefaultReadOnly(false);
    dataSource.setDefaultAutoCommit(false);
    dataSource.setMaxOpenPreparedStatements(connectionPoolSize);

    log.info("RerunClusterStorage initialized with driver='{}', url='{}', connectionPoolSize='{}'",
        driver, url, connectionPoolSize);
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
     * If the action throws an exception, a {@link RerunClusterStorage#rollback()} is performed.
     * If the action passes without exceptions, a {@link RerunClusterStorage#commit()} is performed.
     *
     * @param actionID     a debug-oriented ID for the action, typically the name of the calling method.
     * @param storageClass what Storage class triggered the method
     * @param action       the action to perform on the storage.
     * @return return value from the action.
     * @throws InternalServiceException if anything goes wrong.
     */
    public static <T> T performStorageAction(String actionID,
                                             Class<? extends RerunClusterStorage> storageClass,
                                             RerunClusterStorage.StorageAction<T> action) {
        long start = System.currentTimeMillis();
        try (
            RerunClusterStorage storage = storageClass.getDeclaredConstructor().newInstance()) {
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
     * Callback used with {@link #performStorageAction(String, Class, RerunClusterStorage.StorageAction)}.
     *
     * @param <T> the object returned from the {@link RerunClusterStorage.StorageAction#process(RerunClusterStorage)} method.
     */
    @FunctionalInterface
    public interface StorageAction<T> {
        /**
         * Access or modify the given storage inside a transaction.
         * If the method throws an exception, it will be logged, a {@link RerunClusterStorage#rollback()} will be performed and
         * a wrapping {@link dk.kb.util.webservice.exception.ServiceException} will be thrown.
         *
         * @param storage a storage ready for requests and updates.
         * @return custom return value.
         * @throws Exception if something went wrong.
         */
        T process(RerunClusterStorage storage) throws Exception;
    }

  /**
   * Return new rows from remote p3rerun database in table clusters table.
   *
   * @param created latest created time in our database
   * @return List<RerunClusterDto> of rows
   * @throws Exception
   */
  public List<RerunClusterDto> getRerunClusters(OffsetDateTime created) throws Exception {
    List<RerunClusterDto> rerunClusterDtoList = new ArrayList<>();

    try (PreparedStatement stmt = connection.prepareStatement(getRerunClustersStatement)) {
      stmt.setObject(1, created);
      stmt.setObject(2, created);
      ResultSet resultSet = stmt.executeQuery();

      while (resultSet.next()) {
        RerunClusterDto rerunClusterDto = rerunClusterDtoMapper.map(resultSet);
        rerunClusterDtoList.add(rerunClusterDto);
      }

      return rerunClusterDtoList;
    } catch (SQLException e) {
      String message = "SQL Exception in getRerunClusters: " + e.getMessage();
      log.error(message);
      throw new SQLException(message, e);
    }
  }
}
