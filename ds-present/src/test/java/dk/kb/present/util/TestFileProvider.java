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
package dk.kb.present.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dk.kb.util.Resolver;
import dk.kb.util.yaml.YAML;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Helper class for downloading non-open test files.
 * <p>
 * This will only work on the developer network at the Royal Danish Library,
 * but the resulting files will be cached for future use.
 *
 * As of 2023-08-24, the recommended way to retrieve internal tets files is to run the aegis command 'kb init'
 * in the project checkout. This will copy the test files to the expected location. The aegis project is available at
 * <a href="https://github.com/kb-dk/aegis/">github.com/kb-dk/aegis</a> (KB Developer access only).
 */
public class TestFileProvider {
    private static final Logger log = LoggerFactory.getLogger(TestFileProvider.class);

    public static final String TEST_CONFIG = "internal-test-setup.yaml";
    // This path only exists for a source checkout, but that's the only place we run unit tests
    public static final Path TEST_FOLDER = Path.of("src/test/resources/internal_test_files");
    public static final String TEST_SERVICE_KEY = "testfileprovider.url";
    public static final String TEST_TYPES_KEY = "testfileprovider.types";

    /**
     * Ensures that test files are available, by checking the local cache and downloading from the KB-internal
     * test file provider if they are not.
     * @return true if at least some test files are available, else false.
     */
    public static boolean ensureTestFiles() {
        Path testConfigFile = Resolver.getPathFromClasspath(TEST_CONFIG);
        if (testConfigFile == null) {
            log.warn("Unable to resolve '{}'. Try running 'kb init' in the local git checkout", TEST_CONFIG);
            return hasSomeTestFiles();
        }

        // Resolve the test file provider service
        YAML testConfig;
        try {
            testConfig = YAML.resolveLayeredConfigs(testConfigFile.toString());
        } catch (IOException e) {
            log.warn("Unable to load '{}' as YAML", testConfigFile);
            return hasSomeTestFiles();
        }

        if (!testConfig.containsKey(TEST_SERVICE_KEY)) {
            log.warn("No entry for key '{}' in test config file '{}'", TEST_SERVICE_KEY, testConfigFile);
            return hasSomeTestFiles();
        }
        if (!testConfig.containsKey(TEST_TYPES_KEY)) {
            log.warn("No entry for key '{}' in test config file '{}'", TEST_TYPES_KEY, testConfigFile);
            return hasSomeTestFiles();
        }
        // http://<internal>>/ds-internal-tests/v1/
        String testFileProviderService = testConfig.getString(TEST_SERVICE_KEY);
        List<String> testTypes = testConfig.getList(TEST_TYPES_KEY);

        // Retrieve file lists from the provider and verify their existence, downloading missing ones
        for (String testType: testTypes) {
            // Get the list of test files
            List<String> filenames = retrieveTestFilenames(testFileProviderService, testType);
            if (filenames == null) {
                return hasSomeTestFiles();
            }
            if (!ensureTestFiles(testFileProviderService, testType, filenames)) {
                return hasSomeTestFiles();
            }
            // Check if at least 1 test file is available
        }
        return true;
    }

    /**
     * Checks that there are files for all {@code filenames} and fetch missing files from the
     * {@code testFileProviderService}.
     * @param testFileProviderService the {@code ds-internal-tests} service.
     * @param testType the type of test file names to retrieve.
     * @param filenames a list of names of test files to fetch.
     * @return true is all files are cached locally, else false.
     */
    private static boolean ensureTestFiles(String testFileProviderService, String testType, List<String> filenames) {
        Path destFolder = Path.of(TEST_FOLDER.toString(), testType);
        // Ensure the test folder exists
        try {
            if (!Files.isDirectory(destFolder)) {
                Files.createDirectories(destFolder);
            }
        } catch (IOException e) {
            log.warn("Unable to create test file folder '{}'", destFolder);
            return false;
        }

        for (String filename: filenames) {
            Path destFile = Path.of(destFolder.toString(), filename);
            if (!Files.isRegularFile(destFile)) {
                try {
                    // resource/tvMetadata/5a5357be-5890-472a-a294-41a99f108936.xml
                    URL fileURL = new URL(testFileProviderService + "resource/" + testType + "/" + filename);
                    log.debug("Test file '{}' is not locally cached. Fetching from '{}'", filename, fileURL);
                    try (FileOutputStream fos = new FileOutputStream(destFile.toFile())) {
                        IOUtils.copy(fileURL, fos);
                    }
                } catch (Exception e) {
                    log.warn("Unable to fetch '" + filename + "' of type '" + testType + "' from '" +
                            testFileProviderService + "'. Giving up on all fetching", e);
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Retrieve the JSON structure with test filenames from the {@code testFileProviderService} and return a list
     * with the filenames.
     * @param testFileProviderService the {@code ds-internal-tests} service.
     * @param testType the type of test file names to retrieve.
     * @return a list of test file names for the given {@code testType} or null if non-resolvable
     */
    private static List<String> retrieveTestFilenames(String testFileProviderService, String testType) {
        try {
            URL listURL = new URL(testFileProviderService + "resourceIDs?type=" + testType);
            log.debug("Retrieving list of test files for type '{}' from '{}'", testType, listURL);
            // {
            //  "resources": [
            //    "5a5357be-5890-472a-a294-41a99f108936.xml",
            //    "44979f67-b563-462e-9bf1-c970167a5c5f.xml"
            //  ]
            //}
            String filenameJSON = IOUtils.toString(listURL, StandardCharsets.UTF_8);
            JsonElement json = JsonParser.parseString(filenameJSON);
            JsonArray jsonArray = json.getAsJsonObject().getAsJsonArray("resources");
            List<String> filenames = new ArrayList<>(jsonArray.size());
            for (int i = 0; i < jsonArray.size(); i++) {
                filenames.add(jsonArray.get(i).getAsString());
            }
            return filenames;
        } catch (Exception e) {
            log.warn("Unable to retrieve list of test files from '" + testFileProviderService + "'", e);
            return null;
        }

    }

    /**
     * Checks if at least one test file is available.
     * @return true if at least 1 test file is avalable.
     */
    public static boolean hasSomeTestFiles() {
        log.debug("Checking for locally cached test files in '{}'", TEST_FOLDER);
        if (!Files.isDirectory(TEST_FOLDER)) {
            log.warn("No test files available. Please read the JavaDoc for TestFileProvider on how to get them");
            return false;
        }
        return hasAnyFile(TEST_FOLDER);
    }

    /**
     * Recursive descend into folder, returning true if any file is found.
     * @param folder any folder.
     * @return true if the folder or any subfolders contain at least 1 file.
     */
    private static boolean hasAnyFile(Path folder) {
        try {
            try (Stream<Path> entries = Files.list(folder)) {
                return entries
                        .map(path -> Files.isRegularFile(path) || hasAnyFile(path))
                        .findAny().isPresent();
            }
        } catch (IOException e) {
            log.warn("Exception while scanning for test files", e);
            return false;
        }
    }

}
