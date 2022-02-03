package dk.kb.present.storage;

import dk.kb.util.Resolver;
import dk.kb.util.yaml.YAML;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;

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
class FileStorageTest {

    @Test
    void basicAccess() throws IOException {
        URL albert = Resolver.resolveURL("xml/corpus/albert-einstein.xml");
        assertNotNull(albert, "The test file albert-einstein.xml should be available");
        String rootFolder = Path.of(albert.getPath()).getParent().toString();
        YAML conf = new YAML(Map.of(FileStorage.FOLDER_KEY, rootFolder));
        Storage storage = new FileStorage("test", conf, false);
        assertTrue(storage.getRecord("henrik-hertz.xml").contains("Henrik"));
    }
}