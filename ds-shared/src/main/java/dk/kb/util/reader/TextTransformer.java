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
package dk.kb.util.reader;

import java.io.IOException;
import java.io.Reader;

/**
 * Defines a text-oriented transformer (chars and Strings) with the main focus
 * on performance of multiple transformations with the same rules.
 *
 * While a lot of the methods seem to do the exactly same thing, having them
 * explicitely defined allows for an implementation to do optimizations.
 */
public interface TextTransformer {
    public static final String NO_SOURCE =
            "Neither sourceReader nor sourceBuffer has been set as source";

    /**
     * Performs transformations on the content of the String.
     *
     * @param s the input String.
     * @return the transformed String.
     */
    public String transform(String s);

    /**
     * Transforms a single char to an array of chars.
     *
     * @param c the char to transform.
     * @return the output chars, possibly of length 0.
     */
    public char[] transformToChars(char c);

    /**
     * Transforms an array of chars to an array of chars.
     *
     * @param chars the chars to transform.
     * @return the output chars, possibly of length 0.
     */
    public char[] transformToChars(char[] chars);

    /**
     * Transforms an array of chars to an array of chars.
     *
     * If the transformation results in exactly the same number of chars as
     * the input, it is allowed for the implementation to re-use the input
     * array.
     *
     * @param chars the chars to transform.
     * @return the output chars, possibly of length 0.
     */
    public char[] transformToCharsAllowInplace(char[] chars);


    /* Stream-oriented */


    /**
     * Assigns a reader as source for transformation.
     *
     * @param reader where to get chars.
     * @return always returns {@code this}.
     */
    public TextTransformer setSource(Reader reader);

    /**
     * Assigns a char buffer as source for transformation.
     *
     * @param cbuf where to get chars.
     * @return always return {@code this}.
     */
    public TextTransformer setSource(CircularCharBuffer cbuf);

    /**
     * @return the next char or -1 if there are no more chars available.
     * @throws java.io.IOException if an I/O error occured.
     */
    public int read() throws IOException;

    /**
     * Fills transformed chars in the buffer.
     *
     * @param cbuf   the buffer to assign chars to.
     * @param off    where to start putting chars in the buffer.
     * @param length the maximum number of chars to put in the buffer.
     * @return the number of chars filled or -1 if there are no more chars.
     * @throws java.io.IOException if an I/O error occured.
     */
    public int read(char[] cbuf, int off, int length) throws IOException;

    /**
     * Fills transformed chars in the buffer.
     *
     * @param cbuf   the buffer to assign shars to.
     * @param length the maximum number of chars to put in the buffer.
     * @return the number of chars filled or -1 if there are no more chars.
     * @throws java.io.IOException if an I/O error occured.
     */
    public int read(CircularCharBuffer cbuf, int length) throws IOException;
}
