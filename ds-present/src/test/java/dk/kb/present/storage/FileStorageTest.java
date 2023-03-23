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
package dk.kb.present.storage;

import dk.kb.util.Resolver;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageTest {

    @Test
    void basicAccess() throws IOException {
        Storage storage = getStorage();
        assertTrue(storage.getRecord("40221e30-1414-11e9-8fb8-00505688346e.xml").contains("Christian VIII"));
    }

    @Test
    void DSRecordAccess() throws IOException {
        Storage storage = getStorage();
        assertTrue(storage.getDSRecord("000332.tif.xml").getData().contains("Bærentzen"));
    }

    private Storage getStorage() throws IOException {
        URL testFile = Resolver.resolveURL("xml/copyright_extraction/40221e30-1414-11e9-8fb8-00505688346e.xml");
        assertNotNull(testFile, "The test file 40221e30-1414-11e9-8fb8-00505688346e.xml should be available");
        Path rootFolder = Path.of(testFile.getPath()).getParent();
        Storage storage = new FileStorage("test", rootFolder, "", false, null, null, false);
        return storage;
    }
}