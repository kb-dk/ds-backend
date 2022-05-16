package dk.kb.discover.webservice;

import dk.kb.discover.model.v1.BookDto;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.CharArrayWriter;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
class ExportXMLStreamWriterTest {

    @Tag("fast")
    @Test
    void testBasicWriterEmptyXML() {
        assertEquals("<books>\n</books>\n", getBookXML(0, "books", false));
    }

    @Tag("fast")
    @Test
    void testBasicWriterSingleXML() {
        assertEquals("<books><book id=\"0\">\n" +
                     "  <title>book #0</title>\n" +
                     "</book>\n" +
                     "\n" +
                     "</books>\n", getBookXML(1, "books", false));
    }

    @Tag("fast")
    @Test
    void testBasicWriterMultiXML() {
        // Why is there no linebreak after <books>?
        assertEquals("<books><book id=\"0\">\n" +
                     "  <title>book #0</title>\n" +
                     "</book>\n" +
                     "\n" +
                     "<book id=\"1\">\n" +
                     "  <title>book #1</title>\n" +
                     "</book>\n" +
                     "\n" +
                     "</books>\n", getBookXML(2, "books", false));
    }

    private String getBookXML(int count, String wrapperElement, boolean includeNull) {
        try (CharArrayWriter stringW = new CharArrayWriter() ;
             ExportXMLStreamWriter XMLW = new ExportXMLStreamWriter(stringW, wrapperElement, includeNull)) {
            getBooks(count).forEach(XMLW::write);
            XMLW.close();
            return stringW.toString();
        }
    }

    private Stream<BookDto> getBooks(int count) {
        return IntStream.range(0, count)
                .boxed()
                .map(id -> new BookDto().id(Integer.toString(id)).title("book #" + id));
    }
}