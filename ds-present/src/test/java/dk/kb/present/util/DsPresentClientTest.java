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

import dk.kb.present.model.v1.FormatDto;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.util.webservice.stream.ContinuationInputStream;
import dk.kb.util.webservice.stream.ContinuationStream;
import dk.kb.util.webservice.stream.ContinuationUtil;
import dk.kb.util.yaml.YAML;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Simple verification of client code generation.
 */
public class DsPresentClientTest {
    private static final Logger log = LoggerFactory.getLogger(DsPresentClientTest.class);

    public static final String TEST_CONF = "internal-test-setup.yaml";

    private static DsPresentClient remote = null;

    @BeforeAll
    public static void beforeClass() {
        remote = getRemote();
    }

    // We cannot test usage as that would require a running instance of ds-present to connect to
    @Test
    public void testInstantiation() {
        String backendURIString = "https://example.com/ds-present/v1";
        log.debug("Creating inactive client for ds-present with URI '{}'", backendURIString);
        new DsPresentClient(backendURIString);
    }

    @Test
    public void testRemoteRecordsRaw() throws IOException {
        if (remote == null) {
            return;
        }
        try (ContinuationInputStream<Long> recordsIS = remote.getRecordsJSON(
                "ds.radiotv", 0L, 3L, FormatDto.JSON_LD)) {
            String recordsStr = IOUtils.toString(recordsIS, StandardCharsets.UTF_8);
            assertTrue(recordsStr.contains("\"id\":\"ds.radiotv:oai"),
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
       if (remote == null) {
            return;
        }
        try (ContinuationStream<DsRecordDto, Long> records = remote.getRecordsRawStream("ds.radiotv", 0L, 3L)) {
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

    /**
     * @return a {@link DsPresentClient} if a KB-internal remote storage is specified and is available.
     */
    private static DsPresentClient getRemote() {
        YAML config;
        try {
            config = YAML.resolveLayeredConfigs(TEST_CONF);
        } catch (Exception e) {
            log.info("Unable to resolve '{}' (try running 'kb init'). Skipping test", TEST_CONF);
            return null;
        }
        String presentURL = config.getString(DsPresentClient.PRESENT_SERVER_URL_KEY, null);
        if (presentURL == null) {
            log.info("Resolved internal config '{}' but could not retrieve a value for key '{}'. Skipping test",
                    TEST_CONF, DsPresentClient.PRESENT_SERVER_URL_KEY);
            return null;
        }
        DsPresentClient client = new DsPresentClient(presentURL);
        try {
            client.service.status(); // We cannot use ping as it does not return JSON. This is a problem!
        } catch (Exception e) {
            log.debug("Exc", e);
            log.info("Found ds-storage address '{}' but could not establish contact. Skipping test", presentURL);
            return null;
        }
        log.debug("Established connection to remote ds-present at '{}'", presentURL);
        return client;
    }

}
