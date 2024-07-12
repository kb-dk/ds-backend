package dk.kb.present;

import dk.kb.present.config.ServiceConfig;
import dk.kb.present.holdback.HoldbackObject;
import dk.kb.present.holdback.HoldbackDatePicker;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.util.Resolver;
import dk.kb.util.webservice.exception.InternalServiceException;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration")
public class HoldbackTest {

    private static DsRecordDto tvRecord1 = new DsRecordDto();
    private static DsRecordDto tvRecord2 = new DsRecordDto();
    private static DsRecordDto radioRecord1 = new DsRecordDto();
    private static DsRecordDto badRecord = new DsRecordDto();
    private static DsRecordDto noOriginRecord = new DsRecordDto();
    private static DsRecordDto badOriginRecord = new DsRecordDto();

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

        badRecord.setOrigin("ds.tv");
        badRecord.setData(Resolver.resolveUTF8String(TestFiles.PVICA_HOMEMADE_HOLDBACK_TEST_RECORD));

        noOriginRecord.setOrigin(null);
        badOriginRecord.setOrigin("this.is.not.an.origin");
    }

    @Test
    public void badOriginsTest() throws IOException {
        assertEquals("", HoldbackDatePicker.getInstance().getHoldbackDateForRecord(noOriginRecord).getHoldbackDate());
        assertEquals("", HoldbackDatePicker.getInstance().getHoldbackDateForRecord(noOriginRecord).getHoldbackPurposeName());

        Exception exception = assertThrowsExactly(InternalServiceException.class, () -> HoldbackDatePicker.getInstance().getHoldbackDateForRecord(badOriginRecord));
        assertEquals("Holdback cannot be calculated for records that are not from origins 'ds.radio' or 'ds.tv'. Returning a result object without values.", exception.getMessage());
    }

    @Test
    public void badRecordTest() throws IOException {
        assertEquals("9999-01-01T00:00:00Z", HoldbackDatePicker.getInstance().getHoldbackDateForRecord(badRecord).getHoldbackDate());
    }

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
        HoldbackObject holdbackObject =  HoldbackDatePicker.getInstance().getHoldbackDateForRecord(radioRecord1);

        assertEquals("2021-04-03T08:03:00Z", holdbackObject.getHoldbackDate());
        assertEquals("", holdbackObject.getHoldbackPurposeName());
    }

    /*@Test
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
    }*/

}
