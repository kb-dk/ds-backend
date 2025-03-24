package dk.kb.license.storage;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.license.config.ServiceConfig;
import dk.kb.license.util.H2DbUtil;


/*
 * Setup for the environment for unittest the same way as done in the InitialContext loader in the web container.
 * 
 * 1) Create a h2 database for unittests with schema defined
 * 2) Load the Yaml property files.
 * 
 */
public abstract class DsLicenseUnitTestUtil {
    protected static final String DRIVER = "org.h2.Driver";

    //We need the relative location. This works both in IDE's and Maven.
    protected static final String TEST_CLASSES_PATH = new File(Thread.currentThread().getContextClassLoader().getResource("logback-test.xml").getPath()).getParentFile().getAbsolutePath();
    protected static final String URL = "jdbc:h2:"+TEST_CLASSES_PATH+"/h2/ds_license;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE";
    protected static final String USERNAME = "";
    protected static final String PASSWORD = "";

    private static final Logger log = LoggerFactory.getLogger(DsLicenseUnitTestUtil.class);

    @AfterAll
    public static void afterClass() {
        // No reason to delete DB data file after test, since we clear table it before each test.
        // This way you can open the DB in a DB-browser after a unittest and see the result.
        // Just run that single test and look in the DB
        LicenseModuleStorage.shutdown();
        BaseModuleStorage.shutdown();
    }




}
