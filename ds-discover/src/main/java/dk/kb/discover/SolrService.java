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

import dk.kb.util.other.StringListUtils;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Encapsulates assess to a Solr server.
 */
/*
  20220518: This is an extremely simple wrapper. No throttling, no client-side caching...
 */
public class SolrService {
    private static final Logger log = LoggerFactory.getLogger(SolrService.class);

    public static final String SELECT = "select";
    public static final String MLT = "mlt";

    public static final String Q = "q";
    public static final String FQ = "fq";
    public static final String FL = "fl";
    public static final String ROWS = "rows";
    public static final String START = "start";
    public static final String FACET = "facet";
    public static final String FACET_FIELD = "facet.field";
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

    private final HttpClient client = HttpClient.newHttpClient();

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
    public enum WT_ENUM {json, csv, xml;
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
                default: throw new UnsupportedOperationException("The WT '" + this + "' has no MIME type defined");
            }
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
        UriBuilder builder = createBaseRequestBuilder(MLT, q, fq, rows, start, fl, qOp, wt);

        addParamIfAvailable(builder, MLT_FL, mltFl);
        addParamIfAvailable(builder, MLT_MINTF, mltMintf);
        addParamIfAvailable(builder, MLT_MINDF, mltMindf);
        addParamIfAvailable(builder, MLT_MAXDF, mltMaxdf);
        addParamIfAvailable(builder, MLT_MAXDFPCT, mltMaxdfpct);
        addParamIfAvailable(builder, MLT_MINWL, mltMinwl);
        addParamIfAvailable(builder, MLT_MAXWL, mltMaxwl);
        addParamIfAvailable(builder, MLT_MAXQT, mltMaxqt);
        addParamIfAvailable(builder, MLT_BOOST, mltBoost);
        if (mltInterestingTerms != null) {
            builder.queryParam(MLT_INTERESTING_TERMS, MLT_INTERESTING_TERMS_ENUM.safeParse(mltInterestingTerms));
        }
        if (extra != null) {
            extra.forEach((key, values) ->
                    Arrays.stream(values).forEach(
                            value -> builder.queryParam(key, value)));
        }

