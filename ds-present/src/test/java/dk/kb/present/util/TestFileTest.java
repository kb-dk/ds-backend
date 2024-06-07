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

import dk.kb.present.TestFiles;
import dk.kb.util.Resolver;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Meta test class that checks if test files are locally available.
 */
public class TestFileTest {
    private static final Logger log = LoggerFactory.getLogger(TestFileTest.class);

    // This always passes (as it should). Is is primarily a demonstration on how to write a unit test
    @Disabled
    void testTestFileExistence() {
        if (!TestFileProvider.ensureTestFiles()) {
            log.warn("Test files are not available. This unit test must be executed at the kb.dk developer network. " +
                    "Test files are cached: Subsequent calls does not require access to the developer network");
            return;
        }

        log.info("Test files are available");
        assertNotNull(Resolver.getPathFromClasspath(TestFiles.PVICA_RECORD_3006e2f8));
    }
}
