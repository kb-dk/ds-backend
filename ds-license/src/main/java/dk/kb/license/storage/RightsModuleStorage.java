package dk.kb.license.storage;

import dk.kb.license.model.v1.RestrictedIdOutputDto;
import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Class for interacting with the part of DS-License which handles the calculation of rights, such as holdbacks and restrictions.
 */
public class RightsModuleStorage extends BaseModuleStorage{
    private static final Logger log = LoggerFactory.getLogger(RightsModuleStorage.class);

    private final String RESTRICTED_ID_TABLE = "restricted_ids";

    private final String RESTRICTED_ID_ID = "id";
    private final String RESTRICTED_ID_IDTYPE = "id_type";
    private final String RESTRICTED_ID_PLATFORM = "platform";
    private final String RESTRICTED_ID_COMMENT = "comment";
    private final String RESTRICTED_ID_MODIFIED_BY = "modified_by";
    private final String RESTRICTED_ID_MODIFIED_TIME = "modified_time";

    private final String createRestrictedIdQuery = "INSERT INTO " + RESTRICTED_ID_TABLE +
            " ("+ RESTRICTED_ID_ID +","+ RESTRICTED_ID_IDTYPE +","+ RESTRICTED_ID_PLATFORM +","+
            RESTRICTED_ID_COMMENT +","+ RESTRICTED_ID_MODIFIED_BY +","+ RESTRICTED_ID_MODIFIED_TIME +")" +
            " VALUES (?,?,?,?,?,?)";
    private final String readRestrictedIdQuery = "SELECT * FROM " + RESTRICTED_ID_TABLE + " WHERE " + RESTRICTED_ID_ID + " = ? AND " + RESTRICTED_ID_IDTYPE + " = ? AND " + RESTRICTED_ID_PLATFORM + " = ? ";
    private final String updateRestrictedIdQuery = "UPDATE " + RESTRICTED_ID_TABLE +" SET "+
            RESTRICTED_ID_PLATFORM +" = ? , " +
            RESTRICTED_ID_COMMENT +" = ? , " +
            RESTRICTED_ID_MODIFIED_BY +" = ? , " +
            RESTRICTED_ID_MODIFIED_TIME +" = ? " +
            " WHERE " +
            RESTRICTED_ID_ID +" = ? AND " +
            RESTRICTED_ID_IDTYPE +" = ?" ;
    private final String deleteRestrictedIdQuery = "DELETE FROM " + RESTRICTED_ID_TABLE + " WHERE " + RESTRICTED_ID_ID + " = ? AND " + RESTRICTED_ID_IDTYPE + " = ? AND " + RESTRICTED_ID_PLATFORM + " = ? ";

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
        dataSource.setMaxTotal(10); //

        INITDATE = new Date();

        log.info("DsLicence storage initialized");
    }

    public RightsModuleStorage() throws SQLException {
        connection = dataSource.getConnection();
    }

    public void persistRestrictedId(String id, String idType, String system, String comment, String modifiedBy, long modifiedTime) throws SQLException {
        validateIdType(idType);
        validateSystem(system);

        try (PreparedStatement stmt = connection.prepareStatement(createRestrictedIdQuery)){
            stmt.setString(1, id);
            stmt.setString(2, idType);
            stmt.setString(3, system);
            stmt.setString(4, comment);
            stmt.setString(5, modifiedBy);
            stmt.setLong(6, modifiedTime);
            stmt.execute();
        }  catch (SQLException e) {
            log.error("SQL Exception in persistClause:" + e.getMessage());
            throw e;
        }
    }

    public RestrictedIdOutputDto getRestrictedId(String id, String idType, String system) throws SQLException {
        validateIdType(idType);
        try (PreparedStatement stmt = connection.prepareStatement(readRestrictedIdQuery)) {
            stmt.setString(1, id);
            stmt.setString(2, idType);
            stmt.setString(3, system);
            ResultSet res = stmt.executeQuery();
            while (res.next()) {
                RestrictedIdOutputDto output = new RestrictedIdOutputDto();
                output.setId(res.getString(RESTRICTED_ID_ID));
                output.setIdType(res.getString(RESTRICTED_ID_IDTYPE));
                output.setPlatform(res.getString(RESTRICTED_ID_PLATFORM));
                output.setComment(res.getString(RESTRICTED_ID_COMMENT));
                output.setModifiedBy(res.getString(RESTRICTED_ID_MODIFIED_BY));
                output.setModifiedTime(res.getLong(RESTRICTED_ID_MODIFIED_TIME));
                return output;
            }
            return null;
        } catch (SQLException e) {
            log.error("SQL Exception in readClause:" + e.getMessage());
            throw e;
        }
    }

    public void updateRestrictedId(String id, String idType, String system, String comment, String modifiedBy, long modifiedTime) throws SQLException {
        validateIdType(idType);
        validateSystem(system);

        try (PreparedStatement stmt = connection.prepareStatement(updateRestrictedIdQuery)){
            stmt.setString(1, system);
            stmt.setString(2, comment);
            stmt.setString(3, modifiedBy);
            stmt.setLong(4, modifiedTime);
            stmt.setString(5, id);
            stmt.setString(6, idType);
            stmt.execute();
        }  catch (SQLException e) {
            log.error("SQL Exception in persist restricted ID" + e.getMessage());
            throw e;
        }
    }

    public void deleteRestrictedId(String id, String idType, String system) throws SQLException {
        validateIdType(idType);
        try (PreparedStatement stmt = connection.prepareStatement(deleteRestrictedIdQuery)) {
            stmt.setString(1, id);
            stmt.setString(2, idType);
            stmt.setString(3, system);
            stmt.execute();
        } catch (SQLException e) {
            log.error("SQL Exception in delete restricted Id:" + e.getMessage());
            throw e;
        }
    }

    private void validateSystem(String system) {
        List<String> validSystems = List.of("dr");//TODO get list from config file
        if (!validSystems.contains(system)) {
            throw new IllegalArgumentException("System '" + system + "' is not supported in clauses.");
        }
    }

    private void validateIdType(String idType) {
        List<String> validIdTypes = List.of("dr_produktions_id","ds_id","egenproduktions_kode","strict_titel"); //TODO get list from config file
        if (!validIdTypes.contains(idType)) {
            throw new IllegalArgumentException("IdType '" + idType + "' is not supported in clauses.");
        }
    }

    /*
     * Only called from unittests, not exposed on facade class
     *
     */
    public void clearTableRecords() throws SQLException {
        ArrayList<String> tables = new ArrayList<>();
        tables.add("RESTRICTED_IDS");

        for (String table : tables) {
            String deleteSQL="DELETE FROM " +table;
            try (PreparedStatement stmt = connection.prepareStatement(deleteSQL)) {
                stmt.execute();
            }

        }
        log.info("All tables cleared for unittest");
    }
}   


