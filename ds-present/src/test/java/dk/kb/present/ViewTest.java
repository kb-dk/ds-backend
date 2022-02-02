package dk.kb.present;

import dk.kb.util.Resolver;
import dk.kb.util.yaml.YAML;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

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
class ViewTest {

    @Test
    void identity() throws Exception {
        YAML conf = YAML.resolveLayeredConfigs("test_setup.yaml");
        YAML dsflConf = conf.getYAMLList(".config.collections").get(0);
        View view = new View(dsflConf.getSubMap("dsfl").getYAMLList("views").get(0));
        assertEquals("SameAsInput", view.apply("SameAsInput")); // Identity view
    }

    @Test
    void jsonld() throws Exception {
        YAML conf = YAML.resolveLayeredConfigs("test_setup.yaml");
        YAML dsflConf = conf.getYAMLList(".config.collections").get(0);
        View jsonldView = new View(dsflConf.getSubMap("dsfl").getYAMLList("views").get(1));
        String mods = Resolver.resolveUTF8String("xml/corpus/albert-einstein.xml");
        String jsonld = jsonldView.apply(mods);
        assertTrue(jsonld.contains("\"name\":{\"@language\":\"en\",\"@value\":\"Einstein, Albert"));
    }

    @Test
    void solrjson() throws Exception {
        YAML conf = YAML.resolveLayeredConfigs("test_setup.yaml");
        YAML dsflConf = conf.getYAMLList(".config.collections").get(0);
        View jsonldView = new View(dsflConf.getSubMap("dsfl").getYAMLList("views").get(2));
        String mods = Resolver.resolveUTF8String("xml/corpus/albert-einstein.xml");
        String solrjson = jsonldView.apply(mods);
        assertTrue(solrjson.contains("\"subject_name_da\":\"Einstein, Albert\","));
    }
}