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
    public static final String RECORD_a8afb121 = "internal_test_files/tvMetadata/a8afb121-e8b8-467a-8704-10dc42356ac4.xml";
    public static final String RECORD_3945e2d1 = "internal_test_files/tvMetadata/3945e2d1-83a2-40d8-af1c-30f7b3b94390.xml";
    public static final String RECORD_9d9785a8 = "internal_test_files/tvMetadata/9d9785a8-71f4-4b34-9a0e-1c99c13b001b.xml";
    public static final String RECORD_c6fde2f4 = "internal_test_files/tvMetadata/c6fde2f4-036a-4e04-b83a-39a92021460b.xml";
    public static final String RECORD_68b233c3 = "internal_test_files/tvMetadata/68b233c3-f234-4546-914e-dc912f6001ae.xml";
    public static final String RECORD_df3dc9cf = "internal_test_files/tvMetadata/df3dc9cf-43f6-4a8a-8909-de8b0fb7bd00.xml";


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
        printSchemaOrgJson(RECORD_68b233c3);
        printSchemaOrgJson(RECORD_1F3A6A66);
        printSchemaOrgJson(RECORD_44979f67);
    }

    @Test
    void testCollection() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, RECORD_44979f67);
        Assertions.assertTrue(transformedJSON.contains("\"isPartOf\":[" +
                                                        "{\"@type\":\"Collection\"," +
                                                        "\"name\":\"Det Kgl. Bibliotek; Radio\\/TV-Samlingen\"}"));
    }
    @Test
    void testContentUrl() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithVideoChildAdded(PRESERVICA2SCHEMAORG, RECORD_5a5357be, null);
        Assertions.assertTrue(transformedJSON.contains("\"contentUrl\":\"www.example.com\\/streaming\\/mp4:bart-access-copies-tv\\/cf\\/1d\\/b0\\/cf1db0e1-ade2-462a-a2b4-7488244fcca7\\/playlist.m3u8\""));
    }

    @Test
    void testName() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, RECORD_44979f67);
        Assertions.assertTrue(transformedJSON.contains("\"name\":\"Backstage II\""));
    }

    @Test
    void testBroadcastDisplayName() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, RECORD_44979f67);
        Assertions.assertTrue(transformedJSON.contains("\"broadcastDisplayName\":\"DR Ultra\"}"));
    }

    @Test
    void testStartAndEndDates() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, RECORD_5a5357be);
        Assertions.assertTrue(transformedJSON.contains("\"startTime\":\"2021-01-18T00:00:00Z\"") &&
                                       transformedJSON.contains("\"endTime\":\"2021-01-18T00:30:00Z\""));
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
                && transformedJSON.contains("{\"@type\":\"PropertyValue\",\"PropertyID\":\"kuanaId\",\"value\":\"1f3a6a66-5f5a-48e6-abbf-452552320176\"}")
                && transformedJSON.contains("{\"@type\":\"PropertyValue\",\"PropertyID\":\"InternalAccessionRef\",\"value\":\"c4aa8cf0-3473-4e0f-8738-16b548bc1e34\"}")
                && transformedJSON.contains("{\"@type\":\"PropertyValue\",\"PropertyID\":\"PID\",\"value\":\"109.3.1\\/1f3a6a66-5f5a-48e6-abbf-452552320176\"}"));
    }


    @Test
    void noAlternateName() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, RECORD_1F3A6A66);
        Assertions.assertFalse(transformedJSON.contains("alternateName"));
    }

    @Test
    void testLiveStatus() throws IOException {
        String isLive = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, RECORD_74e22fd8);
        Assertions.assertTrue(isLive.contains("\"isLiveBroadcast\":true"));

        String notLive = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, RECORD_5a5357be);
        printSchemaOrgJson(RECORD_5a5357be);
        Assertions.assertTrue(notLive.contains("\"isLiveBroadcast\":false"));
    }

    @Test
    void testEpisodeNumberAndSeriesLength() throws IOException{
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, RECORD_a8afb121);
        Assertions.assertTrue(transformedJSON.contains("\"episodeNumber\":4"));
        Assertions.assertTrue(transformedJSON.contains("\"numberOfEpisodes\":6"));
    }

    @Test
    void testEpisodeNumberNoSeries() throws IOException{
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, RECORD_44979f67);
        Assertions.assertTrue(transformedJSON.contains("\"episodeNumber\":3"));
        Assertions.assertFalse(transformedJSON.contains("numberOfEpisodes"));
    }

    @Test
    void testNoEpisodeInfo() throws  IOException{
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, RECORD_74e22fd8);
        Assertions.assertFalse(transformedJSON.contains("episodeNumber"));
        Assertions.assertFalse(transformedJSON.contains("numberOfEpisodes"));
    }

    @Test
    void testEpisodeName() throws IOException{
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, RECORD_3945e2d1);
        Assertions.assertTrue(transformedJSON.contains("\"encodesCreativeWork\":{" +
                                                        "\"@type\":\"TVEpisode\"," +
                                                        "\"name\":\"Kagerester\","));
    }

    @Test
    void testNoEpisodeName() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, RECORD_44979f67);
        Assertions.assertTrue(transformedJSON.contains("\"encodesCreativeWork\":{" +
                                                        "\"@type\":\"TVEpisode\"," +
                                                        "\"episodeNumber\":3"));
    }

    @Test
    void testEmptyEpisodeName() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, RECORD_9d9785a8);
        Assertions.assertTrue(transformedJSON.contains("\"encodesCreativeWork\":{" +
                "\"@type\":\"TVEpisode\"," +
                "\"episodeNumber\":4"));
    }

    @Test
    void testTypeExtraction() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, RECORD_74e22fd8);
        Assertions.assertTrue(transformedJSON.startsWith("{\"@context\":\"http:\\/\\/schema.org\\/\",\"@type\":\"VideoObject\""));
    }

    @Test
    void testKeywords() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, RECORD_a8afb121);
        Assertions.assertFalse(transformedJSON.contains("\"keywords\":\"Serier,Krimiserie\","));
    }

    @Test
    void testNoKeywords() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, RECORD_68b233c3);
        Assertions.assertFalse(transformedJSON.contains("\"keywords\":"));
    }

    @Test
    void testAbstractCreation() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, RECORD_a8afb121);
        Assertions.assertTrue(transformedJSON.contains("\"abstract\":\"Eng. krimiserie\""));
    }

    @Test
    void testNoAbstract() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, RECORD_c6fde2f4);
        Assertions.assertFalse(transformedJSON.contains("\"abstract\":"));
    }


    @Test
    void testVideoQuality() throws IOException {
        String ikkeHD = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, RECORD_a8afb121);
        Assertions.assertTrue(ikkeHD.contains("\"videoQuality\":\"ikke hd\""));

        String notSpeficied = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, RECORD_1F3A6A66);
        Assertions.assertFalse(notSpeficied.contains("\"videoQuality\":"));
    }

    @Test
    void testDatePublished() throws IOException {
        String notPremiere = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, RECORD_a8afb121);
        Assertions.assertFalse(notPremiere.contains("\"datePublished\":"));
        // TODO: Add test for datePublished. where the value is present, either by creating a test record with premiere:premiere or by finding a record with that value

    }

    @Test
    void testProgramStructure() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, RECORD_1F3A6A66);
        Assertions.assertTrue(transformedJSON.contains("\"kb:program_structure_missing_seconds_start\":\"0\"")
                                    && transformedJSON.contains("\"kb:program_structure_missing_seconds_end\":\"0\""));
        //TODO: add tests for fields 'holes' and 'overlaps' with a constructed test file.
    }

    @Test
    void testGenre() throws IOException {
        String hasGenre = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, RECORD_3945e2d1);
        System.out.println(hasGenre);
        Assertions.assertTrue(hasGenre.contains("\"genre\":\"Underholdning\""));

        String noGenre = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, RECORD_1F3A6A66);
        Assertions.assertFalse(noGenre.contains("\"genre\":"));

        String emptyGenre = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, RECORD_68b233c3);
        Assertions.assertFalse(emptyGenre.contains("\"genre\":"));
    }

    @Test
    void testKBInternalMap() throws IOException {
        // TODO: Add individual tests for all params
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, RECORD_74e22fd8);
        Assertions.assertTrue(transformedJSON.contains("\"kb:internal\":{" +
                                                "\"kb:genre_sub\":\"Alle\"," +
                                                "\"kb:aspect_ratio\":\"16:9\"," +
                                                "\"kb:surround_sound\":false," +
                                                "\"kb:color\":true," +
                                                "\"kb:premiere\":false," +
                                                "\"kb:format_identifier_ritzau\":\"81213310\"," +
                                                "\"kb:format_identifier_nielsen\":\"101|20220526|140000|180958|0|9629d8b8-b751-450f-bfd7-d2510910bb34|69\"," +
                                                "\"kb:retransmission\":false," +
                                                "\"kb:maingenre_id\":\"1\"," +
                                                "\"kb:channel_id\":3," +
                                                "\"kb:country_of_origin_id\":\"0\"," +
                                                "\"kb:ritzau_program_id\":\"25101114\"," +
                                                "\"kb:program_ophold\":false," +
                                                "\"kb:subgenre_id\":\"708\"," +
                                                "\"kb:episode_id\":\"0\"," +
                                                "\"kb:season_id\":\"0\"," +
                                                "\"kb:series_id\":\"0\"," +
                                                "\"kb:has_subtitles\":false," +
                                                "\"kb:has_subtitles_for_hearing_impaired\":false," +
                                                "\"kb:is_teletext\":false," +
                                                "\"kb:showviewcode\":\"0\"," +
                                                "\"kb:padding_seconds\":15," +
                                                "\"kb:access_individual_prohibition\":\"Nej\"," +
                                                "\"kb:access_claused\":\"Nej\"," +
                                                "\"kb:access_malfunction\":\"Nej\"" +
                                                "}")
        );
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
