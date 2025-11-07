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
    public void getRestrictedIdByIdValue_whenValidDsId_thenReturnComment() throws SQLException {
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
