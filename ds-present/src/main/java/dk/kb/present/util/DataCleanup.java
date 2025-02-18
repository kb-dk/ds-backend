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

import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.kb.present.util.saxhandlers.ElementsExtractionHandler;
import dk.kb.util.DatetimeParser;
import dk.kb.util.MalformedIOException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper methods for cleaning data before passing it on.
 */
public class DataCleanup {
    private static final SAXParserFactory factory = SAXParserFactory.newInstance();
    private static final Logger log = LoggerFactory.getLogger(DataCleanup.class);

    /**
     * If the XML block starts with an XML declaration (https://www.tutorialspoint.com/xml/xml_declaration.htm)
     * it will be removed. This is typically used for representing multiple XML blocks as a list of elements.
     *
     * The method uses a regular expression: It is fast, but does not validate the input.
     * Only the first declaration is removed and only if it positioned at the start of the XML.
     * @param xml a single XML block.
     * @return the XML block without declaration.
     */
    public static String removeXMLDeclaration(String xml) {
        Matcher m = XML_DECLARATION.matcher(xml);
        if (!m.find()) {
            return xml;
        }
        if (m.start() != 0) {
            log.warn("Found XML declaration at index " + m.start() + " with expected index 0. Skipping removal");
            return xml;
        }
        return m.replaceFirst("");
    }
    private static final Pattern XML_DECLARATION = Pattern.compile("[\n ]*<[?]xml [^?]*[?]>\n?", Pattern.DOTALL);

    public static ZonedDateTime getCleanZonedDateTimeFromString(String datetime){
        String dateTimeFormat = "yyyy-MM-dd'T'HH:mm:ss[XX][XXX]";
        try {
            return DatetimeParser.parseStringToZonedDateTime(datetime, dateTimeFormat);
        } catch (MalformedIOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Extract all needed values from a preservica record. These values are either tricky values such as dates, where we know that extra parsing is needed or values that are
     * used in multiple parts of the processing of the record.
     * @param content of the record. i.e. the XML data.
     * @param recordId of the processed record. Used for logging and debugging.
     * @return a {@link ExtractedPreservicaValues}-object containing the extracted values.
     */
    public static ExtractedPreservicaValues extractValuesFromPreservicaContent(String content, String recordId) throws ParserConfigurationException, SAXException {
        try (InputStream xml = IOUtils.toInputStream(content, StandardCharsets.UTF_8)) {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(false);
            SAXParser saxParser = factory.newSAXParser();

            ElementsExtractionHandler handler = new ElementsExtractionHandler(recordId);
            saxParser.parse(xml, handler);

            return handler.getDataValues();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
