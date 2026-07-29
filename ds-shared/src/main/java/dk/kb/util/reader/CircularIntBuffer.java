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
import java.util.Iterator;
import java.util.Locale;
import java.util.NoSuchElementException;

/**
 * A memory efficient Queue/Reader-like mechanism for buffering integers.
 * This shares much of its functionality with {@link CircularCharBuffer}.
 * It avoids memory reallocations by traversing its internal character buffer
 * in a circular manner.
 *
 * The buffer is not thread-safe.
 *
 * Note: the Queue-calls involved conversion between char and Character and
 * are thus not the fastest.
 */
public class CircularIntBuffer implements Iterable<Integer> {
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
    private int[] array;

    /**
     * Create a new buffer with an initial capacity of {@code initialSize}
     * elements and a maximum allowed size of {@code maxSize} elements.
     *
     * The buffer will automatically grow beyond {@code initialSize} as data
     * is added, but will raise an error if the allocation would go above
     * {@code maxSize}.
     *
     * @param initialSize number of elements for the initial allocation.
     * @param maxSize     maximum number of elements that can be stored in this buffer.
     */
    public CircularIntBuffer(int initialSize, int maxSize) {
        array = new int[initialSize];
        this.max = maxSize;
        if (max != Integer.MAX_VALUE) {
            this.max++; // To make room for the first != next invariant
        }
    }

    /**
     * Put the value in the buffer, expanding the buffer if necessary.
     *
     * @param value will be added to the buffer in O(1) amortized.
     * @throws ArrayIndexOutOfBoundsException if the buffer needs to be expanded, but has reached the maximum size.
     */
    public void put(int value) {
        if (size() == array.length - 1) {
            extendCapacity();
        }
        array[next++] = value;
        next %= array.length;
/*        if (next == array.length) {
            next = 0;
        }*/
    }

    /**
     * Puts the values in the buffer, expanding it if necessary.
     *
     * @param values will be added to the buffer in O(n) amortized.
     * @throws ArrayIndexOutOfBoundsException if the buffer needs to be expanded, but has reached the maximum size.
     */
    public void put(int[] values) {
        // TODO: Consider optimizing this with arraycopy
        for (int value : values) {
            put(value);
        }
    }

    /**
     * @return the next integer,
     * @throws ArrayIndexOutOfBoundsException if the queue is empty.
     */
    public int read() {
        if (isEmpty()) {
            throw new ArrayIndexOutOfBoundsException("No more elements");
        }
        return take();
    }

