package dk.kb.license.storage;

import dk.kb.license.model.v1.*;
import dk.kb.license.util.DsLicenseUnitTestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandles;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unittest class for thestorage.
 * All tests create and usePostgres database in the directory: target/h2
 * The directory will be deleted before the first test-method is called.
 * Each test-method will delete all entries in the database, but keep the database tables.
 * Currently, the directory is not deleted after the tests have run. This is useful as you can
 * open and open the database and see what the unit-tests did.
 */
public class RightsModuleStorageTestDsLicense extends DsLicenseUnitTestUtil {
    private static RightsModuleStorageForUnitTest rightsStorage = null;

    @BeforeAll
    public static void beforeClass() throws Exception {
        setupDatabaseForClass(MethodHandles.lookup().lookupClass());
        rightsStorage = new RightsModuleStorageForUnitTest();
    }

    /**
     * Delete all records between each unittest. The clearTableRecords is only called from here.
     * The facade class is responsible for committing transactions. So clean up between unittests.
     */
    @BeforeEach
    public void beforeEach() throws SQLException {
        List<String> tables = new ArrayList<>();
        tables.add("RESTRICTED_IDS");
        tables.add("DR_HOLDBACK_RANGES");
        tables.add("DR_HOLDBACK_CATEGORIES");
        rightsStorage.clearTableRecords(tables);
    }

    @AfterEach
    public void cleanupConnection() throws SQLException {
        rightsStorage.rollbackQuietly();
    }

    @Test
    public void createRestrictedId_whenCreatingRestrictedId_thenReturnId() throws SQLException {
        // Arrange
        String idValue = "test1234";
        IdTypeEnumDto idTypeEnumDto = IdTypeEnumDto.DR_PRODUCTION_ID;
        PlatformEnumDto platformEnumDto = PlatformEnumDto.DRARKIV;
        String title = "Test title";
        String comment = "a comment";

        // Act
        long id = rightsStorage.createRestrictedId(idValue, idTypeEnumDto.name(), platformEnumDto.name(), title, comment);
        RestrictedIdOutputDto restrictedIdOutputDto = rightsStorage.getRestrictedId(idValue, idTypeEnumDto.name(), platformEnumDto.name());

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
        String expectedMessage = "ERROR: duplicate key value violates unique constraint";

        rightsStorage.createRestrictedId(idValue, idType, platform, title, comment);

        // Act
        Exception exception = assertThrows(SQLException.class, () -> rightsStorage.createRestrictedId(idValue, idType, platform, title, comment));

        // Assert
        assertTrue(exception.getMessage().startsWith(expectedMessage));
    }

    @Test
    public void updateRestrictedId_whenUpdatingTitleAndCommentWithValidId_thenTitleAndCommentIsUpdated() throws SQLException {
        // Arrange
        String idValue = "test1234";
        IdTypeEnumDto idTypeEnumDto = IdTypeEnumDto.DR_PRODUCTION_ID;
        PlatformEnumDto platformEnumDto = PlatformEnumDto.DRARKIV;
        String title = "Test title";
        String comment = "a comment";

        long id = rightsStorage.createRestrictedId(idValue, idTypeEnumDto.name(), platformEnumDto.name(), title, comment);

        String newTitle = "new title";
        String newComment = "another comment";

        // Act
        rightsStorage.updateRestrictedId(id, newTitle, newComment);
        RestrictedIdOutputDto restrictedIdOutputDto = rightsStorage.getRestrictedId(idValue, idTypeEnumDto.name(), platformEnumDto.name());

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

        long id = rightsStorage.createRestrictedId(idValue, idTypeEnumDto.name(), platformEnumDto.name(), title, comment);
        RestrictedIdOutputDto restrictedIdOutputDto = rightsStorage.getRestrictedId(idValue, idTypeEnumDto.name(), platformEnumDto.name());

        assertNotNull(restrictedIdOutputDto);
        assertEquals(id, restrictedIdOutputDto.getId());
        assertEquals(idValue, restrictedIdOutputDto.getIdValue());
        assertEquals(idTypeEnumDto, restrictedIdOutputDto.getIdType());
        assertEquals(platformEnumDto, restrictedIdOutputDto.getPlatform());
        assertEquals(title, restrictedIdOutputDto.getTitle());
        assertEquals(comment, restrictedIdOutputDto.getComment());

        // Act
        rightsStorage.deleteRestrictedId(idValue, idTypeEnumDto.name(), platformEnumDto.name());
        RestrictedIdOutputDto deletedRestrictedIdOutputDto = rightsStorage.getRestrictedId(idValue, idTypeEnumDto.name(), platformEnumDto.name());

        // Assert
        assertNull(deletedRestrictedIdOutputDto);
    }

