package dark.leech.text.plugin.vbook.exception;

/**
 * Exception thrown when vBook plugin operations fail. Covers plugin download, extraction, and
 * conversion errors.
 */
public class VBookPluginException extends RuntimeException {

    public VBookPluginException(String message) {
        super(message);
    }

    public VBookPluginException(String message, Throwable cause) {
        super(message, cause);
    }
}
