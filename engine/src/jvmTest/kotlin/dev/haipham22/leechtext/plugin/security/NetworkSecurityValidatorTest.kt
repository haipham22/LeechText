package dev.haipham22.leechtext.plugin.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private const val CONTENT_TYPE_ZIP = "application/zip"

class NetworkSecurityValidatorTest {
    @Test
    fun httpsUrlIsValid() {
        NetworkSecurityValidator.validateUrl("https://example.com/plugin.zip") // không throw
        assertEquals("example.com", dev.haipham22.leechtext.util.parseHost("https://example.com/plugin.zip"))
    }

    @Test
    fun remoteHttpUrlIsBlocked() {
        assertFailsWith<SecurityValidationException> {
            NetworkSecurityValidator.validateUrl("http://example.com/plugin.zip")
        }
    }

    @Test
    fun httpLocalhostIsAllowed() {
        NetworkSecurityValidator.validateUrl("http://localhost:8080/plugin.zip")
        NetworkSecurityValidator.validateUrl("http://127.0.0.1/plugin.zip")
        NetworkSecurityValidator.validateUrl("http://127.0.0.2/plugin.zip")
    }

    @Test
    fun fileUrlIsAllowed() {
        NetworkSecurityValidator.validateUrl("file:///tmp/plugin.zip") // không throw
    }

    @Test
    fun emptyOrNullUrlIsBlocked() {
        assertFailsWith<SecurityValidationException> { NetworkSecurityValidator.validateUrl("") }
        assertFailsWith<SecurityValidationException> { NetworkSecurityValidator.validateUrl(null) }
    }

    @Test
    fun malformedUrlIsBlocked() {
        assertFailsWith<SecurityValidationException> {
            NetworkSecurityValidator.validateUrl("not-a-url")
        }
    }

    @Test
    fun validateResponseAllowsZipContentTypes() {
        listOf(CONTENT_TYPE_ZIP, "application/x-zip-compressed", "application/octet-stream")
            .forEach {
                NetworkSecurityValidator.validateResponse(it, 1024) // không throw
            }
        assertTrue(true, "các content type zip phải pass")
    }

    @Test
    fun validateResponseBlocksUnexpectedContentType() {
        assertFailsWith<SecurityValidationException> {
            NetworkSecurityValidator.validateResponse("text/html", 1024)
        }
    }

    @Test
    fun validateResponseAllowsNullContentType() {
        NetworkSecurityValidator.validateResponse(null, 1024) // không throw
    }

    @Test
    fun validateResponseBlocksOver50MB() {
        val max = NetworkSecurityValidator.getMaxDownloadSize()
        assertFailsWith<SecurityValidationException> {
            NetworkSecurityValidator.validateResponse(CONTENT_TYPE_ZIP, max + 1)
        }
        // Đúng max vẫn pass
        NetworkSecurityValidator.validateResponse(CONTENT_TYPE_ZIP, max)
    }

    @Test
    fun limitConstants() {
        assertEquals(50L * 1024 * 1024, NetworkSecurityValidator.getMaxDownloadSize())
        assertEquals(3, NetworkSecurityValidator.getMaxRedirects())
    }
}
