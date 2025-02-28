package dk.kb.license.storage;

import dk.kb.license.config.ServiceConfig;
import dk.kb.license.model.v1.RestrictedIdOutputDto;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.yaml.YAML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class for interacting with the part of DS-License which handles the calculation of rights, such as holdbacks and restrictions.
 */
public class RightsModuleStorage extends BaseModuleStorage{
    private static final Logger log = LoggerFactory.getLogger(RightsModuleStorage.class);

    private final String RESTRICTED_ID_TABLE = "restricted_ids";

    private final String RESTRICTED_ID_ID = "id";
    private final String RESTRICTED_ID_IDVALUE = "id_value";
    private final String RESTRICTED_ID_IDTYPE = "id_type";
    private final String RESTRICTED_ID_PLATFORM = "platform";
    private final String RESTRICTED_ID_COMMENT = "comment";
    private final String RESTRICTED_ID_MODIFIED_BY = "modified_by";
    private final String RESTRICTED_ID_MODIFIED_TIME = "modified_time";

    private final String createRestrictedIdQuery = "INSERT INTO " + RESTRICTED_ID_TABLE +
            " ("+RESTRICTED_ID_ID+","+ RESTRICTED_ID_IDVALUE +","+ RESTRICTED_ID_IDTYPE +","+ RESTRICTED_ID_PLATFORM +","+
            RESTRICTED_ID_COMMENT +","+ RESTRICTED_ID_MODIFIED_BY +","+ RESTRICTED_ID_MODIFIED_TIME +")" +
            " VALUES (?,?,?,?,?,?,?)";
    private final String readRestrictedIdQuery = "SELECT * FROM " + RESTRICTED_ID_TABLE + " WHERE " + RESTRICTED_ID_IDVALUE + " = ? AND " + RESTRICTED_ID_IDTYPE + " = ? AND " + RESTRICTED_ID_PLATFORM + " = ? ";
    private final String updateRestrictedIdQuery = "UPDATE " + RESTRICTED_ID_TABLE +" SET "+
            RESTRICTED_ID_PLATFORM +" = ? , " +
            RESTRICTED_ID_COMMENT +" = ? , " +
            RESTRICTED_ID_MODIFIED_BY +" = ? , " +
            RESTRICTED_ID_MODIFIED_TIME +" = ? " +
            " WHERE " +
            RESTRICTED_ID_IDVALUE +" = ? AND " +
            RESTRICTED_ID_IDTYPE +" = ?" ;
    private final String deleteRestrictedIdQuery = "DELETE FROM " + RESTRICTED_ID_TABLE + " WHERE " + RESTRICTED_ID_IDVALUE + " = ? AND " + RESTRICTED_ID_IDTYPE + " = ? AND " + RESTRICTED_ID_PLATFORM + " = ? ";

    private final String allRestrictedIdsQuery = "SELECT * FROM " + RESTRICTED_ID_TABLE;

    public RightsModuleStorage() throws SQLException {
        connection = dataSource.getConnection();
    }

    /**
     * Creates an entry in the restrictedID table
     *
     * @param id_value The value of the ID
     * @param id_type The type of the ID
     * @param platform The platform where the object is restricted (e.g DR)
     * @param comment Just a comment
     * @param modifiedBy The id of the user creating the restricted ID
     * @param modifiedTime timestamp for creation
     * @throws SQLException
     */
    public void createRestrictedId(String id_value, String id_type, String platform, String comment, String modifiedBy, long modifiedTime) throws SQLException {
        validatePlatformAndIdType(platform,id_type);

        try (PreparedStatement stmt = connection.prepareStatement(createRestrictedIdQuery)){
            stmt.setLong(1, generateUniqueID());
            stmt.setString(2, id_value);
            stmt.setString(3, id_type);
            stmt.setString(4, platform);
            stmt.setString(5, comment);
            stmt.setString(6, modifiedBy);
            stmt.setLong(7, modifiedTime);
            stmt.execute();
        }  catch (SQLException e) {
            log.error("SQL Exception in persistClause:" + e.getMessage());
            throw e;
        }
    }

