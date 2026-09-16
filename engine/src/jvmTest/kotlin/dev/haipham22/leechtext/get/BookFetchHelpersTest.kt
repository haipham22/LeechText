package dev.haipham22.leechtext.get

import dev.haipham22.leechtext.models.Chapter
import dev.haipham22.leechtext.models.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Pure helpers của BookFetch: normalizeUrl, isValidHttpUrl, merge edge cases. */
class BookFetchHelpersTest {
    @Test
    fun normalizeUrlTrimsWhitespaceAndStripsTrailingSlash() {
        assertEquals("", null.normalizeUrl())
        assertEquals("", "   ".normalizeUrl())
        assertEquals("https://a.com/truyen", "  https://a.com/truyen/  ".normalizeUrl())
        assertEquals("https://a.com", "https://a.com//".normalizeUrl())
    }

    @Test
    fun isValidHttpUrlAcceptsOnlyHttpAndHttps() {
        assertFalse(null.isValidHttpUrl())
        assertFalse("ftp://a.com".isValidHttpUrl())
        assertFalse("a.com".isValidHttpUrl())
        assertTrue("http://a.com".isValidHttpUrl())
        assertTrue("https://a.com".isValidHttpUrl())
    }

    @Test
    fun mergeIntoEmptyBookAssignsIdsFromC0() {
        val p = Properties()
        val newCount =
            p.mergeFetchedChapters(
                listOf(
                    Chapter(url = "u1", chapName = "C1"),
                    Chapter(url = "u2", chapName = "C2"),
                ),
            )

        assertEquals(2, newCount)
        assertEquals(2, p.size)
        assertEquals(listOf("C0", "C1"), p.chapList!!.map { it.id })
    }

    @Test
    fun mergeSkipsFetchedChapterWithNullUrl() {
        val p =
            Properties().apply {
                chapList = listOf(Chapter(url = "u1", chapName = "C1", id = "C0"))
                size = 1
            }
        assertEquals(0, p.mergeFetchedChapters(listOf(Chapter(url = null, chapName = "rác"))))
        assertEquals(1, p.size)
    }
}
