package dk.kb.present.solr;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dk.kb.present.TestUtil;
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
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class EmbeddedSolrTest {

    private static final Logger log = LoggerFactory.getLogger(EmbeddedSolrTest.class);
    private static final String solr_home = "target/test-classes/solr";

    private static CoreContainer coreContainer = null;
    private static EmbeddedSolrServer embeddedServer = null;

    public static final String MODS2SOLR = "xslt/mods2solr.xsl";
    public static final String PRESERVICA2SOLR = "xslt/preservica2solr.xsl";
    public static final String MODS_RECORD_05fea810 = "xml/copyright_extraction/05fea810-7181-11e0-82d7-002185371280.xml";
    public static final String MODS_RECORD_3956d820 = "xml/copyright_extraction/3956d820-7b7d-11e6-b2b3-0016357f605f.xml";
    public static final String MODS_RECORD_096c9090 = "xml/copyright_extraction/096c9090-717f-11e0-82d7-002185371280.xml";
    public static final String MODS_RECORD_aaf3b130 = "xml/copyright_extraction/aaf3b130-e6e7-11e6-bdbe-00505688346e.xml";
    public static final String MODS_RECORD_54b34b50 = "xml/copyright_extraction/54b34b50-2ce6-11ed-81b4-005056882ec3.xml";
    public static final String MODS_RECORD_8e608940 = "xml/copyright_extraction/8e608940-d6db-11e3-8d2e-0016357f605f.xml";
    public static final String MODS_RECORD_ANSK = "xml/copyright_extraction/ANSK_11614.tif.xml";
    public static final String MODS_RECORD_e2519ce0 = "xml/copyright_extraction/e2519ce0-9fb0-11e8-8891-00505688346e.xml";
    public static final String MODS_RECORD_FM = "xml/copyright_extraction/FM103703H.tif.xml";
    public static final String MODS_RECORD_DB_hans = "xml/copyright_extraction/25461fb0-f664-11e0-9d29-0016357f605f.xml";
    public static final String MODS_RECORD_770379f0 = "xml/copyright_extraction/770379f0-8a0d-11e1-805f-0016357f605f.xml";
    public static final String MODS_RECORD_40221e30 = "xml/copyright_extraction/40221e30-1414-11e9-8fb8-00505688346e.xml";
    public static final String MODS_RECORD_0c02aa10 = "xml/copyright_extraction/0c02aa10-b657-11e6-aedf-00505688346e.xml";
    public static final String MODS_RECORD_9c17a440 = "xml/copyright_extraction/9c17a440-fe1a-11e8-9044-00505688346e.xml";
    public static final String MODS_RECORD_226d41a0 = "xml/copyright_extraction/226d41a0-5a83-11e6-8b8d-0016357f605f.xml";
    public static final String PRESERVICA_RECORD_44979f67 = "internal_test_files/tvMetadata/44979f67-b563-462e-9bf1-c970167a5c5f.xml";
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
        SolrDocument record = singleMODSIndex(MODS_RECORD_05fea810);

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
        SolrDocument record = singleMODSIndex(MODS_RECORD_3956d820);

        assertContentAllSingleValues(record, "DPK000107.tif", "da",
                "Billedsamlingen. Postkortsamlingen, Vestindien, Sankt Thomas, Charlotte Amalie, Det gamle fort/politistation",
                "Postkortsamlingen, Vestindien, Postkort, Vestindien, CAR- BLO katagori, Postkortsamlingen, 2022-09-01 15:06:39, 2022-09-01 15:11:09",
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
        SolrDocument record = singleMODSIndex(MODS_RECORD_096c9090);

        assertContentAllSingleValues(record,"000225.tif", "da",
                "Billedsamlingen. Danske portrætter, 4°, Egede, Poul (1708-1789)",
                "Danske portrætter, X-langtidsbevaring test - BLO, Diverse, 2022-09-01 15:06:39, 2022-09-01 15:11:09, 2022-09-02 09:01:13",
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
        SolrDocument record = singleMODSIndex(MODS_RECORD_aaf3b130);

        //Single value field
        assertEquals("DT005031.tif",record.getFieldValue("filename_local"));

		assertEquals("Billede, Todimensionalt billedmateriale", record.getFieldValue("resource_description_general"));
	}

    @Test
    void testRecordANSK() throws IOException {
        SolrDocument record = singleMODSIndex(MODS_RECORD_ANSK);

        //Single value field
        assertEquals("ANSK_11614.tif",record.getFieldValue("filename_local"));
		assertEquals("Billede, Todimensionalt billedmateriale", record.getFieldValue("resource_description_general"));
        assertNull(record.getFieldValue("notes_length"));
	}

    @Test
    void testRecordSkfF0137() throws IOException {
        SolrDocument record = singleMODSIndex(MODS_RECORD_54b34b50);

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
        SolrDocument record = singleMODSIndex(MODS_RECORD_8e608940);

        //Single value field
        assertEquals("KHP0001-049.tif",record.getFieldValue("filename_local"));
        assertEquals("Billede, Todimensionalt billedmateriale", record.getFieldValue("resource_description_general"));

        //multivalue field
		assertMultivalueField(record, "list_of_categories", "KHP",
				"Keld Helmer-Petersen", "1940-1950", "Helmer-Petersen", "Keld",
				"CAR- BLO katagori", "ikke UA");
	}

    @Test
    void testRecordUldallForTitleAndPlaceOfProductionAndGenre() throws IOException {
        SolrDocument record = singleMODSIndex(MODS_RECORD_e2519ce0);

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
        SolrDocument record = singleMODSIndex(MODS_RECORD_FM);

        //Single value field
        assertEquals("2000-3/7",record.getFieldValue("accession_number"));
    }

    @Test
    void testPublishedInAndCollection() throws IOException {
        SolrDocument record = singleMODSIndex(MODS_RECORD_DB_hans);

        //Single value field
        assertEquals("Bladtegnersamlingen",record.getFieldValue("collection"));
        assertEquals("Aktuelt", record.getFieldValue("published_in"));
    }

    @Test
    void testTitle() throws IOException {
        SolrDocument record = singleMODSIndex(MODS_RECORD_770379f0);

        assertMultivalueField(record, "title", "Romeo og Julie");
        assertEquals(1, record.getFieldValue("title_count"));
    }

    @Test
    void testResourceId() throws IOException {
        SolrDocument record = singleMODSIndex(MODS_RECORD_40221e30);

        //Single value field
        assertMultivalueField(record, "resource_id", "/DAMJP2/DAM/Samlingsbilleder/0000/624/420/KE070592");
    }

    @Test
    void testMapScale() throws IOException {
        SolrDocument record = singleMODSIndex(MODS_RECORD_0c02aa10);

        //Single value field
        assertEquals("Målestok 1:75 000",record.getFieldValue("map_scale"));
    }

    @Test
    void testSubjectStrict() throws IOException {
        SolrDocument record = singleMODSIndex(MODS_RECORD_9c17a440);

        String[] testName = Arrays.copyOf(
                record.getFieldValues("subject_full_name_strict").toArray(),
                1, String[].class);

        String[] correctResult = new String[]{"Frederik 9"};
        assertEquals(correctResult[0], testName[0]);
    }

    @Test
    void testSingleProductionDate() throws IOException{
        SolrDocument record = singleMODSIndex(MODS_RECORD_226d41a0);

        assertEquals("1971", record.getFieldValue("production_date"));
    }

    @Test
    void testCountsCreatedBySolr() throws IOException {
        SolrDocument record = singleMODSIndex(MODS_RECORD_05fea810);

        assertEquals(3, record.getFieldValue("creator_count"));
        assertEquals(6, record.getFieldValue("topic_count"));
        assertEquals(1, record.getFieldValue("subject_count"));
        assertEquals(1, record.getFieldValue("notes_count"));
        assertEquals(3, record.getFieldValue("categories_count"));
    }

    @Test
    void testNotesLength() throws IOException {
        SolrDocument record = singleMODSIndex(MODS_RECORD_DB_hans);

        assertEquals(93, record.getFieldValue("notes_length"));
    }

    @Test
    void testPreservicaPremiere() throws Exception {
        SolrDocument record = singlePreservicaIndex(PRESERVICA_RECORD_44979f67);
        assertFalse((Boolean) record.getFieldValue("premiere"));
    }

    @Test
    void testPreservicaDuration() throws Exception {
        SolrDocument record = singlePreservicaIndex(PRESERVICA_RECORD_44979f67);
        assertEquals(950000L,  record.getFieldValue("duration_ms"));
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
        indexModsRecord(modsFile);
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

    private void indexModsRecord(String recordXml) throws IOException {
        String solrString = TestUtil.getTransformedWithAccessFieldsAdded(MODS2SOLR, recordXml);

        addRecordToEmbeddedServer(recordXml, solrString);
    }

    private void indexPreservicaRecord(String preservicaRecord) throws Exception {
        String solrString = TestUtil.getTransformedWithAccessFieldsAdded(PRESERVICA2SOLR, preservicaRecord);
        //TestUtil.prettyPrintSolrJsonFromMetadata(PRESERVICA2SOLR, preservicaRecord);
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
            SolrDocument record, String filenameLocal, String catalogingLanguage, String shelfLocation,
            String categories, String catalog, String collection,
            Long fileBytesize, int imgHeight, int imgWidth) {

        assertEquals(filenameLocal,record.getFieldValue("filename_local"));
        assertEquals(catalogingLanguage,record.getFieldValue("cataloging_language"));
        assertEquals(shelfLocation,record.getFieldValue("location"));
        assertEquals(categories,record.getFieldValue("categories"));
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

}
