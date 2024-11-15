package dk.kb.discover.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.kb.discover.SolrService;
import dk.kb.discover.util.responses.select.SelectResponse;
import dk.kb.discover.util.responses.suggest.SuggestionObject;
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


    private final long workLoad;

    private final List<SuggestionObject> originalSuggestions;

    private final List<SuggestionObject> filteredSuggestions = new ArrayList<>();

    private static List<String> accessFilter = new ArrayList<>();

    SolrService solr;
    private final String wt;

    public SolrQueryRecursiveTask(long workLoad, List<SuggestionObject> suggestionList, SolrService solr, String wt) {
        this.workLoad = workLoad;
        this.originalSuggestions = suggestionList;
        this.solr = solr;
        this.wt = wt;
    }

    public SolrQueryRecursiveTask(long workLoad, List<SuggestionObject> suggestionList, SolrService solr, String wt, List<String> accessFilter) {
        this.workLoad = workLoad;
        this.originalSuggestions = suggestionList;
        this.solr = solr;
        this.wt = wt;
        SolrQueryRecursiveTask.accessFilter = accessFilter;
    }



    @Override
    protected List<SuggestionObject> compute() {
        if (accessFilter.isEmpty()) {
            log.debug("Creating access filter for suggest queries");
            accessFilter = solr.createAccessFilter("");
        }

        if (workLoad > 1) {
            log.debug("Splitting workLoad: {}", workLoad);

            long workLoad1 = this.workLoad / 2;
            long workLoad2 = this.workLoad - workLoad1;

            int halfList = originalSuggestions.size() / 2;

            SolrQueryRecursiveTask subtask1 = new SolrQueryRecursiveTask(workLoad1, originalSuggestions.subList(0, halfList), solr, wt, accessFilter);
            SolrQueryRecursiveTask subtask2 = new SolrQueryRecursiveTask(workLoad2, originalSuggestions.subList(halfList, originalSuggestions.size()), solr, wt, accessFilter);

            subtask1.fork();
            subtask2.fork();

            filteredSuggestions.addAll(subtask1.join());
            filteredSuggestions.addAll(subtask2.join());
            log.debug("filtered suggestions now contain: '{}'", filteredSuggestions);

        } else {
            log.debug("Doing workLoad: {}", workLoad);
            for (SuggestionObject suggestion : originalSuggestions) {
                String title = suggestion.getTerm();
                String titleQuery = "title:\"" + title + "\"";

                SolrParamMerger merger = solr.createBaseParams(SELECT, titleQuery, accessFilter, 0, null, null, null, wt);
                merger.put(FACET, false);
                merger.put(SPELLCHECK, false);
                merger.put("hl", false);


                URI uri = solr.createRequest(SELECT, merger);
                String singleResult = solr.performCall(titleQuery, uri, "search");
                SelectResponse singleResponse = null;
                try {
                    singleResponse = SolrService.objectMapper.readValue(singleResult, SelectResponse.class);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }

                if (singleResponse.getNumFound() > 0) {
                    log.debug("Added record with title '{}' to filtered suggestions", title);
                    filteredSuggestions.add(suggestion);
                } else {
                    log.debug("Record with title '{}' can not be shown in suggestions.", title);
                }
            }

        }
        return filteredSuggestions;
    }
}
