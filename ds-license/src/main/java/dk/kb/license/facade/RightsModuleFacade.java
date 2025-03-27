package dk.kb.license.facade;

import dk.kb.license.RightsCalculation;
import dk.kb.license.model.v1.DrHoldbackRuleDto;
import dk.kb.license.model.v1.RestrictedIdInputDto;
import dk.kb.license.model.v1.RestrictedIdOutputDto;
import dk.kb.license.model.v1.RightsCalculationInputDto;
import dk.kb.license.model.v1.RightsCalculationOutputDrDto;
import dk.kb.license.model.v1.RightsCalculationOutputDto;
import dk.kb.license.storage.BaseModuleStorage;
import dk.kb.license.storage.RightsModuleStorage;
import dk.kb.license.webservice.KBAuthorizationInterceptor;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import dk.kb.util.webservice.exception.NotFoundServiceException;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.keycloak.representations.AccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RightsModuleFacade {
    private static final Logger log = LoggerFactory.getLogger(RightsModuleFacade.class);


    /**
     * Creates a restricted ID using the provided input data transfer object (DTO) and user ID.
     *
     * @param restrictedIdInputDto the data transfer object containing the details of the restricted ID to be created.
     *                               This should not be null.
     * @param userId               the ID of the user performing the action.
     * @throws SQLException if there is an error while persisting the restricted ID in the database.
     */
    public static void createRestrictedId(RestrictedIdInputDto restrictedIdInputDto, String userId) throws SQLException {
        validateCommentLength(restrictedIdInputDto);
        BaseModuleStorage.performStorageAction("Persist restricted ID (klausulering)", new RightsModuleStorage(), storage -> {
            ((RightsModuleStorage) storage).createRestrictedId(
                    restrictedIdInputDto.getIdValue(),
                    restrictedIdInputDto.getIdType(),
                    restrictedIdInputDto.getPlatform(),
                    restrictedIdInputDto.getComment(),
                    userId,
                    System.currentTimeMillis());
            log.info("Created restriction {}", restrictedIdInputDto);
            return null;
        });
    }

    /**
     * Update a restricted ID using the provided input data transfer object (DTO) and user ID.
     *
     * @param restrictedIdInputDto the data transfer object containing the details of the restricted ID to be Updated.
     *                               This should not be null.
     * @throws SQLException if there is an error while persisting the restricted ID in the database.
     */
    public static void updateRestrictedId(RestrictedIdInputDto restrictedIdInputDto) throws SQLException {
        validateCommentLength(restrictedIdInputDto);
        BaseModuleStorage.performStorageAction("Update restricted ID (klausulering)", new RightsModuleStorage(), storage -> {
            ((RightsModuleStorage) storage).updateRestrictedId(
                    restrictedIdInputDto.getIdValue(),
                    restrictedIdInputDto.getIdType(),
                    restrictedIdInputDto.getPlatform(),
                    restrictedIdInputDto.getComment(),
                    getCurrentUserID(),
                    System.currentTimeMillis());
            log.info("Updating restricted ID {}",restrictedIdInputDto);
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
    public static void createRestrictedIds(List<RestrictedIdInputDto> restrictedIds) throws SQLException {
        BaseModuleStorage.performStorageAction("create restricted ID",new RightsModuleStorage(), storage -> {
            for (RestrictedIdInputDto id : restrictedIds) {
                validateCommentLength(id);

                ((RightsModuleStorage) storage).createRestrictedId(
                        id.getIdValue(),
                        id.getIdType(),
                        id.getPlatform(),
                        id.getComment(),
                        getCurrentUserID(),
                        System.currentTimeMillis()
                );
            }
            return null;
        });
        log.info("Added restricted IDs: [{}] ",
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
        return BaseModuleStorage.performStorageAction("Get holdback ID", getRightsStorage(), storage -> {
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
        return BaseModuleStorage.performStorageAction("Get holdback rule", new RightsModuleStorage(), storage -> {
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
        return BaseModuleStorage.performStorageAction("Get restricted id", getRightsStorage(), storage -> {
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
        return BaseModuleStorage.performStorageAction("Get restricted id", getRightsStorage(), storage -> {
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


    private static RightsModuleStorage getRightsStorage() {
        try {
            return new RightsModuleStorage();
        } catch (SQLException e) {
            log.error("Error creating Storage ",e);
            throw new InternalServiceException("Error creating storage");
        }
    }

    /**
     * Gets the name of the current user from the OAuth token.
     * @return
     */
    private static String getCurrentUserID() {
        Message message = JAXRSUtils.getCurrentMessage();
        AccessToken token = (AccessToken) message.get(KBAuthorizationInterceptor.ACCESS_TOKEN);
        if (token != null) {
            return token.getName();
        }
        return "no user";
    }

    private static void validateCommentLength(RestrictedIdInputDto id) {
        if (id.getComment() != null && id.getComment().length() > 1024){
            log.error("Comment was too long and cannot be added to rights module. Only 1024 characters are allowed.");
            throw new InvalidArgumentServiceException("Comment was too long and cannot be added to rights module. Only 1024 characters are allowed.");
        }
    }

}
