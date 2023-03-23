package dk.kb.present.util;

import dk.kb.util.Resolver;
import org.junit.jupiter.api.Test;

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
        String xmlWithDeclaration = Resolver.resolveUTF8String("xml/copyright_extraction/40221e30-1414-11e9-8fb8-00505688346e.xml");
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
}