package dk.kb.present.transform;

import dk.kb.present.TestFiles;
import dk.kb.present.TestUtil;
import dk.kb.util.Resolver;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 *  To run these tests, the test metadata has to be fetched from the internal aegis project.
 *  With aegis running this can be done by running 'kb init' in this repository.
 */
public class XSLTPreservicaToSolrTransformerTest extends XSLTTransformerTestBase {

    public static final String PRESERVICA2SOLR = "xslt/preservica2solr.xsl";

    private static final Logger log = LoggerFactory.getLogger(XSLTPreservicaToSolrTransformerTest.class);


    @Override
    String getXSLT() {
        return PRESERVICA2SOLR;
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
        assertContains(TestFiles.PVICA_RECORD_44979f67, "\"id\":\"ds.test:44979f67-b563-462e-9bf1-c970167a5c5f.xml\"");
    }

    @Test
    public void testTitles() {
        assertContains(TestFiles.PVICA_RECORD_44979f67, "\"title\":\"Backstage II\"");

        assertContains(TestFiles.PVICA_RECORD_5a5357be, "\"title\":\"Dr. Pimple Popper: Before The Pop\"");
    }

    @Test
    public void testDirectDuration() {
        assertContains(TestFiles.PVICA_RECORD_44979f67,"\"duration_ms\":\"950000\"");
    }

    @Test
    public void testCalculatedDuration() {
        assertContains(TestFiles.PVICA_RECORD_5a5357be,"\"duration_ms\":\"1800000\"");
    }

    @Test
    public void testGenrePresent() {
        assertContains(TestFiles.PVICA_RECORD_44979f67, "\"genre\":\"");
    }

    @Test
    void testGenreContent() {
        assertContains(TestFiles.PVICA_RECORD_a8aafb121,"\"categories\":[\"Serier\",\"Krimiserie\"");
        assertNotContains(TestFiles.PVICA_RECORD_a8aafb121, "\"categories\":[\"hovedgenre: Serier ritzau\",\"undergenre: Krimiserie ritzau\"");
    }

    @Test
    void testEmptyGenre(){
        assertNotContains(TestFiles.PVICA_RECORD_68b233c3, "\"categories\":");
        assertNotContains(TestFiles.PVICA_RECORD_68b233c3, "\"genre\":");
    }

    @Test
    void testMainGenre() {
        assertContains(TestFiles.PVICA_RECORD_a8aafb121,"\"genre\":\"Serier\"");
    }

    @Test
    void testSubGenre() {
        assertContains(TestFiles.PVICA_RECORD_a8aafb121, "\"genre_sub\":\"Krimiserie\"");
    }

    @Test
    public void testNoGenre() {
        assertNotContains(TestFiles.PVICA_RECORD_5a5357be, "\"genre\":[\"");
    }

    @Test
    public void testResourceDescription() {
        assertNotContains(TestFiles.PVICA_RECORD_5a5357be, "\"resource_description\": \"Moving Image\"");
    }

    @Test
    public void testCollection() {
        assertNotContains(TestFiles.PVICA_RECORD_5a5357be, "\"collection\": \"Det Kgl. Bibliotek; Radio/TV-Samlingen\"");
    }

    @Test
    public void testCreatorAffiliation() {
        assertNotContains(TestFiles.PVICA_RECORD_5a5357be, "\"creator_affiliation\": \"DR Ultra\"");
    }

    @Test
    public void testNotes() {
        assertContains(TestFiles.PVICA_RECORD_a8aafb121, "\"notes\":[\"Eng. krimiserie\",\"To begravelser er planlagt.");
    }

    @Test
    void testDescription() {
        assertContains(TestFiles.PVICA_RECORD_a8aafb121, "\"description\":\"To begravelser er planlagt. Den ene for Sir Magnus Pye, den anden for Alan Conway.");
    }

    @Test
    void testShortDescription(){
        assertContains(TestFiles.PVICA_RECORD_a8aafb121, "\"abstract\":\"Eng. krimiserie\",");
    }


