package dev.haipham22.leechtext.ui.reader

import kotlin.test.Test
import kotlin.test.assertEquals

/** Chia dòng theo trang (chế độ paged): greedy, không cắt đôi dòng, không trang rỗng. */
class ReaderPagingTest {
    @Test
    fun fitsInOnePage() {
        // 4 dòng × 10px = 40px < 100px → 1 trang
        assertEquals(listOf(0..3), splitLinesByPage(4, 10f, 100f, 0f))
    }

    @Test
    fun splitsAtBoundary() {
        // 10 dòng × 10px, trang 35px → 3 dòng/trang; dòng cuối lẻ một mình
        assertEquals(listOf(0..2, 3..5, 6..8, 9..9), splitLinesByPage(10, 10f, 35f, 0f))
    }

    @Test
    fun offsetConsumesFirstPage() {
        // offset 25px + 3 dòng = 55px ≤ 100px → 1 trang đủ, không vắt sang
        assertEquals(listOf(0..2), splitLinesByPage(3, 10f, 100f, 25f))
        // offset 85px → trang 1 chỉ 1 dòng, còn lại trang sau
        assertEquals(listOf(0..0, 1..2), splitLinesByPage(3, 10f, 100f, 85f))
    }

    @Test
    fun exactFitNoEmptyPage() {
        // 10 dòng vừa đúng 100px → 1 trang, không sinh trang rỗng kế
        assertEquals(listOf(0..9), splitLinesByPage(10, 10f, 100f, 0f))
    }

    @Test
    fun degenerateInputs() {
        // 0 dòng → 1 range rỗng; pageHeight ≤ 0 → tất cả 1 trang
        assertEquals(listOf(0..-1), splitLinesByPage(0, 10f, 100f, 0f))
        assertEquals(listOf(0..4), splitLinesByPage(5, 10f, 0f, 0f))
    }
}
