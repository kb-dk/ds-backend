package dk.kb.discover.config;

import dk.kb.util.Resolver;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

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
class ServiceConfigTest {

    /*
     * This unit-test probably fails when the template is applied and a proper project is taking form.
     * That is okay. It is only here to serve as a temporary demonstration of unit-testing and configuration.
     */
    @Test
    void loadConfigTest() throws IOException {
        // Pretty hacky, but it is only a sample unit test
        Path knownFile = Path.of(Resolver.resolveURL("logback-test.xml").getPath());
        String projectRoot = knownFile.getParent().getParent().getParent().toString();

        Path sampleEnvironmentSetup = Path.of(projectRoot, "conf/ds-discover-behaviour.yaml");
        assertTrue(Files.exists(sampleEnvironmentSetup),
                   "The base setup is expected to be present at '" + sampleEnvironmentSetup + "'");

        ServiceConfig.initialize(projectRoot + File.separator + "conf" + File.separator + "ds-discover*.yaml");

        // The only thing we test here is that configuration loading succeeds
        // This is because each project has independent configuration entries with no required overlap

        // Defined in behaviour
        //assertEquals(10, ServiceConfig.getConfig().getInteger("config.limits.min"));

        // Real value in environment
        //assertEquals("real_dbpassword", ServiceConfig.getConfig().getString("config.backend.password"));
    }

    @Tag("slow")
    @Test
    void autoLoadTest() throws IOException, InterruptedException {
        final String CONF0 = "config:\n  autoupdate:\n    enabled: true\n    intervalms: 100\n  somevalue: 0";
        final String CONF1 = "config:\n  autoupdate:\n    enabled: true\n    intervalms: 100\n  somevalue: 1";
        final String CONF2 = "config:\n  autoupdate:\n    enabled: false\n    intervalms: 100\n  somevalue: 2";
        final AtomicInteger reloads = new AtomicInteger(0);
        final String VALUE_KEY = ".config.somevalue";

        // Initial state
        File conf = File.createTempFile("ds-discover_config_", ".yaml");
        FileUtils.writeStringToFile(conf, CONF0, StandardCharsets.UTF_8);
        ServiceConfig.registerObserver(yaml -> reloads.incrementAndGet());
        ServiceConfig.initialize(conf.toString());
        assertTrue(ServiceConfig.isAutoUpdating(), "Config should be auto updating");
        assertEquals(1, reloads.get(), "After init, reloads should be 0");
        assertEquals(0, ServiceConfig.getConfig().getInteger(VALUE_KEY), "Initial value should match");

        Thread.sleep(200);
        assertEquals(1, reloads.get(), "After first sleep, reloads should still be correct");

        // Update config file with same content: Should not trigger anything
        FileUtils.writeStringToFile(conf, CONF0, StandardCharsets.UTF_8);
        Thread.sleep(200);
        assertEquals(1, reloads.get(), "After second sleep, reloads should still be correct (new config is identical to old)");

        // Update config with new content
        FileUtils.writeStringToFile(conf, CONF1, StandardCharsets.UTF_8);
        Thread.sleep(200);
        assertEquals(2, reloads.get(), "After third sleep, reloads should still be correct");
        assertEquals(1, ServiceConfig.getConfig().getInteger(VALUE_KEY), "First change value should match");

        Thread.sleep(200);
        assertEquals(2, reloads.get(), "After fourth sleep, reloads should still be 1 (no change at all)");

        // Second update and disabling of auto-update
        FileUtils.writeStringToFile(conf, CONF2, StandardCharsets.UTF_8);
        Thread.sleep(200);
        assertEquals(3, reloads.get(), "After fifth sleep, reloads should be correct");
        assertEquals(2, ServiceConfig.getConfig().getInteger(VALUE_KEY), "Second change value should match");
        assertFalse(ServiceConfig.isAutoUpdating(), "Config should have auto updating turned off");
    }

}