    /**
     * Gets an entry from the restrinctedID table
     *
     * @param id_value value of the ID
     * @param id_type type of ID
     * @param platform platform where the ID is restricted
     * @return
     * @throws SQLException
     */
    public RestrictedIdOutputDto getRestrictedId(String id_value, String id_type, String platform) throws SQLException {
        validatePlatformAndIdType(platform,id_type);
        try (PreparedStatement stmt = connection.prepareStatement(readRestrictedIdQuery)) {
            stmt.setString(1, id_value);
            stmt.setString(2, id_type);
            stmt.setString(3, platform);
            ResultSet res = stmt.executeQuery();
            while (res.next()) {
                RestrictedIdOutputDto output = new RestrictedIdOutputDto();
                output.setIdValue(res.getString(RESTRICTED_ID_IDVALUE));
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

    /**
     * Updates an entry in the restricted IDs table
     *
     * @param id_value The value of the ID
     * @param id_type The type of the ID
     * @param platform The platform where the object is restricted (e.g DR)
     * @param comment Just a comment
     * @param modifiedBy The id of the user creating the restricted ID
     * @param modifiedTime timestamp for creation
     * @throws SQLException
     */
    public void updateRestrictedId(String id_value, String id_type, String platform, String comment, String modifiedBy, long modifiedTime) throws SQLException {
        validatePlatformAndIdType(platform,id_type);

        try (PreparedStatement stmt = connection.prepareStatement(updateRestrictedIdQuery)){
            stmt.setString(1, platform);
            stmt.setString(2, comment);
            stmt.setString(3, modifiedBy);
            stmt.setLong(4, modifiedTime);
            stmt.setString(5, id_value);
            stmt.setString(6, id_type);
            stmt.execute();
        }  catch (SQLException e) {
            log.error("SQL Exception in persist restricted ID" + e.getMessage());
            throw e;
        }
    }

    /**
     * Delete an entry from the restricted IDs table
     *
     * @param id_value value of the ID
     * @param id_type type of ID
     * @param platform platform where the ID is restricted
     * @throws SQLException
     */
    public void deleteRestrictedId(String id_value, String id_type, String platform) throws SQLException {
        validatePlatformAndIdType(platform,id_type);
        try (PreparedStatement stmt = connection.prepareStatement(deleteRestrictedIdQuery)) {
            stmt.setString(1, id_value);
            stmt.setString(2, id_type);
            stmt.setString(3, platform);
            stmt.execute();
        } catch (SQLException e) {
            log.error("SQL Exception in delete restricted Id:" + e.getMessage());
            throw e;
        }
    }

    public List<RestrictedIdOutputDto> getAllRestrictedIds() throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(allRestrictedIdsQuery)) {
            ResultSet res = stmt.executeQuery();
            List<RestrictedIdOutputDto> output = new ArrayList<>();
            while (res.next()) {
                RestrictedIdOutputDto restrictedIdOutputDto = new RestrictedIdOutputDto();
                restrictedIdOutputDto.setIdValue(res.getString(RESTRICTED_ID_IDVALUE));
                restrictedIdOutputDto.setIdType(res.getString(RESTRICTED_ID_IDTYPE));
                restrictedIdOutputDto.setPlatform(res.getString(RESTRICTED_ID_PLATFORM));
                restrictedIdOutputDto.setComment(res.getString(RESTRICTED_ID_COMMENT));
                restrictedIdOutputDto.setModifiedBy(res.getString(RESTRICTED_ID_MODIFIED_BY));
                restrictedIdOutputDto.setModifiedTime(res.getLong(RESTRICTED_ID_MODIFIED_TIME));
                output.add(restrictedIdOutputDto);
            }
            return output;
        } catch (SQLException e) {
            log.error("SQL Exception in readClause:" + e.getMessage());
            throw e;
        }
    }

    private void validatePlatformAndIdType(String platform, String idType) {
        YAML platformConfig = ServiceConfig.getRightsPlatformConfig(platform);
        if (platformConfig.isEmpty()) {
            throw new IllegalArgumentException("Invalid platform "+platform);
        }
        if (!platformConfig.getList("idTypes").contains(idType)) {
            throw new IllegalArgumentException("Invalid idType "+idType);
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


