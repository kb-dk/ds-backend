package dk.kb.present.solr;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dk.kb.present.TestFiles;
import dk.kb.present.TestUtil;
import dk.kb.util.Resolver;
import dk.kb.util.yaml.YAML;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.CoreContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

import static dk.kb.present.TestFiles.CUMULUS_RECORD_05fea810;
import static dk.kb.present.TestFiles.CUMULUS_RECORD_096c9090;
import static dk.kb.present.TestFiles.CUMULUS_RECORD_0c02aa10;
import static dk.kb.present.TestFiles.CUMULUS_RECORD_226d41a0;
import static dk.kb.present.TestFiles.CUMULUS_RECORD_25461fb0;
import static dk.kb.present.TestFiles.CUMULUS_RECORD_3956d820;
import static dk.kb.present.TestFiles.CUMULUS_RECORD_40221e30;
import static dk.kb.present.TestFiles.CUMULUS_RECORD_54b34b50;
import static dk.kb.present.TestFiles.CUMULUS_RECORD_770379f0;
import static dk.kb.present.TestFiles.CUMULUS_RECORD_8e608940;
import static dk.kb.present.TestFiles.CUMULUS_RECORD_9c17a440;
import static dk.kb.present.TestFiles.CUMULUS_RECORD_ANSK;
import static dk.kb.present.TestFiles.CUMULUS_RECORD_FM;
import static dk.kb.present.TestFiles.CUMULUS_RECORD_aaf3b130;
import static dk.kb.present.TestFiles.CUMULUS_RECORD_e2519ce0;
import static dk.kb.present.TestFiles.PVICA_RECORD_1f3a6a66;
import static dk.kb.present.TestFiles.PVICA_RECORD_2973e7fa;
import static dk.kb.present.TestFiles.PVICA_RECORD_3945e2d1;
import static dk.kb.present.TestFiles.PVICA_RECORD_44979f67;
import static dk.kb.present.TestFiles.PVICA_RECORD_74e22fd8;
import static dk.kb.present.TestFiles.PVICA_RECORD_9d9785a8;
import static dk.kb.present.TestFiles.PVICA_RECORD_accf8d1c;
import static dk.kb.present.TestFiles.PVICA_RECORD_b346acc8;
import static dk.kb.present.TestFiles.PVICA_RECORD_e683b0b8;
import static dk.kb.present.transform.XSLTPreservicaSchemaOrgTransformerTest.PRESERVICA2SCHEMAORG;
import static org.junit.jupiter.api.Assertions.*;

public class EmbeddedSolrTest {

    private static final Logger log = LoggerFactory.getLogger(EmbeddedSolrTest.class);
    private static final String solr_home = "target/solr";

    private static CoreContainer coreContainer = null;
    private static EmbeddedSolrServer embeddedServer = null;

    public static final String MODS2SOLR = "xslt/mods2solr.xsl";
    public static final String PRESERVICA2SOLR = "xslt/preservica2solr.xsl";

    @BeforeAll
    public static void startEmbeddedSolrServer() {

        File solrHomeDir = new File(solr_home);
        String solrHomeAbsoluteDir= solrHomeDir.getAbsolutePath();
        Path solrHome =  Paths.get(solrHomeAbsoluteDir);
        System.setProperty("solr.install.dir", solrHomeAbsoluteDir);
        Properties props = new Properties();
        // props.put("solr.install.dir", solrHomeDir.getAbsolutePath()); //Does not
        // work. Use system property above for now
        coreContainer = new CoreContainer(solrHome, props);
        coreContainer.load();
        embeddedServer = new EmbeddedSolrServer(coreContainer, "dssolr");
    }

    @AfterAll
    public static void tearDown() throws IOException {
        coreContainer.shutdown();
        embeddedServer.close();
    }

    /*
     * Delete all documents in solr between tests, so each unittest gets a clean solr.
     */
    @BeforeEach
    public void deleteDocs() throws SolrServerException, IOException {
        embeddedServer.deleteByQuery("*:*");
    }

    @Test
    void testRecord000332() throws IOException {
        SolrDocument record = singleMODSIndex(CUMULUS_RECORD_05fea810);

        //Single value field
        assertEquals("000332.tif",record.getFieldValue("filename_local"));
        assertEquals("Billede, Todimensionalt billedmateriale", record.getFieldValue("resource_description_general"));

        //multivalue field
        // Creator date of death
        assertMultivalueField(record, "creator_date_of_death", "1868-2-14", "1895-6-25", "1865-3-8" );

    }

