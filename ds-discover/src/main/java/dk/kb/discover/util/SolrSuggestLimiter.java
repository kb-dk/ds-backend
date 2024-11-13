package dk.kb.discover.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.kb.discover.SolrManager;
import dk.kb.discover.SolrService;
import dk.kb.discover.util.responses.suggest.SuggestResponse;
import dk.kb.discover.util.responses.suggest.SuggestResponseBody;
import dk.kb.discover.util.responses.suggest.SuggestionObject;
import dk.kb.discover.util.responses.suggest.SuggestionObjectList;

public class SolrSuggestLimiter {
    public static String filterSuggestResponse(String suggestResponseString, String suggestQuery, String solrCollection) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        SuggestResponse originalSuggestResponse = objectMapper.readValue(suggestResponseString, SuggestResponse.class);

        SuggestResponse filteredSuggestResponse = createFilteredSuggestResponse(originalSuggestResponse, suggestQuery, solrCollection);

        String jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(filteredSuggestResponse);
        return jsonString;
    }

    private static SuggestResponse createFilteredSuggestResponse(SuggestResponse originalSuggestResponse, String suggestQuery, String solrCollection) {
        SuggestResponse filteredSuggestResponse = new SuggestResponse();
        filteredSuggestResponse.setResponseHeader(originalSuggestResponse.getResponseHeader());

        SuggestionObjectList suggestions = originalSuggestResponse.getSuggest().getRadioTvTitleSuggest().getSuggestQueryObject().get(suggestQuery);

        for (SuggestionObject suggestion : suggestions.getSuggestions()){
            String title = suggestion.getTerm();
            String titleQuery = "title:\"" + title + "\"";

        }


        // TODO: Deliver the filtered result.
        SuggestResponseBody suggestResponseBody = new SuggestResponseBody();
        suggestResponseBody.setRadioTvTitleSuggest(originalSuggestResponse.getSuggest().getRadioTvTitleSuggest());


        filteredSuggestResponse.setSuggest(suggestResponseBody);

        return filteredSuggestResponse;
    }
}
