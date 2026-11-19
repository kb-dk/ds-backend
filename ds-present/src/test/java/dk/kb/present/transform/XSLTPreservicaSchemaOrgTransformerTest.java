package dk.kb.present.transform;

import dk.kb.present.TestFiles;
import dk.kb.present.TestUtil;
import dk.kb.util.Resolver;
import java.util.HashMap;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dk.kb.present.TestUtil.prettyPrintJson;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.Map;

public class XSLTPreservicaSchemaOrgTransformerTest extends XSLTTransformerTestBase {
    private static final Logger log = LoggerFactory.getLogger(XSLTCumulusToSchemaDotOrgTransformerTest.class);
    public static final String PRESERVICA2SCHEMAORG = "xslt/preservica2schemaorg.xsl";

    @Override
    String getXSLT() {
        return PRESERVICA2SCHEMAORG;
    }

    // Only integration tests need the internal test files; the untagged tests use tracked records.
    @BeforeEach
    void requireInternalTestFiles(TestInfo testInfo) {
        if (testInfo.getTags().contains("integration")
                && Resolver.getPathFromClasspath("internal_test_files/preservica7") == null) {
            fail("Internal test files are not present. Integration tests in 'XSLTPreservicaSchemaOrgTransformerTest' cannot run.");
        }
    }

