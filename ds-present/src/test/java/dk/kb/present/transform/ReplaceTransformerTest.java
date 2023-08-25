package dk.kb.present.transform;

import dk.kb.util.yaml.YAML;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;

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
class ReplaceTransformerTest {

    private final ReplaceFactory replaceFactory = new ReplaceFactory();

    @Test
    void testSimpleReplaceAll() {
        DSTransformer replacer = getReplacer(
                "regexp: \"a\"\n" +
                "replacement: \"z\""
        );
        assertEquals("zz-top", replacer.apply("aa-top", null));
    }

    @Test
    void testSimpleReplaceFirst() {
        DSTransformer replacer = getReplacer(
                "regexp: \"a\"\n" +
                "replacement: \"z\"\n" +
                "replaceall: false"
        );
        assertEquals("za-top", replacer.apply("aa-top", null));
    }

    @Test
    void testGrouping() {
        DSTransformer replacer = new ReplaceTransformer("id=\"([0-9]+)-([a-z]+)\"", "id=\"$2-$1\"", false);
        assertEquals("<mystructure id=\"foo-123\">...", replacer.apply("<mystructure id=\"123-foo\">...", null));
    }

    @Test
    void testXIP() {
        DSTransformer replacer = new ReplaceTransformer(
                "<xip:(DeliverableUnit|Collection|Manifestation) +status=\"([^\"]+)\" *>",
                "<xip:$1 status=\"$2\" xmlns:xip=\"http://example.com/\">", false);

        for (String element : new String[]{"DeliverableUnit", "Collection", "Manifestation"}) {
            assertEquals("<xip:" + element + " status=\"foo\" xmlns:xip=\"http://example.com/\">",
                    replacer.apply("<xip:" + element + " status=\"foo\">", null),
                    "Namespace injection should work for element '" + element + "'");
        }
        assertNotEquals("<xip:Selfmade status=\"foo\" xmlns:xip=\"http://example.com/\">",
                replacer.apply("<xip:Selfmade status=\"foo\">", null),
                "Namespace injection should NOT work for element 'Selfmade'");
    }
    
    /**
     * Use the {@link ReplaceFactory} to create a {@link ReplaceTransformer} with config taken from {@code yamlString}.
     * @param yamlString configuration for {@link ReplaceTransformer}.
     * @return a {@link ReplaceTransformer}.
     */
    private DSTransformer getReplacer(String yamlString) {
        try (InputStream in = new ByteArrayInputStream(yamlString.getBytes(StandardCharsets.UTF_8))) {
            YAML yaml = YAML.parse(in);
            return replaceFactory.createTransformer(yaml);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}