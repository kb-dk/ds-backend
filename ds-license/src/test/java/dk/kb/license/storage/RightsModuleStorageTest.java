package dk.kb.license.storage;

import dk.kb.license.config.ServiceConfig;
import dk.kb.license.model.v1.DrHoldbackRuleDto;
import dk.kb.license.model.v1.IdTypeEnumDto;
import dk.kb.license.model.v1.PlatformEnumDto;
import dk.kb.license.model.v1.RestrictedIdOutputDto;
import dk.kb.license.util.H2DbUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RightsModuleStorageTest extends DsLicenseUnitTestUtil   {

    protected static RightsModuleStorage storage = null;


    @BeforeAll
    public static void beforeClass() throws IOException, SQLException {

        ServiceConfig.initialize("conf/ds-license*.yaml", "src/test/resources/ds-license-integration-test.yaml");
        BaseModuleStorage.initialize(DRIVER, URL, USERNAME, PASSWORD);

        H2DbUtil.createEmptyH2DBFromDDL(URL,DRIVER,USERNAME,PASSWORD, List.of("ddl/rightsmodule_create_h2_unittest.ddl"));
        storage = new RightsModuleStorage();
    }

    /*
     * Delete all records between each unittest. The clearTableRecords is only called from here.
     * The facade class is reponsible for committing transactions. So clean up between unittests.
     */
    @BeforeEach
    public void beforeEach() throws SQLException {
        storage.clearTableRecords();
    }

    @Test
    public void testRestrictedIdCRUD() throws SQLException {
        String idValue = "test1234";
        String idType = IdTypeEnumDto.DR_PRODUCTION_ID.getValue();
        String platform = PlatformEnumDto.DRARKIV.getValue();
        String comment = "a comment";
        String modified_by = "user1";
        long modified_time = 1739439979000L;

        storage.createRestrictedId(idValue,idType,platform,comment,modified_by,modified_time);
        RestrictedIdOutputDto retreivedFromStorage = storage.getRestrictedId(idValue, idType, platform);
        assertNotNull(retreivedFromStorage);
        assertEquals(idValue,retreivedFromStorage.getIdValue());
        assertEquals(idType,retreivedFromStorage.getIdType().getValue());
        assertEquals(platform,retreivedFromStorage.getPlatform().getValue());
        assertEquals(comment,retreivedFromStorage.getComment());
        assertEquals(modified_by,retreivedFromStorage.getModifiedBy());
        assertEquals(modified_time,retreivedFromStorage.getModifiedTime());

        String new_comment = "another comment";
        String new_modified_by = "user2";
        long new_modified_time = 17394500000000L;

        storage.updateRestrictedId(idValue,idType,platform,new_comment,new_modified_by,new_modified_time);
        retreivedFromStorage = storage.getRestrictedId(idValue, idType, platform);
        assertNotNull(retreivedFromStorage);
        assertEquals(idValue,retreivedFromStorage.getIdValue());
        assertEquals(idType,retreivedFromStorage.getIdType().getValue());
        assertEquals(platform,retreivedFromStorage.getPlatform().getValue());
        assertEquals(new_comment,retreivedFromStorage.getComment());
        assertEquals(new_modified_by,retreivedFromStorage.getModifiedBy());
        assertEquals(new_modified_time,retreivedFromStorage.getModifiedTime());

        storage.deleteRestrictedId(idValue,idType,platform);
        assertNull(storage.getRestrictedId(idValue, idType,platform));
    }

    @Test
    public void testRestrictedIdSearch() throws SQLException {
        storage.createRestrictedId("test1",IdTypeEnumDto.DS_ID.getValue(),PlatformEnumDto.DRARKIV.getValue(),"","test",System.currentTimeMillis());
        storage.createRestrictedId("test2",IdTypeEnumDto.DS_ID.getValue(),PlatformEnumDto.GENERIC.getValue(),"","test",System.currentTimeMillis());
        storage.createRestrictedId("test3",IdTypeEnumDto.DS_ID.getValue(),PlatformEnumDto.DRARKIV.getValue(),"","test",System.currentTimeMillis());
        storage.createRestrictedId("test4",IdTypeEnumDto.STRICT_TITLE.getValue(),PlatformEnumDto.DRARKIV.getValue(),"","test",System.currentTimeMillis());
        storage.createRestrictedId("test5",IdTypeEnumDto.STRICT_TITLE.getValue(),PlatformEnumDto.GENERIC.getValue(),"","test",System.currentTimeMillis());

        assertEquals(5,storage.getAllRestrictedIds(null,null).size());
        assertEquals(3,storage.getAllRestrictedIds(IdTypeEnumDto.DS_ID.getValue(),null).size());
        assertEquals(2,storage.getAllRestrictedIds(IdTypeEnumDto.STRICT_TITLE.getValue(), null).size());
        assertEquals(3,storage.getAllRestrictedIds(null,PlatformEnumDto.DRARKIV.getValue()).size());
        assertEquals(2,storage.getAllRestrictedIds(null,PlatformEnumDto.GENERIC.getValue()).size());
        assertEquals(2,storage.getAllRestrictedIds(IdTypeEnumDto.DS_ID.getValue(),PlatformEnumDto.DRARKIV.getValue()).size());
    }

    @Test
    public void testUniqueRestrictedID() throws SQLException {
        String idValue = "test12345";
        String idType = "dr_produktions_id";
        String platform = "dr";
        String comment = "a comment";
        String modified_by = "user1";
        long modified_time = 1739439979L;

        storage.createRestrictedId(idValue,idType,platform,comment,modified_by,modified_time);


        assertThrows(SQLException.class, () -> storage.createRestrictedId(idValue, idType, platform, comment, modified_by, modified_time));
    }

    @Test
    public void testHoldbackRuleCRUD() throws SQLException {
        String id = "2.02";
        String name = "Aktualitet & Debat";
        int days = 100;

        storage.createDrHoldbackRule(id,name,100);
        assertEquals(days,storage.getDrHoldbackdaysFromID(id));
        assertEquals(days,storage.getDrHoldbackDaysFromName(name));
        DrHoldbackRuleDto holdbackFromStorage = storage.getDrHoldbackFromID(id);
        assertEquals(name,holdbackFromStorage.getName());

        days  = 200;
        storage.updateDrHolbackdaysForId(days,id);
        assertEquals(days,storage.getDrHoldbackdaysFromID(id));
        assertEquals(days,storage.getDrHoldbackDaysFromName(name));

        days  = 300;
        storage.updateDrHolbackdaysForName(days,name);
        assertEquals(days,storage.getDrHoldbackdaysFromID(id));
        assertEquals(days,storage.getDrHoldbackDaysFromName(name));

        assertEquals(1,storage.getAllDrHoldbackRules().size());
        storage.deleteDrHoldbackRule(id);
        assertEquals(-1,storage.getDrHoldbackdaysFromID(id));
        assertEquals(-1,storage.getDrHoldbackDaysFromName(name));
        assertEquals(0,storage.getAllDrHoldbackRules().size());
    }

    @Test
    public void testHoldbackMap() throws SQLException {
        storage.createDrHoldbackRule("test1","Test",100);
        storage.createDrHoldbackRule("test2","Test2",200);

        storage.createDrHoldbackMapping(1000,1000,1200,1900,"test1");
        storage.createDrHoldbackMapping(2000,3000,2200,2900,"test2");
        storage.createDrHoldbackMapping(2000,3000,3200,3900,"test2");


        assertEquals("test1",storage.getHoldbackRuleId(1000,1200));
        assertEquals("test2",storage.getHoldbackRuleId(2500,2900));
        assertEquals(1,storage.getHoldbackRangesForHoldbackId("test1").size());
        assertEquals(2,storage.getHoldbackRangesForHoldbackId("test2").size());
        assertNull(storage.getHoldbackRuleId(2500,9999));
        assertNull(storage.getHoldbackRuleId(9999,1200));
        assertNull(storage.getHoldbackRuleId(9999,9999));
    }

    @Test
    public void testDeleteHoldbackRanges() throws SQLException {
        storage.createDrHoldbackRule("test1","Test",100);
        storage.createDrHoldbackRule("test2","Test2",200);

        storage.createDrHoldbackMapping(1000,1000,1200,1900,"test1");
        storage.createDrHoldbackMapping(2000,3000,2200,2900,"test2");

        assertEquals("test1",storage.getHoldbackRuleId(1000,1200));
        assertEquals("test2",storage.getHoldbackRuleId(2500,2900));

        storage.deleteMappingsForDrHolbackId("test1");

        assertNull(storage.getHoldbackRuleId(1000,1200));
        assertEquals("test2",storage.getHoldbackRuleId(2500,2900));
    }

    @Test
    public void testPerformStorageAction() throws SQLException {
        RestrictedIdOutputDto result = BaseModuleStorage.performStorageAction("Testing", RightsModuleStorage.class, storage -> {
            ((RightsModuleStorage) storage).createRestrictedId("test1", IdTypeEnumDto.DS_ID.getValue(), PlatformEnumDto.DRARKIV.getValue(), "comment", "unittest", System.currentTimeMillis());
            return ((RightsModuleStorage) storage).getRestrictedId("test1", IdTypeEnumDto.DS_ID.getValue(), PlatformEnumDto.DRARKIV.getValue());
        });
        assertEquals("test1",result.getIdValue());
        assertEquals(IdTypeEnumDto.DS_ID,result.getIdType());
        assertEquals(PlatformEnumDto.DRARKIV,result.getPlatform());

    }

}
