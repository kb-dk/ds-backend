package dk.kb.present.config;

import dk.kb.util.Resolver;
import dk.kb.util.yaml.YAML;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
    //@Test
    void sampleConfigTest() throws IOException {
        // Pretty hacky, but it is only a sample unit test
        Path knownFile = Path.of(Resolver.resolveURL("logback-test.xml").getPath());
        String projectRoot = knownFile.getParent().getParent().getParent().toString();

        Path sampleEnvironmentSetup = Path.of(projectRoot, "conf/ds-present-environment.yaml");
        assertTrue(Files.exists(sampleEnvironmentSetup),
                   "The sample setup is expected to be present at '" + sampleEnvironmentSetup + "'");

        ServiceConfig.initialize(projectRoot + File.separator + "conf" + File.separator + "ds-present*.yaml");

        // Defined in behaviour
        assertEquals(10, ServiceConfig.getConfig().getInteger("limits.min"));

        // Real value in environment
        assertEquals("real_dbpassword", ServiceConfig.getConfig().getString("backend.password"));
    }

    @Test
    void testImageserverAbstraction2() throws IOException {
        Path knownFile = Path.of(Resolver.resolveURL("logback-test.xml").getPath());
        String projectRoot = knownFile.getParent().getParent().getParent().toString();

        // Note: These are test configs to avoid locking the structure of the real configs
        Path behaviour = Resolver.getPathFromClasspath("config/imageserver/ds-present-behaviour.yaml");
        Path collections = Resolver.getPathFromClasspath("config/imageserver/ds-present-kb-collections.yaml");
        Path servers = Resolver.getPathFromClasspath("config/imageserver/ds-present-servers.yaml");

        ServiceConfig.initialize(behaviour.toString(), collections.toString());
        ServiceConfig.initialize(behaviour.toString(), collections.toString(), servers.toString());
        YAML yaml = ServiceConfig.getConfig();

        // Behaviour has an 'invalid' imageserver, but Servers overrides the list to only contain 'local'
        //System.out.println(yaml.getYAMLList("imageservers"));
        assertEquals("true", yaml.getString("imageservers[0].local.default"),
                "The imageserver 'local' should have 'default: true'");

        assertEquals("the_right_url", yaml.getString("imageservers[default=true].url"),
                "The correct URL should be extracted from the default imageserver using yaml.get with conditional");

        assertEquals("the_right_url",
                yaml.getString("collections[4].samlingsbilleder.views[3].SolrJSON.transformers[1].xslt.injections[0].imageserver"),
                "The correct URL should be substitution-extracted from the 'samlingsbilleder' view SolrJSON injection");

        assertEquals("the_right_url",
                yaml.getSubMap("collections[4].samlingsbilleder.views[3].SolrJSON").
                        getString("transformers[1].xslt.injections[0].imageserver"),
                "Requesting path substituted values from a sub map should work");

        // Reset the YAML structure to ensure clean test of submap
        ServiceConfig.initialize(behaviour.toString(), collections.toString(), servers.toString());
        yaml = ServiceConfig.getConfig();

        assertEquals("the_right_url",
                yaml.getSubMap("collections[4].samlingsbilleder.views[3].SolrJSON").
                        getString("transformers[1].xslt.injections[0].imageserver"),
                "Requesting path substituted values from a sub map should work on a newly loaded config");
    }

}
