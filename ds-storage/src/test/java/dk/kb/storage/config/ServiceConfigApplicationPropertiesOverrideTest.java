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
package dk.kb.storage.config;

import dk.kb.util.Resolver;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Demonstrates the override mechanism operations/devops rely on in production: a {@code config/application.properties}
 * file placed next to the running service - never committed to git, see {@code config/application.properties.SAMPLE}
 * - carrying secrets and per-environment values such as the real database password.
 * <p>
 * SmallRye Config picks this file up automatically: it is one of the built-in default sources registered by
 * {@code addDefaultSources()} (ordinal 260, read from the current working directory), so no code change and no
 * extra argument to {@link ServiceConfig#initialize(String)} is needed for it to take effect - it layers on top
 * of whatever the single YAML file configures, overriding only the keys it defines.
 * <p>
 * This test only ever writes dummy, throwaway values - the real production file lives entirely outside this
 * repository (devops manage it in their own repository/deployment process) and is never present here, so there
 * is nothing local to preserve: the test simply creates the file, runs, and removes it again.
 */
class ServiceConfigApplicationPropertiesOverrideTest {

    @Test
    void applicationPropertiesOverridesYamlValues() throws IOException {
        Path knownFile = Path.of(Resolver.resolveURL("logback-test.xml").getPath());
        String projectRoot = knownFile.getParent().getParent().getParent().toString();
        Path overrideFile = Path.of(projectRoot, "config", "application.properties");

        try {
            Files.createDirectories(overrideFile.getParent());
            // db.password is '' and db.connectionPoolSize is 10 in ds-storage-behaviour.yaml. This overrides both
            // with dummy values, purely to demonstrate/verify the override mechanism - only the keys defined here
            // are affected, everything else still comes from the YAML file.
            Files.writeString(overrideFile,
                    "db.password=dummy-test-password\n" +
                    "db.connectionPoolSize=77\n",
                    StandardCharsets.UTF_8);

            ServiceConfig.initialize(projectRoot + "/conf/ds-storage-behaviour.yaml");

            assertEquals("dummy-test-password", ServiceConfig.getDBPassword(),
                    "db.password should come from config/application.properties, not the YAML file");
            assertEquals(77, ServiceConfig.getConnectionPoolSize(),
                    "db.connectionPoolSize should come from config/application.properties, not the YAML file");

            // A key NOT present in the override file should still fall back to the YAML file, unaffected.
            assertEquals("org.h2.Driver", ServiceConfig.getDBDriver(),
                    "db.driver should still come from ds-storage-behaviour.yaml, unaffected by the override file");
        } finally {
            Files.deleteIfExists(overrideFile);
            // Re-initialize once more so later tests in the same JVM run never see our temporary override.
            ServiceConfig.initialize(projectRoot + "/conf/ds-storage-behaviour.yaml");
        }
    }
}
