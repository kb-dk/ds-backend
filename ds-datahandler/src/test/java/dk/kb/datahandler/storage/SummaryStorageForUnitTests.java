package dk.kb.datahandler.storage;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SummaryStorageForUnitTests extends SummaryStorage {
  private static final Logger log = LoggerFactory.getLogger(SummaryStorageForUnitTests.class);

  public SummaryStorageForUnitTests() throws SQLException {
    super();
  }

  /**
   * Will clear data in table. Unit test functionality only.
   */
  public void clearTable() throws SQLException {
    try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM summaries")) {
      stmt.executeUpdate();
    }

    connection.commit();
    log.info("Table cleared for unittest");
  }
}