package dk.kb.discover;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.kb.discover.util.SolrParamMerger;
import dk.kb.discover.util.responses.select.SelectResponse;
import dk.kb.discover.util.responses.suggest.SuggestionObject;
import dk.kb.util.webservice.exception.InternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveTask;

import static dk.kb.discover.SolrService.FACET;
import static dk.kb.discover.SolrService.SELECT;
import static dk.kb.discover.SolrService.SPELLCHECK;

public class SolrQueryRecursiveTask extends RecursiveTask<List<SuggestionObject>> {
    private static final Logger log = LoggerFactory.getLogger(SolrQueryRecursiveTask.class);


    /**
     * Workload defines a value used to specify, when tasks are to be divided to more than a single thread in the ForkJoinPool. For this recursive task, each thread should be
     * doing a call to solr when possible, therefore the logic in the {@link #compute()}-method splits into multiple tasks as long as the workLoad is above 1.
     */
    private final long workLoad;

    private final long requestedAmountOfSuggestions;

    private final List<SuggestionObject> originalSuggestions;

    private final List<SuggestionObject> filteredSuggestions = new ArrayList<>();

    private static List<String> accessFilter = new ArrayList<>();

    private final SolrService solr;
    private final String wt;

    /**
     * Use this constructor when initializing the method for the first time if you have no accessFilter in hand.
     * @param workLoad the amount of work to do. Typically, the same amount as the level of parallelism in the {@link java.util.concurrent.ForkJoinPool}, which the class is
     *                 invoked in.
     * @param suggestionList a list of {@link SuggestionObject}s containing the original suggestions from a solr suggest request, which are to be filtered for access.
     * @param solr client used to perform queries to solr.
     * @param wt solr response writer to use.
     */
    public SolrQueryRecursiveTask(long workLoad, List<SuggestionObject> suggestionList, SolrService solr, String wt) {
        this.workLoad = workLoad;
        this.requestedAmountOfSuggestions = workLoad;
        this.originalSuggestions = suggestionList;
        this.solr = solr;
        this.wt = wt;
    }

    /**
     * Use this constructor when initializing the method for the first time if you have no accessFilter in hand.
     *
     * @param workLoad                     the amount of work to do. Typically, the same amount as the level of parallelism in the {@link java.util.concurrent.ForkJoinPool}, which the class is
     *                                     invoked in.
     * @param suggestionList               a list of {@link SuggestionObject}s containing the original suggestions from a solr suggest request, which are to be filtered for access.
     * @param solr                         client used to perform queries to solr.
     * @param wt                           solr response writer to use.
     * @param accessFilter                 an already present access filter created in DS License.
     * @param requestedAmountOfSuggestions amount of suggestions to aim at returning.
     */
    public SolrQueryRecursiveTask(long workLoad, List<SuggestionObject> suggestionList, SolrService solr, String wt, List<String> accessFilter, Long requestedAmountOfSuggestions) {
        this.workLoad = workLoad;
        this.requestedAmountOfSuggestions = requestedAmountOfSuggestions;
        this.originalSuggestions = suggestionList;
        this.solr = solr;
        this.wt = wt;
        SolrQueryRecursiveTask.accessFilter = accessFilter;
    }


    /**
     * If workload is above 1, then the compute method divides the work into smaller chunks recursively until the workload has the wanted size. When the wanted workload has been
     * obtained, the method calls solr and validates that the there are results, which are allowed for the term being suggested. If the title is allowed, then the suggestion is
     * added to the object {@link #filteredSuggestions} and are returned,
     * @return a list of filtered suggestions, which are all allowed.
     */
    @Override
    protected List<SuggestionObject> compute() {
        if (accessFilter.isEmpty()) {
            log.debug("Creating access filter for suggest queries");
            long getAccessFilterStartTime = System.currentTimeMillis();
            accessFilter = solr.createAccessFilter("");

            log.info("Created access filter for suggest queries in '{}' ms", System.currentTimeMillis() - getAccessFilterStartTime);
        }

        if (workLoad > 1) {
            divideWorkload();
        } else {
            performWork();
        }

        return filteredSuggestions;
    }

    /**
     * Divide workload to subtasks.
     */
    private void divideWorkload() {
        // Divide workload between threads.
        log.debug("Splitting workLoad '{}' into two sub tasks.", workLoad);

        long workLoad1 = this.workLoad / 2;
        long workLoad2 = this.workLoad - workLoad1;
        int halfList = originalSuggestions.size() / 2;

        // Give half the work to each subtask
        SolrQueryRecursiveTask subtask1 = new SolrQueryRecursiveTask(workLoad1, originalSuggestions.subList(0, halfList), solr, wt, accessFilter, workLoad);
        SolrQueryRecursiveTask subtask2 = new SolrQueryRecursiveTask(workLoad2, originalSuggestions.subList(halfList, originalSuggestions.size()), solr, wt, accessFilter, workLoad);

        subtask1.fork();
        subtask2.fork();

        // When subtasks have finished, add the result to the overall filteredSuggestions.
        filteredSuggestions.addAll(subtask1.join());
        filteredSuggestions.addAll(subtask2.join());
    }

    /**
     * Perform the work requested.
     */
    private void performWork() {
        if (filteredSuggestions.size() >= requestedAmountOfSuggestions){
            return;
        }

        long requestStartTime = System.currentTimeMillis();
        log.debug("Doing workLoad: {}", workLoad);
        for (SuggestionObject suggestion : originalSuggestions) {
            // Construct title query
            String title = suggestion.getTerm();
            String titleQuery = "title:\"" + title + "\"";

            // Get solr response for query
            SelectResponse singleResponse = getMinimalSelectResponse(titleQuery);

            // If response has documents, then add response to filteredSuggestions.
            if (singleResponse.getNumFound() > 0 && filteredSuggestions.size() < workLoad) {
                filteredSuggestions.add(suggestion);
            } else {
                log.debug("Record with title '{}' can not be shown in suggestions.", title);
            }

            log.debug("Single SELECT request for suggestion took '{}' ms", System.currentTimeMillis() - requestStartTime);
        }
    }

    /**
     * Query solr select handler with an accessFilter from DS License, but with no facets, no spellchecking, no documents and no highlighting.
     * This response can be used to check if any documents where found for the given query and accessFilter.
     * @param query to perform lookup for.
     * @return a minimal solr select response.
     */
    private SelectResponse getMinimalSelectResponse(String query) {
        SolrParamMerger merger = solr.createBaseParams(SELECT, query, accessFilter, 0, null, "id", null, wt);
        merger.put(FACET, false);
        merger.put(SPELLCHECK, false);
        merger.put("hl", false);

        URI uri = solr.createRequest(SELECT, merger);
        String singleResult = solr.performCall(query, uri, "search");
        SelectResponse singleResponse;

        try {
            singleResponse = SolrService.objectMapper.readValue(singleResult, SelectResponse.class);
        } catch (JsonProcessingException e) {
            throw new InternalServiceException("An error occurred when processing the solr response into a SelectResponse object: ", e);
        }

        return singleResponse;
    }
}
