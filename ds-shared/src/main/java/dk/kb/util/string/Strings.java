/* $Id: Strings.java,v 1.8 2007/12/04 13:22:01 mke Exp $
 * $Revision: 1.8 $
 * $Date: 2007/12/04 13:22:01 $
 * $Author: mke $
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
package dk.kb.util.string;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;

/**
 * Convenience methods for string manipulations.
 */

public class Strings {

    /**
     * Utility class, don't initialise.
     */
    private Strings() {
    }

    // ThreadLocal StringBuilders removed as they are prone to causing subtle memory overheads with
    // multi-threaded web applications
/*    private static final ThreadLocal<StringBuilder> localBuilder =
            new ThreadLocal<StringBuilder>() {
                @Override
                protected StringBuilder initialValue() {
                    return new StringBuilder();
                }

                @Override
                public StringBuilder get() {
                    StringBuilder b = super.get();
                    b.setLength(0);
                    return b;
                }
            };

    private static final ThreadLocal<char[]> localBuffer =
            new ThreadLocal<char[]>() {
                @Override
                protected char[] initialValue() {
                    return new char[1024];
                }
            };
*/
    /**
     * Convenience method: Extract the stacktrace from an Exception and returns
     * it as a String.
     *
     * @param exception the exception to expand
     * @return the stacktrace from the exception, as a String
     */
    public static String getStackTrace(Throwable exception) {
        StringWriter writer = new StringWriter();
        PrintWriter printer = new PrintWriter(writer);
        exception.printStackTrace(printer);
        return writer.toString();
    }

    /**
     * Concatenate all elements in a collection with ", " as delimiter. If a list contains "foo", "bar", and "baz",
     * the returned string will be <code>foo, bar, baz</code>.
     * If any of the collection's elements are null the empty string will be used.
     *
     * @param c         The collection which elements will be concatenated as strings
     * @return A string representation of the collection. If the collection is empty the empty string will be returned.
     * @throws NullPointerException if the collection or delimiter is null.
     */
    public static <T> String join(Collection<T> c) {
       return join(c, ", ", Long.MAX_VALUE);
    }

    /**
     * Concatenate all elements in a collection with ", " as delimiter.
     *
     * @param c         The collection which elements will be concatenated as strings
     * @param max       the maximum number of elements to concatenate. If the number of elements exceeds this, max
     *                  elements will be used, followed by "...".
     * @return A string representation of the collection. If the collection is empty the empty string will be returned.
     * @throws NullPointerException if the collection or delimiter is null.
     */
    public static <T> String join(Collection<T> c, long max) {
       return join(c, ", ", max);
    }

    /**
     * Concatenate all elements in a collection with a given delimiter.
     * For example if a list contains "foo", "bar", and "baz" and the delimiter
     * is ":", the returned string will be <code>foo:bar:baz</code>.
     * If any of the collection's elements are null the empty string will be used.
     *
     * @param c         The collection which elements will be concatenated as strings
     * @param delimiter symbol(s) to put in between elements
     * @return A string representation of the collection. If the collection is empty the empty string will be returned.
     * @throws NullPointerException if the collection or delimiter is null.
     */
    public static <T> String join(Collection<T> c, String delimiter) {
       return join(c, delimiter, Long.MAX_VALUE);
    }

    /**
     * Concatenate all elements in a collection with a given delimiter.
     * For example if a list contains "foo", "bar", and "baz" and the delimiter
     * is ":", the returned string will be <code>foo:bar:baz</code>.
     * If any of the collection's elements are null the empty string will be used.
     *
     * @param c         The collection which elements will be concatenated as strings
     * @param delimiter symbol(s) to put in between elements
     * @param max       the maximum number of elements to concatenate. If the number of elements exceeds this, max
     *                  elements will be used, followed by "...".
     * @return A string representation of the collection. If the collection
     *         is empty the empty string will be returned.
     * @throws NullPointerException if the collection or delimiter is null.
     */
    public static <T> String join(Collection<T> c, String delimiter, long max) {
        if (c == null) {
            throw new NullPointerException("Collection argument is null");
        } else if (delimiter == null) {
            throw new NullPointerException("Delimiter argument is null");
        }

        StringBuilder b = new StringBuilder();
        long counter = 0;
        for (Object o : c) {
            if (b.length() != 0) {
                b.append(delimiter);
            }
            if (counter++ == max) {
                b.append("...");
                break;
            }
            b.append(o == null ? "" : o.toString());
        }

        return b.toString();
    }

    /**
     * @param a the Objects to create a String representation for.
     * @return the input objects converted to Strings and concatenated with the given delimiter.
     * @see #join(java.util.Collection)
     */
    public static String join(Object[] a) {
        return join(a, ", ", Long.MAX_VALUE);
    }

    /**
     * @param a the Objects to create a String representation for.
     * @param max maximum number of Objects to convert to Strings. Objects beyond these will be represented with "...".
     * @return the input objects converted to Strings and concatenated with the given delimiter.
     * @see #join(java.util.Collection, long)
     */
    public static String join(Object[] a, long max) {
        return join(a, ", ", max);
    }

    /**
     * @param a the Objects to create a String representation for.
     * @param delimiter the delimer to insert between each Objects String representation.
     * @return the input objects converted to Strings and concatenated with the given delimiter.
     * @see #join(Collection, String)
     */
    public static String join(Object[] a, String delimiter) {
        return join(a, delimiter, Long.MAX_VALUE);
    }

