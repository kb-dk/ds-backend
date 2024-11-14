/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.kb.discover;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.kb.discover.config.ServiceConfig;
import dk.kb.discover.util.ErrorMessageHandler;
import dk.kb.discover.util.SolrParamMerger;
import dk.kb.discover.util.responses.select.SelectResponse;
import dk.kb.discover.util.responses.suggest.SuggestResponse;
import dk.kb.discover.util.responses.suggest.SuggestResponseBody;
import dk.kb.discover.util.responses.suggest.SuggestionObject;
import dk.kb.discover.util.responses.suggest.SuggestionObjectList;
import dk.kb.license.client.v1.DsLicenseApi;
import dk.kb.license.model.v1.GetUserQueryInputDto;
import dk.kb.license.model.v1.GetUsersFilterQueryOutputDto;
import dk.kb.util.other.StringListUtils;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import dk.kb.util.webservice.exception.ServiceException;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static dk.kb.discover.api.v1.impl.DsDiscoverApiServiceImpl.FILTER_CACHE_PREFIX;
import static dk.kb.discover.api.v1.impl.DsDiscoverApiServiceImpl.getDsLicenseApiClient;
import static dk.kb.discover.api.v1.impl.DsDiscoverApiServiceImpl.getLicenseQueryDto;

/**
 * Encapsulates assess to a Solr server.
 */
/*
  20220518: This is an extremely simple wrapper. No throttling, no client-side caching...
 */
public class SolrService {
    private static final Logger log = LoggerFactory.getLogger(SolrService.class);

    public static final String SELECT = "select";
    public static final String SUGGEST = "suggest";
    public static final String MLT = "mlt";
    public static final String SCHEMA = "schema";

    public static final String Q = "q";
    public static final String SUGGEST_Q = "suggest.q";
    public static final String SUGGEST_DICTIONARY = "suggest.dictionary";
    public static final String SUGGEST_COUNT = "suggest.count";
    public static final String FQ = "fq";
    public static final String FL = "fl";
    public static final String ROWS = "rows";
    public static final String START = "start";
    public static final String FACET = "facet";
    public static final String FACET_FIELD = "facet.field";
    public static final String SPELLCHECK = "spellcheck";
    public static final String SPELLCHECK_BUILD = "spellcheck.build";
    public static final String SPELLCHECK_RELOAD = "spellcheck.reload";
    public static final String SPELLCHECK_QUERY = "spellcheck.q";
    public static final String SPELLCHECK_DICTIONARY = "spellcheck.dictionary";
    public static final String SPELLCHECK_COUNT= "spellcheck.count";
    public static final String SPELLCHECK_ONLY_MORE_POPULAR= "spellcheck.onlyMorePopular";
    public static final String SPELLCHECK_EXTENDED_RESULTS= "spellcheck.extendedResults";
    public static final String SPELLCHECK_COLLATE= "spellcheck.collate";
    public static final String SPELLCHECK_MAX_COLLATIONS= "spellcheck.maxCollations";       
    public static final String SPELLCHECK_MAX_COLLATION_TRIES= "spellcheck.maxCollationTries";
    public static final String SPELLCHECK_ACCURACY= "spellcheck.accuracy";
        
    public static final String QOP = "q.op";
    public static final String WT = "wt";
    public static final String VERSION = "version"; // Used with wt=xml, always 2.2, not mandatory
    public static final String INDENT = "indent";
    public static final String DEBUG = "debug";
    public static final String DEBUG_EXPLAIN_STRUCTURED = "debug.explain.structured";

    public static final String MLT_FL = "mlt.fl";
    public static final String MLT_MINTF = "mlt.mintf";
    public static final String MLT_MINDF = "mlt.mindf";
    public static final String MLT_MAXDF = "mlt.maxdf";
    public static final String MLT_MAXDFPCT = "mlt.maxdfpct";
    public static final String MLT_MINWL = "mlt.minwl";
    public static final String MLT_MAXWL = "mlt.maxwl";
    public static final String MLT_MAXQT = "mlt.maxqt";
    public static final String MLT_BOOST = "mlt.boost";
    public static final String MLT_INTERESTING_TERMS = "mlt.interestingTerms";

    private final String id; // Abstract collection

    private final String server;
    private final String path;
    private final String solrCollection;

    /**
     * A list of fqs created by DSLicense.
     */
    private final List<String> plainLicenseFilter = createAccessFilter("");

    private final HttpClient client = HttpClient.newHttpClient();

