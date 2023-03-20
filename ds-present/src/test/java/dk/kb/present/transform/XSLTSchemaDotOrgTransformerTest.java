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

    @Test
    void testDateCreatedAndTemporal() throws Exception {

        String schemaOrgString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SCHEMAORG, RECORD_000332);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement je = JsonParser.parseString(schemaOrgString);
        String prettyJsonString = gson.toJson(je);
        //System.out.println(prettyJsonString );
        //System.out.println(schemaOrgString);

        String correctString = importTestFile("src/test/resources/schemaOrgJsonTestFiles/record_000332_schemaorg.json");

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

        String correctString = importTestFile("src/test/resources/schemaOrgJsonTestFiles/record_KHP0001_001_schemaorg.json");

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

        String correctString = importTestFile("src/test/resources/schemaOrgJsonTestFiles/record_JB000132_schemaorg.json");

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

        String correctString = importTestFile("src/test/resources/schemaOrgJsonTestFiles/record_KE066530_schemaorg.json");
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

        String correctString = importTestFile("src/test/resources/schemaOrgJsonTestFiles/record_DPK000107_schemaorg.json");
        Assertions.assertEquals(schemaOrgString,correctString);
    }


    @Test
    void testMaterialSize() throws Exception {
        String schemaOrgString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SCHEMAORG, RECORD_ANSK);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement je = JsonParser.parseString(schemaOrgString);
        String prettyJsonString = gson.toJson(je);
        //System.out.println(prettyJsonString);
        //System.out.println(schemaOrgString);

        String correctString = importTestFile("src/test/resources/schemaOrgJsonTestFiles/record_ANSK_schemaorg.json");
        Assertions.assertEquals(schemaOrgString,correctString);
    }

    @Test
    void testInternalNotesToKbAdmin() throws Exception {
        String schemaOrgString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SCHEMAORG, RECORD_DNF);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement je = JsonParser.parseString(schemaOrgString);
        String prettyJsonString = gson.toJson(je);
        //System.out.println(prettyJsonString);
        //System.out.println(schemaOrgString);

        String correctString = importTestFile("src/test/resources/schemaOrgJsonTestFiles/record_DNF_schemaorg.json");
        Assertions.assertEquals(schemaOrgString,correctString);
    }

    /**
     * Method used to create test files.
     * @param jsonString to save to file.
     */
    private void createTestFile(String filename, String jsonString) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter("src/test/resources/schemaOrgJsonTestFiles/" + filename))) {
            Gson gson = new Gson();
            out.write(jsonString);
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
