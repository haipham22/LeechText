package dev.haipham22.leechtext.ui.queue

import dev.haipham22.leechtext.resources.Res
import dev.haipham22.leechtext.resources.queue_status_cancelled
import dev.haipham22.leechtext.resources.queue_status_paused
import dev.haipham22.leechtext.ui.model.UiText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val BOOK_URL = "https://example.com/book1"

/** Unit test cho DownloadQueueState — pure state transitions (không network). */
class DownloadQueueStateTest {
    @Test
    fun pauseSetsPausedStatusAndNullsJob() {
        val state = DownloadQueueState(
            dev.haipham22.leechtext.testComponentContext(),
            log = dev.haipham22.leechtext.log.platformEngineLogger(),
            pluginManager = dev.haipham22.leechtext.plugin.PluginManager(dev.haipham22.leechtext.log.platformEngineLogger()),
        )
        state.enqueue(listOf(BOOK_URL))
        val item = state.items.first()

        state.pause(item)

        assertTrue(item.paused, "paused should be true after pause()")
        assertEquals(UiText.Res(Res.string.queue_status_paused), item.status, "status should be paused")
        assertNull(item.job, "job should be null after pause()")
    }

    @Test
    fun resumeClearsPaused() {
        val state = DownloadQueueState(
            dev.haipham22.leechtext.testComponentContext(),
            log = dev.haipham22.leechtext.log.platformEngineLogger(),
            pluginManager = dev.haipham22.leechtext.plugin.PluginManager(dev.haipham22.leechtext.log.platformEngineLogger()),
        )
        state.enqueue(listOf(BOOK_URL))
        val item = state.items.first()

        state.pause(item)
        assertTrue(item.paused, "paused should be true after pause()")

        state.resume(item)
        assertFalse(item.paused, "paused should be false after resume()")
        assertNull(item.job, "job should be null after resume()")
    }

    @Test
    fun enqueueThenPauseDoesNotCrash() {
        val state = DownloadQueueState(
            dev.haipham22.leechtext.testComponentContext(),
            log = dev.haipham22.leechtext.log.platformEngineLogger(),
            pluginManager = dev.haipham22.leechtext.plugin.PluginManager(dev.haipham22.leechtext.log.platformEngineLogger()),
        )
        state.enqueue(listOf(BOOK_URL))
        val item = state.items.first()

        // Should not crash even though pump() might try to start a job
        state.pause(item)

        assertTrue(item.paused)
        assertEquals(UiText.Res(Res.string.queue_status_paused), item.status)
    }

    @Test
    fun cancelWhenPausedDoesNotSetDone() {
        val state = DownloadQueueState(
            dev.haipham22.leechtext.testComponentContext(),
            log = dev.haipham22.leechtext.log.platformEngineLogger(),
            pluginManager = dev.haipham22.leechtext.plugin.PluginManager(dev.haipham22.leechtext.log.platformEngineLogger()),
        )
        state.enqueue(listOf(BOOK_URL))
        val item = state.items.first()

        state.pause(item)
        state.cancel(item)

        assertTrue(item.paused, "paused should remain true")
        assertEquals(UiText.Res(Res.string.queue_status_paused), item.status, "status should remain paused")
        assertFalse(item.done, "done should NOT be set when paused item is cancelled")
    }

    @Test
    fun cancelWhenNotPausedSetsDone() {
        val state = DownloadQueueState(
            dev.haipham22.leechtext.testComponentContext(),
            log = dev.haipham22.leechtext.log.platformEngineLogger(),
            pluginManager = dev.haipham22.leechtext.plugin.PluginManager(dev.haipham22.leechtext.log.platformEngineLogger()),
        )
        state.enqueue(listOf(BOOK_URL))
        val item = state.items.first()

        state.cancel(item)

        assertFalse(item.paused, "paused should be false")
        assertEquals(UiText.Res(Res.string.queue_status_cancelled), item.status, "status should be cancelled")
        assertTrue(item.done, "done should be true when not paused item is cancelled")
    }

    @Test
    fun pauseThenResumePumpDoesNotPickUpPausedItem() {
        val state = DownloadQueueState(
            dev.haipham22.leechtext.testComponentContext(),
            log = dev.haipham22.leechtext.log.platformEngineLogger(),
            pluginManager = dev.haipham22.leechtext.plugin.PluginManager(dev.haipham22.leechtext.log.platformEngineLogger()),
        )
        state.enqueue(listOf(BOOK_URL))
        val item = state.items.first()

        state.pause(item)

        // Simulate pump() logic - should skip paused items
        val next = state.items.firstOrNull { !it.done && it.job == null && !it.paused }
        assertNull(next, "pump() should not pick up paused item")

        state.resume(item)

        // After resume, pump() could pick it up again
        val nextAfterResume = state.items.firstOrNull { !it.done && it.job == null && !it.paused }
        assertNotNull(nextAfterResume, "pump() should pick up item after resume()")
    }

    @Test
    fun enqueueSameUrlAfterDoneCreatesNewItem() {
        // Item done chặn re-enqueue cùng URL (dogfood 260906): tải xong muốn tải
        // thêm chương khác phải 🗑 clear queue trước — dedup giờ chỉ tính item chưa done
        val state = DownloadQueueState(
            dev.haipham22.leechtext.testComponentContext(),
            log = dev.haipham22.leechtext.log.platformEngineLogger(),
            pluginManager = dev.haipham22.leechtext.plugin.PluginManager(dev.haipham22.leechtext.log.platformEngineLogger()),
        )
        state.enqueue(listOf(BOOK_URL))
        state.items.first().done = true

        state.enqueue(listOf(BOOK_URL))

        assertEquals(2, state.items.size, "done item should NOT block re-enqueue of same URL")
    }

    @Test
    fun enqueueSameUrlWhileRunningStillDeduped() {
        val state = DownloadQueueState(
            dev.haipham22.leechtext.testComponentContext(),
            log = dev.haipham22.leechtext.log.platformEngineLogger(),
            pluginManager = dev.haipham22.leechtext.plugin.PluginManager(dev.haipham22.leechtext.log.platformEngineLogger()),
        )
        state.enqueue(listOf(BOOK_URL))

        state.enqueue(listOf(BOOK_URL))

        assertEquals(1, state.items.size, "non-done duplicate should still be deduped")
    }
}
