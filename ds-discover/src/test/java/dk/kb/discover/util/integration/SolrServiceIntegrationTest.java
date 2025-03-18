package dk.kb.discover.util.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import dk.kb.discover.SolrManager;
import dk.kb.discover.SolrService;
import dk.kb.discover.config.ServiceConfig;


/**
 * Integration test: Requires a local solr at port 10007 from the Digitale Samlinger project with a ds-collection
 *  
 */

@Tag("integration")
public class SolrServiceIntegrationTest extends IntegrationTest {
          
    @Test
    void suggestTestNotAvailable() throws IOException {
        // Integration test towards devel env. Remember to update aegis before running this.
        ServiceConfig.initialize("src/test/resources/ds-discover-integration-test.yaml");
        String suggestDictionary = "radiotv_title_suggest";
        // no suggestions should be available for this query.
        String suggestQuery = "Palle Lauring";
        int suggestCount = 5;
        String wt = "json";
        SolrService solr = SolrManager.getSolrService("ds");

        String filteredResponse = solr.suggest(suggestDictionary, suggestQuery, suggestCount, wt);

        assertTrue(filteredResponse.contains("\"suggest\" : {\n" +
                "    \"radiotv_title_suggest\" : {\n" +
                "      \"Palle Lauring\" : {\n" +
                "        \"numFound\" : 0,\n" +
                "        \"suggestions\" : [ ]\n" +
                "      }\n" +
                "    }\n" +
                "  }"));
    }

    @Test
    void suggestTest() throws IOException {
        // Integration test towards devel env. Remember to update aegis before running this.
        ServiceConfig.initialize("src/test/resources/ds-discover-integration-test.yaml");
        String suggestDictionary = "radiotv_title_suggest";
        // no suggestions should be available for this query.
        String suggestQuery = "tes";
        int suggestCount = 5;
        String wt = "json";
        SolrService solr = SolrManager.getSolrService("ds");

        String filteredResponse = solr.suggest(suggestDictionary, suggestQuery, suggestCount, wt);

        System.out.println(filteredResponse);

        assertTrue(filteredResponse.contains("\"suggest\" : {\n" +
                "    \"radiotv_title_suggest\" : {\n" +
                "      \"tes\" : {\n" +
                "        \"numFound\" : 8,\n"));  //This number will change depending on corpus
    }

}
