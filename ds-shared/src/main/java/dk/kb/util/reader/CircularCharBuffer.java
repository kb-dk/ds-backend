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
import java.io.StringWriter;
import java.nio.CharBuffer;
import java.util.Iterator;
import java.util.Locale;
import java.util.NoSuchElementException;

/**
 * A memory efficient Queue/Reader-like mechanism for buffering character data.
 * It avoids memory reallocations by traversing its internal character buffer
 * in a circular manner, hence the name of the class.
 *
 * The buffer is not thread-safe. It is method-compatible with Reader.
 *
 * Note: the Queue-calls involves conversion between char and Character and
 * are thus not the fastest.
 */
public class CircularCharBuffer implements CharSequence, Iterable<Character> {
    private static final int GROWTH_FACTOR = 2;

    /**
     * The maximum capacity of the buffer + 1. If the maximum capacity is
     * Integer.MAX_VALUE, max is also Integer.MAX_VALUE.
     *
     * The +1 hack is due to performance optimization.
     */
    private int max; // Maximum size
    private int first = 0;
    private int next = 0; // if first = next, the buffer is empty
    private char[] array;

    /**
     * Create a new buffer with an initial capacity of {@code initialSize}
     * characters and a maximum allowed size of {@code maxSize} characters.
     *
     * The buffer will automatically grow beyond {@code initialSize} as data
     * is added, but will raise an error if the allocation would go above
     * {@code maxSize}.
     *
     * @param initialSize number of characters for the initial allocation
     * @param maxSize     maximum number of characters that can be stored in this buffer
     */
    public CircularCharBuffer(int initialSize, int maxSize) {
        array = new char[initialSize];
        this.max = maxSize;
        if (max != Integer.MAX_VALUE) {
            this.max++; // To make room for the first != next invariant
        }
    }

    /**
     * Create a new buffer with an initial capacity equal to the length of the given String s and a max size of
     * the same length + 1. Then fills the buffer with the characters from the given String.
     * @param s the initial content.
     */
    public CircularCharBuffer(String s) {
        array = new char[s.length()];
        max = s.length()+1;
        add(s);
    }

    /**
     * Put the char in the buffer, expanding it if necessary.
     *
     * @param c the character to add to the buffer.
     * @throws ArrayIndexOutOfBoundsException if the buffer needs to be expanded, but has reached the maximum size.
     */
    public void put(char c) {
        if (size() == array.length - 1) {
            extendCapacity();
        }
        array[next++] = c;
        next %= array.length;
/*        if (next == array.length) {
            next = 0;
        }*/
    }

    /**
     * Puts the chars in the buffer, expanding it if necessary.
     *
     * @param chars the chars to add.
     * @throws ArrayIndexOutOfBoundsException if the buffer needs to be expanded, but has reached the maximum size.
     */
    public void put(char[] chars) {
        // TODO: Consider optimizing this with arraycopy
        for (char c : chars) {
            put(c);
        }
    }

    /**
     * Insert a {@code CharSequence} such as a {@link String} into the buffer.
     *
     * @param s the character sequence to add.
     * @throws ArrayIndexOutOfBoundsException if the buffer needs to be expanded, but has reached the maximum size.
     */
    public void put(CharSequence s) {
        for (int i = 0; i < s.length(); i++) {
            put(s.charAt(i));
        }
    }

    /**
     * {@link java.io.Reader}-compatible read.
     *
     * @return the character read, as an integer in the range 0 to 65535
     *         (0x00-0xffff), or -1 if the end of the stream has been reached.
     */
    public int read() {
        if (isEmpty()) {
            return -1;
        }
        return take();
    }

    /**
     * Retrieve the first element in the buffer and move the buffer head
     * to the next character.
     *
     * @return the next char in the buffer.
     * @throws NoSuchElementException if the buffer is empty.
     */
    public char take() {
        if (first == next) {
            throw new NoSuchElementException(
                    "take() called on empty buffer");
        }
        char result = array[first++];
        if (first == array.length) {
            first = 0;
        }
        return result;
    }

    /**
     * @return the maximum capacity of the buffer.
     * @see #size()
     */
    public int getMaximumCapacity() {
        return max == Integer.MAX_VALUE ? max : max - 1;
    }

