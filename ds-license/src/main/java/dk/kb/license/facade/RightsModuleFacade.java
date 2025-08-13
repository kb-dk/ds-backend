package dk.kb.license.facade;

import dk.kb.license.RightsCalculation;
import dk.kb.license.Util;
import dk.kb.license.config.ServiceConfig;
import dk.kb.license.model.v1.*;
import dk.kb.license.solr.SolrServerClient;
import dk.kb.license.storage.AuditLogEntry;
import dk.kb.license.storage.BaseModuleStorage;
import dk.kb.license.storage.RightsModuleStorage;
import dk.kb.license.util.ChangeDifferenceText;
import dk.kb.license.util.RightsChangelogGenerator;
import dk.kb.storage.model.v1.RecordsCountDto;
import dk.kb.storage.util.DsStorageClient;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import dk.kb.util.webservice.exception.NotFoundServiceException;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RightsModuleFacade {
    private static final Logger log = LoggerFactory.getLogger(RightsModuleFacade.class);

    private static final String regexpDsIdPattern="([a-z0-9.]+):([a-zA-Z0-9:._-]+)";
    private static final Pattern dsIdPattern = Pattern.compile(regexpDsIdPattern);

    private static final DsStorageClient storageClient = new DsStorageClient(ServiceConfig.getConfig().getString("storageClient.url"));
    private static final int MAX_COMMENT_LENGTH = 16348;

    /**
     * Retrieves a restrictedID output object from the database
     *
     * @param id the id value
     * @param idType type of ID
     * @param platform The platform
     * @return
     * @throws SQLException
     */
    public static RestrictedIdOutputDto getRestrictedId(String id, IdTypeEnumDto idType, PlatformEnumDto platform) throws SQLException {
        return BaseModuleStorage.performStorageAction("Get restricted ID", RightsModuleStorage.class, storage -> ((RightsModuleStorage)storage).getRestrictedId(id, idType.getValue(), platform.getValue()));
    }

    private static ObjectTypeEnumDto getObjectTypeEnumFromRestrictedIdType(IdTypeEnumDto restrictedIdType) {
        ObjectTypeEnumDto result = null;
        try {
            result = ObjectTypeEnumDto.valueOf(restrictedIdType.getValue());
        } catch (IllegalArgumentException e) {
            String message = "Can not map to ObjectTypeEnumDto from IdTypeEnumDto:" + restrictedIdType;
            log.info(message);
            throw new InvalidArgumentServiceException(message);
        }
        return result;
    }

    /**
     * Creates a restricted ID using the provided input data transfer object (DTO) and user ID.
     *
     * @param restrictedIdInputDto the data transfer object containing the details of the restricted ID to be created.
     *                               This should not be null.
     * @throws SQLException if there is an error while persisting the restricted ID in the database.
     */
    public static void createRestrictedId(RestrictedIdInputDto restrictedIdInputDto, String user, boolean touchDsStorageRecord) throws SQLException {
        validateCommentLength(restrictedIdInputDto.getComment());

        if (restrictedIdInputDto.getIdType() == IdTypeEnumDto.DR_PRODUCTION_ID){
            String validProductionId = Util.validateDrProductionIdFormat(restrictedIdInputDto.getIdValue());
            restrictedIdInputDto.setIdValue(validProductionId);
        }

        if (restrictedIdInputDto.getIdType() == IdTypeEnumDto.DS_ID) {
            validatedsIdFormat(restrictedIdInputDto);
        }

        BaseModuleStorage.performStorageAction("Persist restricted ID (klausulering)", RightsModuleStorage.class, storage -> {
            long id = ((RightsModuleStorage) storage).createRestrictedId(
                    restrictedIdInputDto.getIdValue(),
                    restrictedIdInputDto.getIdType().getValue(),
                    restrictedIdInputDto.getPlatform().getValue(),
                    restrictedIdInputDto.getComment());
            if (touchDsStorageRecord) {
                touchRelatedStorageRecords(restrictedIdInputDto.getIdValue(), restrictedIdInputDto.getIdType());
            }
            ChangeDifferenceText change = RightsChangelogGenerator.createRestrictedIdChanges(restrictedIdInputDto);
            AuditLogEntry logEntry = new AuditLogEntry(id, user, ChangeTypeEnumDto.CREATE, getObjectTypeEnumFromRestrictedIdType(restrictedIdInputDto.getIdType()), restrictedIdInputDto.getIdValue(), "", change.getAfter());
            storage.persistAuditLog(logEntry);
            log.info("Created restriction {}", restrictedIdInputDto);
            return id;
        });
    }

    /**
     * Delete a restricted ID from the Rights database and optionally updates related records in DsStorage.
     * <p/>
     * This method performs the deletion of a restricted ID by its internal ID in the database
     * and logs the deletion in the audit log. If specified, it also touches related storage records
     *
     * @param id in the database of the restricted ID to be deleted.
     * @param user the user performing the deletion action, used for audit logging.
     * @param touchDsStorageRecord a boolean indicating whether to update related storage records.
     */
    public static int deleteRestrictedId(Long id, String user, boolean touchDsStorageRecord) throws Exception {
        return BaseModuleStorage.performStorageAction("delete restricted ID", RightsModuleStorage.class, storage -> {
            // Retrieve object from database
            RestrictedIdOutputDto idToDelete = ((RightsModuleStorage) storage).getRestrictedIdById(id);

            // Delete entry from database
            int deletedCount = ((RightsModuleStorage) storage).deleteRestrictedIdById(id);
            if (touchDsStorageRecord) {
                touchRelatedStorageRecords(idToDelete.getIdValue(), idToDelete.getIdType());
            }

            ChangeDifferenceText change = RightsChangelogGenerator.deleteRestrictedIdChanges(idToDelete.getIdValue(), idToDelete.getIdType().getValue(), idToDelete.getPlatform().toString());
            AuditLogEntry logEntry = new AuditLogEntry(id, user, ChangeTypeEnumDto.DELETE, getObjectTypeEnumFromRestrictedIdType(idToDelete.getIdType()), idToDelete.getIdValue(), change.getBefore(), change.getAfter());
            storage.persistAuditLog(logEntry);
            log.info("Deleted restriction for internal ID: '{}' with idValue: '{}' with idType: '{}' on platform: '{}'.",
                    id, idToDelete.getIdValue(), idToDelete.getIdType(), idToDelete.getPlatform());
            return deletedCount;
        });
    }

    /**
     * Update a restricted ID using the provided input data transfer object (DTO) and user ID.
     *
     * @param updateRestrictedIdCommentInputDto the data transfer object containing the details of the restricted ID to be Updated.
     *                             This should not be null.
     * @param touchDsStorageRecord
     * @throws SQLException if there is an error while persisting the restricted ID in the database.
     */
    public static void updateRestrictedIdComment(UpdateRestrictedIdCommentInputDto updateRestrictedIdCommentInputDto, String user, boolean touchDsStorageRecord) throws SQLException {
        validateCommentLength(updateRestrictedIdCommentInputDto.getComment());

        BaseModuleStorage.performStorageAction("Update restricted ID (klausulering)", RightsModuleStorage.class, storage -> {
            long id = updateRestrictedIdCommentInputDto.getId();
            RestrictedIdOutputDto oldVersion = ((RightsModuleStorage)storage).getRestrictedIdById(id);
            if (oldVersion == null) {
                throw new NotFoundServiceException("updated restricted Id not found " + updateRestrictedIdCommentInputDto.toString());
            }
            ((RightsModuleStorage) storage).updateRestrictedIdComment(
                    updateRestrictedIdCommentInputDto.getId(),
                    updateRestrictedIdCommentInputDto.getComment());
            if (touchDsStorageRecord) {
                touchRelatedStorageRecords(oldVersion.getIdValue(), oldVersion.getIdType());
            }
            RestrictedIdOutputDto newVersion = ((RightsModuleStorage)storage).getRestrictedIdById(id);

            ChangeDifferenceText change = RightsChangelogGenerator.updateRestrictedIdChanges(oldVersion, newVersion);
            AuditLogEntry logEntry = new AuditLogEntry(id, user, ChangeTypeEnumDto.UPDATE, getObjectTypeEnumFromRestrictedIdType(newVersion.getIdType()), newVersion.getIdValue(), change.getBefore(), change.getAfter());
            storage.persistAuditLog(logEntry);
            log.info("Updated restricted ID {}", updateRestrictedIdCommentInputDto);
            return null;
        });
    }

    /**
     * Creates a restricted ID for each of the entries in the list using the provided input data transfer object (DTO) and user ID.
     *
     * @param restrictedIds list containing the data transfer objects containing the details of the restricted IDs to be created.
     *                               This should not be null.
     * @throws SQLException if there is an error while persisting the restricted ID in the database.
     */
    public static void createRestrictedIds(List<RestrictedIdInputDto> restrictedIds, String user, boolean touchDsStorageRecord) throws SQLException {
        BaseModuleStorage.performStorageAction("create restricted ID", RightsModuleStorage.class, storage -> {
            for (RestrictedIdInputDto id : restrictedIds) {
                log.debug("Adding restricted id type='{}' with value='{}'", id.getIdType(), id.getIdValue());
                validateCommentLength(id.getComment());

                if (id.getIdType() == IdTypeEnumDto.DR_PRODUCTION_ID){
                    String validProductionId = Util.validateDrProductionIdFormat(id.getIdValue());
                    id.setIdValue(validProductionId);
                }

                long objectId = ((RightsModuleStorage) storage).createRestrictedId(
                        id.getIdValue(),
                        id.getIdType().getValue(),
                        id.getPlatform().getValue(),
                        id.getComment()
                );
                if (touchDsStorageRecord) {
                    touchRelatedStorageRecords(id.getIdValue(), id.getIdType());
                }
                ChangeDifferenceText change = RightsChangelogGenerator.createRestrictedIdChanges(id);
                AuditLogEntry logEntry = new AuditLogEntry(objectId, user, ChangeTypeEnumDto.CREATE, getObjectTypeEnumFromRestrictedIdType(id.getIdType()), id.getIdValue(), change.getBefore(), change.getAfter());
                storage.persistAuditLog(logEntry);
            }
            return null;
        });
        log.info("Added restricted IDs: [{}] ",
                restrictedIds.stream().map(RestrictedIdInputDto::toString).collect(Collectors.joining(", ")));

    }

    /**
     * Get all restricted Ids
     *
     * @param idType only get restricedIds with this idType
     * @param platform only get retstrictedIds for this platform
     * @return
     */
    public static List<RestrictedIdOutputDto> getAllRestrictedIds(IdTypeEnumDto idType, PlatformEnumDto platform) {
        return BaseModuleStorage.performStorageAction("get restricted IDs", RightsModuleStorage.class, storage ->
                ((RightsModuleStorage) storage).getAllRestrictedIds(
                        idType.getValue(),
                        platform.getValue()
        ));
    }

    /**
     * Retrieves the drHoldbackValue based on the specified content and form values.
     * If no drHoldbackValue is found for the given content and form, a {@link NotFoundServiceException} is thrown.
     *
     * @param content the content identifier, must be a valid integer.
     * @param form the form identifier, must be a valid integer.
     * @return the drHoldbackValue as a {@link String} if found.
     * @throws NotFoundServiceException if no drHoldbackValue is found for the
     *                                   specified content and form.
     */
    public static String getDrHoldbackValueFromContentAndFormValues(Integer content, Integer form)  {
        return BaseModuleStorage.performStorageAction("Get drHoldbackValue", RightsModuleStorage.class, storage -> {
            String id = ((RightsModuleStorage) storage).getDrHoldbackValueFromContentAndForm(content, form);
            if (id == null) {
                log.warn("No DR holdback found for content: '{}' and form: '{}'. Returning an empty string", content, form);
                return "";
            }
            return id;
        });
    }

    /**
     * Retrieves the DR holdback rule identified by the specified drHoldbackValue.
     * This method performs a storage action to access the RightsModuleStorage
     * and fetch the DR holdback rule as an object.
     *
     * @param drHoldbackValue the drHoldbackValue of the DR holdback rule to retrieve. It must not be null or empty.
     * @return the {@link DrHoldbackRuleOutputDto} corresponding to the specified drHoldbackValue if found.
     * @throws NotFoundServiceException if no rule is found for the specified drHoldbackValue.
     */
    public static DrHoldbackRuleOutputDto getDrHoldbackRuleById(String drHoldbackValue) {
        return BaseModuleStorage.performStorageAction("Get DR holdback rule", RightsModuleStorage.class, storage -> {
            DrHoldbackRuleOutputDto output = ((RightsModuleStorage) storage).getDrHoldbackRuleFromValue(drHoldbackValue);
            if (output != null) {
                return output;
            }
            throw new NotFoundServiceException("DR holdback rule not found for drHoldbackValue: " + drHoldbackValue);
        });
    }

    /**
     * Calculate the rights for a specific record based on the input values provided.
     *
     * @param rightsCalculationInputDto the input DTO containing the needed information for rights calculation.
     * @return a {@link RightsCalculationOutputDto} containing the calculated rights.
     */
    public static RightsCalculationOutputDto calculateRightsForRecord(RightsCalculationInputDto rightsCalculationInputDto) throws SQLException {
        Util.validateNoNullFields(rightsCalculationInputDto);

        RightsCalculationOutputDto output = new RightsCalculationOutputDto();

        switch (rightsCalculationInputDto.getPlatform()){
            case DRARKIV:
                RightsCalculationOutputDrDto drOutput = RightsCalculation.calculateDrRights(rightsCalculationInputDto);
                output.setDr(drOutput);
                return output;
            case GENERIC:
                log.error("The generic format haven't been implemented yet and therefore it cannot be used");
                throw new InternalServiceException("The generic format haven't been implemented yet");
            default:
                throw new InternalServiceException("A valid platform enum should have been specified. Allowed values are: '" +
                        Arrays.toString(PlatformEnumDto.values()) + "'");

        }
    }


    /**
     * Checks if the specified ID is restricted based on the provided ID type and platform.
     * The method performs a storage action to retrieve the restriction status
     * for the ID and returns true if the ID is restricted, and false otherwise.
     *
     * @param id the ID to check for restrictions
     * @param idType the type of the ID. Types are: ds_id, dr_produktions_id, strict_title
     * @param platform the platform where the ID is being checked (e.g. DR Archive)
     * @return true if the ID is restricted; false otherwise
     * @throws SQLException if an error occurs during the SQL process.
     */
    public static boolean isIdRestricted(String id, IdTypeEnumDto idType, PlatformEnumDto platform) throws SQLException {
        return BaseModuleStorage.performStorageAction("Get restricted id", RightsModuleStorage.class, storage -> {
            RestrictedIdOutputDto idOutput = ((RightsModuleStorage) storage).getRestrictedId(id, idType.getValue(), platform.getValue());
            // If the object is null, then id is not restricted
            return idOutput != null;
        });
    }

    /**
     * Checks if the specified production code from metadata is allowed based on the provided platform.
     *
     * This method interacts with the storage system to determine whether the given production
     * code is considered allowed. It performs a storage action to retrieve the restriction status
     * for the production code and returns true if the code is allowed, and false otherwise.
     *
     * @param productionCode the production code to check for allowance
     * @param platform the platform where the production code is being checked (e.g. DR Archive)
     * @return true if the production code is allowed; false otherwise
     */
    public static boolean isProductionCodeAllowed(String productionCode, String platform) {
        return BaseModuleStorage.performStorageAction("Get restricted id", RightsModuleStorage.class, storage -> {
            RestrictedIdOutputDto idOutput = ((RightsModuleStorage) storage).getRestrictedId(productionCode, IdTypeEnumDto.OWNPRODUCTION_CODE.getValue(), platform);
            // If the object is null, then productionCode from metadata is not allowed

            if (idOutput == null){
                log.debug("The specified production code '{}' is not known in the database. Therefore it cannot be allowed.", productionCode);
                return false;
            } else {
                log.debug("Production Code is allowed");
                return true;
            }
        });
    }

    /**
     * create a DR holdback rule.
     *
     * @param drHoldbackRuleInputDto
     * @param user the user performing the action
     */
    public static long createDrHoldbackRule(DrHoldbackRuleInputDto drHoldbackRuleInputDto, String user) {
        return BaseModuleStorage.performStorageAction("Create DR holdback rule", RightsModuleStorage.class, storage -> {
            long id = ((RightsModuleStorage)storage).createDrHoldbackRule(
                    drHoldbackRuleInputDto.getDrHoldbackValue(),
                    drHoldbackRuleInputDto.getName(),
                    drHoldbackRuleInputDto.getDays()
            );
            ChangeDifferenceText changes = RightsChangelogGenerator.createDrHoldbackRuleInputDtoChanges(drHoldbackRuleInputDto);
            AuditLogEntry logEntry = new AuditLogEntry(id, user, ChangeTypeEnumDto.CREATE, ObjectTypeEnumDto.HOLDBACK_RULE, drHoldbackRuleInputDto.getDrHoldbackValue(), "", changes.getAfter());
            storage.persistAuditLog(logEntry);
            return id;
        });
    }

    /**
     * Delete a DR holdback rule
     * @param drHoldbackValue drHoldbackValue of the DR holdback rule
     * @param user the user performing the action
     */
    public static void deleteDrHoldbackRule(String drHoldbackValue, String user) {
        BaseModuleStorage.performStorageAction("Delete DR holdback rule", RightsModuleStorage.class, storage -> {
            DrHoldbackRuleOutputDto drHoldbackRule = ((RightsModuleStorage)storage).getDrHoldbackRuleFromValue(drHoldbackValue);
            ChangeDifferenceText changes = RightsChangelogGenerator.deleteDrHoldbackRuleOutputDtoChanges(drHoldbackRule);
            ((RightsModuleStorage)storage).deleteDrHoldbackRule(drHoldbackValue);
            AuditLogEntry logEntry = new AuditLogEntry(drHoldbackRule.getId(), user, ChangeTypeEnumDto.DELETE, ObjectTypeEnumDto.HOLDBACK_RULE, drHoldbackValue, changes.getBefore(), "");
            storage.persistAuditLog(logEntry);
            return null;
        });
    }

    /**
     * get all holdback rules for DR
     * @return
     */
    public static List<DrHoldbackRuleOutputDto> getAllDrHoldbackRules() {
        return BaseModuleStorage.performStorageAction("Get DR holdback rule", RightsModuleStorage.class, storage -> ((RightsModuleStorage)storage).getAllDrHoldbackRules());
    }

    /**
     * Retrieve the number of days for a holdback rule from a drHoldbackValue
     * @param drHoldbackValue the drHoldbackValue of the dr holdback rule
     * @return the number of
     **/
    public static Integer getDrHoldbackDaysFromValue(String drHoldbackValue) {
        return BaseModuleStorage.performStorageAction("Get DR holdback days", RightsModuleStorage.class, storage -> ((RightsModuleStorage) storage).getDrHoldbackDaysFromValue(drHoldbackValue));
    }

    /**
     * Retrieve the number of days for a holdback rule from a drHoldbackValue
     * @param name the name of the dr holdback rule
     * @return the number of
     **/
    public static Integer getDrHoldbackDaysFromName(String name) {
        return BaseModuleStorage.performStorageAction("Get DR holdback days", RightsModuleStorage.class, storage -> ((RightsModuleStorage) storage).getDrHoldbackDaysFromName(name));
    }

    /**
     * Update the number of days for a holdback rule
     * @param drHoldbackValue the drHoldbackValue of the dr holdback rule
     * @param user the user performing the action
     **/
    public static void updateDrHoldbackDaysFromDrHoldbackValue(String drHoldbackValue, Integer days, String user) {
        BaseModuleStorage.performStorageAction("update DR holdback days", RightsModuleStorage.class, storage -> {
            Integer daysBefore = ((RightsModuleStorage)storage).getDrHoldbackDaysFromValue(drHoldbackValue);
            long id = ((RightsModuleStorage)storage).getDrHoldbackRuleIdFromValue(drHoldbackValue);
            ((RightsModuleStorage) storage).updateDrHoldbackDaysFromDrHoldbackValue(drHoldbackValue, days);
            AuditLogEntry logEntry = new AuditLogEntry(id, user, ChangeTypeEnumDto.UPDATE, ObjectTypeEnumDto.HOLDBACK_DAY, drHoldbackValue, "Days before: " + daysBefore, "Days after: " + days);
            storage.persistAuditLog(logEntry);
            return null;
        });
    }

    /**
     * Update the number of days for a DR holdback rule
     * @param name the name of the DR holdback rule
     * @param user the user performing the action
     * @return the number of
     **/
    public static void updateDrHoldbackDaysFromName(String name, Integer days, String user) {
        BaseModuleStorage.performStorageAction("update DR holdback days", RightsModuleStorage.class, storage -> {
            Integer daysBefore = ((RightsModuleStorage)storage).getDrHoldbackDaysFromName(name);
            long id = ((RightsModuleStorage)storage).getDrHoldbackRuleIdFromName(name);
            ((RightsModuleStorage) storage).updateDrHoldbackDaysFromName(name, days);
            AuditLogEntry logEntry = new AuditLogEntry(id, user, ChangeTypeEnumDto.UPDATE, ObjectTypeEnumDto.HOLDBACK_DAY, name, "Days before: " + daysBefore, "Days after: " + days);
            storage.persistAuditLog(logEntry);
            return null;
        });
    }

    /**
     * set the form and content range combinations for a drHoldbackValue
     * This requires the drHoldbackValue to be present in the DR holdback rule table
     *
     * @param drHoldbackRangeInputDto
     * @param user                    : the user performing the action
     */
    public static List<Long> createDrHoldbackRanges(DrHoldbackRangeInputDto drHoldbackRangeInputDto, String user) {

        return BaseModuleStorage.performStorageAction("Create DR holdback ranges for " + drHoldbackRangeInputDto.getDrHoldbackValue(), RightsModuleStorage.class, storage -> {
            List<Long> idList = new ArrayList<>();
            for(DrHoldbackRangesDto rangeDto: drHoldbackRangeInputDto.getRanges()) {
                if (((RightsModuleStorage)storage).getDrHoldbackRuleFromValue(drHoldbackRangeInputDto.getDrHoldbackValue()) == null) {
                    throw new InvalidArgumentServiceException("No drHoldbackValue: " + drHoldbackRangeInputDto.getDrHoldbackValue());
                }
                long objectId = ((RightsModuleStorage)storage).createDrHoldbackRange(
                        rangeDto.getContentRangeFrom(),
                        rangeDto.getContentRangeTo(),
                        rangeDto.getFormRangeFrom(),
                        rangeDto.getFormRangeTo(),
                        drHoldbackRangeInputDto.getDrHoldbackValue()
                );
                ChangeDifferenceText changes = RightsChangelogGenerator.createDrHoldbackRangesChanges(drHoldbackRangeInputDto.getRanges());
                AuditLogEntry logEntry = new AuditLogEntry(objectId, user, ChangeTypeEnumDto.CREATE, ObjectTypeEnumDto.HOLDBACK_RANGE, drHoldbackRangeInputDto.getDrHoldbackValue(), "", changes.getAfter());
                storage.persistAuditLog(logEntry);
                idList.add(objectId);
            }
            return idList;
        });
    }

    /**
     * Deletes all form and content range combinations for a drHoldbackValue
     * @param drHoldbackValue
     * @param user the user performing the action
     */
    public static void deleteRangesForDrHoldbackValue(String drHoldbackValue, String user) {
        BaseModuleStorage.performStorageAction("Delete DR holdback ranges for " + drHoldbackValue, RightsModuleStorage.class, storage -> {
            List<DrHoldbackRangeOutputDto> oldRanges = ((RightsModuleStorage) storage).getDrHoldbackRangesForDrHoldbackValue(drHoldbackValue);
            ((RightsModuleStorage)storage).deleteRangesForDrHoldbackValue(drHoldbackValue);
            ChangeDifferenceText changes = RightsChangelogGenerator.deleteDrHoldbackRangesChanges(oldRanges); //This is weird but will be refactored completely in DRA-2085
            for (DrHoldbackRangeOutputDto rangeDto: oldRanges) {
                AuditLogEntry logEntry = new AuditLogEntry(rangeDto.getId(), user, ChangeTypeEnumDto.DELETE, ObjectTypeEnumDto.HOLDBACK_RANGE, drHoldbackValue, changes.getAfter(), "");
                storage.persistAuditLog(logEntry);
            }
            return null;
        });
    }

    /**
     * Deletes all form and content range combinations for drHoldbackValue
     *
     * @param drHoldbackValue
     * @return
     */
    public static List<DrHoldbackRangeOutputDto> getDrHoldbackRanges(String drHoldbackValue) {
        return BaseModuleStorage.performStorageAction("Get DR holdback ranges for " + drHoldbackValue, RightsModuleStorage.class, storage-> ((RightsModuleStorage)storage).getDrHoldbackRangesForDrHoldbackValue(drHoldbackValue));
    }

    private static void validateCommentLength(String comment) {
        if (comment != null && comment.length() > MAX_COMMENT_LENGTH) {
            log.error("Comment was too long and cannot be added to rights module. Only {} characters are allowed.", MAX_COMMENT_LENGTH);
            throw new InvalidArgumentServiceException("Comment was too long and cannot be added to rights module. Only " + MAX_COMMENT_LENGTH + " characters are allowed.");
        }
    }

    private static void validatedsIdFormat(RestrictedIdInputDto restrictedIdInputDto) {
        Matcher m = dsIdPattern.matcher(restrictedIdInputDto.getIdValue());
        if (!m.matches()) {
            throw new InvalidArgumentServiceException("Invalid ds_id format " + restrictedIdInputDto.getIdValue());
        }
    }

    /**
     * Based on idType, touch related storage records, so that they can be re-indexed with the new information.
     * @param id which have been updated in the rights table
     * @param idType to determine how related records are updated.
     * @return amount of records touched in DS-Storage.
     */
    public static int touchRelatedStorageRecords(String id, IdTypeEnumDto idType) {
        switch (idType){
            case DS_ID:
                return touchStorageRecordById(id);
            // TODO: Implement the rest of these touches by solr queries-
            case DR_PRODUCTION_ID:
                return touchStorageRecordsByProductionId(id);
            case OWNPRODUCTION_CODE:
                return touchStorageRecordsByProductionCode(id);
            case STRICT_TITLE:
                return touchStorageRecordsByStrictTitle(id);
            default:
                throw new IllegalArgumentException("Invalid idType " + idType);
        }
    }

    /**
     * Touch a single ID directly in DS-Storage.
     * @param id to touch in DS-storage.
     * @return amount of records touched in DS-Storage.
     */
    private static int touchStorageRecordById(String id) {
        try {
            RecordsCountDto count = storageClient.touchRecord(id);
            if (count == null || count.getCount() == null) {
                return 0;
            }
            return count.getCount();
        } catch (NotFoundServiceException e) {
            log.info("Touching storage record not found " +id);
            return 0;
        }
    }

    /**
     * Query solr for all records where the restricted title is present and touch the records in DS-storage.
     * @param strictTitle to query solr for.
     * @return amount of records touched in DS-Storage.
     */
    private static int touchStorageRecordsByStrictTitle(String strictTitle) {
        String solrField = "title_strict";
        return touchStorageRecordsByIdFromSolrQuery(solrField, strictTitle);
    }

    /**
     * Query solr for all records where the productionCode is present and touch the records in DS-storage.
     * @param productionCode to query solr for.
     * @return amount of records touched in DS-Storage.
     */
    private static int touchStorageRecordsByProductionCode(String productionCode) {
        String solrField = "production_code_value";
        return touchStorageRecordsByIdFromSolrQuery(solrField, productionCode);
    }

    /**
     * Query solr for all records where the drProductionId is present and touch the records in DS-storage.
     * @param drProductionId to query solr for.
     * @return amount of records touched in DS-Storage.
     */
    private static int touchStorageRecordsByProductionId(String drProductionId) {
        String solrField = "dr_production_id";
        return touchStorageRecordsByIdFromSolrQuery(solrField, drProductionId);
    }


    /**
     * Perform a solr query as {@code solrField:"fieldValue"} and for each record in the solr response get the id for
     * each record and touch the related DS-Record in DS-Storage.
     * @param solrField to query for the fieldValue.
     * @param fieldValue to query for.
     * @return the amount of records touched in DS-Storage.
     */
    private static int touchStorageRecordsByIdFromSolrQuery(String solrField, String fieldValue) {
        List<SolrServerClient> servers = ServiceConfig.SOLR_SERVERS;
        int touchedRecordsCount = 0;

        // Ds-license supports multiple backing solr servers. So we have to wrap it in this for-loop
        for (SolrServerClient server : servers) {
            int pageSize = 500;
            int start = 0;

            try {
                SolrQuery query = getIdSolrQuery(solrField, fieldValue, pageSize);

                while (true){
                    // Update start value before the query is fired against the server
                    query.setStart(start);

                    // Query solr for a response
                    QueryResponse response = server.query(query);
                    SolrDocumentList results = response.getResults();

                    // For each record in the result touch the related DS-storage record
                    for (SolrDocument doc : results) {
                        RecordsCountDto touched = touchStorageRecords(doc);
                        if (touched != null && touched.getCount() != null){
                            touchedRecordsCount = touchedRecordsCount + touched.getCount();
                        }
                    }

                    long totalResults = results.getNumFound();

                    // Break the loop of no more records are available
                    if (start + pageSize >= totalResults) {
                        break;
                    }

                    // Increment start by pageSize
                    start += pageSize;
                }
            } catch (SolrServerException | IOException e) {
                throw new InternalServiceException(e);
            }
        }

        return touchedRecordsCount;
    }

    /**
     * Create a solr query on the form {@code solrField:"fieldValue"} where the rows param is set to {@code pageSize} The query only returns the field ID from the documents.
     * @param solrField to query.
     * @param fieldValue used to query the field defined above.
     * @param pageSize which determines the amount of documents returned by the query.
     * @return a {@link SolrQuery} that can be fired as is or further developed for paging etc.
     */
    private static SolrQuery getIdSolrQuery(String solrField, String fieldValue, int pageSize) {
        SolrQuery query = new SolrQuery();
        query.setQuery(solrField + ":\"" + fieldValue + "\"");
        query.setRows(pageSize);
        query.setFields("id");
        return query;
    }

    /**
     * Touch a related DS-Record in the backing DS-Storage extracting the ID from a given solr document and use that ID when querying the DS-StorageClient
     * @param doc solr document to extract ID from.
     */
    private static RecordsCountDto touchStorageRecords(SolrDocument doc) {
        // For each document in the result, touch its ds-storage record.
        String id = (String) doc.getFieldValue("id");
        log.debug("Touching DS-storage record with id: '{}'", id);
        return storageClient.touchRecord(id);
    }
}
