package dk.kb.datahandler.storage;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RerunClusterStorageForUnitTests extends RerunClusterStorage {
  private static final Logger log = LoggerFactory.getLogger(RerunClusterStorageForUnitTests.class);

  public RerunClusterStorageForUnitTests() throws SQLException {
    super();
  }

  /**
   * Will clear data in table. Unit test functionality only.
   */
  public void clearTable() throws SQLException {
    try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM clusters")) {
      stmt.executeUpdate();
    }

    connection.commit();
    log.info("Table cleared for unittest");
  }
}