package dev.haipham22.leechtext.ui.reader

import kotlin.test.Test
import kotlin.test.assertEquals

/** Bước cuộn phím reader: ↑/↓ ⅛ viewport, Page/Space 90%, viewport lạ → 0. */
class ReaderScrollStepTest {
    @Test
    fun arrowIsEighthOfViewport() {
        assertEquals(120, readerScrollStep(960, page = false))
    }

    @Test
    fun pageIs90PercentViewport() {
        assertEquals(864, readerScrollStep(960, page = true))
    }

    @Test
    fun nonPositiveViewportIsNoScroll() {
        assertEquals(0, readerScrollStep(0, page = true))
        assertEquals(0, readerScrollStep(-5, page = false))
    }
}
