package dk.kb.discover.util.solrshield;

import dk.kb.discover.SolrManager;
import dk.kb.discover.config.ServiceConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.util.yaml.YAML;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

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

    private static SolrShield shield;

    @BeforeAll
    static void setup() throws IOException {
      // ServiceConfig needed by SolrManager (used by per-collection shield tests)
      ServiceConfig.getInstance().initialize("solrshield-test1.yaml");
      YAML fullConf = YAML.resolveLayeredConfigs("solrshield-test1.yaml");
      shield = new SolrShield(fullConf.getSubMap("solr.shield"));
    }

    @Test
    void testDeepCopySearch() {
        SearchComponent search = shield.profile.search;
        assertSearchParamSame(search, "origo");
        assertFalse(search.isCopy, "Origo Search should initially not be a copy");

        SearchComponent searchCopy = search.deepCopy(null);

        assertSearchParamSame(searchCopy, "deep copy");

        assertTrue(searchCopy.isCopy, "The deep copy of Search should be marked as a copy");
        assertFalse(search.isCopy, "The origo Search should still not be a copy itself after deep copy");
    }

    // After deep copying, the first class attributes for search should match the ones in the params map
    void assertSearchParamSame(SearchComponent search, String searchDesignation) {
        assertSame(search.q, search.getParam("q"),
                "The 'q' param from " + searchDesignation + " search should be the same");
        assertSame(search.fq, search.getParam("fq"),
                        "The 'fq' param from " + searchDesignation + " search should be the same");
        assertSame(search.fl, search.getParam("fl"),
                        "The 'fl' param from " + searchDesignation + " search should be the same");
        // TODO: Add the rest of the search params
    }

    private static YAML buildCollectionConfig(Map<String, String> collectionShields) {
        List<Map<String, Object>> collections = new ArrayList<>();
        for (Map.Entry<String, String> entry : collectionShields.entrySet()) {
            Map<String, Object> inner = new LinkedHashMap<>();
            inner.put("server", "http://localhost:8983");
            inner.put("collection", entry.getKey());

            if (entry.getValue() != null) {
                inner.put("shield", entry.getValue());
            }

            Map<String, Object> collEntry = new LinkedHashMap<>();
            collEntry.put(entry.getKey(), inner);
            collections.add(collEntry);
        }
        Map<String, Object> solr = new LinkedHashMap<>();
        solr.put("collections", collections);
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("solr", solr);
        return new YAML(root);
    }

    @Test
    void testDeepCopySearchApply() {
        SearchComponent search = shield.profile.search;
        assertFalse(search.isCopy, "Origo Search should initially not be a copy");

        Map<String, String[]> request = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"title", "text"}
        );
        shield.evaluateRequest(request.entrySet(), 1000.0);

        assertSame(search, shield.profile.search, "The base search should not have been replaced");
        assertFalse(search.isCopy, "The origo Search should still not be a copy itself after test");
    }

    @Test
    void basicSearch() {
        Map<String, String[]> request = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"title", "text"}
        );
        Response response = shield.evaluateRequest(request.entrySet(), 1000.0);
        log.debug("SolrShield response: " + response);
        assertTrue(response.allowed, "Request " + toString(request) + " should be allowed, but was not with reasons " +
                response.reasons);
        assertTrue(response.weight > 0.0, "Response should have weight > 0.0 but had " + response.weight);
    }

    @Test
    void searchFields() {
        Map<String, String[]> request = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"title", "text"}
        );
        Response response = shield.evaluateRequest(request.entrySet(), 100000.0);
        double firstWeight = response.weight;

        Map<String, String[]> request2 = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"title"}
        );
        Response response2 = shield.evaluateRequest(request2.entrySet(), 100000.0);
        double secondWeight = response2.weight;

        assertNotEquals(firstWeight, secondWeight, "The weights for searches with different fields should not be equal");
    }

    @Test
    void searchFieldsStar() {
        Map<String, String[]> request = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"*, score"} // * expands to all fields, making the request too heavy
        );
        Response response = shield.evaluateRequest(request.entrySet(), 1000.0);
        log.debug("SolrShield response: " + response);
        assertFalse(response.allowed, "Request " + toString(request) + " should be allowed. Response: " + response);
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
        Response response = shield.evaluateRequest(request.entrySet(), 2000.0);
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
                "facet.limit", new String[]{"50000"},
                "facet", new String[]{"true"},
                "facet.field", new String[]{"location", "genre"}
        );
        Response response = shield.evaluateRequest(request.entrySet(), 2000.0);
        log.debug("SolrShield response: " + response);
        assertFalse(response.allowed,
                "Request " + toString(request) + " should not be allowed due to facet.limit. Response: " + response);
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
        Response response = shield.evaluateRequest(request.entrySet(), Double.MAX_VALUE);
        log.debug("SolrShield response: " + response);
        assertFalse(response.allowed,
                "Request " + toString(request) + " should not be allowed. Response: " + response);
        assertTrue(response.reasons.toString().contains("is larger than maxValue"),
                "Response should contain clause stating that facet.limit is > maxValue. Response: " + response);
    }

    // --- Unlisted / unknown params ---
    @Test
    void unknownParamRejected() {
        Map<String, String[]> request = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"id"},
                "bogusParam", new String[]{"value"}
        );
        Response response = shield.evaluateRequest(request.entrySet(), 100000.0);
        assertFalse(response.allowed,
                "Unlisted params not allowed but got [bogusParam=[value]]");
        assertTrue(response.reasons.toString().contains("Unlisted params not allowed but got"),
                "Reason should be 'Unlisted params not allowed but got [bogusParam=[value]]'");
    }

    @Test
    void extraAllowedParameterAccepted() {
        // queryUUID is in solr.extraAllowedParameters and should bypass shield
        Map<String, String[]> request = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"id"},
                "queryUUID", new String[]{"abc-123"}
        );
        Response response = shield.evaluateRequest(request.entrySet(), 100000.0);
        assertTrue(response.allowed,
                "Request with extraAllowedParameter 'queryUUID' should be accepted but got reasons: " +
                        response.reasons);
    }

    // --- Explicitly denied param overrides unlistedParams.allowed=true ---
    @Test
    void testDisallowedSolrParam() throws IOException {

        Map<String, String[]> request = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"id"},
                "defType", new String[]{"edismax"}
        );
        Response response = shield.evaluateRequest(request.entrySet(), 100000.0);
        assertFalse(response.allowed,
                "Request with disallowed solr params should be rejected");
        assertTrue(response.reasons.toString().contains("not allowed as the param itself is not allowed"),
                "Reason should indicate the param itself is not allowed, got: " + response.reasons);
    }

    @Test
    void disabledShieldAllowsEverything() throws IOException {
        // Create a shield with enabled=false from the same base config
        YAML conf = YAML.resolveLayeredConfigs("solrshield-test1.yaml").getSubMap("solr.shield");
        conf.put("enabled", false);
        SolrShield disabledShield = new SolrShield(conf);

        // A request that would normally be rejected (unknown param)
        Map<String, String[]> request = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"id"},
                "defType", new String[]{"edismax"},
                "bogusParam", new String[]{"value"}
        );
        Response response = disabledShield.evaluateRequest(request.entrySet(), 100000.0);
        assertTrue(response.allowed,
                "Disabled shield should allow all requests, even with unknown params");
    }

    // --- Filter query (fq) ---

    @Test
    void filterQueryAccepted() {
        Map<String, String[]> request = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"id"},
                "fq", new String[]{"genre:drama"}
        );
        Response response = shield.evaluateRequest(request.entrySet(), 100000.0);
        assertTrue(response.allowed,
                "Request with fq should be accepted but got reasons: " + response.reasons);
    }

    @Test
    void filterQueryAddsWeight() {
        Map<String, String[]> requestWithoutFq = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"id"}
        );
        Response responseWithout = shield.evaluateRequest(requestWithoutFq.entrySet(), 100000.0);

        Map<String, String[]> requestWithFq = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"id"},
                "fq", new String[]{"genre:drama"}
        );
        Response responseWith = shield.evaluateRequest(requestWithFq.entrySet(), 100000.0);

        assertTrue(responseWith.weight > responseWithout.weight,
                "Adding fq should increase weight. Without: " + responseWithout.weight +
                        ", with: " + responseWith.weight);
    }

    @Test
    void rowsExceedingMaxValueRejected() {
        Map<String, String[]> request = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"id"},
                "rows", new String[]{"6000"} // maxValue is 5000
        );
        Response response = shield.evaluateRequest(request.entrySet(), Double.MAX_VALUE);
        assertFalse(response.allowed,
                "Request with rows exceeding maxValue should be rejected");
        assertTrue(response.reasons.toString().contains("rows"),
                "Reason should mention 'rows'");
        assertTrue(response.reasons.toString().contains("maxValue"),
                "Reason should mention 'maxValue'");
    }

    @Test
    void rowsWithinMaxValueAccepted() {
        Map<String, String[]> request = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"id"},
                "rows", new String[]{"100"}
        );
        Response response = shield.evaluateRequest(request.entrySet(), 100000.0);
        assertTrue(response.allowed,
                "Request with rows=100 should be accepted but got reasons: " + response.reasons);
    }

    @Test
    void startExceedingMaxValueRejected() {
        Map<String, String[]> request = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"id"},
                "start", new String[]{"1500"} // maxValue is 1000
        );
        Response response = shield.evaluateRequest(request.entrySet(), Double.MAX_VALUE);
        assertFalse(response.allowed,
                "Request with start exceeding maxValue should be rejected");
        assertTrue(response.reasons.toString().contains("start"),
                "Reason should mention 'start'");
    }


    @Test
    void queryExceedingMaxCharsRejected() {
        // maxChars for q is 1000
        String longQuery = "a".repeat(1001);
        Map<String, String[]> request = Map.of(
                "q", new String[]{longQuery},
                "fl", new String[]{"id"}
        );
        Response response = shield.evaluateRequest(request.entrySet(), Double.MAX_VALUE);
        assertFalse(response.allowed,
                "Request with q exceeding maxChars should be rejected");
        assertTrue(response.reasons.toString().contains("maxChars"),
                "Reason should mention 'maxChars'");
    }

    @Test
    void queryWithinMaxCharsAccepted() {
        String query = "a".repeat(500);
        Map<String, String[]> request = Map.of(
                "q", new String[]{query},
                "fl", new String[]{"id"}
        );
        Response response = shield.evaluateRequest(request.entrySet(), 100000.0);
        assertTrue(response.allowed,
                "Request with q within maxChars should be accepted but got reasons: " + response.reasons);
    }


    @Test
    void weightCalculationSanity() {
        // Minimal request: q + fl with a single lightweight field
        Map<String, String[]> request = Map.of(
                "q", new String[]{"test"},
                "fl", new String[]{"id"}
        );
        Response response = shield.evaluateRequest(request.entrySet(), 100000.0);
        assertTrue(response.allowed, "Simple request should be allowed");

        double expectedMinWeight = 100 + 100 + 10; // At minimum: profile + search + q constants
        assertTrue(response.weight >= expectedMinWeight,
                "Weight should be at least " + expectedMinWeight + " but was " + response.weight);

        Response response2 = shield.evaluateRequest(request.entrySet(), 100000.0);
        assertEquals(response.weight, response2.weight,
                "Identical requests should produce identical weights (prototype integrity)");
    }

    @Test
    void moreRowsIncreasesWeight() {
        Map<String, String[]> requestRows10 = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"title"},
                "rows", new String[]{"10"}
        );
        Map<String, String[]> requestRows100 = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"title"},
                "rows", new String[]{"100"}
        );
        Response responseRows10 = shield.evaluateRequest(requestRows10.entrySet(), 100000.0);
        Response responseRows100 = shield.evaluateRequest(requestRows100.entrySet(), 100000.0);
        assertTrue(responseRows100.weight > responseRows10.weight,
                "More rows should increase weight. rows=10: " + responseRows10.weight + ", rows=100: " + responseRows100.weight);
    }

    @Test
    void heavierFieldsIncreaseWeight() {
        // id has weight 10, text has weight 200
        Map<String, String[]> requestLight = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"id"}
        );
        Map<String, String[]> requestHeavy = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"text"}
        );
        Response responseLight = shield.evaluateRequest(requestLight.entrySet(), 100000.0);
        Response responseHeavy = shield.evaluateRequest(requestHeavy.entrySet(), 100000.0);
        assertTrue(responseHeavy.weight > responseLight.weight,
                "Heavier field should increase weight. id: " + responseLight.weight + ", text: " + responseHeavy.weight);
    }


    @Test
    void facetFieldNotInWhitelistRejected() {
        Map<String, String[]> request = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"id"},
                "facet", new String[]{"true"},
                "facet.field", new String[]{"text"}
        );
        Response response = shield.evaluateRequest(request.entrySet(), Double.MAX_VALUE);
        assertFalse(response.allowed,
                "Facet on field not in allowedFields should be rejected");
        assertTrue(response.reasons.toString().contains("allowed list"),
                "Reason should mention the allowed list. Reasons: " + response.reasons);
    }

    @Test
    void facetFieldInWhitelistAccepted() {
        Map<String, String[]> request = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"id"},
                "facet", new String[]{"true"},
                "facet.field", new String[]{"catalog"}
        );
        Response response = shield.evaluateRequest(request.entrySet(), 100000.0);
        assertTrue(response.allowed,
                "Facet on field in allowedFields should be accepted but got reasons: " + response.reasons);
    }


    @Test
    void evaluateMapOverload() {
        Map<String, String[]> request = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"id"}
        );
        // evaluate(Map) uses defaultMaxWeight from config (1000)
        Response response = shield.evaluateRequest(request);
        assertTrue(response.allowed,
                "evaluate(Map) should work and accept simple request. Reasons: " + response.reasons);
        assertEquals(1000.0, response.maxWeight,
                "evaluate(Map) should use defaultMaxWeight from config");
    }


    @Test
    void defaultMaxWeightEnforced() {
        // defaultMaxWeight is 1000 in test config
        // Request with heavy fields + many rows should exceed it
        Map<String, String[]> request = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"text", "freetext", "abstract"},
                "rows", new String[]{"500"}
        );
        Response response = shield.evaluateRequest(request);
        assertFalse(response.allowed,
                "Heavy request should exceed defaultMaxWeight of 1000");
        assertTrue(response.reasons.toString().contains("Weight exceeded"),
                "Reason should mention weight exceeded");
    }

    @Test
    void facetQueryDenied() {
        Map<String, String[]> request = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"id"},
                "facet", new String[]{"true"},
                "facet.query", new String[]{"genre:drama"}
        );
        Response response = shield.evaluateRequest(request.entrySet(), Double.MAX_VALUE);
        assertFalse(response.allowed,
                "facet.query should be rejected (allowed=false in config)");
        assertTrue(response.reasons.toString().contains("facet.query"),
                "Reason should mention 'facet.query'");
    }

    @Test
    void flDeniedField() {
        Map<String, String[]> request = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"text_shingles"}
        );
        Response response = shield.evaluateRequest(request.entrySet(), Double.MAX_VALUE);
        assertFalse(response.allowed,
                "Requesting denied field 'text_shingles' in fl should be rejected");
        assertTrue(response.reasons.toString().contains("denied"),
                "Reason should mention denied list");
    }

    @Test
    void multipleReasonsCollected() {
        Map<String, String[]> request = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"text_shingles"},   // denied field in search component
                "facet", new String[]{"true"},
                "facet.field", new String[]{"catalog"},
                "facet.limit", new String[]{"50000"}   // exceeds maxValue in facet component
        );
        Response response = shield.evaluateRequest(request.entrySet(), Double.MAX_VALUE);
        assertFalse(response.allowed);
        assertTrue(response.reasons.size() >= 2,
                "Should collect multiple failure reasons but got: " + response.reasons);
    }

    @Test
    void debugAddsSignificantWeight() {
        Map<String, String[]> requestNoDebug = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"id"},
                "rows", new String[]{"1"}
        );
        Map<String, String[]> requestDebug = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"id"},
                "rows", new String[]{"1"},
                "debug", new String[]{"true"}
        );
        Response responseNoDebug = shield.evaluateRequest(requestNoDebug.entrySet(), 100000.0);
        Response responseDebug = shield.evaluateRequest(requestDebug.entrySet(), 100000.0);
        assertTrue(responseDebug.weight > responseNoDebug.weight + 400,
                "Debug should add at least 500 weight (weightConstant=500). " +
                        "Without: " + responseNoDebug.weight + ", with: " + responseDebug.weight);
    }

    @Test
    void perCollectionShieldLoaded() {
        YAML config = buildCollectionConfig(Map.of(
                "permissive-collection", "solrshield-permissive.yaml",
                "restrictive-collection", "solrshield-restrictive.yaml"
        ));
        SolrManager.getInstance().setConfig(config);

        try {
            Optional<SolrShield> permissive = SolrManager.getShield("permissive-collection");
            Optional<SolrShield> restrictive = SolrManager.getShield("restrictive-collection");

            assertTrue(permissive.isPresent(), "Permissive collection should have a shield");
            assertTrue(restrictive.isPresent(), "Restrictive collection should have a shield");
            assertNotSame(permissive.get(), restrictive.get(),
                    "Different collections should have different shield instances");
        } finally {
            // Restore empty state so other tests aren't affected
            SolrManager.getInstance().setConfig(buildCollectionConfig(Map.of()));
        }
    }

    @Test
    void noShieldForUnconfiguredCollection() {
        Map<String, String> collections = new LinkedHashMap<>();
        collections.put("permissive-collection", "solrshield-permissive.yaml");
        collections.put("noshield-collection", null);
        YAML config = buildCollectionConfig(collections);
        SolrManager.getInstance().setConfig(config);

        try {
            Optional<SolrShield> noShield = SolrManager.getShield("noshield-collection");
            assertTrue(noShield.isEmpty(), "Collection without shield config should return empty");
        } finally {
            SolrManager.getInstance().setConfig(buildCollectionConfig(Map.of()));
        }
    }

    @Test
    void permissiveShieldAllowsBasicQuery() {
        YAML config = buildCollectionConfig(Map.of(
                "permissive-collection", "solrshield-permissive.yaml"
        ));
        SolrManager.getInstance().setConfig(config);

        try {
            SolrShield shield = SolrManager.getShield("permissive-collection").orElseThrow();
            Map<String, String[]> request = Map.of(
                    "q", new String[]{"*:*"},
                    "fl", new String[]{"title"},
                    "rows", new String[]{"50"}
            );
            Response response = shield.evaluateRequest(request);
            assertTrue(response.allowed,
                    "Permissive shield should allow basic query. Reasons: " + response.reasons);
        } finally {
            SolrManager.getInstance().setConfig(buildCollectionConfig(Map.of()));
        }
    }

    @Test
    void restrictiveShieldRejectsBasicQuery() {
        YAML config = buildCollectionConfig(Map.of(
                "restrictive-collection", "solrshield-restrictive.yaml"
        ));
        SolrManager.getInstance().setConfig(config);

        try {
            SolrShield shield = SolrManager.getShield("restrictive-collection").orElseThrow();
            // defaultMaxWeight=100, but weightConstant(100) + search.weightConstant(100) already exceeds it
            Map<String, String[]> request = Map.of(
                    "q", new String[]{"*:*"},
                    "fl", new String[]{"id"}
            );
            Response response = shield.evaluateRequest(request);
            assertFalse(response.allowed,
                    "Restrictive shield (maxWeight=100) should reject even basic queries. " +
                            "Weight: " + response.weight);
        } finally {
            SolrManager.getInstance().setConfig(buildCollectionConfig(Map.of()));
        }
    }

    @Test
    void sameQueryDifferentResultPerCollection() {
        YAML config = buildCollectionConfig(Map.of(
                "permissive-collection", "solrshield-permissive.yaml",
                "restrictive-collection", "solrshield-restrictive.yaml"
        ));
        SolrManager.getInstance().setConfig(config);

        try {
            SolrShield permissive = SolrManager.getShield("permissive-collection").orElseThrow();
            SolrShield restrictive = SolrManager.getShield("restrictive-collection").orElseThrow();

            Map<String, String[]> request = Map.of(
                    "q", new String[]{"*:*"},
                    "fl", new String[]{"id"}
            );

            Response permissiveResponse = permissive.evaluateRequest(request);
            Response restrictiveResponse = restrictive.evaluateRequest(request);

            assertTrue(permissiveResponse.allowed,
                    "Permissive shield should allow the query. Reasons: " + permissiveResponse.reasons);
            assertFalse(restrictiveResponse.allowed,
                    "Restrictive shield should reject the same query. Weight: " + restrictiveResponse.weight);
        } finally {
            SolrManager.getInstance().setConfig(buildCollectionConfig(Map.of()));
        }
    }

    @Test
    void restrictiveShieldDeniesTextField() {
        YAML config = buildCollectionConfig(Map.of(
                "restrictive-collection", "solrshield-restrictive.yaml"
        ));
        SolrManager.getInstance().setConfig(config);

        try {
            SolrShield shield = SolrManager.getShield("restrictive-collection").orElseThrow();
            Map<String, String[]> request = Map.of(
                    "q", new String[]{"*:*"},
                    "fl", new String[]{"text"}
            );
            Response response = shield.evaluateRequest(request.entrySet(), Double.MAX_VALUE);
            assertFalse(response.allowed,
                    "Restrictive shield should deny field 'text'. Reasons: " + response.reasons);
            assertTrue(response.reasons.toString().contains("denied"),
                    "Reason should mention denied list");
        } finally {
            SolrManager.getInstance().setConfig(buildCollectionConfig(Map.of()));
        }
    }

    @Test
    void permissiveShieldAllowsTextField() {
        YAML config = buildCollectionConfig(Map.of(
                "permissive-collection", "solrshield-permissive.yaml"
        ));
        SolrManager.getInstance().setConfig(config);

        try {
            SolrShield shield = SolrManager.getShield("permissive-collection").orElseThrow();
            // 'text' is NOT denied in the permissive shield
            Map<String, String[]> request = Map.of(
                    "q", new String[]{"*:*"},
                    "fl", new String[]{"text"}
            );
            Response response = shield.evaluateRequest(request.entrySet(), Double.MAX_VALUE);
            assertTrue(response.allowed,
                    "Permissive shield should allow field 'text'. Reasons: " + response.reasons);
        } finally {
            SolrManager.getInstance().setConfig(buildCollectionConfig(Map.of()));
        }
    }

    private String toString(Map<String, String[]> map) {
        return map.entrySet().stream()
                .map(e -> e.getKey() + "=" + Arrays.toString(e.getValue()))
                .collect(Collectors.joining("\", \"", "[\"", "\"]"));
    }
}