    /**
     * Full test for one item.
     */
    @Test
    void testRecordDPK() throws IOException {
        SolrDocument record = singleMODSIndex(CUMULUS_RECORD_3956d820);

        assertContentAllSingleValues(record, "DPK000107.tif", "da",
                "Billedsamlingen. Postkortsamlingen, Vestindien, Sankt Thomas, Charlotte Amalie, Det gamle fort/politistation",
                "Samlingsbilleder", "Billedsamlingen", 9657172L, 1429,2247);

        //Single value fields
        assertEquals("Vestindien, Sankt Thomas, Charlotte Amalie, Fort Christian", record.getFieldValue("area"));
        // type_of_resource
        assertEquals("Billede, Todimensionalt billedmateriale", record.getFieldValue("resource_description_general"));

        //Multivalue fields
        // topic
        assertMultivalueField(record, "topic", "postkort","forter","Dannebrog", "børn", "arkitekturer",
                "postcards", "forts", "Dannebrog", "children", "architectures" );
    }



    /**
     * Full test for item
     */
    @Test
    void testRecord096c9090() throws IOException {
        SolrDocument record = singleMODSIndex(CUMULUS_RECORD_096c9090);

        assertContentAllSingleValues(record,"000225.tif", "da",
                "Billedsamlingen. Danske portrætter, 4°, Egede, Poul (1708-1789)",
                "Samlingsbilleder","Billedsamlingen",6691996L,1812,1227);


        //Single value field
        assertEquals("1755",record.getFieldValue("production_date_start"));
        assertEquals("1831",record.getFieldValue("production_date_end"));
        assertEquals("Billede, Todimensionalt billedmateriale", record.getFieldValue("resource_description_general"));

        //multivalue fields
        // creator_name
        assertMultivalueField(record,"creator_name", "Clemens, Johann Friderich");

        // creator_full_name
        assertMultivalueField(record,"creator_full_name", "Johann Friderich Clemens");

        // creator_family_name
        assertMultivalueField(record,"creator_family_name", "Clemens");

        // creator_given_name
        assertMultivalueField(record,"creator_given_name", "Johann Friderich");

        // creator_terms_of_address
        assertMultivalueField(record,"creator_terms_of_address", "kobberstikker");

        // topic
        assertMultivalueField(record, "topic", "Poul Egede. 1911,7507.", "Billedet befinder sig i Kort- og Billedafdelingen, Det Kongelige Bibliotek" );

        // subject_name
        assertMultivalueField(record,"subject_name", "Egede, Poul Hansen");

        // subject_full_name
        assertMultivalueField(record,"subject_full_name", "Poul Hansen Egede");

        // subject_family_name
        assertMultivalueField(record,"subject_family_name", "Egede");

        // subject_given_name
        assertMultivalueField(record,"subject_given_name", "Poul Hansen");

        // subject_date_of_birth
        assertMultivalueField(record,"subject_date_of_birth", "1708");

        // subject_date_of_death
        assertMultivalueField(record,"subject_date_of_death", "1789");

        // subject_terms_of_address
        assertMultivalueField(record,"subject_terms_of_address", "teolog, missionær, grønlandsfarer og biskop");
    }

    @Test
    void testRecordDt005031() throws IOException {
        SolrDocument record = singleMODSIndex(CUMULUS_RECORD_aaf3b130);

        //Single value field
        assertEquals("DT005031.tif",record.getFieldValue("filename_local"));

		assertEquals("Billede, Todimensionalt billedmateriale", record.getFieldValue("resource_description_general"));
	}

    @Test
    void testRecordANSK() throws IOException {
        SolrDocument record = singleMODSIndex(CUMULUS_RECORD_ANSK);

        //Single value field
        assertEquals("ANSK_11614.tif",record.getFieldValue("filename_local"));
		assertEquals("Billede, Todimensionalt billedmateriale", record.getFieldValue("resource_description_general"));
        assertNull(record.getFieldValue("notes_length"));
	}

    @Test
    void testRecordSkfF0137() throws IOException {
        SolrDocument record = singleMODSIndex(CUMULUS_RECORD_54b34b50);

        //Single value field
        assertEquals("SKF_f_0137.tif",record.getFieldValue("filename_local"));
		assertEquals("Billede, Todimensionalt billedmateriale", record.getFieldValue("resource_description_general"));

        assertMultivalueField(record, "notes",
                "Beskrivelse: Den gamle rytterskole i Hørning (Sønder-Hørning). Facadebillede. " +
                              "Fotografi kopi udført af Rudolph Jørgensen Helsingør (etabi 1897)",
                              "Topografisk nr: 2156", "Den gamle rytterskole i Hørning (Sønder-Hørning). " +
                              "Facadebillede. Fotografi kopi udført af Rudolph Jørgensen Helsingør (etabi 1897)"  );
	}

