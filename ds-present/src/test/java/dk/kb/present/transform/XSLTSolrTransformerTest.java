package dk.kb.present.transform;

import dk.kb.present.TestUtil;
import dk.kb.util.Resolver;
import dk.kb.util.yaml.YAML;

import org.apache.solr.common.SolrInputDocument;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.*;

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
class XSLTSolrTransformerTest{

    public static final String MODS2SOLR_OLD = "xslt/mods2solr_OLD.xsl";
    public static final String MODS2SOLR = "xslt/mods2solr.xsl";
    public static final String NEW_000332 = "xml/copyright_extraction/000332.tif.xml"; //Updated version
    public static final String NEW_DPK = "xml/copyright_extraction/DPK000107.tif.xml";
    
    

    @Test
    void testSolOld() throws IOException {
        String solrString = TestUtil.getTransformed(MODS2SOLR_OLD,  NEW_000332);
        // TODO: Add more detailed test
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement je = JsonParser.parseString(solrString);                
        String prettyJsonString = gson.toJson(je);        
        System.out.println(prettyJsonString );
        
    }

    
    @Test
    void testSolrNew() throws Exception {
    	
    	String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR, NEW_000332);
        // TODO: Add more detailed test
     
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement je = JsonParser.parseString(solrString);
        String prettyJsonString = gson.toJson(je);        
        System.out.println(prettyJsonString );
       
      //  assertTrue(solrString.contains("{\"id\":\""));
    }

    @Test
    void testXsltNewDpkItem() throws Exception {
        String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR, NEW_DPK);
        // TODO: Add more detailed test

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement je = JsonParser.parseString(solrString);
        String prettyJsonString = gson.toJson(je);
        System.out.println(prettyJsonString );

        //  assertTrue(solrString.contains("{\"id\":\""));
    }


    
    
}