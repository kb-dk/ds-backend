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


import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * Abstract class providing basic methods for making a TextTransformer that is
 * also a Reader.
 */
public abstract class ReplaceReader extends FilterReader implements TextTransformer, Cloneable {

    protected CircularCharBuffer sourceBuffer = null;

    public ReplaceReader(Reader reader) {
        super(dummyIfNull(reader));

        // Don't call setSource() here. Sub classes may have overridden
        // it and it can cause unexpected behaviour
        this.in = reader;
        sourceBuffer = null;
    }

    private static Reader dummyIfNull(Reader reader) {
        return reader == null ? new StringReader("") : reader;
    }

    /**
     * Reset the replace reader and prepare it for reading from {@code source}.
     * What ever replacement rules the replace reader is enforcing
     * remains unchanged.
     *
     * @param source the new character stream to replace substrings in
     * @return always returns {@code this}
     */
    @Override
    public ReplaceReader setSource(Reader source) {
        this.in = source;
        sourceBuffer = null;
        return this;
    }

    @Override
    public ReplaceReader setSource(CircularCharBuffer charBuffer) {
        this.sourceBuffer = charBuffer;
        this.in = null;
        return this;
    }

    @Override
    public void close() throws IOException {
        super.close();
        if (in != null) {
            in.close();
        }
    }

    @SuppressWarnings({"CloneDoesntDeclareCloneNotSupportedException"})
    @Override
    public abstract Object clone();

    @Override
    public String toString() {
        return "abstract ReplaceReader()";
    }
}
