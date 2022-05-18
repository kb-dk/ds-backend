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

import dk.kb.discover.webservice.exception.InternalServiceException;
import dk.kb.discover.webservice.exception.InvalidArgumentServiceException;
import dk.kb.util.other.StringListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Encapsulates assess to a Solr server.
 */
public class SolrService {
    private static final Logger log = LoggerFactory.getLogger(SolrService.class);

    public static final String SELECT = "select";

    public static final String Q = "q";

    private final String id; // Abstract collection

    private final String server;
    private final String path;
    private final String solrCollection;

    private final HttpClient client = HttpClient.newHttpClient();

    public SolrService(String id, String server, String path, String solrCollection) {
        this.id = id;
        this.server = server;
        this.path = path;
        this.solrCollection = solrCollection;
        log.info("Created " + this);
    }

    /**
     * Issue a Solr query and return the result.
     * @param q Solr query, see https://solr.apache.org/guide/8_10/the-standard-query-parser.html
     * @return Solr response.
     */
    public String query(String q) {
        if (q == null) {
            throw new InvalidArgumentServiceException("q is mandatory but was missing");
        }
        // TODO: Catch extra arguments and throw "not supported"
        URI solrCall = UriBuilder.fromUri(server)
                .path(path)
                .path(solrCollection)
                .path(SELECT)
                .queryParam(Q, sanitiseQuery(q))
                // TODO: Add role based filters
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(solrCall)
                .build();
        HttpResponse<String> response;
        try {
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
