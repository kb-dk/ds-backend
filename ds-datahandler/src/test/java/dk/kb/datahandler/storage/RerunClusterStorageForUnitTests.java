package dk.kb.datahandler.storage;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class RerunClusterStorageForUnitTests extends RerunClusterStorage {
    public void clearTables() throws SQLException {
        try(PreparedStatement stmt = connection.prepareStatement("DELETE FROM clusters")) {
            stmt.executeUpdate();
            commit();
        }
    }

    public RerunClusterStorageForUnitTests() throws SQLException {
        super();
    }
}