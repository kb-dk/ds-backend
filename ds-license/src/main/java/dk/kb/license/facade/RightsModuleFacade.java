package dk.kb.license.facade;

import dk.kb.license.RightsCalculation;
import dk.kb.license.config.ServiceConfig;
import dk.kb.license.mapper.BroadcastDtoMapper;
import dk.kb.license.mapper.DrBroadcastDtoMapper;
import dk.kb.license.mapper.FailedRestrictedIdDtoMapper;
import dk.kb.license.model.v1.*;
import dk.kb.license.solr.SolrServerClient;
import dk.kb.license.storage.AuditLogEntry;
import dk.kb.license.storage.AuditLogModuleStorage;
import dk.kb.license.storage.BaseModuleStorage;
import dk.kb.license.storage.RightsModuleStorage;
import dk.kb.license.util.ChangeDifferenceText;
import dk.kb.license.util.RightsChangelogGenerator;
import dk.kb.license.validation.InputValidator;
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

public class RightsModuleFacade {
    private static final Logger log = LoggerFactory.getLogger(RightsModuleFacade.class);
    private static final SolrServerClient solrServerClient = new SolrServerClient();
    private static final DsStorageClient storageClient = new DsStorageClient(ServiceConfig.getConfig().getString("storageClient.url"));
    private static final InputValidator inputValidator = new InputValidator();

    /**
     * Enables the posibility to mock the SolrServerClient
     *
     * @return
     */
    public static SolrServerClient getSolrServerClient() {
        return solrServerClient;
    }

