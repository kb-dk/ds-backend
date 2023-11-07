package dk.kb.discover;

import dk.kb.discover.api.v1.impl.DsDiscoverApiServiceImpl;
import org.junit.jupiter.api.Test;

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
class SolrServiceTest {

    // Integration test: Requires a local solr at port 10007 from the Digitale Samlinger project with
    // a ds-collection
    //@Test
    void baseSearch() {
        SolrService solr = new SolrService("test", "http://localhost:10007", "solr", "ds");
        String response = solr.query("*:*", null, null, null, null, null, null, null, null, null, null, null, null, null);
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
        String exp = "<str name=\"q.op\">AND</str>\n" +
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
}