    @Test
    void testRecordKhp() throws IOException {
        SolrDocument record = singleMODSIndex(CUMULUS_RECORD_8e608940);

        //Single value field
        assertEquals("KHP0001-049.tif",record.getFieldValue("filename_local"));
        assertEquals("Billede, Todimensionalt billedmateriale", record.getFieldValue("resource_description_general"));

        //multivalue field
		assertMultivalueField(record, "categories", "KHP",
				"Keld Helmer-Petersen", "1940-1950", "Helmer-Petersen", "Keld",
				"CAR- BLO katagori", "ikke UA");
	}

    @Test
    void testRecordUldallForTitleAndPlaceOfProductionAndGenre() throws IOException {
        SolrDocument record = singleMODSIndex(CUMULUS_RECORD_e2519ce0);

        //Single value field
        assertEquals("Uldall_186_2_Foborg.tif",record.getFieldValue("filename_local"));

        //assertEquals("Topografi",record.getFieldValue("genre"));
        assertMultivalueField(record, "genre", "Topografi");

        // Title field
        assertMultivalueField(record, "title", "Foborg, Foburgum");

		// Place of production
		assertEquals("Danmark", record.getFieldValue("production_place"));
	}

    @Test
    void testAccessionNumberExtraction() throws IOException {
        SolrDocument record = singleMODSIndex(CUMULUS_RECORD_FM);

        //Single value field
        assertEquals("2000-3/7",record.getFieldValue("accession_number"));
    }

    @Test
    void testPublishedInAndCollection() throws IOException {
        SolrDocument record = singleMODSIndex(CUMULUS_RECORD_25461fb0);

        //Single value field
        assertEquals("Bladtegnersamlingen",record.getFieldValue("collection"));
        assertEquals("Aktuelt", record.getFieldValue("published_in"));
    }

    @Test
    void testTitle() throws IOException {
        SolrDocument record = singleMODSIndex(CUMULUS_RECORD_770379f0);

        assertMultivalueField(record, "title", "Romeo og Julie");
        assertEquals(1, record.getFieldValue("title_count"));
    }

    @Test
    void testResourceId() throws IOException {
        SolrDocument record = singleMODSIndex(CUMULUS_RECORD_40221e30);
        assertMultivalueField(record, "resource_id", "/DAMJP2/DAM/Samlingsbilleder/0000/624/420/KE070592");
    }

    @Test
    void testMapScale() throws IOException {
        SolrDocument record = singleMODSIndex(CUMULUS_RECORD_0c02aa10);

        //Single value field
        assertEquals("Målestok 1:75 000",record.getFieldValue("map_scale"));
    }

    @Test
    void testSubjectStrict() throws IOException {
        SolrDocument record = singleMODSIndex(CUMULUS_RECORD_9c17a440);

        String[] testName = Arrays.copyOf(
                record.getFieldValues("subject_full_name_strict").toArray(),
                1, String[].class);

        String[] correctResult = new String[]{"Frederik 9"};
        assertEquals(correctResult[0], testName[0]);
    }

    @Test
    void testSingleProductionDate() throws IOException{
        SolrDocument record = singleMODSIndex(CUMULUS_RECORD_226d41a0);

        assertEquals("1971", record.getFieldValue("production_date"));
    }

    @Test
    void testCountsCreatedBySolr() throws IOException {
        SolrDocument record = singleMODSIndex(CUMULUS_RECORD_05fea810);

        assertEquals(3, record.getFieldValue("creator_count"));
        assertEquals(6, record.getFieldValue("topic_count"));
        assertEquals(1, record.getFieldValue("subject_count"));
        assertEquals(1, record.getFieldValue("notes_count"));
        assertEquals(3, record.getFieldValue("categories_count"));
    }

    @Test
    void testNotesLength() throws IOException {
        SolrDocument record = singleMODSIndex(CUMULUS_RECORD_25461fb0);

        assertEquals(93, record.getFieldValue("notes_length"));
    }

    @Test
    void testPreservicaPremiere() throws Exception {
        if (Resolver.getPathFromClasspath("internal_test_files/tvMetadata") != null) {
            SolrDocument record = singlePreservicaIndex(PVICA_RECORD_44979f67);
            assertFalse((Boolean) record.getFieldValue("premiere"));
        } else {
            log.info("Preservica test files are not present. Embedded Solr tests for preservica metadata are not run.");
        }
    }

