package dark.leech.text.plugin.security;

/** Exception thrown when security validation fails. */
public class SecurityValidationException extends Exception {

    public SecurityValidationException(String message) {
        super(message);
    }

    public SecurityValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
