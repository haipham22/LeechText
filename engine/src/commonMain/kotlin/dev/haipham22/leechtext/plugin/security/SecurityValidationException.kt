package dev.haipham22.leechtext.plugin.security

/** Exception khi security validation fail (port từ security/SecurityValidationException.java). */
class SecurityValidationException : Exception {
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
}

/** Exception khi cache capacity vượt limit (port từ security/CacheCapacityException.java). */
class CacheCapacityException(
    message: String?,
) : Exception(message)
