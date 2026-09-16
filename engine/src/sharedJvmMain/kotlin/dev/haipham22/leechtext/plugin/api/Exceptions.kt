package dev.haipham22.leechtext.plugin.api

/**
 * Exception parse/validate plugin format fail (port từ api/PluginFormatException.java).
 */
class PluginFormatException(
    message: String?,
    val errorType: ErrorType = ErrorType.UNKNOWN,
    val formatType: String? = null,
) : Exception(message) {
    enum class ErrorType {
        PARSE_ERROR,
        MISSING_REQUIRED_FIELD,
        INVALID_DATA_TYPE,
        SCRIPT_VALIDATION_FAILED,
        INVALID_REGEX,
        UNSUPPORTED_VERSION,
        UNKNOWN,
    }
}

/**
 * Exception repository với error classification + retry guidance (port từ
 * api/RepositoryException.java) — enable intelligent error handling/circuit breaking.
 */
class RepositoryException(
    message: String?,
    val errorType: ErrorType,
    cause: Throwable? = null,
    val repositoryId: String? = null,
) : Exception(message, cause) {
    val retryable: Boolean = determineRetryability(errorType)
    val retryAfterMs: Long = calculateRetryDelay(errorType)

    enum class ErrorType {
        NETWORK_ERROR,
        PARSE_ERROR,
        VALIDATION_ERROR,
        SECURITY_VIOLATION,
        RATE_LIMITED,
        NOT_FOUND,
        SERVICE_UNAVAILABLE,
        AUTHENTICATION_ERROR,
        UNKNOWN,
    }

    companion object {
        private fun determineRetryability(type: ErrorType): Boolean = when (type) {
            ErrorType.NETWORK_ERROR, ErrorType.SERVICE_UNAVAILABLE, ErrorType.RATE_LIMITED -> {
                true
            }

            else -> {
                false
            }
        }

        private fun calculateRetryDelay(type: ErrorType): Long = when (type) {
            ErrorType.RATE_LIMITED -> 60_000L

            // 1 minute
            ErrorType.SERVICE_UNAVAILABLE -> 30_000L

            // 30 seconds
            ErrorType.NETWORK_ERROR -> 5_000L

            // 5 seconds
            else -> 0L
        }
    }
}
