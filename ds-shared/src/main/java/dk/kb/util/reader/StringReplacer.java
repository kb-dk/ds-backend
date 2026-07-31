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

import dk.kb.util.string.Strings;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Map;

/**
 * A reader that replaces Strings on the fly. This replacer uses a tree for
 * finding matches for replacement and should be fairly effective for a large
 * number of replacements, as long as there is a non-trivial amount of diversity
 * among the replacements (e.g. "aaa1" ⇒ "foo", "aaa2" ⇒ "bar" etc.) will give
 * poor performance when the number of cases is in the hundreds or thousands.
 */
public class StringReplacer extends ReplaceReader {
    private CircularCharBuffer readerBuffer;
    private CircularCharBuffer destinationBuffer;
    private CircularCharBuffer tempInBuffer = new CircularCharBuffer(10, Integer.MAX_VALUE);
    private CircularCharBuffer tempOutBuffer = new CircularCharBuffer(10, Integer.MAX_VALUE);
    private Node tree = new Node();
    private int minBufferSize = 10;
    private boolean eof = false;
    private long replacementsFromCurrentSource = 0;

    private final String rulesSample;

    /**
     * Create a new replacer replacing substrings in the {@code in} stream
     * based on the rules defined by the {@code replacements} argument.
     *
     * @param in           the character stream in which to replace substrings
     * @param replacements the Strings to replace, from target to destination.
     */
    public StringReplacer(Reader in, Map<String, String> replacements) {
        super(in);
        rulesSample = Strings.join(replacements.entrySet(), 10);
        for (Map.Entry<String, String> replacement : replacements.entrySet()) {
            minBufferSize = Math.max(minBufferSize, replacement.getKey().length());
            tree.addRule(replacement.getKey(), replacement.getValue(), 0);
        }
        initBuffers(minBufferSize);
    }

    private void initBuffers(int minBufferSize) {
        this.minBufferSize = minBufferSize;
        readerBuffer = new CircularCharBuffer(minBufferSize, minBufferSize);
        destinationBuffer = new CircularCharBuffer(minBufferSize, Integer.MAX_VALUE);
    }

    /**
     * Create a new replacer, with an empty input stream, replacing substrings
     * based on the rules defined by the {@code replacements} argument.
     *
     * Before calling any other methods on this reader you chould set its input
     * stream by calling {@link StringReplacer#setSource(java.io.Reader)}.
     *
     * @param replacements the Strings to replace, from target to destination.
     */
    public StringReplacer(Map<String, String> replacements) {
        this(new StringReader(""), replacements);
    }

    private StringReplacer(int minBufferSize, Node ruleTree) {
        super(null);
        rulesSample = "";
        tree = ruleTree;
        initBuffers(minBufferSize);
    }

    /**
     * A clone of the StringReplacer will share the rules of the replacer, but
     * will otherwise be independent. A clone will not have a source defined.
     * Creating a clone is very cheap with regard to memory and processing time.
     *
     * @return a clone of this ReplaceReader.
     */
    @SuppressWarnings({"CloneDoesntCallSuperClone",
                       "CloneDoesntDeclareCloneNotSupportedException"})
    @Override
    public Object clone() {
        return new StringReplacer(minBufferSize, tree);
    }

    @Override
    public synchronized char[] transformToChars(char c) {
        tempInBuffer.clear();
        tempInBuffer.put(c);
        return returnReplacement(tempInBuffer);
    }

    private char[] returnReplacement(CircularCharBuffer source) {
        tempOutBuffer.clear();
        while (source.size() > 0) {
            Node replacement = tree.getReplacement(source, 0);
            if (replacement == null) { // Copy a char, then repeat
                tempOutBuffer.put(source.take());
                continue;
            }
            for (int i = 0; i < replacement.from.length(); i++) {
                source.take(); // Flush the target
            }
            tempOutBuffer.put(replacement.to); // Add the replacement
        }
        return tempOutBuffer.takeAll();
    }

    @Override
    public char[] transformToChars(char[] chars) {
        tempInBuffer.clear();
        tempInBuffer.put(chars);
        return returnReplacement(tempInBuffer);
    }

    @Override
    public String transform(String s) {
        tempInBuffer.clear();
        tempInBuffer.put(s.toCharArray());
        return new String(returnReplacement(tempInBuffer));
    }

    @Override
    public char[] transformToCharsAllowInplace(char[] chars) {
        return transformToChars(chars);
    }