        return performCall(q, builder, "mlt");

    }

    /**
     * Issue a Solr query and return the result.
     *
     * @param q                      Solr query.
     * @param fq                     Solr filter query.
     * @param rows
     * @param fl                     Solr field list.
     * @param facetField
     * @param qOp                    Solr default boolean operator.
     * @param wt                     Solr response format.
     * @param indent                 if true, Solr response is indented (if possible).
     * @param debug                  as enumerated in {@link DEBUG_ENUM}.
     * @param debugExplainStructured true if debug information should be structuredinstead of just a string.
     * @param extra                  optional extra parameters.
     * @return Solr response.
     */
    @SuppressWarnings("SuspiciousTernaryOperatorInVarargsCall")
    public String query(String q, List<String> fq, Integer rows, Integer start, String fl, String facet, List<String> facetField, String qOp, String wt, String version, String indent, String debug, String debugExplainStructured, Map<String, String[]> extra) {
        if (q == null) {
            throw new InvalidArgumentServiceException("q is mandatory but was missing");
        }
        // TODO: Catch extra arguments and throw "not supported"
        UriBuilder builder = createBaseRequestBuilder(SELECT, q, fq, rows, start, fl, qOp, wt);

        if (facet != null) {
            builder.queryParam(FACET, Boolean.parseBoolean(facet));
        }
        if (facetField != null) {
            facetField.forEach(ff -> builder.queryParam(FACET_FIELD, ff));
        }
        addParamIfAvailable(builder, VERSION, version);
        addParamIfAvailable(builder, INDENT, indent);

        if (debug != null) {
            builder.queryParam(DEBUG, DEBUG_ENUM.safeParse(debug));
        }
        if (debugExplainStructured != null || debug != null) {
            builder.queryParam(DEBUG_EXPLAIN_STRUCTURED, Boolean.parseBoolean(debugExplainStructured));
        }

        if (extra != null) {
            extra.forEach((key, values) ->
                    Arrays.stream(values).forEach(
                            value -> builder.queryParam(key, value)));
        }

        return performCall(q, builder, "search");
    }

    /**
     * Creates a Solr oriented URI builder with basic parameters shared by standard search and More Like This requests.
     * @return a pre-filled builder ready to be extended with caller specific parameters.
     */
    private UriBuilder createBaseRequestBuilder(
            String handler, String q, List<String> fq, Integer rows, Integer start, String fl, String qOp, String wt) {
        UriBuilder builder = UriBuilder.fromUri(server)
                .path(path)
                .path(solrCollection)
                .path(handler)
                .queryParam(Q, sanitiseQuery(q))
                .queryParam(QOP, QOP_ENUM.safeParse(qOp))
                .queryParam(WT, WT_ENUM.safeParse(wt));
        // TODO: Add role based filters
        if (fq != null) {
            fq.forEach(fqs -> builder.queryParam(FQ, fqs));
        }
        addParamIfAvailable(builder, ROWS, rows);
        addParamIfAvailable(builder, START, start);
        addParamIfAvailable(builder, FL, fl);
        return builder;
    }


    private String performCall(String q, UriBuilder builder, String callType) {
        URI solrCall = builder.build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(solrCall)
                .build();

        HttpResponse<String> response;
        try {
            log.debug("Calling " + solrCall);
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            log.warn(String.format(
                    Locale.ROOT, "Unable to perform remote %s call for collection '%s', query '%s'",
                    callType, getID(), q), e);
            throw new InternalServiceException(String.format(
                    Locale.ROOT, "Unable to perform remote %s call for query '%s'." +
                            "Remote service might not be responding",
                    callType, StringListUtils.truncateMiddle(q, 100)));
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.warn("Got HTTP {} from remote {} call for collection '{}', query '{}': {}",
                    response.statusCode(), callType, getID(), q, response.body());
            throw new InternalServiceException(String.format(
                    Locale.ROOT, "Got HTTP %d performing remote %s call for query '%s'",
                    response.statusCode(), callType, StringListUtils.truncateMiddle(q, 100)));
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
     * Disables function calls and catch-all regexp.
     * @param q Solr query.
     * @return sanitised Solr query.
     */
    // TODO: Base sanitising on roles
    // TODO: Catch field-qualified regexp
    private String sanitiseQuery(String q) {
        return SANITISE_PATTERN.matcher(q).replaceFirst(SANITISE_REPLACEMENT);
    }
    private final Pattern SANITISE_PATTERN = Pattern.compile("^([{/])");
    private final String SANITISE_REPLACEMENT = "\\$1";

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
     * If {@code value} is not null, {@code key=value} is added as a param to the builder.
     * @param builder builder for a Solr query.
     * @param key     key for the param to add if a value is present.
     * @param value   value for the key.
     */
    private void addParamIfAvailable(UriBuilder builder, String key, Object value) {
        if (value != null) {
            builder.queryParam(key, value);
        }
    }

    /**
     * Remove filters with {@code prefix} from the {@code params.fq} entries in {@code solrResponse}.
     * Intended use is to remove internal licensing information from responses returned to external caller.
     * <p>
     * Note: This uses regexp-based search/replace to adjust JSON & XML. This is error prone and should generally be
     *       avoided. It is used here
     *
     * @param solrResponse a Solr search response in {@code wt} format.
     * @param prefix the prefix identifying the filter to remove, e.g. {@code {!cache=true}}.
     * @param wt the delivery format ({@code json}, {@code xml}, {@code csv}). null means {@code json}.
     * @return the response with the prefixed filter removed.
     * @throws IllegalArgumentException if the {@code wt} was unknown or the {@code solrResponse} did not contain
     *                                  a prefixed filter.
     */
    public static String removePrefixedFilters(String solrResponse, String prefix, String wt) {
        switch (wt == null ? "json" : wt) {
            case "json": {
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
                // If there is only 1 fq, make it a single value instead of an array.
                response = SINGLE_FQ_JSON.matcher(response).replaceAll(SINGLE_FQ_JSON_REPLACEMENT);
                // If there are no fq left, remove the fq-key
                return EMPTY_FQ_JSON.matcher(response).replaceAll("");
            }

            case "xml": {
//    <str name="q.op">AND</str>
//    <arr name="fq">
//      <str>number_of_episodes:[2 TO 10]</str>
//      <str>resource_description:[* TO "Moving Image"]</str>
//      <str>(((access_searlige_visningsvilkaar:"Visning kun af metadata") OR (catalog:"Maps") OR (collection:"Det Kgl. Bibliotek; Radio/TV-Samlingen") OR (catalog:"Samlingsbilleder")) -(id:("fr508045.tif" OR "fr552041x.tif")) -(access_blokeret:true) -(cataloging_language:*tysk*))</str>
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

            case "csv":
                return solrResponse;

            // Missing is python, ruby, php. Very low priority

            default:
                log.warn("removePrefixedFilters: Request for removing for unsupported format '" + wt + "'");
                throw new IllegalArgumentException("The Solr delivery format '" + wt + "' is unsupported");
        }
    }
    private static final Pattern SINGLE_FQ_JSON = Pattern.compile("\"fq\":\\s*\\[\\s*(\".*?[^\\\\]\")\\s*]", Pattern.DOTALL);
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
