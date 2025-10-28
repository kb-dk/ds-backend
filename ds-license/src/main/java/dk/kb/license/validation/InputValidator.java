package dk.kb.license.validation;

import dk.kb.license.model.v1.RestrictedIdInputDto;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InputValidator {
    private static final Logger log = LoggerFactory.getLogger(InputValidator.class);

    final String dsTv = "ds.tv:oai:io:";
    final String dsRadio = "ds.radio:oai:io:";
    private static final int MAX_ID_VALUE_LENGTH = 256;
    private static final int MAX_TITLE_LENGTH = 4096;
    private static final int MAX_COMMENT_LENGTH = 16384;

    /**
     * Check if dsId starts with "ds.tv:oai:io:" or "ds.radio:oai:io:"
     * It does not check UUID part of the dsId because we don't control it, and it could change later, and then we don't
     * want the validation to fail.
     *
     * @param dsId to be validated
     */
    public void validateDsId(String dsId) {
        if (!StringUtils.startsWith(dsId, dsTv) && !StringUtils.startsWith(dsId, dsRadio)) {
            final String errorMessage = "Invalid dsId: " + dsId;
            log.error(errorMessage);
            throw new InvalidArgumentServiceException(errorMessage);
        }
    }

    /**
     * Validates drProductionId in the given {@link RestrictedIdInputDto}.
     *
     * @param drProductionId to be validated
     */
    public void validateDrProductionIdFormat(String drProductionId) {
        if (StringUtils.isBlank(drProductionId)) {
            final String errorMessage = "The drProductionId cannot be empty";
            log.error(errorMessage);
            throw new InvalidArgumentServiceException(errorMessage);
        }

        if (!drProductionId.matches("\\d+")) {
            final String errorMessage = "The drProductionId: " + drProductionId + " should only contain digits";
            log.error(errorMessage);
            throw new InvalidArgumentServiceException(errorMessage);
        }

        if (drProductionId.length() <= 7) {
            final String errorMessage = "The drProductionId: " + drProductionId + " should be at least 8 digits";
            log.error(errorMessage);
            throw new InvalidArgumentServiceException(errorMessage);
        }
    }

    /**
     * Validates that strictTitle is not null, empty or not blank in the given {@link RestrictedIdInputDto}.
     *
     * @param strictTitle to be validated
     */
    public void validateStrictTitle(String strictTitle) {
        if (StringUtils.isBlank(strictTitle)) {
            final String errorMessage = "The strictTitle cannot be empty";
            log.error(errorMessage);
            throw new InvalidArgumentServiceException(errorMessage);
        }
    }

    /**
     * Validates that ownProductionCode is not null, empty or not blank in the given {@link RestrictedIdInputDto}.
     *
     * @param ownProductionCode to be validated
     */
    public void validateOwnProductionCode(String ownProductionCode) {
        if (StringUtils.isBlank(ownProductionCode)) {
            final String errorMessage = "The ownProductionCode cannot be empty";
            log.error(errorMessage);
            throw new InvalidArgumentServiceException(errorMessage);
        }

        if (!ownProductionCode.matches("\\d+")) {
            final String errorMessage = "The ownProductionCode: " + ownProductionCode + " should only contain digits";
            log.error(errorMessage);
            throw new InvalidArgumentServiceException(errorMessage);
        }
    }

    /**
     * Validates that idValue it not too long in the given {@link RestrictedIdInputDto}.
     *
     * @param idValue to be validated
     */
    public void validateIdValueLength(String idValue) {
        if (idValue.length() > MAX_ID_VALUE_LENGTH) {
            final String errorMessage = "idValue was too long and cannot be added to rights module. Only " + MAX_ID_VALUE_LENGTH + " characters are allowed";
            log.error(errorMessage);
            throw new InvalidArgumentServiceException(errorMessage);
        }
    }

    /**
     * Validates that title is not null, empty or not blank and not too long in the given {@link RestrictedIdInputDto}.
     *
     * @param title to be validated
     */
    public void validateTitle(String title) {
        if (StringUtils.isBlank(title)) {
            final String errorMessage = "Title cannot be empty";
            log.error(errorMessage);
            throw new InvalidArgumentServiceException(errorMessage);
        }

        if (title.length() > MAX_TITLE_LENGTH) {
            final String errorMessage = "Title was too long and cannot be added to rights module. Only " + MAX_TITLE_LENGTH + " characters are allowed";
            log.error(errorMessage);
            throw new InvalidArgumentServiceException(errorMessage);
        }
    }

    /**
     * Validates that comment is not null, empty or not blank and not too long in the given {@link RestrictedIdInputDto}.
     *
     * @param comment to be validated
     */
    public void validateComment(String comment) {
        if (StringUtils.isBlank(comment)) {
            final String errorMessage = "Comment cannot be empty";
            log.error(errorMessage);
            throw new InvalidArgumentServiceException(errorMessage);
        }

        if (comment.length() > MAX_COMMENT_LENGTH) {
            final String errorMessage = "Comment was too long and cannot be added to rights module. Only " + MAX_COMMENT_LENGTH + " characters are allowed";
            log.error(errorMessage);
            throw new InvalidArgumentServiceException(errorMessage);
        }
    }
}
