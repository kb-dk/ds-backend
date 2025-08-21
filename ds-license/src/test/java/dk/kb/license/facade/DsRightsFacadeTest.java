package dk.kb.license.facade;

import dk.kb.license.config.ServiceConfig;
import dk.kb.license.model.v1.*;
import dk.kb.license.storage.BaseModuleStorage;
import dk.kb.license.storage.DsLicenseUnitTestUtil;
import dk.kb.license.storage.RightsModuleStorageForUnitTest;
import dk.kb.license.util.H2DbUtil;
import dk.kb.license.webservice.KBAuthorizationInterceptor;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.MessageImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.AccessToken;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

public class DsRightsFacadeTest extends DsLicenseUnitTestUtil {
    protected static RightsModuleStorageForUnitTest storage = null;

    final String drHoldBackValue = "2.02";
    final String drHoldBackName = "Aktualitet og Debat";
    final int drHoldbackDays = 2190;
    final String userName = "mockedName";
    
    DrHoldbackRuleInputDto drHoldbackRuleInputDto = new DrHoldbackRuleInputDto();
    DrHoldbackRangesDto drHoldbackRangesDtoOne = new DrHoldbackRangesDto();
    DrHoldbackRangesDto drHoldbackRangesDtoTwo = new DrHoldbackRangesDto();

    ArrayList<AuditEntryOutputDto> auditLogEntriesForObject;

    static MockedStatic<JAXRSUtils> mocked;

    @BeforeAll
    public static void beforeClass() throws IOException, SQLException {
        ServiceConfig.initialize("conf/ds-license*.yaml");
        H2DbUtil.createEmptyH2DBFromDDL(URL, DRIVER, USERNAME, PASSWORD, List.of("ddl/rightsmodule_create_h2_unittest.ddl"));
        BaseModuleStorage.initialize(DRIVER, URL, USERNAME, PASSWORD);

        storage = new RightsModuleStorageForUnitTest();

        MessageImpl message = new MessageImpl();
        AccessToken mockedToken = Mockito.mock(AccessToken.class);
        Mockito.when(mockedToken.getName()).thenReturn("mockedName");
        message.put(KBAuthorizationInterceptor.ACCESS_TOKEN, mockedToken);

        mocked = mockStatic(JAXRSUtils.class);
        mocked.when(JAXRSUtils::getCurrentMessage).thenReturn(message);
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
        
        drHoldbackRuleInputDto.setDays(drHoldbackDays);
        drHoldbackRuleInputDto.setDrHoldbackValue(drHoldBackValue);
        drHoldbackRuleInputDto.setName(drHoldBackName);

        drHoldbackRangesDtoOne.setContentRangeFrom(1000);
        drHoldbackRangesDtoOne.setContentRangeTo(1900);
        drHoldbackRangesDtoOne.setFormRangeFrom(1000);
        drHoldbackRangesDtoOne.setFormRangeTo(1000);

        drHoldbackRangesDtoTwo.setContentRangeFrom(1000);
        drHoldbackRangesDtoTwo.setContentRangeTo(1900);
        drHoldbackRangesDtoTwo.setFormRangeFrom(1200);
        drHoldbackRangesDtoTwo.setFormRangeTo(1500);
    }

    /**
     * Close the MockedStatic JAXRSUtils.class when the tests are done, so it don't interfere with other test classes
     */
    @AfterAll
    public static void afterClass() {
        mocked.close();
    }

    @Test
    public void createDrHoldbackRule_WhenUsingDrHoldbackRuleInputDto_CreateRule() throws SQLException {
        long drHoldbackRuleId = RightsModuleFacade.createDrHoldbackRule(drHoldbackRuleInputDto);
        auditLogEntriesForObject = storage.getAuditLogByObjectId(drHoldbackRuleId);

        assertEquals(1, auditLogEntriesForObject.size());
        AuditEntryOutputDto createDrHoldbackRuleAuditLog = auditLogEntriesForObject.get(0);
        assertEquals(ChangeTypeEnumDto.CREATE, createDrHoldbackRuleAuditLog.getChangeType());
        assertEquals(ObjectTypeEnumDto.HOLDBACK_RULE, createDrHoldbackRuleAuditLog.getChangeName());
        assertEquals("", createDrHoldbackRuleAuditLog.getTextBefore());
        assertEquals(drHoldbackRuleInputDto.toString(), createDrHoldbackRuleAuditLog.getTextAfter());
        assertEquals(userName, createDrHoldbackRuleAuditLog.getUserName());
        assertEquals(drHoldbackRuleInputDto.getDrHoldbackValue(), createDrHoldbackRuleAuditLog.getChangeComment());
        assertEquals(drHoldbackRuleId, createDrHoldbackRuleAuditLog.getObjectId());
    }

