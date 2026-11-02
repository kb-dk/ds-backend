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
package dk.kb.util.webservice;

import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests {@link OpenApiResource#resolveWildcard(String)}, in particular the bare {@code *} name-wildcard segment
 * added to support ds-present's {@code origins[*].*.origin} configuration placeholder, where each {@code origins}
 * list entry is a single-key map keyed by a dynamic (and possibly dotted, e.g. {@code "ds.radio"}) origin ID -
 * exactly how {@code dk.kb.present.config.ServiceConfig} flattens that section for its {@code getFlatConfig()}.
 */
class OpenApiResourceWildcardTest {

    @Test
    void plainIndexWildcardMatchesInAscendingOrder() {
        OpenApiResource.setConfig(configOf(Map.of(
                "origins[0].name", "radio",
                "origins[1].name", "tv",
                "origins[10].name", "web" // Exercises numeric (not lexical) ordering past a single digit.
        )));

        List<String> result = OpenApiResource.resolveWildcard("origins[*].name");

        assertEquals(List.of("radio", "tv", "web"), result);
    }

    @Test
    void combinedIndexAndNameWildcardMatchesDottedDynamicKey() {
        // Mirrors dk.kb.present.config.ServiceConfig's flattening of ds-present's origins configuration, where
        // each list entry is a single-key map and the key itself ("ds.radio") is a dynamic ID that may contain
        // literal dots - not a further nested path segment.
        OpenApiResource.setConfig(configOf(Map.of(
                "origins[0].ds.radio.origin", "ds.radio",
                "origins[1].ds.tv.origin", "ds.tv",
                "origins[2].remote.origin", "old.doms.radio"
        )));

        List<String> result = OpenApiResource.resolveWildcard("origins[*].*.origin");

        assertEquals(List.of("ds.radio", "ds.tv", "old.doms.radio"), result);
    }

    @Test
    void noMatchesReturnsEmptyList() {
        OpenApiResource.setConfig(configOf(Map.of("unrelated.key", "value")));

        assertEquals(List.of(), OpenApiResource.resolveWildcard("origins[*].*.origin"));
    }

    private static SmallRyeConfig configOf(Map<String, String> properties) {
        return new SmallRyeConfigBuilder()
                .withSources(new FixedConfigSource(properties))
                .build();
    }

    private static final class FixedConfigSource implements ConfigSource {
        private final Map<String, String> properties;

        FixedConfigSource(Map<String, String> properties) {
            this.properties = new LinkedHashMap<>(properties);
        }

        @Override
        public Map<String, String> getProperties() {
            return properties;
        }

        @Override
        public Set<String> getPropertyNames() {
            return properties.keySet();
        }

        @Override
        public String getValue(String propertyName) {
            return properties.get(propertyName);
        }

        @Override
        public String getName() {
            return "test-fixed-config-source";
        }
    }
}
