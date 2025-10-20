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
    private static final int MAX_COMMENT_LENGTH = 16348;

    /**
     * Check if dsId starts with "ds.tv:oai:io:" or "ds.radio:oai:io:"
     * It does not check UUID part of the dsId because we don't control it, and it could change later, and then we don't
     * want the validation to fail.
     *
     * @param dsId
     */
    public void validateDsId(String dsId) {
        if (!StringUtils.startsWith(dsId, dsTv) && !StringUtils.startsWith(dsId, dsRadio)) {
            final String errorMessage = "Invalid dsId: " + dsId;
            log.error(errorMessage);
            throw new InvalidArgumentServiceException(errorMessage);
        }
    }

    /**
     * Validates DR production id in the given {@link RestrictedIdInputDto}.
     *
     * @param drProductionId the {@link RestrictedIdInputDto} containing the production ID to be validated.
     */
    public void validateDrProductionIdFormat(String drProductionId) {
        if (!drProductionId.matches("\\d+")) {
            final String errorMessage = "The input DR production ID: " + drProductionId + " should only contain digits";
            log.error(errorMessage);
            throw new InvalidArgumentServiceException(errorMessage);
        }

        if (drProductionId.length() <= 7) {
            final String errorMessage = "The input DR production ID: " + drProductionId + " should be at least 8 digits";
            log.error(errorMessage);
            throw new InvalidArgumentServiceException(errorMessage);
        }
    }

    /**
     * Validates that title is not null, empty or not blank
     *
     * @param title
     */
    public void validateTitle(String title) {
        if (StringUtils.isBlank(title)) {
            final String errorMessage = "Title cannot be empty";
            log.error(errorMessage);
            throw new InvalidArgumentServiceException(errorMessage);
        }
    }

    /**
     * Validates that comment is not null, empty or not blank and comment is not too long
     *
     * @param comment
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
