/* $Id: FileAlreadyExistsException.java,v 1.6 2007/12/04 13:22:01 mke Exp $
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

import java.io.File;
import java.io.IOException;

/**
 * This exception is thrown when a method would overwrite
 * an existing file.
 */
public class FileAlreadyExistsException extends IOException {
    static final long serialVersionUID = 7818375828146091234L;

    private String filename;

    /**
     * Create an new instance of a FileAlreadyExistsException, referencing a given file
     *
     * @param filename The full path of the file which was about to be overwritten
     */
    public FileAlreadyExistsException(String filename) {
        super("File already exists: " + filename);
        this.filename = filename;
    }

    /**
     * Create an new instance of a FileAlreadyExistsException, referencing a given file
     *
     * @param file A file representing the full path of the file
     *             which was about to be overwritten
     */
    public FileAlreadyExistsException(File file) {
        super("File already exists: " + file);
    }

    /**
     * Filename as given to the constructor
     *
     * @return The filename as given to the constructor
     */
    public String getFilename() {
        return filename;
    }
}
