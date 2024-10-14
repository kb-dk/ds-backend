package dk.kb.discover.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ErrorMessageHandlerTest {

    @Test
    public void solrErrorExtractionTest(){
        String testResponse = "{\"responseHeader\":{\"status\":400,\"QTime\":6},\"error\":{\"msg\":\"This is a valid solr error response\",\"code\":500}}";
        String errorMsg = ErrorMessageHandler.getErrorMsgFromSolrResponse(testResponse);
        assertEquals("This is a valid solr error response", errorMsg);
    }
}
