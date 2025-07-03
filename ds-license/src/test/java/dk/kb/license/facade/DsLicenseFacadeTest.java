package dk.kb.license.facade;

import dk.kb.license.config.ServiceConfig;
import dk.kb.license.model.v1.AuditEntryOutputDto;
import dk.kb.license.model.v1.ChangeTypeEnumDto;
import dk.kb.license.storage.*;
import dk.kb.license.util.H2DbUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DsLicenseFacadeTest  extends DsLicenseUnitTestUtil{

    private static final Logger log = LoggerFactory.getLogger(LicenseModuleStorageTest.class);
    protected static LicenseModuleStorage storage = null;

    @BeforeAll
    public static void beforeClass() throws IOException, SQLException {

        ServiceConfig.initialize("conf/ds-license*.yaml");
        BaseModuleStorage.initialize(DRIVER, URL, USERNAME, PASSWORD);


        H2DbUtil.createEmptyH2DBFromDDL(URL,DRIVER,USERNAME,PASSWORD, List.of("ddl/licensemodule_create_h2_unittest.ddl"));
        storage = new LicenseModuleStorage();
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
    public void testAuditLog()  {

        ArrayList<AuditEntryOutputDto> auditLogEntries = BaseModuleStorage.performStorageAction("test", LicenseModuleStorage.class, storage -> {
            String type1 = "unit_test_type1";
            String type1_en = "unit_test_type1_en";
            long newPresentationTypeId = ((LicenseModuleStorage) storage).persistLicensePresentationType("keyAuditTest", type1, type1_en);
            return ((LicenseModuleStorage) storage).getAuditLogByObjectId(newPresentationTypeId);
        });

//        assertEquals(ChangeTypeEnumDto.CREATE, auditLogEntries.getChangeType());
//        assertEquals(2, list.size());
//        assertEquals("key1", list.get(0).getKey()); // They are returned in same order they saved (H2 db)
//        assertEquals("unit_test_type1_en", list.get(0).getValue_en()); // They are returned in same order they saved (H2 db)
//        assertEquals("key2", list.get(1).getKey());

        //TODO Jonathan Create, update and delete and object of this type:

        //LicenseModuleFacade.persistLicensePresentationType(..); 
        //LicenseModuleFacade.updatePresentationType(..);
        //LicenseModuleFacade.deletePresentationType(..);
        

        //Retrieve all auditlogs for that object  by ObjectId.
        //Validate everything is correct.               
        
    }
    
    
}