    // Factories for creating param mergers, handling default- and forced-parameters
    private final SolrParamMerger.Factory selectFactory = new SolrParamMerger.Factory("select");
    private final SolrParamMerger.Factory mltFactory = new SolrParamMerger.Factory("mlt");

    public enum QOP_ENUM {OR, AND;
        static QOP_ENUM safeParse(String qOP) {
            if (qOP == null) {
                return AND;
            }
            try {
                return QOP_ENUM.valueOf(qOP);
            } catch (IllegalArgumentException e) {
                throw new InvalidArgumentServiceException(
                        "Unsupported q.op='" + qOP + "'. Supported values are " + Arrays.toString(QOP_ENUM.values()));
            }
        }
    }
    public enum WT_ENUM {
        json, csv, xml;

        static WT_ENUM safeParse(String wt) {
            if (wt == null || wt.isEmpty()) {
                return json;
            }
            try {
                return WT_ENUM.valueOf(wt);
            } catch (IllegalArgumentException e) {
                throw new InvalidArgumentServiceException(
                        "Unsupported wt='" + wt + "'. Supported values are " + Arrays.toString(WT_ENUM.values()));
            }
        }
        String getMIME() {
            switch (this) {
                case json: return "application/json";
                case xml: return "application/xml";
                case csv: return "text/csv";
                default: throw new UnsupportedOperationException(
                        "The WT '" + this + "' has no MIME type defined");
            }
        }
    }

    public enum WT_SCHEMA_ENUM {
        json("json"),
        xml("xml"),
        schema("schema.xml");

        public final String label;
        WT_SCHEMA_ENUM(String label) {
            this.label = label;
        }
        public String getLabel() {
            return label;
        }

