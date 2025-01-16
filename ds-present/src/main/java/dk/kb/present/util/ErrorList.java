package dk.kb.present.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.kb.util.webservice.exception.InternalServiceException;

import java.util.ArrayList;
import java.util.List;

/**
 * An object representing a list of {@link ErrorRecord}s.
 * The list is used to collect information on failing transformations in the {@link dk.kb.present.View}-class.
 */
public class ErrorList {

    public List<ErrorRecord> errors = new ArrayList<>();

    public List<ErrorRecord> getErrors() {
        return this.errors;
    }

    public void setErrors(List<ErrorRecord> errors) {
        this.errors = errors;
    }

    public void addErrorToList(ErrorRecord error) {
        this.errors.add(error);
    }

    public void clearErrors() {
        this.errors.clear();
    }

    public long size(){
        return this.errors.size();
    }

    /**
     * Get an overview of entries in the list. This object contains the count of entries in the list and all the failed records.
     * This returns a JSON structure on the format:
     * <pre>
     * {
     *   "recordsWithErrors":{
     *     "amount": 0,
     *     "records": [
     *       {
     *         "id": "record1",
     *         "error": "description of error"
     *       },
     *       {
     *         "id": "record...N",
     *         "error": "description of error"
     *       },
     *       ...
     *     ]
     *   }
     * }
     * </pre>
     * @return a JSON formatted overview of the {@link #errors} in the list.
     */
    public String getOverview(){
        ObjectMapper mapper = new ObjectMapper();

        ObjectNode outerObject = mapper.createObjectNode();
        ObjectNode innerObject = mapper.createObjectNode();
        ArrayNode errorsArray = mapper.createArrayNode();

        for (ErrorRecord error : this.errors) {
            ObjectNode errorObject = mapper.createObjectNode();
            errorObject.put("id", error.getId());
            errorObject.put("errorMessage", error.getErrorMessage());
            errorsArray.add(errorObject);
        }

        innerObject.put("amount", size());
        innerObject.set("records", errorsArray);
        outerObject.set("recordsWithErrors", innerObject);

        try {
            return mapper.writeValueAsString(outerObject);
        } catch (JsonProcessingException e) {
            throw new InternalServiceException(e);
        }
    }


}
