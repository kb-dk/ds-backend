package dk.kb.discover.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.kb.util.webservice.exception.InternalServiceException;

/**
 * Util class for parsing incoming error messages, so that they can be rethrown as part of the actual error handling in the service
 */
public class ErrorMessageHandler {

    /**
     * Get error message from a standard SolrResponse string.
     * @param solrResponse string to extract error message from.
     * @return the error message from a solr response.
     */
    public static String getErrorMsgFromSolrResponse(String solrResponse){
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = null;
        try {
            rootNode = objectMapper.readTree(solrResponse);
        } catch (JsonProcessingException e) {
            throw new InternalServiceException("An error occurred when extracting Solr Error Message from a SolrResponse. There might not be an error message in the returned " +
                    "SolrResponse. The exception thrown is: ", e);
        }

        // Extract the "error" node
        JsonNode errorNode = rootNode.path("error");
        // Extract the "msg" field from the "error" node
        return errorNode.path("msg").asText();
    }
}
