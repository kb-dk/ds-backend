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
package dk.kb.present.transform;

import dk.kb.present.TestUtil;
import dk.kb.present.util.TestFileProvider;
import dk.kb.util.yaml.YAML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import static dk.kb.present.solr.EmbeddedSolrTest.PRESERVICA2SOLR;
import static dk.kb.present.transform.XSLTPreservicaSchemaOrgTransformerTest.PRESERVICA2SCHEMAORG;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Helper base for writing XSLT transformation tests.
 */
public abstract class XSLTTransformerTestBase {
    private static final Logger log = LoggerFactory.getLogger(XSLTTransformerTestBase.class);

    /**
     * @return the XSLT used for all tests.
     */
    abstract String getXSLT();

    /**
     * Optional injections for the transformation. Override to provide injections.
     * @return injections for the XSLT transformation.
     */
    public Map<String, String> getInjections() {
        return Collections.emptyMap();
    }

    /**
     * Wrapper for {@link #assertMultiTests(String, Consumer[])} that verifies that the transformed record contains
     * the given {@code substring}.
     * @param recordFile the file to load, transform and test.
     * @param substring must be present in the transformed record.
     */
    public void assertContains(String recordFile, String substring) {
        assertMultiTests(recordFile,
                solrDoc -> assertTrue(solrDoc.contains(substring))
        );
    }

    /**
     * Wrapper for {@link #assertMultiTests(String, Consumer[])} that verifies that the transformed record contains
     * the given {@code substring}.
     * @param recordFile the file to load, transform and test.
     * @param substring must be present in the transformed record.
     * @param message debug message for failed test.
     */
    public void assertContains(String recordFile, String substring, String message) {
        assertMultiTests(recordFile,
                solrDoc -> assertTrue(solrDoc.contains(substring), message)
        );
    }

    /**
     * Wrapper for {@link #assertMultiTests(String, Consumer[])} that verifies that the transformed record does not
     * contain the given {@code substring}.
     * @param recordFile the file to load, transform and test.
     * @param substring must be present in the transformed record.
     */
    public void assertNotContains(String recordFile, String substring) {
        assertMultiTests(recordFile,
                solrDoc -> assertFalse(solrDoc.contains(substring))
        );
    }

    /**
     * Wrapper for {@link #assertMultiTests(String, Consumer[])} that verifies that the transformed record does not
     * contain the given {@code substring}.
     * @param recordFile the file to load, transform and test.
     * @param substring must be present in the transformed record.
     * @param message debug message for failed test.
     */
    public void assertNotContains(String recordFile, String substring, String message) {
        assertMultiTests(recordFile,
                solrDoc -> assertFalse(solrDoc.contains(substring), message)
        );
    }

    /**
     * Checks that internal test files are available and if not, logs a warning and returns.
     * <p>
     * If the check passes, the content of the file {@code record} is transformed using XSLT {@link #getXSLT()}
     * and the given tests are performed on the result.
     * @param record file with a record that is to be transformed using {@link #getXSLT()}.
     * @param tests Zero or more tests to perform on the transformed record.
     */
    @SafeVarargs
    public final void assertMultiTests(String record, Consumer<String>... tests) {
        if (!TestFileProvider.hasSomeTestFiles()) {
            return;  // ensureTestFiles takes care of logging is there are no internal test files
        }
        String solrString;
        try {
            solrString = TestUtil.getTransformedToSolrJsonThroughSchemaJson(PRESERVICA2SCHEMAORG, record);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to fetch and transform '" + record + "' using XSLT '" + getXSLT() + "'", e);
        }

        TestUtil.prettyPrintJson(solrString);

        Arrays.stream(tests).forEach(test -> test.accept(solrString));
    }

}
