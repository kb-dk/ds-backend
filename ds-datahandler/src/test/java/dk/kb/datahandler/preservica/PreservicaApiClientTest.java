package dk.kb.datahandler.preservica;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import de.thetaphi.forbiddenapis.SuppressForbidden;
import dk.kb.datahandler.model.v1.OaiTargetSearchDto;
import dk.kb.util.webservice.exception.InternalServiceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// The JDK HTTP server is only used as a local stub for Preservica
@SuppressForbidden
public class PreservicaApiClientTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String SEARCH_RESPONSE = """
            {"success":true,"value":{"totalHits":2,
              "objectIds":["sdb:IO|aaaa","sdb:IO|bbbb"],
              "metadata":[[{"name":"xip.created","value":1532076564174}],[{"name":"xip.created","value":1532076564177}]]}}""";

    private static final String QUERY = """
            {"fields": [
              {"name": "xip.document_type", "values": ["IO"]},
              {"name": "pbcore.PBformatmedia", "values": ["Moving Image", "Sound"]}
            ], "q": "ignored", "sort": [{"sortFields": ["xip.title"], "sortOrder": "desc"}]}""";

    private HttpServer server;
    private final List<String> calls = new CopyOnWriteArrayList<>();
    private final AtomicInteger tokenCounter = new AtomicInteger();
    private volatile int validForMinutes = 15;
    private volatile String rejectedToken = null;

    @BeforeEach
    public void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/api/accesstoken/login", exchange -> {
            calls.add("login");
            respond(exchange, 200, tokenResponse());
        });
        server.createContext("/api/accesstoken/refresh", exchange -> {
            calls.add("refresh");
            respond(exchange, 200, tokenResponse());
        });
        server.createContext("/api/content/search", exchange -> {
            String token = exchange.getRequestHeaders().getFirst("Preservica-Access-Token");
            calls.add("search:" + token);
            if (token.equals(rejectedToken)) {
                respond(exchange, 401, "");
                return;
            }
            respond(exchange, 200, SEARCH_RESPONSE);
        });
        server.start();
    }

    @AfterEach
    public void stopServer() {
        server.stop(0);
    }

    private String tokenResponse() {
        int n = tokenCounter.incrementAndGet();
        return "{\"success\":true,\"token\":\"t" + n + "\",\"refresh-token\":\"r" + n + "\",\"validFor\":" +
               validForMinutes + "}";
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        exchange.getRequestBody().readAllBytes();
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            exchange.getResponseBody().write(bytes);
        }
        exchange.close();
    }

    private PreservicaApiClient client() {
        OaiTargetSearchDto search = new OaiTargetSearchDto();
        search.setApiUrl("http://localhost:" + server.getAddress().getPort() + "/");
        search.setUsername("user");
        search.setPassword("secret");
        search.setQuery(QUERY);
        return new PreservicaApiClient(search);
    }

    @Test
    public void search_whenOk_thenHitsWithCreated() {
        PreservicaSearchPager.Page page = client().search(null, 10);

        assertEquals(2, page.totalHits());
        assertEquals(new PreservicaSearchPager.Hit("aaaa", Instant.ofEpochMilli(1532076564174L)), page.hits().get(0));
        assertEquals("bbbb", page.hits().get(1).id());
        assertEquals(List.of("login", "search:t1"), calls);
    }

    @Test
    public void search_whenTokenStillValid_thenNoNewLogin() {
        PreservicaApiClient client = client();
        client.search(null, 10);
        client.search(null, 10);

        assertEquals(List.of("login", "search:t1", "search:t1"), calls);
    }

    @Test
    public void search_whenTokenExpired_thenRefreshedBeforeRequest() {
        validForMinutes = 0;
        PreservicaApiClient client = client();
        client.search(null, 10);
        client.search(null, 10);

        assertEquals(List.of("login", "search:t1", "refresh", "search:t2"), calls);
    }

    @Test
    public void search_when401_thenLoginAgainAndResend() {
        PreservicaApiClient client = client();
        client.search(null, 10);
        rejectedToken = "t1";
        client.search(null, 10);

        assertEquals(List.of("login", "search:t1", "search:t1", "login", "search:t2"), calls);
    }

    @Test
    public void query_whenRange_thenIsoMillisAndConfiguredFields() throws IOException {
        JsonNode query = JSON.readTree(client().query(Instant.ofEpochMilli(1532076564000L)));

        assertEquals("xip.created:[2018-07-20T08:49:24.000Z TO *]", query.path("q").asText());
        assertEquals(JSON.readTree(QUERY).path("fields"), query.path("fields"));
        assertEquals(1, query.path("sort").size());
        assertEquals("xip.created", query.path("sort").get(0).path("sortFields").get(0).asText());
        assertEquals("asc", query.path("sort").get(0).path("sortOrder").asText());
    }

    @Test
    public void query_whenNoRange_thenEmptyQ() throws IOException {
        assertEquals("", JSON.readTree(client().query(null)).path("q").asText());
    }

    @Test
    public void constructor_whenQueryNotJsonObject_thenInternalError() {
        OaiTargetSearchDto search = new OaiTargetSearchDto();
        search.setApiUrl("http://localhost");
        search.setQuery("{\"fields\": [");
        assertThrows(InternalServiceException.class, () -> new PreservicaApiClient(search));
        search.setQuery("[]");
        assertThrows(InternalServiceException.class, () -> new PreservicaApiClient(search));
    }
}
