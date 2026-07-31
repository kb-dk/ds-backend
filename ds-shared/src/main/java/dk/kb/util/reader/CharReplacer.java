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
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * A highly speed-optimized single char to single char replacer.
 * The implementation maintains an array of all possible char values (65536)
 * mapped to their replacements, thereby making lookup of a single char O(1).
 *
 * This implementation is semi-thread safe. All methods except
 * {@link #setSource(java.io.Reader)} and {@link #setSource(CircularCharBuffer)}
 * can be called safely from different threads.
 *
 * @see CharArrayReplacer
 */
public class CharReplacer extends ReplaceReader {
    private char[] rules;

    /**
     * Create a new CharReplacer based on a map with rules, consisting of
     * target chars and replacement chars.
     * If a rule contains a target or a replacement that isn't exactly 1
     * char long, an exception is thrown.
     *
     * @param in    the character stream in which to replace substrings
     * @param rules the rules used for replacing chars
     * @throws IllegalArgumentException if one or more of the reules are illegal for this {@link TextTransformer}
     */
    public CharReplacer(Reader in, Map<String, String> rules) {
        super(in);

        this.rules = new char[Character.MAX_VALUE];
        for (char c = 0; c < Character.MAX_VALUE; c++) {
            this.rules[c] = c;
        }
        for (Map.Entry<String, String> entry : rules.entrySet()) {
            char[] target = entry.getKey().toCharArray();
            char[] destination = entry.getValue().toCharArray();
            if (target.length != 1 || destination.length != 1) {
                throw new IllegalArgumentException(String.format(Locale.ROOT,
                        "The rule '" + entry.getKey() + "' => '"
                        + entry.getValue() + "' was not single char to single char. The input had length "
                        + entry.getKey().length()));
            }
            this.rules[target[0]] = destination[0];
        }
    }

    /**
     * Create a new CharReplacer with an empty input character stream set,
     * based on a map with rules consisting of target chars and replacement
     * chars.
     *
     * Before calling any other methods on this reader you must call
     * {@link CharReplacer#setSource(java.io.Reader)}.
     *
     * If a rule contains a target or a replacement that isn't exactly 1
     * char long, an exception is thrown.
     *
     * @param rules the rules used for replacing chars
     * @throws IllegalArgumentException if one or more of the reules are
     *                                  illegal for this {@link TextTransformer}
     */
    public CharReplacer(Map<String, String> rules) {
        this(new StringReader(""), rules);
    }

    private CharReplacer(char[] rules) {
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
        return new CharReplacer(rules);
    }

    /* TextTransformer interface implementations */

    public char transform(char c) {
        return rules[c];
    }

    @Override
    public char[] transformToChars(char c) {
        return new char[]{rules[c]};
    }

    @Override
    public char[] transformToChars(char[] chars) {
        char[] output = new char[chars.length];
        for (int i = 0; i < chars.length; i++) {
            output[i] = rules[chars[i]];
        }
        return output;
    }

    @Override
    public char[] transformToCharsAllowInplace(char[] chars) {
        for (int i = 0; i < chars.length; i++) {
            chars[i] = rules[chars[i]];
        }
        return chars;
    }

    // No optimization here. Can we do it without conditionals?
    @Override
    public String transform(String s) {
        return new String(transformToChars(s.toCharArray()));
    }

    /* Stream oriented implementations */

    @Override
    public int read() throws IOException {
        try {
            if (in != null) {
                int codePoint = in.read();
                if (codePoint != -1) {
                    return rules[codePoint];
                } else {
                    return -1;
                }
            } else if (sourceBuffer != null) {
                return rules[sourceBuffer.take()];
            }
            throw new IllegalStateException(NO_SOURCE);
        } catch (NoSuchElementException e) {
            return -1;
        }
    }

    @Override
    public int read(CircularCharBuffer cbuf, int length) throws IOException {
        int read = 0;
        try {
            if (in != null) {
                while (read < length) {
                    cbuf.put(rules[in.read()]);
                    read++;
                }
            } else if (sourceBuffer != null) {
                while (read < length) {
                    cbuf.put(rules[sourceBuffer.take()]);
                    read++;
                }
            } else {
                throw new IllegalStateException(NO_SOURCE);
            }
        } catch (NoSuchElementException e) {
            if (length == 0) {
                return 0;
            }
            return read == 0 ? -1 : read;
        }
        return read;
    }

    @Override
    public int read(char[] cbuf, int off, int length) throws IOException {
        int read = 0;
        try {
            if (in != null) {
                read = in.read(cbuf, off, length);
                transformToCharsInplace(cbuf, off, read);
            } else if (sourceBuffer != null) {
                while (read < length) {
                    cbuf[off + read] = rules[sourceBuffer.take()];
                    read++;
                }
            } else {
                throw new IllegalStateException(NO_SOURCE);
            }
        } catch (NoSuchElementException e) {
            if (length == 0) {
                return 0;
            }
            return read == 0 ? -1 : read;
        }
        return read;
    }

    /* Helpers */
    private void transformToCharsInplace(char[] chars, int off, int length) {
        for (int i = 0; i < length; i++) {
            chars[off + i] = rules[chars[off + i]];
        }
    }

    @Override
    public String toString() {
        return "CharReplacer(...)";
    }
}
