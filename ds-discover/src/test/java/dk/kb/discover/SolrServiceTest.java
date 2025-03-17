package dk.kb.discover;

import dk.kb.discover.api.v1.impl.DsDiscoverApiServiceImpl;
import dk.kb.discover.config.ServiceConfig;
import dk.kb.discover.util.DsDiscoverClient;
import dk.kb.discover.util.SolrParamMerger;
import dk.kb.util.oauth2.KeycloakUtil;
import dk.kb.util.webservice.OAuthConstants;

import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.MessageImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

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
class SolrServiceTest {

    private static final Logger log = LoggerFactory.getLogger(SolrServiceTest.class);
    
    private static DsDiscoverClient remote = null;
    private static String dsDiscoverDevel=null;  
    
    @BeforeAll
    static void setUp() throws Exception{
        try {
            ServiceConfig.initialize("ds-discover-integration-test.yaml"); 
            dsDiscoverDevel= ServiceConfig.getConfig().getString("discover.url");
            remote = new DsDiscoverClient(dsDiscoverDevel);
        } catch (IOException e) { 
            e.printStackTrace();
            log.error("Integration yaml 'ds-discover-integration-test.yaml' file most be present. Call 'kb init'"); 
            fail();
        }
        
        try {            
            String keyCloakRealmUrl= ServiceConfig.getConfig().getString("integration.devel.keycloak.realmUrl");            
            String clientId=ServiceConfig.getConfig().getString("integration.devel.keycloak.clientId");
            String clientSecret=ServiceConfig.getConfig().getString("integration.devel.keycloak.clientSecret");                
            String token=KeycloakUtil.getKeycloakAccessToken(keyCloakRealmUrl, clientId, clientSecret);           
            log.info("Retrieved keycloak access token:"+token);            
            
            //Mock that we have a JaxRS session with an Oauth token as seen from within a service call.
            MessageImpl message = new MessageImpl();                            
            message.put(OAuthConstants.ACCESS_TOKEN_STRING,token);            
            MockedStatic<JAXRSUtils> mocked = mockStatic(JAXRSUtils.class);           
            mocked.when(JAXRSUtils::getCurrentMessage).thenReturn(message);
                                                                         
        }
        catch(Exception e) {
            log.warn("Could not retrieve keycloak access token. Service will be called without Bearer access token");            
            e.printStackTrace();
        }                        
    }
    
    // Integration test: Requires a local solr at port 10007 from the Digitale Samlinger project with
    // a ds-collection
    //@Test
    void baseSearch() {
        SolrService solr = new SolrService("test", "http://localhost:10007", "solr", "ds");
        String response = solr.query("*:*", null, null, null, null, null, null, null, null, null, null, null, null, null,null,null,null,null,null,null,null,null,null,null,null,null);
        assertTrue(response.contains("responseHeader"), "The Solr response should contain a header");
    }

    @Test
    void stripFilterJSONMulti() {
        String prefix = DsDiscoverApiServiceImpl.FILTER_CACHE_PREFIX;
        String response = "\"fq\":[\n" +
                "        \"number_of_episodes:[2 TO 10]\",\n" +
                "        \"resource_description:[* TO \\\"Moving Image\\\"]\",\n" +
                "        \"{!cache=true}(((access_searlige_visningsvilkaar:\\\"Visning kun af metadata\\\") OR (catalog:\\\"Maps\\\") OR (collection:\\\"Det Kgl. Bibliotek; Radio/TV-Samlingen\\\") OR (catalog:\\\"Samlingsbilleder\\\")) -(id:(\\\"fr508045.tif\\\" OR \\\"fr552041x.tif\\\")) -(access_blokeret:true) -(cataloging_language:*tysk*))\"\n" +
                "      ],\n";
        String exp = "\"fq\":[\n" +
                "        \"number_of_episodes:[2 TO 10]\",\n" +
                "        \"resource_description:[* TO \\\"Moving Image\\\"]\"\n" + // Note no comma
                "      ],\n";
        assertEquals(exp, SolrService.removePrefixedFilters(response, prefix,"json"));
    }

    @Test
    void stripFilterJSONMulti2() {
        String prefix = DsDiscoverApiServiceImpl.FILTER_CACHE_PREFIX;
        String response = "      \"q.op\":\"OR\",\n" +
                "      \"fq\":[\"catalog:\\\"Samlingsbilleder\\\"\",\n" +
                "        \"{!cache=true}(((access_searlige_visningsvilkaar:\\\"Visning kun af metadata\\\") OR (catalog:\\\"Maps\\\") OR (collection:\\\"Det Kgl. Bibliotek; Radio/TV-Samlingen\\\") OR (catalog:\\\"Samlingsbilleder\\\")) -(id:(\\\"fr508045.tif\\\" OR \\\"fr552041x.tif\\\")) -(access_blokeret:true) -(cataloging_language:*tysk*))\"],\n" +
                "      \"rows\":\"10\",\n" +
                "      \"wt\":\"json\",";
        String exp = "      \"q.op\":\"OR\",\n" +
                "      \"fq\": \"catalog:\\\"Samlingsbilleder\\\"\"" + ",\n" + // Note no brackets
                "      \"rows\":\"10\",\n" +
                "      \"wt\":\"json\",";
        assertEquals(exp, SolrService.removePrefixedFilters(response, prefix,"json"));
    }

