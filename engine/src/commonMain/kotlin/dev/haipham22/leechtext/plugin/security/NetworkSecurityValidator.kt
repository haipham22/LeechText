package dev.haipham22.leechtext.plugin.security

import dev.haipham22.leechtext.util.parseHost

/**
 * Validator network cho plugin downloads (port từ NetworkSecurityValidator.java; P5.2c
 * common — URL parse qua util.parseHost thay java.net.URL).
 * Enforce HTTPS, content-type, size limit.
 */
object NetworkSecurityValidator {
    private const val MAX_DOWNLOAD_SIZE_BYTES = 50L * 1024 * 1024 // 50MB
    private const val MAX_REDIRECTS = 3

    // Content types được phép cho plugin downloads
    private val ALLOWED_CONTENT_TYPES =
        arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")

    /** Validate URL cho plugin download — enforce HTTPS (trừ localhost). */
    @Throws(SecurityValidationException::class)
    @Suppress("ThrowsCount") // validation chain ở trust boundary — mỗi guard ném exception có message riêng cho UI
    fun validateUrl(urlString: String?) {
        if (urlString.isNullOrEmpty()) {
            throw SecurityValidationException("URL cannot be empty")
        }

        // scheme://... — require scheme hợp lệ
        if (!urlString.matches(Regex("^[A-Za-z][A-Za-z0-9+.-]*://.+"))) {
            throw SecurityValidationException("Invalid URL: $urlString")
        }

        val protocol = urlString.substringBefore("://").lowercase()
        val host = parseHost(urlString)

        // Enforce HTTPS cho remote URLs (trừ localhost)
        if (protocol != "https" && !isLocalhost(protocol, host)) {
            throw SecurityValidationException("Only HTTPS URLs are allowed for security (found: $protocol): $urlString")
        }
    }

    /** URL có trỏ localhost không (http/file scheme + host localhost/127.x). */
    private fun isLocalhost(
        protocol: String,
        host: String?,
    ): Boolean {
        if (protocol == "file") return true
        if (host == null) return false
        return host == "localhost" || host == "127.0.0.1" || host == "[::1]" || host.startsWith("127.")
    }

    /** Validate response headers cho plugin downloads. */
    @Throws(SecurityValidationException::class)
    fun validateResponse(
        contentType: String?,
        contentLength: Long,
    ) {
        // Check content type
        if (!contentType.isNullOrEmpty()) {
            val lowerContentType = contentType.lowercase()
            var typeAllowed = false
            for (allowedType in ALLOWED_CONTENT_TYPES) {
                if (lowerContentType.contains(allowedType.lowercase())) {
                    typeAllowed = true
                    break
                }
            }
            if (!typeAllowed) {
                throw SecurityValidationException("Invalid content type for plugin download: $contentType")
            }
        }

        // Check content length
        if (contentLength > MAX_DOWNLOAD_SIZE_BYTES) {
            throw SecurityValidationException(
                "Download too large: ${contentLength / (1024 * 1024)} MB" +
                    " (max: ${MAX_DOWNLOAD_SIZE_BYTES / (1024 * 1024)} MB)",
            )
        }
    }

    fun getMaxDownloadSize(): Long = MAX_DOWNLOAD_SIZE_BYTES

    fun getMaxRedirects(): Int = MAX_REDIRECTS
}
