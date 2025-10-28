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
    public void createRestrictedId_whenCreatingRestrictedId_thenReturnRestrictedIdOutputDto() throws SQLException {
        // Arrange
        String idValue = "test1234";
        IdTypeEnumDto idTypeEnumDto = IdTypeEnumDto.DR_PRODUCTION_ID;
        PlatformEnumDto platformEnumDto = PlatformEnumDto.DRARKIV;
        String title = "Test title";
        String comment = "a comment";

        // Act
        long id = storage.createRestrictedId(idValue, idTypeEnumDto.name(), platformEnumDto.name(), title, comment);
        RestrictedIdOutputDto restrictedIdOutputDto = storage.getRestrictedId(idValue, idTypeEnumDto.name(), platformEnumDto.name());

        // Assert
        assertNotNull(restrictedIdOutputDto);
        assertEquals(id, restrictedIdOutputDto.getId());
        assertEquals(idValue, restrictedIdOutputDto.getIdValue());
        assertEquals(idTypeEnumDto, restrictedIdOutputDto.getIdType());
        assertEquals(platformEnumDto, restrictedIdOutputDto.getPlatform());
        assertEquals(title, restrictedIdOutputDto.getTitle());
        assertEquals(comment, restrictedIdOutputDto.getComment());
    }

    @Test
    public void createRestrictedId_whenRestrictedIdAlreadyExists_thenThrowSQLException() throws SQLException {
        // Arrange
        String idValue = "12345678";
        String idType = "dr_production_id ";
        String platform = "dr";
        String title = "Test title";
        String comment = "a comment";
        String expectedMessage = "Unique index or primary key violation";

        storage.createRestrictedId(idValue, idType, platform, title, comment);

        // Act
        Exception exception = assertThrows(SQLException.class, () -> storage.createRestrictedId(idValue, idType, platform, title, comment));

        // Assert
        assertTrue(exception.getMessage().startsWith(expectedMessage));
    }

    @Test
    public void updateRestrictedId_whenUpdatingTitleAndComment_thenReturnRestrictedIdOutputDtoWithUpdatedTitleAndComment() throws SQLException {
        // Arrange
        String idValue = "test1234";
        IdTypeEnumDto idTypeEnumDto = IdTypeEnumDto.DR_PRODUCTION_ID;
        PlatformEnumDto platformEnumDto = PlatformEnumDto.DRARKIV;
        String title = "Test title";
        String comment = "a comment";

        long id = storage.createRestrictedId(idValue, idTypeEnumDto.name(), platformEnumDto.name(), title, comment);

        String newTitle = "new title";
        String newComment = "another comment";

        // Act
        storage.updateRestrictedId(id, newTitle, newComment);
        RestrictedIdOutputDto restrictedIdOutputDto = storage.getRestrictedId(idValue, idTypeEnumDto.name(), platformEnumDto.name());

        // Assert
        assertNotNull(restrictedIdOutputDto);
        assertEquals(id, restrictedIdOutputDto.getId());
        assertEquals(idValue, restrictedIdOutputDto.getIdValue());
        assertEquals(idTypeEnumDto, restrictedIdOutputDto.getIdType());
        assertEquals(platformEnumDto, restrictedIdOutputDto.getPlatform());
        assertEquals(newTitle, restrictedIdOutputDto.getTitle());
        assertEquals(newComment, restrictedIdOutputDto.getComment());
    }

    @Test
    public void deleteRestrictedId_whenDeletingRestrictedId_thenRestrictedIdIsDeleted() throws SQLException {
        // Arrange
        String idValue = "test1234";
        IdTypeEnumDto idTypeEnumDto = IdTypeEnumDto.DR_PRODUCTION_ID;
        PlatformEnumDto platformEnumDto = PlatformEnumDto.DRARKIV;
        String title = "Test title";
        String comment = "a comment";

        long id = storage.createRestrictedId(idValue, idTypeEnumDto.name(), platformEnumDto.name(), title, comment);
        RestrictedIdOutputDto restrictedIdOutputDto = storage.getRestrictedId(idValue, idTypeEnumDto.name(), platformEnumDto.name());

        assertNotNull(restrictedIdOutputDto);
        assertEquals(id, restrictedIdOutputDto.getId());
        assertEquals(idValue, restrictedIdOutputDto.getIdValue());
        assertEquals(idTypeEnumDto, restrictedIdOutputDto.getIdType());
        assertEquals(platformEnumDto, restrictedIdOutputDto.getPlatform());
        assertEquals(title, restrictedIdOutputDto.getTitle());
        assertEquals(comment, restrictedIdOutputDto.getComment());

        // Act
        storage.deleteRestrictedId(idValue, idTypeEnumDto.name(), platformEnumDto.name());
        RestrictedIdOutputDto deletedRestrictedIdOutputDto = storage.getRestrictedId(idValue, idTypeEnumDto.name(), platformEnumDto.name());

        // Assert
        assertNull(deletedRestrictedIdOutputDto);
    }

    @Test
    public void getRestrictedIdByIdValue_whenDsId_thenReturnComment() throws SQLException {
        // Arrange
        String dsId = "ds.tv:oai:io:7cb60d39-effd-419c-9bac-881b7b7eb10c";
        String title = "Damages";
        String expectedComment = "Test comment";

        storage.createRestrictedId(dsId, IdTypeEnumDto.DS_ID.getValue(), PlatformEnumDto.DRARKIV.getValue(), title, expectedComment);

        // Act
        String actualComment = storage.getRestrictedIdCommentByIdValue(dsId);

        // Assert
        assertEquals(expectedComment, actualComment);
    }

    @Test
    public void getRestrictedIdByIdValue_whenNotFoundDsId_thenReturnNull() throws SQLException {
        // Act
        String actualComment = storage.getRestrictedIdCommentByIdValue("1");

        // Assert
        assertNull(actualComment);
    }

    @Test
    public void getAllRestrictedIds_whenSearchingForIdTypeDsIdAndPlatformDrArkiv_thenReturnOnlyMatchingRestrictedIds() throws SQLException {
        // Act
        storage.createRestrictedId("test1", IdTypeEnumDto.DS_ID.getValue(), PlatformEnumDto.DRARKIV.getValue(), "Title1", "Comment1");
        storage.createRestrictedId("test2", IdTypeEnumDto.DS_ID.getValue(), PlatformEnumDto.DRARKIV.getValue(), "Title2", "Comment2");
        storage.createRestrictedId("test3", IdTypeEnumDto.DS_ID.getValue(), PlatformEnumDto.GENERIC.getValue(), "Title3", "Comment3");
        storage.createRestrictedId("test4", IdTypeEnumDto.STRICT_TITLE.getValue(), PlatformEnumDto.DRARKIV.getValue(), "Title4", "Comment4");
        storage.createRestrictedId("test5", IdTypeEnumDto.STRICT_TITLE.getValue(), PlatformEnumDto.GENERIC.getValue(), "Title5", "Comment5");

        // Act
        List<RestrictedIdOutputDto> restrictedIdOutputDtoList = storage.getAllRestrictedIds(IdTypeEnumDto.DS_ID.getValue(), PlatformEnumDto.DRARKIV.getValue());

        // Assert
        assertEquals(2, restrictedIdOutputDtoList.size());

        assertEquals("test1", restrictedIdOutputDtoList.get(0).getIdValue());
        assertEquals(IdTypeEnumDto.DS_ID, restrictedIdOutputDtoList.get(0).getIdType());
        assertEquals(PlatformEnumDto.DRARKIV, restrictedIdOutputDtoList.get(0).getPlatform());
        assertEquals("Title1", restrictedIdOutputDtoList.get(0).getTitle());
        assertEquals("Comment1", restrictedIdOutputDtoList.get(0).getComment());

        assertEquals("test2", restrictedIdOutputDtoList.get(1).getIdValue());
        assertEquals(IdTypeEnumDto.DS_ID, restrictedIdOutputDtoList.get(1).getIdType());
        assertEquals(PlatformEnumDto.DRARKIV, restrictedIdOutputDtoList.get(1).getPlatform());
        assertEquals("Title2", restrictedIdOutputDtoList.get(1).getTitle());
        assertEquals("Comment2", restrictedIdOutputDtoList.get(1).getComment());
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
    public void performStorageAction_whenCreatingRestrictedId_thenRestrictedIdIsInsertedInTheTable() {
        RestrictedIdOutputDto result = BaseModuleStorage.performStorageAction("Testing", RightsModuleStorage.class, storage -> {
            ((RightsModuleStorage) storage).createRestrictedId("test1", IdTypeEnumDto.DS_ID.getValue(), PlatformEnumDto.DRARKIV.getValue(), "test title", "comment");
            return ((RightsModuleStorage) storage).getRestrictedId("test1", IdTypeEnumDto.DS_ID.getValue(), PlatformEnumDto.DRARKIV.getValue());
        });

        assertEquals("test1", result.getIdValue());
        assertEquals(IdTypeEnumDto.DS_ID, result.getIdType());
        assertEquals(PlatformEnumDto.DRARKIV, result.getPlatform());
    }
}