    @Test
    @Tag("integration")
    void testPreservicaDuration() throws Exception {
        testLongValuePreservicaField(PVICA_RECORD_44979f67, "duration_ms", 950000L);
    }

    @Test
    void testOriginMods() throws Exception {
        SolrDocument record = singleMODSIndex(CUMULUS_RECORD_40221e30);
        assertEquals("ds.test", record.getFieldValue("origin"));
    }
    @Test
    @Tag("integration")
    void testOriginPreservica() throws Exception {
        testStringValuePreservicaField(PVICA_RECORD_44979f67, "origin", "ds.test");
    }

    @Test
    @Tag("integration")
    void testOriginalTitle() throws Exception {
        // With the new transformation chain this field is not created records where titel and original title are identical
        // Here the value is only extracted to the title field in JSONLD and the solr field original_titel does not
        // contain any values for such a record
        testStringValuePreservicaField(PVICA_RECORD_74e22fd8, "original_title", "Pokalfodbold: Finale: OB - FC Midtjylland, direkte");
    }

    @Test
    @Tag("integration")
    void testEpisodeTitel() throws Exception {
        testStringValuePreservicaField(PVICA_RECORD_3945e2d1, "episode_title", "Kagerester");
    }
    @Test
    @Tag("integration")
    void testRitzauId() throws Exception {
        testStringValuePreservicaField(PVICA_RECORD_3945e2d1, "ritzau_id",
                "4aa3482b-a149-44d1-8715-f789e69f1a1e");
    }

    @Test
    @Tag("integration")
    void testTvmeterId() throws Exception {
        testStringValuePreservicaField(PVICA_RECORD_44979f67, "tvmeter_id",
                "78e74634-bec1-4cea-a984-20192c97b743");
    }


    //The embedded solr is returning timestamps in CEST time, which is 2 hours in front of UTC, which is the indexed
    //format and the one available in the metadata
    @Test
    @Tag("integration")
    void testPvicaStartTime() throws Exception {
        // Epoch value of 2018-07-11T18-06-33Z
        Date startTime = new Date(1531332393000L);
        testDateValuePreservicaField(PVICA_RECORD_44979f67, "startTime", startTime);
    }


    @Test
    @Tag("integration")
    void testPvicaEndTime() throws Exception {
        // Epoch value of 2018-07-11T18-22-23Z
        Date endTime = new Date(1531333343000L);
        testDateValuePreservicaField(PVICA_RECORD_44979f67, "endTime", endTime);
    }

    @Test
    @Tag("integration")
    void testPvicaTemporalFields() throws Exception {
        testStringValuePreservicaField(PVICA_RECORD_1f3a6a66, "temporal_start_year", "2012");
        testStringValuePreservicaField(PVICA_RECORD_1f3a6a66, "temporal_start_time_da_string", "18:15:00" );
        testStringValuePreservicaField(PVICA_RECORD_1f3a6a66, "temporal_end_time_da_string", "18:40:00");
        testStringValuePreservicaField(PVICA_RECORD_1f3a6a66, "temporal_start_day_da", "Saturday");
        testStringValuePreservicaField(PVICA_RECORD_1f3a6a66, "temporal_end_day_da", "Saturday");
        testIntValuePreservicaField(PVICA_RECORD_1f3a6a66, "temporal_start_month", 4);
        testIntValuePreservicaField(PVICA_RECORD_1f3a6a66, "temporal_start_hour_da", 18);

        Date startDate = new Date(253370830500000L);
        testDateValuePreservicaField(PVICA_RECORD_1f3a6a66, "temporal_start_time_da_date", startDate);

        Date endDate = new Date(253370832000000L);
        testDateValuePreservicaField(PVICA_RECORD_1f3a6a66, "temporal_end_time_da_date", endDate);

    /*
    <field name="temporal_start_time_da_date" type="pdate">
    <field name="temporal_end_time_da_date" type="pdate">
    */
    }

    @Test
    @Tag("integration")
    void testColor() throws Exception {
        testBooleanValuePreservicaField(PVICA_RECORD_44979f67, "color", true );
    }

    @Test
    @Tag("integration")
    void testVideoQuality() throws Exception {
        testStringValuePreservicaField(PVICA_RECORD_44979f67, "video_quality", "ikke hd" );
    }

    @Test
    @Tag("integration")
    void testSurroundSound() throws Exception {
        testBooleanValuePreservicaField(PVICA_RECORD_44979f67, "surround_sound", false);
    }

