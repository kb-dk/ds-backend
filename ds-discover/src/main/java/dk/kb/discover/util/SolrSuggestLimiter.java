package dk.kb.discover.util;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.kb.discover.SolrService;
import dk.kb.discover.util.responses.suggest.SuggestResponse;
import dk.kb.discover.util.responses.suggest.SuggestResponseBody;
import dk.kb.discover.util.responses.suggest.SuggestionObject;
import dk.kb.discover.util.responses.suggest.SuggestionObjectList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ForkJoinPool;


public class SolrSuggestLimiter {
    private static final Logger log = LoggerFactory.getLogger(SolrSuggestLimiter.class);


    public static SuggestResponse limit(SolrService solr, String rawSuggestBody, ObjectMapper objectMapper, String suggestQuery, int suggestCount, String wt) throws JsonProcessingException {
        long methodStartTime = System.currentTimeMillis();
        SuggestResponse originalSuggestResponse = objectMapper
                .readValue(rawSuggestBody, SuggestResponse.class);
        SuggestionObjectList originalSuggestions = originalSuggestResponse.getSuggest().getRadioTvTitleSuggest().getSuggestQueryObject().get(suggestQuery);

        ForkJoinPool threadPool = new ForkJoinPool(5);
        SolrQueryRecursiveTask recursiveQueryTask = new SolrQueryRecursiveTask(suggestCount, originalSuggestions.getSuggestions(), solr, wt);
        List<SuggestionObject> filteredSuggestions = threadPool.invoke(recursiveQueryTask);

        SuggestResponse filteredSuggestResponse = new SuggestResponse();
        SuggestResponseBody suggestResponseBody = new SuggestResponseBody();

        suggestResponseBody.setRadioTvTitleSuggest(originalSuggestResponse.getSuggest().getRadioTvTitleSuggest());
        suggestResponseBody.getRadioTvTitleSuggest().getSuggestQueryObject().get(suggestQuery).deleteAllSuggestions();

        for (SuggestionObject suggestion : filteredSuggestions){
            suggestResponseBody.getRadioTvTitleSuggest().getSuggestQueryObject().get(suggestQuery).addSuggestion(suggestion);
        }
        suggestResponseBody.getRadioTvTitleSuggest().getSuggestQueryObject().get(suggestQuery).setNumFound(filteredSuggestions.size());
        filteredSuggestResponse.setResponseHeader(originalSuggestResponse.getResponseHeader());
        filteredSuggestResponse.setSuggest(suggestResponseBody);

        threadPool.shutdown();

        log.info("Limiting of suggest result took '{}' ms", System.currentTimeMillis() - methodStartTime);
        return filteredSuggestResponse;
    }
}
