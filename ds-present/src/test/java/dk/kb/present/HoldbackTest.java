package dk.kb.present;

import dk.kb.present.config.ServiceConfig;
import dk.kb.present.holdback.HoldbackObject;
import dk.kb.present.holdback.HoldbackDatePicker;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.util.Resolver;
import dk.kb.util.webservice.exception.InternalServiceException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration")
public class HoldbackTest {

    private static RecordValues tvValues1 = new RecordValues();
    private static RecordValues tvValues2 = new RecordValues();
    private static RecordValues radioValues = new RecordValues();
    private static RecordValues badValues = new RecordValues();

    @BeforeAll
    static void setup() throws IOException {
        ServiceConfig.initialize("conf/ds-present-behaviour.yaml");
        HoldbackDatePicker.init();


        tvValues1.setFormValue("4411");
        tvValues1.setContentsItem("3190");
        tvValues1.setOrigin("1000");
        tvValues1.setStartTime("2016-01-20T10:34:42+0100");

        tvValues2.setStartTime("2022-02-28T17:29:55Z");

        radioValues.setStartTime("2018-04-03T08:03:00Z");

        badValues.setFormValue("1800");
        badValues.setContentsItem("3100");
        badValues.setOrigin("2211");
        badValues.setStartTime("2016-01-06T18:08:17+0100");
    }

    @Test
    public void badOriginsTest() throws IOException {
        RecordValues values = new RecordValues();
        assertEquals("", HoldbackDatePicker.getInstance().getHoldbackDateForRecord(values, null).getHoldbackDate());
        assertEquals("", HoldbackDatePicker.getInstance().getHoldbackDateForRecord(values, null).getHoldbackPurposeName());

        Exception exception = assertThrowsExactly(InternalServiceException.class, () -> HoldbackDatePicker.getInstance().getHoldbackDateForRecord(values, ""));
        assertEquals("Holdback cannot be calculated for records that are not from origins 'ds.radio' or 'ds.tv'. Returning a result object without values.", exception.getMessage());
    }

    @Test
    public void badRecordTest() throws IOException {
        assertEquals("9999-01-01T00:00:00Z", HoldbackDatePicker.getInstance().getHoldbackDateForRecord(badValues, "ds.tv").getHoldbackDate());
    }

    @Test
    public void getHoldbackDateFromXmlTest() throws IOException {
        assertEquals("2026-01-17T09:34:42Z", HoldbackDatePicker.getInstance().getHoldbackDateForRecord(tvValues1, "ds.tv").getHoldbackDate());
    }


    @Test
    public void holdbackNoValueTest() throws IOException {
        assertEquals("9999-01-01T00:00:00Z", HoldbackDatePicker.getInstance().getHoldbackDateForRecord(tvValues2, "ds.tv").getHoldbackDate());
    }

    @Test
    public void holdbackNameNoValueTest() throws IOException {
        assertTrue(HoldbackDatePicker.getInstance().getHoldbackDateForRecord(tvValues2, "ds.tv").getHoldbackPurposeName().isEmpty());
    }

    @Test
    public void getHoldbackPurposeFromXmlTest() throws IOException {
        assertEquals("Dansk Dramatik & Fiktion", HoldbackDatePicker.getInstance().getHoldbackDateForRecord(tvValues1, "ds.tv").getHoldbackPurposeName());
    }

    @Test
    public void getNoHoldbackDateFromXmlTest() throws IOException {
        assertEquals("9999-01-01T00:00:00Z", HoldbackDatePicker.getInstance().getHoldbackDateForRecord(tvValues2, "ds.tv").getHoldbackDate());
    }

    @Test
    public void radioHoldbackTest() throws IOException {
        HoldbackObject holdbackObject =  HoldbackDatePicker.getInstance().getHoldbackDateForRecord(radioValues, "ds.radio");

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
