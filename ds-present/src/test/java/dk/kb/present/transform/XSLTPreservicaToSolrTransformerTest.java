package dk.kb.present.transform;

import dk.kb.present.TestFiles;
import dk.kb.present.TestUtil;
import dk.kb.present.util.TestFileProvider;
import dk.kb.util.Resolver;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;

import static dk.kb.present.transform.XSLTPreservicaSchemaOrgTransformerTest.PRESERVICA2SCHEMAORG;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 *  To run these tests, the test metadata has to be fetched from the internal aegis project.
 *  With aegis running this can be done by running 'kb init' in this repository.
 */
public class XSLTPreservicaToSolrTransformerTest extends XSLTTransformerTestBase {

    public static final String PRESERVICA2SOLR = "xslt/preservica2solr.xsl";
    public static final String SCHEMA2SOLR =  "xslt/schemaorg2solr.xsl";
    private static final Logger log = LoggerFactory.getLogger(XSLTPreservicaToSolrTransformerTest.class);

    @Override
    String getXSLT() {
        return PRESERVICA2SCHEMAORG + " AND " + SCHEMA2SOLR;
    }

    @BeforeAll
    public static void beforeMethod() {
        if (Resolver.getPathFromClasspath("internal_test_files/tvMetadata") == null){
            log.warn("Internal test files are not present. Unittest 'XSLTPreservicaToSolrTransformerTest' is therefore not run.");
        }
        Assumptions.assumeTrue(Resolver.getPathFromClasspath("internal_test_files/tvMetadata") != null);
    }

    @Test
    public void testExtraction() {
        assertPvicaContains(TestFiles.PVICA_RECORD_44979f67, "\"id\":\"ds.test:44979f67-b563-462e-9bf1-c970167a5c5f.xml\"");
    }

    @Test
    public void testTitles() {
        assertPvicaContains(TestFiles.PVICA_RECORD_44979f67, "\"title\":\"Backstage II\"");

        assertPvicaContains(TestFiles.PVICA_RECORD_5a5357be, "\"title\":\"Dr. Pimple Popper: Before The Pop\"");
    }

    @Test
    public void testDirectDuration() {
        assertPvicaContains(TestFiles.PVICA_RECORD_44979f67,"\"duration_ms\":\"950000\"");
    }

    @Test
    public void testCalculatedDuration() {
        assertPvicaContains(TestFiles.PVICA_RECORD_5a5357be,"\"duration_ms\":\"1800000\"");
    }

    @Test
    public void testGenrePresent() {
        assertPvicaContains(TestFiles.PVICA_RECORD_44979f67, "\"genre\":\"");
    }

    @Test
    void testGenreContent() {
        assertPvicaContains(TestFiles.PVICA_RECORD_a8aafb121,"\"categories\":[\"Serier\",\"Krimiserie\"");
        assertPvicaNotContains(TestFiles.PVICA_RECORD_a8aafb121, "\"categories\":[\"hovedgenre: Serier ritzau\",\"undergenre: Krimiserie ritzau\"");
    }

    @Test
    void testEmptyGenre(){
        assertPvicaNotContains(TestFiles.PVICA_RECORD_68b233c3, "\"categories\":");
        assertPvicaNotContains(TestFiles.PVICA_RECORD_68b233c3, "\"genre\":");
    }

    @Test
    void testMainGenre() {
        assertPvicaContains(TestFiles.PVICA_RECORD_a8aafb121,"\"genre\":\"Serier\"");
    }

    @Test
    void testSubGenre() {
        assertPvicaContains(TestFiles.PVICA_RECORD_a8aafb121, "\"genre_sub\":\"Krimiserie\"");
    }

    @Test
    public void testNoGenre() {
        assertPvicaNotContains(TestFiles.PVICA_RECORD_5a5357be, "\"genre\":[\"");
    }

    @Test
    public void testResourceDescription() {
        assertPvicaNotContains(TestFiles.PVICA_RECORD_5a5357be, "\"resource_description\": \"Moving Image\"");
    }

    @Test
    public void testCollection() {
        assertPvicaNotContains(TestFiles.PVICA_RECORD_5a5357be, "\"collection\": \"Det Kgl. Bibliotek; Radio/TV-Samlingen\"");
    }

    @Test
    public void testCreatorAffiliation() {
        assertPvicaContains(TestFiles.PVICA_RECORD_5a5357be, "\"creator_affiliation\":\"TLC\"");
    }

