/* $Id$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.kb.util.xml;

import dk.kb.util.reader.CharSequenceReader;
import dk.kb.util.reader.CircularCharBuffer;
import dk.kb.util.reader.ReplaceReader;
import dk.kb.util.string.Strings;

import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Java 1.5 compatible Reader which removes namespace declaration from XML.
 * If you are looking for namespace agnostic XSLT transformations use the
 * static methods supplied on the on the {@link XSLT} class, eg.
 * {@link XSLT#transform(java.net.URL, String, boolean)}.
 *
 * The reader looks for the pattern "&lt;.*&gt;".
 * For each match, all elements matching "xmlns:?[a-z]*=\".*\"" are removed.
 * If "&lt;![[CDATA.*]]&gt;" is encountered, it is copied verbatim.
 * if "&lt;!--.*--&gt;" is encountered, it is copied verbatim.
 *
 * This reader reads ahead, so the parent Reader is not guaranteed to be
 * positioned at any deterministic position during processing.
 *
 * The reader is not thread-safe.
 */
public class NamespaceRemover extends ReplaceReader {
//    private static Log log = LogFactory.getLog(NamespaceRemover.class);

    private enum Mode {PLAIN, CDATA, COMMENT}

    private Mode mode = Mode.PLAIN;
    private CircularCharBuffer inBuf = new CircularCharBuffer(100, Integer.MAX_VALUE);
    private CircularCharBuffer outBuf = new CircularCharBuffer(100, Integer.MAX_VALUE);

    // <foo xmlns:bar="sdjslfjs">
    private final Matcher declarationMatcher =
            Pattern.compile("xmlns(\\:[a-zA-Z_\\.\\-0-9]+)? *\\= *\"[^\"]*\"").matcher("");

    // <foo xmlns="sdjslfjs">
    private final Matcher defaultDeclarationMatcher =
            Pattern.compile("xmlns *\\= *\"[^\"]*\"").matcher("");

    // Should be http://www.w3.org/TR/REC-xml/#NT-Name but we cheat
    private final Matcher prefixMatcher = Pattern.compile(
            "[a-zA-Z_\\.\\-0-9]+\\:([a-zA-Z_\\.\\-0-9]+)").matcher("");

    public NamespaceRemover(Reader in) {
        super(in);
    }

    public String transform(CharSequence s) {
        CircularCharBuffer buf = new CircularCharBuffer(s.length(), Integer.MAX_VALUE);
        removeNamespace(s, buf);
        return buf.toString();
    }

    @Override
    public String transform(String s) {
        return transform((CharSequence) s);
    }

    @Override
    public char[] transformToChars(char c) {
        return new char[]{c};
    }

    @Override
    public char[] transformToChars(char[] chars) {
        CircularCharBuffer buf = new CircularCharBuffer(chars.length, Integer.MAX_VALUE);
        removeNamespace(Strings.asCharSequence(chars), buf);
        return buf.takeAll();
    }

    @Override
    public char[] transformToCharsAllowInplace(char[] chars) {
        return transformToChars(chars);
    }

    @Override
    public NamespaceRemover setSource(Reader in) {
        mode = Mode.PLAIN;
        inBuf.clear();
        outBuf.clear();
        this.in = in;
        return this;
    }

    @Override
    public NamespaceRemover setSource(CircularCharBuffer in) {
        mode = Mode.PLAIN;
        inBuf.clear();
        outBuf.clear();
        this.in = new CharSequenceReader(in);
        return this;
    }

    @Override
    public int read() throws IOException {
        ensureLength(1);
        return outBuf.read();
    }

    @Override
    public int read(char cbuf[], int off, int len) throws IOException {
        ensureLength(len);
        return outBuf.read(cbuf, off, len);
    }

    @Override
    public int read(CircularCharBuffer cbuf, int len) throws IOException {
        ensureLength(len);
        return outBuf.read(cbuf, len);
    }