    @Test
    public void createDrHoldbackRanges_WhenUsingDrHoldbackRangeInputDto_CreatesRanges() throws SQLException {
        DrHoldbackRangeInputDto drHoldbackRangeInputDto = new DrHoldbackRangeInputDto();
        drHoldbackRangeInputDto.setDrHoldbackValue(drHoldBackValue);

        List<DrHoldbackRangesDto> ranges = List.of(drHoldbackRangesDtoOne, drHoldbackRangesDtoTwo);

        drHoldbackRangeInputDto.setRanges(ranges);

        long drHoldbackRuleId = RightsModuleFacade.createDrHoldbackRule(drHoldbackRuleInputDto);

        List<Long> drHoldBackRangesIds = RightsModuleFacade.createDrHoldbackRanges(drHoldbackRangeInputDto);

        auditLogEntriesForObject = storage.getAuditLogByObjectId(drHoldbackRuleId);

        assertEquals(2, drHoldBackRangesIds.size());

        for (Long drHoldBackRangesId : drHoldBackRangesIds) {

            auditLogEntriesForObject = storage.getAuditLogByObjectId(drHoldBackRangesId);
            assertEquals(1, auditLogEntriesForObject.size());
            AuditEntryOutputDto createDrHoldbackRangeAuditLog = auditLogEntriesForObject.get(0);

            assertEquals(ChangeTypeEnumDto.CREATE, createDrHoldbackRangeAuditLog.getChangeType());
            assertEquals(ObjectTypeEnumDto.HOLDBACK_RANGE, createDrHoldbackRangeAuditLog.getChangeName());
            assertEquals("", createDrHoldbackRangeAuditLog.getTextBefore());
            assertEquals(ranges.toString(), createDrHoldbackRangeAuditLog.getTextAfter());
            assertEquals(userName, createDrHoldbackRangeAuditLog.getUserName());
            assertEquals(drHoldbackRangeInputDto.getDrHoldbackValue(), createDrHoldbackRangeAuditLog.getChangeComment());
            assertEquals(drHoldBackRangesId, createDrHoldbackRangeAuditLog.getObjectId());
        }
    }

    @Test
    public void updateDrHoldbackDaysFromDrHoldbackValue_WhenUsingDrHoldBackValue_UpdateDaysForRule() throws SQLException {
        int newDrHoldbackDays = 10;

        long drHoldbackRuleId = RightsModuleFacade.createDrHoldbackRule(drHoldbackRuleInputDto);

        auditLogEntriesForObject = storage.getAuditLogByObjectId(drHoldbackRuleId);

        RightsModuleFacade.updateDrHoldbackDaysFromDrHoldbackValue(drHoldBackValue, newDrHoldbackDays);
        auditLogEntriesForObject = storage.getAuditLogByObjectId(drHoldbackRuleId);
        assertEquals(2, auditLogEntriesForObject.size());
        AuditEntryOutputDto updateDrHoldbackRuleAuditLogFromValue = auditLogEntriesForObject.get(0);
        assertEquals(ChangeTypeEnumDto.UPDATE, updateDrHoldbackRuleAuditLogFromValue.getChangeType());
        assertEquals(ObjectTypeEnumDto.HOLDBACK_DAY, updateDrHoldbackRuleAuditLogFromValue.getChangeName());
        assertEquals("Days before: " + drHoldbackDays, updateDrHoldbackRuleAuditLogFromValue.getTextBefore());
        assertEquals("Days after: " + newDrHoldbackDays, updateDrHoldbackRuleAuditLogFromValue.getTextAfter());
        assertEquals(userName, updateDrHoldbackRuleAuditLogFromValue.getUserName());
        assertEquals(drHoldbackRuleInputDto.getDrHoldbackValue(), updateDrHoldbackRuleAuditLogFromValue.getChangeComment());
        assertEquals(drHoldbackRuleId, updateDrHoldbackRuleAuditLogFromValue.getObjectId());
    }

    @Test
    public void updateDrHoldbackDaysFromName_WhenUsingDrHoldBackName_UpdateDaysForRule() throws SQLException {
        int newDrHoldbackDays = 10;

        long drHoldbackRuleId = RightsModuleFacade.createDrHoldbackRule(drHoldbackRuleInputDto);

        auditLogEntriesForObject = storage.getAuditLogByObjectId(drHoldbackRuleId);

        RightsModuleFacade.updateDrHoldbackDaysFromName(drHoldBackName, newDrHoldbackDays);
        auditLogEntriesForObject = storage.getAuditLogByObjectId(drHoldbackRuleId);
        assertEquals(2, auditLogEntriesForObject.size());
        AuditEntryOutputDto updateDrHoldbackRuleAuditLogFromName = auditLogEntriesForObject.get(0);
        assertEquals(ChangeTypeEnumDto.UPDATE, updateDrHoldbackRuleAuditLogFromName.getChangeType());
        assertEquals("Days before: " + drHoldbackDays, updateDrHoldbackRuleAuditLogFromName.getTextBefore());
        assertEquals(ObjectTypeEnumDto.HOLDBACK_DAY, updateDrHoldbackRuleAuditLogFromName.getChangeName());
        assertEquals("Days after: " + newDrHoldbackDays, updateDrHoldbackRuleAuditLogFromName.getTextAfter());
        assertEquals(userName, updateDrHoldbackRuleAuditLogFromName.getUserName());
        assertEquals(drHoldbackRuleInputDto.getName(), updateDrHoldbackRuleAuditLogFromName.getChangeComment());
        assertEquals(drHoldbackRuleId, updateDrHoldbackRuleAuditLogFromName.getObjectId());
    }

