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

import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import dk.kb.util.other.StringListUtils;
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

    public static final String Q = "q";
    public static final String FQ = "fq";
    public static final String FL = "fl";
    public static final String ROWS = "rows";
    public static final String FACET = "facet";
    public static final String FACET_FIELD = "facet.field";
    public static final String QOP = "q.op";
    public static final String WT = "wt";
    public static final String VERSION = "version"; // Used with wt=xml, always 2.2, not mandatory
    public static final String INDENT = "indent";
    public static final String DEBUG = "debug";
    public static final String DEBUG_EXPLAIN_STRUCTURED = "debug.explain.structured";

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

    public SolrService(String id, String server, String path, String solrCollection) {
        this.id = id;
        this.server = server;
        this.path = path;
        this.solrCollection = solrCollection;
        log.info("Created " + this);
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
     * @return Solr response.
     */
    public String query(String q, List<String> fq, Integer rows, String fl, String facet, List<String> facetField, String qOp, String wt, String version, String indent, String debug, String debugExplainStructured) {
        if (q == null) {
            throw new InvalidArgumentServiceException("q is mandatory but was missing");
        }
        // TODO: Catch extra arguments and throw "not supported"
        UriBuilder builder = UriBuilder.fromUri(server)
                .path(path)
                .path(solrCollection)
                .path(SELECT)
                .queryParam(Q, sanitiseQuery(q))
                .queryParam(QOP, QOP_ENUM.safeParse(qOp))
                .queryParam(WT, WT_ENUM.safeParse(wt));
        // TODO: Add role based filters
        if (fq != null) {
            fq.forEach(fqs -> builder.queryParam(FQ, fqs));
        }
        if (rows != null) {
            builder.queryParam(ROWS, rows);
        }
        if (fl != null) {
            builder.queryParam(FL, fl);
        }
        if (facet != null) {
            builder.queryParam(FACET, Boolean.parseBoolean(facet));
        }
        if (facetField != null) {
            facetField.forEach(ff -> builder.queryParam(FACET_FIELD, ff));
        }
        if (version != null) {
            builder.queryParam(VERSION, version);
        }

        if (indent != null) {
            builder.queryParam(INDENT, indent);
        }
        if (debug != null) {
            builder.queryParam(DEBUG, DEBUG_ENUM.safeParse(debug));
        }
        if (debugExplainStructured != null || debug != null) {
            builder.queryParam(DEBUG_EXPLAIN_STRUCTURED, Boolean.parseBoolean(debugExplainStructured));
        }

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
                    Locale.ROOT, "Unable to perform remote call for collection '%s', query '%s'", getID(), q), e);
            throw new InternalServiceException("Unable to perform remote call for query '" +
                                               StringListUtils.truncateMiddle(q, 100) +
                                               "'. Remote service might not be responding");
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.warn("Got HTTP {} from remote call for collection '{}', query '{}'", response.statusCode(), getID(), q);
            throw new InternalServiceException(String.format(
                    Locale.ROOT, "Got HTTP %d performing remote call for query '%s'",
                    response.statusCode(), StringListUtils.truncateMiddle(q, 100)));
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
