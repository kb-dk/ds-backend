package dk.kb.present.transform;

import dk.kb.present.TestUtil;
import dk.kb.util.Resolver;
import dk.kb.util.yaml.YAML;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import com.google.gson.*;

import java.io.IOException;
import java.util.HashMap;
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
class XSLTTransformerTest {
    public static final String MODS2JSONLD = "xslt/mods2schemaorg.xsl";
    public static final String MODS2SOLR = "xslt/mods2solr.xsl";
    public static final String MODS2SOLR_NEW = "xslt/mods2solr_NEW.xsl";
    public static final String ALBERT = "xml/corpus/albert-einstein.xml"; //Need to be updated to newest version
    public static final String CHINESE = "xml/corpus/chinese-manuscripts.xml"; //Need to be updated to newest version

    public static final String NEW_000332 = "xml/copyright_extraction/000332.tif.xml"; //Updated version
    
    
    @Test
    void testJSONLDAlbert() throws IOException {
        JSONObject jsonld = new JSONObject( TestUtil.getTransformed(MODS2JSONLD, ALBERT));
        assertTrue(jsonld.toString().contains("\"name\":{\"@value\":\"Einstein, Albert\",\"@language\":\"en\"}"));
    }

    @Test
    void testJSONLDChinese() throws IOException {
        JSONObject jsonld = new JSONObject(TestUtil.getTransformed(MODS2JSONLD, CHINESE));
        assertTrue(jsonld.toString().contains("\"name\":{\"@value\":\"周培春 Zhou Peichun\",\"@language\":\"zh\"}"));
    }

    
    @Test
    void testNew000332() throws IOException {
        String solrString =  TestUtil.getTransformed(MODS2SOLR_NEW, NEW_000332);
        // TODO: Add more detailed test
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement je = JsonParser.parseString(solrString);
        String prettyJsonString = gson.toJson(je);
        

        System.out.println(prettyJsonString );
    }

    @Test
    void testIDInjection() throws IOException {
    	Map<String, String>  parameters = new HashMap<String,String>();
    	parameters.put("external_parameter1" , "value1");
    	parameters.put("external_parameter2" , "value2");
    	    
    	
        String solrString =  TestUtil.getTransformed("id_inject.xsl", "id_inject.xml", parameters);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement je = JsonParser.parseString(solrString);
        String prettyJsonString = gson.toJson(je);

//        System.out.println(prettyJsonString );
        assertTrue(solrString.contains("{\"field_external1\":\"value1"), "External field_parameter1 was not 'value1':"+prettyJsonString); //First parameter
        assertTrue(solrString.contains("\"field_external2\":\"value2"), "External field_parameter2 was not 'value2':"+prettyJsonString); 
                   

    }

    
    
}