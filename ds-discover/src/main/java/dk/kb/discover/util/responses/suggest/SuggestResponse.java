package dk.kb.discover.util.responses.suggest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.kb.discover.util.responses.header.ResponseHeader;

/**
 * A complete response from solr suggest component on the format:
 * <pre>
 * {
 *   "responseHeader":{
 *     "zkConnected":true,
 *     "status":0,
 *     "QTime":6
 *   },
 *   "suggest":{
 *     "radiotv_title_suggest":{
 *       "test":{
 *         "numFound":5,
 *         "suggestions":[{
 *           "term":"Testen: Det usikre internet",
 *           "weight":10,
 *           "payload":""
 *         },{
 *           "term":"Testen: De ulovlige slagtere",
 *           "weight":10,
 *           "payload":""
 *         }, ... ]
 *        }
 *       }
 *      }
 *     }
 * </pre>
 */
@JsonPropertyOrder({"responseHeader", "suggest"})
public class SuggestResponse {

    @JsonProperty("responseHeader")
    ResponseHeader responseHeader;

    @JsonProperty("suggest")
    SuggestResponseBody suggestResponseBody;

    public ResponseHeader getResponseHeader() {
        return responseHeader;
    }

    public void setResponseHeader(ResponseHeader responseHeader) {
        this.responseHeader = responseHeader;
    }

    public SuggestResponseBody getSuggest() {
        return suggestResponseBody;
    }

    public void setSuggest(SuggestResponseBody suggestResponseBody) {
        this.suggestResponseBody = suggestResponseBody;
    }

    @Override
    public String toString() {
        return "SuggestResponse{" +
                "responseHeader=" + responseHeader +
                ", suggestResponseBody=" + suggestResponseBody +
                '}';
    }
}
