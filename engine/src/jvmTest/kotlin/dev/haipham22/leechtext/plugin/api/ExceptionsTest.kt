package dev.haipham22.leechtext.plugin.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ExceptionsTest {
    @Test
    fun repositoryExceptionRetryableAndDelayByErrorType() {
        val cases =
            mapOf(
                RepositoryException.ErrorType.NETWORK_ERROR to (true to 5_000L),
                RepositoryException.ErrorType.SERVICE_UNAVAILABLE to (true to 30_000L),
                RepositoryException.ErrorType.RATE_LIMITED to (true to 60_000L),
                RepositoryException.ErrorType.PARSE_ERROR to (false to 0L),
                RepositoryException.ErrorType.VALIDATION_ERROR to (false to 0L),
                RepositoryException.ErrorType.SECURITY_VIOLATION to (false to 0L),
                RepositoryException.ErrorType.NOT_FOUND to (false to 0L),
                RepositoryException.ErrorType.AUTHENTICATION_ERROR to (false to 0L),
                RepositoryException.ErrorType.UNKNOWN to (false to 0L),
            )
        for ((type, expected) in cases) {
            val e = RepositoryException("msg", type)
            assertEquals(expected.first, e.retryable, "retryable sai cho $type")
            assertEquals(expected.second, e.retryAfterMs, "retryAfterMs sai cho $type")
        }
    }

    @Test
    fun repositoryExceptionKeepsMessageCauseRepositoryId() {
        val cause = RuntimeException("root")
        val e = RepositoryException("msg", RepositoryException.ErrorType.NOT_FOUND, cause, "repo-1")

        assertEquals("msg", e.message)
        assertSame(cause, e.cause)
        assertEquals("repo-1", e.repositoryId)
        assertFalse(e.retryable)
        assertEquals(0L, e.retryAfterMs)
    }

    @Test
    fun repositoryExceptionDefaultsNullCauseAndRepositoryId() {
        val e = RepositoryException("m", RepositoryException.ErrorType.NETWORK_ERROR)
        assertNull(e.cause)
        assertNull(e.repositoryId)
        assertTrue(e.retryable)
    }

    @Test
    fun pluginFormatExceptionDefaultsUnknownAndNullFormatType() {
        val e = PluginFormatException("parse sai")
        assertEquals(PluginFormatException.ErrorType.UNKNOWN, e.errorType)
        assertNull(e.formatType)
        assertEquals("parse sai", e.message)
    }

    @Test
    fun pluginFormatExceptionKeepsCustomErrorTypeAndFormatType() {
        val e = PluginFormatException("thiếu field", PluginFormatException.ErrorType.MISSING_REQUIRED_FIELD, "vbook")
        assertEquals(PluginFormatException.ErrorType.MISSING_REQUIRED_FIELD, e.errorType)
        assertEquals("vbook", e.formatType)
    }
}
