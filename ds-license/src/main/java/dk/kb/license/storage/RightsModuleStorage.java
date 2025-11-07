package dk.kb.license.storage;

import dk.kb.license.model.v1.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class for interacting with the part of DS-License which handles the calculation of rights, such as holdbacks and restrictions.
 */
public class RightsModuleStorage extends AuditLogModuleStorage {
    private static final Logger log = LoggerFactory.getLogger(RightsModuleStorage.class);

    private final String RESTRICTED_ID_TABLE = "restricted_ids";

    private final String RESTRICTED_ID_ID = "id";
    private final String RESTRICTED_ID_IDVALUE = "id_value";
    private final String RESTRICTED_ID_IDTYPE = "id_type";
    private final String RESTRICTED_ID_PLATFORM = "platform";
    private final String RESTRICTED_ID_COMMENT = "comment";

    private final String createRestrictedIdQuery = "INSERT INTO " + RESTRICTED_ID_TABLE +
            " (" + RESTRICTED_ID_ID + "," + RESTRICTED_ID_IDVALUE + "," + RESTRICTED_ID_IDTYPE + "," + RESTRICTED_ID_PLATFORM + "," +
            RESTRICTED_ID_COMMENT + ")" +
            " VALUES (?,?,?,?,?)";
    private final String readRestrictedIdQuery = "SELECT * FROM " + RESTRICTED_ID_TABLE + " WHERE " + RESTRICTED_ID_IDVALUE + " = ? AND " + RESTRICTED_ID_IDTYPE + " = ? AND " + RESTRICTED_ID_PLATFORM + " = ? ";
    private final String readRestrictedIdByIdQuery = "SELECT * FROM " + RESTRICTED_ID_TABLE + " WHERE " + RESTRICTED_ID_ID + " = ?";

    private final String readRestrictedIdCommentByIdValueQuery = "SELECT comment FROM " + RESTRICTED_ID_TABLE + " WHERE " + RESTRICTED_ID_IDVALUE + " = ?";

    private final String updateRestrictedIdCommentQuery = "UPDATE " + RESTRICTED_ID_TABLE + " SET " +
            RESTRICTED_ID_COMMENT + " = ? " +
            " WHERE " +
            RESTRICTED_ID_ID + " = ?";
    private final String deleteRestrictedIdQuery = "DELETE FROM " + RESTRICTED_ID_TABLE + " WHERE " + RESTRICTED_ID_IDVALUE + " = ? AND " + RESTRICTED_ID_IDTYPE + " = ? AND " + RESTRICTED_ID_PLATFORM + " = ? ";
    private final String deleteRestrictedIdByIdQuery = "DELETE FROM " + RESTRICTED_ID_TABLE + " WHERE " + RESTRICTED_ID_ID + " = ?";

    private final String allRestrictedIdsQuery = "SELECT * FROM " + RESTRICTED_ID_TABLE + " WHERE " + RESTRICTED_ID_IDTYPE + " = ? AND " + RESTRICTED_ID_PLATFORM + " = ?";

    private final String DR_HOLDBACK_RULES_TABLE = "DR_HOLDBACK_RULES";
    private final String DR_HOLDBACK_RULES_ID = "id";
    private final String DR_HOLDBACK_RULES_VALUE = "dr_holdback_value";
    private final String DR_HOLDBACK_RULES_NAME = "name";
    private final String DR_HOLDBACK_RULES_DAYS = "days";

    private final String DR_HOLDBACK_RANGES_TABLE = "DR_HOLDBACK_RANGES";
    private final String DR_HOLDBACK_RANGES_ID = "id";
    private final String DR_HOLDBACK_RANGES_CONTENT_FROM = "content_range_from";
    private final String DR_HOLDBACK_RANGES_CONTENT_TO = "content_range_to";
    private final String DR_HOLDBACK_RANGES_FORM_FROM = "form_range_from";
    private final String DR_HOLDBACK_RANGES_FORM_TO = "form_range_to";
    private final String DR_HOLDBACK_RANGES_VALUE = "dr_holdback_value";

