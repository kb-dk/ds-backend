package dk.kb.util;

import java.io.File;
import java.io.IOException;

/**
 * An {@link IOException} thrown by operations requiring certain file
 * permissions rights to work when the required permissions are not present.
 */
public class FilePermissionException extends IOException {
    static final long serialVersionUID = 7818375828146091236L;

    private final String filename;

    /**
     * Create an new instance of a FilePermissionException, for a given file
     * and required, but missing, permission
     *
     * @param filename The full path of the file which has insufficient access
     *                 rights
     * @param required The required, but missing permission
     */
    public FilePermissionException(String filename, Files.Permission required) {
        super("Insufficient permissions for file '" + filename + "'. File is not " + required);
        this.filename = filename;
    }

    /**
     * Create an new instance of a FilePermissionException, referencing a given
     * file and required, but missing, permission
     *
     * @param file     A file object representing the file in question
     * @param required The required, but missing permission
     */
    public FilePermissionException(File file, Files.Permission required) {
        this(file.toString(), required);
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