    @Test
    public void testOrigin(){
        assertContains(TestFiles.PVICA_RECORD_5a5357be, "\"origin\":\"ds.test\"");
    }

    @Test
    void testEpisode(){
        assertContains(TestFiles.PVICA_RECORD_a8aafb121, "\"episode\":");
        assertNotContains(TestFiles.PVICA_RECORD_1f3a6a66, "\"episode\":");
    }

    @Test
    void testNumberOfEpisodes(){
        assertContains(TestFiles.PVICA_RECORD_a8aafb121, "\"number_of_episodes\":");
        assertNotContains(TestFiles.PVICA_RECORD_5a5357be, "\"number_of_episodes\":");
        assertNotContains(TestFiles.PVICA_RECORD_1f3a6a66, "\"number_of_episodes\":");
    }

    @Test
    void testEpisodeButNoTotalNumberOfEpisodes(){
        assertContains(TestFiles.PVICA_RECORD_44979f67, "\"episode\"");
        assertNotContains(TestFiles.PVICA_RECORD_44979f67, "\"number_of_episode\"");
    }

    @Test
    void testLive(){
        assertContains(TestFiles.PVICA_RECORD_74e22fd8, "\"live_broadcast\":\"true\"");
        assertContains(TestFiles.PVICA_RECORD_a8aafb121, "\"live_broadcast\":\"false\"");
    }

    @Test
    void testEpisodeTitle(){
        assertContains(TestFiles.PVICA_RECORD_3945e2d1, "\"episode_title\":\"Kagerester\"");
        assertNotContains(TestFiles.PVICA_RECORD_74e22fd8, "\"episode_title\"");
    }

    @Test
    void testVideoQuality(){
        //ikke hd
        assertContains(TestFiles.PVICA_RECORD_5a5357be, "\"video_quality\":\"ikke hd\"");
        //Not defined
        assertNotContains(TestFiles.PVICA_RECORD_1f3a6a66, "\"video_quality\":");
        //TODO: We dont have any test files that are HD=true. Either find one when more data is available or create a mock
    }

    @Test
    void testSurround(){
        assertContains(TestFiles.PVICA_RECORD_5a5357be, "\"surround_sound\":\"false\"");
        assertContains(TestFiles.PVICA_RECORD_4b18d02d, "\"surround_sound\":\"true\"");

    }

    @Test
    void testInternalFormatIdentifier(){
        assertContains(TestFiles.PVICA_RECORD_3945e2d1, "\"internal_format_identifier_ritzau\":\"81318588\"");
    }

    @Test
    void testRetransmission(){
        assertContains(TestFiles.PVICA_RECORD_44979f67, "\"retransmission\":\"true\"");
        assertContains(TestFiles.PVICA_RECORD_4b18d02d, "\"retransmission\":\"false\"");
    }

    @Test
    void testHovedgenreId() {
        assertContains(TestFiles.PVICA_RECORD_3945e2d1,"\"internal_maingenre_id\":\"10\"");
    }

    @Test
    void testChannelId(){
        assertContains(TestFiles.PVICA_RECORD_3945e2d1, "\"internal_channel_id\":\"3\"");
    }

    @Test
    void testCountryOfOriginId(){
        assertContains(TestFiles.PVICA_RECORD_3945e2d1, "\"internal_country_of_origin_id\":\"0\"");
    }

    @Test
    void testRitzauProgramID(){
        assertContains(TestFiles.PVICA_RECORD_3945e2d1, "\"internal_ritzau_program_id\":\"25101143\"");
    }

    @Test
    void testProgramOphold(){
        assertContains(TestFiles.PVICA_RECORD_3945e2d1, "\"internal_program_ophold\":\"false\"");
        //TODO: Test true value with test file which contains program_ophold:program_ophold
    }

