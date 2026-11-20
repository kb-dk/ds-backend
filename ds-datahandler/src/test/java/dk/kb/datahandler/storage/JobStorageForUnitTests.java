package dk.kb.datahandler.storage;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobStorageForUnitTests extends JobStorage {
    private static final Logger log = LoggerFactory.getLogger(JobStorageForUnitTests.class);

    public JobStorageForUnitTests() throws SQLException {
        super();
    }

    /**
     * Will clear data in table. Unit test functionality only.
     */
    public void clearTable() throws SQLException {
        try(PreparedStatement stmt = connection.prepareStatement("DELETE FROM JOBS")) {
            stmt.executeUpdate();
        }

        connection.commit();
        log.info("Table cleared for unittest");
    }
}