    /**
     * An equivalent to {@link java.io.Reader#read(char[], int, int)}.
     * Moves buffered chars to cbuf.
     *
     * @param cbuf the buffer to move into.
     * @param off  the offset in the buffer to move into.
     * @param len  the maximum number of chars to move.
     * @return the number of moved chars or -1 if no chars were buffered.
     */
    public int read(char cbuf[], int off, int len) {
        if (len == 0) {
            return 0;
        }
        if (size() == 0) {
            return -1;
        }
        if (first < next) {
            int moved = Math.min(len, next - first);
            System.arraycopy(array, first, cbuf, off, moved);
            first += moved;
            return moved;
        }
        int moved = Math.min(len, array.length - first);
        System.arraycopy(array, first, cbuf, off, moved);
        first += moved;
        if (first == array.length) {
            first = 0;
        }
        if (moved == len || size() == 0) {
            return moved;
        }
        int movedExtra = Math.min(len - moved, next - first);
        System.arraycopy(array, first, cbuf, off + moved, movedExtra);
        first += movedExtra;
        return moved + movedExtra;
    }

    /**
     * An equivalent to {@link java.io.Reader#read(char[], int, int)}.
     * Moves buffered chars to other.
     *
     * @param other the buffer to move into.
     * @param len   the maximum number of chars to move.
     * @return the number of moved chars or -1 if no chars were buffered.
     */
    // TODO: Consider optimizing this with arraycopy
    public int read(CircularCharBuffer other, int len) {
        int counter = 0;
        while (size() > 0 && counter < len) {
            other.put(take());
            counter++;
        }
        if (len == 0) {
            return 0;
        }
        return counter == 0 ? -1 : counter;
    }

    /**
     * Constructs a char array with the full content of the buffer and clears
     * the buffer.
     *
     * @return a char array with the full content of the buffer.
     */
    public char[] takeAll() {
        int size = size();
        char[] result = new char[size];
        for (int i = 0; i < size; i++) {
            result[i] = take();
        }
        return result;
    }

    /**
     * Constructs a String with the full content of the buffer and clears the
     * buffer.
     *
     * @return a String with the full content of the buffer;
     */
    public String takeString() {
        int size = size();
        StringWriter sw = new StringWriter(size);
        for (int i = 0; i < size; i++) {
            sw.append(take());
        }
        return sw.toString();
    }

    /**
     * Constructs a String with the full content of the buffer without
     * affecting the state of the buffer.
     *
     * @return a string representation of the buffer contents
     */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        int size = size();
        for (int i = 0; i < size; i++) {
            b.append(peek(i));
        }

