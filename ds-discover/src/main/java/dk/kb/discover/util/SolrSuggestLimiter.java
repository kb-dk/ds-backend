package dk.kb.discover.util;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.kb.discover.SolrQueryRecursiveTask;
import dk.kb.discover.SolrService;
import dk.kb.discover.util.responses.suggest.SuggestResponse;
import dk.kb.discover.util.responses.suggest.SuggestResponseBody;
import dk.kb.discover.util.responses.suggest.SuggestionObject;
import dk.kb.discover.util.responses.suggest.SuggestionObjectList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ForkJoinPool;

/**
 * Component used to filter results from solr suggest handler on filters created in ds license.
 * The component takes a raw suggest response body, extracts the terms returned then performs a select query for these results, validates if the suggested record can be seen by
 * the user and only returns valid suggestions to the end user.
 */
public class SolrSuggestLimiter {
    private static final Logger log = LoggerFactory.getLogger(SolrSuggestLimiter.class);


    /**
     * Method used to limit suggest response to only contain allowed suggestions.
     * @param solr client used to perform solr requests.
     * @param rawSuggestBody the raw suggest response returned from the initial solr suggest request.
     * @param objectMapper used to map solr response to and from java objects.
     * @param suggestQuery initially performed by the caller.
     * @param suggestCount amount of suggestions requested initially.
     * @param wt param for choosing solr response writer
     * @return a filtered solr SuggestResponse, only containing suggestions, that the user actually can see.
     * @throws JsonProcessingException when the solr response cannot be parsed to POJO and vice versa.
     */
    public static SuggestResponse limit(SolrService solr, String rawSuggestBody, ObjectMapper objectMapper, String suggestQuery, int suggestCount, String wt)
            throws JsonProcessingException {

        long methodStartTime = System.currentTimeMillis();

        SuggestResponse originalSuggestResponse = objectMapper.readValue(rawSuggestBody, SuggestResponse.class);
        SuggestionObjectList originalSuggestions = originalSuggestResponse.getSuggest().getRadioTvTitleSuggest().getSuggestQueryObject().get(suggestQuery);

        List<SuggestionObject> filteredSuggestions = getFilteredSuggestionObjects(solr, suggestCount, wt, originalSuggestions);

        SuggestResponse filteredSuggestResponse = constructFilteredSuggestResponse(suggestQuery, originalSuggestResponse, filteredSuggestions);
        log.info("Limiting of suggest result took '{}' ms", System.currentTimeMillis() - methodStartTime);
        return filteredSuggestResponse;
    }


    /**
     * Perform parallelized requests to solr for each suggestion in the original suggest object list.
     * @param solr client to query through.
     * @param suggestCount amount of suggestions to validate.
     * @param wt param for choosing solr response writer
     * @param originalSuggestions containing all suggestions from original call to the suggest component.
     * @return a list of {@link SuggestionObject}s that are all viewable for the caller.
     */
    private static List<SuggestionObject> getFilteredSuggestionObjects(SolrService solr, int suggestCount, String wt, SuggestionObjectList originalSuggestions) {
        ForkJoinPool threadPool = new ForkJoinPool(5);
        SolrQueryRecursiveTask recursiveQueryTask = new SolrQueryRecursiveTask(suggestCount, originalSuggestions.getSuggestions(), solr, wt);
        List<SuggestionObject> filteredSuggestions = threadPool.invoke(recursiveQueryTask);
        threadPool.shutdown();

        return filteredSuggestions;
    }

    /**
     * From an original SuggestResponse construct a response object containing only suggestions, that the end user can actually access after ds-license filtering has been done.
     * @param suggestQuery original query used for the initial suggest request. Used for retrieving and setting of the correct part of the JSON.
     * @param originalSuggestResponse used for extracting the correct JSON structure for the suggest component in use.
     * @param filteredSuggestions a list of allowed {@link SuggestionObject}s that are to be delivered as suggestions for the returned {@link SuggestResponse}.
     * @return A {@link SuggestResponse} consisting of the header from the original suggest response, with the amount of suggestions updated and containing only the suggestions
     * that are part of the {@code filteredSuggestions}.
     */
    private static SuggestResponse constructFilteredSuggestResponse(String suggestQuery, SuggestResponse originalSuggestResponse, List<SuggestionObject> filteredSuggestions) {
        long objectManipulationStartTime = System.currentTimeMillis();
        // Create empty objects that are to be populated.
        SuggestResponse filteredSuggestResponse = new SuggestResponse();
        SuggestResponseBody suggestResponseBody = new SuggestResponseBody();

        // Set RadioTv title suggest in the object, but delete all original suggestions.
        suggestResponseBody.setRadioTvTitleSuggest(originalSuggestResponse.getSuggest().getRadioTvTitleSuggest());
        suggestResponseBody.getRadioTvTitleSuggest().getSuggestQueryObject().get(suggestQuery).deleteAllSuggestions();

        // Add each filtered suggestion to the constructed suggestResponseBody.
        for (SuggestionObject suggestion : filteredSuggestions){
            suggestResponseBody.getRadioTvTitleSuggest().getSuggestQueryObject().get(suggestQuery).addSuggestion(suggestion);
        }

        // Set numFound and header from original response and add filtered body as body.
        suggestResponseBody.getRadioTvTitleSuggest().getSuggestQueryObject().get(suggestQuery).setNumFound(filteredSuggestions.size());
        filteredSuggestResponse.setResponseHeader(originalSuggestResponse.getResponseHeader());
        filteredSuggestResponse.setSuggest(suggestResponseBody);

        log.info("Moving values between objects took '{}' ms", System.currentTimeMillis() - objectManipulationStartTime);
        return filteredSuggestResponse;
    }
}
