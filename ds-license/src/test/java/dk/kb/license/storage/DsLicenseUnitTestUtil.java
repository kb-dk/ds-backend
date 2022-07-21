package dk.kb.license.storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.license.config.ServiceConfig;
import dk.kb.license.util.H2DbUtil;
import dk.kb.util.Resolver;


/*
 * Setup for the environment for unittest the same way as done in the InitialContext loader in the web container.
 * 
 * 1) Create a h2 database for unittests with schema defined
 * 2) Load the Yaml property files.
 * 
 */
public abstract class DsLicenseUnitTestUtil {


    private static final String INSERT_DEFAULT_CONFIGURATION_DDL_FILE = "src/test/resources/ddl/licensemodule_default_configuration.ddl";



    
    public static void insertDefaultConfigurationTypes() throws Exception {
        File insert_ddl_file = new File(INSERT_DEFAULT_CONFIGURATION_DDL_FILE);
        storage.runDDLScript(insert_ddl_file);        
    }

    protected static final String DRIVER = "org.h2.Driver";

    //We need the relative location. This works both in IDE's and Maven.
    protected static final String TEST_CLASSES_PATH = new File(Thread.currentThread().getContextClassLoader().getResource("logback-test.xml").getPath()).getParentFile().getAbsolutePath();
    protected static final String URL = "jdbc:h2:"+TEST_CLASSES_PATH+"/h2/ds_license;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE";
    protected static final String USERNAME = "";
    protected static final String PASSWORD = "";

    protected static LicenseModuleStorage storage = null;

    private static final Logger log = LoggerFactory.getLogger(DsLicenseUnitTestUtil.class);


    @BeforeAll
    public static void beforeClass() throws Exception {

        ServiceConfig.initialize("conf/ds-license*.yaml"); 	    

        H2DbUtil.createEmptyH2DBFromDDL(URL,DRIVER,USERNAME,PASSWORD);        
        LicenseModuleStorage.initialize(DRIVER, URL, USERNAME, PASSWORD);
        storage = new LicenseModuleStorage();


    }

    /*
     * Delete all records between each unittest. The clearTableRecords is only called from here. 
     * The facade class is reponsible for committing transactions. So clean up between unittests.
     */
    @BeforeEach
    public void beforeEach() throws Exception {                     
        storage.clearTableRecords();                     
    }


    @AfterAll
    public static void afterClass() {
        // No reason to delete DB data file after test, since we clear table it before each test.
        // This way you can open the DB in a DB-browser after a unittest and see the result.
        // Just run that single test and look in the DB
        LicenseModuleStorage.shutdown();


    }




}
