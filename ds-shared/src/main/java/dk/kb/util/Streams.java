/* $Id: Streams.java,v 1.3 2007/12/04 13:22:01 mke Exp $
 * $Revision: 1.3 $
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
package dk.kb.util;

import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Utility methods for handling streams
 */
public class Streams {

    @Deprecated
    public static void pipeStream(InputStream in, OutputStream out, int bufSize) throws IOException {
        pipe(in, out, bufSize);
    }

    @Deprecated
    public static void pipeStream(InputStream in, OutputStream out) throws IOException {
        pipe(in, out);
    }

    /**
     * Copies the contents of an InputStream to an OutputStream, then closes
     * both.
     *
     * @param in      The source stream.
     * @param out     The destination stram.
     * @param bufSize Number of bytes to attempt to copy at a time.
     * @throws java.io.IOException If any sort of read/write error occurs on
     *                             either stream.
     */
    public static void pipe(InputStream in, OutputStream out, int bufSize) throws IOException {
        try {
            byte[] buf = new byte[bufSize];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } finally {
            in.close();
            out.close();
        }
    }

    /**
     * Shorthand for
     * {@link #pipe(java.io.InputStream, java.io.OutputStream, int)}.
     * A default buffer of 4KB is used.
     *
     * @param in  The source stream.
     * @param out The target stream.
     * @throws java.io.IOException If any sort of read/write error occurs on
     *                             either stream.
     */
    public static void pipe(InputStream in, OutputStream out)
            throws IOException {
        pipe(in, out, 4096);
    }

    /**
     * Uses the current thread's context class loader to fetch a resource.
     *
     * @param name the identifier for a resource in the classpath.
     * @return the resource as a String, converted from bytes with UTF-8.
     * @throws FileNotFoundException if the resource could not be located.
     * @throws IOException           if the resource could not be fetched.
     */
    public static String getUTF8Resource(String name) throws IOException {
        URL url =
                Thread.currentThread().getContextClassLoader().getResource(name);
        if (url == null) {
            throw new FileNotFoundException("Could not locate '" + name + "' in the class path");
        }
        InputStream in = url.openStream();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(1000);
        pipe(in, bytes);
        return bytes.toString(StandardCharsets.UTF_8);
    }


    /**
     * @param in the stream to read a long from.
     * @return the next long in the stream if present. If EOF was reached
     *         during read, an exception is thrown. The long must be stored
     *         in big-endian format.
     * @throws IOException  if a fatal error occured during read.
     * @throws EOFException if EOF was reached during read.
     */
    public static long readLong(InputStream in) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(8);
        for (int i = 0; i < 8; i++) {
            int value = in.read();
            if (value == -1) { // -1 is defined as EOF in InputStream
                throw new EOFException("Attempting to read past EOF");
            }
            bb.put((byte) value);
        }
        return bb.getLong(0);
    }
}
