package dev.haipham22.leechtext.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HtmlSanitizerTest {
    @Test
    fun sanitizeHtmlClosesUnclosedInlineTagBeforeBlockClose() {
        assertEquals(
            "<p><strong>text</strong></p>",
            sanitizeHtml("<p><strong>text</p>"),
        )
    }

    @Test
    fun sanitizeHtmlDoesNotTurnBrIntoBold() {
        // Cầu Ma C611 (260907): `<b>` pattern cũ khớp `<br/>` → đậm lệch cuối chương
        assertEquals(
            "<p>a<br/>b</p>",
            sanitizeHtml("<p>a<br/>b</p>"),
        )
    }

    @Test
    fun sanitizeHtmlRemovesOrphanedClosingTag() {
        assertEquals("<p>text</p>", sanitizeHtml("<p></strong>text</p>"))
    }

    @Test
    fun sanitizeHtmlLeavesWellFormedHtmlUntouched() {
        val html = "<p><strong>a</strong><em>b</em></p>"
        assertEquals(html, sanitizeHtml(html))
    }

    @Test
    fun stripParagraphTagsRemovesBalancedAndUnbalancedPTags() {
        // Junk anti-scrape truyenfull 260905: 4 mở / 1 đóng lệch nhau
        assertEquals(
            "xy",
            stripParagraphTags("<p>x<p style=\"display:none\"><p><p hidden>y</p>"),
        )
    }

    @Test
    fun sanitizeHtmlConvertsUnclosedBrToSelfClosing() {
        assertEquals("<br/>line", sanitizeHtml("<br>line"))
    }

    @Test
    fun sanitizeHtmlHandlesNullAndEmpty() {
        assertEquals(null, sanitizeHtml(null))
        assertEquals("", sanitizeHtml(""))
    }

    @Test
    fun isValidHtmlDetectsImbalance() {
        assertTrue(isValidHtml("<p><strong>a</strong></p>"))
        assertFalse(isValidHtml("<p><strong>a</p>"))
    }
}
