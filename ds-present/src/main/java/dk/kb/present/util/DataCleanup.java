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

import dk.kb.present.util.saxhandlers.ElementExtractionHandler;
import dk.kb.util.DatetimeParser;
import dk.kb.util.MalformedIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
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


    /**
     * Extract startDate for a Preservica record. Extracts the value present at {@code PBCoreDescriptionDocument/pbcoreInstantiation/pbcoreDateAvailable/dateAvailableStart} and
     * parses it as a well formatted ZonedDateTime. Resets the stream after use.
     * @param xml stream representing a preservica record, with the field {@code PBCoreDescriptionDocument/pbcoreInstantiation/pbcoreDateAvailable/dateAvailableStart} present.
     * @return a well formatted ZonedDateTime with the value.
     */
    public static ZonedDateTime getStartDate(InputStream xml) throws ParserConfigurationException, SAXException, IOException {
        factory.setNamespaceAware(false);
        SAXParser saxParser = factory.newSAXParser();

        ElementExtractionHandler handler = new ElementExtractionHandler("/XIP/Metadata/Content/PBCoreDescriptionDocument/pbcoreInstantiation/pbcoreDateAvailable/dateAvailableStart");
        return getCleanZonedDateTime(xml, saxParser, handler);
    }

    /**
     * Extract startDate for a Preservica record. Extracts the value present at {@code PBCoreDescriptionDocument/pbcoreInstantiation/pbcoreDateAvailable/dateAvailableEnd} and
     * parses it as a well formatted ZonedDateTime. Resets the stream after use.
     * @param xml stream representing a preservica record, with the field {@code PBCoreDescriptionDocument/pbcoreInstantiation/pbcoreDateAvailable/dateAvailableEnd} present.
     * @return a well formatted ZonedDateTime with the value.
     */
    public static ZonedDateTime getEndDate(InputStream xml) throws ParserConfigurationException, SAXException, IOException {
        factory.setNamespaceAware(false);
        SAXParser saxParser = factory.newSAXParser();
        ElementExtractionHandler handler = new ElementExtractionHandler("/XIP/Metadata/Content/PBCoreDescriptionDocument/pbcoreInstantiation/pbcoreDateAvailable/dateAvailableEnd");
        return getCleanZonedDateTime(xml, saxParser, handler);
    }

    private static ZonedDateTime getCleanZonedDateTime(InputStream xml, SAXParser saxParser, ElementExtractionHandler handler) throws SAXException, IOException {
        saxParser.parse(xml, handler);
        xml.reset();
        String datetimeString =  handler.getCurrentValue();

        String dateTimeFormat = "yyyy-MM-dd'T'HH:mm:ss[XX][XXX]";
        try {
            return DatetimeParser.parseStringToZonedDateTime(datetimeString, dateTimeFormat);
        } catch (MalformedIOException e) {
            throw new RuntimeException(e);
        }
    }
}
