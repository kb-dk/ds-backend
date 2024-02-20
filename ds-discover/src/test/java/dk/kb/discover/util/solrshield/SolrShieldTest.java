package dk.kb.discover.util.solrshield;

import dk.kb.discover.config.ServiceConfig;
import dk.kb.util.Resolver;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
class SolrShieldTest {
    private static final Logger log = LoggerFactory.getLogger(SolrShieldTest.class);

    @BeforeAll
    static void setup() throws IOException {
        ServiceConfig.initialize(Resolver.resolveGlob("solrshield-test1.yaml").get(0).toString());

    }

    @Test
    void basicSearch() {
        Map<String, String[]> request = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"title", "text"}
        );
        Response response = SolrShield.test(request.entrySet(), 1000.0);
        log.debug("SolrShield response: " + response);
        assertTrue(response.allowed, "Request " + request + " should be allowed, but was not with reasons " +
                response.reasons);
        assertTrue(response.weight > 0.0, "Response should have weight > 0.0 but had " + response.weight);
    }

    @Test
    void searchFieldsStar() {
        Map<String, String[]> request = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"*, score"} // * expands to all fields, making the request too heavy
        );
        Response response = SolrShield.test(request.entrySet(), 1000.0);
        log.debug("SolrShield response: " + response);
        assertFalse(response.allowed, "Request " + request + " should be allowed. Response: " + response);
        assertTrue(response.weight > 1000.0, "Response should have weight > 1000.0 but had " + response.weight);
    }

    @Test
    void basicFacet() {
        Map<String, String[]> request = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"id"},
                "facet", new String[]{"true"},
                "facet.field", new String[]{"location", "genre"}
        );
        Response response = SolrShield.test(request.entrySet(), 2000.0);
        log.debug("SolrShield response: " + response);
        assertTrue(response.allowed,
                "Request " + toString(request) + " should be allowed, but was not with reasons " + response.reasons);
        assertTrue(response.weight > 0.0,
                "Response should have weight > 0.0 but had " + response.weight);
    }

    @Test
    void facetTooHeavy() {
        Map<String, String[]> request = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"genre"},
                "facet.limit", new String[]{"5000"},
                "facet", new String[]{"true"},
                "facet.field", new String[]{"location", "genre"}
        );
        Response response = SolrShield.test(request.entrySet(), 2000.0);
        log.debug("SolrShield response: " + response);
        assertFalse(response.allowed,
                "Request " + toString(request) + " should not be allowed. Response: " + response);
        assertTrue(response.weight > 2000.0,
                "The weight of the response should be > 2000 but was " + response.weight);
    }

    @Test
    void facetIllegal() {
        Map<String, String[]> request = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"genre"},
                "facet.limit", new String[]{"50000"},
                "facet", new String[]{"true"},
                "facet.field", new String[]{"location"}
        );
        Response response = SolrShield.test(request.entrySet(), Double.MAX_VALUE);
        log.debug("SolrShield response: " + response);
        assertFalse(response.allowed,
                "Request " + toString(request) + " should not be allowed. Response: " + response);
        assertTrue(response.reasons.toString().contains("is larger than maxValue"),
                "Response should contain clause stating that facet.limit is > maxValue. Response: " + response);
    }

    private String toString(Map<String, String[]> map) {
        return map.entrySet().stream()
                .map(e -> e.getKey() + "=" + Arrays.toString(e.getValue()))
                .collect(Collectors.joining("\", \"", "[\"", "\"]"));
    }
}