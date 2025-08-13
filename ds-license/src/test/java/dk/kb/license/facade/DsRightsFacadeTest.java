package dk.kb.license.facade;

import dk.kb.license.config.ServiceConfig;
import dk.kb.license.model.v1.*;
import dk.kb.license.storage.*;
import dk.kb.license.util.H2DbUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class DsRightsFacadeTest extends DsLicenseUnitTestUtil{
    protected static RightsModuleStorageForUnitTest storage = null;
    private static final Logger log = LoggerFactory.getLogger(RightsModuleStorageTest.class);

    @BeforeAll
    public static void beforeClass() throws IOException, SQLException {
        ServiceConfig.initialize("conf/ds-license*.yaml");
        H2DbUtil.createEmptyH2DBFromDDL(URL,DRIVER,USERNAME,PASSWORD, List.of("ddl/rightsmodule_create_h2_unittest.ddl"));
        BaseModuleStorage.initialize(DRIVER, URL, USERNAME, PASSWORD);

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
        tables.add("AUDITLOG");
        storage.clearTableRecords(tables);
    }

    @Test
    public void testAuditLog() throws SQLException {

        HttpSession mockedSession = Mockito.mock(HttpSession.class);
        Mockito.when(mockedSession.getAttribute("oauth_user")).thenReturn("mockedName"); //TODO - the create method shouldnt take user as an input - fix that, and then this will be used

        String drHoldBackValue = "2.02";
        String drHoldBackName = "Aktualitet og Debat";
        int drHoldbackDays = 2190;

        DrHoldbackRuleInputDto drHoldbackRuleInputDto = new DrHoldbackRuleInputDto();
        drHoldbackRuleInputDto.setDays(drHoldbackDays);
        drHoldbackRuleInputDto.setDrHoldbackValue(drHoldBackValue);
        drHoldbackRuleInputDto.setName(drHoldBackName);

        DrHoldbackRangesDto drHoldbackRangesDtoOne = new DrHoldbackRangesDto();
        drHoldbackRangesDtoOne.setContentRangeFrom(1000);
        drHoldbackRangesDtoOne.setContentRangeTo(1900);
        drHoldbackRangesDtoOne.setFormRangeFrom(1000);
        drHoldbackRangesDtoOne.setFormRangeTo(1000);

        DrHoldbackRangesDto drHoldbackRangesDtoTwo = new DrHoldbackRangesDto();
        drHoldbackRangesDtoTwo.setContentRangeFrom(1000);
        drHoldbackRangesDtoTwo.setContentRangeTo(1900);
        drHoldbackRangesDtoTwo.setFormRangeFrom(1200);
        drHoldbackRangesDtoTwo.setFormRangeTo(1500);

        List<DrHoldbackRangesDto> ranges = List.of(drHoldbackRangesDtoOne, drHoldbackRangesDtoTwo);

        long drHoldbackRuleId = RightsModuleFacade.createDrHoldbackRule(drHoldbackRuleInputDto, "inputedName");
        ArrayList<AuditEntryOutputDto> auditLogEntriesForObject = storage.getAuditLogByObjectId(drHoldbackRuleId);

        assertEquals(1, auditLogEntriesForObject.size());
        AuditEntryOutputDto createDrHoldbackRuleAuditLog = auditLogEntriesForObject.get(0);
        assertEquals(ChangeTypeEnumDto.CREATE, createDrHoldbackRuleAuditLog.getChangeType());
        assertEquals(ObjectTypeEnumDto.HOLDBACK_RULE, createDrHoldbackRuleAuditLog.getChangeName());
        assertEquals("", createDrHoldbackRuleAuditLog.getTextBefore());
        assertEquals(drHoldbackRuleInputDto.toString(), createDrHoldbackRuleAuditLog.getTextAfter());
        assertEquals("inputedName", createDrHoldbackRuleAuditLog.getUserName());
        assertEquals(drHoldbackRuleInputDto.getDrHoldbackValue(), createDrHoldbackRuleAuditLog.getChangeComment());
        assertEquals(drHoldbackRuleId, createDrHoldbackRuleAuditLog.getObjectId());

        DrHoldbackRangeInputDto drHoldbackRangeInputDto = new DrHoldbackRangeInputDto();
        drHoldbackRangeInputDto.setDrHoldbackValue(drHoldBackValue);

        drHoldbackRangeInputDto.setRanges(ranges);

        List<Long> drHoldBackRangesIds = RightsModuleFacade.createDrHoldbackRanges(drHoldbackRangeInputDto, "inputedName");

        assertEquals(2, drHoldBackRangesIds.size());

        for (int i = 0; i < drHoldBackRangesIds.size(); i++) {

            auditLogEntriesForObject = storage.getAuditLogByObjectId(drHoldBackRangesIds.get(i));
            assertEquals(1, auditLogEntriesForObject.size());
            AuditEntryOutputDto createDrHoldbackRangeAuditLog = auditLogEntriesForObject.get(0);

            assertEquals(ChangeTypeEnumDto.CREATE, createDrHoldbackRangeAuditLog.getChangeType());
            assertEquals(ObjectTypeEnumDto.HOLDBACK_RANGE, createDrHoldbackRangeAuditLog.getChangeName());
            assertEquals("", createDrHoldbackRangeAuditLog.getTextBefore());
            assertEquals(ranges.toString(), createDrHoldbackRangeAuditLog.getTextAfter());
            assertEquals("inputedName", createDrHoldbackRangeAuditLog.getUserName());
            assertEquals(drHoldbackRangeInputDto.getDrHoldbackValue(), createDrHoldbackRangeAuditLog.getChangeComment());
            assertEquals(drHoldBackRangesIds.get(i), createDrHoldbackRangeAuditLog.getObjectId());
        }

        RightsModuleFacade.updateDrHoldbackDaysFromDrHoldbackValue(drHoldBackValue, 20, "inputedName");
        auditLogEntriesForObject = storage.getAuditLogByObjectId(drHoldbackRuleId);
        assertEquals(2, auditLogEntriesForObject.size());
        AuditEntryOutputDto updateDrHoldbackRuleAuditLogFromValue = auditLogEntriesForObject.get(0);
        assertEquals(ChangeTypeEnumDto.UPDATE, updateDrHoldbackRuleAuditLogFromValue.getChangeType());
        assertEquals(ObjectTypeEnumDto.HOLDBACK_DAY, updateDrHoldbackRuleAuditLogFromValue.getChangeName());
        assertEquals("Days before: " + drHoldbackDays, updateDrHoldbackRuleAuditLogFromValue.getTextBefore());
        assertEquals("Days after: " + 20, updateDrHoldbackRuleAuditLogFromValue.getTextAfter());
        assertEquals("inputedName", updateDrHoldbackRuleAuditLogFromValue.getUserName());
        assertEquals(drHoldbackRuleInputDto.getDrHoldbackValue(), updateDrHoldbackRuleAuditLogFromValue.getChangeComment());
        assertEquals(drHoldbackRuleId, updateDrHoldbackRuleAuditLogFromValue.getObjectId());
        assertTrue(updateDrHoldbackRuleAuditLogFromValue.getModifiedTime() > createDrHoldbackRuleAuditLog.getModifiedTime());

        RightsModuleFacade.updateDrHoldbackDaysFromName(drHoldBackName, 10,  "inputedName" );
        auditLogEntriesForObject = storage.getAuditLogByObjectId(drHoldbackRuleId);
        assertEquals(3, auditLogEntriesForObject.size());
        AuditEntryOutputDto updateDrHoldbackRuleAuditLogFromName = auditLogEntriesForObject.get(0);
        assertEquals(ChangeTypeEnumDto.UPDATE, updateDrHoldbackRuleAuditLogFromName.getChangeType());
        assertEquals("Days before: " + 20, updateDrHoldbackRuleAuditLogFromName.getTextBefore());
        assertEquals(ObjectTypeEnumDto.HOLDBACK_DAY, updateDrHoldbackRuleAuditLogFromName.getChangeName());
        assertEquals("Days after: " + 10, updateDrHoldbackRuleAuditLogFromName.getTextAfter());
        assertEquals("inputedName", updateDrHoldbackRuleAuditLogFromName.getUserName());
        assertEquals(drHoldbackRuleInputDto.getName(), updateDrHoldbackRuleAuditLogFromName.getChangeComment());
        assertEquals(drHoldbackRuleId, updateDrHoldbackRuleAuditLogFromName.getObjectId());
        assertTrue(updateDrHoldbackRuleAuditLogFromName.getModifiedTime() > updateDrHoldbackRuleAuditLogFromValue.getModifiedTime());

        RightsModuleFacade.deleteRangesForDrHoldbackValue(drHoldBackValue, "inputedName");

        for (int i = 0; i < drHoldBackRangesIds.size(); i++) {

            auditLogEntriesForObject = storage.getAuditLogByObjectId(drHoldBackRangesIds.get(i));
            assertEquals(2, auditLogEntriesForObject.size());
            AuditEntryOutputDto deleteDrHoldbackRangeAuditLog = auditLogEntriesForObject.get(0);

            assertEquals(ChangeTypeEnumDto.DELETE, deleteDrHoldbackRangeAuditLog.getChangeType());
            assertEquals(ObjectTypeEnumDto.HOLDBACK_RANGE, deleteDrHoldbackRangeAuditLog.getChangeName());
            // assertEquals(ranges.toString(), deleteDrHoldbackRangeAuditLog.getTextBefore()); TODO: This should be fixed together with: DRA-2085
            assertEquals("", deleteDrHoldbackRangeAuditLog.getTextAfter());
            assertEquals("inputedName", deleteDrHoldbackRangeAuditLog.getUserName());
            assertEquals(drHoldbackRangeInputDto.getDrHoldbackValue(), deleteDrHoldbackRangeAuditLog.getChangeComment());
            assertEquals(drHoldBackRangesIds.get(i), deleteDrHoldbackRangeAuditLog.getObjectId());

            assertTrue(auditLogEntriesForObject.get(1).getModifiedTime() < deleteDrHoldbackRangeAuditLog.getModifiedTime());
        }

        RightsModuleFacade.deleteDrHoldbackRule(drHoldBackValue, "inputedName");
        auditLogEntriesForObject = storage.getAuditLogByObjectId(drHoldbackRuleId);

        assertEquals(4, auditLogEntriesForObject.size());
        AuditEntryOutputDto deleteDrHoldbackRuleAuditLog = auditLogEntriesForObject.get(0);
        assertEquals(ChangeTypeEnumDto.DELETE, deleteDrHoldbackRuleAuditLog.getChangeType());
        assertEquals(ObjectTypeEnumDto.HOLDBACK_RULE, deleteDrHoldbackRuleAuditLog.getChangeName());
        //assertEquals(drHoldbackRuleInputDto.toString(), deleteDrHoldbackRuleAuditLog.getTextBefore()); TODO: This should be fixed together with: DRA-2085
        assertEquals("", deleteDrHoldbackRuleAuditLog.getTextAfter());
        assertEquals("inputedName", deleteDrHoldbackRuleAuditLog.getUserName());
        assertEquals(drHoldbackRuleInputDto.getDrHoldbackValue(), deleteDrHoldbackRuleAuditLog.getChangeComment());
        assertEquals(drHoldbackRuleId, deleteDrHoldbackRuleAuditLog.getObjectId());
    }
}
