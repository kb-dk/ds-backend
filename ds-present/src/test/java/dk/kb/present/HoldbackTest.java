package dk.kb.present;

import dk.kb.util.Resolver;
import org.apache.commons.io.IOUtils;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HoldbackTest {

    @BeforeAll
    static void setup() throws ParserConfigurationException, SAXException {
        HoldbackDatePicker datePicker = new HoldbackDatePicker();
    }

    @Test
    public void getPurposeNameTest(){
        System.out.println(HoldbackDatePicker.getPurposeNameFromNumber("2.05.01"));
    }

    @Test
    public void getPurposeNumberTest(){
        HoldbackDatePicker.getPurposeFromContentAndForm("3171", "Form10");
    }


    @Test
    public void getFormString(){
        assertEquals("Form3", HoldbackDatePicker.createFormString(3.0));
    }

    @Test
    public void getFormNrTest() {
        assertEquals(3.0, HoldbackDatePicker.getFormNrFromForm("1220"));
    }

    @Test
    public void getCommoncodeTest() throws ParserConfigurationException, SAXException, IOException {
        InputStream xmlStream = IOUtils.toInputStream(Resolver.resolveUTF8String(TestFiles.PVICA_DOMS_MIG_9779a1b2));
        assertEquals("3171",  HoldbackDatePicker.getCommonCode(xmlStream));
    }

    @Test
    public void getFormTest() throws ParserConfigurationException, SAXException, IOException {
        InputStream xmlStream = IOUtils.toInputStream(Resolver.resolveUTF8String(TestFiles.PVICA_DOMS_MIG_9779a1b2));
        assertEquals("4500",  HoldbackDatePicker.getFormValue(xmlStream));
    }

    @Test
    public void xslxSheetTest() throws ParserConfigurationException, SAXException {
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
