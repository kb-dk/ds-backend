package dk.kb.present;

import dk.kb.util.Resolver;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HoldbackTest {

    @BeforeAll
    static void setup() {
        HoldbackDatePicker.init();
    }
    // TODO: introduce "chained" test
    // TODO: introduce excel lookup tests with no results

    @Test
    public void getHoldbackDateFromXmlTest() throws IOException {
        String xml = Resolver.resolveUTF8String(TestFiles.PVICA_DOMS_MIG_9ed10d66);
        assertEquals("2026-01-17T10:34:42+0100", HoldbackDatePicker.getHoldbackDateForRecord(xml));
    }

    @Test
    public void holdbackNoValueTest() throws IOException {
        String xml = Resolver.resolveUTF8String(TestFiles.PVICA_RECORD_3006e2f8);
        assertEquals("9017-07-07T17:29:55+0000", HoldbackDatePicker.getHoldbackDateForRecord(xml));
    }

    @Test
    public void holdbackNameNoValueTest() throws IOException {
        String xml = Resolver.resolveUTF8String(TestFiles.PVICA_RECORD_3006e2f8);
        assertTrue(HoldbackDatePicker.getPurposeNameFromXml(xml).isEmpty());
    }

    @Test
    public void getHoldbackPurposeFromXmlTest() throws IOException {
        String xml = Resolver.resolveUTF8String(TestFiles.PVICA_DOMS_MIG_9ed10d66);
        assertEquals("Dansk Dramatik & Fiktion", HoldbackDatePicker.getPurposeNameFromXml(xml));
    }

    @Test
    public void getNoHoldbackDateFromXmlTest() throws IOException {
        String xml = Resolver.resolveUTF8String(TestFiles.PVICA_DOMS_MIG_9779a1b2);
        assertEquals("9011-05-15T18:08:17+0100", HoldbackDatePicker.getHoldbackDateForRecord(xml));
    }

    @Test
    public void xslxSheetTest(){
        HoldbackDatePicker datePicker = new HoldbackDatePicker();
        int rowIndex = 0;

        // Assert Form Index Sheet is loaded
        XSSFSheet formIndexSheet = datePicker.getFormIndexSheet();
        XSSFRow row = formIndexSheet.getRow(rowIndex);
        XSSFCell cell = row.getCell(0);
        assertEquals("FormNr", cell.getStringCellValue());

        // Assert Purpose Matrix Sheet is loaded
        XSSFSheet purposeMatrixSheet = datePicker.getPurposeMatrixSheet();
        row = purposeMatrixSheet.getRow(rowIndex);
        cell = row.getCell(0);
        assertEquals("Id", cell.getStringCellValue());

        // Assert Purpose Sheet is loaded
        XSSFSheet purposeSheet = datePicker.getPurposeSheet();
        row = purposeSheet.getRow(rowIndex);
        cell = row.getCell(2);
        assertEquals("FormålNavn", cell.getStringCellValue());
    }

}
