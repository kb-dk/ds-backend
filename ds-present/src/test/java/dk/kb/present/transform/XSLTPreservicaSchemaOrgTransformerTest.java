package dk.kb.present.transform;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dk.kb.present.TestFiles;
import dk.kb.present.TestUtil;
import dk.kb.util.Resolver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class XSLTPreservicaSchemaOrgTransformerTest extends XSLTTransformerTestBase {
    private static final Logger log = LoggerFactory.getLogger(XSLTCumulusToSchemaDotOrgTransformerTest.class);
    public static final String PRESERVICA2SCHEMAORG = "xslt/preservica2schemaorg.xsl";

    @Override
    String getXSLT() {
        return PRESERVICA2SCHEMAORG;
    }

    @BeforeAll
    public static void beforeMethod() {
        if (Resolver.getPathFromClasspath("internal_test_files/tvMetadata") == null){
            log.warn("Internal test files are not present. Unittest 'XSLTPreservicaSchemaOrgTransformerTest' is therefore not run.");
        }
        Assumptions.assumeTrue(Resolver.getPathFromClasspath("internal_test_files/tvMetadata") != null);
    }

    @Test
    public void testSetup() throws IOException {
        //printSchemaOrgJson(PVICA_RECORD_74e22fd8);
        printSchemaOrgJson(TestFiles.PVICA_RECORD_4f706cda);
        //printSchemaOrgJson(PVICA_RECORD_1F3A6A66);
        //printSchemaOrgJson(PVICA_RECORD_44979f67);
    }

    @Test
    void testCollection() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_44979f67);
        Assertions.assertTrue(transformedJSON.contains("\"isPartOf\":[" +
                                                        "{\"@type\":\"Collection\"," +
                                                        "\"name\":\"Det Kgl. Bibliotek; Radio\\/TV-Samlingen\"}"));
    }
    @Test
    void testContentUrl() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithVideoChildAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_5a5357be, null);
        Assertions.assertTrue(transformedJSON.contains("\"contentUrl\":\"www.example.com\\/streaming\\/mp4:bart-access-copies-tv\\/cf\\/1d\\/b0\\/cf1db0e1-ade2-462a-a2b4-7488244fcca7\\/playlist.m3u8\""));
    }
    @Test
    void testConditionOfAccess() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithVideoChildAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_5a5357be, null);
        Assertions.assertTrue(transformedJSON.contains("\"conditionsOfAccess\":\"placeholderCondition\""));
    }

    @Test
    void testName() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_44979f67);
        Assertions.assertTrue(transformedJSON.contains("\"name\":\"Backstage II\""));
    }

    @Test
    void testBroadcastDisplayName() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_44979f67);
        Assertions.assertTrue(transformedJSON.contains("\"broadcastDisplayName\":\"DR Ultra\",\"alternateName\":\"drultra\""));
    }

    @Test
    void testBroadcasterOrganization() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_44979f67);
        Assertions.assertTrue(transformedJSON.contains("\"legalName\":\"DR\""));
    }

    @Test
    void testStartAndEndDates() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_5a5357be);
        Assertions.assertTrue(transformedJSON.contains("\"startTime\":\"2021-01-18T00:00:00Z\"") &&
                                       transformedJSON.contains("\"endTime\":\"2021-01-18T00:30:00Z\""));
    }

    @Test
    void testDuration() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_5a5357be);
        // TODO: Fix the time format delivered by the XSLT even though this format is compliant with schema.org as it is part of ISO 8601.
        Assertions.assertTrue(transformedJSON.contains("\"duration\":\"PT30M\""));
    }

    @Test
    void testIdentifiers() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_1F3A6A66);
        Assertions.assertTrue(transformedJSON.contains("\"identifier\":[{\"@type\":\"PropertyValue\",\"PropertyID\":\"Origin\",\"value\":\"ds.test\"}")
                && transformedJSON.contains("{\"@type\":\"PropertyValue\",\"PropertyID\":\"kuanaId\",\"value\":\"1f3a6a66-5f5a-48e6-abbf-452552320176\"}")
                && transformedJSON.contains("{\"@type\":\"PropertyValue\",\"PropertyID\":\"InternalAccessionRef\",\"value\":\"c4aa8cf0-3473-4e0f-8738-16b548bc1e34\"}")
                && transformedJSON.contains("{\"@type\":\"PropertyValue\",\"PropertyID\":\"PID\",\"value\":\"109.3.1\\/1f3a6a66-5f5a-48e6-abbf-452552320176\"}"));
    }


    @Test
    void noAlternateName() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_1F3A6A66);
        Assertions.assertFalse(transformedJSON.contains("\"name\":\"Kunstnere i Kolding\",\"alternateName"));
    }

    @Test
    void testLiveStatus() throws IOException {
        String isLive = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_74e22fd8);
        Assertions.assertTrue(isLive.contains("\"isLiveBroadcast\":true"));

        String notLive = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_5a5357be);
        Assertions.assertTrue(notLive.contains("\"isLiveBroadcast\":false"));
    }

    @Test
    void testEpisodeNumberAndSeriesLength() throws IOException{
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_a8afb121);
        Assertions.assertTrue(transformedJSON.contains("\"episodeNumber\":4"));
        Assertions.assertTrue(transformedJSON.contains("\"numberOfEpisodes\":6"));
    }

    @Test
    void testEpisodeNumberNoSeries() throws IOException{
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_44979f67);
        Assertions.assertTrue(transformedJSON.contains("\"episodeNumber\":3"));
        Assertions.assertFalse(transformedJSON.contains("numberOfEpisodes"));
    }

    @Test
    void testNoEpisodeInfo() throws  IOException{
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_74e22fd8);
        Assertions.assertFalse(transformedJSON.contains("episodeNumber"));
        Assertions.assertFalse(transformedJSON.contains("numberOfEpisodes"));
    }

    @Test
    void testEpisodeName() throws IOException{
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_3945e2d1);
        Assertions.assertTrue(transformedJSON.contains("\"encodesCreativeWork\":{" +
                                                        "\"@type\":\"TVEpisode\"," +
                                                        "\"name\":\"Kagerester\","));
    }

    @Test
    void testNoEpisodeName() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_44979f67);
        Assertions.assertTrue(transformedJSON.contains("\"encodesCreativeWork\":{" +
                                                        "\"@type\":\"TVEpisode\"," +
                                                        "\"episodeNumber\":3"));
    }

    @Test
    void testEmptyEpisodeName() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_9d9785a8);
        Assertions.assertTrue(transformedJSON.contains("\"encodesCreativeWork\":{" +
                "\"@type\":\"TVEpisode\"," +
                "\"episodeNumber\":4"));
    }

    @Test
    void testTypeExtraction() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_74e22fd8);
        Assertions.assertTrue(transformedJSON.startsWith("{\"@context\":\"http:\\/\\/schema.org\\/\",\"@type\":\"VideoObject\""));
    }

    @Test
    void testKeywords() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_a8afb121);
        Assertions.assertFalse(transformedJSON.contains("\"keywords\":\"Serier,Krimiserie\","));
    }

    @Test
    void testNoKeywords() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_68b233c3);
        Assertions.assertFalse(transformedJSON.contains("\"keywords\":"));
    }

    @Test
    void testNoNullKeywords() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_3006e2f8);
        Assertions.assertFalse(transformedJSON.contains("null"));
    }

    @Test
    void testAbstractCreation() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_a8afb121);
        Assertions.assertTrue(transformedJSON.contains("\"abstract\":\"Eng. krimiserie\""));
    }

    @Test
    void testNoAbstract() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_c6fde2f4);
        Assertions.assertFalse(transformedJSON.contains("\"abstract\":"));
    }


    @Test
    void testVideoQuality() throws IOException {
        String ikkeHD = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_a8afb121);
        Assertions.assertTrue(ikkeHD.contains("\"videoQuality\":\"ikke hd\""));

        String notSpeficied = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_1F3A6A66);
        Assertions.assertFalse(notSpeficied.contains("\"videoQuality\":"));
    }

    @Test
    void testDatePublished() throws IOException {
        String notPremiere = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_a8afb121);
        Assertions.assertFalse(notPremiere.contains("\"datePublished\":"));
        // TODO: Add test for datePublished. where the value is present, either by creating a test record with premiere:premiere or by finding a record with that value

    }

    @Test
    void testProgramStructure() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_1F3A6A66);
        Assertions.assertTrue(transformedJSON.contains("\"kb:program_structure_missing_seconds_start\":\"0\"")
                                    && transformedJSON.contains("\"kb:program_structure_missing_seconds_end\":\"0\""));
        //TODO: add tests for fields 'holes' and 'overlaps' with a constructed test file.
    }

    @Test
    void testGenre() throws IOException {
        String hasGenre = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_3945e2d1);
        System.out.println(hasGenre);
        Assertions.assertTrue(hasGenre.contains("\"genre\":\"Underholdning\""));

        String noGenre = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_1F3A6A66);
        Assertions.assertFalse(noGenre.contains("\"genre\":"));

        String emptyGenre = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_68b233c3);
        Assertions.assertFalse(emptyGenre.contains("\"genre\":"));
    }

    @Test
    void noVideoQualityForRadioRecords() throws IOException {
        String radio = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_e683b0b8);
        Assertions.assertFalse(radio.contains("\"videoQuality\":"));
    }
    @Test
    void whiteProgramID() throws IOException {
        String radio = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_c295ae6c);
        Assertions.assertTrue(radio.contains("\"@type\":\"PropertyValue\"," +
                "\"PropertyID\":\"WhiteProgramID\"," +
                "\"value\":\"A-1966-03-20-P-0197_059\""));
    }
    @Test
    void noShowViewcodeForRadio() throws IOException {
        String radio = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_c295ae6c);
        Assertions.assertFalse(radio.contains("kb:showviewcode"));
    }

    @Test
    void noAspectRatioForRadio() throws IOException {
        String radio = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_e683b0b8);
        Assertions.assertFalse(radio.contains("\"kb:aspect_ratio\":"));
    }

    @Test
    void noColorForRadio() throws IOException {
        String radio = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_e683b0b8);
        Assertions.assertFalse(radio.contains("\"kb:color\":"));
    }
    @Test
    void noTeletextForRadio() throws IOException {
        String radio = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_e683b0b8);
        Assertions.assertFalse(radio.contains("\"kb:is_teletext\":"));
    }

    @Test
    void noSubtitlesForRadio() throws IOException {
        String radio = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_e683b0b8);
        Assertions.assertFalse(radio.contains("\"kb:has_subtitles\":"));
        Assertions.assertFalse(radio.contains("\"kb:has_subtitles_for_hearing_impaired\":"));
    }


    @Test
    void testKBInternalMap() throws IOException {
        // TODO: Add individual tests for all params
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_74e22fd8);
        Assertions.assertTrue(transformedJSON.contains("\"kb:surround_sound\":false"));
        Assertions.assertTrue(transformedJSON.contains("\"kb:color\":true"));
        Assertions.assertTrue(transformedJSON.contains("\"kb:premiere\":false"));
        Assertions.assertTrue(transformedJSON.contains("\"kb:retransmission\":false"));
        Assertions.assertTrue(transformedJSON.contains("\"kb:program_ophold\":false"));
        Assertions.assertTrue(transformedJSON.contains("\"kb:showviewcode\":\"0\""));
        Assertions.assertTrue(transformedJSON.contains("\"kb:padding_seconds\":15"));
    }
    @Test
    void testInternalGenreSub() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_74e22fd8);
        Assertions.assertTrue(transformedJSON.contains("\"kb:genre_sub\":\"Alle\""));
    }
    @Test
    void testInternalAspectRatio() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_74e22fd8);

        Assertions.assertTrue(transformedJSON.contains("\"kb:aspect_ratio\":\"16:9\""));
    }
    @Test
    void testInternalSubtitlesAndTeletext() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_74e22fd8);

        Assertions.assertTrue(transformedJSON.contains("\"kb:has_subtitles\":false," +
                                                        "\"kb:has_subtitles_for_hearing_impaired\":false," +
                                                        "\"kb:is_teletext\":false"));
    }
    @Test
    void testInternalAcces() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_74e22fd8);

        Assertions.assertTrue(transformedJSON.contains("\"kb:access_individual_prohibition\":\"Nej\"," +
                                                        "\"kb:access_claused\":\"Nej\"," +
                                                        "\"kb:access_malfunction\":\"Nej\""));
    }
    @Test
    void testInternalIds() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_74e22fd8);

        Assertions.assertTrue(transformedJSON.contains("\"kb:subgenre_id\":\"708\"," +
                                                        "\"kb:episode_id\":\"0\"," +
                                                        "\"kb:season_id\":\"0\"," +
                                                        "\"kb:series_id\":\"0\""));

        Assertions.assertTrue(transformedJSON.contains("\"kb:maingenre_id\":\"1\"," +
                                                        "\"kb:channel_id\":3," +
                                                        "\"kb:country_of_origin_id\":\"0\"," +
                                                        "\"kb:ritzau_program_id\":\"25101114\"" ));

        Assertions.assertTrue(transformedJSON.contains("\"kb:format_identifier_ritzau\":\"81213310\"," +
                "\"kb:format_identifier_nielsen\":\"101|20220526|140000|180958|0|9629d8b8-b751-450f-bfd7-d2510910bb34|69\"," ));
    }

    @Test
    void testNoEmptyInternalMap() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_4f706cda);
        Assertions.assertFalse(transformedJSON.contains("\"kb:internal\":{}"));

    }






    private static void printSchemaOrgJson(String xml) throws IOException {
        Map<String, String> injections = Map.of("imageserver", "https://example.com/imageserver/",
                                                "conditionsOfAccess", "placeholderCondition");
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, xml, injections);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement je = JsonParser.parseString(transformedJSON);
        String transformedPrettyJSON = gson.toJson(je);

        System.out.println(transformedPrettyJSON);
    }
}
