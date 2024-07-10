package dk.kb.present.holdback.dto;

import dk.kb.util.webservice.exception.InternalServiceException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PurposeMatrixSheetDTO {
    private static final Logger log = LoggerFactory.getLogger(PurposeMatrixSheetDTO.class);

    private List<Integer> indholdFra = new ArrayList<>();
    private List<Integer> indholdTil = new ArrayList<>();
    private List<String> formNr = new ArrayList<>();
    private final int NUMBER_OF_COLUMNS = 18; // Number of columns in the sheet
    private final List<List<String>> formNrValues = new ArrayList<>(NUMBER_OF_COLUMNS);

    public PurposeMatrixSheetDTO(XSSFSheet purposeMatrixSheet){
        // Initialize lists for each column
        for (int i = 0; i < NUMBER_OF_COLUMNS; i++) {
            formNrValues.add(new ArrayList<>());
        }

        for (Row row : purposeMatrixSheet) {
            Cell indholdFraCell = row.getCell(1);
            Cell indholdTilCell = row.getCell(2);

            // Get all IndholdFra values.
            if (indholdFraCell != null && indholdFraCell.getCellType() == CellType.NUMERIC) {
                int indholdFraValue = (int) indholdFraCell.getNumericCellValue();
                indholdFra.add(indholdFraValue);
            }

            //Get all IndholdTil values.
            if (indholdTilCell != null && indholdTilCell.getCellType() == CellType.NUMERIC) {
                int indholdTilValue = (int) indholdTilCell.getNumericCellValue();
                indholdTil.add(indholdTilValue);
            }

            //Create a map entry for each column
            Iterator<Cell> cellIterator = row.cellIterator();
            if (row.getRowNum() == 0){
                while (cellIterator.hasNext()){
                    Cell formNrNameCell = cellIterator.next();

                    if (formNrNameCell.getCellType() == CellType.STRING &&
                        formNrNameCell.getStringCellValue().startsWith("Form")){
                        formNr.add(formNrNameCell.getStringCellValue());
                    }
                }
            } else {
                // Iterate over each row in the sheet
                for (int colIndex = 3; colIndex < NUMBER_OF_COLUMNS; colIndex++) {
                    Cell cell = row.getCell(colIndex);
                    if (cell.getCellType() == CellType.NUMERIC){
                        formNrValues.get(colIndex).add(String.valueOf( (int) cell.getNumericCellValue()));
                    } else {
                        formNrValues.get(colIndex).add(cell.getStringCellValue());
                    }
                }
            }
        }

        // Remove the first item 3 times to target empty columns for Id, IndholdFra and IndholdTil
        formNrValues.subList(0, 3).clear();

        if (formNrValues.size() != formNr.size()){
            throw new InternalServiceException("There is a difference between amount of formNrs and formNrValues. Their respective sizes are: " + formNr.size() + " and " + formNrValues.size());
        }

        if (indholdTil.size() != indholdFra.size()){
            throw new InternalServiceException("There is a difference between the sizes of the lists 'indholdTil' and 'indholdFra'. Their respective sizes are: " + indholdTil.size() + " and " + indholdFra.size());
        }

        for (List<String> list : formNrValues) {
            if (list.size() != indholdFra.size()){
                throw new InternalServiceException("There is a difference between the sizes of a list in the nested list 'formNrValues' and 'indholdFra'. Their respective sizes are: " + list.size() + " and " + indholdFra.size());
            }
        }

        log.info("Initialized PurposeMatrixSheet with the following values: \n" +
                "           IndholdFra: {} \n" +
                "           IndholdTil: {} \n" +
                "           FormNr: {} \n" +
                "           FormNrValues: {}", indholdFra, indholdTil, formNr, prettyPrintFormValues());

    }

    /**
     * Get PurposeID from provided Content and FormNr.
     *
     * @param content a four-digit number extracted from the XML, which holdback is being calculated for.
     * @param formNrString a string of the format: 'FormX' where x has been resolved through {@link FormIndexSheetDTO#getFormNr(String)}
     * @return a PurposeID most likely in the format x.xx: An example could be the string '2.02'.
     */
    public String getPurposeIdFromContentAndForm(String content, String formNrString) {
        if (content.isEmpty()){
            log.warn("The field 'content' is empty. PurposeID can't be calculated. Returning an empty string.");
            return "";
        }

        int contentInt = Integer.parseInt(content);

        for (int contentIndex = 0; contentIndex < formNr.size() ; contentIndex++) {
            if (contentInt >= indholdFra.get(contentIndex) && contentInt <= indholdTil.get(contentIndex)){

                for (int formNrList = 0; formNrList <formNr.size() ; formNrList++) {
                    if (formNrString.equals(formNr.get(formNrList))){
                        return formNrValues.get(formNrList).get(contentIndex);
                    }
                }
            }
        }
        log.warn("No PurposeID could be extracted from content: '{}' and form: '{}'", content, formNrString);
        return "";
    }


    private String prettyPrintFormValues(){
        StringBuilder builder = new StringBuilder();

        for (List<String> formValue : formNrValues) {
            builder.append("\n              ").append(formValue);
        }

        return builder.toString();
    }
}
