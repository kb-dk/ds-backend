package dk.kb.present.util;

import dk.kb.util.Resolver;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ExtractedPreservicaValuesTest {

    @Test
    void extractValuesFromPreservicaContent_whenDrRadioWithDrArchiveSupplementaryRightsMetadata_thenExtractedPreservicaValuesIsPopulated() throws IOException, ParserConfigurationException, SAXException {
        // Arrange
        String recordId = "83191087-69b3-4f46-ab64-f230d971def2";
        String xml = Resolver.resolveUTF8String("internal_test_files/preservica7/dr_archive_supplementary_rights_metadata/dsradio/" + recordId + ".xml");

        // Act
        ExtractedPreservicaValues extractedPreservicaValues = ExtractedPreservicaValues.extractValuesFromPreservicaContent(xml, recordId);

        // Assert
        assertEquals(recordId, extractedPreservicaValues.getId());
        assertEquals("På genhør", extractedPreservicaValues.getTitle());
        assertEquals("", extractedPreservicaValues.getOriginalTitle());
        assertEquals("1990-01-22T10:55:00Z", extractedPreservicaValues.getStartTime());
        assertEquals("1990-01-22T11:00:00Z", extractedPreservicaValues.getEndTime());
        assertNull(extractedPreservicaValues.getFormValue());
        assertNull(extractedPreservicaValues.getContent());
        assertNull(extractedPreservicaValues.getPurpose());
        assertEquals("11109009013", extractedPreservicaValues.getProductionId());
        assertNull(extractedPreservicaValues.getOrigin());
        assertNull(extractedPreservicaValues.getOriginCountry());
        assertNull(extractedPreservicaValues.getHoldbackCategory());
    }

    @Test
    void extractValuesFromPreservicaContent_whenDrTvWithNielsenAndDrArchiveSupplementaryRightsMetadata_thenExtractedPreservicaValuesIsPopulated() throws IOException, ParserConfigurationException, SAXException {
        // Arrange
        String recordId = "183c4b3e-e549-40d7-8861-9ec61c229723";
        String xml = Resolver.resolveUTF8String("internal_test_files/homemade/dr_archive_supplementary_rights_metadata/dstv/tidNielsen.xml");

        // Act
        ExtractedPreservicaValues extractedPreservicaValues = ExtractedPreservicaValues.extractValuesFromPreservicaContent(xml, recordId);

        // Assert
        assertEquals(recordId, extractedPreservicaValues.getId());
        assertEquals("Søren Ryge: At være god til at løbe", extractedPreservicaValues.getTitle());
        assertEquals("Søren Ryge: At være god til at løbe", extractedPreservicaValues.getOriginalTitle());
        assertEquals("2022-02-28T05:30:07Z", extractedPreservicaValues.getStartTime());
        assertEquals("2022-02-28T05:58:01Z", extractedPreservicaValues.getEndTime());
        assertEquals("0000", extractedPreservicaValues.getFormValue());
        assertEquals("0000", extractedPreservicaValues.getContent());
        assertEquals("0000", extractedPreservicaValues.getPurpose());
        assertEquals("00000000000", extractedPreservicaValues.getProductionId());
        assertEquals("0000", extractedPreservicaValues.getOrigin());
        assertEquals("0000", extractedPreservicaValues.getOriginCountry());
        assertEquals("Underholdning", extractedPreservicaValues.getHoldbackCategory());
    }

    @Test
    void extractValuesFromPreservicaContent_whenDrTvWithTvmeterAndDrArchiveSupplementaryRightsMetadata_thenExtractedPreservicaValuesIsPopulated() throws IOException, ParserConfigurationException, SAXException {
        // Arrange
        String recordId = "710b6163-59f0-403b-a18f-ae25c2fa6600";
        String xml = Resolver.resolveUTF8String("internal_test_files/homemade/dr_archive_supplementary_rights_metadata/dstv/tidTvmeter.xml");

        // Act
        ExtractedPreservicaValues extractedPreservicaValues = ExtractedPreservicaValues.extractValuesFromPreservicaContent(xml, recordId);

        // Assert
        assertEquals(recordId, extractedPreservicaValues.getId());
        assertEquals("Kender du typen? - med bumser og Bond", extractedPreservicaValues.getTitle());
        assertEquals("Kender du typen? - 2018", extractedPreservicaValues.getOriginalTitle());
        assertEquals("2018-04-03T00:15:07Z", extractedPreservicaValues.getStartTime());
        assertEquals("2018-04-03T00:58:01Z", extractedPreservicaValues.getEndTime());
        assertEquals("0000", extractedPreservicaValues.getFormValue());
        assertEquals("0000", extractedPreservicaValues.getContent());
        assertEquals("0000", extractedPreservicaValues.getPurpose());
        assertEquals("00000000000", extractedPreservicaValues.getProductionId());
        assertEquals("0000", extractedPreservicaValues.getOrigin());
        assertEquals("0000", extractedPreservicaValues.getOriginCountry());
        assertEquals("Underholdning", extractedPreservicaValues.getHoldbackCategory());
    }

    @Test
    void extractValuesFromPreservicaContent_whenDrTvWithTvmeterAndNullValuesInDrArchiveSupplementaryRightsMetadata_thenExtractedPreservicaValuesIsPopulated() throws IOException, ParserConfigurationException, SAXException {
        // Arrange
        String recordId = "710b6163-59f0-403b-a18f-ae25c2fa6600";
        String xml = Resolver.resolveUTF8String("internal_test_files/homemade/dr_archive_supplementary_rights_metadata/dstv/tidNullValues.xml");

        // Act
        ExtractedPreservicaValues extractedPreservicaValues = ExtractedPreservicaValues.extractValuesFromPreservicaContent(xml, recordId);

        // Assert
        assertEquals(recordId, extractedPreservicaValues.getId());
        assertEquals("Kender du typen? - med bumser og Bond", extractedPreservicaValues.getTitle());
        assertEquals("Kender du typen? - 2018", extractedPreservicaValues.getOriginalTitle());
        assertEquals("2018-04-03T00:15:07Z", extractedPreservicaValues.getStartTime());
        assertEquals("2018-04-03T00:58:01Z", extractedPreservicaValues.getEndTime());
        assertEquals("1800", extractedPreservicaValues.getFormValue());
        assertEquals("2790", extractedPreservicaValues.getContent());
        assertEquals("1000", extractedPreservicaValues.getPurpose());
        assertEquals("9518360400", extractedPreservicaValues.getProductionId());
        assertEquals("1000", extractedPreservicaValues.getOrigin());
        assertEquals("1000", extractedPreservicaValues.getOriginCountry());
        assertNull(extractedPreservicaValues.getHoldbackCategory());
    }

    @Test
    void extractValuesFromPreservicaContentWithTVMeterContent() throws IOException, ParserConfigurationException, SAXException {
        String xml = Resolver.resolveUTF8String("internal_test_files/preservica7/cb1930d6-4ae0-41c7-a4c8-0a4bc235175a.xml");
        ExtractedPreservicaValues extractedPreservicaValues = ExtractedPreservicaValues.extractValuesFromPreservicaContent(xml, "cb1930d6-4ae0-41c7-a4c8-0a4bc235175a");
        assertEquals("cb1930d6-4ae0-41c7-a4c8-0a4bc235175a", extractedPreservicaValues.getId());
        assertEquals("I hegnet", extractedPreservicaValues.getTitle());
        assertEquals("I hegnet (6:6)", extractedPreservicaValues.getOriginalTitle());
        assertEquals("2013-08-16T17:34:37Z", extractedPreservicaValues.getStartTime());
        assertEquals("2013-08-16T17:57:41Z", extractedPreservicaValues.getEndTime());
        assertEquals("4000", extractedPreservicaValues.getFormValue());
        assertEquals("3140", extractedPreservicaValues.getContent());
        assertEquals("4000", extractedPreservicaValues.getPurpose());
        assertEquals("5912040600", extractedPreservicaValues.getProductionId());
        assertEquals("1000", extractedPreservicaValues.getOrigin());
        assertEquals("1000", extractedPreservicaValues.getOriginCountry());
        assertNull(extractedPreservicaValues.getHoldbackCategory());
    }

    @Test
    void extractValuesFromPreservicaContentWithNielsenData() throws IOException, ParserConfigurationException, SAXException {
        String xml = Resolver.resolveUTF8String("internal_test_files/preservica7/da77d411-3a7d-4f05-8ad1-05a19538f668.xml");
        ExtractedPreservicaValues extractedPreservicaValues = ExtractedPreservicaValues.extractValuesFromPreservicaContent(xml, "da77d411-3a7d-4f05-8ad1-05a19538f668");
        assertEquals("da77d411-3a7d-4f05-8ad1-05a19538f668", extractedPreservicaValues.getId());
        assertEquals("Dyrenes sang - en truet lyd", extractedPreservicaValues.getTitle());
        assertEquals("Attenborough's Wonder of Song", extractedPreservicaValues.getOriginalTitle());
        assertEquals("2022-02-27T17:59:21Z", extractedPreservicaValues.getStartTime());
        assertEquals("2022-02-27T18:56:54Z", extractedPreservicaValues.getEndTime());
        assertEquals("1300", extractedPreservicaValues.getFormValue());
        assertEquals("2110", extractedPreservicaValues.getContent());
        assertEquals("1000", extractedPreservicaValues.getPurpose());
        assertEquals("0221126300", extractedPreservicaValues.getProductionId());
        assertEquals("4000", extractedPreservicaValues.getOrigin());
        assertEquals("2211", extractedPreservicaValues.getOriginCountry());
        assertNull(extractedPreservicaValues.getHoldbackCategory());
    }

}
