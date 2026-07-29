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
package dk.kb.util.xml;

import dk.kb.util.string.CallbackReplacer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Checks input for XML-escapes, e.g. {@code &#xABCD;} or {@code &#12345;}.
 * If an escape refers to an illegal XML character (https://en.wikipedia.org/wiki/Valid_characters_in_XML), it is
 * replaced with '?' or a custom String specified in the constructor. If it is not illegal, the escape is left as-is.
 *
 * This sanitizer is conservative and allows only the Non-restricted characters stated on the wikipedia page linked
 * above. This should ensure maximum interoperability.
 *
 * The sanitizer is lenient with regard to syntactical errors in the input. It does not try to fix non-valid constructs
 * such as {@code &#0A;} (hex in a decimal escape) but instead returns the original input.
 */
public class XMLEscapeSanitiser extends CallbackReplacer {
    private static final Logger log = LoggerFactory.getLogger(XMLEscapeSanitiser.class);
    private static final Pattern ESCAPE = Pattern.compile("&#x?[a-fA-F0-9]+;");

    /**
     * Constructs an XML unicode escape validator, where illegal XML codepoints are replaced with {@code ?}.
     */
    public XMLEscapeSanitiser() {
        this("?");
    }

    /**
     * Constructs an XML unicode escape validator.
     * @param replacement returned if an illegal XSML Unicode codepoint is encountered.
     */
    public XMLEscapeSanitiser(String replacement) {
        super(ESCAPE, getEscapeSanitizer(replacement));
    }

    /**
     * Isolates the unicode part of an XML escape (either hex or decimal) and parses that to a long, then checks if the
     * Unicode codepoint is a valid XML character. If it is valid, the full original escape is returned, else the
     * replacement character (default {@code ?} is returned.
     * @param replacement the replacement character for illegal XML characters.
     * @return the original input if the Unicode escape is a valid XML character, else the replacement string.
     */
    public static Function<String, String> getEscapeSanitizer(final String replacement) {
        return escape -> { // &#xABCD; or &#12345; (or &#xAB etc.)
            try {
                long unicode;
                try {
                    if (escape.charAt(2) == 'x') { // &#xABCD;
                        unicode = Long.parseLong(escape.substring(3, escape.length() - 1), 16); // &#xABCD; -> ABCD
                    } else { // &#12345;
                        unicode = Long.parseLong(escape.substring(2, escape.length() - 1)); // &#12345; -> 12345
                    }
                } catch (Exception e) {
                    log.trace("Syntactically invalid escape '{}'. Returning unmodified", escape);
                    return escape;
                }

                // https://en.wikipedia.org/wiki/Valid_characters_in_XML#Non-restricted_characters
                if ((unicode == 0x9) ||                               // C0 control character: Horizontal tab (TAB)
                    (unicode == 0xA) ||                               // C0 control character: Line feed (LF aka NL)
                    (unicode == 0xD) ||                               // C0 control character: Carriage return (CR)
                    ((unicode >= 0x20) && (unicode <= 0x7E)) ||       // Basic Latin block
                    (unicode == 0x85) ||                              // C1 control character: Next line (NEL)
                    ((unicode >= 0xA0) && (unicode <= 0xD7FF)) ||     // Basic Multilingual Plane
                    ((unicode >= 0xE000) && (unicode <= 0xFDCF)) ||   // Basic Multilingual Plane
                    ((unicode >= 0xFDF0) && (unicode <= 0xFFFD)) ||   // Basic Multilingual Plane
                    ((unicode >= 0x10000) && (unicode <= 0x1FFFD)) || // Supplementing planes
                    ((unicode >= 0x20000) && (unicode <= 0x2FFFD)) ||
                    ((unicode >= 0x30000) && (unicode <= 0x3FFFD)) ||
                    ((unicode >= 0x40000) && (unicode <= 0x4FFFD)) ||
                    ((unicode >= 0x50000) && (unicode <= 0x5FFFD)) ||
                    ((unicode >= 0x60000) && (unicode <= 0x6FFFD)) ||
                    ((unicode >= 0x70000) && (unicode <= 0x7FFFD)) ||
                    ((unicode >= 0x80000) && (unicode <= 0x8FFFD)) ||
                    ((unicode >= 0x90000) && (unicode <= 0x9FFFD)) ||
                    ((unicode >= 0xA0000) && (unicode <= 0xAFFFD)) ||
                    ((unicode >= 0xB0000) && (unicode <= 0xBFFFD)) ||
                    ((unicode >= 0xC0000) && (unicode <= 0xCFFFD)) ||
                    ((unicode >= 0xD0000) && (unicode <= 0xDFFFD)) ||
                    ((unicode >= 0xE0000) && (unicode <= 0xEFFFD)) ||
                    ((unicode >= 0xF0000) && (unicode <= 0xFFFFD)) ||
                    ((unicode >= 0x100000) && (unicode <= 0x10FFFD))) {
                    return escape; // All OK
                }
                log.trace("Illegal XML escape character '" + escape + "'");
                return replacement;
            } catch (Exception e) {
                log.warn("Exception processing '" + escape + "'", e);
            }
            return null;
        };
    }

    @Override
    public String toString() {
        return "XMLEscapeSanitiser(" + super.toString() + ")";
    }
}
