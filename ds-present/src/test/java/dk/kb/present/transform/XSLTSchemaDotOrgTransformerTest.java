package dk.kb.present.transform;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dk.kb.present.TestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Locale;

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

        Assertions.assertTrue(schemaOrgString.contains("\"id\":\"05fea810-7181-11e0-82d7-002185371280\""));
        Assertions.assertTrue(schemaOrgString.contains("\"@type\":\"Photograph\""));
        Assertions.assertTrue(schemaOrgString.contains("\"dateCreated\":\"1899\""));
        Assertions.assertTrue(schemaOrgString.contains("\"temporal\":\"Created between 1850 and 1899\""));
        Assertions.assertTrue(schemaOrgString.contains("\"creator\":[{\"@type\":\"Person\",\"name\":\"Emil Bærentzen\",\"givenName\":\"Emil\",\"familyName\":\"Bærentzen\",\"birthDate\":\"1799-10-30\",\"deathDate\":\"1868-2-14\",\"affiliation\":\"Em. Bærentzen & Co. lith. Inst.\",\"hasOccupation\":{\"@type\":\"Occupation\",\"name\":\"maler, litograf\"}}," +
                "{\"@type\":\"Person\",\"name\":\"Heinrich August Georg Schiøtt\",\"givenName\":\"Heinrich August Georg\",\"familyName\":\"Schiøtt\",\"birthDate\":\"1823-12-17\",\"deathDate\":\"1895-6-25\",\"hasOccupation\":{\"@type\":\"Occupation\",\"name\":\"maler\"}}," +
                "{\"@type\":\"Person\",\"name\":\"Edvard Westerberg\",\"givenName\":\"Edvard\",\"familyName\":\"Westerberg\",\"birthDate\":\"1824-11-8\",\"deathDate\":\"1865-3-8\",\"hasOccupation\":{\"@type\":\"Occupation\",\"name\":\"tegner, litograf\"}}]"));
    }

    @Test
    void testDateCreatedNoTemporal() throws Exception {
        String schemaOrgString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SCHEMAORG, RECORD_KHP0001_001);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement je = JsonParser.parseString(schemaOrgString);
        String prettyJsonString = gson.toJson(je);
        //System.out.println(prettyJsonString);
        //System.out.println(schemaOrgString);

        Assertions.assertTrue(schemaOrgString.contains("\"id\":\"e5a0e980-d6cb-11e3-8d2e-0016357f605f\""));
        Assertions.assertTrue(schemaOrgString.contains("\"dateCreated\":\"1942\""));
        Assertions.assertTrue(schemaOrgString.contains("\"about\":[\"Træ\",\"Sne\",\"Vinter\",\"Vej\",\"KBpublicering\"]"));
        Assertions.assertTrue(schemaOrgString.contains("\"creator\":[{\"@type\":\"Person\",\"name\":\"Keld Helmer-Petersen\",\"givenName\":\"Keld\",\"familyName\":\"Helmer-Petersen\",\"birthDate\":\"1920-8-23\",\"deathDate\":\"2013-3-6\",\"hasOccupation\":{\"@type\":\"Occupation\",\"name\":\"fotograf\"}}]"));
        Assertions.assertFalse(schemaOrgString.contains("\"temporal\""));

    }

    @Test
    void testCreatorsAndHeadline() throws Exception {
        String schemaOrgString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SCHEMAORG, RECORD_JB000132);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement je = JsonParser.parseString(schemaOrgString);
        String prettyJsonString = gson.toJson(je);
        //System.out.println(prettyJsonString);
        //System.out.println(schemaOrgString);

        Assertions.assertTrue(schemaOrgString.contains("\"id\":\"770379f0-8a0d-11e1-805f-0016357f605f\""));
        Assertions.assertTrue(schemaOrgString.contains("\"dateCreated\":\"1974\""));
        Assertions.assertTrue(schemaOrgString.contains("\"creator\":[{\"@type\":\"Person\",\"name\":\"John R. (John Rosforth) Johnsen\",\"givenName\":\"John R. (John Rosforth)\",\"familyName\":\"Johnsen\",\"birthDate\":\"1945-0-0\",\"deathDate\":\"2016-0-0\",\"hasOccupation\":{\"@type\":\"Occupation\",\"name\":\"fotograf\"}}]"));
        Assertions.assertTrue(schemaOrgString.contains("\"headline\":[{\"value\":\"Romeo og Julie\",\"@language\":\"da\"}]"));

    }


    @Test
    void testCreatorDescriptionAndContentNoteToAbout() throws Exception {
        String schemaOrgString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SCHEMAORG, RECORD_KE066530);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement je = JsonParser.parseString(schemaOrgString);
        String prettyJsonString = gson.toJson(je);
        //System.out.println(prettyJsonString);
        //System.out.println(schemaOrgString);

        Assertions.assertTrue(schemaOrgString.contains("\"creator\":[{\"@type\":\"Person\",\"name\":\"Mason Jackson\",\"givenName\":\"Mason\",\"familyName\":\"Jackson\",\"birthDate\":\"1819-5-25\",\"deathDate\":\"1903-12-28\",\"description\":\"britisk\"," +
                "\"hasOccupation\":{\"@type\":\"Occupation\",\"name\":\"xylograf\"}},"+
                "{\"@type\":\"Organization\",\"affiliation\":\"The Illustrated London News\",\"description\":\"engelsk avis\"}]"));

        Assertions.assertTrue(schemaOrgString.contains("\"F. 3639\",\"Efter fotografi af: Petersen, Jens (19.3.1829-1.2.1905) fotograf\",\"Trykt i: The Illustrated London News, 28.11.1863\"]"));
    }

    @Test
    void testContentLocationAndKeywords() throws Exception {
        String schemaOrgString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SCHEMAORG, RECORD_DPK000107);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement je = JsonParser.parseString(schemaOrgString);
        String prettyJsonString = gson.toJson(je);
        //System.out.println(prettyJsonString);
        //System.out.println(schemaOrgString);

        Assertions.assertTrue(schemaOrgString.contains("\"keywords\":[\"Postkortsamlingen\",\"Vestindien\",\"Postkort\",\"CAR- BLO katagori\"]"));
        Assertions.assertTrue(schemaOrgString.contains("\"contentLocation\":[{\"@type\":\"Place\",\"description\":\"Vestindien, Sankt Thomas, Charlotte Amalie, Fort Christian\"}]"));
    }


    @Test
    void testMaterialSize() throws Exception {
        String schemaOrgString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SCHEMAORG, RECORD_ANSK);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement je = JsonParser.parseString(schemaOrgString);
        String prettyJsonString = gson.toJson(je);
        System.out.println(prettyJsonString);
        //System.out.println(schemaOrgString);

        Assertions.assertTrue(schemaOrgString.contains("\"material\":{\"size\":{\"@type\":\"Text\",\"value\":\"23,5 x 16,5 cm.\"}"));
    }

    @Test
    void testInternalNotesToKbAdmin() throws Exception {
        String schemaOrgString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SCHEMAORG, RECORD_DNF);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement je = JsonParser.parseString(schemaOrgString);
        String prettyJsonString = gson.toJson(je);
        // System.out.println(prettyJsonString);
        //System.out.println(schemaOrgString);

        Assertions.assertTrue(schemaOrgString.contains("\"kb:internalNotes\":[\"Montering: opklæbet på karton og monteret i passepartout\",\"Kunstnernote: Bisson, Louis-Auguste (1814–1876) fransk fotograf\",\"Bisson, Auguste-Rosalie (1826–1900) fransk fotograf\",\"Aktiv: 1852–1863\"]},"));
    }

    @Test
    void testTemplate() throws Exception {
        String schemaOrgString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SCHEMAORG, RECORD_DPK000107);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement je = JsonParser.parseString(schemaOrgString);
        String prettyJsonString = gson.toJson(je);
        System.out.println(prettyJsonString);
        //System.out.println(schemaOrgString);
    }
}
