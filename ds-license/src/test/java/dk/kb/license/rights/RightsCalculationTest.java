package dk.kb.license.rights;

import dk.kb.license.Util;
import dk.kb.license.config.ServiceConfig;
import dk.kb.license.facade.RightsModuleFacade;
import dk.kb.license.model.v1.IdTypeEnumDto;
import dk.kb.license.model.v1.PlatformEnumDto;
import dk.kb.license.model.v1.RightsCalculationInputDto;
import dk.kb.license.model.v1.RightsCalculationOutputDto;
import dk.kb.license.storage.BaseModuleStorage;
import dk.kb.license.storage.DsLicenseUnitTestUtil;
import dk.kb.license.storage.RightsModuleStorageForUnitTest;
import dk.kb.license.util.H2DbUtil;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RightsCalculationTest extends DsLicenseUnitTestUtil {
    private final static Logger log = LoggerFactory.getLogger(RightsCalculationTest.class);

    @BeforeAll
    public static void beforeClass() throws IOException, SQLException {
        ServiceConfig.initialize("conf/ds-license*.yaml", "src/test/resources/ds-license-integration-test.yaml");
        H2DbUtil.createEmptyH2DBFromDDL(URL,DRIVER,USERNAME,PASSWORD, List.of("ddl/rightsmodule_create_h2_unittest.ddl"));
        BaseModuleStorage.initialize(DRIVER, URL, USERNAME, PASSWORD);
    }

    @BeforeEach
    public void beforeEach() throws SQLException {
        try (RightsModuleStorageForUnitTest storage = new RightsModuleStorageForUnitTest()){
            List<String> tables = new ArrayList<String>();
            tables.add("RESTRICTED_IDS");
            tables.add("DR_HOLDBACK_RANGES");
            tables.add("DR_HOLDBACK_RULES");       
                       
            storage.clearTableRecords(tables);
        } catch (Exception e) {
            throw e;
        }
        H2DbUtil.createEmptyH2DBFromDDL(URL,DRIVER,USERNAME,PASSWORD, List.of("ddl/rightsmodule_default_holdbackrulesdata.sql", "ddl/rightsmodule_default_holdbackrangesdata.sql"));
    }

    @Test
    public void testHoldbackEducationEdgeCase() throws SQLException, IllegalAccessException {
        RightsCalculationInputDto alwaysEducationRecord = new RightsCalculationInputDto("testRecord1", "2016-01-20T10:34:42+0100",
                PlatformEnumDto.DRARKIV,4411, 6000, 3190, 1000, "1000", "Program 1", "9283748300", "ds.tv");

        RightsCalculationOutputDto output = RightsModuleFacade.calculateRightsForRecord(alwaysEducationRecord);

        assertEquals("Undervisning", output.getDr().getHoldbackName());
    }

    @Test
    public void testHoldbackTrailersEdgeCase() throws SQLException, IllegalAccessException {
        RightsCalculationInputDto alwaysEducationRecord = new RightsCalculationInputDto("testRecord1", "2016-01-20T10:34:42+0100",
                PlatformEnumDto.DRARKIV,7000, 1000, 3190, 1000, "1000", "Program 1", "9283748300", "ds.tv");

        RightsCalculationOutputDto output = RightsModuleFacade.calculateRightsForRecord(alwaysEducationRecord);

        assertEquals("", output.getDr().getHoldbackName());
        assertEquals("9999-01-01T00:00:00Z", output.getDr().getHoldbackExpiredDate());
    }

    @Test
    public void badHoldbackRecordTest() throws SQLException, IllegalAccessException {
        RightsCalculationInputDto badHoldbackValues = new RightsCalculationInputDto("badValues", "2016-01-06T18:08:17+0100", PlatformEnumDto.DRARKIV, 1800
                , 0, 3100, 2211, "1000", "Record with bad holdback values", "9283748300", "ds.tv");

        RightsCalculationOutputDto output = RightsModuleFacade.calculateRightsForRecord(badHoldbackValues);

        assertEquals("9999-01-01T00:00:00Z", output.getDr().getHoldbackExpiredDate());
    }

    @Test
    public void testHoldbackDate() throws SQLException, IllegalAccessException {

        RightsCalculationInputDto tenYearHoldbackRecord = new RightsCalculationInputDto("testRecord1", "2016-01-20T10:34:42+0100",
                PlatformEnumDto.DRARKIV,4411, 0, 3190, 1000, "1000", "Program 1", "9283748300", "ds.tv");

        RightsCalculationOutputDto output = RightsModuleFacade.calculateRightsForRecord(tenYearHoldbackRecord);

        assertEquals("2027-01-01T00:00:00Z", output.getDr().getHoldbackExpiredDate());
    }

    @Test
    public void testHoldbackDateNews() throws SQLException, IllegalAccessException {
        RightsCalculationInputDto newsRecord = new RightsCalculationInputDto("testRecord1", "2016-01-01T10:34:42+0100",
                PlatformEnumDto.DRARKIV,1100, 0, 1200, 1000, "1000", "Program 1", "9283748300", "ds.tv");

        RightsCalculationOutputDto output = RightsModuleFacade.calculateRightsForRecord(newsRecord);

        assertEquals("2016-01-31T09:34:42Z", output.getDr().getHoldbackExpiredDate());
    }

    @Test
    public void testHoldbackName() throws SQLException, IllegalAccessException {

        RightsCalculationInputDto tenYearHoldbackRecord = new RightsCalculationInputDto("testRecord1", "2016-01-20T10:34:42+0100",
                PlatformEnumDto.DRARKIV,4411, 0, 3190, 1000, "1000", "Program 1", "9283748300", "ds.tv");

        RightsCalculationOutputDto output = RightsModuleFacade.calculateRightsForRecord(tenYearHoldbackRecord);

        assertEquals("Dansk Dramatik & Fiktion", output.getDr().getHoldbackName());
    }

    @Test
    public void testHoldbackForeign() throws SQLException, IllegalAccessException {

        RightsCalculationInputDto foreignRecord = new RightsCalculationInputDto("testRecord1", "2016-01-20T10:34:42+0100",
                PlatformEnumDto.DRARKIV,4411, 0, 3190, 5000, "5000", "Program 1", "9283748300", "ds.tv");

        RightsCalculationOutputDto output = RightsModuleFacade.calculateRightsForRecord(foreignRecord);

        assertEquals("Udenlandsk Dramatik & Fiktion", output.getDr().getHoldbackName());
        assertEquals("9999-01-01T00:00:00Z", output.getDr().getHoldbackExpiredDate());
    }

    @Test
    public void holdbackJanuaryEdgeTest() throws SQLException, IllegalAccessException {
        RightsCalculationInputDto yearlyHoldback1 = new RightsCalculationInputDto("yearlyHoldback1","1999-01-01T10:30:00+0100", PlatformEnumDto.DRARKIV,
                4411, 0, 3190, 1000, "1000", "Program 1", "9283748300", "ds.tv" );
        RightsCalculationOutputDto output1 = RightsModuleFacade.calculateRightsForRecord(yearlyHoldback1);
        // 1st of January test
        assertEquals("2010-01-01T00:00:00Z", output1.getDr().getHoldbackExpiredDate());
    }

    @Test
    public void holdbackDecemberEdgeTest() throws SQLException, IllegalAccessException {
        RightsCalculationInputDto yearlyHoldback2 = new RightsCalculationInputDto("yearlyHoldback1", "2010-12-31T10:00:00+0100", PlatformEnumDto.DRARKIV,
                4411, 0, 3190, 1000, "1000", "Program 1", "9283748300", "ds.tv");
        RightsCalculationOutputDto output2 = RightsModuleFacade.calculateRightsForRecord(yearlyHoldback2);
        // 31st of December test
        assertEquals("2021-01-01T00:00:00Z", output2.getDr().getHoldbackExpiredDate());
    }

    @Test
    public void holdbackYearlyRandomDateTest() throws SQLException, IllegalAccessException {
        RightsCalculationInputDto yearlyHoldback3 = new RightsCalculationInputDto("yearlyHoldback1","1990-06-20T10:00:00+0100", PlatformEnumDto.DRARKIV,
                4411, 0, 3190, 1000, "1000", "Program 1", "9283748300", "ds.tv");
        RightsCalculationOutputDto output3 = RightsModuleFacade.calculateRightsForRecord(yearlyHoldback3);
        // Random date in June test
        assertEquals("2001-01-01T00:00:00Z", output3.getDr().getHoldbackExpiredDate());
    }

    @Test
    public void holdbackRadioTest() throws SQLException, IllegalAccessException {
        RightsCalculationInputDto yearlyHoldback3 = new RightsCalculationInputDto("radioHoldback","1990-06-20T10:00:00+0100", PlatformEnumDto.DRARKIV,
                4411, 0, 3190, 1000, "1000", "Program 1", "9283748300", "ds.radio");
        RightsCalculationOutputDto radioHoldback = RightsModuleFacade.calculateRightsForRecord(yearlyHoldback3);
        assertEquals("1994-01-01T00:00:00Z", radioHoldback.getDr().getHoldbackExpiredDate());
    }


    @Test
    public void restrictedDrProductionIdTest() throws SQLException, IllegalAccessException {
        try ( RightsModuleStorageForUnitTest storage = new RightsModuleStorageForUnitTest()) {
            storage.createRestrictedId("1234567890", IdTypeEnumDto.DR_PRODUCTION_ID.getValue(), PlatformEnumDto.DRARKIV.getValue(), "Not allowed dr production ID");
            storage.commit();
        } catch (Exception e) {
            throw e;
        }

        RightsCalculationInputDto drProductionIdRestrictedEntry = new RightsCalculationInputDto("Restricted DR Production ID","1990-06-20T10:00:00+0100",
                PlatformEnumDto.DRARKIV,
                4411, 0, 3190, 1000, "1000", "Program 1", "1234567890", "ds.tv");
        RightsCalculationOutputDto restrictedDrProductionID = RightsModuleFacade.calculateRightsForRecord(drProductionIdRestrictedEntry);

        assertTrue(restrictedDrProductionID.getDr().getDrIdRestricted());
    }

    @Test
    public void restrictedDsIdTest() throws SQLException, IllegalAccessException {
        try ( RightsModuleStorageForUnitTest storage = new  RightsModuleStorageForUnitTest()) {
            storage.createRestrictedId("restrictedId",IdTypeEnumDto.DS_ID.getValue(), PlatformEnumDto.DRARKIV.getValue(),"dangerous ID");
            storage.commit();
        } catch (Exception e) {
            throw e;
        }

        RightsCalculationInputDto restrictedDsId = new RightsCalculationInputDto("restrictedId","1990-06-20T10:00:00+0100",
                PlatformEnumDto.DRARKIV,
                4411, 0, 3190, 1000, "1000", "Program 1", "9283748300", "ds.tv");
        RightsCalculationOutputDto restrictedDsID = RightsModuleFacade.calculateRightsForRecord(restrictedDsId);

        assertTrue(restrictedDsID.getDr().getDsIdRestricted());
    }
    @Test
    public void restrictedTitleTest() throws SQLException, IllegalAccessException {
        try ( RightsModuleStorageForUnitTest storage = new  RightsModuleStorageForUnitTest()) {
            storage.createRestrictedId("Restricted Test Title",IdTypeEnumDto.STRICT_TITLE.getValue(), PlatformEnumDto.DRARKIV.getValue(), "This title can never be shown");
            storage.commit();
        } catch (Exception e) {
            throw e;
        }

        RightsCalculationInputDto restrictedTitleRecord = new RightsCalculationInputDto("restrictedId","1990-06-20T10:00:00+0100",
                PlatformEnumDto.DRARKIV,
                4411, 0, 3190, 1000, "1000", "Restricted Test Title", "9283748300", "ds.tv");
        RightsCalculationOutputDto restrictedTitleOutput = RightsModuleFacade.calculateRightsForRecord(restrictedTitleRecord);

        assertTrue(restrictedTitleOutput.getDr().getTitleRestricted());
    }

    @Test
    public void allowedProductionCodeFromMetadataTest() throws SQLException, IllegalAccessException {
        try ( RightsModuleStorageForUnitTest storage = new  RightsModuleStorageForUnitTest()) {
            storage.createRestrictedId("1000",IdTypeEnumDto.OWNPRODUCTION_CODE.getValue(), PlatformEnumDto.DRARKIV.getValue(),"1000 equals ownproduction");
            storage.commit();
        } catch (Exception e) {
            throw e;
        }

        RightsCalculationInputDto allowedOwnProductionCode = new RightsCalculationInputDto("restrictedId","1990-06-20T10:00:00+0100",
                PlatformEnumDto.DRARKIV,
                4411, 0, 3190, 1000, "1000", "Random program", "9283748300", "ds.tv");
        RightsCalculationOutputDto allowedOwnProduction = RightsModuleFacade.calculateRightsForRecord(allowedOwnProductionCode);

        assertTrue(allowedOwnProduction.getDr().getProductionCodeAllowed());
    }

    @Test
    public void inputValidationTestTopLevel() {
        RightsCalculationInputDto faultyInput = new RightsCalculationInputDto(null,"1990-06-20T10:00:00+0100",
                PlatformEnumDto.DRARKIV,
                4411, 0, 3190, 1000, "1000", "Random program", "9283748300", "ds.tv");

        InvalidArgumentServiceException exception = assertThrows(InvalidArgumentServiceException.class, () -> RightsModuleFacade.calculateRightsForRecord(faultyInput));
        assertEquals("Field 'recordId' in class dk.kb.license.model.v1.RightsCalculationInputDto is null.", exception.getMessage());
    }

    @Test
    public void inputValidationTestNested() {
        RightsCalculationInputDto faultyInput = new RightsCalculationInputDto("TestId","1990-06-20T10:00:00+0100",
                PlatformEnumDto.DRARKIV,
                4411, 0, 3190, 1000, null, "Random program", "9283748300", "ds.tv");

        InvalidArgumentServiceException exception = assertThrows(InvalidArgumentServiceException.class, () -> RightsModuleFacade.calculateRightsForRecord(faultyInput));
        assertEquals("Field 'productionCode' in class dk.kb.license.model.v1.RestrictionsCalculationInputDto is null.", exception.getMessage());
    }

    @Test
    public void testProductionIdValidation(){
        // Append zero and strip to a length of 10 digits
        assertEquals("1234567890", Util.validateDrProductionIdFormat("000123456789"));
        assertEquals("0012345670", Util.validateDrProductionIdFormat("000001234567"));

    }

    @Test
    public void testShortErrorProductionId(){
        InvalidArgumentServiceException exception = assertThrows(InvalidArgumentServiceException.class, () -> Util.validateDrProductionIdFormat("12345"));
        assertEquals("The input production ID: '12345' got formattet to '123450' which is shorter than 8 digits. This is not an allowed production ID.", exception.getMessage());
    }

    @Test
    public void testLongErrorProductionId(){
        InvalidArgumentServiceException exception = assertThrows(InvalidArgumentServiceException.class, () -> Util.validateDrProductionIdFormat("1234567890123"));
        assertEquals("The input production ID: '1234567890123' got formattet to '12345678901230' which is longer than 10 digits. This is not an allowed production ID.",
                exception.getMessage());
    }
}
