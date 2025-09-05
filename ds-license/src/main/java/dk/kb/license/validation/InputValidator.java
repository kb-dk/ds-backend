package dk.kb.license.validation;

import org.apache.commons.lang3.StringUtils;

public class InputValidator {
    final String dsTv = "ds.tv:oai:io:";
    final String dsRadio = "ds.radio:oai:io:";

    /**
     * Check if dsId starts with "ds.tv:oai:io:" or "ds.radio:oai:io:"
     * It does not check UUID part of the dsId because we don't control it, and it could change later, and then we don't
     * want the validation to fail.
     *
     * @param dsId
     * @return boolean true or false
     */
    public boolean validateDsId(String dsId) {
        if (StringUtils.startsWith(dsId, dsTv) || StringUtils.startsWith(dsId, dsRadio)) {
            return true;
        }

        return false;
    }
}
