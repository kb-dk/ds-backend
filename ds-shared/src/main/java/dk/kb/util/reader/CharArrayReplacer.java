/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The SB Util Library.
 * Copyright (C) 2005-2007  The State and University Library of Denmark
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
package dk.kb.util.reader;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * A highly speed-optimized single char to char array replacer.
 * The implementation maintains an array of all possible char values (65536)
 * mapped to their replacements, thereby making lookup of a single char O(1).
 *
 * This implementation is semi-thread safe. All methods except
 * {@link #setSource(java.io.Reader)} and {@link #setSource(CircularCharBuffer)}
 * can be called safely from different threads.
 *
 * @see CharReplacer
 */
public class CharArrayReplacer extends ReplaceReader {
    private char[][] rules;


    // ThreadLocal removed as they are prone to causing subtle memory overheads with
    // multi-threaded web applications
    /*private ThreadLocal bufferPool = new ThreadLocal() {
        @Override
        protected synchronized Object initialValue() {
            return new CircularCharBuffer(10, Integer.MAX_VALUE);
        }
    };*/

    /**
     * Used when the source is set to hold output chars that queues when the
     * destination of a replacement is more than 1 char.
     */
    private CircularCharBuffer outBuffer =
            new CircularCharBuffer(10, Integer.MAX_VALUE);

    /**
     * Create a new replacer based on a map with rules, consisting of target
     * chars and replacement chars.
     *
     * If a rule contains a target or a replacement that isn't exactly 1
     * char long, an exception is thrown.
     *
     * @param in    the character stream in which to replace substrings
     * @param rules the rules used for replacing chars.
     * @throws IllegalArgumentException if one or more of the reules are
     *                                  illegal for this {@link TextTransformer}.
     */
    public CharArrayReplacer(Reader in, Map<String, String> rules) {
        super(in);

        this.rules = new char[Character.MAX_VALUE][];
        for (char c = 0; c < Character.MAX_VALUE; c++) {
            this.rules[c] = new char[]{c};
        }
        for (Map.Entry<String, String> entry : rules.entrySet()) {
            char[] target = entry.getKey().toCharArray();
            char[] destination = entry.getValue().toCharArray();
            if (target.length != 1) {
                throw new IllegalArgumentException(String.format(
                        Locale.ROOT, "The rule '" + entry.getKey() + "' => " + entry.getValue()
                                     + "' was not single char to char array"));
            }
            this.rules[target[0]] = destination;
        }
    }

    /**
     * Create a new replacer with an empty input stream set based on a map
     * of target chars and replacement chars.
     *
     * You should set the input character stream of the new reader by
     * calling {@link CharArrayReplacer#setSource(java.io.Reader)}.
     *
     * If a rule contains a target or a replacement that isn't exactly 1
     * char long, an exception is thrown.
     *
     * @param rules the rules used for replacing chars.
     * @throws IllegalArgumentException if one or more of the reules are
     *                                  illegal for this {@link TextTransformer}.
     */
    public CharArrayReplacer(Map<String, String> rules) {
        this(new StringReader(""), rules);
    }

    private CharArrayReplacer(char[][] rules) {
        super(null);
        this.rules = rules;
    }

    /**
     * A clone of the CharReplacer will share the rules of the replacer, but
     * will otherwise be independent. A clone will not have a source defined.
     * Creating a clone is very cheap with regard to memory and processing time.
     *
     * @return a clone of this ReplaceReader.
     */
    @SuppressWarnings({"CloneDoesntCallSuperClone", "CloneDoesntDeclareCloneNotSupportedException"})
    @Override
    public Object clone() {
        return new CharArrayReplacer(rules);
    }

    /* TextTransformer interface implementations */

    @Override
    public char[] transformToChars(char c) {
        return rules[c];
    }

    /**
     * Replaces the characters in the given array.
     *
     * This implementation uses {@link ThreadLocal} in order to allow for
     * concurrent usage. The downside is degraded performance if it is called
     * with a new thread every time. Fortunately this is not a very common
     * scenario.
     *
     * @param chars the characters to replace.
     * @return the result of the replacing.
     */
    @Override
    public char[] transformToChars(char[] chars) {
        CircularCharBuffer out = new CircularCharBuffer(chars.length, Integer.MAX_VALUE);
        out.clear();
        for (char c : chars) {
            out.put(rules[c]);
        }
        return out.takeAll();
    }

    @Override
    public char[] transformToCharsAllowInplace(char[] chars) {
        return transformToChars(chars);
    }

    @Override
    public String transform(String s) {
        StringWriter out = new StringWriter(s.length() * 4);
        for (int i = 0; i < s.length(); i++) {
            for (char c : rules[s.charAt(i)]) {
                out.append(c);
            }
        }
        return out.toString();
    }

    /* Stream oriented implementations */

    @Override
    public ReplaceReader setSource(Reader reader) {
        this.in = reader;
        sourceBuffer = null;
        return this;
    }

    @Override
    public ReplaceReader setSource(CircularCharBuffer charBuffer) {
        this.sourceBuffer = charBuffer;
        this.in = null;
        return this;
    }

    /**
     * Reads the next processed char. Note that this is synchronized and that
     * threads competing for access share the output stream with
     * {@link #read(CircularCharBuffer, int)} and
     * {@link #read(char[], int, int)}.
     *
     * @return the next char or -1 if there are no more chars available.
     * @throws java.io.IOException if an I/O error occured.
     */
    @Override
    public synchronized int read() throws IOException {
        fillOutBuffer(1);
        try {
            return outBuffer.take();
        } catch (NoSuchElementException e) {
            return -1;
        }
    }

    private void fillOutBuffer(int min) throws IOException {
        try {
            if (in != null) {
                int codePoint;
                while (outBuffer.size() < min && (codePoint = in.read()) != -1) {
                    outBuffer.put(rules[codePoint]);
                }
                return;
            } else if (sourceBuffer != null) {
                while (outBuffer.size() < min) {
                    outBuffer.put(rules[sourceBuffer.take()]);
                }
                return;
            }
            throw new IllegalStateException(NO_SOURCE);
        } catch (NoSuchElementException e) {
            // This means the in or buffer is empty. As the number of
            // resulting chars can be determined from the outBuffer, we do
            // not need to do more about this.
        }
    }

    /**
     * Fills transformed chars in the buffer. Note that this is synchronized and
     * that threads competing for access share the output stream with
     * {@link #read()} and {@link #read(char[], int, int)}.
     *
     * @param cbuf   the buffer to assign shars to.
     * @param length the maximum number of chars to put in the buffer.
     * @return the number of chars filled or -1 if there are no more chars.
     * @throws java.io.IOException if an I/O error occured.
     */
    @Override
    public int read(CircularCharBuffer cbuf, int length) throws IOException {
        fillOutBuffer(length);
        return outBuffer.read(cbuf, length);
    }

    @Override
    public int read(char[] cbuf, int off, int length) throws IOException {
        fillOutBuffer(length);
        return outBuffer.read(cbuf, off, length);
    }

    @Override
    public String toString() {
        return "CharArrayReplacer(...)";
    }
}
