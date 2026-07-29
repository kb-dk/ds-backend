/* $Id: Checksums.java,v 1.6 2007/12/04 13:22:01 mke Exp $
 * $Revision: 1.6 $
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Checksums {

    private static final int bufferSize = 2048;

    /**
     * Calculate the checksum of a given {@link InputStream}.
     * The stream is guaranteed to be closed after ended operation.
     * If you need a  String version of the digest use {@link Bytes#toHex(byte[])}.
     *
     * @param algorithm the algorithm to use to compute the digest.
     *                  Possible values can be found in the <a href="http://java.sun.com/j2se/1.5.0/docs/guide/security/CryptoSpec.html">Java CryptoSpec</a>.
     * @param in        the stream to digest
     * @return the computed digest in a byte array
     * @throws IOException              if there is an error reading the input stream
     * @throws NoSuchAlgorithmException if the algorithm requested in {@code algorithm} isn't known to the jvm.
     */
    public static byte[] digest(String algorithm, InputStream in) throws IOException, NoSuchAlgorithmException {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);

            byte[] buffer = new byte[bufferSize];
            int count;
            while ((count = in.read(buffer)) > 0) {
                md.update(buffer, 0, count);
            }
            return md.digest();
        } finally {
            in.close();
        }
    }

    /**
     * Calculate the checksum of a byte array. The array will be read in one chunk. If you need a
     * String version of the digest use {@link Bytes#toHex(byte[])}.
     *
     * @param algorithm the algorithm to use to compute the digest. Possible values can be found in the
     *                 <a href="http://java.sun.com/j2se/1.5.0/docs/guide/security/CryptoSpec.html">Java CryptoSpec</a>.
     * @param in        array the byte array to compute the digest of.
     * @return the computed digest in a byte array.
     * @throws NoSuchAlgorithmException if the algorithm requested in {@code algorithm} isn't known to the jvm.
     */
    public static byte[] digest(String algorithm, byte[] in) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        md.update(in);
        return md.digest();
    }

    /**
     * Calculate the checksum of a given {@link String}. If you need a
     * String version of the digest use {@link Bytes#toHex(byte[])}.
     *
     * @param algorithm the algorithm to use, e.g. {@code SHA-1}.
     * @param in the input to digest.
     * @return the digested input.
     * @see #digest(String, java.io.InputStream)
     * @throws NoSuchAlgorithmException if the algorithm was unknown.
     */
    public static byte[] digest(String algorithm, String in) throws NoSuchAlgorithmException {
        return digest(algorithm, in.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Calculate the checksum of a given {@link File}. If you need a
     * String version of the digest use {@link Bytes#toHex(byte[])}.
     * @param algorithm the algorithm to use, e.g. {@code SHA-1}.
     * @param in the input to digest.
     * @return the digested input.
     * @see #digest(String, java.io.InputStream)
     * @throws NoSuchAlgorithmException if the algorithm was unknown.
     * @throws IOException if there is an error reading the stream
     */
    public static byte[] digest(String algorithm, File in) throws NoSuchAlgorithmException, IOException {
        return digest(algorithm, new BufferedInputStream(new FileInputStream(in)));
    }

    /**
     * Calculate the {@code SHA-1} checksum of a given stream. If you need a
     * String version of the digest use {@link Bytes#toHex(byte[])}.
     *
     * @param in the input to digest.
     * @return the digested input.
     * @throws IOException if there is an error reading the stream
     */
    public static byte[] sha1(InputStream in) throws IOException {
        try {
            return digest("SHA-1", in);
        } catch (NoSuchAlgorithmException e) {
            throw new DigestException("Unknown algorithm: SHA-1");
        }
    }

    /**
     * Calculate the {@code SHA-1} checksum of a string. If you need a
     * String version of the digest use {@link Bytes#toHex(byte[])}.
     *
     * @param in the input to digest.
     * @return the digested input.
     * @throws DigestException if the JVM doesn't know the {@code SHA-1} algorithm
     */
    public static byte[] sha1(String in) {
        try {
            return digest("SHA-1", in);
        } catch (NoSuchAlgorithmException e) {
            throw new DigestException("Unknown algorithm: SHA-1");
        }
    }

    /**
     * Calculate the {@code SHA-1} checksum of a {@link File}. If you need a
     * String version of the digest use {@link Bytes#toHex(byte[])}.
     *
     * @param in the input to digest.
     * @return the digested input.
     * @throws IOException     if there is an error reading the file.
     * @throws DigestException if the JVM doesn't know the {@code SHA-1} algorithm.
     */
    public static byte[] sha1(File in) throws IOException {
        try {
            return digest("SHA-1", in);
        } catch (NoSuchAlgorithmException e) {
            throw new DigestException("Unknown algorithm: SHA-1");
        }
    }

    /**
     * Calculate the {@code MD5} checksum of a given stream. If you need a
     * String version of the digest use {@link Bytes#toHex(byte[])}.
     *
     * @param in the input to digest.
     * @return the digested input.
     * @throws IOException     if there is an error reading the stream.
     * @throws DigestException if the JVM doesn't know the {@code MD5} algorithm.
     */
    public static byte[] md5(InputStream in) throws IOException {
        try {
            return digest("MD5", in);
        } catch (NoSuchAlgorithmException e) {
            throw new DigestException("Unknown algorithm: MD5");
        }
    }

    /**
     * Calculate the {@code MD5} checksum of a string. If you need a
     * String version of the digest use {@link Bytes#toHex(byte[])}.
     *
     * @param in the input to digest.
     * @return the digested input.
     * @throws DigestException if the JVM doesn't know the {@code MD5} algorithm
     */
    public static byte[] md5(String in) {
        try {
            return digest("MD5", in);
        } catch (NoSuchAlgorithmException e) {
            throw new DigestException("Unknown algorithm: MD5");
        }
    }

    /**
     * Calculate the {@code MD5} checksum of a {@link File}. If you need a
     * String version of the digest use {@link Bytes#toHex(byte[])}.
     *
     * @param in the input to digest.
     * @return the digested input.
     * @throws IOException     if there is an error reading the file
     * @throws DigestException if the JVM doesn't know the {@code MD5} algorithm
     */
    public static byte[] md5(File in) throws IOException {
        try {
            return digest("MD5", in);
        } catch (NoSuchAlgorithmException e) {
            throw new DigestException("Unknown algorithm: MD5");
        }
    }
}
