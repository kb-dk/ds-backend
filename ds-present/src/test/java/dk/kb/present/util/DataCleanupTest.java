package dk.kb.present.util;

import dk.kb.present.TestFiles;
import dk.kb.util.Resolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;

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
class DataCleanupTest {

    @Test
    public void testDeclarationRemoval() throws IOException {
        String xmlWithDeclaration = Resolver.resolveUTF8String(TestFiles.CUMULUS_RECORD_40221e30);
        assertTrue(xmlWithDeclaration.startsWith("<?xml version"), "The test XML should have a declaration");
        String stripped = DataCleanup.removeXMLDeclaration(xmlWithDeclaration);
        assertTrue(stripped.startsWith("<mets:mets"), "The cleaned test XML should not have a declaration");
    }

    @Test
    public void testDeclarationRemovalNonExisting() {
        String xml = "<foo>bar</foo>";
        assertEquals(xml, DataCleanup.removeXMLDeclaration(xml),
                     "XML without declaration should be left unchanged");
    }

    @Test
    public void testDeclarationRemovalFaulty() {
        String xml = "<foo>bar</foo>\n" +
                     "<?xml encoding=\"UTF-8\"   version=\"1.0\" ?>";
        assertEquals(xml, DataCleanup.removeXMLDeclaration(xml),
                     "XML with faulty declaration should be left unchanged");
    }

    @ParameterizedTest
    @CsvSource({
            "1967-12-19T16:40Z,1967-12-19T16:40:00Z", // utc datetime without seconds
            "2017-03-07T16:06+01:00,2017-03-07T15:06:00Z",
            "2017-03-07T16:06:13+01:00,2017-03-07T15:06:13Z",
            "2017-03-07T16:06:13.1+01:00,2017-03-07T15:06:13Z",
            "2017-03-07T16:06:13.22+01:00,2017-03-07T15:06:13Z",
            "2017-03-07T16:06:13.333+01:00,2017-03-07T15:06:13Z",
            "2017-03-07T16:06:13.4444+01:00,2017-03-07T15:06:13Z",
            "2017-03-07T16:06:13.55555+01:00,2017-03-07T15:06:13Z",
            "2017-03-07T16:06:13.666666+01:00,2017-03-07T15:06:13Z",
            "2017-03-07T16:06:13.7777777+01:00,2017-03-07T15:06:13Z",
            "2017-03-07T16:06:13.88888888+01:00,2017-03-07T15:06:13Z",
            "2017-03-07T16:06:13.999999999+00:00,2017-03-07T16:06:13Z",
            "2017-03-07T16:06:13.999999999+01:00,2017-03-07T15:06:13Z",
            "2017-03-07T16:06:13.999999999+02:00,2017-03-07T14:06:13Z",
    })
    public void standardizeDateTimeToUtc_whenValidDateTime_thenReturnUtcDateTimeWithoutMilliseconds(String input, String expected) {
        // Assert
        assertEquals(expected, DataCleanup.standardizeDateTimeToUtc(input));
    }

    @ParameterizedTest
    @CsvSource({
            "2008-02-1206:30:00+01:00,2008-02-12T05:30:00Z", // Missing T between date and timestamp
            "2008-02-12T06:30:00+0100,2008-02-12T05:30:00Z", // Missing ":" in timezone
            "[2008-02-12T06:30:00+0100],2008-02-12T05:30:00Z", // Bracket
            " 2008-02-12T06:30 :00 +0100 ,2008-02-12T05:30:00Z", // Space test
            "2008-02-12T06:30:00+01000,2008-02-12T05:30:00Z", // 1 zero extra in timezone
            " [2008 -02 -12 T06:30:00+01000] ,2008-02-12T05:30:00Z", // Multi test
            "2008-02-12T06:30:00+010,2008-02-12T05:30:00Z", // Wrong number og zeroes in timezone
            "2008-02-12T06:30:00+00000100000000,2008-02-12T05:30:00Z", // Wrong number og zeroes in timezone Version 2
            "2008-02-12T06:30:00+000000012000000,2008-02-11T18:30:00Z", // Wrong number og zeroes in timezone Version 3
    })
    public void standardizeDateTimeToUtc_whenInvalidDateTime_thenReturnRepairedUtcDatetimeWithoutMilliseconds(String input, String expected) {
        // Assert
        assertEquals(expected, DataCleanup.standardizeDateTimeToUtc(input));
    }

    @Test
    public void standardizeDateTimeToUtc_whenPlaceholderDate_thenThrowRuntimeException() {
        // Arrange
        String placeholderDateTime = "åååå-mm-ddTtt:mm:ss+0200"; // Garbage data
        String expectedMessage = "dk.kb.util.MalformedIOException: Could not parse/repair date: åååå-mm-ddTtt:mm:ss+0200";

        // Act
        Exception exception = assertThrowsExactly(RuntimeException.class, () -> DataCleanup.standardizeDateTimeToUtc(placeholderDateTime));

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
    }
}