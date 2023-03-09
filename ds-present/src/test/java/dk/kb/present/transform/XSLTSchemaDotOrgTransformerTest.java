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

        Assertions.assertTrue(schemaOrgString.contains("\"id\":\"05fea810-7181-11e0-82d7-002185371280\""));
        Assertions.assertTrue(schemaOrgString.contains("\"dateCreated\":\"1899\""));
        Assertions.assertTrue(schemaOrgString.contains("\"temporal\":\"Created between 1850 and 1899\""));
    }

    @Test
    void testDateCreatedNoTemporal() throws Exception {
        String schemaOrgString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SCHEMAORG, RECORD_KHP0001_001);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement je = JsonParser.parseString(schemaOrgString);
        String prettyJsonString = gson.toJson(je);
        System.out.println(prettyJsonString );

        Assertions.assertTrue(schemaOrgString.contains("\"id\":\"e5a0e980-d6cb-11e3-8d2e-0016357f605f\""));
        Assertions.assertTrue(schemaOrgString.contains("\"dateCreated\":\"1942\""));
        Assertions.assertFalse(schemaOrgString.contains("\"temporal\""));

    }
}
