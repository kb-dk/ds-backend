package dk.kb.storage.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * This class is a small extension of the TranscriptionStorage with a few methods used for unittest
 * that we do not want in the production code.
 * Between each unittest the all tables are cleared for data and the method is only defined in this subclass
 */
public class TranscriptionStorageForUnitTest extends TranscriptionStorage {
    private static final Logger log = LoggerFactory.getLogger(TranscriptionStorageForUnitTest.class);

    public TranscriptionStorageForUnitTest() throws SQLException {
        super();
    }

    /**
     * Will clear data in table. Unit test functionality only.
     */
    public void clearTable() throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM TRANSCRIPTIONS")) {
            stmt.executeUpdate();
        }

        connection.commit();
        log.info("Table cleared for unittest");
    }
}
