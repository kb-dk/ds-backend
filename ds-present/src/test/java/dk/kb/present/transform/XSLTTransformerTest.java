package dk.kb.present.transform;

import dk.kb.present.TestUtil;
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

    @Test
    void testIDInjection() throws IOException {
    	Map<String, String>  parameters = new HashMap<String,String>();
    	parameters.put("external_parameter1" , "value1");
    	parameters.put("external_parameter2" , "value2");

        String solrString =  TestUtil.getTransformed("id_inject.xsl", "id_inject.xml", parameters);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement je = JsonParser.parseString(solrString);
        String prettyJsonString = gson.toJson(je);

        assertTrue(solrString.contains("{\"field_external1\":\"value1"), "External field_parameter1 was not 'value1':"+prettyJsonString); //First parameter
        assertTrue(solrString.contains("\"field_external2\":\"value2"), "External field_parameter2 was not 'value2':"+prettyJsonString); 
                   

    }

    @Test
    void testFixedInjection() throws IOException {
    	Map<String, String>  fixed = new HashMap<String,String>();
    	fixed.put("external_parameter1" , "value1");
    	fixed.put("external_parameter2" , "value2");


        String solrString =  TestUtil.getTransformed("id_inject.xsl", "id_inject.xml", fixed, null);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement je = JsonParser.parseString(solrString);
        String prettyJsonString = gson.toJson(je);

//        System.out.println(prettyJsonString );
        assertTrue(solrString.contains("{\"field_external1\":\"value1"), "External field_parameter1 was not 'value1':"+prettyJsonString); //First parameter
        assertTrue(solrString.contains("\"field_external2\":\"value2"), "External field_parameter2 was not 'value2':"+prettyJsonString);
    }

    @Test
    void testFixedInjectionOverride() throws IOException {
    	Map<String, String>  fixed = Map.of("external_parameter1" , "defaultValueSomething",
                                            "external_parameter2" , "value2");

        Map<String, String>  metadata = Map.of("external_parameter1" , "value1");


        String solrString =  TestUtil.getTransformed("id_inject.xsl", "id_inject.xml", fixed, metadata);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement je = JsonParser.parseString(solrString);
        String prettyJsonString = gson.toJson(je);

//        System.out.println(prettyJsonString );
        assertTrue(solrString.contains("{\"field_external1\":\"value1"), "External field_parameter1 was not 'value1':"+prettyJsonString); //First parameter
        assertTrue(solrString.contains("\"field_external2\":\"value2"), "External field_parameter2 was not 'value2':"+prettyJsonString);
    }



}