        static WT_SCHEMA_ENUM safeParse(String wt) {
            if (wt == null || wt.isEmpty()) {
                return json;
            }
            for (WT_SCHEMA_ENUM e: values()) {
                if (e.label.equals(wt)) {
                    return e;
                }
            }
            throw new InvalidArgumentServiceException(
                    "Unsupported schema wt='" + wt + "'. Supported values are " +
                            Arrays.stream(WT_SCHEMA_ENUM.values()).
                                    map(WT_SCHEMA_ENUM::getLabel).
                                    collect(Collectors.toList()));
        }
        String getMIME() {
            switch (this) {
                case json: return "application/json";
                case xml: return "application/xml";
                case schema: return "application/xml";
                default: throw new UnsupportedOperationException(
                        "The schema WT '" + this + "' has no MIME type defined");
            }
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum DEBUG_ENUM {query, timing, results, all;
        static DEBUG_ENUM safeParse(String debug) {
            if (debug == null) {
                return null;
            }
            if ("true".equals(debug)) {
                return all;
            }
            try {
                return DEBUG_ENUM.valueOf(debug);
            } catch (IllegalArgumentException e) {
                throw new InvalidArgumentServiceException(
                        "Unsupported debug='" + debug + "'. Supported values are " + Arrays.toString(DEBUG_ENUM.values()));
            }
        }
    }
    public enum MLT_INTERESTING_TERMS_ENUM {list, none, details;
        static MLT_INTERESTING_TERMS_ENUM safeParse(String interestingTerms) {
            if (interestingTerms == null) {
                return null;
            }
            try {
                return MLT_INTERESTING_TERMS_ENUM.valueOf(interestingTerms);
            } catch (IllegalArgumentException e) {
                throw new InvalidArgumentServiceException(
                        "Unsupported mlt.interestingTerms='" + interestingTerms + "'. Supported values are " +
                                Arrays.toString(MLT_INTERESTING_TERMS_ENUM.values()));
            }
        }
    }

    public SolrService(String id, String server, String path, String solrCollection) {
        this.id = id;
        this.server = server;
        this.path = path;
        this.solrCollection = solrCollection;
        log.info("Created " + this);
    }

    /**
     * Issue a Solr More Like This (MLT) request. All parameters from standard Solr.
     * <p>
     * A Solr MLT works internally in Solr by issuing a search for the given query ({code q} with the given
     * filter queries {@code fq}. The significant terms from the first hit from the search are then used for
     * a new search, where the resulting documents are returned. The end result of this indirect search should
     * be documents that are similar in terms (aka content) to the first hit in the original search result.
     * <p>
     * A common way of using MLT is to issue a query for a specific document: {@code q=id:"ds.radiotv:oai:man:123..."}.
     * @see <a href="https://solr.apache.org/guide/solr/latest/query-guide/morelikethis.html">Solr MLT</a>.
     * @return solr More Like This response.
     */
    @SuppressWarnings("SuspiciousTernaryOperatorInVarargsCall")
    public String mlt(String q, List<String> fq, Integer rows, Integer start, String fl, String qOp, String wt,
                      String mltFl, Integer mltMintf, Integer mltMindf, Integer mltMaxdf, Integer mltMaxdfpct,
                      Integer mltMinwl, Integer mltMaxwl, Integer mltMaxqt, Boolean mltBoost,
                      String mltInterestingTerms, Map<String, String[]> extra) {
        if (q == null) {
            throw new InvalidArgumentServiceException("q is mandatory but was missing");
        }
        // TODO: Catch extra arguments and throw "not supported"
        SolrParamMerger merger = createBaseParams(MLT, q, fq, rows, start, fl, qOp, wt);

        merger.put(MLT_FL, mltFl);
        merger.put(MLT_MINTF, mltMintf);
        merger.put(MLT_MINDF, mltMindf);
        merger.put(MLT_MAXDF, mltMaxdf);
        merger.put(MLT_MAXDFPCT, mltMaxdfpct);
        merger.put(MLT_MINWL, mltMinwl);
        merger.put(MLT_MAXWL, mltMaxwl);
        merger.put(MLT_MAXQT, mltMaxqt);
        merger.put(MLT_BOOST, mltBoost);
        merger.put(MLT_INTERESTING_TERMS, MLT_INTERESTING_TERMS_ENUM.safeParse(mltInterestingTerms));
        merger.addAll(extra); // Extras might contain keys that has already been put, so we use addAll

        URI uri = createRequest(MLT, merger);

        return performCall(q, uri, "mlt");
    }

    /**
     * Issue a Solr query and return the result.
     *
     * @param q                      Solr query.
     * @param fq                     Solr filter query.
     * @param rows                   Maximum number of documents or groups to return.
     * @param fl                     Solr field list.
     * @param facet                  Whether to facet.
     * @param facetField             Fields to facet on.
     * @param qOp                    Solr default boolean operator.
     * @param wt                     Solr response format.
     * @param indent                 if true, Solr response is indented (if possible).
     * @param debug                  as enumerated in {@link DEBUG_ENUM}.
     * @param debugExplainStructured true if debug information should be structuredinstead of just a string.
     * @param extra                  optional extra parameters.
     * @return Solr response.
     */
    @SuppressWarnings("SuspiciousTernaryOperatorInVarargsCall")
    public String query(String q,
    		List<String> fq,
    		Integer rows,
    		Integer start,
    		String fl,
    		String facet,
    		List<String> facetField,
    		String spellcheck,
    		String spellcheckBuild,
    		String spellcheckReload,
    		String spellcheckQuery,
    		String spellcheckDictionary,    		                 
            Integer spellcheckCount,
    	    String spellchecKOnlyMorePopular,
    	    String spellcheckExtendedResults,
    	    String spellcheckCollate,
    	    Integer spellcheckMaxCollations,
    	    Integer spellcheckMaxCollationTries,
    	    Double spellcheckAccuracy,
    	    String qOp, String wt, String version, String indent, String debug, String debugExplainStructured,
                        Map<String, String[]> extra) {
        if (q == null) {
            throw new InvalidArgumentServiceException("q is mandatory but was missing");
        }
        // TODO: Catch extra arguments and throw "not supported"
        SolrParamMerger merger = createBaseParams(SELECT, q, fq, rows, start, fl, qOp, wt);

        merger.put(FACET, facet);
        merger.put(FACET_FIELD, facetField);

        merger.put(SPELLCHECK, spellcheck);
        merger.put(SPELLCHECK_BUILD, spellcheckBuild);
        merger.put(SPELLCHECK_RELOAD, spellcheckReload);
        merger.put(SPELLCHECK_QUERY, spellcheckQuery);
        merger.put(SPELLCHECK_DICTIONARY, spellcheckDictionary);
        merger.put(SPELLCHECK_COUNT, spellcheckCount);
        merger.put(SPELLCHECK_ONLY_MORE_POPULAR, spellchecKOnlyMorePopular);
        merger.put(SPELLCHECK_EXTENDED_RESULTS, spellcheckExtendedResults);               
        merger.put(SPELLCHECK_COLLATE, spellcheckCollate);
        merger.put(SPELLCHECK_MAX_COLLATIONS, spellcheckMaxCollations);
        merger.put(SPELLCHECK_MAX_COLLATION_TRIES, spellcheckMaxCollationTries);
        merger.put(SPELLCHECK_ACCURACY, spellcheckAccuracy);
        
        merger.put(VERSION, version);
        merger.put(INDENT, indent);

        merger.put(DEBUG, DEBUG_ENUM.safeParse(debug));
        merger.put(DEBUG_EXPLAIN_STRUCTURED, Boolean.parseBoolean(debugExplainStructured));

        merger.addAll(extra); // Extras might contain keys that has already been put, so we use addAll

        URI uri = createRequest(SELECT, merger);
        return performCall(q, uri, "search");
    }

    /**
     * Issue a Solr suggest and return the result.
     *
     * @param suggestDictionary A suggest dictionary defined in the solr configuration.
     * @param suggestQuery The prefix text that the suggest compontent will try to autocomplete. 
     * @param suggestCount Number of results to return. 10 is the default value.
     * @param wt The return format from solr. (json, xml etc.) 
     * @return Solr response.
     */
    public String suggest(String suggestDictionary, String suggestQuery, Integer suggestCount, String wt) {
        if (suggestQuery == null) {
            throw new InvalidArgumentServiceException("suggestQuery is mandatory but was missing");
        }
        if (suggestDictionary == null) {
            throw new InvalidArgumentServiceException("suggestDictionary is mandatory but was missing");
        }

    	int minimumSuggestLength=ServiceConfig.getConfig().getInteger("solr.suggestMinimumLength");
        if (suggestQuery.trim().length() < minimumSuggestLength ) {
           throw new InvalidArgumentServiceException("suggestQuery must have length >"+ minimumSuggestLength);
        }

        URI suggestURI = createSuggestRequestBuilder(suggestDictionary, suggestQuery, suggestCount, wt);
        String rawSuggestBody = performCall(suggestQuery, suggestURI, "suggest");
        System.out.println("Solr response");
        System.out.println(rawSuggestBody);

        ObjectMapper objectMapper = new ObjectMapper();
        SuggestionObjectList filteredSuggestions = new SuggestionObjectList();
        AtomicInteger allowedSuggestionsCount = new AtomicInteger(0);
        try {
            SuggestResponse originalSuggestResponse = objectMapper.readValue(rawSuggestBody, SuggestResponse.class);
            SuggestionObjectList originalSuggestions = originalSuggestResponse.getSuggest().getRadioTvTitleSuggest().getSuggestQueryObject().get(suggestQuery);

            ForkJoinPool threadPool = new ForkJoinPool(5);

            /*while (allowedSuggestionsCount.get() < 5){
                threadPool.submit(() ->
                        originalSuggestions.getSuggestions().stream()
                                .parallel() // Do the streaming operations in parallel aka make use of the thread pool.
                                .filter(suggestion -> isSuggestAllowed(suggestion, objectMapper, wt))
                                .forEach(suggestion -> addToSuggestResponse(suggestion, filteredSuggestions, allowedSuggestionsCount))
                ).get();
            }*/


            for (SuggestionObject suggestion : originalSuggestions.getSuggestions()) {
                if (allowedSuggestionsCount.get() >= suggestCount){
                    break;
                }

                threadPool.submit(() -> {
                    String title = suggestion.getTerm();
                    String titleQuery = "title:\"" + title + "\"";

                    SolrParamMerger merger = createBaseParams(SELECT, titleQuery, plainLicenseFilter, 1, null, null, null, wt);
                    merger.put(FACET, false);
                    merger.put(SPELLCHECK, false);
                    merger.put("hl", false);


                    URI uri = createRequest(SELECT, merger);
                    String singleResult = performCall(titleQuery, uri, "search");
                    SelectResponse singleResponse = null;
                    try {
                        singleResponse = objectMapper.readValue(singleResult, SelectResponse.class);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }

                    if (singleResponse.getNumFound() > 0) {
                        filteredSuggestions.addSuggestion(suggestion);
                        allowedSuggestionsCount.getAndAdd(1);
                    } else {
                        log.debug("Record with title '{}' can not be shown in suggestions.", title);
                    }
                }).get();
            }


            System.out.println(filteredSuggestions);
            SuggestResponse filteredSuggestResponse = new SuggestResponse();
            SuggestResponseBody suggestResponseBody = new SuggestResponseBody();


            suggestResponseBody.setRadioTvTitleSuggest(originalSuggestResponse.getSuggest().getRadioTvTitleSuggest());
            suggestResponseBody.getRadioTvTitleSuggest().getSuggestQueryObject().get(suggestQuery).setSuggestions(filteredSuggestions.getSuggestions());
            suggestResponseBody.getRadioTvTitleSuggest().getSuggestQueryObject().get(suggestQuery).setNumFound(allowedSuggestionsCount.get());
            filteredSuggestResponse.setResponseHeader(originalSuggestResponse.getResponseHeader());
            filteredSuggestResponse.setSuggest(suggestResponseBody);

            threadPool.shutdown();
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(filteredSuggestResponse);
        } catch (JsonProcessingException e) {
            throw new InternalServiceException("An error occurred when processing JSON in the suggest response: ", e);
        } catch (InterruptedException | ExecutionException e) {
            throw new InternalServiceException("An error occurred when getting suggest response in parallel: ", e);
        }
    }

    private void addToSuggestResponse(SuggestionObject suggestion, SuggestionObjectList finalSuggestions, AtomicInteger allowedSuggestionsCount) {
        finalSuggestions.addSuggestion(suggestion);
        allowedSuggestionsCount.getAndAdd(1);
    }

    private boolean isSuggestAllowed(SuggestionObject suggestion, ObjectMapper objectMapper, String wt) {
        String title = suggestion.getTerm();
        String titleQuery = "title:\"" + title + "\"";

        SolrParamMerger merger = createBaseParams(SELECT, titleQuery, plainLicenseFilter, 1, null, null, null, wt);
        merger.put(FACET, false);
        merger.put(SPELLCHECK, false);
        merger.put("hl", false);


        URI uri = createRequest(SELECT, merger);
        String singleResult = performCall(titleQuery, uri, "search");
        SelectResponse singleResponse = null;
        try {
            singleResponse = objectMapper.readValue(singleResult, SelectResponse.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        if (singleResponse.getNumFound() > 0) {
            return true;
        } else {
            log.debug("Record with title '{}' can not be shown in suggestions.", title);
            return false;
        }
    }

    /**
     * Issue a Solr request for the schema and rturn the result.
     * @param wt the representation format for the schema. Defined in {@link WT_SCHEMA_ENUM}, with possible values
     *           {@code json}, @{code xml} and {@code schema.xml}. Note that {@code schema.xml} is created from the
     *           abstract schema, so comments and structure from the original {@code schema.xml} file are lost.
     *           Can be {@code null}, with default {@code json}.
     * @return the Solr schema in the requested representation.
     */
    public String schema(String wt){
        WT_SCHEMA_ENUM wt_enum = WT_SCHEMA_ENUM.safeParse(wt);
        URI schemaURI = schemaRequestBuilder(wt_enum);
        return performCall("", schemaURI, "schema");
    }

    /**
     * Create a param merger for the given {@code handler} and add the given parameters to it.
     * @return a handler-specific param merger, filled wit the given parameters.
     */
    private SolrParamMerger createBaseParams(
            String handler, String q, List<String> fq, Integer rows, Integer start, String fl, String qOp, String wt) {
        SolrParamMerger merger;
        switch (handler) {
            case "select":
                merger = selectFactory.createMerger();
                break;
            case "mlt":
                merger = mltFactory.createMerger();
                break;
            default: throw new UnsupportedOperationException(
                    "Unable to create a param merger for handler '" + handler +
                            "'. Supported handlers are 'select' and 'mlt'");
        }
        merger.put(Q, sanitiseQuery(q));
        merger.put(FQ, fq);
        merger.put(ROWS, rows);
        merger.put(START, start);
        merger.put(FL, fl);
        merger.put(QOP, QOP_ENUM.safeParse(qOp));
        merger.put(WT, WT_ENUM.safeParse(wt));
        return merger;
    }

    /**
     * Build an URI for the given Solr handler with the given parameters.
     * @param handler the Solr handler to use.
     * @param params the parameters for the call.
     * @return an URI ready for use with a HTTP component.
     */
    private URI createRequest(String handler, SolrParamMerger params) {
        try {
            URIBuilder builder = new URIBuilder(server)
                    .setPathSegments(path, solrCollection, handler);
            params.forEach((key, values) ->
                    values.forEach(value ->
                            builder.addParameter(key, value)));
            return builder.build();
        } catch (URISyntaxException e) {
            log.error("Unable to construct URL for Solr select call to server='{}', collection='{}' with parameters {}",
                    server, solrCollection, params, e);
            throw new InternalServiceException("Unable to construct URL for Solr request");
        }
    }

    /**
     * Creates a Solr oriented URI builder specific for suggest
     * @return a pre-filled builder ready to be extended with caller specific parameters.
     */
    private URI createSuggestRequestBuilder(String suggestDictionary, String suggestQuery, Integer suggestCount, String wt) {
        try {
            return new URIBuilder(server)
                    .setPathSegments(path, solrCollection, SUGGEST)
                    .addParameter(SUGGEST_Q, suggestQuery)
                    .addParameter(SUGGEST_DICTIONARY, suggestDictionary)
                    .addParameter(SUGGEST_COUNT, suggestCount.toString())
                    .addParameter(WT, WT_ENUM.safeParse(wt).toString())
                    .build();
        } catch (URISyntaxException e) {
            log.error("Unable to construct URL for Solr suggest call to server='{}', collection='{}' with " +
                            "dict='{}', query='{}', count={}, wt='{}'",
                    server, solrCollection, suggestDictionary, suggestQuery, suggestCount, wt, e);
            throw new InternalServiceException(
                    "Unable to construct URL for Solr suggest call for '" + suggestQuery + "'");
        }
    }

    /**
     * Create a {@code URIBuilder} for fetching the schema behind the solr collection
     * @return a ready to use builder.
     */
    public URI schemaRequestBuilder(WT_SCHEMA_ENUM wt){
        try {
            return new URIBuilder(server)
                    .setPathSegments(path, solrCollection, SCHEMA)
                    .addParameter(WT, wt.toString())
                    .build();
        } catch (URISyntaxException e) {
            log.error("Unable to construct URL for Solr schema call call to server='{}', collection='{}' with wt='{}'",
                    server, solrCollection, wt, e);
            throw new InternalServiceException("Unable to construct URL for Solr schema call");
        }
    }

    /**
     * Perform a HTTP(S) request for the given URI and return the response.
     * @param q Solr query used for logging only.
     * @param uri the full URI to request.
     * @param callType the overall type of call (search/facet/...) used for logging only.
     * @return the response from the request for {@code uri}
     */
    private String performCall(String q, URI uri, String callType) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .build();

        HttpResponse<String> response;
        try {
            log.debug("Calling " + uri);
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            log.warn(String.format(
                    Locale.ROOT, "Unable to perform remote %s call for collection '%s', query '%s'",
                    callType, getID(), q), e);
            throw new ServiceException(String.format(
                    Locale.ROOT, "Unable to perform remote %s call for query '%s'." +
                            "Remote service might not be responding.",
                    callType, StringListUtils.truncateMiddle(q, 100)), Response.Status.SERVICE_UNAVAILABLE);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.warn("Got HTTP {} from remote {} call for collection '{}', query '{}': {}",
                    response.statusCode(), callType, getID(), q, response.body());

            String solrError = ErrorMessageHandler.getErrorMsgFromSolrResponse(response.body());

            throw new ServiceException(String.format(
                    Locale.ROOT, "Got HTTP %d performing remote %s call for query '%s'. Solr error was: '%s'",
                    response.statusCode(), callType, StringListUtils.truncateMiddle(q, 100), solrError),
                    Response.Status.fromStatusCode(response.statusCode()));
        }

        return response.body();
    }

    /**
     * Return the MIME type corresponding to the given Solr wt, defaulting to JSON.
     * @param wt the Solr param wt. Can be null, which will result in {@code application/json}.
     * @return the MIME type corresponding to the wt.
     * @throws InvalidArgumentServiceException if the wt is unsupported.
     */
    public String getResponseMIMEType(String wt) {
        return WT_ENUM.safeParse(wt).getMIME();
    }

    /**
     * Return the MIME type corresponding to the given Solr schema wt, defaulting to JSON.
     * <p>
     * The schema WT differs from standard WT by missing {@code csv} and adding {@code schema.xml}.
     * @param wt the Solr param wt. Can be null, which will result in {@code application/json}.
     * @return the MIME type corresponding to the wt.
     * @throws InvalidArgumentServiceException if the wt is unsupported.
     */
    public String getSchemaResponseMIMEType(String wt) {
        return WT_SCHEMA_ENUM.safeParse(wt).getMIME();
    }


    /**
     * Disables function calls and catch-all regexp.
     * @param q Solr query.
     * @return sanitised Solr query.
     */
    // TODO: Base sanitising on roles
    // TODO: Catch field-qualified regexp
    private String sanitiseQuery(String q) {
        return SANITISE_PATTERN.matcher(q).replaceFirst(SANITISE_REPLACEMENT);
    }
    private final static Pattern SANITISE_PATTERN = Pattern.compile("^([{/])");
    private final static String SANITISE_REPLACEMENT = "\\$1";

    /**
     * @return the ID for the Solr service.
     */
    public String getID() {
        return id;
    }

    /**
     * Starts a lenient shutdown, allowing existing calls to be completed.
     * After calling shutdown, new requests should not be issued.
     */
    public void shutdown() {
        log.info("Shutting down " + this);
        // Currently does nothing as there is no locked resources between calls
    }

    /**
     * Remove filters with {@code prefix} from the {@code params.fq} entries in {@code solrResponse}.
     * Intended use is to remove internal licensing information from responses returned to external caller.
     * <p>
     * Note: This uses regexp-based search/replace to adjust JSON & XML.
     *       This is error prone and should generally be avoided.
     *       It is used here as the service is expected to under heavy load so performance is a high priority.
     * @param solrResponse a Solr search response in {@code wt} format.
     * @param prefix the prefix identifying the filter to remove, e.g. {@code {!cache=true}}.
     * @param wt the delivery format ({@code json}, {@code xml}, {@code csv}). null means {@code json}.
     * @return the response with the prefixed filter removed.
     * @throws IllegalArgumentException if the {@code wt} was unknown or the {@code solrResponse} did not contain
     *                                  a prefixed filter.
     */
    public static String removePrefixedFilters(String solrResponse, String prefix, String wt) {
        switch (wt == null ? "json" : wt) {
            case "json": return removePrefixedFiltersJSON(solrResponse, prefix);
            case "xml": return removePrefixedFilterXML(solrResponse, prefix);
            case "csv": return solrResponse;

            // Missing is python, ruby, php. Very low priority

            default:
                log.warn("removePrefixedFilters: Request for removing for unsupported format '" + wt + "'");
                throw new IllegalArgumentException("The Solr delivery format '" + wt + "' is unsupported");
        }
    }

    /**
     * Remove filters with {@code prefix} from the {@code params.fq} entries in {@code solrResponse}.
     * Intended use is to remove internal licensing information from responses returned to external caller.
     * <p>
     * Note: This uses regexp-based search/replace to adjust the JSON.
     *       This is error prone and should generally be avoided.
     *       It is used here as the service is expected to under heavy load so performance is a high priority.
     * @param solrResponse a Solr search response in JSON format.
     * @param prefix the prefix identifying the filter to remove, e.g. {@code {!cache=true}}.
     * @return the response with the prefixed filter removed.
     * @throws IllegalArgumentException if the {@code solrResponse} did not contain a prefixed filter.
     */
    // TODO: This uses 3 regexp-based search-replace, which will be heavy on large inputs. Consider CallbackReplacer
    private static String removePrefixedFiltersJSON(String solrResponse, String prefix) {
        // In the sample below, the filter starting with {!cache=true} is to be removed

//     "fq": [
//        "number_of_episodes:[2 TO 10]",
//        "resource_description:[* TO \"Moving Image\"]",
//        "{!cache=true}(((access_searlige_visningsvilkaar:\"Visning kun af metadata\") OR (catalog:\"Maps\") OR (collection:\"Det Kgl. Bibliotek; Radio/TV-Samlingen\") OR (catalog:\"Samlingsbilleder\")) -(id:(\"fr508045.tif\" OR \"fr552041x.tif\")) -(access_blokeret:true) -(cataloging_language:*tysk*))"
//      ],

        // Note that the optional leading comma is stripped.
        // It is assumed that prefixed filters will appear after other filters
        Pattern stripPattern = Pattern.compile(
                "(?:,\\s*)?\"" + Pattern.quote(prefix) + ".*?[^\\\\]\"", Pattern.DOTALL);
        Matcher stripMatcher = stripPattern.matcher(solrResponse);
        if (!stripMatcher.find()) {
            String message =
                    "Unable to find a match for the prefixed filter with '" + stripMatcher.pattern() + "'";
            log.warn(message);
            throw new IllegalArgumentException(message);
        }
        // Remove prefixed queries
        String response = stripMatcher.replaceAll("");
        // If there is exactly 1 fq, make it a single value instead of an array.
        response = SINGLE_FQ_JSON.matcher(response).replaceAll(SINGLE_FQ_JSON_REPLACEMENT);
        // If there are no fq left, remove the fq-key
        return EMPTY_FQ_JSON.matcher(response).replaceAll("");
    }

    /**
     * Remove filters with {@code prefix} from the {@code params.fq} entries in {@code solrResponse}.
     * Intended use is to remove internal licensing information from responses returned to external caller.
     * <p>
     * Note: This uses regexp-based search/replace to adjust the XML.
     *       This is error prone and should generally be avoided.
     *       It is used here as the service is expected to under heavy load so performance is a high priority.
     * @param solrResponse a Solr search response in XML format.
     * @param prefix the prefix identifying the filter to remove, e.g. {@code {!cache=true}}.
     * @return the response with the prefixed filter removed.
     * @throws IllegalArgumentException if the {@code solrResponse} did not contain a prefixed filter.
     */
    private static String removePrefixedFilterXML(String solrResponse, String prefix) {
        // In the sample below, the filter starting with {!cache=true} is to be removed

//    <str name="q.op">AND</str>
//    <arr name="fq">
//      <str>number_of_episodes:[2 TO 10]</str>
//      <str>resource_description:[* TO "Moving Image"]</str>
//      <str>{!cache=true}(((access_searlige_visningsvilkaar:"Visning kun af metadata") OR (catalog:"Maps") OR (collection:"Det Kgl. Bibliotek; Radio/TV-Samlingen") OR (catalog:"Samlingsbilleder")) -(id:("fr508045.tif" OR "fr552041x.tif")) -(access_blokeret:true) -(cataloging_language:*tysk*))</str>
//    </arr>
        Pattern stripPattern = Pattern.compile(
                " *<str>" + Pattern.quote(prefix) + ".*?</str>\n?", Pattern.DOTALL);
        Matcher stripMatcher = stripPattern.matcher(solrResponse);
        if (!stripMatcher.find()) {
            String message =
                    "Unable to find a match for the prefixed filter with '" + stripMatcher.pattern() + "'";
            log.warn(message);
            throw new IllegalArgumentException(message);
        }
        String response = stripMatcher.replaceAll("");
        return EMPTY_FQ_XML.matcher(response).replaceAll("");
    }

    /**
     * Request a filter query from ds-license and use it as fq.
     * @param designation describes the caller, used for logging only.
     * @return {@code fq} extended with an access filter from ds-license.
     */
    private List<String> createAccessFilter(String designation) {
        //Add filter query from license module.
        DsLicenseApi licenseClient = getDsLicenseApiClient();
        GetUserQueryInputDto licenseQueryDto = getLicenseQueryDto();
        GetUsersFilterQueryOutputDto filterQuery;
        try {
            filterQuery = licenseClient.getUserLicenseQuery(licenseQueryDto);
        } catch (Exception e) {
            log.warn("Unable to get response from ds-license at URL '" +
                    ServiceConfig.getConfig().getString("licensemodule.url") + "'", e);
            throw new InternalServiceException("Unable to contact license server");
        }

        log.debug("{}: Using filter query='{}' for user attributes='{}'",
                designation, filterQuery.getFilterQuery(), getLicenseQueryDto());

        List<String> fq = new ArrayList<>();

        if (filterQuery.getFilterQuery() != null && !filterQuery.getFilterQuery().isEmpty()) {
            fq.add(FILTER_CACHE_PREFIX + filterQuery.getFilterQuery()); //Add the additional filter query
        }
        return fq;
    }

    private static final Pattern SINGLE_FQ_JSON = Pattern.compile("\"fq\":\\s*\\[\\s*(\"(?:[^\"\\\\]|\\\\.)*\")\\s*]", Pattern.DOTALL);
    private static final String SINGLE_FQ_JSON_REPLACEMENT = "\"fq\": $1";
    private static final Pattern EMPTY_FQ_JSON = Pattern.compile(" *\"fq\":\\s*\\[?\\s*]?,\n?", Pattern.DOTALL);
    private static final Pattern EMPTY_FQ_XML = Pattern.compile(" *<arr name=\"fq\">\\s*</arr>\n?", Pattern.DOTALL);

    @Override
    public String toString() {
        return "SolrService(" +
               "id='" + id + '\'' +
               ", server='" + server + '\'' +
               ", path='" + path + '\'' +
               ", solrCollection='" + solrCollection + '\'' +
               ')';
    }

}