    @Test
    @Tag("integration")
    void testAspectRation() throws Exception {
        testStringValuePreservicaField(PVICA_RECORD_44979f67, "aspect_ratio", "16:9");
    }

    // TODO: Why are these not cast correctly.
    @Test
    @Tag("integration")
    void testEpisodeNumber() throws Exception {
        testIntValuePreservicaField(PVICA_RECORD_44979f67, "episode", 3);
    }

    @Test
    @Tag("integration")
    void testNumberOfEpisodes() throws Exception {
        testIntValuePreservicaField(PVICA_RECORD_3945e2d1, "number_of_episodes", 8);
    }

    @Test
    @Tag("integration")
    void testLive() throws Exception {
        testBooleanValuePreservicaField(PVICA_RECORD_3945e2d1, "live_broadcast", false);
    }

    @Test
    @Tag("integration")
    void testRetransmission() throws Exception {
        testBooleanValuePreservicaField(PVICA_RECORD_3945e2d1, "retransmission", false);
    }


    @Test
    @Tag("integration")
    void testAbstract() throws Exception {
        testStringValuePreservicaField(PVICA_RECORD_9d9785a8, "abstract", "Dan. dok.-serie");
    }

    @Test
    @Tag("integration")
    void testSubtitles() throws Exception {
        testBooleanValuePreservicaField(PVICA_RECORD_9d9785a8, "has_subtitles", false);
    }

    @Test
    @Tag("integration")
    void testSubtitlesForImpaired() throws Exception {
        testBooleanValuePreservicaField(PVICA_RECORD_9d9785a8, "has_subtitles_for_hearing_impaired", false);
    }

    @Test
    @Tag("integration")
    void testPreservicaPid() throws Exception {
        testStringValuePreservicaField(PVICA_RECORD_9d9785a8, "pid",
                "109.1.4/9d9785a8-71f4-4b34-9a0e-1c99c13b001b");
    }

    @Test
    @Tag("integration")
    void testGenreSub() throws Exception {
        testStringValuePreservicaField(PVICA_RECORD_3945e2d1, "genre_sub", "Alle");
    }

    @Test
    @Tag("integration")
    void testInternalAccesssionRef() throws Exception {
        testStringValuePreservicaField(PVICA_RECORD_3945e2d1, "internal_accession_ref",
                "4eb00536-5efa-4346-9165-b13997b0ffd2");
    }

    @Test
    @Tag("integration")
    void testInternalFormatIdentiferRitzau() throws Exception {
        testStringValuePreservicaField(PVICA_RECORD_3945e2d1, "internal_format_identifier_ritzau",
                "81318588");
    }
    @Test
    @Tag("integration")
    void testInternalFormatIdentiferNielsen() throws Exception {
        testStringValuePreservicaField(PVICA_RECORD_9d9785a8, "internal_format_identifier_nielsen",
                "101|20220227|252839|255705|0|4d3a94a4-1ff0-4598-b593-034eacf1c77d|98");
    }

    @Test
    @Tag("integration")
    void testInternalMaingenreId() throws Exception {
        testStringValuePreservicaField(PVICA_RECORD_9d9785a8, "internal_maingenre_id", "13");
    }

    @Test
    @Tag("integration")
    void testInternalChannelId() throws Exception {
        testStringValuePreservicaField(PVICA_RECORD_9d9785a8, "internal_channel_id", "3");
    }

    @Test
    @Tag("integration")
    void testInternalCountryOfOriginId() throws Exception {
        testStringValuePreservicaField(PVICA_RECORD_9d9785a8, "internal_country_of_origin_id", "0");
    }
    @Test
    @Tag("integration")
    void testInternalRitzauProgramId() throws Exception {
        testStringValuePreservicaField(PVICA_RECORD_3945e2d1, "internal_ritzau_program_id", "25101143");
    }

    @Test
    @Tag("integration")
    void testSubgenreId() throws Exception {
        testStringValuePreservicaField(PVICA_RECORD_3945e2d1, "internal_subgenre_id", "736");
    }

    @Test
    @Tag("integration")
    void testEpisodeId() throws Exception {
        testStringValuePreservicaField(PVICA_RECORD_3945e2d1, "internal_episode_id", "0");
    }

    @Test
    @Tag("integration")
    void testSeasonId() throws Exception {
        testStringValuePreservicaField(PVICA_RECORD_3945e2d1, "internal_season_id", "174278");
    }