        return b.toString();
    }

    /**
     * Constructs a String content of the buffer from offset to offset+length without
     * affecting the state of the buffer.
     * @param offset where to start reading.
     * @param length how many characters to read.
     * @return a string representation of part of the buffer.
     */
    public String subset(int offset, int length) {
        StringBuilder b = new StringBuilder();
        for (int i = offset; i < length; i++) {
            b.append(peek(i));
        }
        return b.toString();
    }

    /**
     * Get the character {@code ahead} steps from the current position in the
     * buffer. Calling this method will affect the state of the buffer.
     *
     * @param ahead the number of characters to peek ahead.
     * @return the character at the offset ahead.
     * @throws ArrayIndexOutOfBoundsException if {@code ahead} is past the buffer end. That is, calling this
     *                                        method on an empty buffer will always throw this exception
     */
    public char peek(int ahead) {
        if (ahead >= size()) {
            throw new ArrayIndexOutOfBoundsException(
                    "Requesting a peek(" + ahead + ") when the size is only " + size());
        }
        return array[(first + ahead) % array.length];
    }

    /**
     * Clears the content of the buffer. This does not de-allocate any memory.
     */
    public void clear() {
        first = 0;
        next = 0;
    }

    /**
     * Number of characters in the buffer
     *
     * @return the number of characters in the buffer.
     * @see #getMaximumCapacity()
     */
    public int size() {
        if (first <= next) {
            return next - first;
        }
        return array.length - first + next;
    }

    private void extendCapacity() {
        if (array.length == max) {
            throw new ArrayIndexOutOfBoundsException("The buffer if full and cannot be expanded further");
        }

        int newSize = Math.min(max, Math.max(array.length + 1, array.length * GROWTH_FACTOR));
        char[] newArray = new char[newSize];
        if (next == first) { // Empty
            array = newArray;
            return;
        }
        if (next < first) {
            System.arraycopy(array, first, newArray, 0, array.length - first);
            System.arraycopy(array, 0, newArray, array.length - first, next);
        } else {
            System.arraycopy(array, 0, newArray, first, next - first);
        }
        int oldSize = size();
        array = newArray;
        first = 0;
        next = oldSize;
    }

    /**
     * Number of characters in the buffer
     *
     * @return the number of characters in the buffer.
     */
    @Override
    public int length() {
        return size();
    }

    /**
     * Get the {@code n}'th character in the buffer. This method is equivalent
     * to {@link #peek(int n)}.
     *
     * @param n offset into the character buffer
     * @return the character at position {@code n}
     */
    @Override
    public char charAt(int n) {
        return peek(n);
    }

    /**
     * Return the index within this string of the first occurrence of the
     * specified substring.
     *
     * @param str the String to locate.
     * @return the index of the first occurence of str or -1 if the str could
     *         not be located.
     * @see String#indexOf(String)
     */
    public int indexOf(final String str) {
        return indexOf(str, 0);
    }

    /**
     * Return the index within this string of the first occurrence of the
     * specified substring, starting at the specified index.
     *
     * @param str       the String to locate.
     * @param fromIndex the index to start from.
     * @return the index of the first occurence of str, starting from fromIndex
     *         or -1 if the str could not be located.
     * @see String#indexOf(String, int)
     */
    public int indexOf(final String str, final int fromIndex) {
        begin:
        for (int pos = fromIndex; pos < size() - str.length() + 1; pos++) {
            for (int i = 0; i < str.length(); i++) {
                if (str.charAt(i) != charAt(pos + i)) {
                    continue begin;
                }
            }
            return pos;
        }
        return -1;
    }

    /**
     * Get a circular char buffer reflecting a subsequence of this one.
     * The returned buffer will start with the character at {@code start} and
     * end with the character at {@code end - 1}.
     *
     * The new buffer will have its maximum size equalling the maximum size of
     * the buffer from which it was created.
     *
     * Calling this method will not affect the state of the buffer.
     *
     * @param start the start offset into this buffer, inclusive
     * @param end   the end index into this buffer, exclusive
     * @return a newly allocated circular char buffer
     * @throws IllegalArgumentException       if {@code start} is negative
     * @throws ArrayIndexOutOfBoundsException if {@code end} is past the buffer length
     */
    @Override
    public CircularCharBuffer subSequence(int start, int end) {
        if (end < start) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, 
                    "Ending point, %s, is before starting point, %s, for subsequence",
                    end, start));
        } else if (start < 0) {
            throw new IllegalArgumentException(
                    "Starting point for subSequence is negative: " + start);
        } else if (end > length()) {
            throw new ArrayIndexOutOfBoundsException(
                    "Ending point of subSequence is past buffer end: " + end);
        }

        CircularCharBuffer child = new CircularCharBuffer(length(), getMaximumCapacity());
        for (int i = start; i < end; i++) {
            child.put(charAt(i));
        }

        return child;
    }

    public boolean add(CharSequence chars) {
        for (int i = 0 ; i < chars.length() ; i++) {
            put(chars.charAt(i));
        }
        return chars.length() > 0;
    }

    /* Queue<Character> interface */

    public boolean add(Character character) {
        put(character);
        return true;
    }

    public boolean offer(Character character) {
        try {
            put(character);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public Character remove() {
        return take();
    }

    public Character poll() {
        if (isEmpty()) {
            return null;
        }
        return take();
    }

    public Character element() {
        if (isEmpty()) {
            throw new NoSuchElementException(
                    "element() called on empty buffer");
        }
        return peek();
    }

    public Character peek() {
        if (isEmpty()) {
            return null;
        }
        return peek(0);
    }

    /* Collection<Character> interface */

    public boolean isEmpty() {
        return first == next;
    }

    public boolean contains(Object o) {
        if (o == null || !(o instanceof Character)) {
            return false;
        }
        Character c = (Character) o;
        for (int i = 0; i < size(); i++) {
            if (c.equals(Character.valueOf(charAt(i)))) {
                return true;
            }
        }
        return false;
    }

    /**
     * The iterator is non-destructive. It is not fail-fast.
     *
     * @return an iterator over the content of the buffer.
     */
    @Override
    public Iterator<Character> iterator() {
        return new CircularCharBufferIterator();
    }

    private class CircularCharBufferIterator implements Iterator<Character> {
        private int pos = 0;

        @Override
        public boolean hasNext() {
            return pos < size();
        }

        @Override
        public Character next() {
            return charAt(pos++);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Remove not supported");
        }
    }

    // TODO: Implement the rest of the Queue<Character> interface

    public Object[] toArray() {
        Character[] result = new Character[size()];
        for (int i = 0; i < size(); i++) {
            result[i] = charAt(i); // Need to iterate due to cast
        }
        return result;
    }
/*
    public <T> T[] toArray(T[] a) {
        Character[] result;
        try { // How do we test for array-type?
            result = (Character[])a;
        } catch (ClassCastException e) {
            result = new Character[size()];
        }

        result = result.length < size() ? new Character[size()] : result;
        for (int i = 0 ; i < result.length ; i++) {
            result[i] = i < size() ? charAt(i) : null;
        }
        return result;
    }

    public boolean remove(Object o) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean containsAll(Collection<?> c) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean addAll(Collection<? extends Character> c) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean removeAll(Collection<?> c) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean retainAll(Collection<?> c) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }*/

    /* Reader-compatible API */

    /**
     * Reader-compatible wrapper for {@link #clear()}.
     */
    public void close() {
        clear();
    }

    /**
     * Reader-compatible method (always fail as mark is not supported).
     *
     * @param readAheadLimit ignored.
     * @throws IOException as this method is not supported.
     */
    public void mark(int readAheadLimit) throws IOException {
        throw new IOException("Mark not supported (readAheadLimit given: "
                              + readAheadLimit + ")");
    }

    /**
     * Reader-compatible method.
     *
     * @return false.
     */
    public boolean markSupported() {
        return false;
    }

    /**
     * Reader-compatible method. Empries the buffer into the target.
     *
     * @param target where to put the content of the buffer.
     * @return the number of moved characters or -1 if empty.
     */
    public int read(CharBuffer target) {
        int count = 0;
        while (!isEmpty()) {
            target.put(take());
            count++;
        }
        return count == 0 ? -1 : count;
    }

    /**
     * Reader-compatible method.
     *
     * @return true, as a CircularCharBuffer never blocks.
     */
    public boolean ready() {
        return true;
    }

    /**
     * @throws IOException always throws as mark is not supported.
     */
    public void reset() throws IOException {
        throw new IOException("Mark not supported, so reset is not supported");
    }

    /**
     * Reader-compatible method.
     *
     * @param n the amount of characters to skip.
     * @throws IOException if n &lt; {@link #size()}.
     */
    public void skip(long n) throws IOException {
        if (n < 0) {
            throw new IllegalArgumentException("skip(" + n + ") failed: Only positive skips allowed");
        }
        int oldSize = size();
        if (n > oldSize) {
            clear();
            throw new IOException("skip(" + n + ") called with only " + oldSize
                                  + " available chars. Buffer is cleared");
        }
        for (int i = 0; i < n; i++) {
            take();
        }
    }

    /**
     * Copies the content of the buffer into the given array.
     * @param dest the array to copy into.
     * @return the number of values copied.
     * @throws ArrayIndexOutOfBoundsException if the destination array was not large enough.
     */
    public int copy(char[] dest) throws ArrayIndexOutOfBoundsException {
        return copy(dest, 0, length());
    }

    /**
     * Copies the content of the buffer into the given array.
     * @param dest the array to copy into.
     * @param start the start position in the destination to copy into.
     * @return the number of values copied.
     * @throws ArrayIndexOutOfBoundsException if the destination array was not large enough.
     */
    public int copy(char[] dest, int start) throws ArrayIndexOutOfBoundsException {
        return copy(dest, start, size());
    }

    /**
     * Copies the content of the buffer into the given array.
     * @param dest the array to copy into.
     * @param offset the start position in the destination to copy into.
     * @param length the maximum number of values to copy.
     * @return the number of values copied.
     * @throws ArrayIndexOutOfBoundsException if the destination array was not large enough.
     */
    public int copy(char[] dest, int offset, int length) throws ArrayIndexOutOfBoundsException {
        final int realLength = length > size() ? size() : length;

        if (realLength == 0) {
            return 0;
        }

        // Simple direct copy case
        int end = first + length;
        if (end < array.length) {
            System.arraycopy(array, first, dest, offset, realLength);
            return length;
        }

        // Wrap-around case
        int copied = array.length - first;
        System.arraycopy(array, first, dest, offset, copied);
        System.arraycopy(array, 0, dest, offset + copied, realLength-copied);
        return realLength;
    }
}
