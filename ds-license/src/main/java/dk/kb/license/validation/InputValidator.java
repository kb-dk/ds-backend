package dk.kb.license.validation;

import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InputValidator {
    private static final Logger log = LoggerFactory.getLogger(InputValidator.class);

    final String dsTv = "ds.tv:oai:io:";
    final String dsRadio = "ds.radio:oai:io:";

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

    }
}
