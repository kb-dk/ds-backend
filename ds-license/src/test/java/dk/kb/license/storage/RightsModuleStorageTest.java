package dk.kb.license.storage;

import dk.kb.license.config.ServiceConfig;
import dk.kb.license.model.v1.DrHoldbackRuleOutputDto;
import dk.kb.license.model.v1.IdTypeEnumDto;
import dk.kb.license.model.v1.PlatformEnumDto;
import dk.kb.license.model.v1.RestrictedIdOutputDto;
import dk.kb.license.util.H2DbUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RightsModuleStorageTest extends DsLicenseUnitTestUtil {

    protected static RightsModuleStorageForUnitTest storage = null;


    @BeforeAll
    public static void beforeClass() throws IOException, SQLException {

        ServiceConfig.initialize("conf/ds-license*.yaml", "src/test/resources/ds-license-integration-test.yaml");
        BaseModuleStorage.initialize(DRIVER, URL, USERNAME, PASSWORD);

        H2DbUtil.createEmptyH2DBFromDDL(URL, DRIVER, USERNAME, PASSWORD, List.of("ddl/rightsmodule_create_h2_unittest.ddl"));
        storage = new RightsModuleStorageForUnitTest();
    }

    /*
     * Delete all records between each unittest. The clearTableRecords is only called from here.
     * The facade class is responsible for committing transactions. So clean up between unittests.
     */
    @BeforeEach
    public void beforeEach() throws SQLException {
        ArrayList<String> tables = new ArrayList<>();
        tables.add("RESTRICTED_IDS");
        tables.add("DR_HOLDBACK_RANGES");
        tables.add("DR_HOLDBACK_RULES");
        storage.clearTableRecords(tables);
    }

    @Test
    public void testRestrictedIdCRUD() throws SQLException {
        String idValue = "test1234";
        String idType = IdTypeEnumDto.DR_PRODUCTION_ID.getValue();
        String platform = PlatformEnumDto.DRARKIV.getValue();
        String comment = "a comment";

        long id = storage.createRestrictedId(idValue, idType, platform, comment);
        RestrictedIdOutputDto retreivedFromStorage = storage.getRestrictedId(idValue, idType, platform);
        assertNotNull(retreivedFromStorage);
        assertEquals(idValue, retreivedFromStorage.getIdValue());
        assertEquals(idType, retreivedFromStorage.getIdType().getValue());
        assertEquals(platform, retreivedFromStorage.getPlatform().getValue());
        assertEquals(comment, retreivedFromStorage.getComment());

        String new_comment = "another comment";

        storage.updateRestrictedIdComment(id, new_comment);
        retreivedFromStorage = storage.getRestrictedId(idValue, idType, platform);
        assertNotNull(retreivedFromStorage);
        assertEquals(idValue, retreivedFromStorage.getIdValue());
        assertEquals(idType, retreivedFromStorage.getIdType().getValue());
        assertEquals(platform, retreivedFromStorage.getPlatform().getValue());
        assertEquals(new_comment, retreivedFromStorage.getComment());

        storage.deleteRestrictedId(idValue, idType, platform);
        assertNull(storage.getRestrictedId(idValue, idType, platform));
    }

    @Test
    public void testRestrictedIdSearch() throws SQLException {
        storage.createRestrictedId("test1", IdTypeEnumDto.DS_ID.getValue(), PlatformEnumDto.DRARKIV.getValue(), "");
        storage.createRestrictedId("test2", IdTypeEnumDto.DS_ID.getValue(), PlatformEnumDto.GENERIC.getValue(), "");
        storage.createRestrictedId("test3", IdTypeEnumDto.DS_ID.getValue(), PlatformEnumDto.DRARKIV.getValue(), "");
        storage.createRestrictedId("test4", IdTypeEnumDto.STRICT_TITLE.getValue(), PlatformEnumDto.DRARKIV.getValue(), "");
        storage.createRestrictedId("test5", IdTypeEnumDto.STRICT_TITLE.getValue(), PlatformEnumDto.GENERIC.getValue(), "");

        assertEquals(2, storage.getAllRestrictedIds(IdTypeEnumDto.DS_ID.getValue(), PlatformEnumDto.DRARKIV.getValue()).size());
    }

    @Test
    public void testUniqueRestrictedID() throws SQLException {
        String idValue = "test12345";
        String idType = "dr_produktions_id";
        String platform = "dr";
        String comment = "a comment";
        String modified_by = "user1";
        long modified_time = 1739439979L;

        storage.createRestrictedId(idValue, idType, platform, comment);

        assertThrows(SQLException.class, () -> storage.createRestrictedId(idValue, idType, platform, comment));
    }

    @Test
    public void getRestrictedIdByIdValue_whenDsId_thenReturnComment() throws SQLException {
        String dsId = "ds.tv:oai:io:7cb60d39-effd-419c-9bac-881b7b7eb10c";
        String expectedComment = "Test comment";

        storage.createRestrictedId(dsId, IdTypeEnumDto.DS_ID.getValue(), PlatformEnumDto.DRARKIV.getValue(), expectedComment);

        String actualComment = storage.getRestrictedIdCommentByIdValue(dsId);

        assertEquals(expectedComment, actualComment);
    }

    @Test
    public void getRestrictedIdByIdValue_whenNotFoundDsId_thenReturnNull() throws SQLException {
        String actualComment = storage.getRestrictedIdCommentByIdValue("1");

        assertNull(actualComment);
    }

    @Test
    public void testDrHoldbackRuleCRUD() throws SQLException {
        String drHoldbackValue = "2.02";
        String name = "Aktualitet & Debat";
        int days = 100;

        storage.createDrHoldbackRule(drHoldbackValue, name, 100);
        assertEquals(days, storage.getDrHoldbackDaysFromValue(drHoldbackValue));
        assertEquals(days, storage.getDrHoldbackDaysFromName(name));
        DrHoldbackRuleOutputDto holdbackFromStorage = storage.getDrHoldbackRuleFromValue(drHoldbackValue);
        assertEquals(name, holdbackFromStorage.getName());

        days = 200;
        storage.updateDrHoldbackDaysFromDrHoldbackValue(drHoldbackValue, days);
        assertEquals(days, storage.getDrHoldbackDaysFromValue(drHoldbackValue));
        assertEquals(days, storage.getDrHoldbackDaysFromName(name));

        days = 300;
        storage.updateDrHoldbackDaysFromName(name, days);
        assertEquals(days, storage.getDrHoldbackDaysFromValue(drHoldbackValue));
        assertEquals(days, storage.getDrHoldbackDaysFromName(name));

        assertEquals(1, storage.getAllDrHoldbackRules().size());
        storage.deleteDrHoldbackRule(drHoldbackValue);
        assertEquals(-1, storage.getDrHoldbackDaysFromValue(drHoldbackValue));
        assertEquals(-1, storage.getDrHoldbackDaysFromName(name));
        assertEquals(0, storage.getAllDrHoldbackRules().size());
    }

    @Test
    public void testCreateDrHoldbackRanges() throws SQLException {
        storage.createDrHoldbackRule("test1", "Test", 100);
        storage.createDrHoldbackRule("test2", "Test2", 200);

        storage.createDrHoldbackRange(1000, 1000, 1200, 1900, "test1");
        storage.createDrHoldbackRange(2000, 3000, 2200, 2900, "test2");
        storage.createDrHoldbackRange(2000, 3000, 3200, 3900, "test2");


        assertEquals("test1", storage.getDrHoldbackValueFromContentAndForm(1000, 1200));
        assertEquals("test2", storage.getDrHoldbackValueFromContentAndForm(2500, 2900));
        assertEquals(1, storage.getDrHoldbackRangesForDrHoldbackValue("test1").size());
        assertEquals(2, storage.getDrHoldbackRangesForDrHoldbackValue("test2").size());
        assertNull(storage.getDrHoldbackValueFromContentAndForm(2500, 9999));
        assertNull(storage.getDrHoldbackValueFromContentAndForm(9999, 1200));
        assertNull(storage.getDrHoldbackValueFromContentAndForm(9999, 9999));
    }

    @Test
    public void testDeleteDrHoldbackRanges() throws SQLException {
        storage.createDrHoldbackRule("test1", "Test", 100);
        storage.createDrHoldbackRule("test2", "Test2", 200);

        storage.createDrHoldbackRange(1000, 1000, 1200, 1900, "test1");
        storage.createDrHoldbackRange(2000, 3000, 2200, 2900, "test2");

        assertEquals("test1", storage.getDrHoldbackValueFromContentAndForm(1000, 1200));
        assertEquals("test2", storage.getDrHoldbackValueFromContentAndForm(2500, 2900));

        storage.deleteRangesForDrHoldbackValue("test1");

        assertNull(storage.getDrHoldbackValueFromContentAndForm(1000, 1200));
        assertEquals("test2", storage.getDrHoldbackValueFromContentAndForm(2500, 2900));
    }

    @Test
    public void testPerformStorageAction() throws SQLException {
        RestrictedIdOutputDto result = BaseModuleStorage.performStorageAction("Testing", RightsModuleStorage.class, storage -> {
            ((RightsModuleStorage) storage).createRestrictedId("test1", IdTypeEnumDto.DS_ID.getValue(), PlatformEnumDto.DRARKIV.getValue(), "comment");
            return ((RightsModuleStorage) storage).getRestrictedId("test1", IdTypeEnumDto.DS_ID.getValue(), PlatformEnumDto.DRARKIV.getValue());
        });
        assertEquals("test1", result.getIdValue());
        assertEquals(IdTypeEnumDto.DS_ID, result.getIdType());
        assertEquals(PlatformEnumDto.DRARKIV, result.getPlatform());

    }

}
