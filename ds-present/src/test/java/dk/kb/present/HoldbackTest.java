package dk.kb.present;

import dk.kb.present.config.ServiceConfig;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.util.Resolver;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration")
public class HoldbackTest {

    private static DsRecordDto tvRecord1 = new DsRecordDto();
    private static DsRecordDto tvRecord2 = new DsRecordDto();
    private static DsRecordDto radioRecord1 = new DsRecordDto();

    @BeforeAll
    static void setup() throws IOException {
        ServiceConfig.initialize("conf/ds-present-behaviour.yaml");
        HoldbackDatePicker.init();

        tvRecord1.setOrigin("ds.tv");
        tvRecord1.setData(Resolver.resolveUTF8String(TestFiles.PVICA_DOMS_MIG_9ed10d66));

        tvRecord2.setOrigin("ds.tv");
        tvRecord2.setData(Resolver.resolveUTF8String(TestFiles.PVICA_RECORD_3006e2f8));

        radioRecord1.setOrigin("ds.radio");
        radioRecord1.setData(Resolver.resolveUTF8String(TestFiles.PVICA_RECORD_2b462c63));
    }
    // TODO: introduce "chained" test
    // TODO: introduce excel lookup tests with no results

    @Test
    public void getHoldbackDateFromXmlTest() throws IOException {
        assertEquals("2026-01-17T09:34:42Z", HoldbackDatePicker.getInstance().getHoldbackDateForRecord(tvRecord1).getHoldbackDate());
    }

    @Test
    public void holdbackNoValueTest() throws IOException {
        assertEquals("9999-01-01T00:00:00Z", HoldbackDatePicker.getInstance().getHoldbackDateForRecord(tvRecord2).getHoldbackDate());
    }

    @Test
    public void holdbackNameNoValueTest() throws IOException {
        assertTrue(HoldbackDatePicker.getInstance().getHoldbackDateForRecord(tvRecord2).getHoldbackPurposeName().isEmpty());
    }

    @Test
    public void getHoldbackPurposeFromXmlTest() throws IOException {
        assertEquals("Dansk Dramatik & Fiktion", HoldbackDatePicker.getInstance().getHoldbackDateForRecord(tvRecord1).getHoldbackPurposeName());
    }

    @Test
    public void getNoHoldbackDateFromXmlTest() throws IOException {
        assertEquals("9999-01-01T00:00:00Z", HoldbackDatePicker.getInstance().getHoldbackDateForRecord(tvRecord2).getHoldbackDate());
    }

    @Test
    public void radioHoldbackTest() throws IOException {
        HoldbackDTO holdbackDTO =  HoldbackDatePicker.getInstance().getHoldbackDateForRecord(radioRecord1);

        assertEquals("2021-04-03T08:03:00Z", holdbackDTO.getHoldbackDate());
        assertEquals("", holdbackDTO.getHoldbackPurposeName());
    }

    @Test
    public void xslxSheetTest(){
        int rowIndex = 0;

        // Assert Form Index Sheet is loaded
        XSSFSheet formIndexSheet = HoldbackDatePicker.getInstance().getFormIndexSheet();
        XSSFRow row = formIndexSheet.getRow(rowIndex);
        XSSFCell cell = row.getCell(0);
        assertEquals("FormNr", cell.getStringCellValue());

        // Assert Purpose Matrix Sheet is loaded
        XSSFSheet purposeMatrixSheet = HoldbackDatePicker.getInstance().getPurposeMatrixSheet();
        row = purposeMatrixSheet.getRow(rowIndex);
        cell = row.getCell(0);
        assertEquals("Id", cell.getStringCellValue());

        // Assert Purpose Sheet is loaded
        XSSFSheet purposeSheet = HoldbackDatePicker.getInstance().getPurposeSheet();
        row = purposeSheet.getRow(rowIndex);
        cell = row.getCell(2);
        assertEquals("FormålNavn", cell.getStringCellValue());
    }

}
