package dk.kb.present.transform;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dk.kb.present.TestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileWriter;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class XSLTSchemaDotOrgTransformerTest {
    public static final String MODS2SCHEMAORG = "xslt/mods2schemaorg.xsl";
    public static final String RECORD_000332 = "xml/copyright_extraction/000332.tif.xml";
    public static final String RECORD_JB000132 = "xml/copyright_extraction/JB000132_114.tif.xml";
    public static final String RECORD_KHP0001_001 = "xml/copyright_extraction/KHP0001-001.tif.xml";
    public static final String RECORD_KE066530 = "xml/copyright_extraction/KE066530.tif.xml";
    public static final String RECORD_DPK000107 = "xml/copyright_extraction/DPK000107.tif.xml";
    public static final String RECORD_ANSK = "xml/copyright_extraction/ANSK_11614.tif.xml";
    public static final String RECORD_DNF = "xml/copyright_extraction/DNF_1951-00352_00052.tif.xml";
    public static final String RECORD_40221e = "xml/copyright_extraction/40221e30-1414-11e9-8fb8-00505688346e.xml";

    @Test
    void updateTestFiles() throws Exception {
        createTestFiles(RECORD_000332, RECORD_JB000132, RECORD_KHP0001_001, RECORD_KE066530, RECORD_DPK000107,
                RECORD_ANSK, RECORD_DNF, RECORD_40221e);
    }
    @Test
    void testDateCreatedAndTemporal() throws Exception {

        String schemaOrgString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SCHEMAORG, RECORD_000332);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement je = JsonParser.parseString(schemaOrgString);
        String prettyJsonString = gson.toJson(je);
        //System.out.println(prettyJsonString);
        //System.out.println(schemaOrgString);

        String correctString = importTestFile("src/test/resources/schemaOrgJsonTestFiles/schemaOrg_000332.json");

        Assertions.assertEquals(schemaOrgString, correctString);
        }

    @Test
    void testDateCreatedNoTemporal() throws Exception {
        String schemaOrgString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SCHEMAORG, RECORD_KHP0001_001);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement je = JsonParser.parseString(schemaOrgString);
        String prettyJsonString = gson.toJson(je);
        //System.out.println(prettyJsonString);
        //System.out.println(schemaOrgString);

        String correctString = importTestFile("src/test/resources/schemaOrgJsonTestFiles/schemaOrg_KHP0001-001.json");

        Assertions.assertEquals(schemaOrgString, correctString);
    }

    @Test
    void testCreatorsAndHeadline() throws Exception {
        String schemaOrgString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SCHEMAORG, RECORD_JB000132);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement je = JsonParser.parseString(schemaOrgString);
        String prettyJsonString = gson.toJson(je);
        //System.out.println(prettyJsonString);
        //System.out.println(schemaOrgString);

        String correctString = importTestFile("src/test/resources/schemaOrgJsonTestFiles/schemaOrg_JB000132_114.json");

        Assertions.assertEquals(schemaOrgString, correctString);
    }


    @Test
    void testCreatorDescriptionAndContentNoteToAbout() throws Exception {
        String schemaOrgString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SCHEMAORG, RECORD_KE066530);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement je = JsonParser.parseString(schemaOrgString);
        String prettyJsonString = gson.toJson(je);
        //System.out.println(prettyJsonString);
        //System.out.println(schemaOrgString);

        String correctString = importTestFile("src/test/resources/schemaOrgJsonTestFiles/schemaOrg_KE066530.json");
        Assertions.assertEquals(schemaOrgString, correctString);
    }

    @Test
    void testContentLocationAndKeywords() throws Exception {
        String schemaOrgString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SCHEMAORG, RECORD_DPK000107);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement je = JsonParser.parseString(schemaOrgString);
        String prettyJsonString = gson.toJson(je);
        //System.out.println(prettyJsonString);
        //System.out.println(schemaOrgString);

        String correctString = importTestFile("src/test/resources/schemaOrgJsonTestFiles/schemaOrg_DPK000107.json");
        Assertions.assertEquals(schemaOrgString, correctString);
    }


    @Test
    void testMaterialSize() throws Exception {
        String schemaOrgString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SCHEMAORG, RECORD_ANSK);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement je = JsonParser.parseString(schemaOrgString);
        String prettyJsonString = gson.toJson(je);
        //System.out.println(prettyJsonString);
        //System.out.println(schemaOrgString);

        String correctString = importTestFile("src/test/resources/schemaOrgJsonTestFiles/schemaOrg_ANSK_11614.json");
        Assertions.assertEquals(schemaOrgString, correctString);
    }

    @Test
    void testInternalNotesToKbAdmin() throws Exception {
        String schemaOrgString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SCHEMAORG, RECORD_DNF);

        Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
        JsonElement je = JsonParser.parseString(schemaOrgString);
        String prettyJsonString = gson.toJson(je);
        //System.out.println(prettyJsonString);
        //System.out.println(schemaOrgString);

        String correctString = importTestFile("src/test/resources/schemaOrgJsonTestFiles/schemaOrg_DNF_1951-00352_00052.json");
        Assertions.assertEquals(schemaOrgString, correctString);
    }

    @Test
    void testImageUrlCreation () throws Exception {
        String schemaOrgString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SCHEMAORG, RECORD_40221e);

        Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
        JsonElement je = JsonParser.parseString(schemaOrgString);
        String prettyJsonString = gson.toJson(je);
        //System.out.println(prettyJsonString);
        //System.out.println(schemaOrgString);

        String correctString = importTestFile("src/test/resources/schemaOrgJsonTestFiles/schemaOrg_40221e30-1414-11e9-8fb8-00505688346e.json");
        Assertions.assertEquals(schemaOrgString, correctString);

    }


    private void createTestFiles(String... records) throws Exception {
        for (String record : records) {
            String schemaOrgString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SCHEMAORG, record);
            String filename = record.replaceAll("xml/copyright_extraction/", "schemaOrg_");
            String completeFilename = filename.replaceAll("\\..+", ".json");
            //String completeFilename = filename.replaceAll("\\.tif\\.xml", ".json");
            try (PrintWriter out = new PrintWriter(new FileWriter("src/test/resources/schemaOrgJsonTestFiles/" + completeFilename, Charset.defaultCharset()))) {
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
}
