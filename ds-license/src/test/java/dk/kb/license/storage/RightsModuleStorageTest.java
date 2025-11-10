package dk.kb.license.storage;

import dk.kb.license.config.ServiceConfig;
import dk.kb.license.model.v1.*;
import dk.kb.license.util.H2DbUtil;
import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unittest class for the H2Storage.
 * All tests create and use H2 database in the directory: target/h2
 * The directory will be deleted before the first test-method is called.
 * Each test-method will delete all entries in the database, but keep the database tables.
 * Currently, the directory is not deleted after the tests have run. This is useful as you can
 * open and open the database and see what the unit-tests did.
 */
public class RightsModuleStorageTest extends UnitTestUtil {
    protected static RightsModuleStorageForUnitTest storage = null;

    @BeforeAll
    public static void beforeClass() throws IOException, SQLException {
        ServiceConfig.initialize("conf/ds-license*.yaml", "src/test/resources/ds-license-integration-test.yaml");
        BaseModuleStorage.initialize(DRIVER, URL, USERNAME, PASSWORD);
        H2DbUtil.createEmptyH2DBFromDDL(URL, DRIVER, USERNAME, PASSWORD, List.of("ddl/rightsmodule_create_h2_unittest.ddl", "ddl/audit_log_module_create_h2_unittest.ddl"));
        storage = new RightsModuleStorageForUnitTest();
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
        tables.add("DR_HOLDBACK_RULES");
        storage.clearTableRecords(tables);
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
    public void updateRestrictedId_whenUpdatingTitleAndCommentWithValidId_thenTitleAndCommentIsUpdated() throws SQLException {
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
    public void getRestrictedIdCommentByIdValue_whenValidDsId_thenReturnComment() throws SQLException {
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
    public void getRestrictedIdCommentByIdValue_whenNotFoundDsId_thenReturnNull() throws SQLException {
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
    public void createDrHoldbackRule_whenCreatingDrHoldbackRule_thenReturnId() throws SQLException {
        // Arrange
        String drHoldbackValue = "2.02";
        String name = "Aktualitet & Debat";
        int days = 100;

        // Act
        long id = storage.createDrHoldbackRule(drHoldbackValue, name, days);
        DrHoldbackRuleOutputDto drHoldbackRuleById = storage.getDrHoldbackRuleById(id);
        DrHoldbackRuleOutputDto drHoldbackRuleByDrHoldbackValue = storage.getDrHoldbackRuleByDrHoldbackValue(drHoldbackValue);

        // Assert
        assertEquals(id, drHoldbackRuleById.getId());
        assertEquals(drHoldbackValue, drHoldbackRuleById.getDrHoldbackValue());
        assertEquals(name, drHoldbackRuleById.getName());
        assertEquals(days, drHoldbackRuleById.getDays());

        assertEquals(id, drHoldbackRuleByDrHoldbackValue.getId());
        assertEquals(drHoldbackValue, drHoldbackRuleByDrHoldbackValue.getDrHoldbackValue());
        assertEquals(name, drHoldbackRuleByDrHoldbackValue.getName());
        assertEquals(days, drHoldbackRuleByDrHoldbackValue.getDays());
    }

    @Test
    public void createDrHoldbackRule_whenDrHoldbackRuleAlreadyExists_thenThrowSQLException() throws SQLException {
        // Arrange
        String drHoldbackValue = "2.02";
        String name = "Aktualitet & Debat";
        int days = 100;
        String expectedMessage = "Unique index or primary key violation";

        storage.createDrHoldbackRule(drHoldbackValue, name, days);

        // Act
        Exception exception = assertThrows(SQLException.class, () -> storage.createDrHoldbackRule(drHoldbackValue, name, days));

        // Assert
        assertTrue(exception.getMessage().startsWith(expectedMessage));
    }

    @Test
    public void updateDrHoldbackRule_whenUpdatingDaysWithValidId_thenDaysIsUpdated() throws SQLException {
        // Arrange
        String drHoldbackValue = "2.02";
        String name = "Aktualitet & Debat";
        int days = 100;
        int newDays = 200;

        long id = storage.createDrHoldbackRule(drHoldbackValue, name, days);

        // Act
        storage.updateDrHoldbackRule(id, newDays);
        DrHoldbackRuleOutputDto drHoldbackRuleById = storage.getDrHoldbackRuleById(id);
        DrHoldbackRuleOutputDto drHoldbackRuleByDrHoldbackValue = storage.getDrHoldbackRuleByDrHoldbackValue(drHoldbackValue);

        // Assert
        assertEquals(id, drHoldbackRuleById.getId());
        assertEquals(drHoldbackValue, drHoldbackRuleById.getDrHoldbackValue());
        assertEquals(name, drHoldbackRuleById.getName());
        assertEquals(newDays, drHoldbackRuleById.getDays());

        assertEquals(id, drHoldbackRuleByDrHoldbackValue.getId());
        assertEquals(drHoldbackValue, drHoldbackRuleByDrHoldbackValue.getDrHoldbackValue());
        assertEquals(name, drHoldbackRuleByDrHoldbackValue.getName());
        assertEquals(newDays, drHoldbackRuleByDrHoldbackValue.getDays());
    }

    @Test
    public void deleteDrHoldbackRule_whenGivenId_thenDrHoldbackRuleIsDeleted() throws SQLException {
        // Arrange
        String drHoldbackValue = "2.02";
        String name = "Aktualitet & Debat";
        int days = 100;

        long id = storage.createDrHoldbackRule(drHoldbackValue, name, days);
        DrHoldbackRuleOutputDto drHoldbackRuleById = storage.getDrHoldbackRuleById(id);
        DrHoldbackRuleOutputDto drHoldbackRuleByDrHoldbackValue = storage.getDrHoldbackRuleByDrHoldbackValue(drHoldbackValue);

        assertEquals(id, drHoldbackRuleById.getId());
        assertEquals(drHoldbackValue, drHoldbackRuleById.getDrHoldbackValue());
        assertEquals(name, drHoldbackRuleById.getName());
        assertEquals(days, drHoldbackRuleById.getDays());

        assertEquals(id, drHoldbackRuleByDrHoldbackValue.getId());
        assertEquals(drHoldbackValue, drHoldbackRuleByDrHoldbackValue.getDrHoldbackValue());
        assertEquals(name, drHoldbackRuleByDrHoldbackValue.getName());
        assertEquals(days, drHoldbackRuleByDrHoldbackValue.getDays());

        // Act
        int deleteDrHoldbackRule = storage.deleteDrHoldbackRule(id);
        DrHoldbackRuleOutputDto deletedDrHoldbackRuleById = storage.getDrHoldbackRuleById(id);
        DrHoldbackRuleOutputDto deletedDrHoldbackRuleByDrHoldbackValue = storage.getDrHoldbackRuleByDrHoldbackValue(drHoldbackValue);

        // Assert
        assertEquals(1, deleteDrHoldbackRule);
        assertNull(deletedDrHoldbackRuleById);
        assertNull(deletedDrHoldbackRuleByDrHoldbackValue);
    }

    @Test
    public void deleteDrHoldbackRule_whenThereIsADrHoldbackRange_thenThrowJdbcSQLIntegrityConstraintViolationException() throws SQLException {
        String drHoldbackValue = "2.02";
        String name = "Aktualitet & Debat";
        int days = 100;
        String expectedMessage = "Referential integrity constraint violation: \"CONSTRAINT_35: public.dr_holdback_ranges FOREIGN KEY(dr_holdback_value) REFERENCES public.dr_holdback_rules(dr_holdback_value)";

        long id = storage.createDrHoldbackRule(drHoldbackValue, name, days);

        storage.createDrHoldbackRange(1000, 1000, 1200, 1900, drHoldbackValue);

        // Act
        Exception exception = assertThrows(JdbcSQLIntegrityConstraintViolationException.class, () -> storage.deleteDrHoldbackRule(id));

        // Assert
        assertTrue(exception.getMessage().startsWith(expectedMessage));
    }

    @Test
    public void getDrHoldbackRules_whenWantingAllDrHoldbackRules_thenReturnAllDrHoldbackRules() throws SQLException {
        // Arrange
        String drHoldbackValue = "2.02";
        String name = "Aktualitet & Debat";
        int days = 100;

        // Act
        long id = storage.createDrHoldbackRule(drHoldbackValue, name, days);
        List<DrHoldbackRuleOutputDto> drHoldbackRuleOutputDtoList = storage.getDrHoldbackRules();

        // Assert
        assertEquals(1, drHoldbackRuleOutputDtoList.size());
        assertEquals(id, drHoldbackRuleOutputDtoList.get(0).getId());
        assertEquals(drHoldbackValue, drHoldbackRuleOutputDtoList.get(0).getDrHoldbackValue());
        assertEquals(name, drHoldbackRuleOutputDtoList.get(0).getName());
        assertEquals(days, drHoldbackRuleOutputDtoList.get(0).getDays());
    }

    @Test
    public void createDrHoldbackRange_whenCreatingRange_thenReturnId() throws SQLException {
        // Arrange
        String drHoldbackValue = "2.02";
        String name = "Aktualitet & Debat";
        int days = 100;

        int contentRangeFrom = 1000;
        int contentRangeTo = 1000;
        int formRangeFrom = 1200;
        int formRangeTo = 1900;

        storage.createDrHoldbackRule(drHoldbackValue, name, days);

        // Act
        long id = storage.createDrHoldbackRange(contentRangeFrom, contentRangeTo, formRangeFrom, formRangeTo, drHoldbackValue);
        List<DrHoldbackRangeOutputDto> drHoldbackRangeOutputDtoList = storage.getDrHoldbackRangesByDrHoldbackValue(drHoldbackValue);
        String returnedDrHoldbackValueFromContentAndForm = storage.getDrHoldbackValueByContentAndForm(contentRangeFrom, formRangeFrom);

        // Assert
        assertEquals(1, drHoldbackRangeOutputDtoList.size());
        assertEquals(id, drHoldbackRangeOutputDtoList.get(0).getId());
        assertEquals(drHoldbackValue, drHoldbackRangeOutputDtoList.get(0).getDrHoldbackValue());
        assertEquals(contentRangeFrom, drHoldbackRangeOutputDtoList.get(0).getContentRangeFrom());
        assertEquals(contentRangeTo, drHoldbackRangeOutputDtoList.get(0).getContentRangeTo());
        assertEquals(formRangeFrom, drHoldbackRangeOutputDtoList.get(0).getFormRangeFrom());
        assertEquals(formRangeTo, drHoldbackRangeOutputDtoList.get(0).getFormRangeTo());

        assertEquals(drHoldbackValue, returnedDrHoldbackValueFromContentAndForm);
    }

    @Test
    public void deleteRangesByDrHoldbackValue_whenGivenDrHoldbackValue_thenDrHoldbackRangeIsDeleted() throws SQLException {
        String drHoldbackValue = "2.02";
        String name = "Aktualitet & Debat";
        int days = 100;

        int contentRangeFrom = 1000;
        int contentRangeTo = 1000;
        int formRangeFrom = 1200;
        int formRangeTo = 1900;

        storage.createDrHoldbackRule(drHoldbackValue, name, days);

        long id = storage.createDrHoldbackRange(contentRangeFrom, contentRangeTo, formRangeFrom, formRangeTo, drHoldbackValue);
        List<DrHoldbackRangeOutputDto> drHoldbackRangeOutputDtoList = storage.getDrHoldbackRangesByDrHoldbackValue(drHoldbackValue);

        assertEquals(1, drHoldbackRangeOutputDtoList.size());
        assertEquals(id, drHoldbackRangeOutputDtoList.get(0).getId());
        assertEquals(drHoldbackValue, drHoldbackRangeOutputDtoList.get(0).getDrHoldbackValue());
        assertEquals(contentRangeFrom, drHoldbackRangeOutputDtoList.get(0).getContentRangeFrom());
        assertEquals(contentRangeTo, drHoldbackRangeOutputDtoList.get(0).getContentRangeTo());
        assertEquals(formRangeFrom, drHoldbackRangeOutputDtoList.get(0).getFormRangeFrom());
        assertEquals(formRangeTo, drHoldbackRangeOutputDtoList.get(0).getFormRangeTo());

        // Act
        int deleteRangesByDrHoldbackValue = storage.deleteRangesByDrHoldbackValue(drHoldbackValue);
        List<DrHoldbackRangeOutputDto> deletedDrHoldbackRangeOutputDtoList = storage.getDrHoldbackRangesByDrHoldbackValue(drHoldbackValue);

        // Assert
        assertEquals(1, deleteRangesByDrHoldbackValue);
        assertTrue(deletedDrHoldbackRangeOutputDtoList.isEmpty());
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
