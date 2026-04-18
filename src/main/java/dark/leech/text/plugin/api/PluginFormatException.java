package dark.leech.text.plugin.api;

/** Exception thrown when plugin format parsing or validation fails. */
public class PluginFormatException extends Exception {

    private final String formatType;
    private final ErrorType errorType;

    public enum ErrorType {
        /** Invalid syntax or structure */
        PARSE_ERROR,

        /** Missing required fields */
        MISSING_REQUIRED_FIELD,

        /** Invalid data type for field */
        INVALID_DATA_TYPE,

        /** Script validation failed */
        SCRIPT_VALIDATION_FAILED,

        /** Regex pattern invalid */
        INVALID_REGEX,

        /** Format version not supported */
        UNSUPPORTED_VERSION,

        /** Unknown/uncategorized error */
        UNKNOWN
    }

    public PluginFormatException(String message) {
        super(message);
        this.formatType = null;
        this.errorType = ErrorType.UNKNOWN;
    }

    public PluginFormatException(String message, Throwable cause) {
        super(message, cause);
        this.formatType = null;
        this.errorType = ErrorType.UNKNOWN;
    }

    public PluginFormatException(String message, ErrorType errorType) {
        super(message);
        this.formatType = null;
        this.errorType = errorType;
    }

    public PluginFormatException(String message, ErrorType errorType, String formatType) {
        super(message);
        this.formatType = formatType;
        this.errorType = errorType;
    }

    public PluginFormatException(String message, ErrorType errorType, Throwable cause) {
        super(message, cause);
        this.formatType = null;
        this.errorType = errorType;
    }

    public PluginFormatException(
            String message, ErrorType errorType, String formatType, Throwable cause) {
        super(message, cause);
        this.formatType = formatType;
        this.errorType = errorType;
    }

    public String getFormatType() {
        return formatType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }
}
