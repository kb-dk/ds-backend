package dk.kb.present.dr.holdback;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Object representing an Excel sheet containing values used to calculate purposeName from a 'purposeId' value extracted
 * in {@link PurposeMatrixSheet}.
 */
public class PurposeSheet {
    private static final Logger log = LoggerFactory.getLogger(PurposeSheet.class);

    /**
     * Map containing purposes. Keys are Purpose IDs and values are purposeNames.
     */
    private Map<String, String> purposeMap = new HashMap<>();

    public PurposeSheet(XSSFSheet purposeSheet){
        for (Row row : purposeSheet) {
            purposeMap.put(row.getCell(1).getStringCellValue(), row.getCell(2).getStringCellValue());
        }

        log.info("Initialized PurposeSheet with the following value: \n{}", this);
    }

    /**
     * Extract purposeName from a given purposeID in {@link #purposeMap}.
     * @param purposeID which maps to a textual name in the purposeSheet.
     * @return the purposeName for the input ID.
     */
    public String getPurposeNameFromNumber(String purposeID) {
        if (purposeID.isEmpty()){
            log.debug("PurposeID is empty. Returning an empty string as PurposeName.");
            return "";
        }

        for (Map.Entry<String, String> entry : purposeMap.entrySet()) {
            if (purposeID.equals(entry.getKey())){
                return entry.getValue();
            }
        }

        log.warn("No purposeName could be found for PurposeID: '{}'", purposeID);
        return "";
    }

    @Override
    public String toString() {
        return "PurposeSheet{" + "\n" +
                " purposeMap=" + purposeMap +
                '}';
    }
}
