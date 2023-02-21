package dk.kb.present.storage;

import dk.kb.util.yaml.YAML;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
class StorageControllerTest {

    @Test
    void availableImplementations() {
        Set<String> ids = StorageController.getSupportedStorageIDs();
        assertTrue(ids.contains("folder"), "The implementation 'folder' should be available in " + ids);
    }

    
/* TODO FiX!
    @Test
    void multiBackend() throws Exception {
        YAML multiConf = YAML.resolveMultiConfig("test_setup.yaml");
        Storage storage = StorageController.createStorage(multiConf.getYAMLList(".config.storages").get(0));
        assertTrue(storage.getRecord("albert-einstein.xml").contains("Albert"));
    }
*/

    // Not a proper test as it requires a local ds-storage with test-data ingested as described in the README
    void getRecordsCoreTest() throws Exception {
        YAML localDSS = YAML.resolveMultiConfig("local_ds-storage.yaml");
        Storage storage = StorageController.createStorage(localDSS.getYAMLList(".config.storages").get(0));
        assertTrue(storage.getRecord("doms.radio:albert-einstein.xml").contains("Albert"));

        assertEquals(2, storage.getDSRecords("doms.radio", 0, 2).count(),
                     "Retrieving multiple records should work");
        storage.getDSRecords("doms.radio", 0, 1).forEach(System.out::println);
    }

    // Not a proper test as it requires a local ds-storage with test-data ingested as described in the README
    void getRecordsJSMSS() throws Exception {
        YAML localDSS = YAML.resolveMultiConfig("local_ds-storage.yaml");
        Storage storage = StorageController.createStorage(localDSS.getYAMLList(".config.storages").get(0));

        assertEquals(2, storage.getDSRecords("kb.image.judsam.jsmss", 0, 2).count(),
                     "Retrieving multiple records should work");
        storage.getDSRecords("kb.image.judsam.jsmss", 0, 1).forEach(System.out::println);
    }
}