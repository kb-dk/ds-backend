package dk.kb.present.transform;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dk.kb.present.TestUtil;
import dk.kb.present.config.ServiceConfig;
import dk.kb.util.Resolver;
import dk.kb.util.yaml.YAML;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileWriter;

import java.io.StringBufferInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class XSLTSchemaDotOrgTransformerTest {
    private static final Logger log = LoggerFactory.getLogger(XSLTSchemaDotOrgTransformerTest.class);

    public static final String JSON_ROOT = "src/test/resources/schemaOrgJsonTestFiles/";
    
    public static final String MODS2SCHEMAORG = "xslt/mods2schemaorg.xsl";
    public static final String RECORD_000332 = "xml/copyright_extraction/000332.tif.xml";
    public static final String RECORD_JB000132 = "xml/copyright_extraction/JB000132_114.tif.xml";
    public static final String RECORD_KHP0001_001 = "xml/copyright_extraction/KHP0001-001.tif.xml";
    public static final String RECORD_KE066530 = "xml/copyright_extraction/KE066530.tif.xml";
    public static final String RECORD_DPK000107 = "xml/copyright_extraction/DPK000107.tif.xml";
    public static final String RECORD_ANSK = "xml/copyright_extraction/ANSK_11614.tif.xml";
    public static final String RECORD_DNF = "xml/copyright_extraction/DNF_1951-00352_00052.tif.xml";
    public static final String RECORD_40221e = "xml/copyright_extraction/40221e30-1414-11e9-8fb8-00505688346e.xml";

    // Does not seem to be needed for these tests
    @BeforeAll
    public static void fixConfiguration() throws IOException {
        String CONFIG = Resolver.resolveGlob("conf/ds-present-behaviour.yaml").get(0).toString();
        if ("[]".equals(CONFIG)) {
            throw new IllegalStateException("Unable to locate config");
        }

        log.info("Fixing config to '{}'", CONFIG);
        ServiceConfig.initialize(CONFIG);
    }

    @Test
    void testDateCreatedAndTemporal() throws Exception {
        assertJSONTransformation(MODS2SCHEMAORG, RECORD_000332, "schemaOrg_000332.json");
    }

    @Test
    void testDateCreatedNoTemporal() throws Exception {
        assertJSONTransformation(MODS2SCHEMAORG, RECORD_KHP0001_001, "schemaOrg_KHP0001-001.json");
    }

    @Test
    void testCreatorsAndHeadline() throws Exception {
        assertJSONTransformation(MODS2SCHEMAORG, RECORD_JB000132, "schemaOrg_JB000132_114.json");
    }

    // Same functionality as testCreatorsAndHeadline but using a different XSLTFactory creation method
    @Test
    void testFactory() throws Exception {
        assertJSONTransformationFactory(MODS2SCHEMAORG, RECORD_JB000132, "schemaOrg_JB000132_114.json");
    }


    @Test
    void testCreatorDescriptionAndContentNoteToAbout() throws Exception {
        assertJSONTransformation(MODS2SCHEMAORG, RECORD_KE066530, "schemaOrg_KE066530.json");
    }

    @Test
    void testContentLocationAndKeywords() throws Exception {
        assertJSONTransformation(MODS2SCHEMAORG, RECORD_DPK000107, "schemaOrg_DPK000107.json");
    }


    @Test
    void testMaterialSize() throws Exception {
        assertJSONTransformation(MODS2SCHEMAORG, RECORD_ANSK, "schemaOrg_ANSK_11614.json");
    }

    @Test
    void testInternalNotesToKbAdmin() throws Exception {
        assertJSONTransformation(MODS2SCHEMAORG, RECORD_DNF, "schemaOrg_DNF_1951-00352_00052.json");
    }

    @Test
    void testImageUrlCreation () throws Exception {
        assertJSONTransformation(MODS2SCHEMAORG, RECORD_40221e, "schemaOrg_40221e30-1414-11e9-8fb8-00505688346e.json");
    }

    /**
     * Update test files when XSLT changes.
     */
    private void updateTestFiles() throws Exception {
        createTestFiles(RECORD_000332, RECORD_JB000132, RECORD_KHP0001_001, RECORD_KE066530, RECORD_DPK000107,
                RECORD_ANSK, RECORD_DNF, RECORD_40221e);
    }
    private void createTestFiles(String... records) throws Exception {
        if (1 == 1) {
            throw new IllegalStateException(
                    "The MODS2SCHEMAORG XSTL is faulty and does not generate the proper URL to images (the image ID + " +
                    "more is missing). This must be fixed before generating new testfiles.");
        }
        for (String record : records) {
            Map<String, String> injections = Map.of("imageserver", "https://example.com/imageserver/");
            String schemaOrgString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SCHEMAORG, record, injections);
            String filename = record.replaceAll("xml/copyright_extraction/", "schemaOrg_");
            String completeFilename = filename.replaceAll("\\..+", ".json");
            //String completeFilename = filename.replaceAll("\\.tif\\.xml", ".json");
            try (PrintWriter out = new PrintWriter(new FileWriter(JSON_ROOT + completeFilename, Charset.defaultCharset()))) {
                out.write(schemaOrgString);
            }
        }
    }

    /**
     * Import test file to assert JSON strings against.
     * @param path of file to load
     * @return the file as a string
     */
    private String importTestFile(String path) throws IOException {
        Path fileName = Path.of(path);
        String jsonString = Files.readString(fileName);

        return jsonString;
    }

    /**
     * Perform a transformation of the given {@code xml} using the given {@code xslt}.
     * The {@link XSLTTransformer} is used with injection {@code imageserver: "https://example.com/imageserver/"}.
     * <br/>
     * The helper expects the output to be JSON and comparison is done with pretty printed JSON for easy visuel
     * comparison.
     * <br/>
     * The {@code XSLTTransformer} used is created directly.
     * @param xslt the transforming stylesheet.
     * @param xml  the xml to transform.
     * @param expectedJSONFile the expected result, relative to {@code src/test/resources/schemaOrgJsonTestFiles/}.
     */
    private void assertJSONTransformation(String xslt, String xml, String expectedJSONFile) throws Exception {
        Map<String, String> injections = Map.of("imageserver", "https://example.com/imageserver/");
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(xslt, xml, injections);

        assertJSON(expectedJSONFile, transformedJSON);
    }

    /**
     * Perform a transformation of the given {@code xml} using the given {@code xslt}.
     * The {@link XSLTTransformer} is used with injection {@code imageserver: "https://example.com/imageserver/"}.
     * <br/>
     * The helper expects the output to be JSON and comparison is done with pretty printed JSON for easy visuel
     * comparison.
     * <br/>
     * The {@code XSLTTransformer} used is created using {@link XSLTFactory}.
     * @param xslt the transforming stylesheet.
     * @param xml  the xml to transform.
     * @param expectedJSONFile the expected result, relative to {@code src/test/resources/schemaOrgJsonTestFiles/}.
     */
    private void assertJSONTransformationFactory(String xslt, String xml, String expectedJSONFile) throws Exception {
        String yamlStr =
                "stylesheet: '" + xslt + "'\n" +
                "injections:\n" +
                "  - imageserver: 'http:/example/imageserver/'\n";
        YAML yaml = YAML.parse(new ByteArrayInputStream(yamlStr.getBytes(StandardCharsets.UTF_8)));
        String transformedJSON = TestUtil.getTransformedFromConfigWithAccessFields(yaml, xml);

        assertJSON(expectedJSONFile, transformedJSON);
    }

    /**
     * Load JSON from the {@code expectedJSONFile} and compare it to {@code actualJSON},
     * where both JSONs are pretty printed.
     * @param expectedJSONFile a file containing the expected JSON.
     * @param actualJSON a String with the actual JSON.
     */
    private void assertJSON(String expectedJSONFile, String actualJSON) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement je = JsonParser.parseString(actualJSON);
        String transformedPrettyJSON = gson.toJson(je);

        String expectedJSON = importTestFile(JSON_ROOT + expectedJSONFile);
        String expectedPrettyJSON = gson.toJson(JsonParser.parseString(expectedJSON));

        Assertions.assertEquals(expectedPrettyJSON, transformedPrettyJSON);
    }

}
