package dk.kb.license.rights;

import dk.kb.license.config.ServiceConfig;
import dk.kb.license.facade.RightsModuleFacade;
import dk.kb.license.model.v1.RightsCalculationInputDto;
import dk.kb.license.model.v1.RightsCalculationOutputDto;
import dk.kb.license.storage.BaseModuleStorage;
import dk.kb.license.storage.DsLicenseUnitTestUtil;
import dk.kb.license.storage.RightsModuleStorage;
import dk.kb.license.util.H2DbUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RightsCalculationTest extends DsLicenseUnitTestUtil {
    private final static Logger log = LoggerFactory.getLogger(RightsCalculationTest.class);

    protected static RightsModuleStorage storage = null;

    @BeforeAll
    public static void beforeClass() throws IOException, SQLException {
        ServiceConfig.initialize("conf/ds-license*.yaml", "src/test/resources/ds-license-integration-test.yaml");

        BaseModuleStorage.initialize(DRIVER, URL, USERNAME, PASSWORD);
        H2DbUtil.deleteEntriesInTable(URL, USERNAME, PASSWORD, "DR_HOLDBACK_MAP");
        H2DbUtil.deleteEntriesInTable(URL, USERNAME, PASSWORD, "DR_HOLDBACK_RULES");
        H2DbUtil.deleteEntriesInTable(URL, USERNAME, PASSWORD, "RESTRICTED_IDS");
        H2DbUtil.dropIndex(URL, USERNAME, PASSWORD, "unique_restricted_id");
        // "ddl/rightsmodule_default_holdbackdata.sql"
        H2DbUtil.createEmptyH2DBFromDDL(URL,DRIVER,USERNAME,PASSWORD, List.of("ddl/rightsmodule_create_h2_unittest.ddl", "ddl/rightsmodule_default_holdbackdata.sql"));

        storage = new RightsModuleStorage(false);
        storage.createRestrictedId("1000", "egenproduktions_kode", "dr", "Egenproduktion", "test", 9999999999L);
    }

    @BeforeEach
    public void beforeEach() throws SQLException {
        BaseModuleStorage.initialize(DRIVER, URL, USERNAME, PASSWORD);
        storage = new RightsModuleStorage(false);

    }

    @AfterEach
    public void afterEach() throws SQLException {
        storage.commit();
        storage.close();
    }


    @Test
    public void testHoldbackEducationEdgeCase() throws SQLException {
        RightsCalculationInputDto alwaysEducationRecord = new RightsCalculationInputDto("testRecord1", "2016-01-20T10:34:42+0100",
                RightsCalculationInputDto.PlatformEnum.DRARKIV,4411, 6000, 3190, 1000, "1000", "Program 1", "9283748300");

        RightsCalculationOutputDto output = RightsModuleFacade.calculateRightsForRecord(alwaysEducationRecord);

        assertTrue(output.getDr().getHoldbackName().equals("Undervisning"));
    }

    //@Test
    public void testHoldbackTrailersEdgeCase() throws SQLException {
        RightsCalculationInputDto alwaysEducationRecord = new RightsCalculationInputDto("testRecord1", "2016-01-20T10:34:42+0100",
                RightsCalculationInputDto.PlatformEnum.DRARKIV,7000, 1000, 3190, 1000, "1000", "Program 1", "9283748300");

        RightsCalculationOutputDto output = RightsModuleFacade.calculateRightsForRecord(alwaysEducationRecord);

        assertTrue(output.getDr().getHoldbackName().equals(""));
        assertEquals("9999-01-01T00:00:00Z", output.getDr().getHoldbackExpiredDate());
    }

    @Test
    public void badHoldbackRecordTest() throws SQLException {
        RightsCalculationInputDto badHoldbackValues = new RightsCalculationInputDto("badValues", "2016-01-06T18:08:17+0100", RightsCalculationInputDto.PlatformEnum.DRARKIV, 1800
                , 0, 3100, 2211, "1000", "Record with bad holdback values", "9283748300");

        RightsCalculationOutputDto output = RightsModuleFacade.calculateRightsForRecord(badHoldbackValues);

        assertEquals("9999-01-01T00:00:00Z", output.getDr().getHoldbackExpiredDate());
    }

    @Test
    public void testHoldbackDate() throws SQLException {

        RightsCalculationInputDto tenYearHoldbackRecord = new RightsCalculationInputDto("testRecord1", "2016-01-20T10:34:42+0100",
                RightsCalculationInputDto.PlatformEnum.DRARKIV,4411, 0, 3190, 1000, "1000", "Program 1", "9283748300");

        RightsCalculationOutputDto output = RightsModuleFacade.calculateRightsForRecord(tenYearHoldbackRecord);

        assertEquals("2027-01-01T00:00:00Z", output.getDr().getHoldbackExpiredDate());
    }

    @Test
    public void testHoldbackName() throws SQLException {

        RightsCalculationInputDto tenYearHoldbackRecord = new RightsCalculationInputDto("testRecord1", "2016-01-20T10:34:42+0100",
                RightsCalculationInputDto.PlatformEnum.DRARKIV,4411, 0, 3190, 1000, "1000", "Program 1", "9283748300");

        RightsCalculationOutputDto output = RightsModuleFacade.calculateRightsForRecord(tenYearHoldbackRecord);

        assertEquals("Dansk Dramatik & Fiktion", output.getDr().getHoldbackName());
    }

    @Test
    public void testHoldbackForeign() throws SQLException {

        RightsCalculationInputDto foreignRecord = new RightsCalculationInputDto("testRecord1", "2016-01-20T10:34:42+0100",
                RightsCalculationInputDto.PlatformEnum.DRARKIV,4411, 0, 3190, 5000, "5000", "Program 1", "9283748300");

        RightsCalculationOutputDto output = RightsModuleFacade.calculateRightsForRecord(foreignRecord);

        assertEquals("Udenlandsk Dramatik & Fiktion", output.getDr().getHoldbackName());
        assertEquals("9999-01-01T00:00:00Z", output.getDr().getHoldbackExpiredDate());
    }

    @Test
    public void holdbackJanuaryEdgeTest() throws SQLException {
        RightsCalculationInputDto yearlyHoldback1 = new RightsCalculationInputDto("yearlyHoldback1","1999-01-01T10:30:00+0100", RightsCalculationInputDto.PlatformEnum.DRARKIV,
                4411, 0, 3190, 1000, "1000", "Program 1", "9283748300" );
        RightsCalculationOutputDto output1 = RightsModuleFacade.calculateRightsForRecord(yearlyHoldback1);
        // 1st of January test
        assertEquals("2010-01-01T00:00:00Z", output1.getDr().getHoldbackExpiredDate());
    }

    @Test
    public void holdbackDecemberEdgeTest() throws SQLException {
        RightsCalculationInputDto yearlyHoldback2 = new RightsCalculationInputDto("yearlyHoldback1", "2010-12-31T10:00:00+0100", RightsCalculationInputDto.PlatformEnum.DRARKIV,
                4411, 0, 3190, 1000, "1000", "Program 1", "9283748300" );
        RightsCalculationOutputDto output2 = RightsModuleFacade.calculateRightsForRecord(yearlyHoldback2);
        // 31st of December test
        assertEquals("2021-01-01T00:00:00Z", output2.getDr().getHoldbackExpiredDate());
    }

    @Test
    public void holdbackYearlyRandomDateTest() throws SQLException {
        RightsCalculationInputDto yearlyHoldback3 = new RightsCalculationInputDto("yearlyHoldback1","1990-06-20T10:00:00+0100", RightsCalculationInputDto.PlatformEnum.DRARKIV,
                4411, 0, 3190, 1000, "1000", "Program 1", "9283748300" );
        RightsCalculationOutputDto output3 = RightsModuleFacade.calculateRightsForRecord(yearlyHoldback3);
        // Random date in June test
        assertEquals("2001-01-01T00:00:00Z", output3.getDr().getHoldbackExpiredDate());
    }



}