    private final String createDrHoldbackRuleQuery = "INSERT INTO " + DR_HOLDBACK_RULES_TABLE +
            " (" + DR_HOLDBACK_RULES_ID + "," + DR_HOLDBACK_RULES_VALUE + "," + DR_HOLDBACK_RULES_NAME + "," + DR_HOLDBACK_RULES_DAYS + ")" +
            " VALUES (?,?,?,?)";
    private final String updateDrHoldbackRuleQuery = "UPDATE " + DR_HOLDBACK_RULES_TABLE
            + " SET " + DR_HOLDBACK_RULES_DAYS + " = ?"
            + " WHERE " + DR_HOLDBACK_RULES_ID + " = ?";
    private final String deleteDrHoldbackRuleQuery = "DELETE FROM " + DR_HOLDBACK_RULES_TABLE +
            " WHERE " + DR_HOLDBACK_RULES_ID + " = ?";
    private final String getDrHoldbackRuleByIdQuery = "SELECT * FROM " + DR_HOLDBACK_RULES_TABLE
            + " WHERE " + DR_HOLDBACK_RULES_ID + " = ?";
    private final String getDrHoldbackRuleByDrHoldbackValueQuery = "SELECT * FROM " + DR_HOLDBACK_RULES_TABLE
            + " WHERE " + DR_HOLDBACK_RULES_VALUE + " = ?";
    private final String getDrHoldbackRulesQuery = "SELECT * FROM " + DR_HOLDBACK_RULES_TABLE;
    private final String createDrHoldbackRangeQuery = "INSERT INTO " + DR_HOLDBACK_RANGES_TABLE +
            "(" + DR_HOLDBACK_RANGES_ID + "," + DR_HOLDBACK_RANGES_CONTENT_FROM + "," + DR_HOLDBACK_RANGES_CONTENT_TO + "," + DR_HOLDBACK_RANGES_FORM_FROM + "," + DR_HOLDBACK_RANGES_FORM_TO + "," + DR_HOLDBACK_RANGES_VALUE + ")" +
            " VALUES (?,?,?,?,?,?)";

    private final String getDrHoldbackValueByContentAndFormQuery = "SELECT " + DR_HOLDBACK_RANGES_VALUE +
            " FROM " + DR_HOLDBACK_RANGES_TABLE + " WHERE "
            + DR_HOLDBACK_RANGES_CONTENT_FROM + " <= ? AND "
            + DR_HOLDBACK_RANGES_CONTENT_TO + " >= ?  AND "
            + DR_HOLDBACK_RANGES_FORM_FROM + " <= ? AND "
            + DR_HOLDBACK_RANGES_FORM_TO + " >= ? ";


    private final String getDrHoldbackRangesByDrHoldbackValueQuery = "SELECT * FROM " + DR_HOLDBACK_RANGES_TABLE + " WHERE " + DR_HOLDBACK_RANGES_VALUE + " = ?";
    private final String deleteRangesByDrHoldbackValueQuery = "DELETE FROM " + DR_HOLDBACK_RANGES_TABLE + " WHERE " + DR_HOLDBACK_RANGES_VALUE + " = ?";


    public RightsModuleStorage() throws SQLException {
        super();
    }

    /**
     * Creates an entry in the restrictedID table. Also updates the mTime for the related record in ds-storage.
     *
     * @param id_value The value of the ID
     * @param id_type  The type of the ID
     * @param platform The platform where the object is restricted (e.g DR)
     * @param comment  Just a comment
     * @throws SQLException
     */
    public long createRestrictedId(String id_value, String id_type, String platform, String comment) throws SQLException {
        long uniqueID = generateUniqueID();

        try (PreparedStatement stmt = connection.prepareStatement(createRestrictedIdQuery)) {
            stmt.setLong(1, uniqueID);
            stmt.setString(2, id_value);
            stmt.setString(3, id_type);
            stmt.setString(4, platform);
            stmt.setString(5, comment);
            stmt.execute();
        } catch (SQLException e) {
            log.error("SQL Exception in createRestrictedId", e);
            throw e;
        }
        return uniqueID;
    }