    @Test
    @Tag("integration")
    void testSeriesId() throws Exception {
        testStringValuePreservicaField(PVICA_RECORD_3945e2d1, "internal_series_id", "146180");
    }

    @Test
    @Tag("integration")
    void testProgramOphold() throws Exception {
        testBooleanValuePreservicaField(PVICA_RECORD_3945e2d1, "internal_program_ophold", false);
    }

    @Test
    @Tag("integration")
    void testIsTeletext() throws Exception {
        testBooleanValuePreservicaField(PVICA_RECORD_3945e2d1, "internal_is_teletext", false);
    }

    @Test
    @Tag("integration")
    void testShowviewcode() throws Exception {
        testStringValuePreservicaField(PVICA_RECORD_3945e2d1, "internal_showviewcode", "0");
    }

    @Test
    @Tag("integration")
    void testPadding() throws Exception {
        testStringValuePreservicaField(PVICA_RECORD_3945e2d1, "internal_padding_seconds", "15");
    }

    @Test
    @Tag("integration")
    void testInternalAccess() throws Exception {
        testStringValuePreservicaField(PVICA_RECORD_3945e2d1, "internal_access_individual_prohibition", "Nej");
        testStringValuePreservicaField(PVICA_RECORD_3945e2d1, "internal_access_claused", "Nej");
        testStringValuePreservicaField(PVICA_RECORD_3945e2d1, "internal_access_malfunction", "Nej");
        testStringValuePreservicaField(PVICA_RECORD_3945e2d1, "internal_access_comments", null);
    }

    @Test
    @Tag("integration")
    void testProgramStructure() throws Exception {
        testIntValuePreservicaField(PVICA_RECORD_1f3a6a66, "internal_program_structure_missing_seconds_start", 0);
        testIntValuePreservicaField(PVICA_RECORD_1f3a6a66, "internal_program_structure_missing_seconds_end", 0);
        testStringValuePreservicaField(PVICA_RECORD_1f3a6a66, "internal_program_structure_holes", null);
        testBooleanValuePreservicaField(PVICA_RECORD_1f3a6a66, "internal_program_structure_overlaps", false);
        testBooleanValuePreservicaField(PVICA_RECORD_1f3a6a66, "internal_program_structure_overlaps", false);
    }

    @Test
    @Tag("integration")
    void testOverlappingFiles() throws Exception {
        testStringPresentInPreservicaMultiField(PVICA_RECORD_2973e7fa, "internal_overlapping_files", "c8f496cf-4e0b-4682-8eee-67dfe07525d2,8ac98f6e-5653-492a-ab8c-c1462edaeb4a");
    }

    /* Disabled as overlaps arent represented in solr as of now
    @Test
    void testProgramStructureOverlaps() throws Exception {
        testLongValuePreservicaField(PVICA_RECORD_b346acc8, "internal_program_structure_overlap_type_two_length_ms", 3120L);
        testLongValuePreservicaField(PVICA_RECORD_b346acc8, "internal_program_structure_overlap_type_one_length_ms", 1320L);
        testStringValuePreservicaField(PVICA_RECORD_b346acc8, "internal_program_structure_overlap_type_two_file2UUID", "f73b69da-2bc0-4e06-b19b-95f24756804e");
        testStringValuePreservicaField(PVICA_RECORD_b346acc8, "internal_program_structure_overlap_type_one_file1UUID", "f73b69da-2bc0-4e06-b19b-95f24756804e");

    }*/

    @Test
    @Tag("integration")
    void testIndexingOfRadioRecord() throws Exception {
        testStringValuePreservicaField(PVICA_RECORD_e683b0b8, "resource_description", "AudioObject");
    }

    @Test
    @Tag("integration")
    void testAccessConditions() throws Exception {
        testStringValuePreservicaField(PVICA_RECORD_e683b0b8, "conditions_of_access", "placeholderCondition");
    }

    @Test
    @Tag("integration")
    void testBroadcaster() throws Exception {
        testStringValuePreservicaField(PVICA_RECORD_accf8d1c, "broadcaster", "DR");
    }

    @Test
    @Tag("integration")
    void testDateModified() throws Exception {
        testLongValuePreservicaField(PVICA_RECORD_e683b0b8, "internal_storage_mTime", 1701261949625000L);
    }

    /*
     * ------- Private helper methods below --------------
     */