    @Tag("integration")
    @Test
    void testCollection() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_a8aafb121);
        Assertions.assertTrue(transformedJSON.contains("\"isPartOf\":[" +
                                                        "{\"@type\":\"Collection\"," +
                                                        "\"name\":\"Det Kgl. Bibliotek; Radio\\/TV-Samlingen\"}"));
    }

    /*@Test
    void testUrlPreservica7() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_WITH_TRANSCODINGSTATUS, null);
        Assertions.assertTrue(transformedJSON.contains("\"kb:file_id\":\"\\/radio-tv\\/2\\/e\\/e\\/6\\/2ee62889-a4d0-43c4-bfe5-4d7e3dcca7c8.mp3\""));
    }*/

    @Tag("integration")
    @Test
    void testUrlDomsMig() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_DOMS_MIG_WITH_TRANSCODINGSTATUS, Map.of("referenceId", "08909897-cf37-4bd9-a230-1b48c87cea18"));

        assertTrue(transformedJSON.contains("\"kb:file_id\":\"08909897-cf37-4bd9-a230-1b48c87cea18\""));
        assertTrue(transformedJSON.contains("\"kb:file_path\":\"0\\/8\\/9\\/0\\/08909897-cf37-4bd9-a230-1b48c87cea18.mp4\""));
        assertTrue(transformedJSON.contains("\"kb:file_extension\":\"mp4\""));
    }

    @Tag("integration")
    @Test
    void testUrlPreservicaRecord() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_WITH_CORRECT_PRESENTATION, Map.of("referenceId", "c8d2e73c-0943-4b0d-ab1f-186ef10d8eb4"));

        assertTrue(transformedJSON.contains("\"kb:file_id\":\"c8d2e73c-0943-4b0d-ab1f-186ef10d8eb4\""));
        assertTrue(transformedJSON.contains("\"kb:file_path\":\"c8\\/d2\\/e7\\/c8d2e73c-0943-4b0d-ab1f-186ef10d8eb4\""));
        assertTrue(transformedJSON.contains("\"kb:file_extension\":\"mp4\""));
    }

    @Tag("integration")
    @Test
    void testUrlPreservicaRecordMultipleAccessFiles() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_WITH_CORRECT_PRESENTATION_MULTIPLE_FILES, Map.of("referenceId", "b557f9dd-197c-47f6-b481-785d5f7accd2"));
        assertTrue(transformedJSON.contains("\"kb:file_id\":\"b557f9dd-197c-47f6-b481-785d5f7accd2\""));
        assertTrue(transformedJSON.contains("\"kb:file_path\":\"b5\\/57\\/b557f9dd-197c-47f6-b481-785d5f7accd2\""));
        assertTrue(transformedJSON.contains("\"kb:file_extension\":\"mp3\""));
    }

    @Tag("integration")
    @Test
    void getTransformedWithAccessFieldsAdded_whenRerunClusterExists_thenRerunClusterFieldsIsPopulated()
        throws IOException {
        // Arrange
        UUID rerunClusterId = UUID.randomUUID();
        Integer rerunClusterIdCount = 2;
        Map<String, String> map = new HashMap<>();
        map.put("rerun_cluster_id", rerunClusterId.toString());
        map.put("rerun_cluster_id_count", rerunClusterIdCount.toString());

        // Act
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG,
            TestFiles.PVICA_RECORD_e683b0b8, map);

        // Assert
        assertTrue(transformedJSON.contains("\"kb:rerun_cluster_id\":\"" + rerunClusterId + "\""));
        assertTrue(
            transformedJSON.contains("\"kb:rerun_cluster_id_count\":" + rerunClusterIdCount));
    }

    @Tag("integration")
    @Test
    void getTransformedWithAccessFieldsAdded_whenRerunClusterDoesNotExists_thenRerunClusterFieldsDoesNotExits()
        throws IOException {
        // Arrange
        Map<String, String> map = new HashMap<>();

        // Act
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG,
            TestFiles.PVICA_RECORD_e683b0b8, map);

        // Assert
        assertFalse(transformedJSON.contains("\"kb:rerun_cluster_id\":"));
        assertFalse(transformedJSON.contains("\"kb:rerun_cluster_id_count\":"));
    }

    @Tag("integration")
    @Test
    void getTransformedWithAccessFieldsAdded_whenTranscriptionExists_thenTranscriptionFieldIsPopulatedAndHasTranscriptionIsTrue() throws IOException {
        // Arrange
        Map<String, String> map = new HashMap<>();
        map.put("transcription", "Dette er en transcription");
        map.put("has_transcription", "true");

        // Act
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_e683b0b8, map);

        // Assert
        assertTrue(transformedJSON.contains("\"kb:transcription\":\"Dette er en transcription\""));
        assertTrue(transformedJSON.contains("\"kb:has_transcription\":\"true\""));
    }

    @Tag("integration")
    @Test
    void getTransformedWithAccessFieldsAdded_whenTranscriptionDoesNotExists_thenTranscriptionFieldDoesNotExistAndHasTranscriptionIsFalse() throws IOException {
        // Arrange
        Map<String, String> map = new HashMap<>();
        map.put("has_transcription", "false");

        // Act
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_e683b0b8, map);

        // Assert
        assertFalse(transformedJSON.contains("\"kb:transcription\":"));
        assertTrue(transformedJSON.contains("\"kb:has_transcription\":\"false\""));
    }

    @Tag("integration")
    @Test
    void testName() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_4b18d02d);
        Assertions.assertTrue(transformedJSON.contains("\"name\":\"Ibens\""));
    }

    @Tag("integration")
    @Test
    void testBroadcastDisplayName() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_e683b0b8);
        Assertions.assertTrue(transformedJSON.contains("\"broadcastDisplayName\":\"DR P1\",\"alternateName\":\"DR P1\""));
    }

    @Tag("integration")
    @Test
    void testBroadcasterOrganization() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_e683b0b8);
        Assertions.assertTrue(transformedJSON.contains("\"legalName\":\"DR\""));
    }

    @Tag("integration")
    @Test
    void testStartAndEndDates() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_e683b0b8);
        Assertions.assertTrue(transformedJSON.contains("\"startTime\":\"1987-05-04T14:45:00Z\"") &&
                                       transformedJSON.contains("\"endTime\":\"1987-05-04T16:45:00Z\""));
    }

    @Tag("integration")
    @Test
    void testDuration() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_e683b0b8);
        // TODO: Fix the time format delivered by the XSLT even though this format is compliant with schema.org as it is part of ISO 8601.
        Assertions.assertTrue(transformedJSON.contains("\"duration\":\"PT2H\""));
    }

    @Tag("integration")
    @Test
    void testIdentifiers() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_e683b0b8);

        Assertions.assertTrue(transformedJSON.contains("\"identifier\":[{\"@type\":\"PropertyValue\",\"PropertyID\":\"Origin\",\"value\":\"ds.test\"}")
                && transformedJSON.contains("{\"@type\":\"PropertyValue\",\"PropertyID\":\"ritzauId\",\"value\":\"926e730f-b3e6-44b9-aea7-a6ea27ec98ae\"}")
                && transformedJSON.contains("{\"@type\":\"PropertyValue\",\"PropertyID\":\"RecordID\",\"value\":\"ds.test:e683b0b8-425b-45aa-be86-78ac2b4ef0ca.xml\"}")
                && transformedJSON.contains("{\"@type\":\"PropertyValue\",\"PropertyID\":\"PID\",\"value\":\"109.1.4\\/e683b0b8-425b-45aa-be86-78ac2b4ef0ca\"}"));
    }

    @Tag("integration")
    @Test
    void noAlternateName() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_c295ae6c);
        Assertions.assertFalse(transformedJSON.contains("\"name\":\"Kunstnere i Kolding\",\"alternateName"));
    }

    @Tag("integration")
    @Test
    void testLiveStatus() throws IOException {
        String isLive = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_74e22fd8);
        Assertions.assertTrue(isLive.contains("\"isLiveBroadcast\":true"));

        String notLive = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_3006e2f8);
        Assertions.assertTrue(notLive.contains("\"isLiveBroadcast\":false"));
    }

    @Tag("integration")
    @Test
    void testEpisodeNumberAndSeriesLength() throws IOException{
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_a8afb121);
        Assertions.assertTrue(transformedJSON.contains("\"episodeNumber\":4"));
        Assertions.assertTrue(transformedJSON.contains("\"numberOfEpisodes\":6"));
    }

    @Tag("integration")
    @Test
    void testEpisodeNumberNoSeries() throws IOException{
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_b346acc8);
        Assertions.assertTrue(transformedJSON.contains("\"episodeNumber\":433"));
        Assertions.assertFalse(transformedJSON.contains("numberOfEpisodes"));
    }

    @Tag("integration")
    @Test
    void testNoEpisodeInfo() throws  IOException{
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_74e22fd8);
        Assertions.assertFalse(transformedJSON.contains("episodeNumber"));
        Assertions.assertFalse(transformedJSON.contains("numberOfEpisodes"));
    }

    @Tag("integration")
    @Test
    void testEpisodeName() throws IOException{
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_3945e2d1);
        Assertions.assertTrue(transformedJSON.contains("\"encodesCreativeWork\":{" +
                                                        "\"@type\":\"TVEpisode\"," +
                                                        "\"name\":\"Kagerester\","));
    }

    @Tag("integration")
    @Test
    void testNoEpisodeName() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_b346acc8);
        Assertions.assertTrue(transformedJSON.contains("\"encodesCreativeWork\":{" +
                                                        "\"@type\":\"TVEpisode\"," +
                                                        "\"episodeNumber\":433"));
    }

    @Tag("integration")
    @Test
    void testEmptyEpisodeName() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_9d9785a8);
        Assertions.assertFalse(transformedJSON.contains("\"encodesCreativeWork\":{" +
                "\"@type\":\"TVEpisode\",\"name\":\"\""));
    }

    @Tag("integration")
    @Test
    void testTypeExtraction() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_74e22fd8);
        Assertions.assertTrue(transformedJSON.startsWith("{\"@context\":\"http:\\/\\/schema.org\\/\",\"@type\":\"VideoObject\""));
    }

    @Tag("integration")
    @Test
    void testKeywords() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_a8afb121);
        Assertions.assertTrue(transformedJSON.contains("\"keywords\":\"Serier, Krimiserie\","));
    }

    @Tag("integration")
    @Test
    void testNoKeywords() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_4f706cda);
        Assertions.assertFalse(transformedJSON.contains("\"keywords\":"));
    }

    @Tag("integration")
    @Test
    void testNoNullKeywords() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_3006e2f8);
        Assertions.assertFalse(transformedJSON.contains("null"));
    }

    @Tag("integration")
    @Test
    void testAbstractCreation() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_a8afb121);
        Assertions.assertTrue(transformedJSON.contains("\"abstract\":\"Eng. krimiserie\""));
    }

    @Tag("integration")
    @Test
    void testNoAbstract() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_c6fde2f4);
        Assertions.assertFalse(transformedJSON.contains("\"abstract\":"));
    }

    @Tag("integration")
    @Test
    void testVideoQuality() throws IOException {
        String ikkeHD = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_a8afb121);
        Assertions.assertTrue(ikkeHD.contains("\"videoQuality\":\"ikke hd\""));

        // TODO: Find a record not specified
        /*String notSpeficied = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_3006e2f8);
        Assertions.assertFalse(notSpeficied.contains("\"videoQuality\":"));*/
    }

    @Tag("integration")
    @Test
    void testDatePublished() throws IOException {
        String notPremiere = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_a8afb121);
        Assertions.assertFalse(notPremiere.contains("\"datePublished\":"));
        // TODO: Add test for datePublished. where the value is present, either by creating a test record with premiere:premiere or by finding a record with that value
    }

    @Tag("integration")
    @Test
    void testProgramStructure() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_0b3f6a54);
        Assertions.assertTrue(transformedJSON.contains("\"kb:program_structure_missing_seconds_start\":\"0\"")
                                    && transformedJSON.contains("\"kb:program_structure_missing_seconds_end\":\"0\""));
        //TODO: add tests for fields 'holes' and 'overlaps' with a constructed test file.
    }

    @Tag("integration")
    @Test
    void testGenre() throws IOException {
        String hasGenre = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_3945e2d1);
        Assertions.assertTrue(hasGenre.contains("\"genre\":\"Humor, quiz og underholdning\""));

        String noGenre = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_4f706cda);
        Assertions.assertFalse(noGenre.contains("\"genre\":"));

        String emptyGenre = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_4f706cda);
        Assertions.assertFalse(emptyGenre.contains("\"genre\":"));
    }

    @Tag("integration")
    @Test
    void newGenreTest() throws IOException{
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, "internal_test_files/domsMigrated/19fe6686-42a5-41f9-80d8-bffb872f942a.xml");
        prettyPrintJson(transformedJSON);
    }

    @Tag("integration")
    @Test
    void noVideoQualityForRadioRecords() throws IOException {
        String radio = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_e683b0b8);
        Assertions.assertFalse(radio.contains("\"videoQuality\":"));
    }

    @Tag("integration")
    @Test
    void whiteProgramID() throws IOException {
        String radio = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_c295ae6c);
        Assertions.assertTrue(radio.contains("\"@type\":\"PropertyValue\"," +
                "\"PropertyID\":\"WhiteProgramID\"," +
                "\"value\":\"A-1966-03-20-P-0197_059\""));
    }

    @Tag("integration")
    @Test
    void noShowViewcodeForRadio() throws IOException {
        String radio = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_c295ae6c);
        Assertions.assertFalse(radio.contains("kb:showviewcode"));
    }

    @Tag("integration")
    @Test
    void noAspectRatioForRadio() throws IOException {
        String radio = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_e683b0b8);
        Assertions.assertFalse(radio.contains("\"kb:aspect_ratio\":"));
    }

    @Tag("integration")
    @Test
    void noColorForRadio() throws IOException {
        String radio = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_e683b0b8);
        Assertions.assertFalse(radio.contains("\"kb:color\":"));
    }

    @Tag("integration")
    @Test
    void noTeletextForRadio() throws IOException {
        String radio = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_e683b0b8);
        Assertions.assertFalse(radio.contains("\"kb:is_teletext\":"));
    }

    @Tag("integration")
    @Test
    void noSubtitlesForRadio() throws IOException {
        String radio = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_e683b0b8);
        Assertions.assertFalse(radio.contains("\"kb:has_subtitles\":"));
        Assertions.assertFalse(radio.contains("\"kb:has_subtitles_for_hearing_impaired\":"));
    }

    @Tag("integration")
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

    @Tag("integration")
    @Test
    void testInternalGenreSub() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_74e22fd8);
        Assertions.assertTrue(transformedJSON.contains("\"kb:genre_sub\":\"Alle\""));
    }

    @Tag("integration")
    @Test
    void testAspectRatio() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_74e22fd8);

        Assertions.assertTrue(transformedJSON.contains("\"videoFrameSize\":\"16:9\""));
    }

    @Tag("integration")
    @Test
    void testInternalSubtitlesAndTeletext() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_74e22fd8);

        Assertions.assertTrue(transformedJSON.contains("\"kb:has_subtitles\":false," +
                                                        "\"kb:has_subtitles_for_hearing_impaired\":false," +
                                                        "\"kb:is_teletext\":false"));
    }
    @Tag("integration")
    @Test
    void testInternalAcces() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_74e22fd8);

        Assertions.assertTrue(transformedJSON.contains("\"kb:access_individual_prohibition\":\"Nej\"," +
                                                        "\"kb:access_claused\":\"Nej\"," +
                                                        "\"kb:access_malfunction\":\"Nej\""));
    }

    @Test
    void transform_whenIndividueltForbudIsJa_thenAccessIndividualProhibitionIsJa() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_INDIVIDUAL_PROHIBITION_JA);
        assertTrue(transformedJSON.contains("\"kb:access_individual_prohibition\":\"Ja\""), transformedJSON);
    }

    @Test
    void transform_whenIndividueltForbudIsNej_thenAccessIndividualProhibitionIsNej() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_INDIVIDUAL_PROHIBITION_NEJ);
        assertTrue(transformedJSON.contains("\"kb:access_individual_prohibition\":\"Nej\""), transformedJSON);
    }

    @Test
    void transform_whenIndividueltForbudIsMissing_thenAccessIndividualProhibitionIsAbsent() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_INDIVIDUAL_PROHIBITION_MISSING);
        assertFalse(transformedJSON.contains("kb:access_individual_prohibition"), transformedJSON);
    }

    @Tag("integration")
    @Test
    void testInternalIds() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_74e22fd8);

        Assertions.assertTrue(transformedJSON.contains("\"kb:subgenre_id\":\"708\"," +
                                                        "\"kb:episode_id\":\"0\"," +
                                                        "\"kb:season_id\":\"0\"," +
                                                        "\"kb:series_id\":\"0\""));

        Assertions.assertTrue(transformedJSON.contains("\"kb:maingenre_id\":\"1\"," +
                                                        "\"kb:ritzau_channel_id\":3," +
                                                        "\"kb:ritzau_program_id\":\"25101114\"" ));

        Assertions.assertTrue(transformedJSON.contains("\"kb:format_identifier_ritzau\":\"81213310\"," +
                "\"kb:format_identifier_nielsen\":\"101|20220526|140000|180958|0|9629d8b8-b751-450f-bfd7-d2510910bb34|69\"," ));
    }

    @Tag("integration")
    @Test
    void schemaOrgTransformation_whenSeasonNumberRitzauExist_thenSeasonNumberRitzauExistInSchemaOrgData() throws IOException {
        Assertions.assertTrue(TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_0e89456b)
                .contains("\"kb:season_number_ritzau\":\"7\""));
        Assertions.assertTrue(TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_e8c664f9)
                .contains("\"kb:season_number_ritzau\":\"1\""));
    }

    @Tag("integration")
    @Test
    void schemaOrgTransformation_whenSeasonNumberRitzauIsZero_thenSeasonNumberRitzauDoNotExistInSchemaOrgData() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_WITH_CORRECT_PRESENTATION);

        Assertions.assertFalse(transformedJSON.contains("season_number_ritzau"));
    }

    @Tag("integration")
    @Test
    void schemaOrgTransformation_whenSeasonNumberRitzauDoNotExists_thenSeasonNumberRitzauDoNotExistInSchemaOrgData() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_3945e2d1);

        Assertions.assertFalse(transformedJSON.contains("season_number_ritzau"));
    }

    @Tag("integration")
    @Test
    void testCountryOfOrigin() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_74e22fd8);

        assertTrue(transformedJSON.contains("\"countryOfOrigin\":{" +
                "\"@type\":\"Country\"," +
                "\"identifier\":\"0\"}"));
    }

    @Tag("integration")
    @Test
    void testNoEmptyInternalMap() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_4f706cda);
        Assertions.assertFalse(transformedJSON.contains("\"kb:internal\":{}"));
    }

    @Tag("integration")
    @Test
    void testDateInjection() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_e683b0b8);
        Assertions.assertTrue(transformedJSON.contains("\"kb:storage_mTime\":"));
    }

    @Tag("integration")
    @Test
    public void testTransformationOfDomsRecord() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_DOMS_MIG_bd612d1e);
        assertTrue(transformedJSON.contains("\"@type\":\"VideoObject\""));
    }

    @Tag("integration")
    @Test
    public void testDomsBroadcastAlternateName() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_DOMS_MIG_bd612d1e);
        assertFalse(transformedJSON.contains("\"broadcastDisplayName\":\"Kanal 4\",\"alternateName\":\"\""));
    }

    @Tag("integration")
    @Test
    public void testDomsNoAccessionRef() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_DOMS_MIG_bd612d1e);
        assertFalse(transformedJSON.contains("\"PropertyID\":\"InternalAccessionRef\",\"value\":\"\""));
    }

    @Tag("integration")
    @Test
    public void testDomsEpisodeNumbers() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_DOMS_MIG_eaea0362);
        assertTrue(transformedJSON.contains("\"episodeNumber\":8,") && transformedJSON.contains("\"numberOfEpisodes\":24"));
    }

    @Tag("integration")
    @Test
    public void testNotANumberPlusSign() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_HOMEMADE_INVALID_NUMBERS_PLUSSIGN);

        assertFalse(transformedJSON.contains("\"kb:transformation_error_description\":\"err:FOJS0006: xml-to-json: Invalid number: 2+3 \""));
    }

    @Tag("integration")
    @Test
    public void testNoPresentationCopyDoms() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_DOMS_MIG_1ab7e0fc);
        assertTrue(transformedJSON.contains("\"kb:has_doms_access_copy\":\"false\""));
    }
    @Tag("integration")
    @Test
    public void testPresentationCopyDoms() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_DOMS_MIG_17f56f97);
        prettyPrintJson(transformedJSON);
        assertTrue(transformedJSON.contains("\"kb:has_doms_access_copy\":\"true\""));
    }

    //@Test
    void testErrorCatching() throws IOException {
        // This does not produce an error anymore, however I would like to produce an error to test the error handling.
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_HOMEMADE_INVALID_NUMBERS_PLUSSIGN);

        Assertions.assertTrue(transformedJSON.contains("\"kb:transformation_error_description\":\"err:FOJS0006: xml-to-json: Invalid number: 2+3 \""));
    }

    @Tag("integration")
    @Test
    public void testHoldbackFields() throws IOException {
        Map<String, String> holdbackInjections = Map.of("holdbackDate", "2026-01-17T09:34:42Z",
                                                        "holdbackPurposeName","Aktualitet og debat");
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_DOMS_MIG_eaea0362, holdbackInjections);

        assertTrue(transformedJSON.contains("\"kb:holdback_date\":\"2026-01-17T09:34:42Z\""));
        assertTrue(transformedJSON.contains("\"kb:holdback_name\":\"Aktualitet og debat\""));
    }

    /**
     * This tests that when multiple main genres are present the conversion to predefined values still occurs correctly.
     *
     * @throws IOException
     */
    @Tag("integration")
    @Test
    public void testMultipleMaingenres() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_DOMS_MIG_82514cd9);
        assertTrue(transformedJSON.contains("\"genre\":\"Film og serier\""));
    }

    @Tag("integration")
    @Test
    public void testDomsRecordsNotConvertingToMediaObject() throws IOException {
        String transformedTV = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_DOMS_MIG_dd5f2f60);
        assertTrue(transformedTV.contains("\"@type\":\"VideoObject\","));

        String transformedRadio = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_DOMS_MIG_597e79f7);
        assertTrue(transformedRadio.contains("\"@type\":\"AudioObject\","));
    }

    @Tag("integration")
    @Test
    void testDateTimeNoT() throws IOException {
        String transformed = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_DOMS_MIG_4ad48e98);
        assertTrue(transformed.contains("\"startTime\":\"1987-05-04T14:45:00Z\""));
    }

    @Tag("integration")
    @Test
    void testTimezone() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_DOMS_MIG_cb4bb835);
        assertTrue(transformedJSON.contains("\"startTime\":\"1987-05-04T14:45:00Z\""));
    }

    @Tag("integration")
    @Test
    public void testCountryOfOriginNoName() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_DOMS_MIG_e2dfb840);
        assertFalse(transformedJSON.contains("\"countryOfOrigin\":{\"@type\":\"Country\",\"name\""));
        assertTrue(transformedJSON.contains("\"countryOfOrigin\":{" +
                "\"@type\":\"Country\"," +
                "\"identifier\":\"0\"}"));
    }

    @Tag("integration")
    @Test
    public void testFormatAspectRatio() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_DOMS_MIG_e2dfb840);
        assertFalse(transformedJSON.contains("\"videoFrameSize\""));
    }

    @Tag("integration")
    @Test
    public void testOriginatesFromDOMS() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_DOMS_MIG_e2dfb840);
        assertTrue(transformedJSON.contains("kb:originates_from\":\"DOMS\""));
    }

    @Tag("integration")
    @Test
    public void testColonInCategory() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_07fc1c7c);
        prettyPrintJson(transformedJSON);
        assertTrue(transformedJSON.contains("\"genre\":\"Film og serier\","));
    }

    @Tag("integration")
    @Test
    public void testDomsTitleCleaning() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_DOMS_MIG_73aad1c3);
        assertTrue(transformedJSON.contains("\"name\":\"Temalørdag: Pavarotti\","));
    }

    @Tag("integration")
    @Test
    public void testActors() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_DOMS_MIG_054c55b3);
        assertTrue(transformedJSON.contains("\"actor\":" +
                                            "[{\"@type\":\"PerformanceRole\",\"actor\":{\"@type\":\"Person\",\"name\":\"Elizabeth McGovern\"},\"characterName\":\"Deborah\"}," +
                                            "{\"@type\":\"PerformanceRole\",\"actor\":{\"@type\":\"Person\",\"name\":\"James Woods\"},\"characterName\":\"Max\"}," +
                                            "{\"@type\":\"PerformanceRole\",\"actor\":{\"@type\":\"Person\",\"name\":\"Robert De Niro\"},\"characterName\":\"Noodles\"}]"));
    }

    @Tag("integration")
    @Test
    public void testNoActors() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_DOMS_MIG_597e79f7);
        assertFalse(transformedJSON.contains("\"actor\""));
    }

    @Tag("integration")
    @Test
    public void testDirectors() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_DOMS_MIG_054c55b3);
        assertTrue(transformedJSON.contains("\"director\":[{\"@type\":\"Person\",\"name\":\"Sergio Leone\"}]"));
    }

    @Tag("integration")
    @Test
    public void testNoDirectors() throws IOException{
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_DOMS_MIG_597e79f7);
        assertFalse(transformedJSON.contains("\"director\""));
    }

    @Tag("integration")
    @Test
    public void testCreators() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_DOMS_MIG_054c55b3);
        assertTrue(transformedJSON.contains("\"creator\":[{\"@type\":\"Person\",\"name\":\"Franco Ferrini og Sergio Leon\"},{\"@type\":\"Person\",\"name\":\"Franco Arcalli\"}," +
                                            "{\"@type\":\"Person\",\"name\":\"Enrico Medioli\"},{\"@type\":\"Person\",\"name\":\"Piero De Bernardi\"}," +
                                            "{\"@type\":\"Person\",\"name\":\"Leonardo Benvenuti\"}]"));
    }

    @Tag("integration")
    @Test
    public void testNoCreators() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_DOMS_MIG_597e79f7);
        assertFalse(transformedJSON.contains("\"creator\""));
    }

    @Tag("integration")
    @Test
    public void voidContributorsTest() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_HOMEMADE_RADIO_WITH_CONTRIBUTORS);
        assertTrue(transformedJSON.contains("\"contributor\""));
    }

    @Tag("integration")
    @Test
    public void channelCleanupTest() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_DOMS_MIG_1ab7e0fc);
        assertTrue(transformedJSON.contains("\"alternateName\":\"DR OLINE\""));
        assertTrue(transformedJSON.contains("\"broadcastDisplayName\":\"DR Oline\","));
    }

    @Tag("integration")
    @Test
    public void testAlternateNameForDR1AndDR2() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_DOMS_MIG_172c987b);
        prettyPrintJson(transformedJSON);
        assertTrue(transformedJSON.contains("\"alternateName\":\"DR 1\""));
    }

    @Tag("integration")
    @Test
    public void invalidNumbersTest() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_HOMEMADE_INVALID_NUMBERS);
        assertFalse(transformedJSON.contains("\"episodeNumber\""));
        assertFalse(transformedJSON.contains("\"numberOfEpisodes\""));
        assertFalse(transformedJSON.contains("\"kb:channel_id\""));
        assertFalse(transformedJSON.contains("\"overlap_length\""));
        assertFalse(transformedJSON.contains("\"kb:production_code_allowed\""));
        prettyPrintJson(transformedJSON);
    }

    @Tag("integration")
    @Test
    public void noGenreTest() throws IOException {
        String transformedJson = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_03f18f50);
        assertTrue(transformedJson.contains("\"genre\":\"Radio-rodekasse\""));
    }

    @Tag("integration")
    @Test
    public void testDoubleChannelIds() throws IOException {
        String transformedJson = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_DOUBLE_CHANNEL);
        prettyPrintJson(transformedJson);
        assertTrue(transformedJson.contains("\"kb:ritzau_channel_id\":325"));
        assertTrue(transformedJson.contains("\"kb:nielsen_channel_id\":103"));
    }

    @Tag("integration")
    @Test
    public void getTransformedWithAccessFieldsAdded_whenContainsDsTvDrArchiveSupplementaryRightsMetadata_thenContainsDrArchiveSupplementaryRightsMetadataIsTrue() throws IOException {
        // Act
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA7_HOMEMADE_DR_ARCHIVE_SUPPLEMENTARY_RIGHTS_METADATA_DS_TV_TID_TV_METER);
        log.info(transformedJSON);

        //Assert
        assertTrue(transformedJSON.contains("\"PropertyID\":\"Origin\",\"value\":\"ds.test\""));
        assertTrue(transformedJSON.contains("\"kb:contains_dr_archive_supplementary_rights_metadata\":true"));
        assertTrue(transformedJSON.contains("\"kb:contains_tvmeter\":true"));
        assertTrue(transformedJSON.contains("\"kb:contains_nielsen\":false"));
        assertTrue(transformedJSON.contains("\"kb:contains_ritzau\":true"));
    }

    @Tag("integration")
    @Test
    public void getTransformedWithAccessFieldsAdded_whenContainsDsRadioDrArchiveSupplementaryRightsMetadata_thenContainsDrArchiveSupplementaryRightsMetadataIsTrue() throws IOException {
        // Act
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA7_DR_ARCHIVE_SUPPLEMENTARY_RIGHTS_METADATA_DS_RADIO_83191087);
        log.info(transformedJSON);

        // Assert
        assertTrue(transformedJSON.contains("\"PropertyID\":\"Origin\",\"value\":\"ds.test\""));
        assertTrue(transformedJSON.contains("\"kb:contains_dr_archive_supplementary_rights_metadata\":true"));
        assertTrue(transformedJSON.contains("\"kb:contains_tvmeter\":false"));
        assertTrue(transformedJSON.contains("\"kb:contains_nielsen\":false"));
        assertTrue(transformedJSON.contains("\"kb:contains_ritzau\":false"));
    }

    @Tag("integration")
    @Test
    public void transformationWithAnnotation() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_WITH_ANNOTATION);
        log.info(transformedJSON);

        //Assert
        assertTrue(transformedJSON.contains("\"annotation\":\"Dette indhold blev sendt i dansk radio under den tyske besættelse af Danmark, hvor Danmarks Radio ikke havde redaktionel frihed. Oprindeligt udsendelses tidspunkt er ukendt, det angivne tidspunkt kan derfor være ukorrekt.\""));
    }
}
