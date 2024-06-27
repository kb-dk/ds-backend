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
    // TODO: introduce negative tests
    // TODO: introduce "chained" test

    @Test
    public void getHoldbackDateFromXmlTest() throws IOException {
        String xml = Resolver.resolveUTF8String(TestFiles.PVICA_DOMS_MIG_9ed10d66);
        assertEquals("2026-01-17T10:34:42+0100", HoldbackDatePicker.getHoldbackDateForRecord(xml));
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
