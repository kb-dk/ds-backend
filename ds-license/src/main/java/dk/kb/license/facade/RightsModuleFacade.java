package dk.kb.license.facade;

import dk.kb.license.RightsCalculation;
import dk.kb.license.model.v1.DrHoldbackRuleDto;
import dk.kb.license.model.v1.RestrictedIdInputDto;
import dk.kb.license.model.v1.RightsCalculationInputDto;
import dk.kb.license.model.v1.RightsCalculationOutputDrDto;
import dk.kb.license.model.v1.RightsCalculationOutputDto;
import dk.kb.license.storage.BaseModuleStorage;
import dk.kb.license.storage.RightsModuleStorage;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.NotFoundServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Arrays;

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
     * Retrieves the holdback ID based on the specified content and form values.
     * If no holdback ID is found for the given content and form, a {@link NotFoundServiceException} is thrown.
     *
     * @param content the content identifier, must be a valid integer.
     * @param form the form identifier, must be a valid integer.
     * @return the holdback ID as a {@link String} if found.
     * @throws SQLException if a database access error occurs during the
     *                      storage action.
     * @throws NotFoundServiceException if no holdback ID is found for the
     *                                   specified content and form.
     */
    public static String getHoldbackIdFromContentAndFormValues(Integer content, Integer form) throws SQLException {
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

    private static RightsModuleStorage getRightsStorage() {
        try {
            return new RightsModuleStorage();
        } catch (SQLException e) {
            log.error("Error creating Storage ",e);
            throw new InternalServiceException("Error creating storage");
        }
    }

}
