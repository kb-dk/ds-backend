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

import dk.kb.util.Resolver;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.source.yaml.YamlConfigSource;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Demonstrates/verifies the override mechanism operations/devops rely on in production: a
 * {@code config/application.properties} file placed next to the running service - never committed to git, see
 * {@code config/application.properties.SAMPLE} in every module - carrying secrets and per-environment values
 * such as the real database password.
 * <p>
 * SmallRye Config picks this file up automatically: it is one of the built-in default sources registered by
 * {@code addDefaultSources()} (ordinal 260, read from the current working directory - {@code ${user.dir}},
 * which for a Maven-run test is this module's own root directory), so no code change is needed for it to take
 * effect - it layers on top of whatever the YAML configuration source below configures, overriding only the
 * keys it defines.
 * <p>
 * This test previously lived in ds-storage ({@code ServiceConfigApplicationPropertiesOverrideTest}) and wrote
 * then deleted a throwaway {@code config/application.properties} at test time. It was moved here because the
 * mechanism it demonstrates is generic SmallRye Config behaviour, not anything specific to ds-storage - every
 * module's {@code ServiceConfig} relies on exactly this same convention, and ds-shared is the module that
 * actually owns the property-loading logic (the vendored {@code dk.kb.util} package, including {@link Resolver}
 * and the old {@code YAML} class). It was also changed to read a pre-existing, persistent
 * {@code config/application.properties} instead of writing/deleting one itself every run.
 * <p>
 * <b>Prerequisite:</b> {@code ds-shared/config/application.properties} must exist locally with:
 * <pre>
 * db.password=dummy-test-password
 * db.connectionPoolSize=77
 * </pre>
 * (see {@code ds-shared/config/application.properties.SAMPLE}). This file is deliberately not committed to git
 * (the same {@code **&#47;config/application.properties} .gitignore rule every module's real override file
 * uses applies here too), so on a machine where it hasn't been created yet, this test is skipped (not failed).
 */
class ApplicationPropertiesOverrideTest {

    @Test
    void applicationPropertiesOverridesYamlValues() throws IOException {
        Path overrideFile = Path.of(System.getProperty("user.dir"), "config", "application.properties");
        assumeTrue(Files.exists(overrideFile),
                "No local 'config/application.properties' override file found at '" + overrideFile + "' - copy " +
                "config/application.properties.SAMPLE there (without the .SAMPLE suffix) to run this test. " +
                "Skipping.");

        URL yamlUrl = Resolver.resolveURL("application-properties-override-test.yaml");

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .addDefaultSources() // Includes the implicit config/application.properties convention (ordinal 260).
                .withSources(new YamlConfigSource(yamlUrl, 100))
                .build();

        assertEquals("dummy-test-password", config.getValue("db.password", String.class),
                "db.password should come from config/application.properties, not the YAML file");
        assertEquals(77, config.getValue("db.connectionPoolSize", Integer.class),
                "db.connectionPoolSize should come from config/application.properties, not the YAML file");

        // A key NOT present in the override file should still fall back to the YAML file, unaffected.
        assertEquals("org.h2.Driver", config.getValue("db.driver", String.class),
                "db.driver should still come from the YAML file, unaffected by the override file");
    }
}
