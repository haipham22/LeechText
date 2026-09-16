package dev.haipham22.leechtext.ui.reader

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Auto-scroll: bước px/frame theo dt thực (độc lập tần số quét) + điều kiện auto-advance. */
class ReaderAutoScrollTest {
    @Test
    fun stepScalesWithFrameDelta() {
        // 60Hz (16.67ms) → speed px/frame; 30ms (màn hình chậm hơn) → gấp đôi px, cùng tốc px/s
        assertEquals(3f, autoScrollStep(3, 16_666_667L))
        assertEquals(6f, autoScrollStep(3, 33_333_334L))
    }

    @Test
    fun stepClampsSpeedRange() {
        assertEquals(10f, autoScrollStep(99, 16_666_667L))
        assertEquals(1f, autoScrollStep(0, 16_666_667L))
    }

    @Test
    fun stepIgnoresFirstFrame() {
        assertEquals(0f, autoScrollStep(3, 0L))
    }

    @Test
    fun advanceOnlyAtListEndWithNextChapter() {
        assertTrue(shouldAutoAdvance(canScrollForward = false, idx = 2, total = 5))
        assertFalse(shouldAutoAdvance(canScrollForward = true, idx = 2, total = 5))
        assertFalse(shouldAutoAdvance(canScrollForward = false, idx = 4, total = 5))
    }
}
