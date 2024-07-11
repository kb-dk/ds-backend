package dk.kb.present.transform;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dk.kb.present.TestFiles;
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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class XSLTCumulusToSchemaDotOrgTransformerTest extends XSLTTransformerTestBase{
    private static final Logger log = LoggerFactory.getLogger(XSLTCumulusToSchemaDotOrgTransformerTest.class);
    public static final String JSON_ROOT = "src/test/resources/schemaOrgJsonTestFiles/";
    public static final String MODS2SCHEMAORG = "xslt/mods2schemaorg.xsl";
    @Override
    String getXSLT() {
        return MODS2SCHEMAORG;
    }

    // Does not seem to be needed for these tests
    @BeforeAll
    public static void fixConfiguration() throws IOException {
        ServiceConfig.initialize("conf/ds-present-behaviour.yaml");
    }

    @Test
    void testDateCreatedAndTemporal() throws Exception {
        assertJSONTransformation(MODS2SCHEMAORG, TestFiles.CUMULUS_RECORD_05fea810, "schemaOrg_05fea810-7181-11e0-82d7-002185371280.json");
    }

    @Test
    void testDateCreatedNoTemporal() throws Exception {
        assertJSONTransformation(MODS2SCHEMAORG, TestFiles.CUMULUS_RECORD_e5a0e980, "schemaOrg_e5a0e980-d6cb-11e3-8d2e-0016357f605f.json");
    }

    @Test
    void testCreatorsAndHeadline() throws Exception {
        assertJSONTransformation(MODS2SCHEMAORG, TestFiles.CUMULUS_RECORD_770379f0, "schemaOrg_770379f0-8a0d-11e1-805f-0016357f605f.json");
    }

    // Same functionality as testCreatorsAndHeadline but using a different XSLTFactory creation method
    @Test
    void testFactory() throws Exception {
        assertJSONTransformationFactory(MODS2SCHEMAORG, TestFiles.CUMULUS_RECORD_770379f0, "schemaOrg_770379f0-8a0d-11e1-805f-0016357f605f.json");
    }


    @Test
    void testCreatorDescriptionAndContentNoteToAbout() throws Exception {
        assertJSONTransformation(MODS2SCHEMAORG, TestFiles.CUMULUS_RECORD_f4668ad0, "schemaOrg_f4668ad0-f334-11e8-b74f-00505688346e.json");
    }

    @Test
    void testContentLocationAndKeywords() throws Exception {
        assertJSONTransformation(MODS2SCHEMAORG, TestFiles.CUMULUS_RECORD_3956d820, "schemaOrg_3956d820-7b7d-11e6-b2b3-0016357f605f.json");
    }


    @Test
    void testMaterialSize() throws Exception {
        assertJSONTransformation(MODS2SCHEMAORG, TestFiles.CUMULUS_RECORD_ANSK, "schemaOrg_ANSK_11614.json");
    }

    @Test
    void testInternalNotesToKbAdmin() throws Exception {
        assertJSONTransformation(MODS2SCHEMAORG, TestFiles.CUMULUS_RECORD_DNF, "schemaOrg_DNF_1951-00352_00052.json");
    }

    @Test
    void testImageUrlCreation () throws Exception {
        assertJSONTransformation(MODS2SCHEMAORG, TestFiles.CUMULUS_RECORD_40221e30, "schemaOrg_40221e30-1414-11e9-8fb8-00505688346e.json");
    }



    private void updateTestFiles() throws Exception {
        createTestFiles(TestFiles.CUMULUS_RECORD_05fea810, TestFiles.CUMULUS_RECORD_770379f0, TestFiles.CUMULUS_RECORD_e5a0e980, TestFiles.CUMULUS_RECORD_f4668ad0, TestFiles.CUMULUS_RECORD_3956d820,
                TestFiles.CUMULUS_RECORD_ANSK, TestFiles.CUMULUS_RECORD_DNF, TestFiles.CUMULUS_RECORD_40221e30);
    }
    private void createTestFiles(String... records) throws Exception {
        for (String record : records) {
            Map<String, String> injections = Map.of("imageserver", "https://example.com/imageserver/",
                                                    "origin", "ds.test");
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
        Map<String, String> injections = Map.of("imageserver", "https://example.com/imageserver/",
                                                "origin", "ds.test");
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
                "  - imageserver: 'https://example.com/imageserver/'\n";
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

        //System.out.println(transformedPrettyJSON);
        //System.out.println(expectedPrettyJSON);

        Assertions.assertEquals(expectedPrettyJSON, transformedPrettyJSON);
    }

}
