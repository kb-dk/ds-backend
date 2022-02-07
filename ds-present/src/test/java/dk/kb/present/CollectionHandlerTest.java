package dk.kb.present;

import dk.kb.util.yaml.YAML;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.regex.Pattern;

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
class CollectionHandlerTest {

    @Test
    void idPattern() {
        final Pattern recordIDPattern = Pattern.compile("^([a-z0-9.]+):([a-z0-9._-]+)$");
        final String ID = "images.dsfl:luftfoto-sample.xml";
        assertTrue(recordIDPattern.matcher(ID).matches());
    }

    @Test
    void localCorpusMODS() throws IOException {
        YAML conf = YAML.resolveLayeredConfigs("test_setup.yaml");
        CollectionHandler ch = new CollectionHandler(conf);
        String record = ch.getRecord("local_henrik-hertz.xml", "mods");
        assertTrue(record.contains("<forename>Henrik</forename>"));
    }

    @Test
    void localCorpusFail() throws IOException {
        YAML conf = YAML.resolveLayeredConfigs("test_setup.yaml");
        CollectionHandler ch = new CollectionHandler(conf);
        try {
            ch.getRecord("local_henrik-hertz.xml", "raw");
            fail("Requesting record in raw format should fail");
        } catch (Exception e) {
            // Expected
        }
    }
}