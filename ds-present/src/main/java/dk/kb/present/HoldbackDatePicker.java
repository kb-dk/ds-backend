package dk.kb.present;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import dk.kb.present.config.ServiceConfig;
import dk.kb.present.util.saxhandlers.ElementExtractionHandler;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.util.DatetimeParser;
import dk.kb.util.MalformedIOException;
import dk.kb.util.Resolver;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.NotFoundServiceException;
import dk.kb.util.yaml.YAML;
import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Material from DR can be subject to holdback rules. Which means that resources can only be shown in the archive after
 * X amount of days have past since the program aired. Calculation of these holdback dates are done in this class.
 * Beware, the calculations are neither logical nor straightforward. Every method in this class should therefore
 * include documentation.
 * <p/>
 * The calculation of holdback dates is made from data in the Preservica record, which is then compared to values in
 * spreadsheets from DR. At first the PurposeID and PurposeName for a program needs to be calculated. This is done by
 * extracting Form and Content from records and then doing the following:
 * <ol>
 *     <li>Extract FormNr by looking up Form in {@link #formIndexSheet}</li>
 *     <li>Extract ContentNumber by looking up Content in {@link #purposeMatrixSheet} and then extracting the value from FormNr from above</li>
 *     <li>Lookup PurposeName by finding ContentNumber in {@link #purposeSheet}</li>
 * </ol>
 * </p>
 * When the correct purpose has been extracted, the amount of holdback days needs to be found for the specific purpose.
 * This is done by looking up the PurposeName in the dr_holdback Excel sheet.
 */
public class HoldbackDatePicker {
    private static final Logger log = LoggerFactory.getLogger(HoldbackDatePicker.class);

    private static HoldbackDatePicker datePicker = new HoldbackDatePicker();

    /**
     * Excel sheet containing table to find FormNr in based on Form from the XML.
     */
    private static XSSFSheet formIndexSheet;

    /**
     * Excel sheet containing table to find PurposeID in based on Content and FormNr.
     */
    private static XSSFSheet purposeMatrixSheet;
    /**
     * Excel sheet containing table to find PurposeName in based on PurposeID
     */
    private static XSSFSheet purposeSheet;

    /**
     * Excel sheet containing table to find holdback days for a given purpose.
     */
    private static XSSFSheet holdbackSheet;

    private static SAXParserFactory factory;

    HoldbackDatePicker() {}

    public static void init(){
        factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(false);
        readSheet();
    }

    public static synchronized HoldbackDatePicker getInstance(){
        return datePicker;
    }

    /**
     * Apply the HoldbackDatePicker to a string of XML representing a Preservica Information Object.
     *
     * @param record DsStorage record containing an InformationObject from Preservica encapsulating a DR record/program,
     *              which holdback needs to be calculated for.
     * @return a string containing the date for when the holdback for the record has expired.
     *         In the format: yyyy-MM-ddTHH:mm:ssZ
     */
    public HoldbackDTO getHoldbackDateForRecord(DsRecordDto record) throws IOException {
        HoldbackDTO result = new HoldbackDTO();
        if (record.getOrigin() == null){
            log.error("Origin was null. Holdback cannot be calculated for records that are not from origins 'ds.radio' or 'ds.tv'. Returning a result object without values.");
            result.setHoldbackPurposeName("");
            result.setHoldbackDate("");
            return result;
        }

        if (record.getOrigin().equals("ds.tv")){
            return getHoldbackForTvRecord(record, result);
        } else if (record.getOrigin().equals("ds.radio")) {
            return getHoldbackForRadioRecord(record, result);
        } else {
            log.error("Holdback cannot be calculated for records that are not from origins 'ds.radio' or 'ds.tv'." +
                    " Returning a result object without values.");
            throw new InternalServiceException("Holdback cannot be calculated for records that are not from origins 'ds.radio' or 'ds.tv'." +
                    " Returning a result object without values.");
        }

    }

    /**
     * Calculate holdback for a Radio record by adding 3 years to its aired time. This value has been requested/defined by DR.
     * @param record to calculate holdback for
     * @param result holdbackDTO object, which the calculated holdback date is added to. This will never contain a value
     *               for {@code holdbackPurposeName} as these are not present for radio records.
     *
     * @return the updated HoldbackDTO.
     */
    private HoldbackDTO getHoldbackForRadioRecord(DsRecordDto record, HoldbackDTO result) throws IOException {
        if (record.getData() == null){
            throw new RuntimeException("Record with ID: '" + record.getId() + "' does not contain data");
        }
        try (InputStream xmlStream = IOUtils.toInputStream(record.getData(), StandardCharsets.UTF_8)) {
            // Radio should be held back by three years by DR request.
            result.setHoldbackDate(addHoldbackDaysToRecordStartDate(xmlStream, 1096));
            result.setHoldbackPurposeName("");
            return result;
        } catch (ParserConfigurationException | SAXException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Calculate holdback for a TV record by comparing values in the record to the DR provided schemas.
     * @param record to calculate holdback for.
     * @param result holdbackDTO containing the purposeName and the holdbackDate for a record.
     * @return the result object with updated values.
     */
    private static HoldbackDTO getHoldbackForTvRecord(DsRecordDto record, HoldbackDTO result) throws IOException {
        if (record.getData() == null){
            throw new RuntimeException("Record with ID: '" + record.getId() + "' does not contain data");
        }

        try (InputStream xmlStream = IOUtils.toInputStream(record.getData(), StandardCharsets.UTF_8)) {
            try {
                result.setHoldbackPurposeName(getPurposeName(xmlStream));

                if (result.getHoldbackPurposeName().isEmpty()){
                    log.warn("Purpose name was empty. Setting holdback date to 9999-01-01T00:00:00Z");
                    result.setHoldbackDate("9999-01-01T00:00:00Z");
                } else {
                    int holdbackDays = getHoldbackDaysForPurpose(result.getHoldbackPurposeName());
                    result.setHoldbackDate(addHoldbackDaysToRecordStartDate(xmlStream, holdbackDays));
                }


                return result;
            } catch (IOException | SAXException | ParserConfigurationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Extract the start date from a Preservica record and add the input holdbackDays to the date to create the date,
     * when the holdback period for the record has expired.
     * @param xmlStream containing a preservica record with a dateAvailableStart value in the PB Core metadata.
     * @param holdbackDays amount of days to be added to the start date
     * @return the calculated date for when holdback expires for the record. In the format: yyyy-MM-dd'T'HH:mm:ssZ
     */
    private static String addHoldbackDaysToRecordStartDate(InputStream xmlStream, int holdbackDays) throws ParserConfigurationException, IOException, SAXException {
        ZonedDateTime startDate = getStartDate(xmlStream);
        return calculateHoldbackDate(startDate, holdbackDays);
    }

    /**
     * Apply the amount of holdback days to the start date and return the date for when holdback has expired.
     * @param startDate a date representing the date when a program was broadcast.
     * @param holdbackDays the amount of days that has to parse before a program can be retrieved in the archive.
     * @return the date, when the holdback period has expired as a string in the format: yyyy-MM-dd'T'HH:mm:ssZ.
     */
    private static String calculateHoldbackDate(ZonedDateTime startDate, int holdbackDays) {
        ZonedDateTime holdbackExpiredDate = startDate.plusDays(holdbackDays);

        // Using .ISO_INSTANT as this is solr standard
        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;

        String formattedHoldbackDate = holdbackExpiredDate.format(formatter);
        return formattedHoldbackDate;
    }

    /**
     * From a purposeName, get the amount of days which the program should be held back.
     * This is looked up in the DR provided holdback sheet.
     * @param purpose purposeName from {@link #purposeSheet} to lookup.
     * @return amount of holdback days.
     */
    private static int getHoldbackDaysForPurpose(String purpose) {
        for (Row row : holdbackSheet) {
            // Check the value against the 2019 and 2022 values.
            if (purpose.equals(row.getCell(2).getStringCellValue()) || purpose.equals(row.getCell(0).getStringCellValue() )){
                return (int) row.getCell(4).getNumericCellValue();
            }
        }

        log.error("No holdback has been defined for purpose: '{}'.", purpose);
        throw new NotFoundServiceException("No holdback value cold be found for purpose: '" + purpose + "' in holdbackSheet: '" + holdbackSheet.getSheetName() + "' ");
    }

    /**
     * Get purposeName for a preservica record containing metadata about a DR program.
     * @param xmlStream containing the preservica record for analysis.
     * @return the purposeName for a given program.
     */
    private static String getPurposeName(InputStream xmlStream) throws IOException, ParserConfigurationException, SAXException {
        // Get form value
        String form = getFormValue(xmlStream);

        // get formNr by looking up form in formIndexSheet.
        int formNr = getFormNrFromForm(form);
        String formString = createFormString(formNr);

        // TODO: do some logic on gallup/nielsen differences.
        // get contentsitem from xml
        String contentsItem = getContentsItem(xmlStream);

        // Slå Indhold op i IndholdFra-IndholdTil i matrice i FormNr kolonne.
        String purposeNumber = getPurposeIdFromContentAndForm(contentsItem, formString);
        purposeNumber = validatePurpose(purposeNumber, xmlStream);

        // Brug den fundne værdi i formåls arket til at finde formålNavn
        return getPurposeNameFromNumber(purposeNumber);
    }

    /**
     * Extract purposeName from a given purposeID in {@link #purposeSheet}.
     * @param purposeID which maps to a textual name in the purposeSheet.
     * @return the purposeName for the input ID.
     */
    private static String getPurposeNameFromNumber(String purposeID) {
        if (purposeID.isEmpty()){
            log.warn("PurposeID is empty. Returning an empty string as PurposeName.");
            return "";
        }
        for (Row row : purposeSheet) {
            if (row.getCell(1).getStringCellValue().equals(purposeID)) {
                return row.getCell(2).getStringCellValue();
            }
        }

        log.warn("No purposeName could be found for PurposeID: '{}'", purposeID);
        return "";
    }

    /**
     * Validate and handle special cases of IDs constructed by {@link #getPurposeIdFromContentAndForm(String, String)}.
     * @param purposeId which is to be validated.
     * @param xml of the preservica record. Needs to be included here when validating against values from it.
     * @return an updated PurposeID
     */
    private static String validatePurpose(String purposeId, InputStream xml) throws ParserConfigurationException, IOException, SAXException {
        // Handling of special case for purposeID 2.05, where country of production is needed to create the correct value.
        if (purposeId.equals("2.05")) {
            String productionCountry = getProductionCountry(xml);
            if (productionCountry.equals("1000")) {
                return purposeId + ".01";
            } else {
                return purposeId + ".02";
            }
        } else {
            return purposeId;
        }
    }

    /**
     * Get PurposeID from provided Content and FormNr.
     *
     * @param content a four-digit number extracted from the XML, which holdback is being calculated for.
     * @param formNrString a string of the format: 'FormX' where x has been resolved through {@link #getFormNrFromForm(String)}
     * @return a PurposeID most likely in the format x.xx: An example could be the string '2.02'.
     */
    private static String getPurposeIdFromContentAndForm(String content, String formNrString) {
        if (content.isEmpty()){
            log.warn("The field 'content' is empty. PurposeID can't be calculated. Returning an empty string.");
            return "";
        }

        double contentDouble = Double.parseDouble(content);
        for (Row row : purposeMatrixSheet) {
            Cell indholdFra = row.getCell(1);
            Cell indholdTil = row.getCell(2);
            if (indholdFra != null && indholdFra.getCellType() == CellType.NUMERIC) {
                double indholdFraValue = indholdFra.getNumericCellValue();
                double indholdTilValue = indholdTil.getNumericCellValue();
                if (contentDouble >= indholdFraValue && contentDouble <= indholdTilValue) {
                    log.info("IndholdFra and IndholdTil matches in row: '{}'", row.getRowNum());

                    for (Cell formCell : purposeMatrixSheet.getRow(0)) {
                        if (formCell.getStringCellValue().equals(formNrString)) {
                            log.info("Found the correct formNrString, which is: '{}'", formCell.getStringCellValue());
                            log.info("Extracting Purpose Number from this cell, which is: '{}'", row.getCell(formCell.getColumnIndex()));
                            return String.valueOf(row.getCell(formCell.getColumnIndex()));
                        }

                    }
                }
            }
        }

        log.warn("No PurposeID could be extracted from content: '{}' and form: '{}'", content, formNrString);
        return "";
    }

    /**
     * Prefix the input formNr with the literal string: "Form". This is done because the constructed formString is needed
     * when looking up PurposeID in {@link #purposeMatrixSheet}.
     * @param formNr to prefix with "Form"
     * @return a formString of the format Form+formNr eg. Form1, Form2 etc.
     */
    private static String createFormString(int formNr) {
        return "Form" + formNr;
    }

    /**
     * Extract FormNr from {@link #formIndexSheet} based on the value of form, which is a four-digit number.
     * @param form four-digit number extracted from a preservica record.
     * @return the corresponding FormNr for the input form. Returns zero if no value has been extracted.
     */
    private static int getFormNrFromForm(String form) {
        if (form.isEmpty()){
            log.warn("No FormNr could be extracted as form string is empty. Returning 0.");
            return 0;
        }

        double formDouble = Double.parseDouble(form);
        if (formDouble < 1000 || formDouble > 7000){
            log.warn("The Form value extracted was out of range 1000-7000. This does not map to a FormNr. Form was: '{}'", formDouble);
        }

        for (Row row : formIndexSheet) {
            // Value in FormFra column
            Cell formFra = row.getCell(1);
            // Value in FormTil column
            Cell formTil = row.getCell(2);
            if (formFra != null && formFra.getCellType() == CellType.NUMERIC) {
                double formFraValue = formFra.getNumericCellValue();
                double formTilValue = formTil.getNumericCellValue();
                // If form is between FormFra and FormTil (both inclusive) the FormNr is matching and extracted.
                if (formDouble >= formFraValue && formDouble <= formTilValue) {
                    return (int) row.getCell(0).getNumericCellValue();
                }
            }
        }

        log.warn("No FormNr could be extracted for form: '{}'. Returning 0.", form);
        return 0;
    }

    /**
     * Setup of the HoldbackDatePicker. Fetches the Excel sheets for {@link #formIndexSheet}, {@link #purposeMatrixSheet},
     * {@link #purposeSheet} and {@link #holdbackSheet}.
     */
    private static void readSheet() {
        try {
            String purposeSheetPath = ServiceConfig.getConfig().getString("holdback.dr.purposeSheet");
            XSSFWorkbook purposeWorkbook;
            try (FileInputStream purposeExcel = new FileInputStream(Resolver.resolveURL(purposeSheetPath).getPath())) {
                purposeWorkbook = new XSSFWorkbook(purposeExcel);

                formIndexSheet = purposeWorkbook.getSheetAt(0);
                purposeMatrixSheet = purposeWorkbook.getSheetAt(1);
                purposeSheet = purposeWorkbook.getSheetAt(2);
                purposeWorkbook.close();
            }

            String holdbackSheetPath = ServiceConfig.getConfig().getString("holdback.dr.holdbackSheet");
            XSSFWorkbook holdbackWorkbook;
            try (FileInputStream holdbackExcel = new FileInputStream(Resolver.resolveURL(holdbackSheetPath).getPath())) {
                holdbackWorkbook = new XSSFWorkbook(holdbackExcel);
                holdbackSheet = holdbackWorkbook.getSheetAt(1);
                holdbackWorkbook.close();
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the value of Form from a Preservica record.
     * @param xml InputStream containing a preservica record as XML.
     * @return the value extracted from the following XPath in the given XML: {@code /XIP/Metadata/Content/ns2:record/source/tvmeter/form}
     */
    private static String getFormValue(InputStream xml) throws IOException, ParserConfigurationException, SAXException {
        SAXParser saxParser = factory.newSAXParser();
        ElementExtractionHandler handler = new ElementExtractionHandler("/XIP/Metadata/Content/record/source/tvmeter/form");
        saxParser.parse(xml, handler);
        xml.reset();
        return handler.getCurrentValue();
    }

    /**
     * Get the value of contentsitem from a Preservica record.
     * @param xml InputStream containing a preservica record as XML.
     * @return the value extracted from the following XPath in the given XML: {@code /XIP/Metadata/Content/ns2:record/source/tvmeter/contentsitem}
     */
    private static String getContentsItem(InputStream xml) throws ParserConfigurationException, SAXException, IOException {
        SAXParser saxParser = factory.newSAXParser();
        ElementExtractionHandler handler = new ElementExtractionHandler("/XIP/Metadata/Content/record/source/tvmeter/contentsitem");
        saxParser.parse(xml, handler);
        xml.reset();
        return handler.getCurrentValue();
    }

    /**
     * Get the value of productioncountry from a Preservica record.
     * @param xml InputStream containing a preservica record as XML.
     * @return the value extracted from the following XPath in the given XML: {@code /XIP/Metadata/Content/ns2:record/source/tvmeter/productioncountry}
     */
    private static String getProductionCountry(InputStream xml) throws ParserConfigurationException, SAXException, IOException {
        SAXParser saxParser = factory.newSAXParser();
        ElementExtractionHandler handler = new ElementExtractionHandler("/XIP/Metadata/Content/record/source/tvmeter/productioncountry");
        saxParser.parse(xml, handler);
        xml.reset();
        return handler.getCurrentValue();
    }

    private static ZonedDateTime getStartDate(InputStream xml) throws ParserConfigurationException, SAXException, IOException {
        SAXParser saxParser = factory.newSAXParser();
        ElementExtractionHandler handler = new ElementExtractionHandler("/XIP/Metadata/Content/PBCoreDescriptionDocument/pbcoreInstantiation/pbcoreDateAvailable/dateAvailableStart");
        saxParser.parse(xml, handler);
        xml.reset();
        String datetimeString =  handler.getCurrentValue();

        String dateTimeFormat = "yyyy-MM-dd'T'HH:mm:ss[XX][XXX]";
        try {
            return DatetimeParser.parseStringToZonedDateTime(datetimeString, dateTimeFormat);
        } catch (MalformedIOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Get the formIndexSheet.
     */
    XSSFSheet getFormIndexSheet() {
        return formIndexSheet;
    }

    /**
     * Get the purposeMatrixSheet.
     */
    XSSFSheet getPurposeMatrixSheet() {
        return purposeMatrixSheet;
    }

    /**
     * Get the purposeSheet.
     */
    XSSFSheet getPurposeSheet() {
        return purposeSheet;
    }
}