    @Test
    public void testNoCreatorAffiliation() {
        assertPvicaNotContains(TestFiles.PVICA_RECORD_4f706cda, "\"creator_affiliation\"");
    }

    @Test
    public void testBroadcaster() {
        assertPvicaContains(TestFiles.PVICA_RECORD_accf8d1c, "\"broadcaster\":\"DR\"");
    }
    @Test
    public void testCreatorAffiliationGeneric() {
        assertPvicaContains(TestFiles.PVICA_RECORD_accf8d1c, "\"creator_affiliation_generic\":\"drp1\"");
    }

    @Test
    public void testNotes() {
        assertPvicaContains(TestFiles.PVICA_RECORD_a8aafb121, "\"notes\":[\"Eng. krimiserie\",\"To begravelser er planlagt.");
    }

    @Test
    void testDescription() {
        assertPvicaContains(TestFiles.PVICA_RECORD_a8aafb121, "\"description\":\"To begravelser er planlagt. Den ene for Sir Magnus Pye, den anden for Alan Conway.");
    }

    @Test
    void testShortDescription(){
        assertPvicaContains(TestFiles.PVICA_RECORD_a8aafb121, "\"abstract\":\"Eng. krimiserie\",");
    }


    @Test
    public void testOrigin(){
        assertPvicaContains(TestFiles.PVICA_RECORD_5a5357be, "\"origin\":\"ds.test\"");
    }

    @Test
    void testEpisode(){
        assertPvicaContains(TestFiles.PVICA_RECORD_a8aafb121, "\"episode\":");
        assertPvicaNotContains(TestFiles.PVICA_RECORD_1f3a6a66, "\"episode\":");
    }

    @Test
    void testNumberOfEpisodes() throws IOException {
        String transformedJSON = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_a8aafb121);
        TestUtil.prettyPrintJson(transformedJSON);