    /*
     * Embedded solr does not have a http listener, so we can not add call and add documents as JSON.
     * They needs to be converted to SolrInputDocument. This seems to be the simplest way to do it...
     * Correct me if I am wrong.
     *
     */
    private  SolrInputDocument convertJsonToSolrJavaDoc(String json) throws IOException {

        //Object is string or String[] for multivalued
        Map<String, Object> map = new ObjectMapper().readValue(json, new TypeReference<Map<String, Object>>(){});

        SolrInputDocument document = new SolrInputDocument();

        for (String key : map.keySet()) {
            //Object can be String or String[]
            Object  value = map.get(key);
            if (value instanceof String) {
                //	System.out.println("Adding:"+key +"="+map.get(key));
                document.addField(key, map.get(key));

            }
            else if (value instanceof ArrayList) {
                for (Object o : (ArrayList<Object>) value) {
                    //		System.out.println("Adding:"+key +"="+o.toString());
                    document.addField(key, o.toString());
                }
            }
            else {//sanity check, should not happen
                log.error("Unknown json type"+value.getClass());
                throw new IOException("Unknown json type"+value.getClass());
            }
        }
        return document;
    }

    /**
     * <ul>
     * <li>Transform the METS/MODS record using {@link #MODS2SOLR}</li>
     * <li>Index it into Solr</li>
     * <li>Check that there is only a single record in the index</li>
     * <li>Retrieve the Record from Solr and return it</li>
     * </ul>
     * @param modsFile a file with a METS/MODS transformable by {@link #MODS2SOLR}.
     * @return the indexed record.
     */
    private SolrDocument singleMODSIndex(String modsFile) throws IOException {
        try {
            indexModsRecord(modsFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertEquals(1, getNumberOfTotalDocuments(),
                "After indexing '" + modsFile + "' the index should only hold a single record");
        return getRecordByDerivedId(modsFile);
    }

    private SolrDocument singlePreservicaIndex(String preservicaFile) throws Exception {
        indexPreservicaRecord(preservicaFile);
        assertEquals(1, getNumberOfTotalDocuments(),
                "After indexing '" + preservicaFile + "' the index should only hold a single record");
        return getRecordByDerivedId(preservicaFile);
    }

    private void indexModsRecord(String recordXml) throws Exception {
        String yamlStr =
                "stylesheet: '" + MODS2SOLR + "'\n" +
                        "injections:\n" +
                        "  - imageserver: 'https://example.com/imageserver/'\n" +
                        "  - old_imageserver: 'http://kb-images.kb.dk'\n" +
                        "  - origin: 'ds.test'\n";
        YAML yaml = YAML.parse(new ByteArrayInputStream(yamlStr.getBytes(StandardCharsets.UTF_8)));

        String solrString = TestUtil.getTransformedFromConfigWithAccessFields(yaml, recordXml);

        addRecordToEmbeddedServer(recordXml, solrString);
    }

    private void indexPreservicaRecord(String preservicaRecord) throws Exception {
        String solrString = TestUtil.getTransformedToSolrJsonThroughSchemaJson(PRESERVICA2SCHEMAORG, preservicaRecord);
        addRecordToEmbeddedServer(preservicaRecord, solrString);
    }

    /**
     * Adds a SolrJSON document to the embedded solr server.
     * @param recordXml     that the solrJson has been created from.
     * @param solrString    containing the solr json representation of the record.
     */
    private void addRecordToEmbeddedServer(String recordXml, String solrString) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement je = JsonParser.parseString(solrString);
        String prettyJsonString = gson.toJson(je);

        SolrInputDocument document = convertJsonToSolrJavaDoc(prettyJsonString);
        try {
            embeddedServer.add(document);
            embeddedServer.commit();
        } catch (SolrServerException e) {
            throw new IOException("Unable to add Solr document generated from '" + recordXml + "'", e);
        }
    }

    private SolrDocument getRecordById(String id) throws IOException{
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery("id:\""+id +"\"");
        solrQuery.setRows(10);
        try {
            QueryResponse rsp = embeddedServer.query(solrQuery, METHOD.POST);
            if (rsp.getResults().getNumFound() !=1) {
                throw new IOException("No record found with id '" + id + "'");
            }
            return rsp.getResults().get(0);
        } catch (SolrServerException e) {
            throw new IOException("Unable to process query '" + solrQuery.getQuery() + "'", e);
        }
    }

    private long getNumberOfTotalDocuments() throws IOException {

        // Test number of documents
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery("*:*");
        solrQuery.setRows(10);
        solrQuery.add("fl", "id");

        try {
            QueryResponse rsp = embeddedServer.query(solrQuery, METHOD.POST);
            return rsp.getResults().getNumFound();
        } catch (SolrServerException e) {
            throw new IOException("Unable to process query '" + solrQuery.getQuery() + "' for all documents", e);
        }

    }

    private void assertContentAllSingleValues(
            SolrDocument record, String filenameLocal, String catalogingLanguage,
            String shelfLocation, String catalog, String collection,
            Long fileBytesize, int imgHeight, int imgWidth) {

        assertEquals(filenameLocal,record.getFieldValue("filename_local"));
        assertEquals(catalogingLanguage,record.getFieldValue("cataloging_language"));
        assertEquals(shelfLocation,record.getFieldValue("location"));
        assertEquals(catalog, record.getFieldValue("catalog"));
        assertEquals(collection, record.getFieldValue("collection"));
        assertEquals(fileBytesize, record.getFieldValue("file_byte_size"));
        assertEquals(imgHeight, record.getFieldValue("image_height"));
        assertEquals(imgWidth, record.getFieldValue("image_width"));
    }

    private void assertMultivalueField(SolrDocument record, String fieldName, String... contentsInField) {
        Collection<Object> fieldValues = record.getFieldValues(fieldName);
        assertEquals(contentsInField.length,fieldValues.size());
        for (String s : contentsInField) {
            assertTrue(fieldValues.contains(s));
        }
    }

    // The test setup sets recordID to "ds.test:" + filename
    private SolrDocument getRecordByDerivedId(String recordFile) throws IOException {
        String recordID = "ds.test:" + Path.of(recordFile).getFileName().toString();
        return getRecordById(recordID);
    }

    private void testStringValuePreservicaField(String preservicaRecord, String solrField, String fieldValue) throws Exception {
        if (Resolver.getPathFromClasspath(preservicaRecord) == null){
           log.info("Preservica test file '{}' is not present. Embedded Solr test for field '{}'.",preservicaRecord, solrField);
           fail("Missing internal test files");
        }
        SolrDocument record = singlePreservicaIndex(preservicaRecord);
        assertEquals(fieldValue, record.getFieldValue(solrField));

    }

    private void testStringPresentInPreservicaMultiField(String preservicaRecord, String solrField, String... fieldValues) throws Exception {
        if (Resolver.getPathFromClasspath(preservicaRecord) == null) {
            log.info("Preservica test file '{}' is not present. Embedded Solr test for field '{}'",preservicaRecord, solrField);
            fail("Missing internal test files");
        }
        SolrDocument record = singlePreservicaIndex(preservicaRecord);
        for (String value:fieldValues) {
            assertTrue(record.getFieldValue(solrField).toString().contains(value));
        }
    }

    private void testDateValuePreservicaField(String preservicaRecord, String solrField, Date fieldValue) throws Exception {
        if (Resolver.getPathFromClasspath(preservicaRecord) == null){
            log.info("Preservica test file '{}' is not present. Embedded Solr test for field '{}'",preservicaRecord, solrField);
            fail("Missing internal test files");
        }
        SolrDocument record = singlePreservicaIndex(preservicaRecord);
        assertEquals(fieldValue, record.getFieldValue(solrField));
    }

    private void testLongValuePreservicaField(String preservicaRecord, String solrField, Long fieldValue) throws Exception {
        if (Resolver.getPathFromClasspath(preservicaRecord) == null){
            log.info("Preservica test file '{}' is not present. Embedded Solr test for field '{}'",preservicaRecord, solrField);
            fail("Missing internal test files");            
        }
        SolrDocument record = singlePreservicaIndex(preservicaRecord);
        assertEquals(fieldValue, record.getFieldValue(solrField));
    }

    private void testIntValuePreservicaField(String preservicaRecord, String solrField, Integer fieldValue) throws Exception {
        if (Resolver.getPathFromClasspath(preservicaRecord) == null){
            log.info("Preservica test file '{}' is not present. Embedded Solr test for field '{}'",preservicaRecord, solrField);
            fail("Missing internal test files");        
        }

        SolrDocument record = singlePreservicaIndex(preservicaRecord);
        assertEquals(fieldValue, record.getFieldValue(solrField));
    }

    private void testBooleanValuePreservicaField(String preservicaRecord, String solrField, boolean fieldValue) throws Exception {
        if (Resolver.getPathFromClasspath(preservicaRecord) == null){
            log.info("Preservica test file '{}' is not present. Embedded Solr test for field '{}'",preservicaRecord, solrField);
            fail("Missing internal test files");
        }

        SolrDocument record = singlePreservicaIndex(preservicaRecord);
        assertEquals(fieldValue, record.getFieldValue(solrField));
    }

}
