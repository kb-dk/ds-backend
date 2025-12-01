package dk.kb.license.rights;

import dk.kb.license.Util;
import dk.kb.license.config.ServiceConfig;
import dk.kb.license.facade.RightsModuleFacade;
import dk.kb.license.model.v1.*;
import dk.kb.license.storage.BaseModuleStorage;
import dk.kb.license.storage.DsLicenseUnitTestUtil;
import dk.kb.license.storage.RightsModuleStorageForUnitTest;
import dk.kb.license.util.H2DbUtil;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RightsCalculationTest extends DsLicenseUnitTestUtil {
    private final static Logger log = LoggerFactory.getLogger(RightsCalculationTest.class);

    @BeforeAll
    public static void beforeClass() throws IOException, SQLException {
        ServiceConfig.initialize("conf/ds-license*.yaml", "src/test/resources/ds-license-integration-test.yaml");
        H2DbUtil.createEmptyH2DBFromDDL(URL, DRIVER, USERNAME, PASSWORD, List.of("ddl/rightsmodule_create_h2_unittest.ddl"));
        BaseModuleStorage.initialize(DRIVER, URL, USERNAME, PASSWORD);
    }

    @BeforeEach
    public void beforeEach() throws SQLException {
        try (RightsModuleStorageForUnitTest storage = new RightsModuleStorageForUnitTest()) {
            List<String> tables = new ArrayList<String>();
            tables.add("RESTRICTED_IDS");
            tables.add("DR_HOLDBACK_RANGES");
            tables.add("DR_HOLDBACK_RULES");

            storage.clearTableRecords(tables);
        } catch (Exception e) {
            throw e;
        }
        H2DbUtil.createEmptyH2DBFromDDL(URL, DRIVER, USERNAME, PASSWORD, List.of("ddl/rightsmodule_default_holdbackrulesdata.sql", "ddl/rightsmodule_default_holdbackrangesdata.sql"));
    }

    public RightsCalculationInputDto map(String recordId, PlatformEnumDto platform, String startTime,
                                         Integer hensigt, Integer form, Integer indhold, String holdbackCategory, Integer productionCountry, String origin,
                                         String productionCode, String drProductionId, String title) {
        RightsCalculationInputDto rightsCalculationInputDto = new RightsCalculationInputDto();
        rightsCalculationInputDto.setRecordId(recordId);
        rightsCalculationInputDto.setPlatform(platform);
        rightsCalculationInputDto.setStartTime(startTime);

        HoldbackCalculationInputDto holdbackCalculationInputDto = new HoldbackCalculationInputDto();
        holdbackCalculationInputDto.setHensigt(hensigt);
        holdbackCalculationInputDto.setForm(form);
        holdbackCalculationInputDto.setIndhold(indhold);
        holdbackCalculationInputDto.setHoldbackCategory(holdbackCategory);
        holdbackCalculationInputDto.setProductionCountry(productionCountry);
        holdbackCalculationInputDto.setOrigin(origin);

        rightsCalculationInputDto.setHoldbackInput(holdbackCalculationInputDto);

        RestrictionsCalculationInputDto restrictionsCalculationInputDto = new RestrictionsCalculationInputDto();
        restrictionsCalculationInputDto.setRecordId(recordId);
        restrictionsCalculationInputDto.setProductionCode(productionCode);
        restrictionsCalculationInputDto.setDrProductionId(drProductionId);
        restrictionsCalculationInputDto.setTitle(title);

        rightsCalculationInputDto.setRestrictionsInput(restrictionsCalculationInputDto);

        return rightsCalculationInputDto;
    }

    @Test
    public void testHoldbackEducationEdgeCase() throws SQLException {
        RightsCalculationInputDto rightsCalculationInputDto = map("testRecord1", PlatformEnumDto.DRARKIV,
                "2016-01-20T10:34:42+0100", 6000, 4411, 3190, null,
                1000, "ds.tv", "1000", "9283748300", "Program 1");

        RightsCalculationOutputDto output = RightsModuleFacade.calculateRightsForRecord(rightsCalculationInputDto);

        assertEquals("Undervisning", output.getDr().getHoldbackName());
    }

    @Test
    public void testHoldbackTrailersEdgeCase() throws SQLException {
        RightsCalculationInputDto alwaysEducationRecord = map("testRecord1", PlatformEnumDto.DRARKIV,
                "2016-01-20T10:34:42+0100", 1000, 7000, 3190, null,
                1000, "ds.tv", "1000", "9283748300", "Program 1");

        RightsCalculationOutputDto output = RightsModuleFacade.calculateRightsForRecord(alwaysEducationRecord);

        assertEquals("", output.getDr().getHoldbackName());
        assertEquals("9999-01-01T00:00:00Z", output.getDr().getHoldbackExpiredDate());
    }

    @Test
    public void badHoldbackRecordTest() throws SQLException {
        RightsCalculationInputDto badHoldbackValues = map("badValues", PlatformEnumDto.DRARKIV,
                "2016-01-06T18:08:17+0100", 1000, 1800, 3100, null,
                2211, "ds.tv", "1000", "9283748300",
                "Record with bad holdback values");

        RightsCalculationOutputDto output = RightsModuleFacade.calculateRightsForRecord(badHoldbackValues);

        assertEquals("9999-01-01T00:00:00Z", output.getDr().getHoldbackExpiredDate());
    }

    @Test
    public void testHoldbackDate() throws SQLException {

        RightsCalculationInputDto tenYearHoldbackRecord = map("testRecord1", PlatformEnumDto.DRARKIV,
                "2016-01-20T10:34:42+0100", 1000, 4411, 3190, null,
                1000, "ds.tv", "1000", "9283748300", "Program 1");

        RightsCalculationOutputDto output = RightsModuleFacade.calculateRightsForRecord(tenYearHoldbackRecord);

        assertEquals("2027-01-01T00:00:00Z", output.getDr().getHoldbackExpiredDate());
    }

    @Test
    public void testHoldbackDateNews() throws SQLException {
        RightsCalculationInputDto newsRecord = map("testRecord1", PlatformEnumDto.DRARKIV,
                "2016-01-01T10:34:42+0100", 1000, 1100, 1200, null,
                1000, "ds.tv", "1000", "9283748300", "Program 1");

        RightsCalculationOutputDto output = RightsModuleFacade.calculateRightsForRecord(newsRecord);

        assertEquals("2016-01-31T09:34:42Z", output.getDr().getHoldbackExpiredDate());
    }

    @Test
    public void calculateRightsForRecord_whenDsTvDrArchiveSupplementaryRightsMetadata_thenOutputIsPopulated() throws SQLException {
        RightsCalculationInputDto tenYearHoldbackRecord = map("testRecord1", PlatformEnumDto.DRARKIV,
                "2016-01-20T10:34:42+0100", null, 1000, null,
                "Dansk Dramatik & Fiktion", 1000, "ds.tv", "1000",
                "9283748300", "Program 1");

        RightsCalculationOutputDto output = RightsModuleFacade.calculateRightsForRecord(tenYearHoldbackRecord);

        assertEquals(false, output.getDr().getDsIdRestricted());
        assertEquals("Dansk Dramatik & Fiktion", output.getDr().getHoldbackName());
        assertEquals("2027-01-01T00:00:00Z", output.getDr().getHoldbackExpiredDate());
        assertEquals(false, output.getDr().getProductionCodeAllowed());
        assertEquals(false, output.getDr().getDrIdRestricted());
        assertEquals(false, output.getDr().getTitleRestricted());
    }

    @Test
    public void testHoldbackName() throws SQLException {
        RightsCalculationInputDto tenYearHoldbackRecord = map("testRecord1", PlatformEnumDto.DRARKIV,
                "2016-01-20T10:34:42+0100", 1000, 4411, 3190, null,
                1000, "ds.tv", "1000", "9283748300", "Program 1");

        RightsCalculationOutputDto output = RightsModuleFacade.calculateRightsForRecord(tenYearHoldbackRecord);

        assertEquals(false, output.getDr().getDsIdRestricted());
        assertEquals("Dansk Dramatik & Fiktion", output.getDr().getHoldbackName());
        assertEquals("2027-01-01T00:00:00Z", output.getDr().getHoldbackExpiredDate());
        assertEquals(false, output.getDr().getProductionCodeAllowed());
        assertEquals(false, output.getDr().getDrIdRestricted());
        assertEquals(false, output.getDr().getTitleRestricted());
    }

    @Test
    public void testHoldbackForeign() throws SQLException {

        RightsCalculationInputDto foreignRecord = map("testRecord1", PlatformEnumDto.DRARKIV,
                "2016-01-20T10:34:42+0100", 1000, 4411, 3190, null,
                5000, "ds.tv", "5000", "9283748300", "Program 1");

        RightsCalculationOutputDto output = RightsModuleFacade.calculateRightsForRecord(foreignRecord);

        assertEquals("Udenlandsk Dramatik & Fiktion", output.getDr().getHoldbackName());
        assertEquals("3017-01-01T00:00:00Z", output.getDr().getHoldbackExpiredDate()); // 1000 years, should never be released to the public
    }

    @Test
    public void holdbackJanuaryEdgeTest() throws SQLException {
        RightsCalculationInputDto yearlyHoldback1 = map("yearlyHoldback1", PlatformEnumDto.DRARKIV,
                "1999-01-01T10:30:00+0100", 1000, 4411, 3190, null,
                1000, "ds.tv", "1000", "9283748300", "Program 1");

        RightsCalculationOutputDto output1 = RightsModuleFacade.calculateRightsForRecord(yearlyHoldback1);
        // 1st of January test
        assertEquals("2010-01-01T00:00:00Z", output1.getDr().getHoldbackExpiredDate());
    }

    @Test
    public void holdbackDecemberEdgeTest() throws SQLException {
        RightsCalculationInputDto yearlyHoldback2 = map("yearlyHoldback1", PlatformEnumDto.DRARKIV,
                "2010-12-31T10:00:00+0100", 1000, 4411, 3190, null,
                1000, "ds.tv", "1000", "9283748300", "Program 1");

        RightsCalculationOutputDto output2 = RightsModuleFacade.calculateRightsForRecord(yearlyHoldback2);

        // 31st of December test
        assertEquals("2021-01-01T00:00:00Z", output2.getDr().getHoldbackExpiredDate());
    }

    @Test
    public void holdbackYearlyRandomDateTest() throws SQLException {
        RightsCalculationInputDto yearlyHoldback3 = map("yearlyHoldback1", PlatformEnumDto.DRARKIV,
                "1990-06-20T10:00:00+0100", 1000, 4411, 3190, null,
                1000, "ds.tv", "1000", "9283748300", "Program 1");

        RightsCalculationOutputDto output3 = RightsModuleFacade.calculateRightsForRecord(yearlyHoldback3);
        // Random date in June test
        assertEquals("2001-01-01T00:00:00Z", output3.getDr().getHoldbackExpiredDate());
    }

    @Test
    public void calculateRightsForRecord_whenDsRadioDrArchiveSupplementaryRightsMetadata_thenOutputIsPopulated() throws SQLException {
        RightsCalculationInputDto yearlyHoldback3 = map("radioHoldback", PlatformEnumDto.DRARKIV,
                "1990-06-20T10:00:00+0100", null, null, null, null,
                null, "ds.radio", null, "9283748300", "Program 1");

        RightsCalculationOutputDto output = RightsModuleFacade.calculateRightsForRecord(yearlyHoldback3);

        assertEquals(false, output.getDr().getDsIdRestricted());
        assertEquals("Radio", output.getDr().getHoldbackName());
        assertEquals("1994-01-01T00:00:00Z", output.getDr().getHoldbackExpiredDate());
        assertEquals(false, output.getDr().getProductionCodeAllowed());
        assertEquals(false, output.getDr().getDrIdRestricted());
        assertEquals(false, output.getDr().getTitleRestricted());
    }

    @Test
    public void holdbackRadioTest() throws SQLException {
        RightsCalculationInputDto yearlyHoldback3 = map("radioHoldback", PlatformEnumDto.DRARKIV,
                "1990-06-20T10:00:00+0100", 1000, 4411, 3190, null,
                1000, "ds.radio", "1000", "9283748300", "Program 1");

        RightsCalculationOutputDto output = RightsModuleFacade.calculateRightsForRecord(yearlyHoldback3);

        assertEquals(false, output.getDr().getDsIdRestricted());
        assertEquals("Radio", output.getDr().getHoldbackName());
        assertEquals("1994-01-01T00:00:00Z", output.getDr().getHoldbackExpiredDate());
        assertEquals(false, output.getDr().getProductionCodeAllowed());
        assertEquals(false, output.getDr().getDrIdRestricted());
        assertEquals(false, output.getDr().getTitleRestricted());
    }

    @Test
    public void restrictedDrProductionIdTest() throws SQLException {
        try (RightsModuleStorageForUnitTest storage = new RightsModuleStorageForUnitTest()) {
            storage.createRestrictedId("1234567890", IdTypeEnumDto.DR_PRODUCTION_ID.getValue(), PlatformEnumDto.DRARKIV.getValue(), "Not allowed dr production ID");
            storage.commit();
        } catch (Exception e) {
            throw e;
        }

        RightsCalculationInputDto drProductionIdRestrictedEntry = map("Restricted DR Production ID",
                PlatformEnumDto.DRARKIV, "1990-06-20T10:00:00+0100", 1000, 4411, 3190,
                null, 1000, "ds.tv", "1000", "1234567890",
                "Program 1");

        RightsCalculationOutputDto restrictedDrProductionID = RightsModuleFacade.calculateRightsForRecord(drProductionIdRestrictedEntry);

        assertTrue(restrictedDrProductionID.getDr().getDrIdRestricted());
    }

    @Test
    public void restrictedDsIdTest() throws SQLException {
        try (RightsModuleStorageForUnitTest storage = new RightsModuleStorageForUnitTest()) {
            storage.createRestrictedId("restrictedId", IdTypeEnumDto.DS_ID.getValue(), PlatformEnumDto.DRARKIV.getValue(), "dangerous ID");
            storage.commit();
        } catch (Exception e) {
            throw e;
        }

        RightsCalculationInputDto restrictedDsId = map("restrictedId", PlatformEnumDto.DRARKIV,
                "1990-06-20T10:00:00+0100", 1000, 4411, 3190, null,
                1000, "ds.tv", "1000", "9283748300", "Program 1");

        RightsCalculationOutputDto restrictedDsID = RightsModuleFacade.calculateRightsForRecord(restrictedDsId);

        assertTrue(restrictedDsID.getDr().getDsIdRestricted());
    }

    @Test
    public void restrictedTitleTest() throws SQLException {
        try (RightsModuleStorageForUnitTest storage = new RightsModuleStorageForUnitTest()) {
            storage.createRestrictedId("Restricted Test Title", IdTypeEnumDto.STRICT_TITLE.getValue(), PlatformEnumDto.DRARKIV.getValue(), "This title can never be shown");
            storage.commit();
        } catch (Exception e) {
            throw e;
        }

        RightsCalculationInputDto restrictedTitleRecord = map("restrictedId", PlatformEnumDto.DRARKIV,
                "1990-06-20T10:00:00+0100", 1000, 4411, 3190, null,
                1000, "ds.tv", "1000", "9283748300",
                "Restricted Test Title");

        RightsCalculationOutputDto restrictedTitleOutput = RightsModuleFacade.calculateRightsForRecord(restrictedTitleRecord);

        assertTrue(restrictedTitleOutput.getDr().getTitleRestricted());
    }

    @Test
    public void allowedProductionCodeFromMetadataTest() throws SQLException {
        try (RightsModuleStorageForUnitTest storage = new RightsModuleStorageForUnitTest()) {
            storage.createRestrictedId("1000", IdTypeEnumDto.OWNPRODUCTION_CODE.getValue(), PlatformEnumDto.DRARKIV.getValue(), "1000 equals ownproduction");
            storage.commit();
        } catch (Exception e) {
            throw e;
        }

        RightsCalculationInputDto allowedOwnProductionCode = map("restrictedId", PlatformEnumDto.DRARKIV,
                "1990-06-20T10:00:00+0100", 1000, 4411, 3190, null,
                1000, "ds.tv", "1000", "9283748300",
                "Random program");
        RightsCalculationOutputDto allowedOwnProduction = RightsModuleFacade.calculateRightsForRecord(allowedOwnProductionCode);

        assertTrue(allowedOwnProduction.getDr().getProductionCodeAllowed());
    }

    @Test
    public void testShortErrorProductionId() {
        assertThrows(InvalidArgumentServiceException.class, () -> Util.validateDrProductionIdFormat("12345"));
    }

    @Test
    public void testInvalidProductionId() {
        assertThrows(InvalidArgumentServiceException.class, () -> Util.validateDrProductionIdFormat("12345abcde"));
    }
}