    @Test
    void stripFilterJSONSingle() {
        String prefix = DsDiscoverApiServiceImpl.FILTER_CACHE_PREFIX;
        String response = "  \"fq\":[\n" +
                "        \"{!cache=true}(((access_searlige_visningsvilkaar:\\\"Visning kun af metadata\\\") OR (catalog:\\\"Maps\\\") OR (collection:\\\"Det Kgl. Bibliotek; Radio/TV-Samlingen\\\") OR (catalog:\\\"Samlingsbilleder\\\")) -(id:(\\\"fr508045.tif\\\" OR \\\"fr552041x.tif\\\")) -(access_blokeret:true) -(cataloging_language:*tysk*))\"\n" +
                "      ],\n";
        String exp = "";
        assertEquals(exp, SolrService.removePrefixedFilters(response, prefix,"json"));
    }

    @Test
    void stripFilterJSONNone() {
        String prefix = DsDiscoverApiServiceImpl.FILTER_CACHE_PREFIX;
        String response = "  \"fq\":[\n" +
                "        \"number_of_episodes:[2 TO 10]\",\n" +
                "        \"resource_description:[* TO \\\"Moving Image\\\"]\"\n" +
                "      ],\n";
        assertThrows(IllegalArgumentException.class,
                () -> SolrService.removePrefixedFilters(response, prefix,"json"));
    }

    @Test
    void stripFilterXMLMulti() {
        String prefix = DsDiscoverApiServiceImpl.FILTER_CACHE_PREFIX;
        String response = "  <str name=\"q.op\">AND</str>\n" +
                "    <arr name=\"fq\">\n" +
                "      <str>number_of_episodes:[2 TO 10]</str>\n" +
                "      <str>resource_description:[* TO \"Moving Image\"]</str>\n" +
                "      <str>{!cache=true}(((access_searlige_visningsvilkaar:\"Visning kun af metadata\") OR (catalog:\"Maps\") OR (collection:\"Det Kgl. Bibliotek; Radio/TV-Samlingen\") OR (catalog:\"Samlingsbilleder\")) -(id:(\"fr508045.tif\" OR \"fr552041x.tif\")) -(access_blokeret:true) -(cataloging_language:*tysk*))</str>\n" +
                "    </arr>\n";
        String exp = "  <str name=\"q.op\">AND</str>\n" +
                "    <arr name=\"fq\">\n" +
                "      <str>number_of_episodes:[2 TO 10]</str>\n" +
                "      <str>resource_description:[* TO \"Moving Image\"]</str>\n" +
                "    </arr>\n";
        assertEquals(exp, SolrService.removePrefixedFilters(response, prefix,"xml"));
    }

    @Test
    void stripFilterXMLSingle() {
        String prefix = DsDiscoverApiServiceImpl.FILTER_CACHE_PREFIX;
        String response = "<str name=\"q.op\">AND</str>\n" +
                "    <arr name=\"fq\">\n" +
                "      <str>{!cache=true}(((access_searlige_visningsvilkaar:\"Visning kun af metadata\") OR (catalog:\"Maps\") OR (collection:\"Det Kgl. Bibliotek; Radio/TV-Samlingen\") OR (catalog:\"Samlingsbilleder\")) -(id:(\"fr508045.tif\" OR \"fr552041x.tif\")) -(access_blokeret:true) -(cataloging_language:*tysk*))</str>\n" +
                "    </arr>\n";
        String exp = "<str name=\"q.op\">AND</str>\n";
        assertEquals(exp, SolrService.removePrefixedFilters(response, prefix,"xml"));
    }

    @Test
    void stripFilterXMLNone() {
        String prefix = DsDiscoverApiServiceImpl.FILTER_CACHE_PREFIX;
        String response = "<str name=\"q.op\">AND</str>\n" +
                "    <arr name=\"fq\">\n" +
                "      <str>number_of_episodes:[2 TO 10]</str>\n" +
                "    </arr>\n";
        assertThrows(IllegalArgumentException.class,
                () -> SolrService.removePrefixedFilters(response, prefix,"xml"));
    }

    @Test
    @Tag("integration")
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
    @Tag("integration")
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