    /**
     * @param a the Objects to create a String representation for.
     * @param delimiter the delimer to insert between each Objects String representation.
     * @param max maximum number of Objects to convert to Strings. Objects beyond these will be represented with "...".
     * @return the input objects converted to Strings and concatenated with the given delimiter.
     * @see #join(java.util.Collection, String, long)
     */
    public static String join(Object[] a, String delimiter, long max) {
        if (a == null) {
            throw new NullPointerException("Collection argument is null");
        } else if (delimiter == null) {
            throw new NullPointerException("Delimiter argument is null");
        }

        StringBuilder b = new StringBuilder();

        long counter = 0;
        for (Object o : a) {
            if (b.length() != 0) {
                b.append(delimiter);
            }
            if (counter++ == max) {
                b.append("...");
                break;
            }
            b.append(o == null ? "" : o.toString());
        }

        return b.toString();
    }

    // TODO: Create a callback implementation so we can do this for all atomics without duplicating code too much
    public static String join(int[] values) {
        return join(values, ", ", Long.MAX_VALUE);
    }
    public static String join(int[] values, String delimiter, long max) {
        if (values == null) {
            throw new NullPointerException("Collection argument is null");
        } else if (delimiter == null) {
            throw new NullPointerException("Delimiter argument is null");
        }

        StringBuilder b = new StringBuilder();

        long counter = 0;
        for (int value : values) {
            if (b.length() != 0) {
                b.append(delimiter);
            }
            if (counter++ == max) {
                b.append("...");
                break;
            }
            b.append(value);
        }

        return b.toString();
    }

    /**
     * Read all character data from {@code r} and create a String based on
     * that data. The reader is guaranteed to be closed when this method
     * returns.
     *
     * This method is optimized to only allocate the needed space for the final
     * string and not any intermediate buffers.
     *
     * @param r the reader to flush
     * @return a string representation of the character stream
     * @throws IOException if failing to read from {@code r}
     */
    public static String flush(Reader r) throws IOException {
        int numRead;
        char[] buf = new char[1024];
        StringBuilder b = new StringBuilder();

        try {
            while ((numRead = r.read(buf)) != -1) {
                b.append(buf, 0, numRead);
            }
        } finally {
            r.close();
        }

        return b.toString();
    }

    /**
     * As {@link #flush(java.io.Reader)}, but operate on a InputStream
     *
     * @param in the input stream to flush
     * @return a string representation of the input stream
     * @throws IOException if failing to read from {@code in}
     */
    public static String flush(InputStream in) throws IOException {
        return flush(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    /**
     * Read all character data from {@code r} and create a String based on
     * that data. The reader is guaranteed to be closed when this method
     * returns.
     *
     * The difference from this method to
     * {@link #flush(java.io.Reader)} is that it can not throw an IOException.
     * It is expected that the caller guarantees that the character stream is
     * based on a local memory buffer.
     *
     * This method is optimized to only allocate the needed space for the final
     * string and not any intermediate buffers.
     *
     * @param r the reader to flush
     * @return a string representation of the character stream
     * @throws RuntimeException if failing to read from {@code r}
     */
    public static String flushLocal(Reader r) {
        try {
            return flush(r);
        } catch (IOException e) {
            throw new RuntimeException("Unexpected IOException when reading character stream", e);
        }
    }

    /**
     * As {@link #flushLocal(java.io.Reader)}, but operate on a InputStream
     *
     * @param in the input stream to flush
     * @return a string representation of the input stream
     * @throws RuntimeException if failing to read from {@code in}
     */
    public static String flushLocal(InputStream in) {
        return flushLocal(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    /**
     * Wrap a {@code char} array as a {@link CharSequence} without doing any
     * memory- allocations or copying.
     *
     * Note that since the original array underneath the returned character
     * sequence is exactly {@code chars} any changes made to {@code chars}
     * will be reflected in the returned character sequence as well.
     *
     * @param chars the character array to wrap
     * @return {@code chars} wrapped as a {@link CharSequence}
     */
    public static CharSequence asCharSequence(final char[] chars) {
        return new CharSequence() {

            @Override
            public int length() {
                return chars.length;
            }

            @Override
            public char charAt(int index) {
                // Note: This will indeed throw an exception of the type
                // required by this method's contract
                return chars[index];
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return asCharSequence(Arrays.copyOfRange(chars, start, end));
            }

            @Override
            public String toString() {
                return new String(chars);
            }
        };
    }

    /**
     * Finds the first index of the occurence of {@code c} in {@code chars}
     * or returns -1.
     *
     * @param c     the character to look for
     * @param chars the character sequence to search in
     * @return the index for which {@code chars.charAt(i) == c} or -1 if
     *         {@code c} doesn't exist in {@code chars}
     */
    public static int indexOf(char c, CharSequence chars) {
        return indexOf(c, 0, chars);
    }

    /**
     * Finds the first index of the occurence of {@code c} in {@code chars}
     * or returns -1.
     *
     * @param c     the character to look for.
     * @param offset the starting point for searching.
     * @param chars the character sequence to search in.
     * @return the index for which {@code chars.charAt(i) == c} or -1 if
     *         {@code c} doesn't exist in {@code chars}
     */
    public static int indexOf(char c, int offset, CharSequence chars) {
        for (int i = offset; i < chars.length(); i++) {
            if (c == chars.charAt(i)) {
                return i;
            }
        }
        return -1;
    }
}
