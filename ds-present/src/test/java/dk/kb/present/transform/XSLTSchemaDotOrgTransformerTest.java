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

    @Test
    void testDateCreatedAndTemporal() throws Exception {

        String schemaOrgString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SCHEMAORG, RECORD_000332);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement je = JsonParser.parseString(schemaOrgString);
        String prettyJsonString = gson.toJson(je);
        System.out.println(prettyJsonString );
        //System.out.println(schemaOrgString);

        Assertions.assertTrue(schemaOrgString.contains("\"id\":\"05fea810-7181-11e0-82d7-002185371280\""));
        Assertions.assertTrue(schemaOrgString.contains("\"dateCreated\":\"1899\""));
        Assertions.assertTrue(schemaOrgString.contains("\"temporal\":\"Created between 1850 and 1899\""));
        Assertions.assertTrue(schemaOrgString.contains("\"creator\":[{\"@type\":\"Person\",\"name\":\"Emil Bærentzen\",\"givenName\":\"Emil\",\"familyName\":\"Bærentzen\",\"birthDate\":\"1799-10-30\",\"deathDate\":\"1868-2-14\","));
        Assertions.assertTrue(schemaOrgString.contains("\"about\":[{\"@type\":\"Person\",\"name\":\"Otto Joachim Moltke\",\"givenName\":\"Otto Joachim\",\"familyName\":\"Moltke\",\"birthDate\":\"1770-0-0\",\"deathDate\":\"1853-0-0\"}"));
    }

    @Test
    void testDateCreatedNoTemporal() throws Exception {
        String schemaOrgString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SCHEMAORG, RECORD_KHP0001_001);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement je = JsonParser.parseString(schemaOrgString);
        String prettyJsonString = gson.toJson(je);
        System.out.println(prettyJsonString);
        //System.out.println(schemaOrgString);

        Assertions.assertTrue(schemaOrgString.contains("\"id\":\"e5a0e980-d6cb-11e3-8d2e-0016357f605f\""));
        Assertions.assertTrue(schemaOrgString.contains("\"dateCreated\":\"1942\""));
        Assertions.assertTrue(schemaOrgString.contains("\"about\":[\"Træ\",\"Sne\",\"Vinter\",\"Vej\",\"KBpublicering\"]"));
        Assertions.assertTrue(schemaOrgString.contains("\"creator\":[{\"@type\":\"Person\",\"name\":\"Keld Helmer-Petersen\",\"givenName\":\"Keld\",\"familyName\":\"Helmer-Petersen\",\"birthDate\":\"1920-8-23\",\"deathDate\":\"2013-3-6\"}]"));
        Assertions.assertFalse(schemaOrgString.contains("\"temporal\""));

    }

    @Test
    void testCreators() throws Exception {
        String schemaOrgString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SCHEMAORG, RECORD_JB000132);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement je = JsonParser.parseString(schemaOrgString);
        String prettyJsonString = gson.toJson(je);

        System.out.println(prettyJsonString);
        //System.out.println(schemaOrgString);


        Assertions.assertTrue(schemaOrgString.contains("\"id\":\"770379f0-8a0d-11e1-805f-0016357f605f\""));
        Assertions.assertTrue(schemaOrgString.contains("\"dateCreated\":\"1974\""));
        Assertions.assertTrue(schemaOrgString.contains("\"creator\":[{\"@type\":\"Person\",\"name\":\"John R. (John Rosforth) Johnsen\",\"givenName\":\"John R. (John Rosforth)\",\"familyName\":\"Johnsen\",\"birthDate\":\"1945-0-0\",\"deathDate\":\"2016-0-0\"}]"));
        Assertions.assertTrue(schemaOrgString.contains("\"about\":[{\"@type\":\"Person\",\"name\":\"Mette Hønningen\",\"givenName\":\"Mette\",\"familyName\":\"Hønningen\",\"birthDate\":\"1944-10-3\",\"affiliation\":\"Den Kongelige Ballet\"}," +
                "{\"@type\":\"Person\",\"name\":\"Mette-Ida Kirk\",\"givenName\":\"Mette-Ida\",\"familyName\":\"Kirk\",\"birthDate\":\"1955-1-11\",\"affiliation\":\"Den Kongelige Ballet\"}," +
                "{\"@type\":\"Person\",\"name\":\"Henning Kronstam\",\"givenName\":\"Henning\",\"familyName\":\"Kronstam\",\"birthDate\":\"1934-6-29\",\"deathDate\":\"1995-5-28\",\"affiliation\":\"Den Kongelige Ballet\"}," +
                "{\"@type\":\"Person\",\"name\":\"John Neumeier\",\"givenName\":\"John\",\"familyName\":\"Neumeier\",\"birthDate\":\"1939-2-24\",\"affiliation\":\"Den Kongelige Ballet\"}," +
                "\"ballet\",\"dans\",\"balletfotos\",\"KBpublicering\"]"));


    }
}
