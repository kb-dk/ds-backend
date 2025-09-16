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
            log.error("Invalid DS_ID: '{}'", dsId);
            throw new InvalidArgumentServiceException("Invalid dsId");
        }
    }

    public void validateCommentLength(String comment) {
        if (comment != null && comment.length() > MAX_COMMENT_LENGTH) {
            log.error("Comment was too long and cannot be added to rights module. Only {} characters are allowed.", MAX_COMMENT_LENGTH);
            throw new InvalidArgumentServiceException("Comment was too long and cannot be added to rights module. Only " + MAX_COMMENT_LENGTH + " characters are allowed.");
        }
    }

    /**
     * Validates production ID in the given {@link RestrictedIdInputDto}.
     *
     * @param productionId the {@link RestrictedIdInputDto} containing the production ID to be validated.
     */
    public void validateDrProductionIdFormat(String productionId) {
        // Check for null because .matches else throw a NullPointerException
        if (productionId == null) {
            log.error("The input production ID must not be null");
            throw new InvalidArgumentServiceException("The input production ID must not be null");
        }

        if (!productionId.matches("\\d+")) {
            log.error("The input production ID: '{}' should only contain digits", productionId);
            throw new InvalidArgumentServiceException("The input production ID: '" + productionId + "' should only contain digits");
        }

        if (productionId.length() <= 7) {
            log.error("The input production ID must not be null");
            log.error("The input production ID: '{}' should be at least 8 digits", productionId);
            throw new InvalidArgumentServiceException("The input production ID: '" + productionId + "' should be at least 8 digits");
        }
    }
}
