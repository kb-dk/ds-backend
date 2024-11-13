package dk.kb.discover.util.responses.select;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.kb.discover.util.responses.header.ResponseHeader;
import dk.kb.discover.util.responses.suggest.SuggestResponseBody;

import java.util.HashMap;
import java.util.Map;

public class SelectResponse {
    @JsonProperty("responseHeader")
    ResponseHeader responseHeader;

    @JsonProperty("response")
    EmptySelectResponseBody response;

    public ResponseHeader getResponseHeader() {
        return responseHeader;
    }

    public void setResponseHeader(ResponseHeader responseHeader) {
        this.responseHeader = responseHeader;
    }

    public EmptySelectResponseBody getResponse() {
        return response;
    }

    public void setResponse(EmptySelectResponseBody response) {
        this.response = response;
    }

    public Long getNumFound(){
        return response.getNumFound();
    }
}