    @Override
    public boolean ready() throws IOException {
        return !outBuf.isEmpty() || in.ready();
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public int read(CharBuffer target) throws IOException {
        ensureLength(100); // No lengthy reflection on this number
        return outBuf.read(target);
    }

    @Override
    public void mark(int readAheadLimit) throws IOException {
        throw new UnsupportedOperationException("No marking in NamespaceRemover");
    }

    // Quite ineffective...
    @Override
    public long skip(long n) throws IOException {
        long counter = 0;
        for (int i = 0; i < n; i++) {
            if (read() == -1) {
                break;
            }
            counter++;
        }
        return counter;
    }

    @Override
    public void reset() throws IOException {
        outBuf.clear();
        in.reset();
    }

    @Override
    public void close() throws IOException {
        if (in != null) {
            in.close();
        }
        outBuf.clear();
    }

    @SuppressWarnings({"CloneDoesntCallSuperClone", "CloneDoesntDeclareCloneNotSupportedException"})
    @Override
    public Object clone() {
        return new NamespaceRemover(null);
    }

    private static final String CDATA_START = "<![CDATA[";
    private static final String CDATA_END = "]]>";
    private static final String COMMENT_START = "<!--";
    private static final String COMMENT_END = "-->";
    private static final String TAG_START = "<";
    private static final String TAG_END = ">";
    private static final int LONGEST = CDATA_START.length();

    /**
     * Attempts to ensure that there is at least length characters in the out
     * buffer. This might involve reading far ahead in the in buffer.
     *
     * @param length the number of characters that should ideally be in the out buffer after processing.
     * @throws IOException if an I/O error occured in the parent Reader.
     */
    private void ensureLength(int length) throws IOException {
        while (outBuf.size() < length) {
            ensureInLength(LONGEST);
            if (inBuf.isEmpty()) {
                return;
            }
            switch (mode) {
                case PLAIN: {
                    // CDATA
                    if (inStartsWith(CDATA_START)) {
                        mode = Mode.CDATA;
                        for (int i = 0; i < CDATA_START.length(); i++) {
                            outBuf.put((char) inBuf.read());
                        }
                        continue;
                    }
                    // Comment
                    if (inStartsWith(COMMENT_START)) {
                        mode = Mode.COMMENT;
                        for (int i = 0; i < COMMENT_START.length(); i++) {
                            outBuf.put((char) inBuf.read());
                        }
                        continue;
                    }
                    // Tag
                    if (inStartsWith(TAG_START)) {
                        handleTag();
                        continue;
                    }
                    // skip one char ahead
                    outBuf.put((char) inBuf.read());
                    break;
                }
                case CDATA: { // Finish CDATA: Look for "]]>"
                    if (inStartsWith(CDATA_END)) {
                        mode = Mode.PLAIN;
                        for (int i = 0; i < CDATA_END.length(); i++) {
                            outBuf.put((char) inBuf.read());
                        }
                        continue;
                    }
                    outBuf.put((char) inBuf.read());
                    break;
                }
                case COMMENT: { // Finish Comment: Look for "-->"
                    if (inStartsWith(COMMENT_END)) {
                        mode = Mode.PLAIN;
                        for (int i = 0; i < COMMENT_END.length(); i++) {
                            outBuf.put((char) inBuf.read());
                        }
                        continue;
                    }
                    outBuf.put((char) inBuf.read());
                    break;
                }
                default:
                    throw new IllegalStateException(String.format(Locale.ROOT, "Mode %s is unknown", mode));
            }
        }
    }

    // Guarantees that at least one character is copied from inBuf to outBuf
    private void handleTag() throws IOException {
        ensureInLength(2);
        if (inBuf.length() < 2 || inBuf.charAt(1) == '!' || !ensureEnd(TAG_END)) {
            outBuf.put((char) inBuf.read()); // No tag-end found
            return;
        }
        // We now have <baz:foo xmlns="..." xmlns:bar="..." zoo="...">
        //          or </baz:foo>
        // Extract the tag
        char[] cbuf = new char[inBuf.indexOf(TAG_END) + TAG_END.length()];
        inBuf.read(cbuf, 0, cbuf.length);
        removeNamespace(new String(cbuf), outBuf);
    }

    protected void removeNamespace(CharSequence tag, CircularCharBuffer out) {
        tag = declarationMatcher.reset(tag).replaceAll("");
        tag = defaultDeclarationMatcher.reset(tag).replaceAll("");
        int first = Strings.indexOf('"', tag);
        int next = Strings.indexOf('"', first + 1, tag);
        if (first != -1 && next > first) {
            out.put(prefixMatcher.reset(tag.subSequence(0, first)).replaceAll("$1"));
            out.put(tag.subSequence(first, next + 1));
            removeNamespace(tag.subSequence(next + 1, tag.length()), out);
        } else {
            out.put(prefixMatcher.reset(tag).replaceAll("$1"));
        }
    }


    /**
     * Tries to fill the end buffer until it contains endStr. The method reads
     * through
     *
     * @param endStr the String that in should contain.
     * @return true if in contains endStr after an attempt has been made.
     * @throws java.io.IOException if an I/O error occured in parent.
     */
    private boolean ensureEnd(String endStr) throws IOException {
        int pos = 0;
        do {
            if (inBuf.indexOf(endStr, pos) != -1) {
                return true;
            }
            pos = inBuf.size();
        } while (ensureInLength(inBuf.size() * 2 + endStr.length()) != -1);
        return false;
    }

    /**
     * Looks in the in buffer and returns true if the in buffer starts with the
     * given match.
     *
     * @param match the String to search for.
     * @return true if the in buffer starts with the given match.
     */
    private boolean inStartsWith(String match) {
        if (inBuf.size() < match.length()) {
            return false;
        }
        for (int i = 0; i < match.length(); i++) {
            if (match.charAt(i) != inBuf.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Attempts to ensure that there is at least length characters in the in
     * buffer.
     *
     * @param length the number of characters that should ideally be in the
     *               in buffer after processing.
     * @return the number of chars read or -1 if parent has reached EOF.
     * @throws IOException if an I/O error occured in the parent Reader.
     */
    private int ensureInLength(int length) throws IOException {
        int count = 0;
        while (inBuf.size() < length) {
            int next = in.read();
            if (next == -1) {
                return count == 0 ? -1 : count;
            }
            inBuf.put((char) next);
            count++;
        }
        return count;
    }

    @Override
    public String toString() {
        return "NamespaceRemover(mode=" + mode + ", ..., ..., inBuf=" + inBuf
               + ", declarationMatcher=" + declarationMatcher + ", ..., prefixMatcher=" + prefixMatcher + ")";
    }
}
