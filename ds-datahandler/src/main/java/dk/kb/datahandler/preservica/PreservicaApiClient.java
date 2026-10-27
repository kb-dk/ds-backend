package dk.kb.datahandler.preservica;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.kb.datahandler.model.v1.OaiTargetSearchDto;
import dk.kb.util.webservice.exception.InternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Client for the Preservica Content API search, used to list information objects in {@code xip.created} order.
 * <p>
 * Access tokens are valid for {@code validFor} minutes. They are refreshed shortly before they expire, since a
 * refresh token is single-use and only accepted while its access token is still valid. A failed refresh or a 401
 * falls back to a full login.
 * <p>
 * Thread safe.
 */
public class PreservicaApiClient {
    private static final Logger log = LoggerFactory.getLogger(PreservicaApiClient.class);

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration RETRY_BASE_DELAY = Duration.ofSeconds(2);
    private static final Duration REFRESH_MARGIN = Duration.ofMinutes(1);

    private final OaiTargetSearchDto search;
    private final ObjectNode configuredQuery;
    private final String baseUrl;
    private final HttpClient http;

    private String token;
    private String refreshToken;
    private Instant expiresAt = Instant.MIN;

    public PreservicaApiClient(OaiTargetSearchDto search) {
        this.search = search;
        this.configuredQuery = parseQuery(search.getQuery());
        ObjectNode sort = configuredQuery.putArray("sort").addObject();
        sort.putArray("sortFields").add("xip.created");
        sort.put("sortOrder", "asc");
        this.baseUrl = search.getApiUrl().replaceAll("/+$", "");
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
    }

    /**
     * @param from inclusive lower bound for {@code xip.created}, or null for no lower bound.
     * @return the first {@code max} hits, sorted ascending on {@code xip.created}.
     */
    public PreservicaSearchPager.Page search(Instant from, int max) {
        String query = query(from);
        String uri = baseUrl + "/api/content/search?q=" + urlEncode(query) + "&start=0&max=" + max +
                     "&metadata=" + urlEncode("xip.created");
        HttpResponse<String> response = get(uri);
        if (response.statusCode() != 200) {
            throw new InternalServiceException("Preservica search failed with HTTP " + response.statusCode() +
                                               " for query: " + query);
        }
        return parseSearchResponse(response.body());
    }

    /**
     * @param from inclusive lower bound for {@code xip.created}, or null for no lower bound.
     * @return the configured search expression with {@code q} set to the range and sorted on {@code xip.created}.
     */
    String query(Instant from) {
        // The search only accepts ISO timestamps with milliseconds in ranges, not epoch milliseconds
        return configuredQuery.deepCopy()
                .put("q", from == null ? "" : "xip.created:[" + PreservicaSearchPager.iso(from) + " TO *]")
                .toString();
    }

    private static ObjectNode parseQuery(String query) {
        try {
            if (JSON.readTree(query) instanceof ObjectNode node) {
                return node;
            }
        } catch (IOException e) {
            throw new InternalServiceException("The configured Preservica search query is not valid JSON: " + query, e);
        }
        throw new InternalServiceException("The configured Preservica search query is not a JSON object: " + query);
    }

    static PreservicaSearchPager.Page parseSearchResponse(String body) {
        try {
            JsonNode root = JSON.readTree(body);
            if (!root.path("success").asBoolean(false)) {
                throw new InternalServiceException("Preservica search was not successful: " + root.path("message").asText());
            }
            JsonNode value = root.path("value");
            JsonNode objectIds = value.path("objectIds");
            List<PreservicaSearchPager.Hit> hits = new ArrayList<>();
            for (int i = 0; i < objectIds.size(); i++) {
                String id = objectIds.get(i).asText().replaceFirst("^sdb:[A-Z]+\\|", "");
                hits.add(new PreservicaSearchPager.Hit(id, created(id, value.path("metadata").path(i))));
            }
            return new PreservicaSearchPager.Page(hits, value.path("totalHits").asLong());
        } catch (IOException e) {
            throw new InternalServiceException("Could not parse Preservica search response", e);
        }
    }