    @Test
    public void deleteRangesForDrHoldbackValue_WhenUsingDrHoldBackValue_DeletesAllRanges() throws SQLException {
        DrHoldbackRangeInputDto drHoldbackRangeInputDto = new DrHoldbackRangeInputDto();
        drHoldbackRangeInputDto.setDrHoldbackValue(drHoldBackValue);

        List<DrHoldbackRangesDto> ranges = List.of(drHoldbackRangesDtoOne, drHoldbackRangesDtoTwo);

        drHoldbackRangeInputDto.setRanges(ranges);

        long drHoldbackRuleId = RightsModuleFacade.createDrHoldbackRule(drHoldbackRuleInputDto);

        List<Long> drHoldBackRangesIds = RightsModuleFacade.createDrHoldbackRanges(drHoldbackRangeInputDto);

        auditLogEntriesForObject = storage.getAuditLogByObjectId(drHoldbackRuleId);

        RightsModuleFacade.deleteRangesForDrHoldbackValue(drHoldBackValue);

        for (Long drHoldBackRangesId : drHoldBackRangesIds) {

            auditLogEntriesForObject = storage.getAuditLogByObjectId(drHoldBackRangesId);
            assertEquals(2, auditLogEntriesForObject.size());
            AuditEntryOutputDto deleteDrHoldbackRangeAuditLog = auditLogEntriesForObject.get(0);

            assertEquals(ChangeTypeEnumDto.DELETE, deleteDrHoldbackRangeAuditLog.getChangeType());
            assertEquals(ObjectTypeEnumDto.HOLDBACK_RANGE, deleteDrHoldbackRangeAuditLog.getChangeName());
            // assertEquals(ranges.toString(), deleteDrHoldbackRangeAuditLog.getTextBefore()); TODO: This should be fixed together with: DRA-2085
            assertEquals("", deleteDrHoldbackRangeAuditLog.getTextAfter());
            assertEquals(userName, deleteDrHoldbackRangeAuditLog.getUserName());
            assertEquals(drHoldbackRangeInputDto.getDrHoldbackValue(), deleteDrHoldbackRangeAuditLog.getChangeComment());
            assertEquals(drHoldBackRangesId, deleteDrHoldbackRangeAuditLog.getObjectId());

            assertTrue(auditLogEntriesForObject.get(1).getModifiedTime() < deleteDrHoldbackRangeAuditLog.getModifiedTime());
        }
    }

    @Test
    public void deleteDrHoldbackRule_WhenUsingDrHoldbackRuleId_DeleteRule() throws SQLException {
        long drHoldbackRuleId = RightsModuleFacade.createDrHoldbackRule(drHoldbackRuleInputDto);

        auditLogEntriesForObject = storage.getAuditLogByObjectId(drHoldbackRuleId);

        RightsModuleFacade.deleteDrHoldbackRule(drHoldBackValue);
        auditLogEntriesForObject = storage.getAuditLogByObjectId(drHoldbackRuleId);

        assertEquals(2, auditLogEntriesForObject.size());
        AuditEntryOutputDto deleteDrHoldbackRuleAuditLog = auditLogEntriesForObject.get(0);
        assertEquals(ChangeTypeEnumDto.DELETE, deleteDrHoldbackRuleAuditLog.getChangeType());
        assertEquals(ObjectTypeEnumDto.HOLDBACK_RULE, deleteDrHoldbackRuleAuditLog.getChangeName());
        //assertEquals(drHoldbackRuleInputDto.toString(), deleteDrHoldbackRuleAuditLog.getTextBefore()); TODO: This should be fixed together with: DRA-2085
        assertEquals("", deleteDrHoldbackRuleAuditLog.getTextAfter());
        assertEquals(userName, deleteDrHoldbackRuleAuditLog.getUserName());
        assertEquals(drHoldbackRuleInputDto.getDrHoldbackValue(), deleteDrHoldbackRuleAuditLog.getChangeComment());
        assertEquals(drHoldbackRuleId, deleteDrHoldbackRuleAuditLog.getObjectId());
    }
}
