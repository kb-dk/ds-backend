package dk.kb.license.storage;

import dk.kb.license.mapper.DrHoldbackCategoryOutputDtoMapper;
import dk.kb.license.mapper.DrHoldbackRangeOutputDtoMapper;
import dk.kb.license.mapper.RestrictedIdOutputDtoMapper;
import dk.kb.license.model.v1.DrHoldbackCategoryOutputDto;
import dk.kb.license.model.v1.DrHoldbackRangeOutputDto;
import dk.kb.license.model.v1.RestrictedIdOutputDto;
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

    private final static RestrictedIdOutputDtoMapper restrictedIdOutputDtoMapper = new RestrictedIdOutputDtoMapper();
    private final static DrHoldbackCategoryOutputDtoMapper drHoldbackCategoryOutputDtoMapper = new DrHoldbackCategoryOutputDtoMapper();
    private final static DrHoldbackRangeOutputDtoMapper drHoldbackRangeOutputDtoMapper = new DrHoldbackRangeOutputDtoMapper();

    private final String RESTRICTED_ID_TABLE = "restricted_ids";

    private final String RESTRICTED_ID_ID = "id";
    private final String RESTRICTED_ID_IDVALUE = "id_value";
    private final String RESTRICTED_ID_IDTYPE = "id_type";
    private final String RESTRICTED_ID_PLATFORM = "platform";
    private final String RESTRICTED_ID_TITLE = "title";
    private final String RESTRICTED_ID_COMMENT = "comment";

    private final String createRestrictedIdQuery = "INSERT INTO " + RESTRICTED_ID_TABLE +
            " (" + RESTRICTED_ID_ID + "," + RESTRICTED_ID_IDVALUE + "," + RESTRICTED_ID_IDTYPE + "," + RESTRICTED_ID_PLATFORM + "," +
            RESTRICTED_ID_TITLE + "," + RESTRICTED_ID_COMMENT + ")" +
            " VALUES (?,?,?,?,?,?)";
    private final String readRestrictedIdQuery = "SELECT * FROM " + RESTRICTED_ID_TABLE + " WHERE " + RESTRICTED_ID_IDVALUE + " = ? AND " + RESTRICTED_ID_IDTYPE + " = ? AND " + RESTRICTED_ID_PLATFORM + " = ? ";
    private final String readRestrictedIdByIdQuery = "SELECT * FROM " + RESTRICTED_ID_TABLE + " WHERE " + RESTRICTED_ID_ID + " = ?";

    private final String readRestrictedIdCommentByIdValueQuery = "SELECT comment FROM " + RESTRICTED_ID_TABLE + " WHERE " + RESTRICTED_ID_IDVALUE + " = ?";
    private final String updateRestrictedIdQuery = "UPDATE " + RESTRICTED_ID_TABLE + " SET " +
            RESTRICTED_ID_TITLE + " = ? " + ", " +
            RESTRICTED_ID_COMMENT + " = ? " +
            "WHERE " +
            RESTRICTED_ID_ID + " = ?";
    private final String deleteRestrictedIdQuery = "DELETE FROM " + RESTRICTED_ID_TABLE + " WHERE " + RESTRICTED_ID_IDVALUE + " = ? AND " + RESTRICTED_ID_IDTYPE + " = ? AND " + RESTRICTED_ID_PLATFORM + " = ? ";
    private final String deleteRestrictedIdByIdQuery = "DELETE FROM " + RESTRICTED_ID_TABLE + " WHERE " + RESTRICTED_ID_ID + " = ?";

    private final String allRestrictedIdsQuery = "SELECT * FROM " + RESTRICTED_ID_TABLE + " WHERE " + RESTRICTED_ID_IDTYPE + " = ? AND " + RESTRICTED_ID_PLATFORM + " = ?";

    private final String DR_HOLDBACK_CATEGORIES_TABLE = "dr_holdback_categories";
    private final String DR_HOLDBACK_CATEGORIES_ID = "id";
    private final String DR_HOLDBACK_CATEGORIES_KEY = "key";
    private final String DR_HOLDBACK_CATEGORIES_NAME = "name";
    private final String DR_HOLDBACK_CATEGORIES_DAYS = "days";

    private final String DR_HOLDBACK_RANGES_TABLE = "dr_holdback_ranges";
    private final String DR_HOLDBACK_RANGES_ID = "id";
    private final String DR_HOLDBACK_RANGES_CONTENT_FROM = "content_range_from";
    private final String DR_HOLDBACK_RANGES_CONTENT_TO = "content_range_to";
    private final String DR_HOLDBACK_RANGES_FORM_FROM = "form_range_from";
    private final String DR_HOLDBACK_RANGES_FORM_TO = "form_range_to";
    private final String DR_HOLDBACK_RANGES_DR_HOLDBACK_CATEGORY_KEY = "dr_holdback_category_key";

    private final String createDrHoldbackCategoryQuery = "INSERT INTO " + DR_HOLDBACK_CATEGORIES_TABLE +
            " (" + DR_HOLDBACK_CATEGORIES_ID + ", \"" + DR_HOLDBACK_CATEGORIES_KEY + "\" ," + DR_HOLDBACK_CATEGORIES_NAME + ", " + DR_HOLDBACK_CATEGORIES_DAYS + ")" +
            " VALUES (?,?,?,?)";
    private final String updateDrHoldbackCategoryQuery = "UPDATE " + DR_HOLDBACK_CATEGORIES_TABLE
            + " SET " + DR_HOLDBACK_CATEGORIES_DAYS + " = ?"
            + " WHERE " + DR_HOLDBACK_CATEGORIES_ID + " = ?";
    private final String deleteDrHoldbackCategoryQuery = "DELETE FROM " + DR_HOLDBACK_CATEGORIES_TABLE +
            " WHERE " + DR_HOLDBACK_CATEGORIES_ID + " = ?";
    private final String getDrHoldbackCategoryByNameQuery = "SELECT * FROM " + DR_HOLDBACK_CATEGORIES_TABLE
            + " WHERE " + DR_HOLDBACK_CATEGORIES_NAME + " = ?";
    private final String getDrHoldbackCategoryByIdQuery = "SELECT * FROM " + DR_HOLDBACK_CATEGORIES_TABLE
            + " WHERE " + DR_HOLDBACK_CATEGORIES_ID + " = ?";
    private final String getDrHoldbackCategoryByKeyQuery = "SELECT * FROM " + DR_HOLDBACK_CATEGORIES_TABLE
            + " WHERE \"" + DR_HOLDBACK_CATEGORIES_KEY + "\" = ?";
    private final String getDrHoldbackCategoriesQuery = "SELECT * FROM " + DR_HOLDBACK_CATEGORIES_TABLE;
    private final String createDrHoldbackRangeQuery = "INSERT INTO " + DR_HOLDBACK_RANGES_TABLE +
            "(" + DR_HOLDBACK_RANGES_ID + "," + DR_HOLDBACK_RANGES_CONTENT_FROM + "," + DR_HOLDBACK_RANGES_CONTENT_TO + "," + DR_HOLDBACK_RANGES_FORM_FROM + "," + DR_HOLDBACK_RANGES_FORM_TO + "," + DR_HOLDBACK_RANGES_DR_HOLDBACK_CATEGORY_KEY + ")" +
            " VALUES (?,?,?,?,?,?)";

    private final String getDrHoldbackCategoryKeyByContentAndFormQuery = "SELECT " + DR_HOLDBACK_RANGES_DR_HOLDBACK_CATEGORY_KEY +
            " FROM " + DR_HOLDBACK_RANGES_TABLE + " WHERE "
            + DR_HOLDBACK_RANGES_CONTENT_FROM + " <= ? AND "
            + DR_HOLDBACK_RANGES_CONTENT_TO + " >= ?  AND "
            + DR_HOLDBACK_RANGES_FORM_FROM + " <= ? AND "
            + DR_HOLDBACK_RANGES_FORM_TO + " >= ? ";

    private final String getDrHoldbackRangesByDrHoldbackCategoryKeyQuery = "SELECT * FROM " + DR_HOLDBACK_RANGES_TABLE + " WHERE " + DR_HOLDBACK_RANGES_DR_HOLDBACK_CATEGORY_KEY + " = ?";
    private final String deleteDrHoldbackRangesByDrHoldbackCategoryKeyQuery = "DELETE FROM " + DR_HOLDBACK_RANGES_TABLE + " WHERE " + DR_HOLDBACK_RANGES_DR_HOLDBACK_CATEGORY_KEY + " = ?";

    public RightsModuleStorage() throws SQLException {
        super();
    }

    /**
     * Creates an entry in the restricted_ids table. Also updates the mTime for the related record in ds-storage.
     *
     * @param idValue  The value of the id
     * @param idType   The type of the id
     * @param platform The platform where the object is restricted (e.g DR)
     * @param title    Title on broadcast
     * @param comment  Just a comment
     * @throws SQLException
     */
    public long createRestrictedId(String idValue, String idType, String platform, String title, String comment) throws SQLException {
        long id = generateUniqueID();

        log.debug("generating unique restricted id: " + id);

        try (PreparedStatement stmt = connection.prepareStatement(createRestrictedIdQuery)) {
            stmt.setLong(1, id);
            stmt.setString(2, idValue);
            stmt.setString(3, idType);
            stmt.setString(4, platform);
            stmt.setString(5, title);
            stmt.setString(6, comment);
            stmt.execute();
        } catch (SQLException e) {
            log.error("SQL Exception in createRestrictedId", e);
            throw e;
        }
        return id;
    }

    /**
     * Gets an entry from the restricted_ids table.
     *
     * @param idValue  value of the id
     * @param idType   type of id
     * @param platform platform where the id is restricted
     * @return
     * @throws SQLException
     */
    public RestrictedIdOutputDto getRestrictedId(String idValue, String idType, String platform) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(readRestrictedIdQuery)) {
            stmt.setString(1, idValue);
            stmt.setString(2, idType);
            stmt.setString(3, platform);
            ResultSet res = stmt.executeQuery();

            while (res.next()) {
                return restrictedIdOutputDtoMapper.map(res);
            }

            return null;
        } catch (SQLException e) {
            log.error("SQL Exception in getRestrictedId", e);
            throw e;
        }
    }

    /**
     * Updates title and comment on an entry in the restricted_ids table.
     *
     * @param id      The unique restricted id
     * @param title   Title to update
     * @param comment Comment to update
     * @throws SQLException
     */
    public void updateRestrictedId(Long id, String title, String comment) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(updateRestrictedIdQuery)) {
            stmt.setString(1, title);
            stmt.setString(2, comment);
            stmt.setLong(3, id);
            stmt.execute();
        } catch (SQLException e) {
            log.error("SQL Exception in updateRestrictedId", e);
            throw e;
        }
    }

    /**
     * Delete an entry from the restricted_ids table.
     *
     * @param idValue  value of the id
     * @param idType   type of id
     * @param platform platform where the id is restricted
     * @throws SQLException
     */
    public void deleteRestrictedId(String idValue, String idType, String platform) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(deleteRestrictedIdQuery)) {
            stmt.setString(1, idValue);
            stmt.setString(2, idType);
            stmt.setString(3, platform);
            stmt.execute();
        } catch (SQLException e) {
            log.error("SQL Exception in deleteRestrictedId", e);
            throw e;
        }
    }

    /**
     * Get an entry from the restricted_ids table by id.
     *
     * @param id to get entry for.
     * @return a {@link RestrictedIdOutputDto}
     */
    public RestrictedIdOutputDto getRestrictedIdById(Long id) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(readRestrictedIdByIdQuery)) {
            stmt.setLong(1, id);
            ResultSet res = stmt.executeQuery();
            while (res.next()) {
                return restrictedIdOutputDtoMapper.map(res);
            }
            return null;
        } catch (SQLException e) {
            log.error("SQL Exception in getRestrictedIdById", e);
            throw e;
        }
    }

    /**
     * Get an entry from the restricted_ids table by id_value (dsId).
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
     * Delete an entry from the restricted_ids table by id
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
     * Get all restricted ids from the restricted_ids table
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
                RestrictedIdOutputDto restrictedIdOutputDto = restrictedIdOutputDtoMapper.map(res);
                output.add(restrictedIdOutputDto);
            }
            return output;
        } catch (SQLException e) {
            log.error("SQL Exception in getAllRestrictedIds", e);
            throw e;
        }
    }

    /**
     * Create a holdback category for DR content
     *
     * @param key  value of the DR holdback category
     * @param name name of the DR holdback category
     * @param days number of holdback days of the DR holdback category
     * @return uniqueId       id of the DR holdback category
     * @throws SQLException
     */
    public long createDrHoldbackCategory(String key, String name, int days) throws SQLException {
        long uniqueId = generateUniqueID();
        try (PreparedStatement stmt = connection.prepareStatement(createDrHoldbackCategoryQuery)) {
            stmt.setLong(1, uniqueId);
            stmt.setString(2, key);
            stmt.setString(3, name);
            stmt.setInt(4, days);
            stmt.execute();
        } catch (SQLException e) {
            log.error("SQL Exception in createDrHoldbackCategory", e);
            throw e;
        }

        return uniqueId;
    }

    /**
     * update the number of holdback days of a holdback category
     *
     * @param id   value of the holdback category
     * @param days number of days
     * @throws SQLException
     */
    public void updateDrHoldbackCategory(Long id, int days) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(updateDrHoldbackCategoryQuery)) {
            stmt.setInt(1, days);
            stmt.setLong(2, id);
            stmt.execute();
        } catch (SQLException e) {
            log.error("SQL Exception in updateDrHoldbackCategory", e);
            throw e;
        }
    }

    /**
     * Delete a holdback category for DR content
     *
     * @param id value of the holdback category
     * @throws SQLException
     */
    public int deleteDrHoldbackCategory(Long id) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(deleteDrHoldbackCategoryQuery)) {
            stmt.setLong(1, id);
            int result = stmt.executeUpdate();
            log.info("Deleted: {} documents by id: {}", result, id);
            return result;
        } catch (SQLException e) {
            log.error("SQL Exception in deleteDrHoldbackCategory", e);
            throw e;
        }
    }

    /**
     * Get a DR holdback category by id
     *
     * @param id unique id of the holdback category
     * @return
     * @throws SQLException
     */
    public DrHoldbackCategoryOutputDto getDrHoldbackCategoryById(Long id) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(getDrHoldbackCategoryByIdQuery)) {
            stmt.setLong(1, id);
            ResultSet res = stmt.executeQuery();
            if (res.next()) {
                return drHoldbackCategoryOutputDtoMapper.map(res);
            }
            return null;
        } catch (SQLException e) {
            log.error("SQL Exception in getDrHoldbackCategoryById", e);
            throw e;
        }
    }

    /**
     * Get a DR holdback category by name
     *
     * @param name name of holdback category
     * @return
     * @throws SQLException
     */
    public DrHoldbackCategoryOutputDto getDrHoldbackCategoryByName(String name) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(getDrHoldbackCategoryByNameQuery)) {
            stmt.setString(1, name);
            ResultSet res = stmt.executeQuery();
            if (res.next()) {
                return drHoldbackCategoryOutputDtoMapper.map(res);
            }
            return null;
        } catch (SQLException e) {
            log.error("SQL Exception in getDrHoldbackCategoryFromName: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Get a DR holdback category by key
     *
     * @param key value of the holdback category
     * @return
     * @throws SQLException
     */
    public DrHoldbackCategoryOutputDto getDrHoldbackCategoryByKey(String key) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(getDrHoldbackCategoryByKeyQuery)) {
            stmt.setString(1, key);
            ResultSet res = stmt.executeQuery();
            if (res.next()) {
                return drHoldbackCategoryOutputDtoMapper.map(res);
            }
            return null;
        } catch (SQLException e) {
            log.error("SQL Exception in getDrHoldbackCategoryByKey", e);
            throw e;
        }
    }

    /**
     * List all DR holdback categories
     *
     * @return a list of DR holdback categories
     * @throws SQLException if fails
     */
    public List<DrHoldbackCategoryOutputDto> getDrHoldbackCategories() throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(getDrHoldbackCategoriesQuery)) {
            List<DrHoldbackCategoryOutputDto> output = new ArrayList<>();
            ResultSet res = stmt.executeQuery();
            while (res.next()) {
                DrHoldbackCategoryOutputDto drHoldbackCategoryOutputDto = drHoldbackCategoryOutputDtoMapper.map(res);
                output.add(drHoldbackCategoryOutputDto);
            }
            return output;
        } catch (SQLException e) {
            log.error("SQL Exception in getDrHoldbackCategories", e);
            throw e;
        }
    }

    /**
     * add a content and form range to a drHoldbackCategoryKey
     *
     * @param contentRangeFrom
     * @param contentRangeTo
     * @param formRangeFrom
     * @param formRangeTo
     * @param drHoldbackCategoryKey
     * @throws SQLException
     */
    public long createDrHoldbackRange(int contentRangeFrom, int contentRangeTo, int formRangeFrom, int formRangeTo, String drHoldbackCategoryKey) throws SQLException {
        long uniqueId = generateUniqueID();
        try (PreparedStatement stmt = connection.prepareStatement(createDrHoldbackRangeQuery)) {
            stmt.setLong(1, uniqueId);
            stmt.setInt(2, contentRangeFrom);
            stmt.setInt(3, contentRangeTo);
            stmt.setInt(4, formRangeFrom);
            stmt.setInt(5, formRangeTo);
            stmt.setString(6, drHoldbackCategoryKey);
            stmt.execute();
        } catch (SQLException e) {
            log.error("SQL Exception in createDrHoldbackRange", e);
            throw e;
        }
        return uniqueId;
    }

    /**
     * Delete all holdback ranges for a drHoldbackCategoryKey
     *
     * @param drHoldbackCategoryKey
     * @throws SQLException
     */
    public int deleteDrHoldbackRangesByDrHoldbackCategoryKey(String drHoldbackCategoryKey) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(deleteDrHoldbackRangesByDrHoldbackCategoryKeyQuery)) {
            stmt.setString(1, drHoldbackCategoryKey);
            int result = stmt.executeUpdate();
            log.info("Deleted: {} documents by drHoldbackCategoryKey: {}", result, drHoldbackCategoryKey);
            return result;
        } catch (SQLException e) {
            log.error("SQL Exception in deleteDrHoldbackRangesByDrHoldbackCategoryKey", e);
            throw e;
        }
    }

    /**
     * get all form and content ranges for a key
     *
     * @param key
     * @return
     * @throws SQLException
     */
    public List<DrHoldbackRangeOutputDto> getDrHoldbackRangesByDrHoldbackCategoryKey(String key) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(getDrHoldbackRangesByDrHoldbackCategoryKeyQuery)) {
            stmt.setString(1, key);
            ResultSet result = stmt.executeQuery();
            List<DrHoldbackRangeOutputDto> output = new ArrayList<>();
            while (result.next()) {
                DrHoldbackRangeOutputDto rangeDto = drHoldbackRangeOutputDtoMapper.map(result);
                output.add(rangeDto);
            }
            return output;
        } catch (SQLException e) {
            log.error("SQL Exception in getDrHoldbackRangesByDrHoldbackCategoryKey", e);
            throw e;
        }
    }

    /**
     * Get the drHoldbackCategoryKey from content and form metadata values.
     *
     * @param content the content id
     * @param form    the form id
     * @return
     * @throws SQLException
     */
    public String getDrHoldbackCategoryKeyByContentAndForm(int content, int form) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(getDrHoldbackCategoryKeyByContentAndFormQuery)) {
            stmt.setInt(1, content);
            stmt.setInt(2, content);
            stmt.setInt(3, form);
            stmt.setInt(4, form);
            ResultSet res = stmt.executeQuery();
            if (res.next()) {
                return res.getString(DR_HOLDBACK_RANGES_DR_HOLDBACK_CATEGORY_KEY);
            }
            return null;
        } catch (SQLException e) {
            log.error("SQL Exception in getDrHoldbackCategoryKeyByContentAndForm", e);
            throw e;
        }
    }
}
