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
package dk.kb.present.config;

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
 * shared development Tomcat instance: {@link ServiceConfig#initializeWithPropertiesOverride(String, String)}'s
 * second argument, populated in practice from the {@code application-properties-config} JNDI entry (see
 * {@link dk.kb.present.webservice.ContextListener} and {@code conf/ocp/ds-present.xml}). Mirrors the same test for
 * {@code ds-storage}/{@code ds-datahandler}/{@code ds-license}/{@code ds-image}/{@code ds-discover}.
 * <p>
 * Unlike those services, the override file here only ever affects {@link ServiceConfig#getFlatConfig()} - the
 * nested {@link ServiceConfig#getConfig()} tree is untouched by it. See {@link ServiceConfig}'s class javadoc.
 * <p>
 * {@code licensemodule.url} is used as the overridden key here purely as a convenient, non-sensitive example; in
 * production the override file's main purpose is secrets such as {@code security.client.secret}.
 */
class ServiceConfigPropertiesOverrideFileTest {

    @Test
    void explicitPropertiesFileOverridesFlatConfigValues() throws IOException {
        Path knownFile = Path.of(Resolver.resolveURL("logback-test.xml").getPath());
        String projectRoot = knownFile.getParent().getParent().getParent().toString();
        String yamlFile = projectRoot + "/conf/ds-present-behaviour.yaml";

        Path overrideFile = Files.createTempFile("ds-present-application-", ".properties");
        try {
            Files.writeString(overrideFile, "licensemodule.url=http://overridden.example.com/ds-license/v1\n",
                    StandardCharsets.UTF_8);

            ServiceConfig.initializeWithPropertiesOverride(yamlFile, overrideFile.toString());

            assertEquals("http://overridden.example.com/ds-license/v1",
                    ServiceConfig.getFlatConfig().getValue("licensemodule.url", String.class),
                    "licensemodule.url should come from the explicit properties override file, not the YAML file");

            // A key NOT present in the override file should still fall back to the YAML file, unaffected.
            assertEquals(Boolean.TRUE, ServiceConfig.getFlatConfig().getValue("index.useTransriptions", Boolean.class),
                    "index.useTransriptions should still come from ds-present-behaviour.yaml, unaffected by the " +
                    "override file");

            // The nested tree is unaffected by the flat-config-only override file.
            assertEquals("http://localhost:9076/ds-license/v1",
                    ServiceConfig.getConfig().getString("licensemodule.url"),
                    "The nested YAML tree (getConfig()) should be unaffected by the properties override file, " +
                    "which only applies to getFlatConfig()");
        } finally {
            Files.deleteIfExists(overrideFile);
            ServiceConfig.initialize(yamlFile);
        }
    }

    @Test
    void missingPropertiesFileIsLoggedButDoesNotFailStartup() throws IOException {
        Path knownFile = Path.of(Resolver.resolveURL("logback-test.xml").getPath());
        String projectRoot = knownFile.getParent().getParent().getParent().toString();
        String yamlFile = projectRoot + "/conf/ds-present-behaviour.yaml";

        String nonExistentFile = projectRoot + "/conf/this-file-does-not-exist-application.properties";

        // Configured but unresolvable: ServiceConfig logs an error and continues, it does not throw.
        assertDoesNotThrow(() -> ServiceConfig.initializeWithPropertiesOverride(yamlFile, nonExistentFile));

        // Without the override file, licensemodule.url falls back to whatever the YAML file provides.
        assertEquals("http://localhost:9076/ds-license/v1",
                ServiceConfig.getFlatConfig().getValue("licensemodule.url", String.class));

        ServiceConfig.initialize(yamlFile);
    }
}
