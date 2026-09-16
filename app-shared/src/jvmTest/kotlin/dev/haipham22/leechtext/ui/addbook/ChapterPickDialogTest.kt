package dev.haipham22.leechtext.ui.addbook

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Unit test parseRanges trong ChapterPickDialog — pure logic, không UI. */
class ChapterPickDialogTest {
    @Test
    fun basicRangeAndSingle() {
        assertEquals(listOf(2, 3, 4, 9), parseRanges("3-5,10", 100))
    }

    @Test
    fun blankInputReturnsNull() {
        assertNull(parseRanges("", 100))
        assertNull(parseRanges("   ", 100))
    }

    @Test
    fun clampBeyondTotal() {
        assertEquals((0..9).toList(), parseRanges("1-100", 10))
        assertEquals(emptyList(), parseRanges("999", 10))
    }

    @Test
    fun dedupeAndSort() {
        assertEquals(listOf(2, 4), parseRanges("5,5,3", 100))
        assertEquals(listOf(0, 1, 9), parseRanges("10,1-2", 100))
    }

    @Test
    fun garbageTokensIgnored() {
        assertEquals(listOf(6), parseRanges("abc,7,x-", 100))
        assertEquals(emptyList(), parseRanges("abc", 100))
    }

    @Test
    fun reversedRangeSwapped() {
        assertEquals(listOf(2, 3, 4), parseRanges("5-3", 100))
    }

    @Test
    fun index0Invalid() {
        assertEquals(emptyList(), parseRanges("0", 100))
    }

    @Test
    fun hugeNumberIgnoredNoCrash() {
        // >Int.MAX_VALUE từng bị NumberFormatException crash app
        assertEquals(emptyList(), parseRanges("99999999999", 10))
        assertEquals(listOf(2), parseRanges("3,99999999999", 10))
    }

    @Test
    fun hugeRangeClippedToTotal() {
        // không loop hàng tỷ iteration — clip biên theo total trước khi chạy
        assertEquals((0..9).toList(), parseRanges("1-99999999999", 10))
        assertEquals((5..9).toList(), parseRanges("6-99999999999", 10))
    }

    @Test
    fun compressToRangeBasic() {
        assertEquals("", compressToRange(emptyList()))
        assertEquals("1", compressToRange(listOf(0)))
        assertEquals("1-5,8", compressToRange(listOf(0, 1, 2, 3, 4, 7)))
    }

    @Test
    fun rangeRoundTrip() {
        // sync 2 chiều dialog: tick checkbox → compressToRange → gõ lại → parseRanges
        // phải ra lại đúng selection ban đầu
        val indices = listOf(0, 2, 4, 9, 10, 11, 29)
        assertEquals(indices, parseRanges(compressToRange(indices), 100))
        assertEquals((0..99).toList(), parseRanges(compressToRange((0..99).toList()), 100))
    }
}
