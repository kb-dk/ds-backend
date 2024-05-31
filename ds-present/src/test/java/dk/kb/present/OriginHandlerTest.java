package dk.kb.present;

import dk.kb.present.config.ServiceConfig;
import dk.kb.present.model.v1.FormatDto;
import dk.kb.util.Resolver;
import dk.kb.util.yaml.YAML;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contains some Integration tests, that will not be run by automatic build flow.
 */
class OriginHandlerTest {
    private static final Logger log = LoggerFactory.getLogger(OriginHandlerTest.class);
    private static YAML config;
    
    @BeforeAll
    static void setup() {
        try {
            ServiceConfig.initialize("test_setup.yaml");
            config = ServiceConfig.getConfig();
        } catch (IOException e) {          
            log.error("test_setup.yaml could not be loaded");            
            fail();
        }
    }
    

    @Test
    void idPattern() {
        final Pattern recordIDPattern = Pattern.compile("([a-z0-9.]+):([a-zA-Z0-9:._-]+)");
        for (String TEST: new String[]{
                "1::", // Degenerate example
                ".::_..-", // Degenerate example
                "images.dsfl:luftfoto-sample.xml",
                "images:luftfoto:sample.xml",
                "oai:kb.dk:images:luftfo:2011:maj:luftfoto:object967062"
        }) {
            assertTrue(recordIDPattern.matcher(TEST).matches());
        }

    }

    @Test
    void demoPattern() {
        final Pattern recordIDPattern = Pattern.compile("([a-z0-9.]+):([a-zA-Z0-9:._-]+)");
        String id = "images.dsfl:luftfoto-sample.xml";
        Matcher matcher = recordIDPattern.matcher(id);
        if (matcher.matches()) {
            log.debug("Collection=" + matcher.group(1) + ", material-id=" + matcher.group(2));
        } else {
            throw new IllegalArgumentException("Unsupported ID format '" + id + "'");
        }
    }

    @Test
    void normaliseID() {
        final Pattern NO_GO = Pattern.compile("[^a-zA-Z0-9:._-]");

        // Note: Not a proper id as the collection is not added
        String id = "himmel/luftfoto-sampleæøå .xml";
        for (String[] subst: new String[][]{
                {"æ", "ae"}, {"ä", "ae"}, {"Æ", "Ae"}, {"Ä", "Ae"},
                {"ø", "oe"}, {"ö", "oe"}, {"Ø", "Oe"}, {"Ö", "Oe"},
                {"å", "aa"}, {"Å", "Aa"},
                {" ", "-"}, {"/", "-"}, {"~", "-"}
        }) {
            id = id.replace(subst[0], subst[1]);
        }
        id = NO_GO.matcher(id).replaceAll(".");

        log.debug("Prepend collection and colon to this: " + id);
    }



    @Test
    void localCorpusMODS() throws IOException {
        OriginHandler ch = new OriginHandler(config);
        String record = ch.getRecord("local.mods:40221e30-1414-11e9-8fb8-00505688346e.xml", FormatDto.MODS);
        assertTrue(record.contains("<mods:title>Christian VIII</mods:title>"));
    }

    @Test
    @Tag("integration")
    void localCorpusPvica() throws IOException {
        if (Resolver.getPathFromClasspath("internal_test_files/preservica7/9d9785a8-71f4-4b34-9a0e-1c99c13b001b.xml") == null){
            log.error("Preservica test file is not present. Test for file 9d9785a8-71f4-4b34-9a0e-1c99c13b001b.xml");
            fail();
        }                
        OriginHandler ch = new OriginHandler(config);
        String record = ch.getRecord("local.radio:9d9785a8-71f4-4b34-9a0e-1c99c13b001b.xml", FormatDto.JSON_LD);
        assertTrue(record.contains("\"id\":\"local.radio:9d9785a8-71f4-4b34-9a0e-1c99c13b001b.xml\""));
    }

    @Test
    @Tag("integration")
    void testManifestationFiltering() throws IOException {
        if (Resolver.getPathFromClasspath("internal_test_files/preservica7/9d9785a8-71f4-4b34-9a0e-1c99c13b001b.xml") == null){
            log.info("Preservica test file is not present. Test for file 9d9785a8-71f4-4b34-9a0e-1c99c13b001b.xml");
            fail();
        }
        // This test checks that the correct filtering is applied in DSOrigin.getFirstChild()
        // The FileStorage used for testing appends two children to each record. One with referenceType = 1 and one with
        // referenceType = 2. Only children with type = 2 should be returned as these are presentation manifestations.
        OriginHandler ch = new OriginHandler(config);
        String record = ch.getRecord("local.tv:9d9785a8-71f4-4b34-9a0e-1c99c13b001b.xml", FormatDto.JSON_LD);
        assertTrue(record.contains("correct-reference\\/playlist.m3u8"));
        assertFalse(record.contains("wrong-reference\\/playlist.m3u8"));
    }


    @Test
    void localCorpusFail() throws IOException {        
        OriginHandler ch = new OriginHandler(config);
        try {
            ch.getRecord("local.radio:40221e30-1414-11e9-8fb8-00505688346e.xml", FormatDto.RAW);
            fail("Requesting record in raw format should fail");
        } catch (Exception e) {
            // Expected
        }
    }

}