    @Test
    public void getRestrictedIdCommentByIdValue_whenValidDsId_thenReturnComment() throws SQLException {
        // Arrange
        String dsId = "ds.tv:oai:io:7cb60d39-effd-419c-9bac-881b7b7eb10c";
        String title = "Damages";
        String expectedComment = "Test comment";

        rightsStorage.createRestrictedId(dsId, IdTypeEnumDto.DS_ID.getValue(), PlatformEnumDto.DRARKIV.getValue(), title, expectedComment);

        // Act
        String actualComment = rightsStorage.getRestrictedIdCommentByIdValue(dsId);

        // Assert
        assertEquals(expectedComment, actualComment);
    }

    @Test
    public void getRestrictedIdCommentByIdValue_whenNotFoundDsId_thenReturnNull() throws SQLException {
        // Act
        String actualComment = rightsStorage.getRestrictedIdCommentByIdValue("1");

        // Assert
        assertNull(actualComment);
    }

    @Test
    public void getAllRestrictedIds_whenSearchingForIdTypeDsIdAndPlatformDrArkiv_thenReturnOnlyMatchingRestrictedIds() throws SQLException {
        // Act
        rightsStorage.createRestrictedId("test1", IdTypeEnumDto.DS_ID.getValue(), PlatformEnumDto.DRARKIV.getValue(), "Title1", "Comment1");
        rightsStorage.createRestrictedId("test2", IdTypeEnumDto.DS_ID.getValue(), PlatformEnumDto.DRARKIV.getValue(), "Title2", "Comment2");
        rightsStorage.createRestrictedId("test3", IdTypeEnumDto.DS_ID.getValue(), PlatformEnumDto.GENERIC.getValue(), "Title3", "Comment3");
        rightsStorage.createRestrictedId("test4", IdTypeEnumDto.STRICT_TITLE.getValue(), PlatformEnumDto.DRARKIV.getValue(), "Title4", "Comment4");
        rightsStorage.createRestrictedId("test5", IdTypeEnumDto.STRICT_TITLE.getValue(), PlatformEnumDto.GENERIC.getValue(), "Title5", "Comment5");

        // Act
        List<RestrictedIdOutputDto> restrictedIdOutputDtoList = rightsStorage.getAllRestrictedIds(IdTypeEnumDto.DS_ID.getValue(), PlatformEnumDto.DRARKIV.getValue());

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
    public void createDrHoldbackCategory_whenCreatingDrHoldbackCategory_thenReturnId() throws SQLException {
        // Arrange
        String key = "2.02";
        String name = "Aktualitet & Debat";
        int days = 100;

        // Act
        long id = rightsStorage.createDrHoldbackCategory(key, name, days);
        DrHoldbackCategoryOutputDto drHoldbackCategoryById = rightsStorage.getDrHoldbackCategoryById(id);
        DrHoldbackCategoryOutputDto drHoldbackCategoryByKey = rightsStorage.getDrHoldbackCategoryByKey(key);

        // Assert
        assertEquals(id, drHoldbackCategoryById.getId());
        assertEquals(key, drHoldbackCategoryById.getKey());
        assertEquals(name, drHoldbackCategoryById.getName());
        assertEquals(days, drHoldbackCategoryById.getDays());

        assertEquals(id, drHoldbackCategoryByKey.getId());
        assertEquals(key, drHoldbackCategoryByKey.getKey());
        assertEquals(name, drHoldbackCategoryByKey.getName());
        assertEquals(days, drHoldbackCategoryByKey.getDays());
    }

    @Test
    public void createDrHoldbackCategory_whenDrHoldbackCategoryAlreadyExists_thenThrowSQLException() throws SQLException {
        // Arrange
        String key = "2.02";
        String name = "Aktualitet & Debat";
        int days = 100;
        String expectedMessage = "ERROR: duplicate key value violates unique constraint";

        rightsStorage.createDrHoldbackCategory(key, name, days);

        // Act
        Exception exception = assertThrows(SQLException.class, () -> rightsStorage.createDrHoldbackCategory(key, name, days));

        // Assert
        assertTrue(exception.getMessage().startsWith(expectedMessage));
    }

    @Test
    public void updateDrHoldbackCategory_whenUpdatingDaysWithValidId_thenDaysIsUpdated() throws SQLException {
        // Arrange
        String key = "2.02";
        String name = "Aktualitet & Debat";
        int days = 100;
        int newDays = 200;

        long id = rightsStorage.createDrHoldbackCategory(key, name, days);

        // Act
        rightsStorage.updateDrHoldbackCategory(id, newDays);
        DrHoldbackCategoryOutputDto drHoldbackCategoryById = rightsStorage.getDrHoldbackCategoryById(id);
        DrHoldbackCategoryOutputDto drHoldbackCategoryByKey = rightsStorage.getDrHoldbackCategoryByKey(key);

        // Assert
        assertEquals(id, drHoldbackCategoryById.getId());
        assertEquals(key, drHoldbackCategoryById.getKey());
        assertEquals(name, drHoldbackCategoryById.getName());
        assertEquals(newDays, drHoldbackCategoryById.getDays());

        assertEquals(id, drHoldbackCategoryByKey.getId());
        assertEquals(key, drHoldbackCategoryByKey.getKey());
        assertEquals(name, drHoldbackCategoryByKey.getName());
        assertEquals(newDays, drHoldbackCategoryByKey.getDays());
    }

    @Test
    public void deleteDrHoldbackCategory_whenGivenId_thenDrHoldbackCategoryIsDeleted() throws SQLException {
        // Arrange
        String key = "2.02";
        String name = "Aktualitet & Debat";
        int days = 100;

        long id = rightsStorage.createDrHoldbackCategory(key, name, days);
        DrHoldbackCategoryOutputDto drHoldbackCategoryById = rightsStorage.getDrHoldbackCategoryById(id);
        DrHoldbackCategoryOutputDto drHoldbackCategoryByKey = rightsStorage.getDrHoldbackCategoryByKey(key);

        assertEquals(id, drHoldbackCategoryById.getId());
        assertEquals(key, drHoldbackCategoryById.getKey());
        assertEquals(name, drHoldbackCategoryById.getName());
        assertEquals(days, drHoldbackCategoryById.getDays());

        assertEquals(id, drHoldbackCategoryByKey.getId());
        assertEquals(key, drHoldbackCategoryByKey.getKey());
        assertEquals(name, drHoldbackCategoryByKey.getName());
        assertEquals(days, drHoldbackCategoryByKey.getDays());

        // Act
        int deleteDrHoldbackCategory = rightsStorage.deleteDrHoldbackCategory(id);
        DrHoldbackCategoryOutputDto deletedDrHoldbackCategoryById = rightsStorage.getDrHoldbackCategoryById(id);
        DrHoldbackCategoryOutputDto deletedDrHoldbackCategoryByKey = rightsStorage.getDrHoldbackCategoryByKey(key);

        // Assert
        assertEquals(1, deleteDrHoldbackCategory);
        assertNull(deletedDrHoldbackCategoryById);
        assertNull(deletedDrHoldbackCategoryByKey);
    }

    @Test
    public void deleteDrHoldbackCategory_whenThereIsADrHoldbackRange_thenThrowJdbcSQLIntegrityConstraintViolationException() throws SQLException {
        String key = "2.02";
        String name = "Aktualitet & Debat";
        int days = 100;
        String expectedMessage = "ERROR: update or delete on table";

        long id = rightsStorage.createDrHoldbackCategory(key, name, days);

        rightsStorage.createDrHoldbackRange(1000, 1000, 1200, 1900, key);

        // Act
        Exception exception = assertThrows(org.postgresql.util.PSQLException.class, () -> rightsStorage.deleteDrHoldbackCategory(id));

        // Assert
        assertTrue(exception.getMessage().startsWith(expectedMessage));
    }

    @Test
    public void getDrHoldbackCategories_whenWantingAllDrHoldbackCategories_thenReturnAllDrHoldbackCategories() throws SQLException {
        // Arrange
        String key = "2.02";
        String name = "Aktualitet & Debat";
        int days = 100;

        // Act
        long id = rightsStorage.createDrHoldbackCategory(key, name, days);
        List<DrHoldbackCategoryOutputDto> drHoldbackCategoryOutputDtoList = rightsStorage.getDrHoldbackCategories();

        // Assert
        assertEquals(1, drHoldbackCategoryOutputDtoList.size());
        assertEquals(id, drHoldbackCategoryOutputDtoList.get(0).getId());
        assertEquals(key, drHoldbackCategoryOutputDtoList.get(0).getKey());
        assertEquals(name, drHoldbackCategoryOutputDtoList.get(0).getName());
        assertEquals(days, drHoldbackCategoryOutputDtoList.get(0).getDays());
    }

    @Test
    public void createDrHoldbackRange_whenCreatingRange_thenReturnId() throws SQLException {
        // Arrange
        String drHoldbackCategoryKey = "2.02";
        String name = "Aktualitet & Debat";
        int days = 100;

        int contentRangeFrom = 1000;
        int contentRangeTo = 1000;
        int formRangeFrom = 1200;
        int formRangeTo = 1900;

        rightsStorage.createDrHoldbackCategory(drHoldbackCategoryKey, name, days);

        // Act
        long id = rightsStorage.createDrHoldbackRange(contentRangeFrom, contentRangeTo, formRangeFrom, formRangeTo, drHoldbackCategoryKey);
        List<DrHoldbackRangeOutputDto> drHoldbackRangeOutputDtoList = rightsStorage.getDrHoldbackRangesByDrHoldbackCategoryKey(drHoldbackCategoryKey);
        String returnedDrHoldbackCategoryKeyByContentAndForm = rightsStorage.getDrHoldbackCategoryKeyByContentAndForm(contentRangeFrom, formRangeFrom);

        // Assert
        assertEquals(1, drHoldbackRangeOutputDtoList.size());
        assertEquals(id, drHoldbackRangeOutputDtoList.get(0).getId());
        assertEquals(drHoldbackCategoryKey, drHoldbackRangeOutputDtoList.get(0).getDrHoldbackCategoryKey());
        assertEquals(contentRangeFrom, drHoldbackRangeOutputDtoList.get(0).getContentRangeFrom());
        assertEquals(contentRangeTo, drHoldbackRangeOutputDtoList.get(0).getContentRangeTo());
        assertEquals(formRangeFrom, drHoldbackRangeOutputDtoList.get(0).getFormRangeFrom());
        assertEquals(formRangeTo, drHoldbackRangeOutputDtoList.get(0).getFormRangeTo());

        assertEquals(drHoldbackCategoryKey, returnedDrHoldbackCategoryKeyByContentAndForm);
    }

    @Test
    public void deleteDrHoldbackRangesByDrHoldbackCategoryKey_whenGivenDrHoldbackCategoryKey_thenDrHoldbackRangeIsDeleted() throws SQLException {
        String drHoldbackCategoryKey = "2.02";
        String name = "Aktualitet & Debat";
        int days = 100;

        int contentRangeFrom = 1000;
        int contentRangeTo = 1000;
        int formRangeFrom = 1200;
        int formRangeTo = 1900;

        rightsStorage.createDrHoldbackCategory(drHoldbackCategoryKey, name, days);

        long id = rightsStorage.createDrHoldbackRange(contentRangeFrom, contentRangeTo, formRangeFrom, formRangeTo, drHoldbackCategoryKey);
        List<DrHoldbackRangeOutputDto> drHoldbackRangeOutputDtoList = rightsStorage.getDrHoldbackRangesByDrHoldbackCategoryKey(drHoldbackCategoryKey);

        assertEquals(1, drHoldbackRangeOutputDtoList.size());
        assertEquals(id, drHoldbackRangeOutputDtoList.get(0).getId());
        assertEquals(drHoldbackCategoryKey, drHoldbackRangeOutputDtoList.get(0).getDrHoldbackCategoryKey());
        assertEquals(contentRangeFrom, drHoldbackRangeOutputDtoList.get(0).getContentRangeFrom());
        assertEquals(contentRangeTo, drHoldbackRangeOutputDtoList.get(0).getContentRangeTo());
        assertEquals(formRangeFrom, drHoldbackRangeOutputDtoList.get(0).getFormRangeFrom());
        assertEquals(formRangeTo, drHoldbackRangeOutputDtoList.get(0).getFormRangeTo());

        // Act
        int deleteDrHoldbackRangesByDrHoldbackCategoryKey = rightsStorage.deleteDrHoldbackRangesByDrHoldbackCategoryKey(drHoldbackCategoryKey);
        List<DrHoldbackRangeOutputDto> deletedDrHoldbackRangeOutputDtoList = rightsStorage.getDrHoldbackRangesByDrHoldbackCategoryKey(drHoldbackCategoryKey);

        // Assert
        assertEquals(1, deleteDrHoldbackRangesByDrHoldbackCategoryKey);
        assertEquals(0, deletedDrHoldbackRangeOutputDtoList.size());
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
