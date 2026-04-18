package dark.leech.text.plugin.api;

/**
 * Repository exception with error classification and retry guidance. Enables intelligent error
 * handling and circuit breaking.
 */
public class RepositoryException extends Exception {

    private final ErrorType errorType;
    private final boolean retryable;
    private final long retryAfterMs;
    private final String repositoryId;

    public enum ErrorType {
        /** Network connectivity issues */
        NETWORK_ERROR,

        /** Parse/format errors */
        PARSE_ERROR,

        /** Validation failures */
        VALIDATION_ERROR,

        /** Security violations */
        SECURITY_VIOLATION,

        /** Rate limiting */
        RATE_LIMITED,

        /** Plugin not found */
        NOT_FOUND,

        /** Repository unavailable */
        SERVICE_UNAVAILABLE,

        /** Authentication failed */
        AUTHENTICATION_ERROR,

        /** Unknown/uncategorized error */
        UNKNOWN
    }

    public RepositoryException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
        this.retryable = determineRetryability(errorType);
        this.retryAfterMs = calculateRetryDelay(errorType);
        this.repositoryId = null;
    }

    public RepositoryException(String message, ErrorType errorType, String repositoryId) {
        super(message);
        this.errorType = errorType;
        this.retryable = determineRetryability(errorType);
        this.retryAfterMs = calculateRetryDelay(errorType);
        this.repositoryId = repositoryId;
    }

    public RepositoryException(String message, ErrorType errorType, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
        this.retryable = determineRetryability(errorType);
        this.retryAfterMs = calculateRetryDelay(errorType);
        this.repositoryId = null;
    }

    public RepositoryException(
            String message, ErrorType errorType, Throwable cause, String repositoryId) {
        super(message, cause);
        this.errorType = errorType;
        this.retryable = determineRetryability(errorType);
        this.retryAfterMs = calculateRetryDelay(errorType);
        this.repositoryId = repositoryId;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public long getRetryAfterMs() {
        return retryAfterMs;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    private static boolean determineRetryability(ErrorType type) {
        return switch (type) {
            case NETWORK_ERROR, SERVICE_UNAVAILABLE, RATE_LIMITED -> true;
            case PARSE_ERROR,
                    SECURITY_VIOLATION,
                    AUTHENTICATION_ERROR,
                    VALIDATION_ERROR,
                    NOT_FOUND,
                    UNKNOWN -> false;
        };
    }

    private static long calculateRetryDelay(ErrorType type) {
        return switch (type) {
            case RATE_LIMITED -> 60_000; // 1 minute
            case SERVICE_UNAVAILABLE -> 30_000; // 30 seconds
            case NETWORK_ERROR -> 5_000; // 5 seconds
            default -> 0;
        };
    }
}
