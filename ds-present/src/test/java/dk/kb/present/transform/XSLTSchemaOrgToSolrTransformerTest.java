package dk.kb.present.transform;

import dk.kb.present.TestUtil;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class XSLTSchemaOrgToSolrTransformerTest {
    private static final Logger log = LoggerFactory.getLogger(XSLTSchemaOrgToSolrTransformerTest.class);
    public static final String PRESERVICA2SCHEMAORG = "xslt/preservica2schemaorg.xsl";
    public static final String SCHEMA2SOLR = "xslt/schemaorg2solr.xsl";
    public static final String RECORD_5a5357be = "internal_test_files/tvMetadata/5a5357be-5890-472a-a294-41a99f108936.xml";
    public static final String RECORD_44979f67 = "internal_test_files/tvMetadata/44979f67-b563-462e-9bf1-c970167a5c5f.xml";




    @Test
    public void testSetup() throws IOException {
        String schemaJson = inputToSchemaJson(RECORD_44979f67);
        //schemaJson = "<data>"+schemaJson+"</data>";


        printSolrJsonFromSchemaOrgJson(schemaJson);

    }


    public static void printSolrJsonFromSchemaOrgJson(String schemaOrgJson) throws IOException {
        String placeholderXml = "placeholder.xml";
        Map<String, String> injections = Map.of("schemaorgjson", schemaOrgJson);
        String solrJson = TestUtil.getTransformedWithAccessFieldsAdded(SCHEMA2SOLR, placeholderXml, injections);

        System.out.println("Solr document below:");
        System.out.println(solrJson);
        /*Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement je = JsonParser.parseString(solrJson);
        String transformedPrettyJSON = gson.toJson(je);

        System.out.println(transformedPrettyJSON);*/

    }

    private static String inputToSchemaJson(String xml) throws IOException {
        Map<String, String> injections = Map.of("imageserver", "https://example.com/imageserver/");
        String schemaOrgJson = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, xml, injections);
        System.out.println(schemaOrgJson);
        return schemaOrgJson;
    }

}
