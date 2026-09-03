package dk.kb.storage.facade;

import dk.kb.storage.storage.DsStorage;
import dk.kb.storage.storage.TranscriptionStorageForUnitTest;
import dk.kb.storage.util.DsStorageUnitTestUtil;
import java.lang.invoke.MethodHandles;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

public class TranscriptionFacadeTestDsStorage extends DsStorageUnitTestUtil {
    protected static TranscriptionStorageForUnitTest storage = null;

    @BeforeAll
    public static void beforeClass() throws Exception {
        setupDatabaseForClass(MethodHandles.lookup().lookupClass());
        storage = new TranscriptionStorageForUnitTest();
    }

    /**
     * Delete all records between each unittest. The clearTableRecords is only called from here.
     * The facade class is responsible for committing transactions. So clean up between unittests.
     */
    @BeforeEach
    public void beforeEach() throws Exception {
        storage.clearTableRecords();
        storage.commit();
    }

    /**
     * No reason to delete DB data file after test, since we clear table it before each test.
     * This way you can open the DB in a DB-browser after the unittest and see the result.
     * Just run that single test and look in the DB
     */
    @AfterAll
    public static void afterClass() {
        DsStorage.shutdown();
    }
}
