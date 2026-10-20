package dk.kb.datahandler.storage;

import dk.kb.datahandler.mapper.SummaryRequestDtoMapper;
import dk.kb.storage.model.v1.SummaryRequestDto;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import dk.kb.util.webservice.exception.NotFoundServiceException;
import dk.kb.util.webservice.exception.ServiceException;
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

public class SummaryStorage implements AutoCloseable {
  private static final Logger log = LoggerFactory.getLogger(SummaryStorage.class);
  private final static SummaryRequestDtoMapper summaryRequestDtoMapper =
      new SummaryRequestDtoMapper();
  private static final String getSummariesStatement = """
                                                      SELECT DISTINCT ON (s.file_id) -- there can be multiple of the same file_id (history) and we want the newest inserted file_id
                                                          s.id,
                                                          s.file_id,
                                                          s.summary,
                                                          s.created,
                                                          s.job_id
                                                      FROM
                                                          summaries s
                                                      WHERE
                                                          (
                                                              CAST(? AS TIMESTAMP WITH TIME ZONE) IS NULL -- takes care if the summaries table is empty
                                                              OR
                                                              s.created >= ? -- match with matching created if job has failed midway in loading summary data
                                                          )
                                                      ORDER BY
                                                          s.file_id ASC,
                                                          s.created DESC
                                                      """;
  private static BasicDataSource dataSource;
  protected Connection connection;

  public SummaryStorage() throws SQLException {
    connection = dataSource.getConnection();
  }

  public static void initialize(String driver, String url, String username, String password,
                                int connectionPoolSize) {
    dataSource = new BasicDataSource();
    dataSource.setDriverClassName(driver);
    dataSource.setUsername(username);
    dataSource.setPassword(password);
    dataSource.setUrl(url);
    dataSource.setDefaultReadOnly(false);
    dataSource.setDefaultAutoCommit(false);
    dataSource.setMaxOpenPreparedStatements(connectionPoolSize);

    log.info("SummaryStorage initialized with driver='{}', url='{}', connectionPoolSize='{}'",
        driver, url, connectionPoolSize);
  }

  /**
   * Start a storage transaction and performs the given action on it, returning the result from the
   * action. If the action throws an exception, a {@link SummaryStorage#rollback()} is performed. If
   * the action passes without exceptions, a {@link SummaryStorage#commit()} is performed.
   *
   * @param actionID     a debug-oriented ID for the action, typically the name of the calling
   *                     method.
   * @param storageClass what Storage class triggered the method
   * @param action       the action to perform on the storage.
   * @return return value from the action.
   * @throws InternalServiceException if anything goes wrong.
   */
  public static <T> T performStorageAction(String actionID,
                                           Class<? extends SummaryStorage> storageClass,
                                           SummaryStorage.StorageAction<T> action) {
    long start = System.currentTimeMillis();
    try (SummaryStorage storage = storageClass.getDeclaredConstructor().newInstance()) {
      T result;
      try {
        result = action.process(storage);
      } catch (NotFoundServiceException e) {
        // Do not have stack strace higher than debug - it is expected behavior, no stack trace
        log.debug("Not found performing action '{}'. Exception message: '{}'", actionID,
            e.getMessage());
        storage.rollback();
        throw e;
      } catch (InvalidArgumentServiceException e) {
        log.warn("Exception performing action '{}'. Initiating rollback", actionID, e);
        storage.rollback();
        throw e;
      } catch (ServiceException e) {
        // Other service exceptions: keep the HTTP status
        log.error("Service exception performing action '{}'. Initiating rollback", actionID, e);
        storage.rollback();
        throw e;
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

      log.debug("ds-datahandler method '{}' SQL time in millis: {} ", actionID,
          (System.currentTimeMillis() - start));
      return result;
    } catch (ServiceException e) {
      throw e; // Already logged above
    } catch (Exception e) {
      log.error("Exception performing action '{}'", actionID, e);
      throw new InternalServiceException(e);
    }
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
   * Return new rows from remote p3rerun database in summaries table.
   *
   * @param created latest created time in our database
   * @return List<SummaryRequestDto> of rows
   * @throws Exception
   */
  public List<SummaryRequestDto> getSummaries(OffsetDateTime created) throws Exception {
    List<SummaryRequestDto> summaryRequestDtoList = new ArrayList<>();

    try (PreparedStatement stmt = connection.prepareStatement(getSummariesStatement)) {
      stmt.setObject(1, created);
      stmt.setObject(2, created);
      ResultSet resultSet = stmt.executeQuery();

      while (resultSet.next()) {
        SummaryRequestDto summaryRequestDto = summaryRequestDtoMapper.map(resultSet);
        summaryRequestDtoList.add(summaryRequestDto);
      }

      return summaryRequestDtoList;
    } catch (SQLException e) {
      String message = "SQL Exception in getSummaries: " + e.getMessage();
      log.error(message);
      throw new SQLException(message, e);
    }
  }

  /**
   * Callback used with {@link #performStorageAction(String, Class, SummaryStorage.StorageAction)}.
   *
   * @param <T> the object returned from the
   *            {@link SummaryStorage.StorageAction#process(SummaryStorage)} method.
   */
  @FunctionalInterface
  public interface StorageAction<T> {
    /**
     * Access or modify the given storage inside a transaction. If the method throws an exception,
     * it will be logged, a {@link SummaryStorage#rollback()} will be performed and a wrapping
     * {@link dk.kb.util.webservice.exception.ServiceException} will be thrown.
     *
     * @param storage a storage ready for requests and updates.
     * @return custom return value.
     * @throws Exception if something went wrong.
     */
    T process(SummaryStorage storage) throws Exception;
  }
}
