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
package dk.kb.license.config;

import dk.kb.util.Resolver;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Demonstrates the explicit, per-service devops/operations properties override file used in production/on the
 * shared development Tomcat instance: {@link ServiceConfig#initialize(String, String)}'s second argument,
 * populated in practice from the {@code application-properties-config} JNDI entry (see
 * {@link dk.kb.license.webservice.ContextListener} and {@code conf/ocp/ds-license.xml}). Mirrors the same test
 * for {@code ds-storage}/{@code ds-datahandler}.
 * <p>
 * {@code db.password} is used as the overridden key here since {@code ds-license-behaviour.yaml} has no
 * {@code db:} section at all in this repository - in production, the whole {@code db.*} block is expected to
 * come from this override file (or environment variables/system properties).
 */
class ServiceConfigPropertiesOverrideFileTest {

    @Test
    void explicitPropertiesFileOverridesYamlValues() throws IOException {
        Path knownFile = Path.of(Resolver.resolveURL("logback-test.xml").getPath());
        String projectRoot = knownFile.getParent().getParent().getParent().toString();
        String yamlFile = projectRoot + "/conf/ds-license-behaviour.yaml";

        Path overrideFile = Files.createTempFile("ds-license-application-", ".properties");
        try {
            // Dummy, throwaway value purely to demonstrate/verify the override mechanism.
            Files.writeString(overrideFile, "db.password=dummy-test-password\n", StandardCharsets.UTF_8);

            ServiceConfig.initialize(yamlFile, overrideFile.toString());

            assertEquals("dummy-test-password", ServiceConfig.getDBPassword(),
                    "db.password should come from the explicit properties override file, not the YAML file");

            // A key NOT present in the override file should still fall back to the YAML file, unaffected.
            assertEquals(300, ServiceConfig.getCacheRefreshTimeInSeconds(),
                    "cache.reloadInSeconds should still come from ds-license-behaviour.yaml, unaffected by the " +
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
        String yamlFile = projectRoot + "/conf/ds-license-behaviour.yaml";

        String nonExistentFile = projectRoot + "/conf/this-file-does-not-exist-application.properties";

        // Configured but unresolvable: ServiceConfig logs an error and continues, it does not throw.
        assertDoesNotThrow(() -> ServiceConfig.initialize(yamlFile, nonExistentFile));

        // Without the override file, db.password falls back to whatever the YAML file provides (empty, since
        // ds-license-behaviour.yaml has no db.* keys at all).
        assertEquals("", ServiceConfig.getDBPassword());

        ServiceConfig.initialize(yamlFile);
    }
}
