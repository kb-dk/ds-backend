package dk.kb.present.holdback.dto;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class PurposeSheetDTO {
    private static final Logger log = LoggerFactory.getLogger(PurposeSheetDTO.class);

    /**
     * Map containing purposes. Keys are Purpose IDs and values are purposeNames.
     */
    private Map<String, String> purposeMap = new HashMap<>();

    public PurposeSheetDTO(XSSFSheet purposeSheet){
        for (Row row : purposeSheet) {
            purposeMap.put(row.getCell(1).getStringCellValue(), row.getCell(2).getStringCellValue());
        }
    }

    /**
     * Extract purposeName from a given purposeID in {@link #purposeMap}.
     * @param purposeID which maps to a textual name in the purposeSheet.
     * @return the purposeName for the input ID.
     */
    public String getPurposeNameFromNumber(String purposeID) {
        if (purposeID.isEmpty()){
            log.warn("PurposeID is empty. Returning an empty string as PurposeName.");
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
}
