package dk.kb.present.util;

import dk.kb.util.Resolver;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ExtractedPreservicaValuesTest {

    @Test
    void extractValuesFromPreservicaContentWithTVMeterContent() throws IOException, ParserConfigurationException, SAXException {
        String xml = Resolver.resolveUTF8String("internal_test_files/preservica7/cb1930d6-4ae0-41c7-a4c8-0a4bc235175a.xml");
        ExtractedPreservicaValues extractedPreservicaValues = ExtractedPreservicaValues.extractValuesFromPreservicaContent(xml, "cb1930d6-4ae0-41c7-a4c8-0a4bc235175a");
        assertEquals("cb1930d6-4ae0-41c7-a4c8-0a4bc235175a", extractedPreservicaValues.getId());
        assertEquals("I hegnet", extractedPreservicaValues.getTitle());
        assertEquals("2013-08-16T17:34:37Z", extractedPreservicaValues.getStartTime());
        assertEquals("2013-08-16T17:57:41Z", extractedPreservicaValues.getEndTime());
        assertEquals("4000", extractedPreservicaValues.getFormValue());
        assertEquals("3140", extractedPreservicaValues.getContent());
        assertEquals("4000", extractedPreservicaValues.getPurpose());
        assertEquals("5912040600", extractedPreservicaValues.getProductionId());
        assertEquals("1000", extractedPreservicaValues.getOrigin());
        assertEquals("1000", extractedPreservicaValues.getOriginCountry());
    }

    @Test
    void extractValuesFromPreservicaContentWithNielsenData() throws IOException, ParserConfigurationException, SAXException {
        String xml = Resolver.resolveUTF8String("internal_test_files/preservica7/da77d411-3a7d-4f05-8ad1-05a19538f668.xml");
        ExtractedPreservicaValues extractedPreservicaValues = ExtractedPreservicaValues.extractValuesFromPreservicaContent(xml, "da77d411-3a7d-4f05-8ad1-05a19538f668");
        assertEquals("da77d411-3a7d-4f05-8ad1-05a19538f668", extractedPreservicaValues.getId());
        assertEquals("Dyrenes sang - en truet lyd", extractedPreservicaValues.getTitle());
        assertEquals("2022-02-27T17:59:21Z", extractedPreservicaValues.getStartTime());
        assertEquals("2022-02-27T18:56:54Z", extractedPreservicaValues.getEndTime());
        assertEquals("1300", extractedPreservicaValues.getFormValue());
        assertEquals("2110", extractedPreservicaValues.getContent());
        assertEquals("1000", extractedPreservicaValues.getPurpose());
        assertEquals("0221126300", extractedPreservicaValues.getProductionId());
        assertEquals("4000", extractedPreservicaValues.getOrigin());
        assertEquals("2211", extractedPreservicaValues.getOriginCountry());
    }

}
