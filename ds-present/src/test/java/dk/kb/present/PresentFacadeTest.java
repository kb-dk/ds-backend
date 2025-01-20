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
package dk.kb.present;

import dk.kb.present.config.ServiceConfig;
import dk.kb.present.model.v1.FormatDto;
import dk.kb.util.Resolver;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PresentFacadeTest {

    private HttpServletResponse testResponse;

    @BeforeAll
    static void setup() {
        try {
            ServiceConfig.initialize("test_setup.yaml");
            PresentFacade.warmUp();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void resetMock() {
        testResponse = Mockito.mock(HttpServletResponse.class);
    }

    @Test
    void getRecord() {
        // Throws an Exception if not found
        PresentFacade.getRecord("local.mods:40221e30-1414-11e9-8fb8-00505688346e.xml", FormatDto.MODS);
    }

    @Test
    @Tag("integration")
    void getRecordsMODS() throws IOException {
        if (Resolver.getPathFromClasspath("internal_test_files") == null){
           fail("Missing internal_test_files");
        }
        StreamingOutput out = PresentFacade.getRecords(testResponse, "dsfl", 0L, -1L, FormatDto.MODS, ids -> ids);
        String result = toString(out);
        assertTrue(result.contains("<mods:namePart type=\"family\">Andersen</mods:namePart>"));
    }

    @Test
    @Tag("integration")
    void accessFilterMultiRecords() throws IOException {
        if (Resolver.getPathFromClasspath("internal_test_files") == null){
            fail("Missing internal_test_files");
        }

        // Random subset of the sample files
        final Set<String> ALLOWED = new HashSet<>(List.of(
                "09222b40-dba1-11e5-9785-0016357f605f.xml",
                "e5a0e980-d6cb-11e3-8d2e-0016357f605f.xml",
                "05fea810-7181-11e0-82d7-002185371280.xml"));

        // No access checking
        StreamingOutput out = PresentFacade.getRecords(testResponse, "dsfl", 0L, -1L, FormatDto.MODS, ids -> ids);
        long baseCount = countMETS(out);
        assertTrue(baseCount > 1, "There should be more than 1 record returned when requesting 'dsfl'-records");

        // Filter every other record, sorted order
        out = PresentFacade.getRecords(testResponse, "dsfl", 0L, -1L, FormatDto.MODS,
                ids -> ids.stream()
                        .filter(ALLOWED::contains)
                        .collect(Collectors.toList()));
        long evenCount = countMETS(out);
        assertEquals(ALLOWED.size(), evenCount, "Keeping only allowed METS/MODS records should yield the expected count");
    }

    @Test
    @Tag("integration")
    void solrPreservicaRecords() throws IOException {
        if (Resolver.getPathFromClasspath("internal_test_files") == null){
            fail("Missing internal_test_files");  
        }

        // No access checking
        StreamingOutput out = PresentFacade.getRecords(testResponse, "ds.radiotv", 0L, -1L, FormatDto.SOLRJSON, ids -> ids);
        ByteArrayOutputStream resultBytes = new ByteArrayOutputStream();
        out.write(resultBytes);
        String result = resultBytes.toString(StandardCharsets.UTF_8);
        // Very primitive check just to see if something passes
        assertTrue(result.contains("0b3f6a54-befa-4471-95c0-78bcb1de6300"), "Result should contain expected UUID");
    }

    @Test
    void skiponerror() {
        assertEquals(false, ServiceConfig.getConfig().getBoolean(DSOrigin.STOP_ON_ERROR_KEY, true));
    }

    /**
     * Count the number of occurrences of {@code <mets:mets } (note the trailing space) in {@code out}.
     * This is equivalent to record counting.
     * @param out stream of records in METS/MODS-format.
     * @return the number of records in the stream.
     */
    public static long countMETS(StreamingOutput out) throws IOException {
        Matcher m = METS_PATTERN.matcher(toString(out));
        long count = 0;
        while (m.find()) {
            count++;
        }
        return count;
    }
    private static final Pattern METS_PATTERN = Pattern.compile("<mets:mets ");

    @Test
    @Tag("integration")
    void getRecordsMODSDeclaration() throws IOException {
        if (Resolver.getPathFromClasspath("internal_test_files") == null){
            fail("Missing internal_test_files");                 
        }
        StreamingOutput out = PresentFacade.getRecords(testResponse, "dsfl", 0L, -1L, FormatDto.MODS, ids -> ids);
        String result = toString(out);

        Pattern DECLARATION = Pattern.compile("<[?]xml version=\"1.0\" encoding=\"UTF-8\"[?]>", Pattern.DOTALL);
        Matcher m = DECLARATION.matcher(result);
        int count = 0;
        while (m.find()) {
            count++;
        }
        assertTrue(count <= 1, "There should be at most 1 XML declaration but there was " + count);
    }

    @Test
    @Tag("integration")
    void getRecordsRaw() throws IOException {
        if (Resolver.getPathFromClasspath("internal_test_files") == null){
            fail("Missing internal_test_files");
        }

        try (MockedStatic<FormatDto> FormatDtoMockedStatic = Mockito.mockStatic(FormatDto.class)) {
            final FormatDto RAWBYPASS = Mockito.mock(FormatDto.class);
            Mockito.doReturn("RAWBYPASS").when(RAWBYPASS).getValue();

            FormatDtoMockedStatic.when(FormatDto::values)
                    .thenReturn(new FormatDto[]{
                            FormatDto.RAW,
                            FormatDto.MODS,
                            FormatDto.JSON_LD,
                            FormatDto.SOLRJSON,
                            RAWBYPASS});

            PresentFacade.recordView = RAWBYPASS; //"raw-bypass"; // We don't want to check security here
            StreamingOutput out = PresentFacade.getRecordsRaw(testResponse, "dsfl", 0L, -1L,  ids -> ids, null);
            String result = toString(out);

            assertTrue(result.contains("\"id\":\"40221e30-1414-11e9-8fb8-00505688346e.xml\",\"origin\":null,\"recordType\":null,\"deleted\":false"));
            assertTrue(result.contains(",\n"), "Result should contain a comma followed by newline as it should be a multi-entry JSON array"); // Plain JSON array
            assertTrue(result.endsWith("]\n"), "Result should end with ']' as it should be a JSON array"); // JSON array

        }


    }

    @Test
    @Tag("integration")
    void getRecordsRawLines() throws IOException {
        if (Resolver.getPathFromClasspath("internal_test_files") == null){
            fail("Missing internal_test_files");      
        }
        try (MockedStatic<FormatDto> FormatDtoMockedStatic = Mockito.mockStatic(FormatDto.class)) {
            final FormatDto RAWBYPASS = Mockito.mock(FormatDto.class);
            Mockito.doReturn("RAWBYPASS").when(RAWBYPASS).getValue();

            FormatDtoMockedStatic.when(FormatDto::values)
                    .thenReturn(new FormatDto[]{
                            FormatDto.RAW,
                            FormatDto.MODS,
                            FormatDto.JSON_LD,
                            FormatDto.SOLRJSON,
                            RAWBYPASS});

            PresentFacade.recordView = RAWBYPASS; //"raw-bypass"; // We don't want to check security here
            StreamingOutput out = PresentFacade.getRecordsRaw(testResponse, "dsfl", 0L, -1L, ids -> ids, true);
            String result = toString(out);
            assertTrue(result.contains("\"id\":\"40221e30-1414-11e9-8fb8-00505688346e.xml\",\"origin\":null,\"recordType\":null,\"deleted\":false"));
            assertFalse(result.contains(",\n"), "Result should not contain a comma followed by newline as it should be a multi-entry JSON-Lines");
            assertFalse(result.endsWith("]\n"), "Result should not end with ']' as it should be in JSON-Lin es");
        }
    }

    @Test
    @Tag("integration")
    void getRecordsSolr() throws IOException {
        if (Resolver.getPathFromClasspath("internal_test_files") == null){
            fail("Missing internal_test_files");
        }
        StreamingOutput out = PresentFacade.getRecords(testResponse, "dsfl", 0L, -1L, FormatDto.SOLRJSON, ids -> ids);
        String result = toString(out);
    }

    @Test
    @Tag("integration")
    void errorHandlingTest() throws IOException, InterruptedException {
        if (Resolver.getPathFromClasspath("internal_test_files") == null){
            fail("Missing internal_test_files");
        }

        // No access checking
        StreamingOutput out = PresentFacade.getRecords(testResponse, "ds.radiotv", 0L, -1L, FormatDto.JSON_LD, ids -> ids);

        String result = toString(out);
        assertTrue(result.contains("\"recordsWithErrors\":{\"amount\":1,\"records\":[{\"id\":\"errorRecord.xml\","));
    }

    @Test
    @Tag("integration")
    void errorHandlingTestSolr() throws IOException, InterruptedException {
        if (Resolver.getPathFromClasspath("internal_test_files") == null){
            fail("Missing internal_test_files");
        }

        // No access checking
        StreamingOutput out = PresentFacade.getRecords(testResponse, "ds.radiotv", 0L, -1L, FormatDto.SOLRJSON, ids -> ids);
        String result = toString(out);
        // Solr result should never contain the error marker as that would create an indexing problem.
        assertFalse(result.contains("\"recordsWithErrors\":{\"amount\":1,\"records\":[{\"id\":\"errorRecord.xml\","));
    }

    @Test
    @Tag("integration")
    void errorHandlingConcurrencyTest() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10; i++) {
            executorService.submit(() -> {
                // Do the same task five times on each of the 10 threads
                for (int j = 0; j < 5; j++) {
                    StreamingOutput out = PresentFacade.getRecords(testResponse, "ds.radiotv", 0L, -1L, FormatDto.JSON_LD, ids -> ids);
                    String result;
                    try {
                        result = toString(out);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    // It is especially important that the amount is always=1 in each of the test cases.
                    assertTrue(result.contains("\"recordsWithErrors\":{\"amount\":1,\"records\":[{\"id\":\"errorRecord.xml\","));
                }
            });
        }

        // Shutdown the executor and wait for all tasks to complete
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);
    }

    private static String toString(StreamingOutput out) throws IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            out.write(os);
            return os.toString(StandardCharsets.UTF_8);
        }
    }
    
}