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
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Demonstrates that {@link ServiceConfig} (now backed by SmallRye Config) supports injecting new/changed
 * properties at runtime: {@link ServiceConfig#setRuntimeProperty(String, String)} takes effect immediately,
 * without a restart and without touching any configuration file, and can be reverted with
 * {@link ServiceConfig#clearRuntimeProperty(String)}.
 */
class ServiceConfigRuntimeInjectionTest {

    /**
     * Mirrors the setup in {@link ServiceConfigTest}: resolve the project's own {@code conf/} folder so the
     * test runs against the real {@code ds-storage-behaviour.yaml} (plus a local/environment overlay, if present).
     */
    private static void initializeFromProjectConf() throws IOException {
        Path knownFile = Path.of(Resolver.resolveURL("logback-test.xml").getPath());
        String projectRoot = knownFile.getParent().getParent().getParent().toString();
        ServiceConfig.initialize(projectRoot + "/conf/ds-storage*.yaml");
    }

    @Test
    void runtimePropertyOverridesYamlImmediately() throws IOException {
        initializeFromProjectConf();

        // db.connectionPoolSize is 10 in ds-storage-behaviour.yaml
        int original = ServiceConfig.getConnectionPoolSize();
        assertEquals(10, original);

        // Inject a new value at runtime: no re-initialize(), no restart.
        ServiceConfig.setRuntimeProperty("db.connectionPoolSize", "42");
        assertEquals(42, ServiceConfig.getConnectionPoolSize(),
                     "Runtime-injected property should be visible immediately");
        assertTrue(ServiceConfig.getRuntimePropertyNames().contains("db.connectionPoolSize"));

        // Clearing the override reverts to the YAML-configured value.
        ServiceConfig.clearRuntimeProperty("db.connectionPoolSize");
        assertEquals(original, ServiceConfig.getConnectionPoolSize(),
                     "Clearing the runtime override should revert to the configured YAML value");
        assertFalse(ServiceConfig.getRuntimePropertyNames().contains("db.connectionPoolSize"));
    }

    @Test
    void runtimePropertyCanInjectAKeyNotPresentInYamlAtAll() throws IOException {
        initializeFromProjectConf();

        String key = "demo.featureFlag.newBehaviour";
        assertTrue(ServiceConfig.getConfig().getOptionalValue(key, String.class).isEmpty());

        ServiceConfig.setRuntimeProperty(key, "enabled");
        assertEquals("enabled", ServiceConfig.getConfig().getValue(key, String.class));

        ServiceConfig.clearRuntimeProperty(key);
        assertTrue(ServiceConfig.getConfig().getOptionalValue(key, String.class).isEmpty());
    }
}
