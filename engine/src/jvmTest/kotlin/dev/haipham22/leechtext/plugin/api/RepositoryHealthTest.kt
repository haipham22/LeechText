package dev.haipham22.leechtext.plugin.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RepositoryHealthTest {
    @Test
    fun healthyFactorySetsHealthyStatus() {
        val h = RepositoryHealth.healthy()
        assertTrue(h.isHealthy())
        assertEquals(RepositoryHealth.Status.HEALTHY, h.status)
        assertEquals(0, h.failureCount)
        assertTrue(h.lastSuccessTime > 0)
        assertTrue(h.message.isNotEmpty())
    }

    @Test
    fun unhealthyFactoryKeepsReason() {
        val h = RepositoryHealth.unhealthy("timeout khi fetch")
        assertFalse(h.isHealthy())
        assertEquals(RepositoryHealth.Status.UNHEALTHY, h.status)
        assertEquals("timeout khi fetch", h.message)
        assertEquals(1, h.failureCount)
        assertEquals(0, h.lastSuccessTime)
    }

    @Test
    fun unhealthyWithNullReasonGivesEmptyMessage() {
        assertEquals("", RepositoryHealth.unhealthy(null).message)
    }

    @Test
    fun unknownFactory() {
        val h = RepositoryHealth.unknown()
        assertFalse(h.isHealthy())
        assertEquals(RepositoryHealth.Status.UNKNOWN, h.status)
        assertEquals(0, h.failureCount)
    }

    @Test
    fun isHealthyOnlyTrueForHealthyStatus() {
        for (status in RepositoryHealth.Status.entries.filter { it != RepositoryHealth.Status.HEALTHY }) {
            assertFalse(RepositoryHealth(status = status).isHealthy(), "$status không healthy")
        }
    }

    @Test
    fun repositoryMetadataDefaultValues() {
        val m = RepositoryMetadata()
        assertEquals("1.0.0", m.version)
        assertEquals(0, m.pluginCount)
        assertEquals("", m.name)
        assertTrue(m.lastUpdated > 0)
    }
}