    @Test
    void testSubGenreId(){
        assertContains(TestFiles.PVICA_RECORD_3945e2d1, "\"internal_subgenre_id\":\"736\"");
    }

    @Test
    void testEpisodeId(){
        assertContains(TestFiles.PVICA_RECORD_3945e2d1, "\"internal_episode_id\":\"0\"");
    }

    @Test
    void testSeasonId(){
        assertContains(TestFiles.PVICA_RECORD_3945e2d1, "\"internal_season_id\":\"174278\"");
    }
    @Test
    void testSeriesId(){
        assertContains(TestFiles.PVICA_RECORD_3945e2d1, "\"internal_series_id\":\"146180\"");
    }
    @Test
    void testSubtitles(){
        assertContains(TestFiles.PVICA_RECORD_3945e2d1, "\"has_subtitles\":\"false\"");
        // TODO: Create test for has_subtitles:true, with custom test file
    }

    @Test
    void testSubtitlesHearingImpaired(){
        assertContains(TestFiles.PVICA_RECORD_3945e2d1, "\"has_subtitles_for_hearing_impaired\":\"false\"");
        // TODO: Create test for has_subtitles_for_hearing_impaired:true, with custom test file
    }
    @Test
    void testTeletext(){
        assertContains(TestFiles.PVICA_RECORD_3945e2d1, "\"internal_is_teletext\":\"false\"");
        // TODO: Create test for internal_is_teletext:true, with custom test file
    }

    @Test
    void testShowviewcode(){
        assertContains(TestFiles.PVICA_RECORD_3945e2d1, "\"internal_showviewcode\":\"0\"");
        // TODO: Create test for internal_is_teletext:true, with custom test file
    }

    @Test
    void testPaddingSeconds(){
        assertContains(TestFiles.PVICA_RECORD_3945e2d1, "\"internal_padding_seconds\":\"15\"");
        // TODO: Create test for internal_is_teletext:true, with custom test file
    }

    @Test
    void testAccessMetadata(){
        assertContains(TestFiles.PVICA_RECORD_3945e2d1, "\"internal_access_individual_prohibition\":\"Nej\"," +
                                                 "\"internal_access_claused\":\"Nej\"," +
                                                 "\"internal_access_malfunction\":\"Nej\"");
        //TODO: Add test that contains internal_access_comments
    }

    @Test
    void testPid(){
        assertContains(TestFiles.PVICA_RECORD_3945e2d1, "\"pid\":\"109.1.4\\/3945e2d1-83a2-40d8-af1c-30f7b3b94390\"");
    }

    @Test
    void testProgramStructure(){
        assertContains(TestFiles.PVICA_RECORD_1f3a6a66, "\"internal_program_structure_missing_seconds_start\":\"0\"," +
                                                 "\"internal_program_structure_missing_seconds_end\":\"0\"");

        //TODO: add tests for fields 'holes' and 'overlaps' with a constructed test file.
    }

    @Test
    void testStartTime(){
        assertContains(TestFiles.PVICA_RECORD_1f3a6a66, "\"startTime\":\"2012-04-28T16:15:00Z\"");
    }
    @Test
    void testEndTime(){
        assertContains(TestFiles.PVICA_RECORD_1f3a6a66, "\"endTime\":\"2012-04-28T16:40:00Z\"");
    }

    @Test
    void testStreamingUrl() throws IOException {
        String solrJson = TestUtil.getTransformedWithVideoChildAdded(PRESERVICA2SOLR, TestFiles.PVICA_RECORD_1f3a6a66, null);
        assertTrue(solrJson.contains("\"www.example.com\\/streaming\\/mp4:bart-access-copies-tv\\/cf\\/1d\\/b0\\/cf1db0e1-ade2-462a-a2b4-7488244fcca7\\/playlist.m3u8\""));
    }


    @Test
    public void prettyPrintTransformation() throws Exception {
        TestUtil.prettyPrintSolrJsonFromPreservica(TestFiles.PVICA_RECORD_44979f67);
    }



}
