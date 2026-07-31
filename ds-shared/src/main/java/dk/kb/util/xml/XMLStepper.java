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

import dk.kb.util.MutablePair;
import dk.kb.util.reader.CharSequenceReader;
import dk.kb.util.reader.ThreadedPiper;
import dk.kb.util.string.Strings;

import javax.xml.stream.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Helper class for stream oriented processing of XML.
 */
public class XMLStepper {

    private static final XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
    private static final XMLOutputFactory xmlOutFactory = XMLOutputFactory.newInstance();
    static {
        xmlFactory.setProperty(XMLInputFactory.IS_COALESCING, true);
        // No resolving of external DTDs
        xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);

        // Need to repair namespaces for piping of subset of XML
        // https://stackoverflow.com/questions/38970894/xmlstreamexception-the-namespace-uri-has-not-been-bound-to-a-prefix-ibm-jre
        xmlOutFactory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, Boolean.TRUE);
    }

    /**
     * Iterates through the start tags in the stream until the current sub tree in the DOM is depleted
     * Leaves the cursor after END_ELEMENT.
     * @param xml the stream to iterate.
     * @param callback called for each start element.
     * @throws XMLStreamException if the xml was not valid or XML processing failed for other reasons.
     */
    public static void iterateTags(XMLStreamReader xml, Callback callback) throws XMLStreamException {
        iterateTags(xml, false, callback);
    }

    /**
     * Iterates through the start tags in the stream until the current sub tree in the DOM is depleted
     * Leaves the cursor after END_ELEMENT.
     * @param xml the stream to iterate.
     * @param lenient if true, the iterator tries to compensate for element-exceeding advances in the XML stream by the callback.
     * @param callback called for each start element.
     * @throws XMLStreamException if the xml was not valid or XML processing failed for other reasons.
     */
    public static void iterateTags(XMLStreamReader xml, boolean lenient, Callback callback) throws XMLStreamException {
        List<String> tagStack = new ArrayList<String>(10);
        while (true) {
            if (xml.getEventType() == XMLStreamReader.START_ELEMENT) {
                String currentTag = xml.getLocalName();
                tagStack.add(currentTag);
                switch(callback.elementStart2(xml, tagStack, currentTag)) {
                    case requests_stop_success:
                    case requests_stop_fail:
                        callback.end();
                        return;
                    case no_action: xml.next();
                    // Ignore no_action
                }
                continue;
            }
            if (xml.getEventType() == XMLStreamReader.END_ELEMENT) {
                String currentTag = xml.getLocalName();
                if (tagStack.isEmpty()) {
                    callback.end();
                    return;
                }
                if (!currentTag.equals(tagStack.get(tagStack.size()-1))) {
                    boolean fail = true;
                    if (lenient) {
                        fail = !reduceStack(tagStack, currentTag);
                    }
                    if (fail) {
                        throw new IllegalStateException(String.format(Locale.ROOT, 
                                "Encountered end tag '%s' where '%s' from the stack %s were expected",
                                currentTag, tagStack.get(tagStack.size() - 1), Strings.join(tagStack, ", ")));
                    }
                }
                callback.elementEnd(tagStack.remove(tagStack.size() - 1));
            } else if (xml.getEventType() == XMLStreamReader.END_DOCUMENT) {
                callback.end();
                return;
            }
            try {
                xml.next();
            } catch (XMLStreamException e) {
                throw new XMLStreamException(String.format(Locale.ROOT, 
                        "XMLStreamException with lenient=%b, stack=[%s], type=%s, content='%s'",
                        lenient, Strings.join(tagStack), XMLUtil.eventID2String(xml.getEventType()),
                        xml.getEventType() == XMLStreamReader.CHARACTERS ? xml.getText() : "N/A"),
                        e);
            }
        }
    }

   /**
     * Reduce the stack from the end until the last element is equal to tag.
     * @param stack a stack of tags.
     * @param tag    the tag to reduce to
     * @return true if it was possible to reduce the stack.
     */
    private static boolean reduceStack(List<String> stack, String tag) {
        for (int i = 0 ; i < stack.size() ; i++) {
            if (stack.get(i).equals(tag)) {
                // Found a match, so we know we can reduce
                while (!tag.equals(stack.get(stack.size()-1))) {
                   stack.remove(stack.size()-1);
                }
                return true;
            }
        }
        return false;  // TODO: Implement this
    }

    /**
     * Shorthand for {@link #isWellformed(javax.xml.stream.XMLStreamReader)}.
     * @param xml the XML to check.
     * @return true if the XML is well formed.
     */
    public static boolean isWellformed(String xml) {
        try {
            return isWellformed(xmlFactory.createXMLStreamReader(new StringReader(xml)));
        } catch (Exception e) {
            return false;
        }
    }
    /**
     * Iterates the given XML stream and checks if it is well-formed (basic syntax check).
     * No check for validity is performed.
     * @param xml the input to be checked. This will be iterated all the way through and closed, unless an
     *            error is encountered during iteration.
     * @return true if the XML is well-formed, else false.
     */
    public static boolean isWellformed(XMLStreamReader xml) {
        try {
            while (xml.hasNext()) {
                xml.next();
            }
            xml.close();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * Equivalent to {@link #pipeXML(javax.xml.stream.XMLStreamReader, javax.xml.stream.XMLStreamWriter, boolean)} but
     * returns the sub XML as a String instead of piping the result. For performance, it is recommended to
     * @param in must be positioned at START_ELEMENT and be coalescing.
     * @param failOnError if true, unrecognized elements will result in an UnsupportedOperationException.
     *                    if false, unrecognized elements will be ignored.
     * @return the sub XML as a String.
     * @throws XMLStreamException if in was faulty.
     */
    public static String getSubXML(XMLStreamReader in, boolean failOnError) throws XMLStreamException {
        return getSubXML(in, failOnError, false);
/*        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XMLStreamWriter out = xmlOutFactory.createXMLStreamWriter(os);
        pipeXML(in, out, failOnError);
        return os.toString();*/
    }

    /**
     * Equivalent to
     * {@link #pipeXML(javax.xml.stream.XMLStreamReader, javax.xml.stream.XMLStreamWriter, boolean, boolean)} but
     * returns the sub XML as a String instead of piping the result.
     *
     * Note: This methods is resilient against the multiple root-problem in pipeXML. This also means that the returned
     *       String is not necessarily valid XML.
     * @param in must be positioned at START_ELEMENT and be coalescing.
     * @param failOnError if true, unrecognized elements will result in an UnsupportedOperationException.
     *                    if false, unrecognized elements will be ignored.
     * @return the sub XML as a String.
     * @param onlyInner  if true, the start- and end-tag of the current element are not piped to out.
     * @throws XMLStreamException if in was faulty.
     */
    public static String getSubXML(XMLStreamReader in, boolean failOnError, boolean onlyInner)
            throws XMLStreamException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XMLStreamWriter out = xmlOutFactory.createXMLStreamWriter(os);
        out.writeStartElement("a");
        pipeXML(in, out, failOnError, onlyInner);
        out.flush();
        String xml = os.toString(StandardCharsets.UTF_8);
        // TODO: How can this ever be less than 3? A search for 'foo' against summon has this problem in the test gui
        return xml.length() < 3 ? "" : xml.substring(3); // We remove the start <a> from the String
    }

    /**
     * Streaming wrap of {@link #replaceElementText(XMLStreamReader, XMLStreamWriter, boolean, ContentReplaceCallback)}.
     * The processing runs in its own thread, shared between calls with a thread pool. For very small inputs, it is
     * better to call {@link #replaceElementText(String, ContentReplaceCallback)} or
     * {@link #replaceElementText(XMLStreamReader, XMLStreamWriter, boolean, ContentReplaceCallback)}.
     * @param xml the input for the replacer. This will be parsed as XML.
     * @param replacer the replacer for handling element text.
     * @return a stream with the output from the replacement of text in the input xml.
     * @throws IOException if the xml content could not be read.
     */
    public static InputStream streamingReplaceElementText(
            final InputStream xml, final ContentReplaceCallback replacer) throws IOException {
        return ThreadedPiper.getDeferredStream(new ThreadedPiper.Producer() {
            @Override
            public void process(OutputStream out) throws IOException {
                XMLStreamReader in;
                XMLStreamWriter xmlOut;
                try {
                    in = xmlFactory.createXMLStreamReader(xml);
                } catch (XMLStreamException e) {
                    throw new IOException("Unable to construct XML reader", e);
                }
                try {
                    xmlOut = xmlOutFactory.createXMLStreamWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
                } catch (XMLStreamException e) {
                    throw new IOException("Unable to construct XML reader", e);
                }
                replacer.setOut(xmlOut);
                try {
                    pipeXML(in, xmlOut, true, false, replacer);
                } catch (XMLStreamException e) {
                    throw new IOException("Exception piping throusg XML text replacer", e);
                }
                out.flush();
            }
        });
    }

    /**
     * Wrapper for {@link #replaceElementText(XMLStreamReader, XMLStreamWriter, boolean, ContentReplaceCallback)}.
     * @param xml      the XML to process.
     * @param replacer the replacer for handling element text.
     * @return the xml with element text being processed by the replacer.
     * @throws XMLStreamException if parsing fails.
     */
    public static String replaceElementText(String xml, ContentReplaceCallback replacer) throws XMLStreamException {
        XMLStreamReader in = xmlFactory.createXMLStreamReader(new CharSequenceReader(xml));
        StringWriter sw = new StringWriter();
        XMLStreamWriter out = xmlOutFactory.createXMLStreamWriter(sw);
        replacer.setOut(out);
        pipeXML(in, out, true, false, replacer);
        out.flush();
        return sw.toString();
    }

    /**
     * Traverses all parts in the given element, including sub elements etc., and pipes the parts to out.
     * The callback allows for selective replacement of texts inside of elements.
     * Leaves in positioned immediately after the END_ELEMENT matching the START_ELEMENT.
     * <p>
     * Note: The method does not repair namespaces automatically.
     * To ensure namespaces are handled properly, set the {@link XMLOutputFactory#IS_REPAIRING_NAMESPACES} to true using
     * {@link XMLOutputFactory#setProperty(String, Object)} before creating an {@link XMLStreamWriter}.
     *
     * @param in must be positioned at START_ELEMENT and be coalescing.
     * @param out the destination for the traversed XML.
     * @param failOnError if true, unrecognized elements will result in an UnsupportedOperationException.
     *                    if false, unrecognized elements will be ignored.
     * @param replacer at each elementStart, the replacer will be called allowing for selective replacement of element
     *                 text.
     * @throws XMLStreamException if in was faulty.
     */
    public static void replaceElementText(
            XMLStreamReader in, XMLStreamWriter out, boolean failOnError, ContentReplaceCallback replacer)
            throws XMLStreamException {
        pipeXML(in, out, failOnError, false, replacer);
    }

    /**
     * Traverses all parts in the given element, including sub elements etc., and pipes the parts to out.
     * Used for copying snippets verbatim from one XML structure to another.
     * Leaves in positioned immediately after the END_ELEMENT matching the START_ELEMENT.
     * <p>
     * Note: The method does not repair namespaces automatically.
     * To ensure namespaces are handled properly, set the {@link XMLOutputFactory#IS_REPAIRING_NAMESPACES} to true using
     * {@link XMLOutputFactory#setProperty(String, Object)} before creating an {@link XMLStreamWriter}.
     * @param in must be positioned at START_ELEMENT and be coalescing.
     * @param out the destination for the traversed XML.
     * @param failOnError if true, unrecognized elements will result in an UnsupportedOperationException.
     *                    if false, unrecognized elements will be ignored.
     * @throws XMLStreamException if in was faulty.
     */
    public static void pipeXML(XMLStreamReader in, XMLStreamWriter out, boolean failOnError) throws XMLStreamException {
        pipeXML(in, out, failOnError, false, null);
    }

    /**
     * Traverses all parts in the given element, including sub elements etc., and pipes the parts to out.
     * Used for copying snippets verbatim from one XML structure to another.
     * Leaves in positioned immediately after the END_ELEMENT matching the START_ELEMENT.
     * <p>
     * Note: The method does not repair namespaces automatically.
     * To ensure namespaces are handled properly, set the {@link XMLOutputFactory#IS_REPAIRING_NAMESPACES} to true using
     * {@link XMLOutputFactory#setProperty(String, Object)} before creating an {@link XMLStreamWriter}.
     * <p>
     * Warning: Skipping the outer element is dangerous as the outer element can contain multiple inner elements.
     * If the destination (out) is empty and in contains multiple sub-elements, the piping will fail with an Exception
     * stating "Trying to output second root". In order to avoid that, the destination needs to have at least one
     * open element.
     * @param in must be positioned at START_ELEMENT and be coalescing.
     * @param out the destination for the traversed XML.
     * @param failOnError if true, unrecognized elements will result in an UnsupportedOperationException.
     *                    if false, unrecognized elements will be ignored.
     * @param onlyInner  if true, the start- and end-tag of the current element are not piped to out.
     * @throws XMLStreamException if in was faulty.
     */
    public static void pipeXML(XMLStreamReader in, XMLStreamWriter out, boolean failOnError, boolean onlyInner)
            throws XMLStreamException {
        pipeXML(in, out, !failOnError, onlyInner, null);
    }

    /**
     * Traverses all parts in the given element, including sub elements etc., and pipes the parts to out.
     * Used for copying snippets verbatim from one XML structure to another.
     * Leaves in positioned immediately after the END_ELEMENT matching the START_ELEMENT.
     * <p>
     * Note: The method does not repair namespaces automatically.
     * To ensure namespaces are handled properly, set the {@link XMLOutputFactory#IS_REPAIRING_NAMESPACES} to true using
     * {@link XMLOutputFactory#setProperty(String, Object)} before creating an {@link XMLStreamWriter}.
     * <p>
     * Warning: Skipping the outer element is dangerous as the outer element can contain multiple inner elements.
     * If the destination (out) is empty and in contains multiple sub-elements, the piping will fail with an Exception
     * stating "Trying to output second root". In order to avoid that, the destination needs to have at least one
     * open element.
     * @param in must be positioned at START_ELEMENT and be coalescing.
     * @param out the destination for the traversed XML.
     * @param ignoreErrors if false, unrecognized elements will result in an UnsupportedOperationException.
     *                     if true, unrecognized elements will be ignored.
     * @param onlyInner    if true, the start- and end-tag of the current element are not piped to out.
     * @param callback     will be called for all elementStarts. If {@link Callback#elementStart} returns true,
     *                     piping will assume that the element has been processed and will skip it. In that case it is
     *                     the responsibility of the callback to leave {@code in} at the END_ELEMENT corresponding to
     *                     the START_ELEMENT.
     * @return true if piping has finished.
     * @throws XMLStreamException if in was faulty.
     */
    public static boolean pipeXML(XMLStreamReader in, XMLStreamWriter out, boolean ignoreErrors, boolean onlyInner,
                                   Callback callback) throws XMLStreamException {
        if (in.getProperty(XMLInputFactory.IS_COALESCING) == null ||
            Boolean.TRUE != in.getProperty(XMLInputFactory.IS_COALESCING)) {
            throw new IllegalArgumentException("The XMLInputStream must be coalescing but was not");
        }
        if (!ignoreErrors) {
            return pipeXML(in, out, false, onlyInner, new ArrayList<String>(), callback);
        }
        try {
            return pipeXML(in, out, true, onlyInner, new ArrayList<String>(), callback);
        } catch (XMLStreamException e) {
            // Ignoring exception as ignoreErrors == true
            out.flush();
        }
        return true;
    }
    /**
     * @param in           XML, optionally positioned inside the stream.
     * @param out          the piped content will be send to this.
     * @param ignoreErrors if true, unknown element types will be ignored. If false, an exception will be thrown.
     * @param onlyInner    if true, in must be positioned at an elementStart, which will be skipped.
     * @param elementStack nested elements encountered so far.
     * @param callback     will be called for all elementStarts. If {@link Callback#elementStart} returns true,
     *                     piping will assume that the element has been processed and will skip it. In that case it is
     *                     the responsibility of the callback to leave {@code in} at the END_ELEMENT corresponding to
     *                     the START_ELEMENT.
     * @return true if piping has finished.
     * @throws XMLStreamException if processing failed due to an XML problem.
     */
    private static boolean pipeXML(XMLStreamReader in, XMLStreamWriter out, boolean ignoreErrors, boolean onlyInner,
                                   List<String> elementStack, Callback callback) throws XMLStreamException {
        if (onlyInner) {
            if (XMLStreamReader.START_ELEMENT != in.getEventType()) {
                throw new IllegalStateException(
                        "onlyInner == true, but the input was not positioned at START_ELEMENT. Current element is "
                        + XMLUtil.eventID2String(in.getEventType()));
            }
            String element = in.getLocalName();
            elementStack.add(element);
            in.next();
        }

        // TODO: Add better namespace support by matching NameSpaceContexts for in and out

        while (true) {
            switch (in.getEventType()) {
                case XMLStreamReader.START_DOCUMENT: {
                    if ((in.getEncoding() != null && !in.getEncoding().isEmpty()) ||
                        (in.getVersion() != null && !"1.0".equals(in.getVersion()))) {
                        // Only write a declaration if the source has one
                        out.writeStartDocument(in.getEncoding(), in.getVersion());
                    }
                    break;
                }
                case XMLStreamReader.END_DOCUMENT: {
                    out.writeEndDocument();
                    out.flush();
                    return true;
                }

                case XMLStreamReader.START_ELEMENT: {
                    String element = in.getLocalName();
                    elementStack.add(element);
                    Callback.PROCESS_ACTION callResult = callback == null ? Callback.PROCESS_ACTION.no_action :
                            callback.elementStart2(in, elementStack, element);
                    switch (callResult) {
                        case no_action:
                            copyStartElement(in, out);
                            in.next();
                            if (pipeXML(in, out, ignoreErrors, false, elementStack, callback)) {
                                out.flush();
                                return true;
                            }
                            break;
                        case called_next: // callback handled the element so we do not pipe the END_ELEMENT
                            if (XMLStreamReader.END_ELEMENT != in.getEventType()) {
                                throw new IllegalStateException(String.format(Locale.ROOT, 
                                        "Callback for %s returned calles_next, but did not position the XML stream at "
                                        + "END_ELEMENT. Current eventType is %s",
                                        Strings.join(elementStack, ", "),
                                        XMLUtil.eventID2String(in.getEventType())));
                            }
                            elementStack.remove(elementStack.size()-1);
                            break;
                        case requests_stop_success:
                            out.writeEndDocument();
                            out.flush();
                            return true;
                        case requests_stop_fail:
                            out.flush(); // No end document af processing failed
                            return false;
                        default: throw new UnsupportedOperationException("Unknown PROCESS_ACTION '" + callResult + "'");
                    }
                    break;
                }
                case XMLStreamReader.END_ELEMENT: {
                    if (elementStack.isEmpty()) {
                        if (callback != null) {
                            callback.end();
                        }
                        out.flush();
                        return true;
                    }
                    String element = in.getLocalName();
                    if (!element.equals(elementStack.get(elementStack.size()-1))) {
                        throw new IllegalStateException(String.format(Locale.ROOT, 
                                "Encountered end tag '%s' where '%s' from the stack %s were expected",
                                element, elementStack.get(elementStack.size()-1), Strings.join(elementStack, ", ")));
                    }
                    String popped = elementStack.remove(elementStack.size()-1);
                    if (callback != null && !(elementStack.isEmpty() && onlyInner)) {
                        callback.elementEnd(popped);
                    }

                    if (!elementStack.isEmpty() || !onlyInner) {
                        out.writeEndElement();
                    }
                    if (elementStack.isEmpty()) {
                        in.next();
                    }
                    return elementStack.isEmpty();
                }

                case XMLStreamReader.SPACE:
                case XMLStreamReader.CHARACTERS: {
                    out.writeCharacters(in.getText());
                    break;
                }
                case XMLStreamReader.CDATA: {
                    out.writeCData(in.getText());
                    break;
                }
                case XMLStreamReader.COMMENT: {
                    out.writeComment(in.getText());
                    break;
                }
                default: if (!ignoreErrors) {
                    throw new UnsupportedOperationException(
                            "pipeXML does not support event type " + XMLUtil.eventID2String(in.getEventType()));
                }
            }
            in.next();
        }
    }

    private static void copyStartElement(XMLStreamReader in, XMLStreamWriter out) throws XMLStreamException {
        if (in.getPrefix() == null || in.getPrefix().isEmpty()) {
            if (in.getNamespaceURI() == null || in.getNamespaceURI().isEmpty()) {
                out.writeStartElement(in.getLocalName());
            } else {
                out.writeStartElement(in.getPrefix(), in.getLocalName(), in.getNamespaceURI());
            }
        } else {
            if (in.getNamespaceURI() == null || in.getNamespaceURI().isEmpty()) {
                throw new XMLStreamException(
                        "Encountered element '" + in.getLocalName() + "' with prefix '" + in.getPrefix()
                        + "' but no namespace URI");
            } else {
                out.writeStartElement(in.getPrefix(), in.getLocalName(), in.getNamespaceURI());
            }
        }
        for (int i = 0 ; i < in.getNamespaceCount() ; i++) {
            out.writeNamespace(in.getNamespacePrefix(i), in.getNamespaceURI(i));
        }
        for (int i = 0 ; i < in.getAttributeCount() ; i++) {
            if (in.getAttributeNamespace(i) == null || in.getAttributeNamespace(i).isEmpty()) {
                out.writeAttribute(in.getAttributeLocalName(i), in.getAttributeValue(i));
            } else {
                out.writeAttribute(in.getAttributeNamespace(i), in.getAttributeLocalName(i),
                                   in.getAttributeValue(i));
            }
        }
    }

    /**
     * Steps through the provided XML and returns the text content of the first element with the given tag.
     * @param xml the XML to extract text from.
     * @param tag the designation of the element to extract text from.
     * @return the text of the element with the given tag or null if the tag could not be found. If the tag is empty,
     *         the empty String will be returned.
     * @throws XMLStreamException if the xml was not valid or XML processing failed for other reasons.
     */
    public static String getFirstElementText(CharSequence xml, String tag) throws XMLStreamException {
        final MutablePair<Boolean, String> result = new MutablePair<Boolean, String>(false, null);
        XMLStepper.iterateElements(xmlFactory.createXMLStreamReader(new CharSequenceReader(xml)),
                                   "", tag, new XMLStepper.XMLCallback() {
            @Override
            public void execute(XMLStreamReader xml) throws XMLStreamException {
                if (result.getKey().equals(false)) { // We only want the first one
                    result.setKey(true);
                    result.setValue(xml.getElementText());
                }
            }
        });
        return result.getValue();
    }

    /**
     * Extraction of an XPath-like query, returning at most 1 result.
     * @param xml        the XML to extract text from.
     * @param fakeXPath  the fakeXPath to evaluate.
     * @see FakeXPath
     * @return the result of the evaluation or null if there was no match.
     * @throws XMLStreamException if the xml was not valid or XML processing failed for other reasons.
     */
    public static String evaluateFakeXPath(CharSequence xml, String fakeXPath) throws XMLStreamException {
        return evaluateFakeXPathsSingleResultsPremade(xml, parsePaths(Collections.singletonList(fakeXPath))).get(0);
    }

    /**
     * Extraction of an XPath-like query, returning at most 1 result.
     * @param xml        the XML to extract text from.
     * @param fakeXPath  the fakeXPath to evaluate.
     * @see FakeXPath
     * @return the result of the evaluation or null if there was no match.
     * @throws XMLStreamException if the xml was not valid or XML processing failed for other reasons.
     */
    public static String evaluateFakeXPath(CharSequence xml, FakeXPath fakeXPath) throws XMLStreamException {
        return evaluateFakeXPathsSingleResultsPremade(xml, Collections.singletonList(fakeXPath)).get(0);
    }
    /**
     * Extraction of values with an XPath-like query, returning at most 1 result/FakeXPath.
     * @param xml        the XML to extract text from.
     * @param fakeXPaths list of fakeXPaths to evaluate.
     * @see FakeXPath
     * @return a list with the same number of elements as fakeXPaths with the results of the XPaths matching the order,
     *         null as a xpath-result means no match.
     * @throws XMLStreamException if the xml was not valid or XML processing failed for other reasons.
     */
    public static List<String> evaluateFakeXPathsSingleResults(
            CharSequence xml, List<String> fakeXPaths) throws XMLStreamException {
        return evaluateFakeXPathsSingleResultsPremade(xml, parsePaths(fakeXPaths));
    }
    /**
     * Extraction of values with an XPath-like query, returning at most 1 result/FakeXPath.
     * @param xml        the XML to extract text from.
     * @param fakeXPaths list of fakeXPaths to evaluate.
     * @see FakeXPath
     * @return a list with the same number of elements as fakeXPaths with the results of the XPaths matching the order,
     *         null as a xpath-result means no match.
     * @throws XMLStreamException if the xml was not valid or XML processing failed for other reasons.
     */
    public static List<String> evaluateFakeXPathsSingleResultsPremade(
            CharSequence xml, List<FakeXPath> fakeXPaths) throws XMLStreamException {
        List<List<String>> nested = evaluateFakeXPathsPremade(xml, fakeXPaths, 1);
        List<String> single = new ArrayList<String>(nested.size());
        for (List<String> entry: nested) {
            single.add(entry.isEmpty() ? null : entry.get(0));
        }
        return single;
    }
    /**
     * Extraction of values with an XPath-like query.
     * @param xml        the XML to extract text from.
     * @param fakeXPaths list of fakeXPaths to evaluate.
     * @param maxResultsPerFakeXPath maximum numbers of results per XPath, -1 means no limit.
     * @see FakeXPath
     * @return a list with the same number of elements as fakeXPaths with the results of the XPaths matching the order.
     * @throws XMLStreamException if the xml was not valid or XML processing failed for other reasons.
     */
    public static List<List<String>> evaluateFakeXPaths(
            CharSequence xml, List<String> fakeXPaths, final int maxResultsPerFakeXPath) throws XMLStreamException {
        return evaluateFakeXPaths(xmlFactory.createXMLStreamReader(new CharSequenceReader(xml)),
                                  parsePaths(fakeXPaths), maxResultsPerFakeXPath);
    }
    private static List<FakeXPath> parsePaths(List<String> fakeXPaths) {
        List<FakeXPath> fxs = new ArrayList<FakeXPath>(fakeXPaths.size());
        for (String fakeXPathString: fakeXPaths) {
            fxs.add(new FakeXPath(fakeXPathString));
        }
        return fxs;
    }
    /**
     * Extraction of values with an XPath-like query.
     * @param xml        the XML to extract text from.
     * @param fakeXPaths list of fakeXPaths to evaluate.
     * @param maxResultsPerFakeXPath maximum numbers of results per XPath, -1 means no limit.
     * @return a list with the same number of elements as fakeXPaths with the results of the XPaths matching the order.
     * @throws XMLStreamException if the xml was not valid or XML processing failed for other reasons.
     */
    // Problem: foo matches foo/bar . Trailing /?
    public static List<List<String>> evaluateFakeXPathsPremade(
            CharSequence xml, final List<FakeXPath> fakeXPaths, final int maxResultsPerFakeXPath)
            throws XMLStreamException {
        return evaluateFakeXPaths(
                xmlFactory.createXMLStreamReader(new CharSequenceReader(xml)), fakeXPaths, maxResultsPerFakeXPath);
    }
    /**
     * Specialized extraction of values with an XPath-like query.
     * @param xml        the XML to extract text from.
     * @param fakeXPaths list of fakeXPaths to evaluate.
     * @param maxResultsPerFakeXPath maximum numbers of results per XPath, -1 means no limit.
     * @return a list with the same number of elements as fakeXPaths with the results of the XPaths matching the order.
     * @throws XMLStreamException if the xml was not valid or XML processing failed for other reasons.
     */
    // Problem: foo matches foo/bar . Trailing /?
    public static List<List<String>> evaluateFakeXPaths(
            XMLStreamReader xml, final List<FakeXPath> fakeXPaths, final int maxResultsPerFakeXPath)
            throws XMLStreamException {
        final List<List<String>> matches = new ArrayList<List<String>>(fakeXPaths.size());
        for (int i = 0 ; i < fakeXPaths.size() ; i++) {
            matches.add(new ArrayList<String>());
        }
        final AtomicInteger totalCollects = new AtomicInteger(0);

        iterateTags(xml, new Callback() {
            @Override
            public PROCESS_ACTION elementStart2(XMLStreamReader xml, List<String> tags, String current)
                    throws XMLStreamException {
                PROCESS_ACTION action = PROCESS_ACTION.no_action; // Default
                for (int i = 0 ; i < fakeXPaths.size() ; i++) {
                    List<String> localMatches = matches.get(i);
                    if (maxResultsPerFakeXPath != -1 && localMatches.size() >= maxResultsPerFakeXPath) {
                        continue;
                    }
                    FakeXPath fakeXPath = fakeXPaths.get(i);
                    if (fakeXPath.matches(xml, tags)) { // We have a match
                        localMatches.add(fakeXPath.getValue(xml));
                        if (totalCollects.incrementAndGet() == fakeXPaths.size()*maxResultsPerFakeXPath) {
                            return PROCESS_ACTION.requests_stop_success; // All full, so we stop at once
                        }
                        action = PROCESS_ACTION.called_next; // No early break as we might have more matches
                    }
                }
                return action;
            }
        });

        return matches;
    }

    /**
     * Skips to the next position in the XML matching the given fakeXPath.
     * If the fakeXPath cannot be matched, the xml stream will be depleted and false will be returned.
     * @param xml       the XML to iterate for the given fakeXPath.
     * @param fakeXPath a {@link FakeXPath}, as described in the class javaDoc.
     * @return true if the fakeXPath was matched, else false. If false, the xml will be depleted.
     * @throws XMLStreamException if the xml was not valid or XML processing failed for other reasons.
     */
    public static boolean jumpToNextFakeXPath(XMLStreamReader xml, final String fakeXPath)
            throws XMLStreamException {
        FakeXPath xpath = new FakeXPath(fakeXPath);
        AtomicBoolean matched = new AtomicBoolean(false);
        iterateTags(xml, new Callback() {
            @Override
            public PROCESS_ACTION elementStart2(XMLStreamReader xml, List<String> tags, String current) {
                if (xpath.matches(xml, tags)) { // We have a match
                    matched.set(true);
                    return PROCESS_ACTION.requests_stop_success;
                }
                return PROCESS_ACTION.no_action;
            }
        });

        return matched.get();
    }

    /**
     * Skips to the next position in the XML matching the given fakeXPath.
     * If the fakeXPath cannot be matched, the xml stream will be depleted and null will be returned.
     * @param xml       the XML to iterate for the given fakeXPath.
     * @param fakeXPath a {@link FakeXPath}, as described in the class javaDoc.
     * @return an XMLStreamReader positioned at the given fakeXPath or null if there were no match.
     * @throws XMLStreamException if the xml was not valid or XML processing failed for other reasons.
     */
    public static XMLStreamReader jumpToNextFakeXPath(String xml, final String fakeXPath) throws XMLStreamException {
        XMLStreamReader xmlReader = xmlFactory.createXMLStreamReader(new StringReader(xml));
        return jumpToNextFakeXPath(xmlReader, fakeXPath) ? xmlReader : null;
    }

    /**
     * Subset of XPath @{url https://www.w3schools.com/xml/xpath_syntax.asp}.
     * Parsing always start from the root of the document, so @{code foo} and {@code /foo} are equal.
     * For the same reason, {@code ..} is not supported.
     * {@code //} is supported, but not in combination with attributes on any element besides the last one.
     *
     * Predicate support:
     * {@code foo[@bar]}: The element foo with the attribute bar.
     * {@code foo[@bar='zoo']}: The element foo with the attribute bar with value zoo.
     *
     * Wildcard support:
     * {@code *}: Any element node.
     * {@code @*}: Any attribute node.
     * Not supported: {@code node()}.
     *
     * Trailing {@code /text()} is treated as element text and ignored.
     */
    // TODO: /foo/[@bar=zoo]@baz
    // TODO: /foo/[@bar=zoo]/*
    // TODO: foo/[@bar=zoo]/*
    // TODO: foo matches foo/bar . Trailing /?
    // https://www.w3schools.com/xml/xpath_syntax.asp
    static class FakeXPath {
        private final String xpathString;

        private final boolean locationIndependent;
        private final PathElement[] path;
        private final PathElement extraction;
        private final List<Boolean> matchTracker = new ArrayList<>();

        public FakeXPath(String fakeXPath) {
            if (fakeXPath.startsWith("//")) {
                locationIndependent = true;
                fakeXPath = fakeXPath.substring(2);
            } else {
                locationIndependent = false;
                if (fakeXPath.startsWith("./")) {
                    fakeXPath = fakeXPath.substring(2);
                } else if (fakeXPath.startsWith("/")) {
                    fakeXPath = fakeXPath.substring(1);
                }
            }
            if (fakeXPath.endsWith("/text()")) {
                fakeXPath = fakeXPath.substring(0, fakeXPath.length()-"/text()".length());
            }
            this.xpathString = fakeXPath;

            String[] potentialPath = fakeXPath.split("/");
            PathElement extractionCandidate = new PathElement(potentialPath[potentialPath.length-1]);
            int convertCount;
            if (extractionCandidate.isAttribute) {
                extraction = extractionCandidate;
                convertCount = potentialPath.length-1;
            } else {
                extraction = PathElement.ELEMENT_TEXT;
                convertCount = potentialPath.length;
            }
            path = new PathElement[convertCount];
            for (int i = 0 ; i < convertCount ; i++) {
                try {
                    path[i] = new PathElement(potentialPath[i]);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Unable to parse FakeXPath element '" + potentialPath[i] +
                                                       "' from full expression '" + fakeXPath + "'", e);
                }
            }

            // Check for unsupported XPaths
            if (locationIndependent) {
                for (int i = 0 ; i < path.length-1 ; i++) { // -1 as the last element supports predicates
                    if (path[i].hasPredicate) {
                        throw new UnsupportedOperationException(
                                "Currently only the last path element supports attributes. Got elements " +
                                Arrays.toString(path));
                    }
                }
            }
            // TODO: If locationIndependent, check for attribute-matcher and fail early
        }

        public boolean matches(XMLStreamReader xml, List<String> tags) {
            if (locationIndependent) {
                return matchesLocationIndependent(xml, tags);
            }
            // Adjust tracker to match tag-chain size
            while(matchTracker.size() > tags.size()) {
                matchTracker.remove(matchTracker.size()-1);
            }
            while(matchTracker.size() < tags.size()) {
                matchTracker.add(false);
            }
            if (path.length < tags.size()) {
                return false;
            }

            // Update tracker
            final int current = tags.size()-1;
            matchTracker.set(current, path[current].matches(xml, tags.get(current)));
            // Check for match
            if (path.length != tags.size()) {
                return false;
            }
            return !matchTracker.contains(Boolean.FALSE);
        }
        public boolean matchesLocationIndependent(XMLStreamReader xml, List<String> tags) {
            if (path.length < tags.size()) {
                int offset = tags.size() - path.length;
                return matchesLocationIndependent(xml, tags.subList(offset, tags.size()));
            }
            if (path.length != tags.size()) {
                return false;
            }
            for (int i = 0 ; i < path.length ; i++) {
                if (!path[i].matches(xml, tags.get(i))) {
                    return false;
                }
            }
            return true;
        }

        public String getValue(XMLStreamReader xml) throws XMLStreamException {
            return extraction.value(xml);
        }

        public String getFakeXPathString() {
            return xpathString;
        }

        public String toString() {
            return Strings.join(List.of(path), "/");
        }

        public static class PathElement {
            private final boolean isAttribute;
            private final String key;
            private final boolean wildcard;

            private final boolean hasPredicate;
            private final String predicateAttributeName;
            private final String predicateAttributeValue;

            public static final PathElement ELEMENT_TEXT = new PathElement("*");

            public PathElement(String element) {
                if (element.startsWith("@")) { // Is an attribute
                    // @bar
                    isAttribute = true;
                    hasPredicate = false;
                    predicateAttributeName = null;
                    predicateAttributeValue = null;
                    key = element.substring(1);
                    wildcard = "*".equals(key);
                    return;
                }

                if (element.contains("[")) { // Contains a predicate
                    // foo[@bar='zoo']
                    // foo[@bar]
                    isAttribute = false;
                    hasPredicate = true;
                    int start = element.indexOf("[") + 1;
                    int end = element.indexOf("]");
                    key = element.substring(0, start-1);
                    wildcard = "*".equals(key);

                    // @bar='zoo'
                    // @bar
                    String predicate = element.substring(start, end);
                    if (!predicate.startsWith("@")) {
                        throw new IllegalArgumentException("The predicate '" + predicate + "' must start with '@'");
                    }
                    int eqIndex = predicate.indexOf("=");
                    if (eqIndex > 0) {
                        // @bar='zoo'
                        predicateAttributeName = predicate.substring(1, eqIndex);
                        predicateAttributeValue = predicate.substring(eqIndex + 2, predicate.length() - 1);
                    } else {
                        // @bar
                        predicateAttributeName = predicate.substring(1);
                        predicateAttributeValue = null;
                    }
                    return;
                }

                // Plain element key
                // foo
                isAttribute = false;
                key = element;
                wildcard = "*".equals(key);
                hasPredicate = false;
                predicateAttributeName = null;
                predicateAttributeValue = null;
            }
            public boolean matches(XMLStreamReader xml, String elementName) { // Always at element_start
                if (isAttribute) {
                    return getAttribute(xml, key, null) != null;
                }
                if (!wildcard && !elementName.equals(key)) {
                    return false;
                }
                if (hasPredicate) {
                    String value = getAttribute(xml, predicateAttributeName, null);
                    if (value == null) {
                        return false;
                    }
                    if (predicateAttributeValue != null && !predicateAttributeValue.equals(value)) {
                        return false;
                    }
                }
                return true;
            }
            // Always advances, expects match
            public String value(XMLStreamReader xml) throws XMLStreamException {
                if (!isAttribute) {
                    return xml.getElementText();
                }

                String attributeValue = getAttribute(xml, key, null);
                if (attributeValue == null) {
                    throw new IllegalStateException(
                            "Tried extracting value for attribute '" + key + "' but got null");
                }
                xml.next(); // Hmm... Could we avoid this?
                return attributeValue;
            }

            @Override
            public String toString() {
                if (isAttribute) {
                    return "@" + key;
                }
                String val = key;
                if (hasPredicate) {
                    val += "[@" + predicateAttributeName;
                    if (predicateAttributeValue != null) {
                        val += "='" + predicateAttributeValue + "'";
                    }
                    val += "]";
                }
                return val;
            }
        }
    }

    /**
     * Skips everything until a start tag is reacted or the readers is depleted.
     * @param xml the stream to iterate over.
     * @return the name of the start tag or null if EOD.
     * @throws javax.xml.stream.XMLStreamException if there was an error
     * accessing the xml stream.
     */
    public static String jumpToNextTagStart(XMLStreamReader xml)
            throws XMLStreamException {
        if (xml.getEventType() == XMLStreamReader.START_ELEMENT) {
            xml.next(); // Skip if already located at a start
        }
        while (true) {
            if (xml.getEventType() == XMLStreamReader.START_ELEMENT) {
                return xml.getLocalName();
            }
            if (xml.getEventType() == XMLStreamReader.END_DOCUMENT) {
                return null;
            }
            xml.next();
        }
    }

    /**
     * Extracts the value from the attribute with the given name. This does not advance the xml stream.
     * @param xml stream positioned at a start tag.
     * @param attributeName the wanted attribute
     * @param defaultValue the value to return if the attributes is not present.
     * @return the attribute content og the default value.
     */
    public static String getAttribute(XMLStreamReader xml, String attributeName, String defaultValue) {
        for (int i = 0 ; i < xml.getAttributeCount() ; i++) {
            if (xml.getAttributeLocalName(i).equals(attributeName)) {
                return xml.getAttributeValue(i);
            }
        }
        return defaultValue;
    }

    /**
     * Iterates over the xml until a start tag with startTagName is reached.
     * @param xml          the stream to iterate over.
     * @param startTagName the name of the tag to locate.
     * @return true if the tag was found, else false.
     * @throws javax.xml.stream.XMLStreamException if there were an error
     * seeking the xml stream.
     */
    public static boolean findTagStart(XMLStreamReader xml, String startTagName) throws XMLStreamException {
        while (true)  {
            if (xml.getEventType() == XMLStreamReader.START_ELEMENT && startTagName.equals(xml.getLocalName())) {
                return true;
            }
            if (xml.getEventType() == XMLStreamReader.END_DOCUMENT) {
                return false;
            }
            try {
                xml.next();
            } catch (XMLStreamException e) {
                throw new XMLStreamException("Error seeking to start tag for element '" + startTagName + "'", e);
            }
        }
    }

    public static boolean findTagEnd(XMLStreamReader xml, String endTagName) throws XMLStreamException {
        while (true)  {
            if (xml.getEventType() == XMLStreamReader.END_ELEMENT && endTagName.equals(xml.getLocalName())) {
                return true;
            }
            if (xml.getEventType() == XMLStreamReader.END_DOCUMENT) {
                return false;
            }
            try {
                xml.next();
            } catch (XMLStreamException e) {
                throw new XMLStreamException("Error seeking to end tag for element '" + endTagName + "'", e);
            }
        }
    }

    /**
     * Iterates over elements in the stream until end element is encountered or end of document is reached.
     * For each element matching actionElement, callback is called.
     * @param xml        the stream to iterate.
     * @param endElement the stopping element.
     * @param actionElement callback is activated when encountering elements with this name.
     * @param callback   called for each encountered element.
     * @throws javax.xml.stream.XMLStreamException if the stream could not be iterated or an error occurred during
     * callback.
     */
    public static void iterateElements(XMLStreamReader xml, String endElement, String actionElement,
                                       XMLCallback callback) throws XMLStreamException {
        iterateElements(xml, endElement, actionElement, true, callback);
        while (true) {
            if (xml.getEventType() == XMLStreamReader.END_DOCUMENT ||
                (xml.getEventType() == XMLStreamReader.END_ELEMENT && xml.getLocalName().equals(endElement))) {
                break;
            }
            if (xml.getEventType() == XMLStreamReader.START_ELEMENT && xml.getLocalName().equals(actionElement)) {
                callback.execute(xml);
            }
            xml.next();
        }
    }

    /**
     * Iterates over elements in the stream until end element is encountered
     * or end of document is reached. For each element matching actionElement,
     * callback is called.
     *
     * @param xml           the stream to iterate.
     * @param endElement    the stopping element.
     * @param actionElement callback is activated when encountering elements with this name.
     * @param advanceOnHit  if true, the iterator always calls {@code xml.next()}. If false, next is only called if
     *                      no callback has been issued.
     * @param callback   called for each encountered element.
     * @throws javax.xml.stream.XMLStreamException if the stream could not
     * be iterated or an error occured during callback.
     */
    public static void iterateElements(XMLStreamReader xml, String endElement, String actionElement,
                                       boolean advanceOnHit, XMLCallback callback) throws XMLStreamException {
        while (true) {
            if (xml.getEventType() == XMLStreamReader.END_DOCUMENT ||
                (xml.getEventType() == XMLStreamReader.END_ELEMENT && xml.getLocalName().equals(endElement))) {
                break;
            }
            if (xml.getEventType() == XMLStreamReader.START_ELEMENT && xml.getLocalName().equals(actionElement)) {
                callback.execute(xml);
                if (advanceOnHit) {
                    xml.next();
                }
            } else {
                xml.next();
            }
        }
    }

    /**
     * Streaming parse + export of sub elements from XML in String serialized form.
     * Example use case: Extracting records from a large OAI-PMH document.
     * <p>
     * The serializer repairs namespaces, i.e. if a namespace is defined in the OAI-PMH outer element but used in the
     * embedded records, the serialized records will have the namespace defined and (hopefully) be valid XML.
     * <p>
     * Note: The given {@code xml} will NOT be automatically closed after use.
     * @param xml an InputStream containing XML.
     * @param subElement the element name to look for when locating sub element start, without namespace.
     * @param addDeclaration if true, an XML declaration will be added at the top of each serialized sub element.
     * @return a stream of serialized sub elements for the given {@code xml}.
     * @see #serializeSubElements(Reader, String, boolean)
     * @see #serializeSubElements(XMLStreamReader, String, boolean)
     */
    public static Stream<String> serializeSubElements(InputStream xml, String subElement, boolean addDeclaration) {
        try {
            return serializeSubElements(xmlFactory.createXMLStreamReader(xml), subElement, addDeclaration);
        } catch (XMLStreamException e) {
            throw new RuntimeException(
                    "XMLStreamException creating XMLStreamReader for xml with subElement '" + subElement + "'");
        }
    }

    /**
     * Streaming parse + export of sub elements from XML in String serialized form.
     * Example use case: Extracting records from a large OAI-PMH document.
     * <p>
     * The serializer repairs namespaces, i.e. if a namespace is defined in the OAI-PMH outer element but used in the
     * embedded records, the serialized records will have the namespace defined and (hopefully) be valid XML.
     * <p>
     * Note: The given {@code xml} will NOT be automatically closed after use.
     * @param xml a reader containing XML.
     * @param subElement the element name to look for when locating sub element start, without namespace.
     * @param addDeclaration if true, an XML declaration will be added at the top of each serialized sub element.
     * @return a stream of serialized sub elements for the given {@code xml}.
     * @see #serializeSubElements(InputStream, String, boolean)
     * @see #serializeSubElements(XMLStreamReader, String, boolean)
     */
    public static Stream<String> serializeSubElements(Reader xml, String subElement, boolean addDeclaration) {
        try {
            return serializeSubElements(xmlFactory.createXMLStreamReader(xml), subElement, addDeclaration);
        } catch (XMLStreamException e) {
            throw new RuntimeException(
                    "XMLStreamException creating XMLStreamReader for xml with subElement '" + subElement + "'");
        }
    }

    /**
     * Streaming parse + export of sub elements from XML in String serialized form.
     * Example use case: Extracting records from a large OAI-PMH document.
     * <p>
     * The serializer repairs namespaces, i.e. if a namespace is defined in the OAI-PMH outer element but used in the
     * embedded records, the serialized records will have the namespace defined and (hopefully) be valid XML.
     * <p>
     * The given {@code xml} will be fully iterated and closed after use.
     * @param xml an XML stream, typically positioned at the beginning.
     * @param subElement the element name to look for when locating sub element start, without namespace.
     * @param addDeclaration if true, an XML declaration will be added at the top of each serialized sub element.
     * @return a stream of serialized sub elements for the given {@code xml}.
     * @see #serializeSubElements(InputStream, String, boolean)
     * @see #serializeSubElements(Reader, String, boolean)
     */
    public static Stream<String> serializeSubElements(XMLStreamReader xml, String subElement, boolean addDeclaration) {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();

        // Seek to first subElement
        try {
            if (!XMLStepper.findTagStart(xml, subElement)) {
                return Stream.empty();
            }
        } catch (XMLStreamException e) {
            throw new RuntimeException("XMLStreamException seeking start element '" + subElement + "'", e);
        }

        Iterator<String> iterator = new Iterator<>() {
            boolean finished = false;
            String next = null;

            @Override
            public boolean hasNext() {
                if (finished) {
                    return false;
                }
                if (next != null) {
                    return true;
                }

                // Resolve next
                try {
                    if (!XMLStepper.findTagStart(xml, subElement)) {
                        finished = true;
                        xml.close();
                        return false;
                    }

                    os.reset();
                    XMLStreamWriter out = xmlOutFactory.createXMLStreamWriter(os);
                    if (addDeclaration) {
                        out.writeStartDocument("utf-8", "1.0");
                        out.writeCharacters("\n");
                    }
                    XMLStepper.pipeXML(xml, out, true, false);
                    out.writeEndDocument(); // Very rule obeying here. Should always be a no-op
                    out.flush();
                    next = os.toString(StandardCharsets.UTF_8);
                    return true;
                } catch (XMLStreamException e) {
                    finished = true;
                    throw new RuntimeException("XMLStreamException while producing element '" + subElement + "'", e);
                }
            }

            @Override
            public String next() {
                if (!hasNext()) {
                    throw new IllegalStateException("next() called when hasNext() == false");
                }
                String result = next;
                next = null;
                return result;
            }
        };
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
    }

    public static void skipSubTree(XMLStreamReader xml) throws XMLStreamException {
        iterateTags(xml, new Callback() {
            @Override
            public PROCESS_ACTION elementStart2(XMLStreamReader xml, List<String> tags, String current)
                    throws XMLStreamException {
                return PROCESS_ACTION.no_action; // Ignore everything until end of sub tree
            }
        });
    }

    public static void skipElement(XMLStreamReader xml) throws XMLStreamException {
        if (XMLStreamReader.START_ELEMENT != xml.getEventType()) {
            throw new IllegalStateException("The reader must be positioned at START_ELEMENT but was positioned at "
                                            + XMLUtil.eventID2String(xml.getEventType()));
        }
        xml.next();
        iterateTags(xml, new Callback() {
            @Override
            public PROCESS_ACTION elementStart2(XMLStreamReader xml, List<String> tags, String current)
                    throws XMLStreamException {
                return PROCESS_ACTION.no_action; // Ignore everything until end of sub tree
            }
        });
    }

    public static class Limiter {
        final private Map<Pattern, Integer> limits;
        final private boolean countPatterns;
        final private boolean onlyCheckElementPaths;
        final private boolean discardNonMatched;

        public Limiter(Map<Pattern, Integer> limits, boolean countPatterns, boolean onlyCheckElementPaths,
                       boolean discardNonMatched) {
            this.limits = limits;
            this.countPatterns = countPatterns;
            this.onlyCheckElementPaths = onlyCheckElementPaths;
            this.discardNonMatched = discardNonMatched;
        }

        /**
         * Apply the specified limitations on the XML and return the result.
         * @param xml an XML block that should be reduced.
         * @return the processed XML.
         * @throws javax.xml.stream.XMLStreamException if there was a problem reading or writing XML.
         */
        public String limit(String xml) throws XMLStreamException {
            return limitXML(xml, limits, countPatterns, onlyCheckElementPaths, discardNonMatched);
        }
        /**
         * Apply the specified limitations on the in XML, writing the result to out.
         * @param in     XML stream positioned at the point from which reduction should occur (normally the start).
         * @param out    the reduced XML.
         * @throws javax.xml.stream.XMLStreamException if there was a problem reading (in) or writing (out) XML.
         */
        public void limit(XMLStreamReader in, XMLStreamWriter out) throws XMLStreamException {
            limitXML(in, out, limits, countPatterns, onlyCheckElementPaths, discardNonMatched);
        }

        @Override
        public String toString() {
            return "XMLStepper.Limiter(#limits=" + limits.size() + ", countPatterns=" + countPatterns
                   + ", onlyCheckElementPaths=" + onlyCheckElementPaths + ", discardNonMatched=" + discardNonMatched
                   + ")";
        }
    }

    /**
     * Packaging of {@link #limitXML(String, java.util.Map, boolean, boolean, boolean)} with pre-defined setup.
     * @param limits patterns and max occurrences for entries. The limits are processed in entrySet order.
     *               If max occurrence is -1 there is no limit for the given pattern.
     * @param countPatterns if true, the limit applies to matched patterns. If false, the limit if for each regexp.
     *                      If the limit is {code ".*", 10}, only 10 elements in total is kept.
     * @param onlyCheckElementPaths if true, only element names are matched, not attributes.
     *                              Setting this to true speeds up processing.
     * @param discardNonMatched if true, paths that are not matched by any limit are discarded.
     * @return an XML processor ready for limiting XML with the given constraints.
     */
    public static Limiter createLimiter(final Map<Pattern, Integer> limits, final boolean countPatterns,
                                        final boolean onlyCheckElementPaths, final boolean discardNonMatched) {
        return new Limiter(limits, countPatterns, onlyCheckElementPaths, discardNonMatched);
    }

    /**
     * Convenience wrapper for {@link #limitXML(javax.xml.stream.XMLStreamReader, javax.xml.stream.XMLStreamWriter, java.util.Map, boolean, boolean, boolean)}
     * that takes care of constructing and deconstructing XML streams.
     * @param xml an XML block that should be reduced.
     * @param limits patterns and max occurrences for entries. The limits are processed in entrySet order.
     *               If max occurrence is -1 there is no limit for the given pattern.
     * @param countPatterns if true, the limit applies to matched patterns. If false, the limit if for each regexp.
     *                      If the limit is {code ".*", 10}, only 10 elements in total is kept.
     * @param onlyCheckElementPaths if true, only element names are matched, not attributes.
     *                              Setting this to true speeds up processing.
     * @param discardNonMatched if true, paths that are not matched by any limit are discarded.
     * @return the processed XML.
     * @throws javax.xml.stream.XMLStreamException if there was a problem reading or writing XML.
     */
    public static String limitXML(final String xml, final Map<Pattern, Integer> limits,
                                  final boolean countPatterns, final boolean onlyCheckElementPaths,
                                  final boolean discardNonMatched) throws XMLStreamException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XMLStreamWriter out = xmlOutFactory.createXMLStreamWriter(os);
        XMLStreamReader in = xmlFactory.createXMLStreamReader(new StringReader(xml));
        limitXML(in, out, limits, countPatterns, onlyCheckElementPaths, discardNonMatched);
        return os.toString(StandardCharsets.UTF_8);
    }

    /**
     * Iterates the given input, counting occurrences of limit-matches and skipping matching elements when the limits
     * are reached.
     *
     * Every tag and every attribute (optional) is matched against the limits. Tags are represented as
     * {@code /rootelement/subelement}, attributes as {@code /rootelement/subelement#attributename=value}.
     * Namespaces are not part of the representation.
     *
     * Sample: in={@code &lt;foo&gt;&lt;bar zoo="true"&gt;&lt;/bar&gt;&lt;bar zoo="true"&gt;&lt;/bar&gt;&lt;bar zoo="false"&gt;&lt;/bar&gt;&lt;baz /&gt;&lt;/foo&gt;}
     * Limits {@code "/foo/bar", 1} -&gt; {@code &lt;foo&gt;&lt;bar zoo="true"&gt;&lt;/bar&gt;&lt;baz /&gt;&lt;/foo&gt;}
     * Limits {@code "bar", 1} -&gt; {@code &lt;foo&gt;&lt;bar zoo="true"&gt;&lt;/bar&gt;&lt;baz /&gt;&lt;/foo&gt;}
     * Limits {@code "/foo/bar", 2} -&gt; {@code &lt;foo&gt;&lt;bar zoo="true"&gt;&lt;/bar&gt;&lt;bar zoo="true"&gt;&lt;/bar&gt;&lt;baz /&gt;&lt;/foo&gt;}
     * Limits {@code "/foo/bar", 0} -&gt; {@code &lt;foo&gt;&lt;baz&gt;&lt;/baz&gt;&lt;/foo&gt;}
     * Limits {@code "/foo/bar#zoo=true", 1} -&gt; {@code &lt;foo&gt;&lt;bar zoo="true"&gt;&lt;/bar&gt;&lt;bar zoo="false"&gt;&lt;/bar&gt;&lt;baz /&gt;&lt;/foo&gt;}
     *
     * Example: limits={@code ["/foo$", -1], ["/foo/bar", 1]}, countPatterns=false, onlyCheckElementPaths=true,
                discardNonMatched=true} -&gt; {@code "&lt;foo&gt;&lt;bar zoo=\"true\" /&gt;&lt;/foo&gt;"}
     * Example: limits={@code ["/foo$", -1], ["/foo/bar", 1]}, countPatterns=false, onlyCheckElementPaths=true,
                discardNonMatched=false} -&gt; {@code "&lt;foo&gt;&lt;bar zoo=\"true\" /&gt;&lt;baz /&gt;&lt;/foo&gt;"}
     * @param in     XML stream positioned at the point from which reduction should occur (normally the start).
     * @param out    the reduced XML.
     * @param limits patterns and max occurrences for entries. The limits are processed in entrySet order.
     *               If max occurrence is -1 there is no limit for the given pattern.
     * @param countPatterns if true, the limit applies to matched patterns. If false, the limit if for each regexp.
     *                      If the limit is {code ".*", 10}, only 10 elements in total is kept.
     * @param onlyCheckElementPaths if true, only element names are matched, not attributes.
     *                              Setting this to true speeds up processing.
     * @param discardNonMatched if true, paths that are not matched by any limit are discarded.
     * @throws javax.xml.stream.XMLStreamException if there was a problem reading (in) or writing (out) XML.
     */
    public static void limitXML(final XMLStreamReader in, XMLStreamWriter out, final Map<Pattern, Integer> limits,
                                final boolean countPatterns, final boolean onlyCheckElementPaths,
                                final boolean discardNonMatched) throws XMLStreamException {
        final Map<Object, Integer> counters = new HashMap<Object, Integer>();
        pipeXML(in, out, false, false, new Callback() {
            @Override
            public PROCESS_ACTION elementStart2(
                    XMLStreamReader xml, List<String> tags, String current) throws XMLStreamException {
                Set<RESULT> result = exceeded(counters, xml, tags);
                if (result.contains(RESULT.exceeded) || (discardNonMatched && !result.contains(RESULT.match))) {
                    skipElement(xml);
                    return PROCESS_ACTION.called_next;
                }
                return PROCESS_ACTION.no_action;
            }

            private Set<RESULT> exceeded(Map<Object, Integer> counters, XMLStreamReader xml, List<String> tags) {
                Set<RESULT> result = EnumSet.noneOf(RESULT.class);
                for (Map.Entry<Pattern, Integer> limit: limits.entrySet()) {
                    final Pattern pattern = limit.getKey();
                    final int max = limit.getValue();

                    final String element = "/" + Strings.join(tags, "/");
                    exceeded(result, counters, pattern, max, element, countPatterns);
                    if (result.contains(RESULT.exceeded)) {
                        return result;
                    }
                    if (!onlyCheckElementPaths) {
                        for (int i = 0 ; i < xml.getAttributeCount() ; i++) {
                            final String merged =
                                    element + "#" + xml.getAttributeLocalName(i) + "=" + xml.getAttributeValue(i);
                            exceeded(result, counters, pattern, max, merged, countPatterns);
                            if (result.contains(RESULT.exceeded)) {
                                return result;
                            }
                        }
                    }
                }
                return result;
            }

            private void exceeded(Set<RESULT> result, Map<Object, Integer> counters, Pattern pattern, int max,
                                  String path, boolean countPatterns) {
                Matcher elementMatch = pattern.matcher(path);
                if (elementMatch.matches()) {
                    result.add(RESULT.match);
                    Integer count = counters.get(countPatterns ? elementMatch.group() : pattern);
                    if (count == null) {
                        count = 0;
                    }
                    counters.put(countPatterns ? elementMatch.group() : pattern, ++count);
                    if (max != -1 && count > max) {
                        result.add(RESULT.exceeded);
                    }
                }
            }

        });
    }
    enum RESULT {match, exceeded}

    public abstract static class ContentReplaceCallback extends Callback {
        private XMLStreamWriter out = null;


        protected void setOut(XMLStreamWriter out) {
            this.out = out;
        }

        @Override
        public boolean elementStart(XMLStreamReader in, List<String> tags, String current) throws XMLStreamException {
            if (out == null) {
                throw new IllegalStateException("The XMLStreamWriter has not been set");
            }
            if (!match(in, tags, current)) {
                return false;
            }
            copyStartElement(in, out);
            final String original = in.getElementText();
            final String replaced = replace(tags, current, original);
            out.writeCharacters(replaced);
            out.writeEndElement();
            findTagEnd(in, current);
            return true;
        }

        /**
         * @param tags the tags for the current branch of the XML-tree.
         * @param current the current element.
         * @param originalText the text of the current element.
         * @return the text to be used instead of originalText.
         */
        protected abstract String replace(List<String> tags, String current, String originalText);

        /**
         * @param xml the xml stream.
         * @param tags the tags for the current branch of the XML-tree.
         * @param current the current element.
         * @return true if the text content of the element should be replaced.
         */
        protected abstract boolean match(XMLStreamReader xml, List<String> tags, String current);
    }

    /**
     * Override either
     */
    public abstract static class Callback {
        public enum PROCESS_ACTION {
            /** The processing did not change the state of the XMLStreamReader (did not call .next() et al) */
            no_action,
            /** The processing called .next() or similar XMLStreamReader-advancing code at least once */
            called_next,
            /** The processing has finished all processing on the current stream and requests that further processing
             * is halted. All is ok.
             */
            requests_stop_success,
            /** The processing has finished all processing on the current stream and requests that further processing
             * is halted. The processing was not a success.
             */
            requests_stop_fail
        };
        /**
         * Called for each encountered START_ELEMENT in the part of the xml that is within scope. If the implementation
         * calls {@code xml.next()} or otherwise advances the position in the stream, it must ensure that the list of
         * tags is consistent with the position in the DOM.
         *
         * @param xml        the Stream.
         * @param tags       the start tags encountered in the current sub tree.
         * @param current    the local name of the current tag.
         * @return true if the implementation called {@code xml.next()} one or more times, else false.
         * @deprecated use {@link #elementStart2(XMLStreamReader, List, String)} instead.
         * @throws XMLStreamException if processing failed.
         */
        @Deprecated
        public boolean elementStart(
                XMLStreamReader xml, List<String> tags, String current) throws XMLStreamException {
            throw new UnsupportedOperationException("Either elementStart or elementStart2 must be overridden");
        }

        /**
         * Called for each encountered START_ELEMENT in the part of the xml that is within scope. If the implementation
         * calls {@code xml.next()} or otherwise advances the position in the stream, it must ensure that the list of
         * tags is consistent with the position in the DOM.
         *
         * @param xml        the Stream.
         * @param tags       the start tags encountered in the current sub tree.
         * @param current    the local name of the current tag.
         * @return the action taken by the implementation that affects overall processing of the xml stream.
         * @throws XMLStreamException if processing failed.
         */
        @SuppressWarnings("deprecation")
        public PROCESS_ACTION elementStart2(
                XMLStreamReader xml, List<String> tags, String current) throws XMLStreamException {
            return elementStart(xml, tags, current) ? PROCESS_ACTION.called_next : PROCESS_ACTION.no_action;
        }

        /**
         * Called for each encountered ELEMENT_END in the part of the XML that is within scope.
         * @param element the name of the element that ends.
         */
        @SuppressWarnings("UnusedParameters")
        public void elementEnd(String element) { }

        /**
         * Called when the last END_ELEMENT is encountered or PROCESS_ACTION.requests_stop_success has been returned.
         */
        public void end() { }

    }

    public abstract static class XMLCallback {
        public abstract void execute(XMLStreamReader xml) throws XMLStreamException;
        public void close() { } // Called when iteration has finished
    }
}
