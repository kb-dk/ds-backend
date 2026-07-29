package dk.kb.util.yaml;

import dk.kb.util.yaml.YAMLException;

/**
 * Thrown when attempting to lookup a path that does not exist in the YAML
 */
public class NotFoundException extends YAMLException {
    
    
    private static final long serialVersionUID = 8017942512809154887L;
    
    public NotFoundException(String message, String path) {
        super(message, path);
    }
    
    public NotFoundException(String message, String path, Throwable cause) {
        super(message, path, cause);
    }
    
    public NotFoundException(Throwable cause, String path) {
        super(cause, path);
    }
    
    public NotFoundException(String message,
                             String path,
                             Throwable cause,
                             boolean enableSuppression,
                             boolean writableStackTrace) {
        super(message, path, cause, enableSuppression, writableStackTrace);
    }
}
