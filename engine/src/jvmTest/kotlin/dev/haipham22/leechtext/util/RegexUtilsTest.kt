package dev.haipham22.leechtext.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RegexUtilsTest {
    @Test
    fun regexFindReturnsFirstMatchingGroup() {
        assertEquals("50", regexFind("50% done", "(^\\d+)%", 1))
    }

    @Test
    fun regexFindGroup0ReturnsWholeMatch() {
        assertEquals("50%", regexFind("progress 50% ok", "\\d+%", 0))
    }

    @Test
    fun regexFindReturnsNullWhenNoMatch() {
        assertNull(regexFind("no digits here", "(^\\d+)%", 1))
    }

    @Test
    fun regexFindReturnsNullOnInvalidRegex() {
        assertNull(regexFind("abc", "([", 1))
    }
}
