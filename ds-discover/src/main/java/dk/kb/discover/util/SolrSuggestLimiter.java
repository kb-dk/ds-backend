package dk.kb.discover.util;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.kb.discover.SolrService;
import dk.kb.discover.util.responses.select.SelectResponse;
import dk.kb.discover.util.responses.suggest.SuggestResponse;
import dk.kb.discover.util.responses.suggest.SuggestResponseBody;
import dk.kb.discover.util.responses.suggest.SuggestionObject;
import dk.kb.discover.util.responses.suggest.SuggestionObjectList;
import dk.kb.util.webservice.exception.InternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static dk.kb.discover.SolrService.FACET;
import static dk.kb.discover.SolrService.FACET_FIELD;
import static dk.kb.discover.SolrService.SELECT;
import static dk.kb.discover.SolrService.SPELLCHECK;

/**
 * Component used to filter results from solr suggest handler on filters created in ds license.
 * The component takes a raw suggest response body, extracts the terms returned then performs a select query for these results, validates if the suggested record can be seen by
 * the user and only returns valid suggestions to the end user.
 */
public class SolrSuggestLimiter {
    private static final Logger log = LoggerFactory.getLogger(SolrSuggestLimiter.class);

    private static List<String> accessFilter = new ArrayList<>();

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

        String combinedQueryString = createQueryStringFromSuggestions(originalSuggestions);
        log.info("Combined query is: {}", combinedQueryString);
        SelectResponse result = getMinimalSelectResponse(combinedQueryString, solr, wt);

        Map<String, Integer> allowedQueries = createSuggestResponseFromFacets(result);
        SuggestResponse filteredSuggestResponse = constructFilteredSuggestResponseFromFacets(suggestQuery, originalSuggestResponse, allowedQueries);


        /*List<SuggestionObject> filteredSuggestions = getFilteredSuggestionObjects(solr, suggestCount, wt, originalSuggestions);
        SuggestResponse filteredSuggestResponse = constructFilteredSuggestResponse(suggestQuery, originalSuggestResponse, filteredSuggestions);*/
        log.info("Limiting of suggest result took '{}' ms", System.currentTimeMillis() - methodStartTime);
        return filteredSuggestResponse;
    }

    /**
     * From a solr select response with facet results, get the first entry from the facet fields object and convert these to a map of value, amount.
     * @param response from solr, which is parsed for facet values.
     * @return a map containing facet values and hits from the input solr response.
     */
    private static Map<String, Integer> createSuggestResponseFromFacets(SelectResponse response) {
        List<Object> initialList = response.getFacetCounts().getFacetFields().values().stream().findFirst().get();
        Map<String, Integer> facetPairs = new LinkedHashMap<>();

        // Converting list to map
        for (int i = 0; i < initialList.size(); i += 2) {
            String key = (String) initialList.get(i); // The string key
            Integer value = (Integer) initialList.get(i + 1); // The integer value
            facetPairs.put(key, value);
        }

        return facetPairs;
    }

    private static String createQueryStringFromSuggestions(SuggestionObjectList originalSuggestions) {
        StringJoiner titleJoiner = new StringJoiner("\" OR \"", "(\"", "\")");
        String queryField = "title_strict:";
        for (SuggestionObject suggestion : originalSuggestions.getSuggestions()) {
            titleJoiner.add(suggestion.getTerm());
        }

        return queryField + titleJoiner;
    }

    /**
     * From an original SuggestResponse construct a response object containing only suggestions, that the end user can actually access after ds-license filtering has been done.
     * @param suggestQuery original query used for the initial suggest request. Used for retrieving and setting of the correct part of the JSON.
     * @param originalSuggestResponse used for extracting the correct JSON structure for the suggest component in use.
     * @param facetResults a map of allowed results that are to be delivered as suggestions for the returned {@link SuggestResponse}.
     * @return A {@link SuggestResponse} consisting of the header from the original suggest response, with the amount of suggestions updated and containing only the suggestions
     * that are part of the {@code filteredSuggestions}.
     */
    private static SuggestResponse constructFilteredSuggestResponseFromFacets(String suggestQuery, SuggestResponse originalSuggestResponse,
                                                                          Map<String, Integer> facetResults) {
        long objectManipulationStartTime = System.currentTimeMillis();
        // Create empty objects that are to be populated.
        SuggestResponse filteredSuggestResponse = new SuggestResponse();
        SuggestResponseBody suggestResponseBody = new SuggestResponseBody();

        // Set RadioTv title suggest in the object, but delete all original suggestions.
        suggestResponseBody.setRadioTvTitleSuggest(originalSuggestResponse.getSuggest().getRadioTvTitleSuggest());
        suggestResponseBody.getRadioTvTitleSuggest().getSuggestQueryObject().get(suggestQuery).deleteAllSuggestions();

        // Add each filtered suggestion to the constructed suggestResponseBody.
        for (Map.Entry<String, Integer> entry : facetResults.entrySet()) {
            SuggestionObject suggestion = new SuggestionObject();
            suggestion.setTerm(entry.getKey());
            suggestion.setWeight(entry.getValue());

            suggestResponseBody.getRadioTvTitleSuggest().getSuggestQueryObject().get(suggestQuery).addSuggestion(suggestion);
        }

        // Set numFound and header from original response and add filtered body as body.
        suggestResponseBody.getRadioTvTitleSuggest().getSuggestQueryObject().get(suggestQuery).setNumFound(facetResults.size());
        filteredSuggestResponse.setResponseHeader(originalSuggestResponse.getResponseHeader());
        filteredSuggestResponse.setSuggest(suggestResponseBody);

        log.info("Moving values between objects took '{}' ms", System.currentTimeMillis() - objectManipulationStartTime);
        return filteredSuggestResponse;
    }


    /**
     * Query solr select handler with an accessFilter from DS License, but with no facets, no spellchecking, no documents and no highlighting.
     * This response can be used to check if any documents where found for the given query and accessFilter.
     * @param query to perform lookup for.
     * @return a minimal solr select response.
     */
    private static SelectResponse getMinimalSelectResponse(String query, SolrService solr, String wt) {
        if (accessFilter.isEmpty()) {
            log.debug("Creating access filter for suggest queries");
            long getAccessFilterStartTime = System.currentTimeMillis();
            accessFilter = solr.createAccessFilter("");

            log.info("Created access filter for suggest queries in '{}' ms", System.currentTimeMillis() - getAccessFilterStartTime);
        }

        String singleResult = getMinimalSolrResponseString(query, solr, wt);
        log.info(singleResult);
        SelectResponse singleResponse;

        try {
            singleResponse = SolrService.objectMapper.readValue(singleResult, SelectResponse.class);
        } catch (JsonProcessingException e) {
            throw new InternalServiceException("An error occurred when processing the solr response into a SelectResponse object: ", e);
        }

        return singleResponse;
    }

    private static String getMinimalSolrResponseString(String query, SolrService solr, String wt) {
        SolrParamMerger merger = solr.createBaseParams(SELECT, query, accessFilter, 0, null, "id", null, wt);
        merger.put(FACET, true);
        merger.put(FACET_FIELD, "title_strict");
        merger.put(SPELLCHECK, false);
        merger.put("hl", false);

        URI uri = solr.createRequest(SELECT, merger);
        return solr.performCall(query, uri, "search");
    }
}
