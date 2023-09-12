package dk.kb.present.transform;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dk.kb.present.TestUtil;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

@Disabled
public class XSLTPreservicaSchemaOrgTransformerTest {
    private static final Logger log = LoggerFactory.getLogger(XSLTSchemaDotOrgTransformerTest.class);
    public static final String PRESERVICA2SCHEMAORG = "xslt/preservica2schemaorg.xsl";
    public static final String RECORD_5a5357be = "internal_test_files/tvMetadata/5a5357be-5890-472a-a294-41a99f108936.xml";
    public static final String RECORD_44979f67 = "internal_test_files/tvMetadata/44979f67-b563-462e-9bf1-c970167a5c5f.xml";



    @Test
    public void testSetup() throws IOException {
        printSchemaOrgJson(RECORD_44979f67);
    }







    private static void printSchemaOrgJson(String xml) throws IOException {
        Map<String, String> injections = Map.of("imageserver", "https://example.com/imageserver/");
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, xml, injections);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement je = JsonParser.parseString(transformedJSON);
        String transformedPrettyJSON = gson.toJson(je);

        System.out.println(transformedPrettyJSON);
    }
}
