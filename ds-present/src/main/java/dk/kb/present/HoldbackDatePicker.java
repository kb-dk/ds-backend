package dk.kb.present;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import dk.kb.present.util.saxhandlers.FormHandler;
import dk.kb.present.util.saxhandlers.ProductionCountryHandler;
import dk.kb.present.util.saxhandlers.StartDateHandler;
import dk.kb.util.Resolver;
import dk.kb.present.util.saxhandlers.ContentsItemHandler;
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
 * This is done by looking up the PurposeName in the TODO: what is the name of this sheet and where is it?
 */
public class HoldbackDatePicker {
    private static final Logger log = LoggerFactory.getLogger(HoldbackDatePicker.class);

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

    private static final SAXParserFactory factory = SAXParserFactory.newInstance();

    HoldbackDatePicker() {
        // TODO: Convert to proper singleton
        readSheet();
    }

    /**
     * Apply the HoldbackDatePicker to a string of XML representing a Preservica Information Object.
     *
     * @param xml InformationObject from Preservica encapsulating a DR record/program, which holdback needs to be
     *            calculated for
     * @return a string containing the date for when the holdback for the record has expired.
     *         In the format: yyyy-MM-dd'T'HH:mm:ssZ
     */
    public static String getHoldbackDateForRecord(String xml) throws IOException {
        try (InputStream xmlStream = IOUtils.toInputStream(xml, StandardCharsets.UTF_8)) {
            try {
                String purposeName = getPurposeName(xmlStream);

                int holdbackDays = getHoldbackDaysForPurpose(purposeName);

                String holdbackDate = addHoldbackDaysToRecordStartDate(xmlStream, holdbackDays);

                return holdbackDate;
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
     * @param startDate a date representing the date when a program was broadcasted.
     * @param holdbackDays the amount of days that has to parse before a program can be retrieved in the archive.
     * @return the date, when the holdback period has expired as a string in the format: yyyy-MM-dd'T'HH:mm:ssZ.
     */
    protected static String calculateHoldbackDate(ZonedDateTime startDate, int holdbackDays) {
        ZonedDateTime holdbackExpiredDate = startDate.plusDays(holdbackDays);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

        String formattedHoldbackDate = holdbackExpiredDate.format(formatter);
        return formattedHoldbackDate;

    }

    /**
     * From a purposeName, get the amount of days which the program should be held back.
     * This is looked up in the DR provided holdback sheet.
     * @param purpose purposeName from {@link #purposeSheet} to lookup.
     * @return amount of holdback days.
     */
    protected static int getHoldbackDaysForPurpose(String purpose) {
        for (Row row : holdbackSheet) {
            // Check the value against the 2019 and 2022 values.
            if (purpose.equals(row.getCell(2).getStringCellValue()) || purpose.equals(row.getCell(0).getStringCellValue() )){
                return (int) row.getCell(4).getNumericCellValue();
            }
        }

        log.warn("No holdback has been defined for purpose: '{}'. Returning -1.", purpose);
        return -1;
    }

    /**
     * Get purposeName for a preservica record containing metadata about a DR program.
     * @param xmlStream containing the preservica record for analysis.
     * @return the purposeName for a given program.
     */
    public static String getPurposeName(InputStream xmlStream) throws IOException, ParserConfigurationException, SAXException {
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
    protected static String getPurposeNameFromNumber(String purposeID) {
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
    protected static String getPurposeIdFromContentAndForm(String content, String formNrString) {
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
    protected static String createFormString(int formNr) {
        return "Form" + formNr;
    }

    /**
     * Extract FormNr from {@link #formIndexSheet} based on the value of form, which is a four-digit number.
     * @param form four-digit number extracted from a preservica record.
     * @return the corresponding FormNr for the input form. Returns zero if no value has been extracted.
     */
    protected static int getFormNrFromForm(String form) {
        double formDouble = Double.parseDouble(form);
        // TODO: Add warning logs for values below 1000 and above 7000.

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
     * Setup of the HoldbackDatePicker. Fetches the Excel sheets for {@link #formIndexSheet}, {@link #purposeMatrixSheet}
     * and {@link #purposeSheet}.
     */
    private void readSheet() {
        try {
            FileInputStream purposeExcel = new FileInputStream(Resolver.getPathFromClasspath("dr_formålstabeller.xlsx").toString());
            XSSFWorkbook purposeWorkbook = new XSSFWorkbook(purposeExcel);

            formIndexSheet = purposeWorkbook.getSheetAt(0);
            purposeMatrixSheet = purposeWorkbook.getSheetAt(1);
            purposeSheet = purposeWorkbook.getSheetAt(2);

            purposeExcel.close();

            FileInputStream holdbackExcel = new FileInputStream(Resolver.getPathFromClasspath("dr_holdback.xlsx").toString());
            XSSFWorkbook holdbackWorkbook = new XSSFWorkbook(holdbackExcel);

            holdbackSheet = holdbackWorkbook.getSheetAt(1);

            holdbackExcel.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the value of Form from a Preservica record.
     * @param xml InputStream containing a preservica record as XML.
     * @return the value extracted from the following XPath in the given XML: {@code /XIP/Metadata/Content/ns2:record/source/tvmeter/form}
     */
    protected static String getFormValue(InputStream xml) throws IOException, ParserConfigurationException, SAXException {
        SAXParser saxParser = factory.newSAXParser();
        FormHandler handler = new FormHandler();
        saxParser.parse(xml, handler);
        xml.reset();
        return handler.getCurrentValue();
    }

    /**
     * Get the value of contentsitem from a Preservica record.
     * @param xml InputStream containing a preservica record as XML.
     * @return the value extracted from the following XPath in the given XML: {@code /XIP/Metadata/Content/ns2:record/source/tvmeter/contentsitem}
     */
    protected static String getContentsItem(InputStream xml) throws ParserConfigurationException, SAXException, IOException {
        SAXParser saxParser = factory.newSAXParser();
        ContentsItemHandler handler = new ContentsItemHandler();
        saxParser.parse(xml, handler);
        xml.reset();
        return handler.getCurrentValue();
    }

    /**
     * Get the value of productioncountry from a Preservica record.
     * @param xml InputStream containing a preservica record as XML.
     * @return the value extracted from the following XPath in the given XML: {@code /XIP/Metadata/Content/ns2:record/source/tvmeter/productioncountry}
     */
    protected static String getProductionCountry(InputStream xml) throws ParserConfigurationException, SAXException, IOException {
        SAXParser saxParser = factory.newSAXParser();
        ProductionCountryHandler handler = new ProductionCountryHandler();
        saxParser.parse(xml, handler);
        xml.reset();
        return handler.getCurrentValue();
    }

    protected static ZonedDateTime getStartDate(InputStream xml) throws ParserConfigurationException, SAXException, IOException {
        SAXParser saxParser = factory.newSAXParser();
        StartDateHandler handler = new StartDateHandler();
        saxParser.parse(xml, handler);
        xml.reset();
        String datetimeString =  handler.getCurrentValue();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
        return ZonedDateTime.parse(datetimeString, formatter);
    }

    /**
     * Get the formIndexSheet.
     */
    protected XSSFSheet getFormIndexSheet() {
        return formIndexSheet;
    }

    /**
     * Get the purposeMatrixSheet.
     */
    protected XSSFSheet getPurposeMatrixSheet() {
        return purposeMatrixSheet;
    }

    /**
     * Get the purposeSheet.
     */
    protected XSSFSheet getPurposeSheet() {
        return purposeSheet;
    }
}
