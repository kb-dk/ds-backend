package dk.kb.discover;

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
        String response = solr.query("*:*", null, null, null, null, null, null, null, null, null, null, null);
        assertTrue(response.contains("responseHeader"), "The Solr response should contain a header");
    }
}