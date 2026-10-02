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
import io.smallrye.config.EnvConfigSource;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.source.yaml.YamlConfigSource;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Demonstrates/verifies the config-source override mechanisms operations/devops rely on in production, and the
 * ordinal hierarchy between them (highest ordinal wins):
 * <pre>
 * system properties(400) &gt; env vars(300) &gt; .env(295) &gt; devops properties-override file(270)
 *     &gt; implicit config/application.properties(260) &gt; classpath application.properties(250) &gt; YAML(100)
 * </pre>
 * Four tests build up from a bare YAML config to progressively more realistic override scenarios:
 * <ol>
 *     <li>{@link #simpleYamlLoading()} - the YAML file alone, no override source at all.</li>
 *     <li>{@link #applicationPropertiesOverridesYamlValues()} - a real {@code config/application.properties}
 *     file placed next to the running service - never committed to git, see
 *     {@code config/application.properties.SAMPLE} in every module - carrying secrets and per-environment
 *     values such as the real database password. SmallRye Config picks this file up automatically: it is one
 *     of the built-in default sources registered by {@code addDefaultSources()} (ordinal 260, read from the
 *     current working directory - {@code ${user.dir}}, which for a Maven-run test is this module's own root
 *     directory), so no code change is needed for it to take effect.</li>
 *     <li>{@link #environmentVariablesOverrideYamlValues()} - the same idea, but via environment variables
 *     (ordinal 300) instead of a properties file. Simulated with {@link EnvConfigSource}'s map-based
 *     constructor rather than mutating the real process environment, which the JVM does not allow cleanly.</li>
 *     <li>{@link #environmentVariablesOutrankApplicationProperties()} - an environment variable and an
 *     {@code application.properties} entry set the very same key at once, to demonstrate the hierarchy: env
 *     (300) wins over {@code application.properties} (260).</li>
 * </ol>
 * Tests 1, 3 and 4 build their config sources entirely in-memory, so they are self-contained and always run.
 * Test 2 is the odd one out - it is also the one that matters most for production confidence, since it
 * exercises the real on-disk convention devops relies on.
 * <p>
 * The YAML fixture also carries an {@code oaiTargets} list of maps - a stand-in for the kind of nested,
 * multi-entry config real modules have - so {@link #simpleYamlLoading()} additionally verifies that structure
 * survives flattening into SmallRye's indexed-property syntax ({@code oaiTargets[0].name}, ...), not just the
 * flat {@code db.*} scalars.
 * <p>
 * This test class previously lived in ds-storage ({@code ServiceConfigApplicationPropertiesOverrideTest}) and
 * wrote then deleted a throwaway {@code config/application.properties} at test time. It was moved here because
 * the mechanism it demonstrates is generic SmallRye Config behaviour, not anything specific to ds-storage -
 * every module's {@code ServiceConfig} relies on exactly this same convention, and ds-shared is the module that
 * actually owns the property-loading logic (the vendored {@code dk.kb.util} package, including {@link Resolver}
 * and the old {@code YAML} class). It was also changed to read a pre-existing, persistent
 * {@code config/application.properties} instead of writing/deleting one itself every run.
 * <p>
 * <b>Prerequisite (test 2 only):</b> {@code ds-shared/config/application.properties} must exist locally with:
 * <pre>
 * db.password=dummy-test-password
 * db.connectionPoolSize=77
 * oaiTargets[0].password=elephant
 * oaiTargets[1].password=mountain
 * </pre>
 * The last two lines show that indexed-property syntax works in a plain {@code .properties} file exactly as it
 * does in YAML: each line overrides only that one nested field of that one list entry, leaving every other
 * field on both {@code oaiTargets} entries - {@code name}, {@code url}, {@code datasource}, ... - untouched,
 * still coming from the YAML.
 * <p>
 * (see {@code ds-shared/config/application.properties.SAMPLE}). This file is deliberately not committed to git
 * (the same {@code **&#47;config/application.properties} .gitignore rule every module's real override file
 * uses applies here too), so on a machine where it hasn't been created yet, only that one test is skipped (not
 * failed) - the other three need no local file and always run.
 */
class ApplicationPropertiesOverrideTest {

    private static final String YAML_RESOURCE = "application-properties-override-test.yaml";

    private static URL yamlUrl() throws IOException {
        return Resolver.resolveURL(YAML_RESOURCE);
    }

    @Test
    void simpleYamlLoading() throws IOException {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withSources(new YamlConfigSource(yamlUrl(), 100))
                .build();

        assertEquals(10, config.getValue("db.connectionPoolSize", Integer.class),
                "db.connectionPoolSize should come straight from the YAML file - no override source is in play");
        assertEquals("org.h2.Driver", config.getValue("db.driver", String.class),
                "db.driver should come straight from the YAML file");

        // db.password is deliberately left blank ("") in the YAML fixture, mirroring how production YAML files
        // leave secrets blank and rely on an override source. MicroProfile Config treats a blank value as "not
        // configured" (Config.getValue would throw NoSuchElementException), so this is verified via
        // getOptionalValue rather than asserting an empty string back from getValue.
        assertTrue(config.getOptionalValue("db.password", String.class).isEmpty(),
                "db.password is blank in the YAML and no override source is configured, so it should resolve " +
                "as absent, not as an empty string");

        // A more realistic nested structure: a YAML list of maps, resolved via SmallRye's indexed-property
        // syntax ("oaiTargets[0].name", "oaiTargets[1].name", ...) rather than kb-util's own YAML tree API
        // (YPath/YAMLVisitor) - this is the flattening every module's SmallRye-based ServiceConfig relies on.
        assertEquals("pvica.prod", config.getValue("oaiTargets[0].name", String.class),
                "oaiTargets[0].name should come from the first list entry in the YAML");
        assertEquals("ds.radiotv", config.getValue("oaiTargets[0].datasource", String.class),
                "oaiTargets[0].datasource should come from the first list entry in the YAML");
        assertEquals("https://<kuana-prod>/OAI-PMH/", config.getValue("oaiTargets[0].url", String.class),
                "oaiTargets[0].url should come from the first list entry in the YAML");
        assertEquals(false, config.getValue("oaiTargets[0].dayOnly", Boolean.class),
                "oaiTargets[0].dayOnly should come from the first list entry in the YAML");

        assertEquals("pvica.stage", config.getValue("oaiTargets[1].name", String.class),
                "oaiTargets[1].name should come from the second list entry in the YAML, not overwrite the first");
        assertEquals("https://<kuana-stage>/OAI-PMH/", config.getValue("oaiTargets[1].url", String.class),
                "oaiTargets[1].url should come from the second list entry in the YAML");

        // dayOnly is omitted entirely for the second target in the YAML - verify it really is absent rather
        // than silently inheriting the first entry's value or some converter default.
        assertTrue(config.getOptionalValue("oaiTargets[1].dayOnly", Boolean.class).isEmpty(),
                "oaiTargets[1].dayOnly has no entry in the YAML for the second target and should resolve as " +
                "absent");
    }

    @Test
    void applicationPropertiesOverridesYamlValues() throws IOException {
        Path overrideFile = Path.of(System.getProperty("user.dir"), "config", "application.properties");
        assumeTrue(Files.exists(overrideFile),
                "No local 'config/application.properties' override file found at '" + overrideFile + "' - copy " +
                "config/application.properties.SAMPLE there (without the .SAMPLE suffix) to run this test. " +
                "Skipping.");

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .addDefaultSources() // Includes the implicit config/application.properties convention (ordinal 260).
                .withSources(new YamlConfigSource(yamlUrl(), 100))
                .build();

        assertEquals("dummy-test-password", config.getValue("db.password", String.class),
                "db.password should come from config/application.properties, not the YAML file");
        assertEquals(77, config.getValue("db.connectionPoolSize", Integer.class),
                "db.connectionPoolSize should come from config/application.properties, not the YAML file");

        // A key NOT present in the override file should still fall back to the YAML file, unaffected.
        assertEquals("org.h2.Driver", config.getValue("db.driver", String.class),
                "db.driver should still come from the YAML file, unaffected by the override file");

        // Indexed-property syntax works for the override file too: "oaiTargets[0].password" and
        // "oaiTargets[1].password" each override only that one nested field of that one list entry.
        assertEquals("elephant", config.getValue("oaiTargets[0].password", String.class),
                "oaiTargets[0].password should come from config/application.properties, not the YAML file");
        assertEquals("mountain", config.getValue("oaiTargets[1].password", String.class),
                "oaiTargets[1].password should come from config/application.properties, not the YAML file, " +
                "and must not be confused with oaiTargets[0]'s overridden password");

        // Every other field on both list entries should be untouched by the override file, still from the YAML.
        assertEquals("pvica.prod", config.getValue("oaiTargets[0].name", String.class),
                "oaiTargets[0].name should still come from the YAML file, unaffected by the override file");
        assertEquals("pvica.stage", config.getValue("oaiTargets[1].name", String.class),
                "oaiTargets[1].name should still come from the YAML file, unaffected by the override file");
    }

    @Test
    void environmentVariablesOverrideYamlValues() throws IOException {
        // A simulated environment, not the real process environment: EnvConfigSource's map-based constructor
        // is SmallRye's own supported way of injecting env-style values into a test, without reflection or a
        // forked JVM to mutate System.getenv(). Keys use the usual env-var naming convention (dots become
        // underscores, upper-cased) that EnvConfigSource maps back to "db.password" / "db.connectionPoolSize".
        EnvConfigSource simulatedEnv = new EnvConfigSource(
                Map.of("DB_PASSWORD", "env-password", "DB_CONNECTIONPOOLSIZE", "99"),
                300);

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withSources(new YamlConfigSource(yamlUrl(), 100), simulatedEnv)
                .build();

        assertEquals("env-password", config.getValue("db.password", String.class),
                "db.password should come from the (simulated) environment, not the YAML file");
        assertEquals(99, config.getValue("db.connectionPoolSize", Integer.class),
                "db.connectionPoolSize should come from the (simulated) environment, not the YAML file");

        // A key NOT present in the environment should still fall back to the YAML file, unaffected.
        assertEquals("org.h2.Driver", config.getValue("db.driver", String.class),
                "db.driver should still come from the YAML file, unaffected by the environment");
    }

    @Test
    void environmentVariablesOutrankApplicationProperties() throws IOException {
        // Both sources set db.password at the same time, simulated rather than relying on a local
        // config/application.properties file, so this test is self-contained, always runs, and its outcome is
        // deterministic regardless of what either file happens to contain locally: environment variables
        // (ordinal 300) must win over application.properties (ordinal 260).
        EnvConfigSource simulatedEnv = new EnvConfigSource(
                Map.of("DB_PASSWORD", "env-wins"), 300);
        PropertiesConfigSource simulatedProperties = new PropertiesConfigSource(
                Map.of("db.password", "properties-loses"), "simulated application.properties", 260);

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withSources(new YamlConfigSource(yamlUrl(), 100), simulatedProperties, simulatedEnv)
                .build();

        assertEquals("env-wins", config.getValue("db.password", String.class),
                "Environment variables (ordinal 300) should outrank application.properties (ordinal 260), " +
                "regardless of the order sources are added to the builder in");
    }
}