    /* Stream based */
    @Override
    public synchronized int read(CircularCharBuffer cbuf, int length) throws IOException {
        ensureBuffers(length);
        return destinationBuffer.read(cbuf, length);
    }

    @Override
    public synchronized int read() throws IOException {
        ensureBuffers(1);
        if (destinationBuffer.size() > 0) {
            return destinationBuffer.take();
        }
        return -1;
    }

    @Override
    public synchronized int read(char cbuf[], int off, int len) throws IOException {
        ensureBuffers(len); // Dangerous as we risk large buffer
        return destinationBuffer.read(cbuf, off, len);
    }

    /**
     * Ensures that the destination buffer contains minSize characters or that
     * the source has reached EOF and that all characters from source has been
     * processed.
     *
     * @param minSize the minimum size wanted in destination.
     * @throws IOException if an I/Oerror happened in source.
     */
    private void ensureBuffers(int minSize) throws IOException {
        while (destinationBuffer.size() < minSize) {
            CircularCharBuffer source = getSourceBuffer();
            if (source.size() == 0) { // Source dried up
                return;
            }
            Node replacement = tree.getReplacement(source, 0);
            if (replacement == null) { // Copy a char, then repeat
                destinationBuffer.put(source.take());
                continue;
            }
            for (int i = 0; i < replacement.from.length(); i++) {
                readerBuffer.take(); // Flush the target
            }
            destinationBuffer.put(replacement.to); // Add the replacement
            replacementsFromCurrentSource++;
            // Don't return as replacement might be of length 0
        }
    }

    private CircularCharBuffer getSourceBuffer() throws IOException {
        if (sourceBuffer != null) {
            return sourceBuffer;
        }
        if (in == null) {
            throw new IllegalStateException(NO_SOURCE);
        }
        // We're using a reader, so read up to minBufferSize is we can
        int next = 0;
        while (readerBuffer.size() < minBufferSize && !eof && (next = in.read()) != -1) {
            readerBuffer.put((char) next);
        }
        if (next == -1) {
            eof = true;
        }
        return readerBuffer;
    }

    @Override
    public synchronized ReplaceReader setSource(Reader source) {
        super.setSource(source);
        readerBuffer.clear();
        destinationBuffer.clear();
        replacementsFromCurrentSource = 0;
        eof = false;
        return this;
    }

    /**
     * @return the number of replacements that has been performed on the
     *         current source.
     */
    public long getReplacementCount() {
        return replacementsFromCurrentSource;
    }

    private static class Node {
        public char c;
        public String from = null;
        public char[] to = null;
        private Node[] children = new Node[0];
        private boolean root = false;

        /**
         * Constructor for the super-Node.
         */
        public Node() {
            root = true;
        }

        public Node(char c) {
            this.c = c;
        }

        public Node getReplacement(CircularCharBuffer buffer) {
            return getReplacement(buffer, 0);
        }

        public Node getReplacement(CircularCharBuffer buffer, int level) {
            if (buffer.size() <= level) {
                return null;
            }
            if (root) {
                level--;
            }
            if (buffer.size() > level + 1) {
                char next = buffer.peek(level + 1);
                Node child = getChild(next);
                if (child != null) {
                    Node subReplacement = child.getReplacement(buffer, level + 1);
                    if (subReplacement != null) {
                        return subReplacement;
                    }
                }
            }
            return root || to == null || buffer.peek(level) != c ? null : this;
        }

        public void addRule(String from, String to, int level) {
            if (level >= from.length()) { // No more nodes
                return;
            }

            if (root) {
                level--;
            } else {
                if (level + 1 == from.length()) { // Leaf
                    this.from = from; // TODO: Warn on duplicate sources
                    this.to = to.toCharArray();
                    return;
                }
            }
            char next = from.charAt(level + 1);
            Node child = getChild(next);
            if (child == null) {
                child = addChild(next);
            }
            child.addRule(from, to, ++level);
        }

        private Node addChild(char next) {
            Node[] newChildren = new Node[children.length + 1];
            System.arraycopy(children, 0, newChildren, 0, children.length);
            Node child = new Node(next);
            newChildren[newChildren.length - 1] = child;
            children = newChildren;
            return child;
        }

        private Node getChild(char c) {
            for (Node child : children) {
                if (child.c == c) {
                    return child;
                }
            }
            return null;
        }
    }

    @Override
    public String toString() {
        return "StringReplacer(rules=" + rulesSample + ")";
    }
}
