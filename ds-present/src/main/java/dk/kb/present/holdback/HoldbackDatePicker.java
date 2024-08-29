package dk.kb.present.holdback;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import dk.kb.present.util.ExtractedPreservicaValues;
import dk.kb.present.config.ServiceConfig;
import dk.kb.util.Resolver;
import dk.kb.util.webservice.exception.InternalServiceException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
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
     * Object representing values from an Excel sheet containing table to find FormNr in based on Form from the XML.
     */
    private static FormIndexSheet formIndexSheet;

    /**
     * Object representing an Excel sheet containing table to find PurposeID in based on Content and FormNr.
     */
    private static PurposeMatrixSheet purposeMatrixSheet;

    /**
     * Object representing an Excel sheet containing table to find PurposeName in based on PurposeID
     */
    private static PurposeSheet purposeSheet;

    /**
     * Object representing an Excel sheet containing table to find holdback days for a given purpose.
     */
    private static HoldbackSheet holdbackSheet;

    private static SAXParserFactory factory;

    /**
     * StartDate from XML content of the record. This date is used to calculate the holdback date from.
     */
    private static String startDate;

    HoldbackDatePicker() {}

    public static void init(){
        factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(false);
        readSheet();
    }

    public static synchronized HoldbackDatePicker getInstance(){
        return datePicker;
    }

    public HoldbackObject getHoldbackDateForRecord(ExtractedPreservicaValues extractedValues, String origin) throws IOException {
        HoldbackDatePicker.startDate = extractedValues.getStartTime();
        HoldbackObject result = new HoldbackObject();
        if (origin == null){
            log.error("Origin was null. Holdback cannot be calculated for records that are not from origins 'ds.radio' or 'ds.tv'. Returning a result object without " +
                    "values.");
            result.setHoldbackPurposeName("");
            result.setHoldbackDate("");
            return result;
        }

        if (origin.equals("ds.tv")){
            return getHoldbackForTvRecord(extractedValues, result);
        } else if (origin.equals("ds.radio")) {
            return getHoldbackForRadioRecord(result);
        } else {
            log.error("Holdback cannot be calculated for records that are not from origins 'ds.radio' or 'ds.tv'." +
                    " Returning a result object without values.");
            throw new InternalServiceException("Holdback cannot be calculated for records that are not from origins 'ds.radio' or 'ds.tv'." +
                    " Returning a result object without values.");
        }
    }

    /**
     * Calculate holdback for a Radio record by adding 3 years to its aired time. This value has been requested/defined by DR.
     * @param result holdbackDTO object, which the calculated holdback date is added to. This will never contain a value
     *               for {@code holdbackPurposeName} as these are not present for radio records.
     *
     * @return the updated HoldbackObject.
     */
    private HoldbackObject getHoldbackForRadioRecord(HoldbackObject result) {
        // Radio should be held back by three years by DR request.
        result.setHoldbackDate(calculateHoldbackDate(ZonedDateTime.parse(startDate), 1096));
        result.setHoldbackPurposeName("");
        return result;
    }

    /**
     * Calculate holdback for a TV record by comparing values in the record to the DR provided schemas.
     * @param extractedValues containing values used to calculate holdback.
     * @param result holdbackDTO containing the purposeName and the holdbackDate for a record.
     * @return the result object with updated values.
     */
    private static HoldbackObject getHoldbackForTvRecord(ExtractedPreservicaValues extractedValues, HoldbackObject result) {
        try {
            result.setHoldbackPurposeName(getPurposeName(extractedValues));

            if (result.getHoldbackPurposeName().isEmpty()){
                log.warn("Purpose name was empty. Setting holdback date to 9999-01-01T00:00:00Z");
                result.setHoldbackDate("9999-01-01T00:00:00Z");
            } else {
                int holdbackDays = holdbackSheet.getHoldbackDaysForPurpose(result.getHoldbackPurposeName());
                log.info("Start date is: " + startDate);
                result.setHoldbackDate(calculateHoldbackDate(ZonedDateTime.parse(startDate), holdbackDays));
            }

            return result;
        } catch (IOException | SAXException | ParserConfigurationException e) {
            throw new RuntimeException(e);
        }

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
     * Get purposeName for a preservica record containing metadata about a DR program.
     * @param extractedValues {@link ExtractedPreservicaValues} containing data from a preservica record for analysis.
     * @return the purposeName for a given program.
     */
    private static String getPurposeName(ExtractedPreservicaValues extractedValues) throws IOException, ParserConfigurationException, SAXException {
        // Get form value
        String form = extractedValues.getFormValue();

        // get formNr by looking up form in formIndexSheet.
        String formString = formIndexSheet.getFormNr(form);

        // TODO: do some logic on gallup/nielsen differences.
        // get contentsitem from xml
        String contentsItem = extractedValues.getContentsItem();

        // Slå Indhold op i IndholdFra-IndholdTil i matrice i FormNr kolonne.
        String purposeNumber = purposeMatrixSheet.getPurposeIdFromContentAndForm(contentsItem, formString);
        purposeNumber = validatePurpose(purposeNumber, extractedValues.getOrigin());

        // Brug den fundne værdi i formåls arket til at finde formålNavn
        return purposeSheet.getPurposeNameFromNumber(purposeNumber);
    }

    /**
     * Validate and handle special cases of IDs constructed by {@link PurposeMatrixSheet#getPurposeIdFromContentAndForm(String, String)}.
     * @param purposeId which is to be validated.
     * @param productionCountry string containing the productionCountry value from a preservica record. This should not be extracted from the fields named productionCountry or
     *                          countryOfOrigin, but rather from the field origin.
     * @return an updated PurposeID
     */
    private static String validatePurpose(String purposeId, String productionCountry) {
        // Handling of special case for purposeID 2.05, where country of production is needed to create the correct value.
        if (purposeId.equals("2.05")) {
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
     * Setup of the HoldbackDatePicker. Fetches the Excel sheets for {@link #formIndexSheet}, {@link #purposeMatrixSheet},
     * {@link #purposeSheet} and {@link #holdbackSheet}.
     */
    private static void readSheet() {
        try {
            String purposeSheetPath = ServiceConfig.getConfig().getString("holdback.dr.purposeSheet");
            XSSFWorkbook purposeWorkbook;
            try (FileInputStream purposeExcel = new FileInputStream(Resolver.resolveURL(purposeSheetPath).getPath())) {
                purposeWorkbook = new XSSFWorkbook(purposeExcel);

                formIndexSheet = new FormIndexSheet(purposeWorkbook.getSheetAt(0));

                purposeMatrixSheet = new PurposeMatrixSheet(purposeWorkbook.getSheetAt(1));
                purposeSheet = new PurposeSheet(purposeWorkbook.getSheetAt(2));
                purposeWorkbook.close();
            }

            String holdbackSheetPath = ServiceConfig.getConfig().getString("holdback.dr.holdbackSheet");
            XSSFWorkbook holdbackWorkbook;
            try (FileInputStream holdbackExcel = new FileInputStream(Resolver.resolveURL(holdbackSheetPath).getPath())) {
                holdbackWorkbook = new XSSFWorkbook(holdbackExcel);
                holdbackSheet = new HoldbackSheet(holdbackWorkbook.getSheetAt(1));
                holdbackWorkbook.close();
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
