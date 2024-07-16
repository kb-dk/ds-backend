package dk.kb.present.transform;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dk.kb.present.TestFiles;
import dk.kb.present.TestUtil;
import dk.kb.util.Resolver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dk.kb.present.TestUtil.prettyPrintJson;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.Map;


@Tag("integration")
public class XSLTPreservicaSchemaOrgTransformerTest extends XSLTTransformerTestBase {
    private static final Logger log = LoggerFactory.getLogger(XSLTCumulusToSchemaDotOrgTransformerTest.class);
    public static final String PRESERVICA2SCHEMAORG = "xslt/preservica2schemaorg.xsl";

    @Override
    String getXSLT() {
        return PRESERVICA2SCHEMAORG;
    }

    @BeforeAll
    public static void beforeMethod() {
        if (Resolver.getPathFromClasspath("internal_test_files/preservica7") == null){
            fail("Internal test files are not present. Unittest 'XSLTPreservicaSchemaOrgTransformerTest'");
        }
        
    }

    @Test
    void testCollection() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_a8aafb121);
        Assertions.assertTrue(transformedJSON.contains("\"isPartOf\":[" +
                                                        "{\"@type\":\"Collection\"," +
                                                        "\"name\":\"Det Kgl. Bibliotek; Radio\\/TV-Samlingen\"}"));
    }

    @Test
    void testConditionOfAccess() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithVideoChildAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_3945e2d1, null);
        Assertions.assertTrue(transformedJSON.contains("\"conditionsOfAccess\":\"placeholderCondition\""));
    }

    @Test
    void testUrlPreservica7() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithVideoChildAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_b346acc8, null);
        Assertions.assertTrue(transformedJSON.contains("\"kb:file_id\":\"8946d31d-a81c-447f-b84d-ff80644353d2.mp4\""));
    }

    @Test
    void testName() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_4b18d02d);
        Assertions.assertTrue(transformedJSON.contains("\"name\":\"Ibens\""));
    }

    @Test
    void testBroadcastDisplayName() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_e683b0b8);
        Assertions.assertTrue(transformedJSON.contains("\"broadcastDisplayName\":\"DR P1\",\"alternateName\":\"drp1\""));
    }

    @Test
    void testBroadcasterOrganization() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_e683b0b8);
        Assertions.assertTrue(transformedJSON.contains("\"legalName\":\"DR\""));
    }

    @Test
    void testStartAndEndDates() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_e683b0b8);
        Assertions.assertTrue(transformedJSON.contains("\"startTime\":\"2022-05-29T16:48:00Z\"") &&
                                       transformedJSON.contains("\"endTime\":\"2022-05-29T17:03:00Z\""));
    }

    @Test
    void testDuration() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_e683b0b8);
        // TODO: Fix the time format delivered by the XSLT even though this format is compliant with schema.org as it is part of ISO 8601.
        Assertions.assertTrue(transformedJSON.contains("\"duration\":\"PT15M\""));
    }

    @Test
    void testIdentifiers() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_e683b0b8);

        Assertions.assertTrue(transformedJSON.contains("\"identifier\":[{\"@type\":\"PropertyValue\",\"PropertyID\":\"Origin\",\"value\":\"ds.test\"}")
                && transformedJSON.contains("{\"@type\":\"PropertyValue\",\"PropertyID\":\"ritzauId\",\"value\":\"926e730f-b3e6-44b9-aea7-a6ea27ec98ae\"}")
                && transformedJSON.contains("{\"@type\":\"PropertyValue\",\"PropertyID\":\"RecordID\",\"value\":\"ds.test:e683b0b8-425b-45aa-be86-78ac2b4ef0ca.xml\"}")
                && transformedJSON.contains("{\"@type\":\"PropertyValue\",\"PropertyID\":\"PID\",\"value\":\"109.1.4\\/e683b0b8-425b-45aa-be86-78ac2b4ef0ca\"}"));
    }


    @Test
    void noAlternateName() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_c295ae6c);
        Assertions.assertFalse(transformedJSON.contains("\"name\":\"Kunstnere i Kolding\",\"alternateName"));
    }

    @Test
    void testLiveStatus() throws IOException {
        String isLive = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_74e22fd8);
        Assertions.assertTrue(isLive.contains("\"isLiveBroadcast\":true"));

        String notLive = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_3006e2f8);
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
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_b346acc8);
        Assertions.assertTrue(transformedJSON.contains("\"episodeNumber\":433"));
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
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_b346acc8);
        Assertions.assertTrue(transformedJSON.contains("\"encodesCreativeWork\":{" +
                                                        "\"@type\":\"TVEpisode\"," +
                                                        "\"episodeNumber\":433"));
    }

    @Test
    void testEmptyEpisodeName() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_9d9785a8);
        prettyPrintJson(transformedJSON);
        Assertions.assertFalse(transformedJSON.contains("\"encodesCreativeWork\":{" +
                "\"@type\":\"TVEpisode\",\"name\":\"\""));
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
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_4f706cda);
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
        prettyPrintJson(transformedJSON);
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

        // TODO: Find a record not specified
        /*String notSpeficied = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_3006e2f8);
        Assertions.assertFalse(notSpeficied.contains("\"videoQuality\":"));*/
    }

    @Test
    void testDatePublished() throws IOException {
        String notPremiere = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_a8afb121);
        Assertions.assertFalse(notPremiere.contains("\"datePublished\":"));
        // TODO: Add test for datePublished. where the value is present, either by creating a test record with premiere:premiere or by finding a record with that value

    }

    @Test
    void testProgramStructure() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_0b3f6a54);
        Assertions.assertTrue(transformedJSON.contains("\"kb:program_structure_missing_seconds_start\":\"0\"")
                                    && transformedJSON.contains("\"kb:program_structure_missing_seconds_end\":\"0\""));
        //TODO: add tests for fields 'holes' and 'overlaps' with a constructed test file.
    }

    @Test
    void testGenre() throws IOException {
        String hasGenre = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_3945e2d1);
        Assertions.assertTrue(hasGenre.contains("\"genre\":\"Underholdning\""));

        String noGenre = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_4f706cda);
        Assertions.assertFalse(noGenre.contains("\"genre\":"));

        String emptyGenre = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_4f706cda);
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
    void testAspectRatio() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_74e22fd8);

        Assertions.assertTrue(transformedJSON.contains("\"videoFrameSize\":\"16:9\""));
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
                                                        "\"kb:ritzau_program_id\":\"25101114\"" ));

        Assertions.assertTrue(transformedJSON.contains("\"kb:format_identifier_ritzau\":\"81213310\"," +
                "\"kb:format_identifier_nielsen\":\"101|20220526|140000|180958|0|9629d8b8-b751-450f-bfd7-d2510910bb34|69\"," ));
    }

    @Test
    void testCountryOfOrigin() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_74e22fd8);

        assertTrue(transformedJSON.contains("\"countryOfOrigin\":{" +
                "\"@type\":\"Country\"," +
                "\"identifier\":\"0\"}"));
    }

    @Test
    void testNoEmptyInternalMap() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_4f706cda);
        Assertions.assertFalse(transformedJSON.contains("\"kb:internal\":{}"));

    }

    @Test
    void testDateInjection() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_e683b0b8);
        Assertions.assertTrue(transformedJSON.contains("\"kb:storage_mTime\":"));
    }

    @Test
    public void testTransformationOfDomsRecord() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_DOMS_MIG_bd612d1e);
        assertTrue(transformedJSON.contains("\"@type\":\"VideoObject\""));
    }

    @Test
    public void testDomsBroadcastAlternateName() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_DOMS_MIG_bd612d1e);
        assertFalse(transformedJSON.contains("\"broadcastDisplayName\":\"Kanal 4\",\"alternateName\":\"\""));
    }

    @Test
    public void testDomsNoAccessionRef() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_DOMS_MIG_bd612d1e);
        assertFalse(transformedJSON.contains("\"PropertyID\":\"InternalAccessionRef\",\"value\":\"\""));
    }

    @Test
    public void testDomsEpisodeNumbers() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_DOMS_MIG_eaea0362);
        prettyPrintJson(transformedJSON);
        assertTrue(transformedJSON.contains("\"episodeNumber\":8,") && transformedJSON.contains("\"numberOfEpisodes\":24"));
    }


    //@Test
    void testErrorCatching() throws IOException {
        // This does not produce an error anymore, however I would like to produce an error to test the error handling.
        Map<String, String> fakeManifestation = Map.of("manifestation", "test");
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_4f706cda, fakeManifestation);

        Assertions.assertTrue(transformedJSON.contains("\"kb:transformation_error_description\":" +
                "\"First argument to parse-xml() is not a well-formed and namespace-well-formed XML document."));
    }

    @Test
    public void testHoldbackFields() throws IOException {
        Map<String, String> holdbackInjections = Map.of("holdbackDate", "2026-01-17T09:34:42Z",
                                                        "holdbackPurposeName","Aktualitet og debat");
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_DOMS_MIG_eaea0362, holdbackInjections);

        assertTrue(transformedJSON.contains("\"kb:holdback_date\":\"2026-01-17T09:34:42Z\""));
        assertTrue(transformedJSON.contains("\"kb:holdback_name\":\"Aktualitet og debat\""));
    }

    @Test
    public void testMultipleMaingenres() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_DOMS_MIG_82514cd9);
        assertTrue(transformedJSON.contains("\"genre\":\"film\""));
    }

    @Test
    public void testDomsRecordsNotConvertingToMediaObject() throws IOException {
        String transformedTV = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_DOMS_MIG_dd5f2f60);
        assertTrue(transformedTV.contains("\"@type\":\"VideoObject\","));

        String transformedRadio = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_DOMS_MIG_597e79f7);
        assertTrue(transformedRadio.contains("\"@type\":\"AudioObject\","));
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
