package dk.kb.license.storage;

import dk.kb.license.config.ServiceConfig;
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

        ServiceConfig.initialize("conf/ds-license*.yaml");
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
        String idType = "dr_produktions_id";
        String platform = "dr";
        String comment = "a comment";
        String modified_by = "user1";
        long modified_time = 1739439979L;

        storage.createRestrictedId(idValue,idType,platform,comment,modified_by,modified_time);
        RestrictedIdOutputDto retreivedFromStorage = storage.getRestrictedId(idValue, idType, platform);
        assertNotNull(retreivedFromStorage);
        assertEquals(idValue,retreivedFromStorage.getIdValue());
        assertEquals(idType,retreivedFromStorage.getIdType());
        assertEquals(platform,retreivedFromStorage.getPlatform());
        assertEquals(comment,retreivedFromStorage.getComment());
        assertEquals(modified_by,retreivedFromStorage.getModifiedBy());
        assertEquals(modified_time,retreivedFromStorage.getModifiedTime());

        List<RestrictedIdOutputDto> ids = storage.getAllRestrictedIds();

        String new_comment = "another comment";
        String new_modified_by = "user2";
        long new_modified_time = 17394500000L;

        storage.updateRestrictedId(idValue,idType,platform,new_comment,new_modified_by,new_modified_time);
        retreivedFromStorage = storage.getRestrictedId(idValue, idType, platform);
        assertNotNull(retreivedFromStorage);
        assertEquals(idValue,retreivedFromStorage.getIdValue());
        assertEquals(idType,retreivedFromStorage.getIdType());
        assertEquals(platform,retreivedFromStorage.getPlatform());
        assertEquals(new_comment,retreivedFromStorage.getComment());
        assertEquals(new_modified_by,retreivedFromStorage.getModifiedBy());
        assertEquals(new_modified_time,retreivedFromStorage.getModifiedTime());

        storage.deleteRestrictedId(idValue,idType,platform);
        assertNull(storage.getRestrictedId(idValue, idType,platform));
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


        assertThrows(SQLException.class, () -> {
            storage.createRestrictedId(idValue, idType, platform, comment, modified_by, modified_time);
        });
    }
}
