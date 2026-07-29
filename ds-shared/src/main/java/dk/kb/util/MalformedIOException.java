package dk.kb.util;

import java.util.Locale;

public class MalformedIOException extends Exception {
    public MalformedIOException() {
    }

    public MalformedIOException(String message) {
        super(message);
    }

    public MalformedIOException(String message, Object... vals) {
        super(String.format(Locale.ROOT, message, vals));
    }

    public MalformedIOException(String message, Throwable cause) {
        super(message, cause);
    }

    public MalformedIOException(Throwable cause) {
        super(cause);
    }
}
