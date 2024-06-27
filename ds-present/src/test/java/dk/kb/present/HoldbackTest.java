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
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HoldbackTest {

    @BeforeAll
    static void setup() {
        HoldbackDatePicker.init();
    }


    @Test
    public void getHoldbackDateFromXmlTest() throws IOException {
        String xml = Resolver.resolveUTF8String(TestFiles.PVICA_DOMS_MIG_9ed10d66);
        assertEquals("2026-01-17T10:34:42+0100", HoldbackDatePicker.getHoldbackDateForRecord(xml));
    }
    @Test
    public void getNoHoldbackDateFromXmlTest() throws IOException {
        String xml = Resolver.resolveUTF8String(TestFiles.PVICA_DOMS_MIG_9779a1b2);
        assertEquals("9011-05-15T18:08:17+0100", HoldbackDatePicker.getHoldbackDateForRecord(xml));
    }
    @Test
    public void getPurposeName() throws IOException, ParserConfigurationException, SAXException {
        InputStream xmlStream = IOUtils.toInputStream(Resolver.resolveUTF8String(TestFiles.PVICA_DOMS_MIG_9779a1b2));
        assertEquals("Udenlandsk Dramatik & Fiktion", HoldbackDatePicker.getPurposeName(xmlStream));
    }
    @Test
    public void getHoldbackDateTest() throws ParserConfigurationException, IOException, SAXException {
        InputStream xmlStream = IOUtils.toInputStream(Resolver.resolveUTF8String(TestFiles.PVICA_DOMS_MIG_9779a1b2));
        ZonedDateTime startDate = HoldbackDatePicker.getStartDate(xmlStream);


        String holdbackExpiredDate = HoldbackDatePicker.calculateHoldbackDate(startDate, 30);
        assertEquals("2016-02-05T18:08:17+0100", holdbackExpiredDate);
    }
    @Test
    public void getHoldbackDaysTest(){
        assertEquals(30, HoldbackDatePicker.getHoldbackDaysForPurpose("Nyheder"));
    }

    @Test
    public void getHoldbackDaysNonExistingTest(){
        assertEquals(2555000, HoldbackDatePicker.getHoldbackDaysForPurpose("Nyhed"));
    }

    @Test
    public void getPurposeNameTest(){
        assertEquals("Dansk Dramatik & Fiktion", HoldbackDatePicker.getPurposeNameFromNumber("2.05.01"));
        assertEquals("Aktualitet & Debat", HoldbackDatePicker.getPurposeNameFromNumber("2.02"));
    }

    @Test
    public void getPurposeNumberTest(){
        // TODO: More tests. Also negative tests for those fields that can be empty.
        assertEquals("2.02", HoldbackDatePicker.getPurposeIdFromContentAndForm("1800", "Form10"));
    }


    @Test
    public void getFormString(){
        assertEquals("Form3", HoldbackDatePicker.createFormString(3));
    }

    @Test
    public void getFormNrTest() {
        assertEquals(3.0, HoldbackDatePicker.getFormNrFromForm("1220"));
    }

    @Test
    public void getCommoncodeTest() throws ParserConfigurationException, SAXException, IOException {
        InputStream xmlStream = IOUtils.toInputStream(Resolver.resolveUTF8String(TestFiles.PVICA_DOMS_MIG_9779a1b2));
        assertEquals("3171",  HoldbackDatePicker.getContentsItem(xmlStream));
    }

    @Test
    public void getFormTest() throws ParserConfigurationException, SAXException, IOException {
        InputStream xmlStream = IOUtils.toInputStream(Resolver.resolveUTF8String(TestFiles.PVICA_DOMS_MIG_9779a1b2));
        assertEquals("4500",  HoldbackDatePicker.getFormValue(xmlStream));
    }

    @Test
    public void getStartDate() throws IOException, ParserConfigurationException, SAXException {
        InputStream xmlStream = IOUtils.toInputStream(Resolver.resolveUTF8String(TestFiles.PVICA_DOMS_MIG_9779a1b2));
        System.out.println(HoldbackDatePicker.getStartDate(xmlStream));
        //assertEquals("2016-01-06T18:08:17+0100", HoldbackDatePicker.getStartDate(xmlStream));
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