    /**
     * Retrieves a restricted id output object from the database
     *
     * @param idValue  the id value
     * @param idType   type of id
     * @param platform The platform
     * @return RestrictedIdOutputDto
     */
    public static RestrictedIdOutputDto getRestrictedId(String idValue, IdTypeEnumDto idType, PlatformEnumDto platform) {
        return BaseModuleStorage.performStorageAction("Get restricted id", RightsModuleStorage.class, storage -> {
            RestrictedIdOutputDto restrictedIdOutputDto = ((RightsModuleStorage) storage).getRestrictedId(idValue, idType.getValue(), platform.getValue());

            if (restrictedIdOutputDto == null) {
                final String errorMessage = "restricted id idValue: " + idValue + ", idType: " + idType + ", platform: " + platform + " not found";
                log.error(errorMessage);
                throw new NotFoundServiceException(errorMessage);
            }

            return restrictedIdOutputDto;
        });
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
     * Creates a restricted id using the provided {@link RestrictedIdOutputDto}.
     *
     * @param restrictedIdInputDto the data transfer object containing the details of the restricted id to be created.
     *                             This should not be null.
     * @return RestrictedIdOutputDto
     */
    public static RestrictedIdOutputDto createRestrictedId(boolean touchDsStorageRecord, RestrictedIdInputDto restrictedIdInputDto) {
        inputValidator.validateRestrictedIdInputDto(restrictedIdInputDto);

        return BaseModuleStorage.performStorageAction("Persist restricted id (klausulering)", RightsModuleStorage.class, storage -> {
            long id = ((RightsModuleStorage) storage).createRestrictedId(restrictedIdInputDto.getIdValue(), restrictedIdInputDto.getIdType().getValue(), restrictedIdInputDto.getPlatform().getValue(), restrictedIdInputDto.getTitle(), restrictedIdInputDto.getComment());

            if (touchDsStorageRecord) {
                touchRelatedStorageRecords(restrictedIdInputDto.getIdValue(), restrictedIdInputDto.getIdType());
            }

            RestrictedIdOutputDto createdRestrictedIdOutputDto = ((RightsModuleStorage) storage).getRestrictedIdById(id);

            ChangeDifferenceText change = RightsChangelogGenerator.createRestrictedIdChanges(createdRestrictedIdOutputDto);
            AuditLogEntry logEntry = new AuditLogEntry(id, null, ChangeTypeEnumDto.CREATE, getObjectTypeEnumFromRestrictedIdType(restrictedIdInputDto.getIdType()), restrictedIdInputDto.getIdValue(), null, change.getBefore(), change.getAfter());
            ((AuditLogModuleStorage) storage).persistAuditLog(logEntry);

            log.info("Created restriction id: {}", createdRestrictedIdOutputDto);

            return createdRestrictedIdOutputDto;
        });
    }

    /**
     * This method performs the deletion of a restricted id by its internal id in the restricted_ids table
     * and logs the deletion in the audit log. If specified, it also touches related storage records
     *
     * @param id                   id on the restricted id to be deleted.
     * @param touchDsStorageRecord a boolean indicating whether to update related storage records.
     * @param deleteReasonDto      comment about why object gets deleted
     */
    public static RecordsCountDto deleteRestrictedId(Long id, boolean touchDsStorageRecord, DeleteReasonDto deleteReasonDto) {
        inputValidator.validateId(id);
        inputValidator.validateChangeComment(deleteReasonDto.getChangeComment());

        return BaseModuleStorage.performStorageAction("Delete restricted id", RightsModuleStorage.class, storage -> {
            // Retrieve object from database
            RestrictedIdOutputDto deleteRestrictedIdOutputDto = ((RightsModuleStorage) storage).getRestrictedIdById(id);

            if (deleteRestrictedIdOutputDto == null) {
                final String errorMessage = "restricted id: " + id + " not found";
                log.error(errorMessage);
                throw new NotFoundServiceException(errorMessage);
            }

            // Delete entry from database
            RecordsCountDto recordsCountDto = new RecordsCountDto();
            int deletedCount = ((RightsModuleStorage) storage).deleteRestrictedIdById(id);
            recordsCountDto.setCount(deletedCount);

            if (touchDsStorageRecord) {
                touchRelatedStorageRecords(deleteRestrictedIdOutputDto.getIdValue(), deleteRestrictedIdOutputDto.getIdType());
            }

            ChangeDifferenceText change = RightsChangelogGenerator.deleteRestrictedIdChanges(deleteRestrictedIdOutputDto);
            AuditLogEntry logEntry = new AuditLogEntry(id, null, ChangeTypeEnumDto.DELETE, getObjectTypeEnumFromRestrictedIdType(deleteRestrictedIdOutputDto.getIdType()), deleteRestrictedIdOutputDto.getIdValue(), deleteReasonDto.getChangeComment(), change.getBefore(), change.getAfter());
            ((AuditLogModuleStorage) storage).persistAuditLog(logEntry);

            log.info("Deleted restriction id: {} ", deleteRestrictedIdOutputDto);

            return recordsCountDto;
        });
    }

    /**
     * Update a restricted id using the provided {@link UpdateRestrictedIdCommentInputDto}.
     *
     * @param id
     * @param touchDsStorageRecord
     * @param restrictedIdInputDto the data transfer object containing the details of the restricted id to be updated.
     *                             This should not be null.
     */
    public static RestrictedIdOutputDto updateRestrictedId(Long id, Boolean touchDsStorageRecord, RestrictedIdInputDto restrictedIdInputDto) {
        inputValidator.validateId(id);
        inputValidator.validateRestrictedIdInputDto(restrictedIdInputDto);

        RestrictedIdOutputDto oldRestrictedIdOutputDto = getRestrictedId(restrictedIdInputDto.getIdValue(), restrictedIdInputDto.getIdType(), restrictedIdInputDto.getPlatform());

        if (oldRestrictedIdOutputDto == null) {
            final String errorMessage = "id: " + id + " not found";
            log.error(errorMessage);
            throw new NotFoundServiceException(errorMessage);
        }

        return BaseModuleStorage.performStorageAction("Update restricted id (klausulering)", RightsModuleStorage.class, storage -> {
            ((RightsModuleStorage) storage).updateRestrictedId(id, restrictedIdInputDto.getTitle(), restrictedIdInputDto.getComment());

            if (touchDsStorageRecord) {
                touchRelatedStorageRecords(restrictedIdInputDto.getIdValue(), restrictedIdInputDto.getIdType());
            }
            RestrictedIdOutputDto updatedRestrictedIdOutputDto = ((RightsModuleStorage) storage).getRestrictedIdById(id);

            ChangeDifferenceText change = RightsChangelogGenerator.updateRestrictedIdChanges(oldRestrictedIdOutputDto, updatedRestrictedIdOutputDto);
            AuditLogEntry logEntry = new AuditLogEntry(id, null, ChangeTypeEnumDto.UPDATE, getObjectTypeEnumFromRestrictedIdType(updatedRestrictedIdOutputDto.getIdType()), updatedRestrictedIdOutputDto.getIdValue(), null, change.getBefore(), change.getAfter());
            ((AuditLogModuleStorage) storage).persistAuditLog(logEntry);
            log.info("Updated restricted id: {}", updatedRestrictedIdOutputDto);
            return updatedRestrictedIdOutputDto;
        });
    }

    /**
     * Returns a DrBroadcastDto object that shows if the input dsId broadcast has a drProductionId.
     * DrBroadcastDto has a list of BroadcastDto that is populated with broadcasts sharing the same drProductionId.
     * Each BroadcastDto also shows if there is a restriction and restriction comment on that broadcast.
     *
     * @param dsId the unique id of a DR-arkiv broadcast
     * @return DrBroadcastDto with a list of BroadcastDto
     * @throws SolrServerException
     * @throws IOException
     */
    public static DrBroadcastDto matchingDrProductionIdBroadcasts(String dsId) throws SolrServerException, IOException {
        // Check if dsId is valid
        inputValidator.validateDsId(dsId);

        String queryDsId = "id:\"" + dsId + "\"";
        String fieldListDsId = "dr_production_id, id, title, startTime, endTime";

        SolrDocumentList resultsFromDsId = getSolrServerClient().callSolr(queryDsId, fieldListDsId);

        if (resultsFromDsId.getNumFound() == 0) {
            final String errorMessage = "dsId: " + dsId + " not found";
            log.error(errorMessage);
            throw new NotFoundServiceException(errorMessage);
        }

        // There is only one object in the SolrDocumentList, so we fetch that out
        SolrDocument resultFromDsId = resultsFromDsId.get(0);

        DrBroadcastDto drBroadcastDto = new DrBroadcastDto();
        List<BroadcastDto> broadcastDtoList = new ArrayList<>();
        DrBroadcastDtoMapper drBroadcastDtoMapper = new DrBroadcastDtoMapper();
        BroadcastDtoMapper broadcastDtoMapper = new BroadcastDtoMapper();

        if (resultFromDsId.getFieldValue("dr_production_id") == null) {
            drBroadcastDto = drBroadcastDtoMapper.map(drBroadcastDto, null, null);

            // There could be a restriction already on the broadcast
            String dsIdRestrictedIdComment = getRestrictedIdCommentByIdValue(resultFromDsId.getFieldValue("id").toString());
            BroadcastDto broadcastDto = broadcastDtoMapper.map(resultFromDsId, dsIdRestrictedIdComment);
            broadcastDtoList.add(broadcastDto);
        } else {
            // drProductionId can be restricted
            String drProductionIdRestrictedIdComment = getRestrictedIdCommentByIdValue(resultFromDsId.getFieldValue("dr_production_id").toString());
            drBroadcastDto = drBroadcastDtoMapper.map(drBroadcastDto, resultFromDsId.getFieldValue("dr_production_id").toString(), drProductionIdRestrictedIdComment);

            String queryDrProductionId = "dr_production_id:\"" + drBroadcastDto.getDrProductionId() + "\"";
            String fieldListDrProductionId = "id, title, startTime, endTime";

            SolrDocumentList resultsFromDrProductionId = getSolrServerClient().callSolr(queryDrProductionId, fieldListDrProductionId);

            // Should never happen
            if (resultsFromDrProductionId.getNumFound() == 0) {
                final String errorMessage = "No DR broadcasts found with drProductionId: " + drBroadcastDto.getDrProductionId();
                log.error(errorMessage);
                throw new NotFoundServiceException(errorMessage);
            }

            for (SolrDocument solrDocument : resultsFromDrProductionId) {
                // There could be a restriction already on the broadcast
                String dsIdRestrictedIdComment = getRestrictedIdCommentByIdValue(solrDocument.getFieldValue("id").toString());
                BroadcastDto broadcastDto = broadcastDtoMapper.map(solrDocument, dsIdRestrictedIdComment);
                broadcastDtoList.add(broadcastDto);
            }
        }
        drBroadcastDto.setBroadcast(broadcastDtoList);
        return drBroadcastDto;
    }

    /**
     * Fetch comment from restricted_ids
     *
     * @param idValue
     * @return comment about why a broadcast or drProductionId is restricted
     */
    public static String getRestrictedIdCommentByIdValue(String idValue) {
        return BaseModuleStorage.performStorageAction("Select comment from restricted_ids", RightsModuleStorage.class, storage -> (((RightsModuleStorage) storage).getRestrictedIdCommentByIdValue(idValue)));
    }

    /**
     * Creates or updates a restricted id for each of the entries in the list using the provided input data transfer object (DTO).
     *
     * @param restrictedIds list containing the data transfer objects containing the details of the restricted ids to be created.
     *                      This should not be null.
     * @return ProcessedRestrictedIdsOutputDto
     */
    public static ProcessedRestrictedIdsOutputDto createOrUpdateRestrictedIds(boolean touchDsStorageRecord, List<RestrictedIdInputDto> restrictedIds) {
        ProcessedRestrictedIdsOutputDto processedRestrictedIdsOutputDto = new ProcessedRestrictedIdsOutputDto();
        List<FailedRestrictedIdDto> failedRestrictedIdDtoList = new ArrayList<>();
        int processedSuccessfully = 0;
        FailedRestrictedIdDtoMapper failedRestrictedIdDtoMapper = new FailedRestrictedIdDtoMapper();

        for (RestrictedIdInputDto restrictedIdInputDto : restrictedIds) {
            log.debug("Adding restricted id idValue: {}, idType: {}, platform: {}",  restrictedIdInputDto.getIdValue(), restrictedIdInputDto.getIdType(), restrictedIdInputDto.getPlatform());

            try {
                // Is there already a restrictions on idValue?
                RestrictedIdOutputDto restrictedIdOutputDto = getRestrictedId(restrictedIdInputDto.getIdValue(), restrictedIdInputDto.getIdType(), restrictedIdInputDto.getPlatform());

                if (restrictedIdOutputDto == null) {
                    createRestrictedId(touchDsStorageRecord, restrictedIdInputDto);
                } else {
                    // the restriction already existed, so updating it instead
                    updateRestrictedId(restrictedIdOutputDto.getId(), touchDsStorageRecord, restrictedIdInputDto);
                }

                // If no exception was thrown, we know that the restriction was created
                processedSuccessfully++;

            } catch (Exception exception) { // need to catch every exception that could be thrown
                log.error("Failed to add restricted id restrictedIdInputDto: {}, exception: ", restrictedIdInputDto, exception);
                FailedRestrictedIdDto failedRestrictedIdDto = failedRestrictedIdDtoMapper.map(restrictedIdInputDto, exception);

                failedRestrictedIdDtoList.add(failedRestrictedIdDto);
            }
        }

        // Need to start with this, for not getting wrongly PARTIAL_PROCESSED
        if (processedSuccessfully == 0) {
            processedRestrictedIdsOutputDto.setProcessStatus(ProcessStatusDto.FAILED);
        } else if (failedRestrictedIdDtoList.isEmpty()) {
            processedRestrictedIdsOutputDto.setProcessStatus(ProcessStatusDto.SUCCESS);
        } else if (!failedRestrictedIdDtoList.isEmpty()) {
            processedRestrictedIdsOutputDto.setProcessStatus(ProcessStatusDto.PARTIAL_PROCESSED);
        }

        processedRestrictedIdsOutputDto.setProcessedSuccessfully(processedSuccessfully);
        processedRestrictedIdsOutputDto.setFailedRestrictedIds(failedRestrictedIdDtoList);
        log.info("Successfully added: {}. Failed to add: {}", processedSuccessfully, failedRestrictedIdDtoList.size());
        return processedRestrictedIdsOutputDto;
    }

    /**
     * Get all restricted ids
     *
     * @param idType   only get restricedIds with this idType
     * @param platform only get retstrictedIds for this platform
     * @return
     */
    public static List<RestrictedIdOutputDto> getAllRestrictedIds(IdTypeEnumDto idType, PlatformEnumDto platform) {
        return BaseModuleStorage.performStorageAction("get restricted ids", RightsModuleStorage.class, storage -> ((RightsModuleStorage) storage).getAllRestrictedIds(idType.getValue(), platform.getValue()));
    }

    /**
     * Retrieves the drHoldbackValue based on the specified content and form values.
     * If no drHoldbackValue is found for the given content and form, a {@link NotFoundServiceException} is thrown.
     *
     * @param content the content identifier, must be a valid integer.
     * @param form    the form identifier, must be a valid integer.
     * @return the drHoldbackValue as a {@link String} if found.
     */
    public static String getDrHoldbackValueFromContentAndFormValues(Integer content, Integer form) {
        return BaseModuleStorage.performStorageAction("Get drHoldbackValue", RightsModuleStorage.class, storage -> {
            String id = ((RightsModuleStorage) storage).getDrHoldbackValueByContentAndForm(content, form);
            if (id == null) {
                log.warn("No DR holdback found for content: '{}' and form: '{}'. Returning an empty string", content, form);
                return "";
            }
            return id;
        });
    }

    /**
     * Calculate the rights for a specific record based on the input values provided.
     *
     * @param rightsCalculationInputDto the input DTO containing the needed information for rights calculation.
     * @return a {@link RightsCalculationOutputDto} containing the calculated rights.
     * @throws SQLException if an error occurs during the SQL process.
     */
    public static RightsCalculationOutputDto calculateRightsForRecord(RightsCalculationInputDto rightsCalculationInputDto) throws SQLException {
        RightsCalculationOutputDto output = new RightsCalculationOutputDto();

        switch (rightsCalculationInputDto.getPlatform()) {
            case DRARKIV:
                RightsCalculationOutputDrDto drOutput = RightsCalculation.calculateDrRights(rightsCalculationInputDto);
                output.setDr(drOutput);
                return output;
            case GENERIC:
                log.error("The generic format haven't been implemented yet and therefore it cannot be used");
                throw new InternalServiceException("The generic format haven't been implemented yet");
            default:
                throw new InternalServiceException("A valid platform enum should have been specified. Allowed values are: '" + Arrays.toString(PlatformEnumDto.values()) + "'");

        }
    }

    /**
     * Checks if the specified idValue is restricted based on the provided idType and platform.
     * The method performs a storage action to retrieve the restriction status
     * for the idValue and returns true if the idValue is restricted, and false otherwise.
     *
     * @param idValue  the idValue to check for restrictions
     * @param idType   the type of the id. Types are: ds_id, dr_produktions_id, strict_title
     * @param platform the platform where the id is being checked (e.g. DRARKIV)
     * @return true if the id is restricted; false otherwise
     * @throws SQLException if an error occurs during the SQL process.
     */
    public static boolean isIdRestricted(String idValue, IdTypeEnumDto idType, PlatformEnumDto platform) throws SQLException {
        return BaseModuleStorage.performStorageAction("Get restricted idValue", RightsModuleStorage.class, storage -> {
            RestrictedIdOutputDto idOutput = ((RightsModuleStorage) storage).getRestrictedId(idValue, idType.getValue(), platform.getValue());
            // If the object is null, then idValue is not restricted
            return idOutput != null;
        });
    }

    /**
     * Checks if the specified production code from metadata is allowed based on the provided platform.
     * <p>
     * This method interacts with the storage system to determine whether the given production
     * code is considered allowed. It performs a storage action to retrieve the restriction status
     * for the production code and returns true if the code is allowed, and false otherwise.
     *
     * @param productionCode the production code to check for allowance
     * @param platform       the platform where the production code is being checked (e.g. DRARKIV)
     * @return true if the production code is allowed; false otherwise
     */
    public static boolean isProductionCodeAllowed(String productionCode, String platform) {
        return BaseModuleStorage.performStorageAction("Get restricted id", RightsModuleStorage.class, storage -> {
            RestrictedIdOutputDto idOutput = ((RightsModuleStorage) storage).getRestrictedId(productionCode, IdTypeEnumDto.OWNPRODUCTION_CODE.getValue(), platform);
            // If the object is null, then productionCode from metadata is not allowed

            if (idOutput == null) {
                log.debug("The specified production code '{}' is not known in the database. Therefore it cannot be allowed.", productionCode);
                return false;
            } else {
                log.debug("Production Code is allowed");
                return true;
            }
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
    public static DrHoldbackRuleOutputDto getDrHoldbackRuleByDrHoldbackValue(String drHoldbackValue) {
        inputValidator.validateDrHoldbackValue(drHoldbackValue);

        return BaseModuleStorage.performStorageAction("Get DR holdback rule", RightsModuleStorage.class, storage -> {
            DrHoldbackRuleOutputDto drHoldbackRuleOutputDto = ((RightsModuleStorage) storage).getDrHoldbackRuleByDrHoldbackValue(drHoldbackValue);

            if (drHoldbackRuleOutputDto == null) {
                final String errorMessage = "DR holdback rule not found for drHoldbackValue: " + drHoldbackValue;
                log.error(errorMessage);
                throw new NotFoundServiceException(errorMessage);
            }

            return drHoldbackRuleOutputDto;
        });
    }

    /**
     * Retrieves the DR holdback rule identified by the specified id.
     * This method performs a storage action to access the RightsModuleStorage
     * and fetch the DR holdback rule as an object.
     *
     * @param id the id of the DR holdback rule to retrieve. It must not be null or empty.
     * @return the {@link DrHoldbackRuleOutputDto} corresponding to the specified id if found.
     * @throws NotFoundServiceException if no rule is found for the specified id.
     */
    public static DrHoldbackRuleOutputDto getDrHoldbackRuleById(Long id) {
        inputValidator.validateId(id);

        return BaseModuleStorage.performStorageAction("Get DR holdback rule", RightsModuleStorage.class, storage -> {
            DrHoldbackRuleOutputDto drHoldbackRuleOutputDto = ((RightsModuleStorage) storage).getDrHoldbackRuleById(id);

            if (drHoldbackRuleOutputDto == null) {
                final String errorMessage = "DR holdback rule not found for id: " + id;
                log.error(errorMessage);
                throw new NotFoundServiceException(errorMessage);
            }

            return drHoldbackRuleOutputDto;
        });
    }

    /**
     * Get all DR holdback rules
     *
     * @return
     */
    public static List<DrHoldbackRuleOutputDto> getDrHoldbackRules() {
        return BaseModuleStorage.performStorageAction("Get DR holdback rule", RightsModuleStorage.class, storage -> ((RightsModuleStorage) storage).getDrHoldbackRules());
    }

    /**
     * Creates a DR holdback rule using the provided {@link DrHoldbackRuleInputDto}.
     *
     * @param drHoldbackRuleInputDto
     */
    public static DrHoldbackRuleOutputDto createDrHoldbackRule(DrHoldbackRuleInputDto drHoldbackRuleInputDto) {
        inputValidator.validateDrHoldbackValue(drHoldbackRuleInputDto.getDrHoldbackValue());

        return BaseModuleStorage.performStorageAction("Create DR holdback rule", RightsModuleStorage.class, storage -> {
            long id = ((RightsModuleStorage) storage).createDrHoldbackRule(drHoldbackRuleInputDto.getDrHoldbackValue(), drHoldbackRuleInputDto.getName(), drHoldbackRuleInputDto.getDays());

            DrHoldbackRuleOutputDto drHoldbackRuleOutputDto = ((RightsModuleStorage) storage).getDrHoldbackRuleByDrHoldbackValue(drHoldbackRuleInputDto.getDrHoldbackValue());

            ChangeDifferenceText change = RightsChangelogGenerator.createDrHoldbackRuleChanges(drHoldbackRuleOutputDto);
            AuditLogEntry logEntry = new AuditLogEntry(id, null, ChangeTypeEnumDto.CREATE, ObjectTypeEnumDto.HOLDBACK_RULE, drHoldbackRuleInputDto.getDrHoldbackValue(), null, change.getBefore(), change.getAfter());
            ((AuditLogModuleStorage) storage).persistAuditLog(logEntry);

            log.info("Created DR holdback rule: {}", drHoldbackRuleOutputDto);

            return drHoldbackRuleOutputDto;
        });
    }

    /**
     * Update the number of days for a DR holdback rule
     *
     * @param id the id of the dr holdback rule
     **/
    public static DrHoldbackRuleOutputDto updateDrHoldbackRule(Long id, DrHoldbackRuleInputDto drHoldbackRuleInputDto) {
        inputValidator.validateId(id);
        inputValidator.validateDays(drHoldbackRuleInputDto.getDays());

        // Check if the id has a correspondent rule
        DrHoldbackRuleOutputDto oldDrHoldbackRuleOutputDto = getDrHoldbackRuleById(id);

        return BaseModuleStorage.performStorageAction("update DR holdback rule", RightsModuleStorage.class, storage -> {
            ((RightsModuleStorage) storage).updateDrHoldbackRule(id, drHoldbackRuleInputDto.getDays());

            DrHoldbackRuleOutputDto updatedDrHoldbackRuleOutputDto = ((RightsModuleStorage) storage).getDrHoldbackRuleById(id);

            ChangeDifferenceText change = RightsChangelogGenerator.updateDrHoldbackRuleChanges(oldDrHoldbackRuleOutputDto, updatedDrHoldbackRuleOutputDto);
            AuditLogEntry logEntry = new AuditLogEntry(id, null, ChangeTypeEnumDto.UPDATE, ObjectTypeEnumDto.HOLDBACK_DAY, drHoldbackRuleInputDto.getDrHoldbackValue(), drHoldbackRuleInputDto.getChangeComment(), change.getBefore(), change.getAfter());
            ((AuditLogModuleStorage) storage).persistAuditLog(logEntry);
            log.info("Updated DR holdback rule: {}, updatedDrHoldbackRuleOutputDto: {}", id, updatedDrHoldbackRuleOutputDto);
            return updatedDrHoldbackRuleOutputDto;
        });
    }

    /**
     * Delete a DR holdback rule
     *
     * @param id id of the DR holdback rule
     * @param deleteReasonDto comment about why object gets deleted
     */
    public static RecordsCountDto deleteDrHoldbackRule(Long id, DeleteReasonDto deleteReasonDto) {
        inputValidator.validateId(id);
        inputValidator.validateChangeComment(deleteReasonDto.getChangeComment());

        // Check if the id has a correspondent rule
        DrHoldbackRuleOutputDto deleteDrHoldbackRuleOutputDto = getDrHoldbackRuleById(id);

        return BaseModuleStorage.performStorageAction("Delete DR holdback rule", RightsModuleStorage.class, storage -> {
            // Delete entry from database
            RecordsCountDto recordsCountDto = new RecordsCountDto();
            int deletedCount = ((RightsModuleStorage) storage).deleteDrHoldbackRule(id);
            recordsCountDto.setCount(deletedCount);

            ChangeDifferenceText change = RightsChangelogGenerator.deleteDrHoldbackRuleChanges(deleteDrHoldbackRuleOutputDto);
            AuditLogEntry logEntry = new AuditLogEntry(deleteDrHoldbackRuleOutputDto.getId(), null, ChangeTypeEnumDto.DELETE, ObjectTypeEnumDto.HOLDBACK_RULE, deleteDrHoldbackRuleOutputDto.getDrHoldbackValue(), deleteReasonDto.getChangeComment(), change.getBefore(), change.getAfter());
            ((AuditLogModuleStorage) storage).persistAuditLog(logEntry);

            log.info("Deleted DR holdback rule: {} ", deleteDrHoldbackRuleOutputDto);

            return recordsCountDto;
        });
    }

    /**
     * Retrieves DR holdback ranges identified by the input drHoldbackValue.
     * This method performs a storage action to access the RightsModuleStorage
     * and fetch the DR holdback ranges as object in a list.
     *
     * @param drHoldbackValue the drHoldbackValue of the DR holdback rule to retrieve. It must not be null or empty.
     * @return list of {@link DrHoldbackRangeOutputDto} corresponding to the specified drHoldbackValue if found.
     * @throws NotFoundServiceException if no rule is found for the specified drHoldbackValue.
     */
    public static List<DrHoldbackRangeOutputDto> getDrHoldbackRanges(String drHoldbackValue) {
        inputValidator.validateDrHoldbackValue(drHoldbackValue);

        return BaseModuleStorage.performStorageAction("Get DR holdback ranges for " + drHoldbackValue, RightsModuleStorage.class, storage -> {
            List<DrHoldbackRangeOutputDto> drHoldbackRangeOutputDtoList = ((RightsModuleStorage) storage).getDrHoldbackRangesByDrHoldbackValue(drHoldbackValue);

            if (drHoldbackRangeOutputDtoList.isEmpty()) {
                final String errorMessage = "DR holdback ranges not found for drHoldbackValue: " + drHoldbackValue;
                log.error(errorMessage);
                throw new NotFoundServiceException(errorMessage);
            }

            return drHoldbackRangeOutputDtoList;
        });
    }

    /**
     * Creates DR holdback range from form and content range combinations for a drHoldbackValue using the provided
     * {@link DrHoldbackRangeInputDto}.
     * This requires the drHoldbackValue to be present in the DR holdback rule table
     *
     * @param drHoldbackRangeInputDto
     */
    public static List<DrHoldbackRangeOutputDto> createDrHoldbackRanges(DrHoldbackRangeInputDto drHoldbackRangeInputDto) {
        inputValidator.validateDrHoldbackValue(drHoldbackRangeInputDto.getDrHoldbackValue());

        // Check if the drHoldbackValue has correspondent DR holdback rule
        getDrHoldbackRuleByDrHoldbackValue(drHoldbackRangeInputDto.getDrHoldbackValue());

        BaseModuleStorage.performStorageAction("Create DR holdback ranges for drHoldbackValue: " + drHoldbackRangeInputDto.getDrHoldbackValue(), RightsModuleStorage.class, storage -> {
            for (DrHoldbackRangeDto drHoldbackRangeDto : drHoldbackRangeInputDto.getRanges()) {
                long objectId = ((RightsModuleStorage) storage).createDrHoldbackRange(drHoldbackRangeDto.getContentRangeFrom(), drHoldbackRangeDto.getContentRangeTo(), drHoldbackRangeDto.getFormRangeFrom(), drHoldbackRangeDto.getFormRangeTo(), drHoldbackRangeInputDto.getDrHoldbackValue());

                ChangeDifferenceText change = RightsChangelogGenerator.createDrHoldbackRangeChanges(drHoldbackRangeDto);
                AuditLogEntry logEntry = new AuditLogEntry(objectId, null, ChangeTypeEnumDto.CREATE, ObjectTypeEnumDto.HOLDBACK_RANGE, drHoldbackRangeInputDto.getDrHoldbackValue(), null, change.getBefore(), change.getAfter());
                ((AuditLogModuleStorage) storage).persistAuditLog(logEntry);

                log.info("Created DR holdback range: {}", drHoldbackRangeDto);
            }
            return null;
        });

        List<DrHoldbackRangeOutputDto> drHoldbackRangeOutputDtoList = getDrHoldbackRanges(drHoldbackRangeInputDto.getDrHoldbackValue());
        return drHoldbackRangeOutputDtoList;
    }

    /**
     * Deletes all form and content range combinations for a drHoldbackValue
     *
     * @param drHoldbackValue drHoldbackValue of DR holdback ranges
     * @param deleteReasonDto comment about why object gets deleted
     */
    public static RecordsCountDto deleteDrHoldbackRanges(String drHoldbackValue, DeleteReasonDto deleteReasonDto) {
        inputValidator.validateDrHoldbackValue(drHoldbackValue);
        inputValidator.validateChangeComment(deleteReasonDto.getChangeComment());

        // Check if the drHoldbackValue has correspondent DR holdback ranges
        List<DrHoldbackRangeOutputDto> oldRanges = getDrHoldbackRanges(drHoldbackValue);

        return BaseModuleStorage.performStorageAction("Delete DR holdback ranges", RightsModuleStorage.class, storage -> {
            // Delete entry from database
            RecordsCountDto recordsCountDto = new RecordsCountDto();
            int deletedCount = ((RightsModuleStorage) storage).deleteRangesByDrHoldbackValue(drHoldbackValue);
            recordsCountDto.setCount(deletedCount);

            for (DrHoldbackRangeOutputDto drHoldbackRangeOutputDto : oldRanges) {
                ChangeDifferenceText change = RightsChangelogGenerator.deleteDrHoldbackRangeChanges(drHoldbackRangeOutputDto);
                AuditLogEntry logEntry = new AuditLogEntry(drHoldbackRangeOutputDto.getId(), null, ChangeTypeEnumDto.DELETE, ObjectTypeEnumDto.HOLDBACK_RANGE, drHoldbackValue, deleteReasonDto.getChangeComment(), change.getBefore(), change.getAfter());
                ((AuditLogModuleStorage) storage).persistAuditLog(logEntry);

                log.info("Deleted DR holdback range: {}", drHoldbackRangeOutputDto);
            }
            return recordsCountDto;
        });
    }

    /**
     * Based on idType, touch related storage records, so that they can be re-indexed with the new information.
     *
     * @param id     which have been updated in the rights table
     * @param idType to determine how related records are updated.
     * @return amount of records touched in DS-Storage.
     * @throws SolrServerException
     * @throws IOException
     */
    public static int touchRelatedStorageRecords(String id, IdTypeEnumDto idType) throws SolrServerException, IOException {
        switch (idType) {
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
     *
     * @param id to touch in DS-storage.
     * @return amount of records touched in DS-Storage.
     */
    private static int touchStorageRecordById(String id) {
        try {
            dk.kb.storage.model.v1.RecordsCountDto count = storageClient.touchRecord(id);
            if (count == null || count.getCount() == null) {
                return 0;
            }
            return count.getCount();
        } catch (NotFoundServiceException e) {
            log.info("Touching storage record not found " + id);
            return 0;
        }
    }

    /**
     * Query solr for all records where the restricted title is present and touch the records in DS-storage.
     *
     * @param strictTitle to query solr for.
     * @return amount of records touched in DS-Storage.
     * @throws SolrServerException
     * @throws IOException
     */
    private static int touchStorageRecordsByStrictTitle(String strictTitle) throws SolrServerException, IOException {
        String solrField = "title_strict";
        return touchStorageRecordsByIdFromSolrQuery(solrField, strictTitle);
    }

    /**
     * Query solr for all records where the productionCode is present and touch the records in DS-storage.
     *
     * @param productionCode to query solr for.
     * @return amount of records touched in DS-Storage.
     * @throws SolrServerException
     * @throws IOException
     */
    private static int touchStorageRecordsByProductionCode(String productionCode) throws SolrServerException, IOException {
        String solrField = "production_code_value";
        return touchStorageRecordsByIdFromSolrQuery(solrField, productionCode);
    }

    /**
     * Query solr for all records where the drProductionId is present and touch the records in DS-storage.
     *
     * @param drProductionId to query solr for.
     * @return amount of records touched in DS-Storage.
     * @throws SolrServerException
     * @throws IOException
     */
    private static int touchStorageRecordsByProductionId(String drProductionId) throws SolrServerException, IOException {
        String solrField = "dr_production_id";
        return touchStorageRecordsByIdFromSolrQuery(solrField, drProductionId);
    }

    /**
     * Perform a solr query as {@code solrField:"fieldValue"} and for each record in the solr response get the id for
     * each record and touch the related DS-Record in DS-Storage.
     *
     * @param solrField  to query for the fieldValue.
     * @param fieldValue to query for.
     * @return the amount of records touched in DS-Storage.
     * @throws SolrServerException
     * @throws IOException
     */
    private static int touchStorageRecordsByIdFromSolrQuery(String solrField, String fieldValue) throws SolrServerException, IOException {
        List<SolrServerClient> servers = ServiceConfig.getSolrServers();
        int touchedRecordsCount = 0;

        // Ds-license supports multiple backing solr servers. So we have to wrap it in this for-loop
        for (SolrServerClient server : servers) {
            int rows = 500;
            int start = 0;

            SolrQuery query = getIdSolrQuery(solrField, fieldValue, rows);

            while (true) {
                // Update start value before the query is fired against the server
                query.setStart(start);

                // Query solr for a response
                QueryResponse response = server.query(query);
                SolrDocumentList results = response.getResults();

                // For each record in the result touch the related DS-storage record
                for (SolrDocument doc : results) {
                    dk.kb.storage.model.v1.RecordsCountDto touched = touchStorageRecords(doc);
                    if (touched != null && touched.getCount() != null) {
                        touchedRecordsCount = touchedRecordsCount + touched.getCount();
                    }
                }

                long totalResults = results.getNumFound();

                // Break the loop of no more records are available
                if (start + rows >= totalResults) {
                    break;
                }

                // Increment start by rows
                start += rows;
            }
        }

        return touchedRecordsCount;
    }

    /**
     * Create a solr query on the form {@code solrField:"fieldValue"} where the rows param is set to {@code rows} The query only returns the field ID from the documents.
     *
     * @param solrField  to query.
     * @param fieldValue used to query the field defined above.
     * @param rows       which determines the amount of documents returned by the query.
     * @return a {@link SolrQuery} that can be fired as is or further developed for paging etc.
     */
    private static SolrQuery getIdSolrQuery(String solrField, String fieldValue, int rows) {
        SolrQuery query = new SolrQuery();
        query.setQuery(solrField + ":\"" + fieldValue + "\"");
        query.setRows(rows);
        query.setFields("id");
        return query;
    }

    /**
     * Touch a related DS-Record in the backing DS-Storage extracting the ID from a given solr document and use that ID when querying the DS-StorageClient
     *
     * @param doc solr document to extract ID from.
     */
    private static dk.kb.storage.model.v1.RecordsCountDto touchStorageRecords(SolrDocument doc) {
        // For each document in the result, touch its ds-storage record.
        String id = (String) doc.getFieldValue("id");
        log.debug("Touching DS-storage record with id: '{}'", id);
        return storageClient.touchRecord(id);
    }
}
