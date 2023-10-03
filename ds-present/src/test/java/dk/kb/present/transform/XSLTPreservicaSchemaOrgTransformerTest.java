package dk.kb.present.transform;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dk.kb.present.TestUtil;
import dk.kb.util.Resolver;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;



public class XSLTPreservicaSchemaOrgTransformerTest {
    private static final Logger log = LoggerFactory.getLogger(XSLTSchemaDotOrgTransformerTest.class);
    public static final String PRESERVICA2SCHEMAORG = "xslt/preservica2schemaorg.xsl";
    public static final String RECORD_5a5357be = "internal_test_files/tvMetadata/5a5357be-5890-472a-a294-41a99f108936.xml";
    public static final String RECORD_44979f67 = "internal_test_files/tvMetadata/44979f67-b563-462e-9bf1-c970167a5c5f.xml";
    public static final String RECORD_1F3A6A66 = "internal_test_files/tvMetadata/1f3a6a66-5f5a-48e6-abbf-452552320176.xml";
    public static final String RECORD_74e22fd8 = "internal_test_files/tvMetadata/74e22fd8-1268-4bcf-8a9f-22ca25379ea4.xml";

    @BeforeAll
    public static void beforeMethod() {
        if (Resolver.getPathFromClasspath("internal_test_files/tvMetadata") == null){
            log.warn("Internal test files are not present. Unittest 'XSLTPreservicaSchemaOrgTransformerTest' is therefore not run.");
        }
        Assumptions.assumeTrue(Resolver.getPathFromClasspath("internal_test_files/tvMetadata") != null);
    }

    @Test
    public void testSetup() throws IOException {
        printSchemaOrgJson(RECORD_74e22fd8);
    }



    @Test
    void testContentUrl() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, RECORD_5a5357be);
        Assertions.assertTrue(transformedJSON.contains("\"contentUrl\":\"www.example.com\\/streaming\\/cf1db0e1-ade2-462a-a2b4-7488244fcca7\""));
    }

    @Test
    void testName() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, RECORD_44979f67);
        Assertions.assertTrue(transformedJSON.contains("\"name\":[{\"value\":\"Backstage II\"}]"));
    }

    @Test
    void testBroadcastDisplayName() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, RECORD_44979f67);
        System.out.println(transformedJSON);
        Assertions.assertTrue(transformedJSON.contains("\"broadcastDisplayName\":\"DR Ultra\"}"));
    }

    @Test
    void testStartAndEndDates() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, RECORD_5a5357be);
        Assertions.assertTrue(transformedJSON.contains("\"startDate\":\"2021-01-18T00:00:00Z\"") &&
                                       transformedJSON.contains("\"endDate\":\"2021-01-18T00:30:00Z\""));
    }

    @Test
    void testDuration() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, RECORD_5a5357be);
        // TODO: Fix the time format delivered by the XSLT even though this format is compliant with schema.org as it is part of ISO 8601.
        Assertions.assertTrue(transformedJSON.contains("\"duration\":\"PT30M\""));
    }

    @Test
    void testIdentifiers() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, RECORD_1F3A6A66);
        Assertions.assertTrue(transformedJSON.contains("\"identifier\":[{\"@type\":\"PropertyValue\",\"PropertyID\":\"Origin\",\"value\":\"ds.test\"}")
                && transformedJSON.contains("{\"@type\":\"PropertyValue\",\"PropertyID\":\"kuanaId\",\"value\":\"1f3a6a66-5f5a-48e6-abbf-452552320176\"}]"));
    }

    @Test
    void noAlternateName() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, RECORD_1F3A6A66);
        Assertions.assertFalse(transformedJSON.contains("alternateName"));
    }

    @Test
    void testLiveStatus() throws IOException {
        String isLive = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, RECORD_74e22fd8);
        Assertions.assertTrue(isLive.contains("\"isLiveBroadcast\":\"true\""));

        String notLive = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, RECORD_5a5357be);
        Assertions.assertTrue(notLive.contains("\"isLiveBroadcast\":\"false\""));
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
