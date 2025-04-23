package dk.kb.license.storage;

import dk.kb.license.config.ServiceConfig;
import dk.kb.license.model.v1.DrHoldbackRangeMappingDto;
import dk.kb.license.model.v1.DrHoldbackRuleDto;
import dk.kb.license.model.v1.RestrictedIdOutputDto;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
    private final String readRestrictedIdQueryByInternalId = "SELECT * FROM " + RESTRICTED_ID_TABLE + " WHERE " + RESTRICTED_ID_ID + " = ?";

    private final String updateRestrictedIdQuery = "UPDATE " + RESTRICTED_ID_TABLE +" SET "+
            RESTRICTED_ID_PLATFORM +" = ? , " +
            RESTRICTED_ID_COMMENT +" = ? , " +
            RESTRICTED_ID_MODIFIED_BY +" = ? , " +
            RESTRICTED_ID_MODIFIED_TIME +" = ? " +
            " WHERE " +
            RESTRICTED_ID_IDVALUE +" = ? AND " +
            RESTRICTED_ID_IDTYPE +" = ?" ;
    private final String deleteRestrictedIdQuery = "DELETE FROM " + RESTRICTED_ID_TABLE + " WHERE " + RESTRICTED_ID_IDVALUE + " = ? AND " + RESTRICTED_ID_IDTYPE + " = ? AND " + RESTRICTED_ID_PLATFORM + " = ? ";
    private final String deleteRestrictedIdByInternalIdQuery = "DELETE FROM " + RESTRICTED_ID_TABLE + " WHERE " + RESTRICTED_ID_ID + " = ?";

    private final String allRestrictedIdsQuery = "SELECT * FROM " + RESTRICTED_ID_TABLE + " WHERE " + RESTRICTED_ID_IDTYPE + " LIKE ? AND " + RESTRICTED_ID_PLATFORM + " LIKE ?";

    private final String DR_HOLDBACK_RULES_TABLE = "DR_HOLDBACK_RULES";
    private final String DR_HOLDBACK_RULES_ID = "id";
    private final String DR_HOLDBACK_RULES_DAYS = "days";
    private final String DR_HOLDBACK_RULES_NAME = "name";

    private final String DR_HOLDBACK_MAP_TABLE = "DR_HOLDBACK_MAP";
    private final String DR_HOLDBACK_MAP_ID = "id";
    private final String DR_HOLDBACK_MAP_CONTENT_FROM = "content_range_from";
    private final String DR_HOLDBACK_MAP_CONTENT_TO = "content_range_to";
    private final String DR_HOLDBACK_MAP_FORM_FROM = "form_range_from";
    private final String DR_HOLDBACK_MAP_FORM_TO = "form_range_to";
    private final String DR_HOLDBACK_MAP_HOLDBACK_ID = "dr_holdback_id";

    private final String createDrHoldbackRuleQuery = "INSERT INTO "+DR_HOLDBACK_RULES_TABLE +
            " ("+ DR_HOLDBACK_RULES_ID +","+ DR_HOLDBACK_RULES_NAME +","+ DR_HOLDBACK_RULES_DAYS +")" +
            " VALUES (?,?,?)";
    private final String deleteDrHoldbackRuleQuery = "DELETE FROM " + DR_HOLDBACK_RULES_TABLE +
            " WHERE id = ?";
    private final String getDrHoldbackDaysFromNameQuery = "SELECT " + DR_HOLDBACK_RULES_DAYS +" FROM "+DR_HOLDBACK_RULES_TABLE +
            " WHERE name = ?";
    private final String getDrHoldbackDaysFromIDQuery = "SELECT " + DR_HOLDBACK_RULES_DAYS +" FROM "+DR_HOLDBACK_RULES_TABLE +
            " WHERE id = ?";
    private final String getAllDrHoldbackRulesQuery = "SELECT * FROM " + DR_HOLDBACK_RULES_TABLE;
    private final String getDrHoldbackRuleFromId = "SELECT * FROM " + DR_HOLDBACK_RULES_TABLE
            + " WHERE id = ?";
    private final String updateDrHoldbackDaysForId = "Update " + DR_HOLDBACK_RULES_TABLE
            + " SET days = ?"
            + " WHERE id = ?";
    private final String updateDrHoldbackDaysForName = "Update " + DR_HOLDBACK_RULES_TABLE
            + " SET days = ?"
            + " WHERE name = ?";

    private final String createHoldbackMapping = "INSERT INTO " + DR_HOLDBACK_MAP_TABLE +
            "("+DR_HOLDBACK_MAP_ID+","+DR_HOLDBACK_MAP_CONTENT_FROM+","+DR_HOLDBACK_MAP_CONTENT_TO+","+DR_HOLDBACK_MAP_FORM_FROM+","+DR_HOLDBACK_MAP_FORM_TO+","+DR_HOLDBACK_MAP_HOLDBACK_ID+")"+
            " VALUES (?,?,?,?,?,?)";

    private final String getHoldbackIdFromContentAndForm = "SELECT " + DR_HOLDBACK_MAP_HOLDBACK_ID +
            " FROM " + DR_HOLDBACK_MAP_TABLE + " WHERE " +
            " content_range_from <= ? AND " +
            " content_range_to >= ?  AND " +
            " form_range_from <= ? AND " +
            " form_range_to >= ? ";


    private final String getRangesForDrHoldbackId = "SELECT * FROM "+DR_HOLDBACK_MAP_TABLE+" WHERE dr_holdback_id = ?";
    private final String deleteRangesForDrHoldbackId = "DELETE from "+DR_HOLDBACK_MAP_TABLE+" WHERE dr_holdback_id = ?";


    public RightsModuleStorage() throws SQLException {
       super();
    }

    /**
     * Creates an entry in the restrictedID table. Also updates the mTime for the related record in ds-storage.
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
        long uniqueID = generateUniqueID();

        try (PreparedStatement stmt = connection.prepareStatement(createRestrictedIdQuery)){
            stmt.setLong(1, uniqueID);
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
        try (PreparedStatement stmt = connection.prepareStatement(readRestrictedIdQuery)) {
            stmt.setString(1, id_value);
            stmt.setString(2, id_type);
            stmt.setString(3, platform);
            ResultSet res = stmt.executeQuery();
            while (res.next()) {
                return createRestrictedIdOutputDtoFromResultSet(res);
            }
            return null;
        } catch (SQLException e) {
            log.error("SQL Exception in readClause:" + e.getMessage());
            throw e;
        }
    }

    /**
     * Updates an entry in the restricted IDs table. Also updates the mTime of the related record in ds-storage.
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
     * Delete an entry from the restricted IDs table.
     *
     * @param id_value value of the ID
     * @param id_type type of ID
     * @param platform platform where the ID is restricted
     * @throws SQLException
     */
    public void deleteRestrictedId(String id_value, String id_type, String platform) throws SQLException {
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


    /**
     * Get an entry from the restricted IDs table by internal ID.
     *
     * @param internalId to get entry for.
     * @return a {@link RestrictedIdOutputDto}
     */
    public RestrictedIdOutputDto getRestrictedIdByInternalId(String internalId) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(readRestrictedIdQueryByInternalId)) {
            stmt.setString(1, internalId);
            ResultSet res = stmt.executeQuery();
            while (res.next()) {
                return createRestrictedIdOutputDtoFromResultSet(res);
            }
            return null;
        } catch (SQLException e) {
            log.error("SQL Exception in readClause:" + e.getMessage());
            throw e;
        }
    }

    /**
     * Delete an entry from the restricted IDs table by internal ID.
     *
     * @param internalId to delete entry for.
     */
    public int deleteRestrictedIdByInternalId(String internalId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(deleteRestrictedIdByInternalIdQuery)) {
            statement.setString(1, internalId);
            int result = statement.executeUpdate();
            log.info("Deleted '{}' documents by internal ID: '{}'", result, internalId);
            return result;
        } catch (SQLException e){
            log.error("SQL Exception in delete internal ID: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Get all restricted Ids from the database
     *
     * @param idType add clause on this idType
     * @param platform add clause on this platform
     * @return
     */
    public List<RestrictedIdOutputDto> getAllRestrictedIds(String idType, String platform) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(allRestrictedIdsQuery)) {
            stmt.setString(1,StringUtils.isEmpty(idType) ? "%" : idType);
            stmt.setString(2,StringUtils.isEmpty(platform) ? "%" : platform);

            ResultSet res = stmt.executeQuery();
            List<RestrictedIdOutputDto> output = new ArrayList<>();
            while (res.next()) {
                RestrictedIdOutputDto restrictedIdOutputDto = createRestrictedIdOutputDtoFromResultSet(res);
                output.add(restrictedIdOutputDto);
            }
            return output;
        } catch (SQLException e) {
            log.error("SQL Exception in readClause:" + e.getMessage());
            throw e;
        }
    }

    /**
     * Create a holdback rule for DR content
     *
     * @param id id of the rule
     * @param name name of the rule
     * @param days number of holdback days
     * @throws SQLException
     */
    public void createDrHoldbackRule(String id, String name, int days) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(createDrHoldbackRuleQuery)) {
            stmt.setString(1, id);
            stmt.setString(2, name);
            stmt.setInt(3, days);
            stmt.execute();
        } catch (SQLException e) {
            log.error("SQL Exception in createDrHoldbackRule:" + e.getMessage());
            throw e;
        }
    }

    /**
     * Delete a holdback rule for DR content
     * @param id id of the rule
     * @throws SQLException
     */
    public void deleteDrHoldbackRule(String id) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(deleteDrHoldbackRuleQuery)) {
            stmt.setString(1,id);
            stmt.execute();
        } catch (SQLException e) {
            log.error("SQL Exception in createDrHoldbackRule:" + e.getMessage());
            throw e;
        }
    }

    /**
     * Get the number of holdback days
     * @param id id of the holdback rule
     * @return the number of holdback days or -1 if not holdback rule is found.
     * @throws SQLException
     */
    public int getDrHoldbackdaysFromID(String id) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(getDrHoldbackDaysFromIDQuery)) {
            stmt.setString(1,id);
            ResultSet res = stmt.executeQuery();
            if (res.next()) {
                return res.getInt(DR_HOLDBACK_RULES_DAYS);
            }
            return -1;
        } catch (SQLException e) {
            log.error("SQL Exception in createDrHoldbackRule:" + e.getMessage());
            throw e;
        }
    }

    /**
     * Get the number of holdback days
     *
     * @param name name of the holdback rule
     * @return
     * @throws SQLException
     */
    public int getDrHoldbackDaysFromName(String name) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(getDrHoldbackDaysFromNameQuery)) {
            stmt.setString(1,name);
            ResultSet res = stmt.executeQuery();
            if (res.next()) {
                return res.getInt(DR_HOLDBACK_RULES_DAYS);
            }
            return -1;
        } catch (SQLException e) {
            log.error("SQL Exception in createDrHoldbackRule:" + e.getMessage());
            throw e;
        }
    }

    /**
     * Get a DR holdback rule
     * @param id id of the rule
     * @return
     * @throws SQLException
     */
    public DrHoldbackRuleDto getDrHoldbackFromID(String id) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(getDrHoldbackRuleFromId)) {
            stmt.setString(1,id);
            ResultSet res = stmt.executeQuery();
            if (res.next()) {
                DrHoldbackRuleDto output = new DrHoldbackRuleDto();
                output.setId(res.getString(DR_HOLDBACK_RULES_ID));
                output.setName(res.getString(DR_HOLDBACK_RULES_NAME));
                output.setDays(res.getInt(DR_HOLDBACK_RULES_DAYS));
                return output;
            }
            return null;
        } catch (SQLException e) {
            log.error("SQL Exception in createDrHoldbackRule:" + e.getMessage());
            throw e;
        }
    }

    /**
     * update the number of holdback days of a holdback rule
     * @param days number of days
     * @param id id of the rule
     * @throws SQLException
     */
    public void updateDrHolbackdaysForId(int days, String id) throws SQLException {
        try(PreparedStatement stmt = connection.prepareStatement(updateDrHoldbackDaysForId)) {
            stmt.setInt(1,days);
            stmt.setString(2,id);
            stmt.execute();
        } catch (SQLException e) {
            log.error("SQL Exception in updateDrHolbackdaysForId:" + e.getMessage());
            throw e;
        }
    }

    /**
     * update the number of holdback days of a holdback rule
     * @param days number of days
     * @param name name of the rule
     * @throws SQLException
     */
    public void updateDrHolbackdaysForName(int days, String name) throws SQLException {
        try(PreparedStatement stmt = connection.prepareStatement(updateDrHoldbackDaysForName)) {
            stmt.setInt(1,days);
            stmt.setString(2,name);
            stmt.execute();
        } catch (SQLException e) {
            log.error("SQL Exception in updateDrHolbackdaysForName:" + e.getMessage());
            throw e;
        }
    }

    /**
     * list all holdback rules
     * @return a list of rules
     * @throws SQLException if fails
     */
    public List<DrHoldbackRuleDto> getAllDrHoldbackRules() throws SQLException {
        try(PreparedStatement stmt = connection.prepareStatement(getAllDrHoldbackRulesQuery)) {
            List<DrHoldbackRuleDto> output = new ArrayList<>();
            ResultSet res = stmt.executeQuery();
            while (res.next()) {
                DrHoldbackRuleDto rule = new DrHoldbackRuleDto();
                rule.setId(res.getString(DR_HOLDBACK_RULES_ID));
                rule.setName(res.getString(DR_HOLDBACK_RULES_NAME));
                rule.setDays(res.getInt(DR_HOLDBACK_RULES_DAYS));
                output.add(rule);
            }
            return output;
        } catch (SQLException e) {
            log.error("SQL Exception in getAllDrHoldbackRules:" + e.getMessage());
            throw e;
        }
    }

    /**
     * Get the dr_holdback_id from content and form metadata values.
     *
     * @param content the content id
     * @param form the form id
     * @return
     * @throws SQLException
     */
    public String getHoldbackRuleId(int content, int form) throws SQLException {
        try(PreparedStatement stmt = connection.prepareStatement(getHoldbackIdFromContentAndForm)) {
            stmt.setInt(1,content);
            stmt.setInt(2,content);
            stmt.setInt(3,form);
            stmt.setInt(4,form);
            ResultSet res = stmt.executeQuery();
            if (res.next()) {
                return res.getString(DR_HOLDBACK_MAP_HOLDBACK_ID);
            }
            return null;
        } catch (SQLException e) {
            log.error("SQL Exception in get holdback rule ID:" + e.getMessage());
            throw e;
        }

    }

    /**
     * add a content and form range to a dr_holdback id
     *
     * @param content_range_from
     * @param content_range_to
     * @param form_range_from
     * @param form_range_to
     * @param holdback_id
     * @throws SQLException
     */
    public void createDrHoldbackMapping(int content_range_from, int content_range_to, int form_range_from, int form_range_to, String holdback_id) throws SQLException {
        long uniqueID = generateUniqueID();
        try(PreparedStatement stmt = connection.prepareStatement(createHoldbackMapping)) {
            stmt.setLong(1,uniqueID);
            stmt.setInt(2,content_range_from);
            stmt.setInt(3,content_range_to);
            stmt.setInt(4,form_range_from);
            stmt.setInt(5,form_range_to);
            stmt.setString(6,holdback_id);
            stmt.execute();
        } catch (SQLException e) {
            log.error("SQL Exception in get holdback rule ID:" + e.getMessage());
            throw e;
        }
    }

    /**
     * get all form and content ranges for a dr holdback-id
     *
     * @param holdbackId
     * @return
     * @throws SQLException
     */
    public List<DrHoldbackRangeMappingDto> getHoldbackRangesForHoldbackId(String holdbackId) throws SQLException {
        try(PreparedStatement stmt = connection.prepareStatement(getRangesForDrHoldbackId)) {
            stmt.setString(1, holdbackId);
            ResultSet result = stmt.executeQuery();
            List<DrHoldbackRangeMappingDto> output = new ArrayList<>();
            while(result.next()) {
                DrHoldbackRangeMappingDto mapping = new DrHoldbackRangeMappingDto();
                mapping.setId(result.getString(DR_HOLDBACK_MAP_ID));
                mapping.setContentRangeFrom(result.getInt(DR_HOLDBACK_MAP_CONTENT_FROM));
                mapping.setContentRangeTo(result.getInt(DR_HOLDBACK_MAP_CONTENT_TO));
                mapping.setFormRangeFrom(result.getInt(DR_HOLDBACK_MAP_FORM_FROM));
                mapping.setFormRangeTo(result.getInt(DR_HOLDBACK_MAP_FORM_TO));
                mapping.setDrHoldbackId(result.getString(DR_HOLDBACK_MAP_HOLDBACK_ID));
                output.add(mapping);
            }
            return output;
        } catch (SQLException e) {
            log.error("SQL Exception delete ranges for holdback id" + e.getMessage());
            throw e;
        }
    }

    /**
     * Delete all holback mappings for a dr_holdback_id
     *
     * @param holdbackId
     * @throws SQLException
     */
    public void deleteMappingsForDrHolbackId(String holdbackId) throws SQLException {
        try(PreparedStatement stmt = connection.prepareStatement(deleteRangesForDrHoldbackId)) {
            stmt.setString(1, holdbackId);
            stmt.execute();
        } catch (SQLException e) {
            log.error("SQL Exception delete ranges for holdback id" + e.getMessage());
            throw e;
        }
    }



    private String convertToHumanReadable(Long modifiedTime) {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(modifiedTime), ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(ServiceConfig.getConfig().getString("human-readable-date-format","yyyy-MM-dd HH:mm:ss"), Locale.ENGLISH);
        return localDateTime.format(formatter);
    }

     /*
     * Only called from unittests, not exposed on facade class
     *
     */
    public void clearTableRecords() throws SQLException {
        ArrayList<String> tables = new ArrayList<>();
        tables.add("RESTRICTED_IDS");
        tables.add("DR_HOLDBACK_MAP");
        tables.add("DR_HOLDBACK_RULES");

        for (String table : tables) {
            String deleteSQL="DELETE FROM " +table;
            try (PreparedStatement stmt = connection.prepareStatement(deleteSQL)) {
                stmt.execute();
            }
        }
        connection.commit();
        log.info("All tables cleared for unittest");
    }

    /**
     * Create a {@link RestrictedIdOutputDto} from a ResultSet, which should contain all needed values for the DTO
     * @param resultSet containing values from the backing Rights database
     */
    private RestrictedIdOutputDto createRestrictedIdOutputDtoFromResultSet(ResultSet resultSet) throws SQLException {
        RestrictedIdOutputDto output = new RestrictedIdOutputDto();
        output.setInternalId(resultSet.getString(RESTRICTED_ID_ID));
        output.setIdValue(resultSet.getString(RESTRICTED_ID_IDVALUE));
        output.setIdType(resultSet.getString(RESTRICTED_ID_IDTYPE));
        output.setPlatform(resultSet.getString(RESTRICTED_ID_PLATFORM));
        output.setComment(resultSet.getString(RESTRICTED_ID_COMMENT));
        output.setModifiedBy(resultSet.getString(RESTRICTED_ID_MODIFIED_BY));
        output.setModifiedTime(resultSet.getLong(RESTRICTED_ID_MODIFIED_TIME));
        output.setModifiedTimeHuman(convertToHumanReadable(output.getModifiedTime()));
        return output;
    }

    /*
     * Only called from unittests, not exposed on facade class
     *
     */
    public void clearRestrictedIds() throws SQLException {
        String deleteSQL="DELETE FROM RESTRICTED_IDS";
        try (PreparedStatement stmt = connection.prepareStatement(deleteSQL)) {
            stmt.execute();
        }
    }
}   