    /**
     * Retrieve the first element in the buffer and move the buffer head to the next element.
     *
     * @return the next element in the buffer.
     * @throws java.util.NoSuchElementException if the buffer is empty.
     */
    public int take() {
        if (first == next) {
            throw new NoSuchElementException("take() called on empty buffer");
        }
        int result = array[first++];
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
     * Moves buffered elements to cbuf.
     *
     * @param buf  the buffer to move into.
     * @param off  the offset in the buffer to move into.
     * @param len  the maximum number of elements to move.
     * @return the number of moved elements or -1 if no elements were buffered.
     */
    public int read(int buf[], int off, int len) {
        if (len == 0) {
            return 0;
        }
        if (size() == 0) {
            return -1;
        }
        if (first < next) {
            int moved = Math.min(len, next - first);
            System.arraycopy(array, first, buf, off, moved);
            first += moved;
            return moved;
        }
        int moved = Math.min(len, array.length - first);
        System.arraycopy(array, first, buf, off, moved);
        first += moved;
        if (first == array.length) {
            first = 0;
        }
        if (moved == len || size() == 0) {
            return moved;
        }
        int movedExtra = Math.min(len - moved, next - first);
        System.arraycopy(array, first, buf, off + moved, movedExtra);
        first += movedExtra;
        return moved + movedExtra;
    }

    /**
     * An equivalent to {@link java.io.Reader#read(char[], int, int)}.
     * Moves elements to other.
     *
     * @param other the buffer to move into.
     * @param len   the maximum number of elements to move.
     * @return the number of moved elements or -1 if no elements were buffered.
     */
    // TODO: Consider optimizing this with arraycopy
    public int read(CircularIntBuffer other, int len) {
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
     * Constructs an int array with the full content of the buffer and clears the buffer.
     *
     * @return an int array with the full content of the buffer.
     */
    public int[] takeAll() {
        int size = size();
        int[] result = new int[size];
        for (int i = 0; i < size; i++) {
            result[i] = take();
        }
        return result;
    }

    @Override
    public String toString() {
        return "CircularIntBuffer(size=" + size() + ")";
    }

    /**
     * Get the element {@code ahead} steps from the current position in the buffer. Calling this method will not affect
     * the state of the buffer.
     *
     * @param ahead the number of elements to peek ahead.
     * @return the element at the offset ahead.
     * @throws ArrayIndexOutOfBoundsException if {@code ahead} is past the buffer end. That is, calling this
     *                                        method on an empty buffer will always throw this exception
     */
    public int peek(int ahead) {
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
     * @return the number of elements in the buffer.
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
        int[] newArray = new int[newSize];
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
     * @return the number of elementsin the buffer.
     */
    public int length() {
        return size();
    }

    /**
     * Get a circular buffer reflecting a subsequence of this one.
     * The returned buffer will start with the element at {@code start} and
     * end with the element at {@code end - 1}.
     *
     * The new buffer will have its maximum size equalling the maximum size of
     * the buffer from which it was created.
     *
     * Calling this method will not affect the state of the buffer.
     *
     * @param start the start offset into this buffer, inclusive
     * @param end   the end index into this buffer, exclusive
     * @return a newly allocated circular buffer
     * @throws IllegalArgumentException       if {@code start} is negative
     * @throws ArrayIndexOutOfBoundsException if {@code end} is past the buffer length
     */
    public CircularIntBuffer subSequence(int start, int end) {
        if (end < start) {
            throw new IllegalArgumentException(String.format(
                    Locale.ROOT, "Ending point, %s, is before starting point, %s, for subsequence",
                    end, start));
        } else if (start < 0) {
            throw new IllegalArgumentException("Starting point for subSequence is negative: " + start);
        } else if (end > length()) {
            throw new ArrayIndexOutOfBoundsException("Ending point of subSequence is past buffer end: " + end);
        }

        CircularIntBuffer child = new CircularIntBuffer(length(), getMaximumCapacity());
        for (int i = start; i < end; i++) {
            child.put(peek(i));
        }

        return child;
    }

    /* Queue<Integer> interface */

    public boolean add(Integer value) {
        put(value);
        return true;
    }

    public boolean offer(Integer value) {
        try {
            put(value);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public Integer remove() {
        return take();
    }

    public Integer poll() {
        if (isEmpty()) {
            return null;
        }
        return take();
    }

    public Integer element() {
        if (isEmpty()) {
            throw new NoSuchElementException("element() called on empty buffer");
        }
        return peek();
    }

    public Integer peek() {
        if (isEmpty()) {
            return null;
        }
        return peek(0);
    }

    /* Collection<Integer> interface */

    public boolean isEmpty() {
        return first == next;
    }

    public boolean contains(Object o) {
        if (o == null || !(o instanceof Integer)) {
            return false;
        }
        Integer value = (Integer) o;
        for (int i = 0; i < size(); i++) {
            if (value == peek(i)) {
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
    public Iterator<Integer> iterator() {
        return new CircularIntBufferIterator();
    }

    private class CircularIntBufferIterator implements Iterator<Integer> {
        private int pos = 0;

        @Override
        public boolean hasNext() {
            return pos < size();
        }

        @Override
        public Integer next() {
            return peek(pos++);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Remove not supported");
        }
    }

    // TODO: Implement the rest of the Queue<Character> interface

    public Object[] toArray() {
        Integer[] result = new Integer[size()];
        for (int i = 0; i < size(); i++) {
            result[i] = peek(i); // Need to iterate due to cast
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
     * @throws java.io.IOException as this method is not supported.
     */
    public void mark(int readAheadLimit) throws IOException {
        throw new IOException("Mark not supported (readAheadLimit given: " + readAheadLimit + ")");
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
     * Reader-compatible method.
     *
     * @return true, as a CircularCharBuffer never blocks.
     */
    public boolean ready() {
        return true;
    }

    /**
     * @throws java.io.IOException always throws as mark is not supported.
     */
    public void reset() throws IOException {
        throw new IOException("Mark not supported, so reset is not supported");
    }

    /**
     * Reader-compatible method.
     *
     * @param n the amount of elements to skip.
     * @throws java.io.IOException if n &gt; {@link #size()}.
     */
    public void skip(long n) throws IOException {
        if (n < 0) {
            throw new IllegalArgumentException(
                    "skip(" + n + ") failed: Only positive skips allowed");
        }
        int oldSize = size();
        if (n > oldSize) {
            clear();
            throw new IOException(
                    "skip(" + n + ") called with only " + oldSize + " available chars. Buffer is cleared");
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
    public int copy(int[] dest) throws ArrayIndexOutOfBoundsException {
        return copy(dest, 0, length());
    }

    /**
     * Copies the content of the buffer into the given array.
     * @param dest the array to copy into.
     * @param start the start position in the destination to copy into.
     * @return the number of values copied.
     * @throws ArrayIndexOutOfBoundsException if the destination array was not large enough.
     */
    public int copy(int[] dest, int start) throws ArrayIndexOutOfBoundsException {
        return copy(dest, start, length());
    }

    /**
     * Copies the content of the buffer into the given array.
     * @param dest the array to copy into.
     * @param offset the start position in the destination to copy into.
     * @param length the maximum number of values to copy.
     * @return the number of values copied.
     * @throws ArrayIndexOutOfBoundsException if the destination array was not large enough.
     */
    public int copy(int[] dest, int offset, int length) throws ArrayIndexOutOfBoundsException {
        final int realLength = length > length() ? length() : length;

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
