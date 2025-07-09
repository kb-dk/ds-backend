package dk.kb.license.storage;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This class is a small extension of the LicenseModuleStorage with a few methods used for unittest 
 * that we do not want in the production code.
 *
 * <p>
 * Between each unittest the all tables are cleared for data and the method is only defined in this subclass  
 */
public class LicenseModuleStorageForUnitTest extends LicenseModuleStorage  {

    private static final Logger log = LoggerFactory.getLogger(LicenseModuleStorageForUnitTest.class);
    
    public  LicenseModuleStorageForUnitTest() throws SQLException {
        super();
    }

    public void clearTableRecords(List<String> tables) throws SQLException {
    
      for (String table : tables) {
          String deleteSQL="DELETE FROM " + table; 
        try (PreparedStatement stmt = connection.prepareStatement(deleteSQL);) {
            stmt.execute();
        }           
      }
      connection.commit();
      log.info("Tables cleared for unittest");
    }

    
}
