package dev.haipham22.leechtext.util

import kotlin.test.Test
import kotlin.test.assertEquals

class SyntaxUtilsTest {
    @Test
    fun normalizeVietnameseConvertsNfdToNfc() {
        // chỉ tone-mark NFD có trong bảng: base + U+0300/U+0301/U+0323...
        assertEquals("à", normalizeVietnamese("a" + "̀"))
        assertEquals("é", normalizeVietnamese("e" + "́"))
        assertEquals("ọ", normalizeVietnamese("o" + "̣"))
        assertEquals("ứ", normalizeVietnamese("ư" + "́"))
    }

    @Test
    fun removeDiacriticsStripsVietnameseDiacritics() {
        assertEquals("Truong Sa Hoang Sa", removeDiacritics("Trường Sa Hoàng Sa"))
    }

    @Test
    fun removeDiacriticsHandlesUppercaseAndD() {
        assertEquals("Do Dang cap", removeDiacritics("Đỗ Đăng cấp"))
    }

    @Test
    fun fixNameCollapsesZeroPaddedChapterNumbers() {
        assertEquals("Chương 1", fixName("Chương 01"))
        assertEquals("Chương 12", fixName("Chương 0012"))
        // 2 space + 3 số 0: chỉ strip 1 space + 2 số 0 (behavior bản gốc)
        assertEquals("Chương  12", fixName("Chương  0012"))
    }

    @Test
    fun fixNameCollapsesRepeatedSeparatorsToSingle() {
        assertEquals("Chương 1: Mở. đầu", fixName("Chương 1::: Mở... đầu"))
    }
}
