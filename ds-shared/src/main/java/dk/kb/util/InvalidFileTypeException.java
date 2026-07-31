package dk.kb.util;

import java.io.File;
import java.io.IOException;

/**
 * An {@link IOException} thrown by operations expecting a regular file but
 * finding a directory, or reverse, expecting a directory, but receiving
 * a regular file.
 */
public class InvalidFileTypeException extends IOException {
    static final long serialVersionUID = 7818375828146091235L;

    private final String filename;

    /**
     * Create an new instance of a FileAlreadyExistsException, referencing a given file
     *
     * @param filename the path of the file of the unexpected type
     * @param expected the file type expected by the party throwing the
     *                 exception
     */
    public InvalidFileTypeException(String filename, Files.Type expected) {
        super("File '" + filename + "' is a " + (expected == Files.Type.directory ?
                 Files.Type.file : Files.Type.directory) + ", expected a " + expected);
        this.filename = filename;
    }

    /**
     * Create an new instance of a FileAlreadyExistsException, referencing a given file
     *
     * @param file     A file object with the unexpected file type
     * @param expected the file type expected by the party throwing the
     *                 exception
     */
    public InvalidFileTypeException(File file, Files.Type expected) {
        this(file.toString(), expected);
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
