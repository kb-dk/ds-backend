package dk.kb.present;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import dk.kb.present.saxhandlers.FormHandler;
import dk.kb.present.saxhandlers.ProductionCountryHandler;
import dk.kb.util.Resolver;
import dk.kb.present.saxhandlers.CommonCodeHandler;
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

public class HoldbackDatePicker {
    private static final Logger log = LoggerFactory.getLogger(HoldbackDatePicker.class);

    private static XSSFSheet formIndexSheet;
    private static XSSFSheet purposeMatrixSheet;
    private static XSSFSheet purposeSheet;

    private static final SAXParserFactory factory = SAXParserFactory.newInstance();

    HoldbackDatePicker() {
        readSheet();
    }

    public static String apply(String xml) throws IOException {
        try (InputStream xmlStream = IOUtils.toInputStream(xml, StandardCharsets.UTF_8)) {
            try {
                // Get form value
                String form = getFormValue(xmlStream);

                // Slå Form op i FormFra-FormTil og gem FormNr.
                Double formNr = getFormNrFromForm(form);
                String formString = createFormString(formNr);

                // get Common Code from xml
                String commonCode = getCommonCode(xmlStream);

                // Slå Indhold op i IndholdFra-IndholdTil i matrice i FormNr kolonne.
                String purposeNumber = getPurposeFromContentAndForm(commonCode, formString);
                purposeNumber = validatePurpose(purposeNumber, xmlStream);

                // Brug den fundne værdi i formåls arket til at finde formålNavn
                String purposeName = getPurposeNameFromNumber(purposeNumber);

                return purposeName;
            } catch (IOException | SAXException | ParserConfigurationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static String getPurposeNameFromNumber(String purposeNumber) {
        for (Row row : purposeSheet) {
            if (row.getCell(1).getStringCellValue().equals(purposeNumber)){
                return row.getCell(2).getStringCellValue();
            }
        }

        return "";
    }

    private static String validatePurpose(String purposeNumber, InputStream xml) throws ParserConfigurationException, IOException, SAXException {
        if (purposeNumber.equals("2.05")){
            String productionCountry = getProductionCountry(xml);
            if (productionCountry.equals("1000")){
                return purposeNumber + ".01";
            } else {
                return purposeNumber + ".02";
            }
        } else {
            return purposeNumber;
        }
    }

    static String getPurposeFromContentAndForm(String content, String formString) {
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
                        if (formCell.getStringCellValue().equals(formString)){
                            log.info("Found the correct formString, which is: '{}'", formCell.getStringCellValue());
                            log.info("Extracting Purpose Number from this cell, which is: '{}'", row.getCell(formCell.getColumnIndex()));
                            return String.valueOf(row.getCell(formCell.getColumnIndex()));
                        }

                    }
                }
            }
        }

        return "";
    }

    static String createFormString(Double formNr) {
        int formInt = formNr.intValue();
        return "Form"+formInt;
    }

    static double getFormNrFromForm(String form) {
        double formDouble = Double.parseDouble(form);
        for (Row row : formIndexSheet) {
            Cell formFra = row.getCell(1);
            Cell formTil = row.getCell(2);
            if (formFra != null && formFra.getCellType() == CellType.NUMERIC) {
                double formFraValue = formFra.getNumericCellValue();
                double formTilValue = formTil.getNumericCellValue();
                if (formDouble >= formFraValue && formDouble <= formTilValue) {
                    return row.getCell(0).getNumericCellValue();
                }
            }
        }

        return 0.0;
    }

    private void readSheet() {
        try {
            FileInputStream file = new FileInputStream(String.valueOf(Resolver.getPathFromClasspath("dr_formålstabeller.xlsx")));

            //Create Workbook instance holding reference to .xlsx file
            XSSFWorkbook workbook = new XSSFWorkbook(file);

            //Get first/desired sheet from the workbook
            formIndexSheet = workbook.getSheetAt(0);
            purposeMatrixSheet = workbook.getSheetAt(1);
            purposeSheet = workbook.getSheetAt(2);

            file.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static String getFormValue(InputStream xml) throws IOException, ParserConfigurationException, SAXException {
        SAXParser saxParser = factory.newSAXParser();
        FormHandler handler = new FormHandler();
        saxParser.parse(xml, handler);
        return handler.getCurrentValue();
    }

    static String getCommonCode(InputStream xml) throws ParserConfigurationException, SAXException, IOException {
        SAXParser saxParser = factory.newSAXParser();
        CommonCodeHandler handler = new CommonCodeHandler();
        saxParser.parse(xml, handler);
        return handler.getCurrentValue();
    }

    static String getProductionCountry(InputStream xml) throws ParserConfigurationException, SAXException, IOException {
        SAXParser saxParser = factory.newSAXParser();
        ProductionCountryHandler handler = new ProductionCountryHandler();
        saxParser.parse(xml, handler);
        return handler.getCurrentValue();
    }

    public XSSFSheet getFormIndexSheet() {
        return formIndexSheet;
    }

    public XSSFSheet getPurposeMatrixSheet() {
        return purposeMatrixSheet;
    }

    public XSSFSheet getPurposeSheet() {
        return purposeSheet;
    }
}
