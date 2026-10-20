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
package dk.kb.image.config;

import dk.kb.util.Resolver;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Demonstrates the explicit, per-service devops/operations properties override file used in production/on the
 * shared development Tomcat instance: {@link ServiceConfig#initialize(String, String)}'s second argument,
 * populated in practice from the {@code application-properties-config} JNDI entry (see
 * {@link dk.kb.image.webservice.ContextListener} and {@code conf/ocp/ds-image.xml}). Mirrors the same test for
 * {@code ds-storage}/{@code ds-datahandler}/{@code ds-license}.
 * <p>
 * {@code kaltura.partnerId} is used as the overridden key here purely as a convenient, non-sensitive example;
 * in production the override file's main purpose is secrets and the {@code imageservers.*} keys, which are
 * entirely absent from the checked-in {@code ds-image-behaviour.yaml} (see that file's SAMPLE devops override).
 */
class ServiceConfigPropertiesOverrideFileTest {

    @Test
    void explicitPropertiesFileOverridesYamlValues() throws IOException {
        Path knownFile = Path.of(Resolver.resolveURL("logback-test.xml").getPath());
        String projectRoot = knownFile.getParent().getParent().getParent().toString();
        String yamlFile = projectRoot + "/conf/ds-image-behaviour.yaml";

        Path overrideFile = Files.createTempFile("ds-image-application-", ".properties");
        try {
            Files.writeString(overrideFile, "kaltura.partnerId=999\n", StandardCharsets.UTF_8);

            ServiceConfig.initialize(yamlFile, overrideFile.toString());

            assertEquals(999, ServiceConfig.getConfig().getValue("kaltura.partnerId", Integer.class),
                    "kaltura.partnerId should come from the explicit properties override file, not the YAML file");

            // A key NOT present in the override file should still fall back to the YAML file, unaffected.
            assertEquals(150, ServiceConfig.getConfig().getValue("thumbnail.height.max", Integer.class),
                    "thumbnail.height.max should still come from ds-image-behaviour.yaml, unaffected by the " +
                    "override file");
        } finally {
            Files.deleteIfExists(overrideFile);
            ServiceConfig.initialize(yamlFile);
        }
    }

    @Test
    void missingPropertiesFileIsLoggedButDoesNotFailStartup() throws IOException {
        Path knownFile = Path.of(Resolver.resolveURL("logback-test.xml").getPath());
        String projectRoot = knownFile.getParent().getParent().getParent().toString();
        String yamlFile = projectRoot + "/conf/ds-image-behaviour.yaml";

        String nonExistentFile = projectRoot + "/conf/this-file-does-not-exist-application.properties";

        // Configured but unresolvable: ServiceConfig logs an error and continues, it does not throw.
        assertDoesNotThrow(() -> ServiceConfig.initialize(yamlFile, nonExistentFile));

        // Without the override file, kaltura.partnerId falls back to whatever the YAML file provides.
        assertEquals(398, ServiceConfig.getConfig().getValue("kaltura.partnerId", Integer.class));

        ServiceConfig.initialize(yamlFile);
    }
}
