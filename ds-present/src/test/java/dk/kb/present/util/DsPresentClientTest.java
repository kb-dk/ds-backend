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
package dk.kb.present.util;

import dk.kb.present.webservice.AccessUtil;
import dk.kb.present.config.ServiceConfig;
import dk.kb.present.model.v1.FormatDto;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.util.Resolver;
import dk.kb.util.webservice.stream.ContinuationInputStream;
import dk.kb.util.webservice.stream.ContinuationStream;
import dk.kb.util.webservice.stream.ContinuationUtil;
import dk.kb.util.yaml.YAML;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Provider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test, will not be run by automatic build flow.
 * 
 */

@Tag("integration")
public class DsPresentClientTest {
    private static final Logger log = LoggerFactory.getLogger(DsPresentClientTest.class);

    public static final String TEST_CONF = "internal-test-setup.yaml";

    private static DsPresentClient remote = null;

    @BeforeAll
    static void setup() {
        try {
            ServiceConfig.initialize(TEST_CONF);                        
            remote = new DsPresentClient(ServiceConfig.getConfig());
        } catch (IOException e) {          
            log.error("Integration yaml "+TEST_CONF+" file most be present. Call 'kb init'");            
            fail();
        }
    }

    @Test
    public void testRemoteRecordsRaw() throws IOException {      
        try (ContinuationInputStream<Long> recordsIS = remote.getRecordsJSON(
                "ds.tv", 0L, 3L, FormatDto.JSON_LD)) {
            String recordsStr = IOUtils.toString(recordsIS, StandardCharsets.UTF_8);
            assertTrue(recordsStr.contains("\"id\":\"ds.tv:oai"),
                    "At least 1 JSON block for a record should be returned");
            assertNotNull(recordsIS.getContinuationToken(),
                    "The continuation header '" + ContinuationUtil.HEADER_PAGING_CONTINUATION_TOKEN +
                            "' should be present");
            assertTrue(ContinuationUtil.getHasMore(recordsIS).isPresent(),
                       "The continuation header '" + ContinuationUtil.HEADER_PAGING_HAS_MORE + "' should be present");
        }
    }

    
    @Test
    public void testRemoteRecordsStream() throws IOException {
       
        try (ContinuationStream<DsRecordDto, Long> records = remote.getRecordsRawStream("ds.tv", 0L, 3L)) {
            List<DsRecordDto> recordList = records.collect(Collectors.toList());

            assertEquals(3L, recordList.size(), "The requested number of records should be received");
            assertNotNull(records.getContinuationToken(),
                    "The highest modification time should be present");
            log.debug("Stated highest modification time was " + records.getContinuationToken());
            assertEquals(recordList.get(recordList.size()-1).getmTime(),
                         records.getContinuationToken(),
                    "Received highest mTime should match stated highest mTime");
        }
    }

    @Test
    public void testRecordCount() throws IOException {       
        try (ContinuationInputStream<Long> records = remote.getRecordsJSON("ds.tv", 0L, 1L, FormatDto.JSON_LD)) {
            // Here we are implying that there are at least 1 record in the backing storage,
            // which are "Deliverable units", in other words, records that contain actual metadata and are delivered.
            assertEquals(1L, records.getRecordCount());
        }
    }

    @Test
    public void testSchemaTransformation() throws IOException {
        String schema = Resolver.resolveUTF8String("src/main/solr/dssolr/conf/schema.xml");

        String result = remote.transformSolrSchema(schema, "markdown");
        assertTrue(result.startsWith("# Schema documentation"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void getFixedHeaders() {
        YAML conf = ServiceConfig.getConfig();
        Map<String, String> newHeaders= Map.of("Simulated-OAuth2-Group", "anonymous",
                                        "Some-Other-Header", "foo");
        Map<String, String> immutableHeaders = DsPresentClient.getAllHeaders(conf, newHeaders);
        Map<String, String> headers = new HashMap<>(immutableHeaders);
        assertEquals(2, headers.size(),
                "The right number of headers should be extracted");
        assertEquals("anonymous", headers.get(AccessUtil.HEADER_SIMULATED_GROUP),
                "The group header should be correct");
    }
}
