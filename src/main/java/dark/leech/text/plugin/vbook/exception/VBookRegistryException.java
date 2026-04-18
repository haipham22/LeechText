package dark.leech.text.plugin.vbook.exception;

/**
 * Exception thrown when vBook registry operations fail. Covers registry fetch, parsing, and
 * validation errors.
 */
public class VBookRegistryException extends RuntimeException {

    public VBookRegistryException(String message) {
        super(message);
    }

    public VBookRegistryException(String message, Throwable cause) {
        super(message, cause);
    }
}
