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
package dk.kb.present;

import dk.kb.present.config.ServiceConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresentFacadeTest {

	
	
    @BeforeAll
    static void setup() {
        try {
            ServiceConfig.initialize("test_setup.yaml");
            PresentFacade.warmUp();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    void getRecord() {
        // Throws an Exception if not found
        PresentFacade.getRecord("local.mods:40221e30-1414-11e9-8fb8-00505688346e.xml", "mods");
    }



    @Test
    void getRecordsMODS() throws IOException {
        StreamingOutput out = PresentFacade.getRecords(null, "dsfl", 0L, -1L, "mods");
        String result = toString(out);
        assertTrue(result.contains("<mods:namePart type=\"family\">Andersen</mods:namePart>"));
    }


    @Test
    void getRecordsMODSDeclaration() throws IOException {
        StreamingOutput out = PresentFacade.getRecords(null, "dsfl", 0L, -1L, "mods");
        String result = toString(out);

        Pattern DECLARATION = Pattern.compile("<[?]xml version=\"1.0\" encoding=\"UTF-8\"[?]>", Pattern.DOTALL);
        Matcher m = DECLARATION.matcher(result);
        int count = 0;
        while (m.find()) {
            count++;
        }
        assertTrue(count <= 1, "There should be at most 1 XML declaration but there was " + count);
    }


    @Test
    void getRecordsRaw() throws IOException {
        PresentFacade.recordView = "raw-bypass"; // We don't want to check security here
        StreamingOutput out = PresentFacade.getRecords(null, "dsfl", 0L, -1L, "storagerecord");
        String result = toString(out);

        assertTrue(result.contains("\"id\":\"40221e30-1414-11e9-8fb8-00505688346e.xml\",\"deleted\":false"));
        assertTrue(result.contains(",\n"), "Result should contain a comma followed by newline as it should be a multi-entry JSON array"); // Plain JSON array
        assertTrue(result.endsWith("]\n"), "Result should end with ']' as it should be a JSON array"); // JSON array
    }


    

    @Test
    void getRecordsRawLines() throws IOException {
        PresentFacade.recordView = "raw-bypass"; // We don't want to check security here
        StreamingOutput out = PresentFacade.getRecords(null, "dsfl", 0L, -1L, "storagerecord-lines");
        String result = toString(out);
        assertTrue(result.contains("\"id\":\"40221e30-1414-11e9-8fb8-00505688346e.xml\",\"deleted\":false"));
        assertFalse(result.contains(",\n"), "Result should not contain a comma followed by newline as it should be a multi-entry JSON-Lines");
        assertFalse(result.endsWith("]\n"), "Result should not end with ']' as it should be in JSON-Lin es");
    }



    /* TODO: FIX!
    //   Can only be fixed, when the updated XSLT to JSON-LD has been reviewed and merged to master

    @Test
    void getRecordsJSONLD() throws IOException {
        StreamingOutput out = PresentFacade.getRecords(null, "dsfl", 0L, -1L, "json-ld");
        String result = toString(out);
        System.out.println(result);
        //assertTrue(result.contains("\"@value\":\"Letters to\\/from David Simonsen\"}"), "Result should contain the name David Simonsen in the expected wrapping");
        assertTrue(result.contains(",\n"), "Result should contain a comma followed by newline as it should be a multi-entry JSON array"); // Plain JSON array
        assertTrue(result.endsWith("]\n"), "Result should end with ']' as it should be a JSON array"); // JSON array
    }

     */

    /*  TODO FIX!
    /   Can only be fixed, when the updated XSLT to JSON-LD has been reviewed and merged to master
    @Test
    void getRecordsJSONLDLines() throws IOException {
        StreamingOutput out = PresentFacade.getRecords(null, "dsfl", 0L, -1L, "json-ld-lines");
        String result = toString(out);
        assertTrue(result.contains("\"@value\":\"Letters to\\/from David Simonsen\"}"), "Result should contain the name David Simonsen in the expected wrapping");
        assertFalse(result.contains(",\n"), "Result should not contain a comma followed by newline as it should be a multi-entry JSON-Lines");
        assertFalse(result.endsWith("]\n"), "Result should not end with ']' as it should be in JSON-Lin es");
    }
*/

    @Test
    void getRecordsSolr() throws IOException {
        StreamingOutput out = PresentFacade.getRecords(null, "dsfl", 0L, -1L, "SolrJSON");
        String result = toString(out);
    }

    private String toString(StreamingOutput out) throws IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            out.write(os);
            return os.toString(StandardCharsets.UTF_8);
        }
    }
    
}