        assertPvicaContains(TestFiles.PVICA_RECORD_a8aafb121, "\"number_of_episodes\":");
        assertPvicaNotContains(TestFiles.PVICA_RECORD_5a5357be, "\"number_of_episodes\":");
        assertPvicaNotContains(TestFiles.PVICA_RECORD_1f3a6a66, "\"number_of_episodes\":");
    }

    @Test
    void testEpisodeButNoTotalNumberOfEpisodes(){
        assertPvicaContains(TestFiles.PVICA_RECORD_44979f67, "\"episode\"");
        assertPvicaNotContains(TestFiles.PVICA_RECORD_44979f67, "\"number_of_episode\"");
    }

    @Test
    void testLive(){
        assertPvicaContains(TestFiles.PVICA_RECORD_74e22fd8, "\"live_broadcast\":\"true\"");
        assertPvicaContains(TestFiles.PVICA_RECORD_a8aafb121, "\"live_broadcast\":\"false\"");
    }

    @Test
    void testEpisodeTitle(){
        assertPvicaContains(TestFiles.PVICA_RECORD_3945e2d1, "\"episode_title\":\"Kagerester\"");
        assertPvicaNotContains(TestFiles.PVICA_RECORD_74e22fd8, "\"episode_title\"");
    }

    @Test
    void testVideoQuality(){
        //ikke hd
        assertPvicaContains(TestFiles.PVICA_RECORD_5a5357be, "\"video_quality\":\"ikke hd\"");
        //Not defined
        assertPvicaNotContains(TestFiles.PVICA_RECORD_1f3a6a66, "\"video_quality\":");
        //TODO: We dont have any test files that are HD=true. Either find one when more data is available or create a mock
    }

    @Test
    void testSurround(){
        assertPvicaContains(TestFiles.PVICA_RECORD_5a5357be, "\"surround_sound\":\"false\"");
        assertPvicaContains(TestFiles.PVICA_RECORD_4b18d02d, "\"surround_sound\":\"true\"");

    }

    @Test
    void testInternalFormatIdentifier(){
        assertPvicaContains(TestFiles.PVICA_RECORD_3945e2d1, "\"internal_format_identifier_ritzau\":\"81318588\"");
    }

    @Test
    void testRetransmission(){
        assertPvicaContains(TestFiles.PVICA_RECORD_44979f67, "\"retransmission\":\"true\"");
        assertPvicaContains(TestFiles.PVICA_RECORD_4b18d02d, "\"retransmission\":\"false\"");
    }

    @Test
    void testHovedgenreId() {
        assertPvicaContains(TestFiles.PVICA_RECORD_3945e2d1,"\"internal_maingenre_id\":\"10\"");
    }

    @Test
    void testChannelId(){
        assertPvicaContains(TestFiles.PVICA_RECORD_3945e2d1, "\"internal_channel_id\":\"3\"");
    }

    @Test
    void testCountryOfOriginId(){
        assertPvicaContains(TestFiles.PVICA_RECORD_3945e2d1, "\"internal_country_of_origin_id\":\"0\"");
    }

    @Test
    void testRitzauProgramID(){
        assertPvicaContains(TestFiles.PVICA_RECORD_3945e2d1, "\"internal_ritzau_program_id\":\"25101143\"");
    }

    @Test
    void testProgramOphold(){
        assertPvicaContains(TestFiles.PVICA_RECORD_3945e2d1, "\"internal_program_ophold\":\"false\"");
        //TODO: Test true value with test file which contains program_ophold:program_ophold
    }

    @Test
    void testSubGenreId(){
        assertPvicaContains(TestFiles.PVICA_RECORD_3945e2d1, "\"internal_subgenre_id\":\"736\"");
    }

    @Test
    void testEpisodeId(){
        assertPvicaContains(TestFiles.PVICA_RECORD_3945e2d1, "\"internal_episode_id\":\"0\"");
    }

    @Test
    void testSeasonId(){
        assertPvicaContains(TestFiles.PVICA_RECORD_3945e2d1, "\"internal_season_id\":\"174278\"");
    }

    @Test
    void testSeriesId(){
        assertPvicaContains(TestFiles.PVICA_RECORD_3945e2d1, "\"internal_series_id\":\"146180\"");
    }
    @Test
    void testSubtitles(){
        assertPvicaContains(TestFiles.PVICA_RECORD_3945e2d1, "\"has_subtitles\":\"false\"");
        // TODO: Create test for has_subtitles:true, with custom test file
    }
    @Test
    void testSubtitlesHearingImpaired(){
        assertPvicaContains(TestFiles.PVICA_RECORD_3945e2d1, "\"has_subtitles_for_hearing_impaired\":\"false\"");
        // TODO: Create test for has_subtitles_for_hearing_impaired:true, with custom test file
    }

    @Test
    void testTeletext(){
        assertPvicaContains(TestFiles.PVICA_RECORD_3945e2d1, "\"internal_is_teletext\":\"false\"");
        // TODO: Create test for internal_is_teletext:true, with custom test file
    }
    @Test
    void testShowviewcode(){
        assertPvicaContains(TestFiles.PVICA_RECORD_3945e2d1, "\"internal_showviewcode\":\"0\"");
        // TODO: Create test for internal_is_teletext:true, with custom test file
    }

    @Test
    void testPaddingSeconds(){
        assertPvicaContains(TestFiles.PVICA_RECORD_3945e2d1, "\"internal_padding_seconds\":\"15\"");
        // TODO: Create test for internal_is_teletext:true, with custom test file
    }

    @Test
    void testAccessMetadata(){
        assertPvicaContains(TestFiles.PVICA_RECORD_3945e2d1, "\"internal_access_individual_prohibition\":\"Nej\"," +
                                                 "\"internal_access_claused\":\"Nej\"," +
                                                 "\"internal_access_malfunction\":\"Nej\"");
        //TODO: Add test that contains internal_access_comments
    }

    @Test
    void testPid(){
        assertPvicaContains(TestFiles.PVICA_RECORD_3945e2d1, "\"pid\":\"109.1.4\\/3945e2d1-83a2-40d8-af1c-30f7b3b94390\"");
    }

    @Test
    void testProgramStructure(){
        assertPvicaContains(TestFiles.PVICA_RECORD_1f3a6a66, "\"internal_program_structure_missing_seconds_start\":\"0\"," +
                                                 "\"internal_program_structure_missing_seconds_end\":\"0\"");

        //TODO: add tests for fields 'holes' and 'overlaps' with a constructed test file.
    }

    @Test
    void testStartTime(){
        assertPvicaContains(TestFiles.PVICA_RECORD_1f3a6a66, "\"startTime\":\"2012-04-28T16:15:00Z\"");
    }

    @Test
    void testEndTime(){
        assertPvicaContains(TestFiles.PVICA_RECORD_1f3a6a66, "\"endTime\":\"2012-04-28T16:40:00Z\"");
    }

    @Test
    void testNoNotes(){
        assertPvicaNotContains(TestFiles.PVICA_RECORD_b346acc8, "\"notes\":");
    }

    @Test
    void testStreamingUrl() throws IOException {
        String solrJson = TestUtil.getTransformedWithVideoChildAdded(PRESERVICA2SOLR, TestFiles.PVICA_RECORD_1f3a6a66, null);
        assertTrue(solrJson.contains("\"www.example.com\\/streaming\\/mp4:bart-access-copies-tv\\/cf\\/1d\\/b0\\/cf1db0e1-ade2-462a-a2b4-7488244fcca7\\/playlist.m3u8\""));
    }

    /* disabled as they are not represented in solr
    @Test
    void testOverlaps() {
        assertPvicaContains(TestFiles.PVICA_RECORD_b346acc8, "\"internal_program_structure_overlap_type_two_length_ms\":\"3120\"");
        assertPvicaContains(TestFiles.PVICA_RECORD_b346acc8, "\"internal_program_structure_overlap_type_one_length_ms\":\"1320\"");
        assertPvicaContains(TestFiles.PVICA_RECORD_b346acc8, "\"internal_program_structure_overlap_type_one_file1UUID\":\"f73b69da-2bc0-4e06-b19b-95f24756804e\"");
        assertPvicaContains(TestFiles.PVICA_RECORD_b346acc8, "\"internal_program_structure_overlap_type_two_file2UUID\":\"f73b69da-2bc0-4e06-b19b-95f24756804e\"");
    }*/

    @Test
    void testAccessConditions(){
        assertPvicaContains(TestFiles.PVICA_RECORD_b346acc8, "\"conditions_of_access\":\"placeholderCondition\"");
    }

    @Test
    void testNoBroadcasterInformation(){
        assertPvicaNotContains(TestFiles.PVICA_RECORD_0b3f6a54, "\"publishedOn\"");
    }

    @Test
    void testNoSpecificOrigin(){
        assertPvicaContains(TestFiles.PVICA_RECORD_5b29fca1, "\"origin\"");
    }

    @Test
    public void prettyPrintTransformation() throws Exception {
        String solrJson = TestUtil.getTransformedToSolrJsonThroughSchemaJson(PRESERVICA2SCHEMAORG, TestFiles.PVICA_RECORD_2973e7fa);
        TestUtil.prettyPrintJson(solrJson);
    }

    /**
     * Wrapper for {@link #assertMultiTestsThroughSchemaTransformation(String, Consumer[])} which verifies that the
     * transformed record contains the given {@code substring}.
     * @param recordFile the file to load, transform and test.
     * @param substring must be present in the transformed record.
     */
    public void assertPvicaContains(String recordFile, String substring) {
        assertMultiTestsThroughSchemaTransformation(recordFile,
                solrDoc -> assertTrue(solrDoc.contains(substring))
        );
    }

    /**
     * Wrapper for {@link #assertMultiTestsThroughSchemaTransformation(String, Consumer[])} which verifies that the
     * transformed record does not contain the given {@code substring}.
     * @param recordFile the file to load, transform and test.
     * @param substring must be present in the transformed record.
     */
    public void assertPvicaNotContains(String recordFile, String substring) {
        assertMultiTestsThroughSchemaTransformation(recordFile,
                solrDoc -> assertFalse(solrDoc.contains(substring))
        );
    }

    /**
     * Checks that internal test files are available and if not, logs a warning and returns.
     * <p>
     * If the check passes, the content of the file {@code record} is transformed using two XSLTs.
     * At first the XML record is transformed to Schema.org JSON and then the schema.org JSON is transformed to solr
     * documents and the given tests are performed on the result.
     * @param record file with a record that is to be transformed.
     * @param tests Zero or more tests to perform on the transformed record.
     */
    @SafeVarargs
    public final void assertMultiTestsThroughSchemaTransformation(String record, Consumer<String>... tests){
        if (!TestFileProvider.hasSomeTestFiles()) {
            return;  // ensureTestFiles takes care of logging is there are no internal test files
        }
        String solrString;
        try {
            solrString = TestUtil.getTransformedToSolrJsonThroughSchemaJson(PRESERVICA2SCHEMAORG, record);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to fetch and transform '" + record + "' using XSLT '" + getXSLT() + "'", e);
        }

        TestUtil.prettyPrintJson(solrString);

        Arrays.stream(tests).forEach(test -> test.accept(solrString));
    }



}
