package dk.kb.present;

import dk.kb.util.yaml.YAML;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.regex.Matcher;
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
class OriginHandlerTest {

    @Test
    void idPattern() {
        final Pattern recordIDPattern = Pattern.compile("([a-z0-9.]+):([a-zA-Z0-9:._-]+)");
        for (String TEST: new String[]{
                "1::", // Degenerate example
                ".::_..-", // Degenerate example
                "images.dsfl:luftfoto-sample.xml",
                "images:luftfoto:sample.xml",
                "oai:kb.dk:images:luftfo:2011:maj:luftfoto:object967062"
        }) {
            assertTrue(recordIDPattern.matcher(TEST).matches());
        }

    }

    @Test
    void demoPattern() {
        final Pattern recordIDPattern = Pattern.compile("([a-z0-9.]+):([a-zA-Z0-9:._-]+)");
        String id = "images.dsfl:luftfoto-sample.xml";
        Matcher matcher = recordIDPattern.matcher(id);
        if (matcher.matches()) {
            System.out.println("Collection=" + matcher.group(1) + ", material-id=" + matcher.group(2));
        } else {
            throw new IllegalArgumentException("Unsupported ID format '" + id + "'");
        }
    }

    @Test
    void normaliseID() {
        final Pattern NO_GO = Pattern.compile("[^a-zA-Z0-9:._-]");

        // Note: Not a proper id as the collection is not added
        String id = "himmel/luftfoto-sampleæøå .xml";
        for (String[] subst: new String[][]{
                {"æ", "ae"}, {"ä", "ae"}, {"Æ", "Ae"}, {"Ä", "Ae"},
                {"ø", "oe"}, {"ö", "oe"}, {"Ø", "Oe"}, {"Ö", "Oe"},
                {"å", "aa"}, {"Å", "Aa"},
                {" ", "-"}, {"/", "-"}, {"~", "-"}
        }) {
            id = id.replace(subst[0], subst[1]);
        }
        id = NO_GO.matcher(id).replaceAll(".");

        System.out.println("Prepend collection and colon to this: " + id);
    }



    @Test
    void localCorpusMODS() throws IOException {
        YAML conf = YAML.resolveLayeredConfigs("test_setup.yaml");
        OriginHandler ch = new OriginHandler(conf);
        String record = ch.getRecord("local.mods:40221e30-1414-11e9-8fb8-00505688346e.xml", "mods");
        assertTrue(record.contains("<mods:title>Christian VIII</mods:title>"));
    }

    @Test
    void localCorpusPvica() throws IOException {
        YAML conf = YAML.resolveLayeredConfigs("test_setup.yaml");
        OriginHandler ch = new OriginHandler(conf);
        String record = ch.getRecord("local.radiotv:9d9785a8-71f4-4b34-9a0e-1c99c13b001b.xml", "json-ld");
        assertTrue(record.contains("\"id\":\"local.radiotv:9d9785a8-71f4-4b34-9a0e-1c99c13b001b.xml\""));
    }

    @Test
    void testManifestationFiltering() throws IOException {
        // This test checks that the correct filtering is applied in DSOrigin.getFirstChild()
        // The FileStorage used for testing appends two children to each record. One with referenceType = 1 and one with
        // referenceType = 2. Only children with type = 2 should be returned as these are presentation manifestations.
        YAML conf = YAML.resolveLayeredConfigs("test_setup.yaml");
        OriginHandler ch = new OriginHandler(conf);
        String record = ch.getRecord("local.radiotv:9d9785a8-71f4-4b34-9a0e-1c99c13b001b.xml", "json-ld");
        assertTrue(record.contains("correct-reference\\/playlist.m3u8"));
        assertFalse(record.contains("wrong-reference\\/playlist.m3u8"));
    }


    @Test
    void localCorpusFail() throws IOException {
        YAML conf = YAML.resolveLayeredConfigs("test_setup.yaml");
        OriginHandler ch = new OriginHandler(conf);
        try {
            ch.getRecord("local:40221e30-1414-11e9-8fb8-00505688346e.xml", "raw");
            fail("Requesting record in raw format should fail");
        } catch (Exception e) {
            // Expected
        }
    }

}