    /**
     * Gets an entry from the restrinctedID table
     *
     * @param id_value value of the ID
     * @param id_type  type of ID
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
            log.error("SQL Exception in getRestrictedId", e);
            throw e;
        }
    }

    /**
     * Updates an entry in the restricted IDs table. Also updates the mTime of the related record in ds-storage.
     *
     * @param comment Just a comment
     * @throws SQLException
     */
    public void updateRestrictedIdComment(long id, String comment) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(updateRestrictedIdCommentQuery)) {
            stmt.setString(1, comment);
            stmt.setLong(2, id);
            stmt.execute();
        } catch (SQLException e) {
            log.error("SQL Exception in updateRestrictedIdComment" + e);
            throw e;
        }
    }

    /**
     * Delete an entry from the restricted IDs table.
     *
     * @param id_value value of the ID
     * @param id_type  type of ID
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
            log.error("SQL Exception in deleteRestrictedId", e);
            throw e;
        }
    }


    /**
     * Get an entry from the restricted IDs table by ID.
     *
     * @param id to get entry for.
     * @return a {@link RestrictedIdOutputDto}
     */
    public RestrictedIdOutputDto getRestrictedIdById(Long id) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(readRestrictedIdByIdQuery)) {
            stmt.setLong(1, id);
            ResultSet res = stmt.executeQuery();
            while (res.next()) {
                return createRestrictedIdOutputDtoFromResultSet(res);
            }
            return null;
        } catch (SQLException e) {
            log.error("SQL Exception in getRestrictedIdById", e);
            throw e;
        }
    }

    /**
     * Get an entry from the "restricted_id" table by id_value (dsId).
     *
     * @param dsId to get entry for.
     * @return a restricted_id comment
     */
    public String getRestrictedIdCommentByIdValue(String dsId) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(readRestrictedIdCommentByIdValueQuery)) {
            stmt.setString(1, dsId);
            ResultSet res = stmt.executeQuery();

            if (res.next()) {
                return res.getString(RESTRICTED_ID_COMMENT);
            }

            return null;
        } catch (SQLException e) {
            log.error("SQL Exception in getRestrictedIdCommentByIdValue", e);
            throw e;
        }
    }

    /**
     * Delete an entry from the restricted IDs table by id
     *
     * @param id to delete entry for.
     */
    public int deleteRestrictedIdById(Long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(deleteRestrictedIdByIdQuery)) {
            statement.setLong(1, id);
            int result = statement.executeUpdate();
            log.info("Deleted: {} documents by id: {}", result, id);
            return result;
        } catch (SQLException e) {
            log.error("SQL Exception in deleteRestrictedIdById", e);
            throw e;
        }
    }

    /**
     * Get all restricted Ids from the database
     *
     * @param idType   add clause on this idType
     * @param platform add clause on this platform
     * @return
     */
    public List<RestrictedIdOutputDto> getAllRestrictedIds(String idType, String platform) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(allRestrictedIdsQuery)) {
            stmt.setString(1, idType);
            stmt.setString(2, platform);

            ResultSet res = stmt.executeQuery();
            List<RestrictedIdOutputDto> output = new ArrayList<>();
            while (res.next()) {
                RestrictedIdOutputDto restrictedIdOutputDto = createRestrictedIdOutputDtoFromResultSet(res);
                output.add(restrictedIdOutputDto);
            }
            return output;
        } catch (SQLException e) {
            log.error("SQL Exception in getAllRestrictedIds", e);
            throw e;
        }
    }

    /**
     * Create a holdback rule for DR content
     *
     * @param drHoldbackValue value of the rule
     * @param name            name of the rule
     * @param days            number of holdback days
     * @return uniqueId       id of the rule
     * @throws SQLException
     */
    public long createDrHoldbackRule(String drHoldbackValue, String name, int days) throws SQLException {
        long uniqueId = generateUniqueID();
        try (PreparedStatement stmt = connection.prepareStatement(createDrHoldbackRuleQuery)) {
            stmt.setLong(1, uniqueId);
            stmt.setString(2, drHoldbackValue);
            stmt.setString(3, name);
            stmt.setInt(4, days);
            stmt.execute();
        } catch (SQLException e) {
            log.error("SQL Exception in createDrHoldbackRule", e);
            throw e;
        }

        return uniqueId;
    }

    /**
     * update the number of holdback days of a holdback rule
     *
     * @param id value of the holdback rule
     * @param days            number of days
     * @throws SQLException
     */
    public void updateDrHoldbackRule(Long id, int days) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(updateDrHoldbackRuleQuery)) {
            stmt.setInt(1, days);
            stmt.setLong(2, id);
            stmt.execute();
        } catch (SQLException e) {
            log.error("SQL Exception in updateDrHoldbackRule", e);
            throw e;
        }
    }

    /**
     * Delete a holdback rule for DR content
     *
     * @param id value of the holdback rule
     * @throws SQLException
     */
    public int deleteDrHoldbackRule(Long id) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(deleteDrHoldbackRuleQuery)) {
            stmt.setLong(1, id);
            int result = stmt.executeUpdate();
            log.info("Deleted: {} documents by id: {}", result, id);
            return result;
        } catch (SQLException e) {
            log.error("SQL Exception in deleteDrHoldbackRule", e);
            throw e;
        }
    }

    /**
     * Get a DR holdback rule by id
     *
     * @param id unique id of the holdback rule
     * @return
     * @throws SQLException
     */
    public DrHoldbackRuleOutputDto getDrHoldbackRuleById(Long id) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(getDrHoldbackRuleByIdQuery)) {
            stmt.setLong(1, id);
            ResultSet res = stmt.executeQuery();
            if (res.next()) {
                DrHoldbackRuleOutputDto output = new DrHoldbackRuleOutputDto();
                output.setId(res.getLong(DR_HOLDBACK_RULES_ID));
                output.setDrHoldbackValue(res.getString(DR_HOLDBACK_RULES_VALUE));
                output.setName(res.getString(DR_HOLDBACK_RULES_NAME));
                output.setDays(res.getInt(DR_HOLDBACK_RULES_DAYS));
                return output;
            }
            return null;
        } catch (SQLException e) {
            log.error("SQL Exception in getDrHoldbackRuleById", e);
            throw e;
        }
    }

    /**
     * Get a DR holdback rule by drHoldbackValue
     *
     * @param drHoldbackValue value of the holdback rule
     * @return
     * @throws SQLException
     */
    public DrHoldbackRuleOutputDto getDrHoldbackRuleByDrHoldbackValue(String drHoldbackValue) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(getDrHoldbackRuleByDrHoldbackValueQuery)) {
            stmt.setString(1, drHoldbackValue);
            ResultSet res = stmt.executeQuery();
            if (res.next()) {
                DrHoldbackRuleOutputDto output = new DrHoldbackRuleOutputDto();
                output.setId(res.getLong(DR_HOLDBACK_RULES_ID));
                output.setDrHoldbackValue(res.getString(DR_HOLDBACK_RULES_VALUE));
                output.setName(res.getString(DR_HOLDBACK_RULES_NAME));
                output.setDays(res.getInt(DR_HOLDBACK_RULES_DAYS));
                return output;
            }
            return null;
        } catch (SQLException e) {
            log.error("SQL Exception in getDrHoldbackRuleByDrHoldbackValue", e);
            throw e;
        }
    }

    /**
     * list all holdback rules
     *
     * @return a list of rules
     * @throws SQLException if fails
     */
    public List<DrHoldbackRuleOutputDto> getDrHoldbackRules() throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(getDrHoldbackRulesQuery)) {
            List<DrHoldbackRuleOutputDto> output = new ArrayList<>();
            ResultSet res = stmt.executeQuery();
            while (res.next()) {
                DrHoldbackRuleOutputDto rule = new DrHoldbackRuleOutputDto();
                rule.setId(res.getLong(DR_HOLDBACK_RULES_ID));
                rule.setDrHoldbackValue(res.getString(DR_HOLDBACK_RULES_VALUE));
                rule.setName(res.getString(DR_HOLDBACK_RULES_NAME));
                rule.setDays(res.getInt(DR_HOLDBACK_RULES_DAYS));
                output.add(rule);
            }
            return output;
        } catch (SQLException e) {
            log.error("SQL Exception in getDrHoldbackRules", e);
            throw e;
        }
    }

    /**
     * add a content and form range to a drHoldbackValue
     *
     * @param contentRangeFrom
     * @param contentRangeTo
     * @param formRangeFrom
     * @param formRangeTo
     * @param drHoldbackValue
     * @throws SQLException
     */
    public long createDrHoldbackRange(int contentRangeFrom, int contentRangeTo, int formRangeFrom, int formRangeTo, String drHoldbackValue) throws SQLException {
        long uniqueId = generateUniqueID();
        try (PreparedStatement stmt = connection.prepareStatement(createDrHoldbackRangeQuery)) {
            stmt.setLong(1, uniqueId);
            stmt.setInt(2, contentRangeFrom);
            stmt.setInt(3, contentRangeTo);
            stmt.setInt(4, formRangeFrom);
            stmt.setInt(5, formRangeTo);
            stmt.setString(6, drHoldbackValue);
            stmt.execute();
        } catch (SQLException e) {
            log.error("SQL Exception in createDrHoldbackRange", e);
            throw e;
        }
        return uniqueId;
    }

    /**
     * Delete all holdback ranges for a drHoldbackValue
     *
     * @param drHoldbackValue
     * @throws SQLException
     */
    public int deleteRangesByDrHoldbackValue(String drHoldbackValue) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(deleteRangesByDrHoldbackValueQuery)) {
            stmt.setString(1, drHoldbackValue);
            int result = stmt.executeUpdate();
            log.info("Deleted: {} documents by drHoldbackValue: {}", result, drHoldbackValue);
            return result;
        } catch (SQLException e) {
            log.error("SQL Exception in deleteRangesByDrHoldbackValue", e);
            throw e;
        }
    }

    /**
     * get all form and content ranges for a drHoldbackValue
     *
     * @param drHoldbackValue
     * @return
     * @throws SQLException
     */
    public List<DrHoldbackRangeOutputDto> getDrHoldbackRangesByDrHoldbackValue(String drHoldbackValue) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(getDrHoldbackRangesByDrHoldbackValueQuery)) {
            stmt.setString(1, drHoldbackValue);
            ResultSet result = stmt.executeQuery();
            List<DrHoldbackRangeOutputDto> output = new ArrayList<>();
            while (result.next()) {
                DrHoldbackRangeOutputDto rangeDto = new DrHoldbackRangeOutputDto();
                rangeDto.setId(result.getLong(DR_HOLDBACK_RANGES_ID));
                rangeDto.setContentRangeFrom(result.getInt(DR_HOLDBACK_RANGES_CONTENT_FROM));
                rangeDto.setContentRangeTo(result.getInt(DR_HOLDBACK_RANGES_CONTENT_TO));
                rangeDto.setFormRangeFrom(result.getInt(DR_HOLDBACK_RANGES_FORM_FROM));
                rangeDto.setFormRangeTo(result.getInt(DR_HOLDBACK_RANGES_FORM_TO));
                rangeDto.setDrHoldbackValue(result.getString(DR_HOLDBACK_RANGES_VALUE));
                output.add(rangeDto);
            }
            return output;
        } catch (SQLException e) {
            log.error("SQL Exception in getDrHoldbackRangesByDrHoldbackValue", e);
            throw e;
        }
    }

    /**
     * Get the drHoldbackValue from content and form metadata values.
     *
     * @param content the content id
     * @param form    the form id
     * @return
     * @throws SQLException
     */
    public String getDrHoldbackValueByContentAndForm(int content, int form) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(getDrHoldbackValueByContentAndFormQuery)) {
            stmt.setInt(1, content);
            stmt.setInt(2, content);
            stmt.setInt(3, form);
            stmt.setInt(4, form);
            ResultSet res = stmt.executeQuery();
            if (res.next()) {
                return res.getString(DR_HOLDBACK_RANGES_VALUE);
            }
            return null;
        } catch (SQLException e) {
            log.error("SQL Exception in getDrHoldbackValueByContentAndForm", e);
            throw e;
        }
    }

    /**
     * Create a {@link RestrictedIdOutputDto} from a ResultSet, which should contain all needed values for the DTO
     *
     * @param resultSet containing values from the backing Rights database
     */
    private RestrictedIdOutputDto createRestrictedIdOutputDtoFromResultSet(ResultSet resultSet) throws SQLException {
        RestrictedIdOutputDto output = new RestrictedIdOutputDto();
        output.setId(resultSet.getLong(RESTRICTED_ID_ID));
        output.setIdValue(resultSet.getString(RESTRICTED_ID_IDVALUE));
        output.setIdType(IdTypeEnumDto.fromValue(resultSet.getString(RESTRICTED_ID_IDTYPE)));
        output.setPlatform(PlatformEnumDto.fromValue(resultSet.getString(RESTRICTED_ID_PLATFORM)));
        output.setComment(resultSet.getString(RESTRICTED_ID_COMMENT));
        return output;
    }
}   


