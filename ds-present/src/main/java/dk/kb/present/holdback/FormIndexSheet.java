package dk.kb.present.holdback;

import dk.kb.util.webservice.exception.InternalServiceException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Object representing an Excel sheet containing values used to calculate FormNr from a 'form' value in metadata from record.
 */
public class FormIndexSheet {
    private static final Logger log = LoggerFactory.getLogger(FormIndexSheet.class);

    private List<String> formNr = new ArrayList<>();
    private List<Integer> formFra = new ArrayList<>();
    private List<Integer> formTil = new ArrayList<>();

    public FormIndexSheet(XSSFSheet formIndexSheet){

        for (Row row: formIndexSheet) {
            if (row.getRowNum() != 0){
                Cell formNrCell = row.getCell(0);
                Cell formFraCell = row.getCell(1);
                Cell formTilCell = row.getCell(2);

                // Add values from FormNr column to formNr List.
                if (formNrCell.getCellType() == CellType.NUMERIC){
                    int formNrContent = (int) formNrCell.getNumericCellValue();
                    // Adding the values as Form1, Form2, Form3 etc.
                    formNr.add("Form" + formNrContent);
                }
                // Add values from FormFra column to formFra List.
                if (formFraCell.getCellType() == CellType.NUMERIC){
                    int formFraContent = (int) formFraCell.getNumericCellValue();
                    formFra.add(formFraContent);
                }

                // Add values from FormTil column to formTil List.
                if (formTilCell.getCellType() == CellType.NUMERIC){
                    int formTilContent = (int) formTilCell.getNumericCellValue();
                    formTil.add(formTilContent);
                }

            }
        }

        if (formFra.size() != formTil.size()){
            log.error("Error in sizes of FormFra and FormTil columns");
            throw new InternalServiceException("The amount of values read from the columns FormTil and FormFra are divergent");
        }
        if (formFra.size() != formNr.size()){
            log.error("There is a different amount af values in the list formNr than formFra");
            throw new InternalServiceException("The amount of values read from the columns FormNr and FormFra are divergent");
        }


        log.info("Initialized FormIndexSheet with the following values: \n" +
                "           FormNr: {} \n" +
                "           FormFra: {} \n" +
                "           FormTil: {}", formNr, formFra, formTil);
    }

    /**
     * Extract FormNr based on the value of form, which is a four-digit number.
     * @param form four-digit number extracted from a preservica record.
     * @return the corresponding FormNr for the input form. Returns zero if no value has been extracted.
     */
    public String getFormNr(String form){
        if (form.isEmpty()){
            log.warn("No FormNr could be extracted as form string is empty. Returning an empty string.");
            return "";
        }

        int formInt = Integer.parseInt(form);
        if (formInt < 1000 || formInt > 7000){
            log.warn("The Form value extracted was out of range 1000-7000. This does not map to a FormNr. Form was: '{}'", formInt);
        }

        for (int i = 0; i < formNr.size(); i++) {
            int formFraInt = formFra.get(i);
            int formTilInt = formTil.get(i);

            if (formInt >= formFraInt && formInt <= formTilInt){
                return formNr.get(i);
            }

        }

        log.warn("No FormNr could be extracted for form: '{}'. Returning an empty string.", form);
        return "";
    }

}
