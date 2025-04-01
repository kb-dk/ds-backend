package dk.kb.license.facade;

import dk.kb.license.RightsCalculation;
import dk.kb.license.config.ServiceConfig;
import dk.kb.license.model.v1.*;
import dk.kb.license.solr.SolrServerClient;
import dk.kb.license.storage.BaseModuleStorage;
import dk.kb.license.storage.RightsModuleStorage;
import dk.kb.license.webservice.KBAuthorizationInterceptor;
import dk.kb.storage.model.v1.RecordsCountDto;
import dk.kb.storage.util.DsStorageClient;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import dk.kb.util.webservice.exception.NotFoundServiceException;
import dk.kb.util.yaml.YAML;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.keycloak.representations.AccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RightsModuleFacade {
    private static final Logger log = LoggerFactory.getLogger(RightsModuleFacade.class);

    private static final DsStorageClient storageClient = new DsStorageClient(ServiceConfig.getConfig().getString("storageClient.url"));
    /**
     * Retrieves a restrictedID output object from the database
     *
     * @param id the id value
     * @param idType type of ID
     * @param platform The platform
     * @return
     * @throws SQLException
     */
    public static RestrictedIdOutputDto getRestrictedId(String id, String idType, String platform) throws SQLException {
        return BaseModuleStorage.performStorageAction("Get restricted ID", RightsModuleStorage.class, storage -> ((RightsModuleStorage)storage).getRestrictedId(id, idType, platform));
    }

    /**
     * Creates a restricted ID using the provided input data transfer object (DTO) and user ID.
     *
     * @param restrictedIdInputDto the data transfer object containing the details of the restricted ID to be created.
     *                               This should not be null.
     * @throws SQLException if there is an error while persisting the restricted ID in the database.
     */
    public static void createRestrictedId(RestrictedIdInputDto restrictedIdInputDto, String user, boolean touchDsStorageRecord) throws SQLException {
        validateCommentLength(restrictedIdInputDto);
        validatePlatformAndIdType(restrictedIdInputDto.getPlatform(), restrictedIdInputDto.getIdType());
        BaseModuleStorage.performStorageAction("Persist restricted ID (klausulering)", RightsModuleStorage.class, storage -> {
            ((RightsModuleStorage) storage).createRestrictedId(
                    restrictedIdInputDto.getIdValue(),
                    restrictedIdInputDto.getIdType(),
                    restrictedIdInputDto.getPlatform(),
                    restrictedIdInputDto.getComment(),
                    user,
                    System.currentTimeMillis());
            log.info("Created restriction {}", restrictedIdInputDto);
            if (touchDsStorageRecord) {
                touchRelatedStorageRecords(restrictedIdInputDto.getIdValue(), restrictedIdInputDto.getIdType());
            }
            return null;
        });
    }

    public static void deleteRestrictedId(String id, String idType, String platform, boolean touchDsStorageRecord) throws Exception {
        BaseModuleStorage.performStorageAction("delete restricted ID",RightsModuleStorage.class, storage -> {
            ((RightsModuleStorage) storage).deleteRestrictedId(id, idType, platform);
            if (touchDsStorageRecord) {
                touchRelatedStorageRecords(id, idType);
            }
            return null;
        });
        log.info("Deleted restricted id:{} idType:{} platform:{}", id, idType, platform);
    }

    /**
     * Update a restricted ID using the provided input data transfer object (DTO) and user ID.
     *
     * @param restrictedIdInputDto the data transfer object containing the details of the restricted ID to be Updated.
     *                             This should not be null.
     * @param touchDsStorageRecord
     * @throws SQLException if there is an error while persisting the restricted ID in the database.
     */
    public static void updateRestrictedId(RestrictedIdInputDto restrictedIdInputDto, String user, boolean touchDsStorageRecord) throws SQLException {
        validatePlatformAndIdType(restrictedIdInputDto.getPlatform(), restrictedIdInputDto.getIdType());
        validateCommentLength(restrictedIdInputDto);
        BaseModuleStorage.performStorageAction("Update restricted ID (klausulering)", RightsModuleStorage.class, storage -> {
            ((RightsModuleStorage) storage).updateRestrictedId(
                    restrictedIdInputDto.getIdValue(),
                    restrictedIdInputDto.getIdType(),
                    restrictedIdInputDto.getPlatform(),
                    restrictedIdInputDto.getComment(),
                    user,
                    System.currentTimeMillis());
            if (touchDsStorageRecord) {
                touchRelatedStorageRecords(restrictedIdInputDto.getIdValue(), restrictedIdInputDto.getIdType());
            }
            log.info("Updated restricted ID {}",restrictedIdInputDto);
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
                validatePlatformAndIdType(id.getPlatform(), id.getIdType());
                validateCommentLength(id);

                ((RightsModuleStorage) storage).createRestrictedId(
                        id.getIdValue(),
                        id.getIdType(),
                        id.getPlatform(),
                        id.getComment(),
                        user,
                        System.currentTimeMillis()
                );
                if (touchDsStorageRecord) {
                    touchRelatedStorageRecords(id.getIdValue(), id.getIdType());
                }
            }
            return null;
        });
        log.info("Added restricted IDs: [{}] ",
                restrictedIds.stream().map(RestrictedIdInputDto::toString).collect(Collectors.joining(", ")));

    }

    /**
     * Deletes multiple restricted Ids
     *
     * @param restrictedIds        list of restricted Ids to be deleted.
     * @param touchDsStorageRecord
     */
    public static void deleteRestrictedIds(List<RestrictedIdInputDto> restrictedIds, boolean touchDsStorageRecord) {
        BaseModuleStorage.performStorageAction("delete restricted ID",RightsModuleStorage.class, storage -> {
            for(RestrictedIdInputDto id : restrictedIds) {
                ((RightsModuleStorage) storage).deleteRestrictedId(
                        id.getIdValue(),
                        id.getIdType(),
                        id.getPlatform()
                );
                if (touchDsStorageRecord) {
                    touchRelatedStorageRecords(id.getIdValue(), id.getIdType());
                }
            }
            return null;
        });
        log.info("Deleted restricted IDs: [{}] ",
                restrictedIds.stream().map(RestrictedIdInputDto::toString).collect(Collectors.joining(", ")));
    }


    /**
     * Retrieves the holdback ID based on the specified content and form values.
     * If no holdback ID is found for the given content and form, a {@link NotFoundServiceException} is thrown.
     *
     * @param content the content identifier, must be a valid integer.
     * @param form the form identifier, must be a valid integer.
     * @return the holdback ID as a {@link String} if found.
     * @throws NotFoundServiceException if no holdback ID is found for the
     *                                   specified content and form.
     */
    public static String getHoldbackIdFromContentAndFormValues(Integer content, Integer form)  {
        return BaseModuleStorage.performStorageAction("Get holdback ID", RightsModuleStorage.class, storage -> {
            String id = ((RightsModuleStorage) storage).getHoldbackRuleId(content, form);
            if (id == null) {
                log.warn("No holdback found for content: '{}' and form: '{}'. Returning an empty string", content, form);
                return "";
            }
            return id;
        });
    }

    /**
     * Retrieves the DR holdback rule identified by the specified holdback ID.
     * This method performs a storage action to access the RightsModuleStorage
     * and fetch the DR holdback rule as an object.
     *
     * @param holdbackId the ID of the holdback rule to retrieve. It must not be null or empty.
     * @return the {@link DrHoldbackRuleDto} corresponding to the specified ID if found.
     * @throws SQLException if a database access error occurs during the storage transaction.
     * @throws NotFoundServiceException if no rule is found for the specified holdback ID.
     */
    public static DrHoldbackRuleDto getDrHoldbackRuleById(String holdbackId) throws SQLException {
        return BaseModuleStorage.performStorageAction("Get holdback rule", RightsModuleStorage.class, storage -> {
            DrHoldbackRuleDto output = ((RightsModuleStorage) storage).getDrHoldbackFromID(holdbackId);
            if (output != null) {
                return output;
            }
            throw new NotFoundServiceException("holdback rule not found for id: " + holdbackId);
        });
    }

    /**
     * Calculate the rights for a specific record based on the input values provided.
     *
     * @param rightsCalculationInputDto the input DTO containing the needed information for rights calculation.
     * @return a {@link RightsCalculationOutputDto} containing the calculated rights.
     */
    public static RightsCalculationOutputDto calculateRightsForRecord(RightsCalculationInputDto rightsCalculationInputDto) throws SQLException {
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
                        Arrays.toString(RightsCalculationInputDto.PlatformEnum.values()) + "'");

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
    public static boolean isIdRestricted(String id, String idType, String platform) throws SQLException {
        return BaseModuleStorage.performStorageAction("Get restricted id", RightsModuleStorage.class, storage -> {
            RestrictedIdOutputDto idOutput = ((RightsModuleStorage) storage).getRestrictedId(id, idType, platform);
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
    public static boolean isProductionCodeAllowed(String productionCode, String platform)  {
        return BaseModuleStorage.performStorageAction("Get restricted id", RightsModuleStorage.class, storage -> {
            RestrictedIdOutputDto idOutput = ((RightsModuleStorage) storage).getRestrictedId(productionCode, "egenproduktions_kode", platform);
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
     * @param drHoldbackRuleDto
     */
    public static void createDrHoldbackRule(DrHoldbackRuleDto drHoldbackRuleDto) {
        BaseModuleStorage.performStorageAction("Create holdback rule", RightsModuleStorage.class, storage -> {
            ((RightsModuleStorage)storage).createDrHoldbackRule(
                    drHoldbackRuleDto.getId(),
                    drHoldbackRuleDto.getName(),
                    drHoldbackRuleDto.getDays()
            );
            return null;
        });
    }

    /**
     * Delete a holdback rule
     * @param id id of the holdback rule
     */
    public static void deleteDrHoldbackRule(String id) throws SQLException {
        BaseModuleStorage.performStorageAction("Delete holdback rule", RightsModuleStorage.class, storage -> {
            ((RightsModuleStorage)storage).deleteDrHoldbackRule(id);
            return null;
        });
    }

    /**
     * get all holdback rules for DR
     * @return
     */
    public static List<DrHoldbackRuleDto> getAllDrHoldbackRules() throws SQLException {
        return BaseModuleStorage.performStorageAction("Get holdback rule", RightsModuleStorage.class, storage -> ((RightsModuleStorage)storage).getAllDrHoldbackRules());
    }

    /**
     * Retrieve the number of days for a holdback rule, either based on either the id or the name of the holdbackrule.
     *
     * @param id if this parameter is not empty it returns the number of holdback days for the id
     * @param name if this parameter is not empty it returns the number of holdback days for the name
     * @return the number of
     */
    public static Integer getDrHoldbackDaysByIdOrName(String id, String name) {
        Integer days;
        if (!StringUtils.isEmpty(id)) {
            days = BaseModuleStorage.performStorageAction("Get holdback days", RightsModuleStorage.class, storage -> ((RightsModuleStorage) storage).getDrHoldbackdaysFromID(id));
        } else if (!StringUtils.isEmpty(name)) {
            days = BaseModuleStorage.performStorageAction("Get holdback days", RightsModuleStorage.class, storage -> ((RightsModuleStorage) storage).getDrHoldbackDaysFromName(name));
        } else {
            throw new InvalidArgumentServiceException("missing id or name");
        }
        return days;
    }

    /**
     * Retrieve the number of days for a holdback rule, either based on either the id or the name of the holdbackrule.
     *
     * @param days the new number of holdback days for the rule
     * @param id if this parameter is not empty it updates the number of holdback days for the id
     * @param name if this parameter is not empty it updates the number of holdback days for the name
     * @return the number of
     */
    public static void updateDrHoldbackDaysByIdOrName(Integer days, String id, String name) {
        if (!StringUtils.isEmpty(id)) {
            BaseModuleStorage.performStorageAction("update holdback dayss", RightsModuleStorage.class, storage -> {
                ((RightsModuleStorage) storage).updateDrHolbackdaysForId(days,id);
                return null;
            });
        } else if (!StringUtils.isEmpty(name)) {
            BaseModuleStorage.performStorageAction("update holdback dayss", RightsModuleStorage.class, storage -> {
                ((RightsModuleStorage) storage).updateDrHolbackdaysForName(days,name);
                return null;
            });
        } else {
            throw new InvalidArgumentServiceException("missing id or name");
        }
    }
    /**
     * set the form and content range combinations for a list dr_holdback_id
     * This requires the holdback_id to be present in the DR holback rule table
     *
     * @param drHoldbackId
     * @param drHoldbackRangeMappingInputDto
     */
    public static void createHoldbackRanges(String drHoldbackId, List<DrHoldbackRangeMappingInputDto> drHoldbackRangeMappingInputDto) {
        BaseModuleStorage.performStorageAction("Create holdback ranges for "+ drHoldbackId, RightsModuleStorage.class, storage -> {
            for(DrHoldbackRangeMappingInputDto mapping: drHoldbackRangeMappingInputDto) {
                if (((RightsModuleStorage)storage).getDrHoldbackFromID(drHoldbackId) == null) {
                    throw new InvalidArgumentServiceException("No dr holdback_id "+drHoldbackId);
                }
                ((RightsModuleStorage)storage).createDrHoldbackMapping(
                        mapping.getContentRangeFrom(),
                        mapping.getContentRangeTo(),
                        mapping.getFormRangeFrom(),
                        mapping.getFormRangeTo(),
                        drHoldbackId
                );
            }
            return null;
        });
    }
    /**
     * Deletes all form and content range combinations for a drHoldbackID
     * @param drHoldbackId
     */
    public static void deleteHoldbackRanges(String drHoldbackId) {
        BaseModuleStorage.performStorageAction("Delete holdback ranges for " + drHoldbackId, RightsModuleStorage.class, storage -> {
            ((RightsModuleStorage)storage).deleteMappingsForDrHolbackId(drHoldbackId);
            return null;
        });
    }

    /**
     * Deletes all form and content range combinations for drHoldbackId
     *
     * @param drHoldbackId
     * @return
     */
    public static List<DrHoldbackRangeMappingDto> getHoldbackRanges(String drHoldbackId) {
        return BaseModuleStorage.performStorageAction("Get holdbackmappings for "+drHoldbackId, RightsModuleStorage.class, storage-> ((RightsModuleStorage)storage).getHoldbackRangesForHoldbackId(drHoldbackId));
    }

    private static void validateCommentLength(RestrictedIdInputDto id) {
        if (id.getComment() != null && id.getComment().length() > 1024){
            log.error("Comment was too long and cannot be added to rights module. Only 1024 characters are allowed.");
            throw new InvalidArgumentServiceException("Comment was too long and cannot be added to rights module. Only 1024 characters are allowed.");
        }
    }

    private static void validatePlatformAndIdType(String platform, String idType) {
        YAML platformConfig = ServiceConfig.getRightsPlatformConfig(platform);
        if (platformConfig.isEmpty()) {
            throw new IllegalArgumentException("Invalid platform "+platform);
        }
        if (!platformConfig.getList("idTypes").contains(idType)) {
            throw new IllegalArgumentException("Invalid idType "+idType);
        }

    }

    /**
     * Based on idType, touch related storage records, so that they can be re-indexed with the new information.
     * @param id which have been updated in the rights table
     * @param idType to determine how related records are updated.
     * @return amount of records touched in DS-Storage.
     */
    public static int touchRelatedStorageRecords(String id, String idType){
        switch (idType){
            case "ds_id":
                return touchStorageRecordById(id);
            // TODO: Implement the rest of these touches by solr queries-
            case "dr_produktions_id":
                return touchStorageRecordsByProductionId(id);
            case "egenproduktions_kode":
                return touchStorageRecordsByProductionCode(id);
            case "strict_title":
                return touchStorageRecordsByStrictTitle(id);
            default:
                throw new IllegalArgumentException("Invalid idType "+idType);
        }
    }

    /**
     * Touch a single ID directly in DS-Storage.
     * @param id to touch in DS-storage.
     * @return amount of records touched in DS-Storage.
     */
    private static int touchStorageRecordById(String id) {
        RecordsCountDto count = storageClient.touchRecord(id);

        if (count == null || count.getCount() == null){
            return 0;
        }

        return count.getCount();
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