    // The metadata of a hit is a list of {name, value}; xip.created is epoch milliseconds
    private static Instant created(String id, JsonNode hitMetadata) {
        for (JsonNode field : hitMetadata) {
            if ("xip.created".equals(field.path("name").asText()) && field.path("value").isNumber()) {
                return Instant.ofEpochMilli(field.path("value").asLong());
            }
        }
        throw new InternalServiceException("Search hit '" + id + "' has no numeric xip.created");
    }

    private HttpResponse<String> get(String uri) {
        boolean loggedInAgain = false;
        for (int attempt = 1; ; attempt++) {
            String usedToken = currentToken();
            HttpRequest request = HttpRequest.newBuilder(URI.create(uri))
                    .header("Preservica-Access-Token", usedToken)
                    .header("Accept", "application/json")
                    .timeout(Duration.ofMinutes(2))
                    .GET().build();
            try {
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                int status = response.statusCode();
                if (status == 401 && !loggedInAgain) {
                    login(usedToken);
                    loggedInAgain = true;
                    attempt--;
                    continue;
                }
                if ((status == 429 || status >= 500) && attempt < MAX_ATTEMPTS) {
                    backoff(attempt, uri, "HTTP " + status);
                    continue;
                }
                return response;
            } catch (IOException e) {
                if (attempt >= MAX_ATTEMPTS) {
                    throw new InternalServiceException("Request to Preservica failed: " + uri, e);
                }
                backoff(attempt, uri, e.toString());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InternalServiceException("Interrupted during request to Preservica: " + uri, e);
            }
        }
    }

    private synchronized String currentToken() {
        if (token == null) {
            login(null);
        } else if (Instant.now().isAfter(expiresAt)) {
            refresh();
        }
        return token;
    }

    // Only log in again if no other thread has done so since the token was used
    private synchronized void login(String rejectedToken) {
        if (token != null && !token.equals(rejectedToken)) {
            return;
        }
        String form = "username=" + urlEncode(search.getUsername()) + "&password=" + urlEncode(search.getPassword());
        if (!storeToken(post("/api/accesstoken/login", form, null))) {
            throw new InternalServiceException("Login to Preservica at " + baseUrl + " failed");
        }
        log.debug("Logged in to Preservica at {}", baseUrl);
    }

    private synchronized void refresh() {
        try {
            if (storeToken(post("/api/accesstoken/refresh", "refreshToken=" + urlEncode(refreshToken), token))) {
                log.debug("Refreshed Preservica access token");
                return;
            }
            log.info("Refreshing the Preservica access token was rejected, logging in again");
        } catch (InternalServiceException e) {
            log.info("Refreshing the Preservica access token failed, logging in again", e);
        }
        token = null;
        login(null);
    }

    private HttpResponse<String> post(String path, String form, String accessToken) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .timeout(Duration.ofMinutes(1))
                .POST(HttpRequest.BodyPublishers.ofString(form));
        if (accessToken != null) {
            request.header("Preservica-Access-Token", accessToken);
        }
        try {
            return http.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new InternalServiceException("Request to Preservica failed: " + baseUrl + path, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalServiceException("Interrupted during request to Preservica: " + baseUrl + path, e);
        }
    }

    /**
     * @return true if the response held a new token, which is then stored.
     */
    private boolean storeToken(HttpResponse<String> response) {
        if (response.statusCode() != 200) {
            return false;
        }
        try {
            JsonNode root = JSON.readTree(response.body());
            String newToken = root.path("token").asText("");
            if (!root.path("success").asBoolean(false) || newToken.isEmpty()) {
                return false;
            }
            token = newToken;
            refreshToken = root.path("refresh-token").asText(null);
            expiresAt = Instant.now().plus(Duration.ofMinutes(root.path("validFor").asLong(15))).minus(REFRESH_MARGIN);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static void backoff(int attempt, String uri, String reason) {
        Duration delay = RETRY_BASE_DELAY.multipliedBy(1L << (attempt - 1));
        log.warn("Request to Preservica failed with {} (attempt {}/{}), retrying in {}s: {}",
                 reason, attempt, MAX_ATTEMPTS, delay.toSeconds(), uri);
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalServiceException("Interrupted while waiting to retry: " + uri, e);
        }
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
