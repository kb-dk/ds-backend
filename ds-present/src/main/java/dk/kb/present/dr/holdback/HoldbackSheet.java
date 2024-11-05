package dk.kb.present.dr.holdback;

import dk.kb.util.webservice.exception.NotFoundServiceException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Object representing an Excel sheet containing values used to calculate amount of holdback days from value extracted
 * from {@link PurposeSheet}.
 */
public class HoldbackSheet {
    private static final Logger log = LoggerFactory.getLogger(HoldbackSheet.class);

    /**
     * A Map containing purposeNames as keys and days of holdback as values.
     */
    Map<String, Integer> holdbackDaysForPurpose = new HashMap<>();



    public HoldbackSheet(XSSFSheet holdbackSheet) {
        for (Row row : holdbackSheet) {
            if (row.getRowNum() != 0){
                // Add holdback days to list
                String purposeName = getPurposeName(row);
                int holdbackDays = (int) row.getCell(4).getNumericCellValue();
                holdbackDaysForPurpose.put(purposeName, holdbackDays);
            }
        }

        log.info("Initialized HoldbackSheet with the following values: \n{}", this);
    }

    /**
     * Get the 2019 purposeName, if this value is empty try fetching the 2022 value from the same row
     * @param row to get purposeValue from
     * @return if 2019 purposeValue is present return that, otherwise try to return the 2022 value.
     */
    private String getPurposeName(Row row) {
        String purposeName =  row.getCell(2).getStringCellValue();

        if (purposeName.isEmpty()){
            purposeName =  row.getCell(0).getStringCellValue();
        }

        if (purposeName.isEmpty()){
            log.warn("No purposeName could be extracted from row number: {}", row.getRowNum());
        }
        return purposeName;
    }


    /**
     * From a purposeName, get the amount of days which the program should be held back.
     * This is looked up in the DR provided holdback sheet.
     * @param purpose purposeName from {@link PurposeSheet} to lookup.
     * @return amount of holdback days.
     */
    public int getHoldbackDaysForPurpose(String purpose) {
        // If the purpose gets calculated to 'Udenlandsk Dramatik & Fiktion' the record will be filtered away by the own-prodution filter and therefore a "random" high value is
        // set here.
        if (purpose.equals("Udenlandsk Dramatik & Fiktion")){
            return 999999;
        }

        for (Map.Entry<String, Integer> entry : holdbackDaysForPurpose.entrySet()) {
            if (purpose.equals(entry.getKey())){
                return entry.getValue();
            }
        }


        log.error("No holdback has been defined for purpose: '{}'.", purpose);
        throw new NotFoundServiceException("No holdback value cold be found for purpose: '" + purpose + "' in holdbackSheet.");
    }

    @Override
    public String toString() {
        return "HoldbackSheet{" + "\n" +
                " holdbackDaysForPurpose=" + holdbackDaysForPurpose +
                "\n}";
    }
}
