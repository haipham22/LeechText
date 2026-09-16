package dev.haipham22.leechtext.plugin.vbook.exception

/**
 * Exception khi vBook plugin operation fail (port từ exception/VBookPluginException.java) —
 * download, extraction, conversion. [code] để UI map stringResource theo locale
 * (engine không phụ thuộc resources — i18n nằm ở UI).
 */
class VBookPluginException : RuntimeException {
    /** Machine-readable code (vd "encrypted") — null khi là lỗi khác. */
    val code: String?

    constructor(message: String?, code: String? = null) : super(message) {
        this.code = code
    }

    constructor(message: String?, cause: Throwable?, code: String? = null) : super(message, cause) {
        this.code = code
    }
}

/**
 * Exception khi vBook registry operation fail (port từ exception/VBookRegistryException.java)
 * — registry fetch, parsing, validation.
 */
class VBookRegistryException : RuntimeException {
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
}
