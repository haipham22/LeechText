package dev.haipham22.leechtext

import com.fleeksoft.ksoup.Ksoup
import org.jsoup.Jsoup
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * P4 ksoup feasibility gate (design doc "wasm seam risk"): jsoup không chạy wasm —
 Html.parse
 * cần ksoup (KMP port). Gate: cùng HTML + cùng selector plugin thật, output ksoup phải
 * khớp jsoup. PASS → wasm preview khả thi; FAIL → CUT wasm, QA screenshot-diff desktop.
 *
 * HTML/selector lấy từ pattern plugin thật (truyenfull-style: .list-chapter li a,
 * detail page metadata, [attr^=], nth-child).
 */
class KsoupGateTest {
    private val detailHtml = """
        <html><head><title>Thân Đạo Đan Tôn - Truyện full</title></head><body>
        <div class="book-info">
            <h1>Thân Đạo Đan Tôn</h1>
            <img src="https://img.example.com/cover.webp" alt="cover">
            <span class="author">Vô Nhị Đạo Nhân</span>
            <div class="description">Giới thiệu truyện <b>test</b> &amp; entities</div>
            <span class="status ongoing">Đang ra</span>
        </div>
        </body></html>
    """.trimIndent()

    private val tocHtml = """
        <html><body>
        <ul class="list-chapter">
            <li><a href="/chuong-1" title="Chương 1">Chương 1</a></li>
            <li><a href="/chuong-2" title="Chương 2">Chương 2: Hồi ức</a></li>
            <li><a href="/chuong-3" title="Chương 3">Chương 3</a></li>
        </ul>
        </body></html>
    """.trimIndent()

    private val chapterHtml = """
        <html><body>
        <div class="chapter">Nội dung chương 1 với <b>đậm</b> và <i>nhẹ</i>.</div>
        <div class="ads">quảng cáo nên lọc</div>
        <a href="https://cdn.example.com/page-2.jpg">trang 2</a>
        </body></html>
    """.trimIndent()

    private fun jsoupRows(
        html: String,
        selector: String,
    ): List<List<String>> = Jsoup.parse(html).select(selector).map { el ->
        listOf(el.tagName(), el.attr("href"), el.text(), el.ownText())
    }

    private fun ksoupRows(
        html: String,
        selector: String,
    ): List<List<String>> = Ksoup.parse(html).select(selector).map { el ->
        listOf(el.tagName(), el.attr("href"), el.text(), el.ownText())
    }

    @Test
    fun tocSelectorMatchesJsoup() {
        assertEquals(jsoupRows(tocHtml, ".list-chapter li a"), ksoupRows(tocHtml, ".list-chapter li a"))
    }

    @Test
    fun detailMetadataMatchesJsoup() {
        assertEquals(jsoupRows(detailHtml, ".book-info h1"), ksoupRows(detailHtml, ".book-info h1"))
        assertEquals(jsoupRows(detailHtml, ".author"), ksoupRows(detailHtml, ".author"))
        assertEquals(
            jsoupRows(detailHtml, "img[src^=https]"),
            ksoupRows(detailHtml, "img[src^=https]"),
        )
    }

    @Test
    fun chapterTextAndAttrStartsWithMatchesJsoup() {
        assertEquals(jsoupRows(chapterHtml, ".chapter"), ksoupRows(chapterHtml, ".chapter"))
        assertEquals(jsoupRows(chapterHtml, "a[href^=https://cdn]"), ksoupRows(chapterHtml, "a[href^=https]"))
    }

    @Test
    fun nthChildAndTextEntityMatchesJsoup() {
        assertEquals(
            jsoupRows(tocHtml, ".list-chapter li:nth-child(2) a"),
            ksoupRows(tocHtml, ".list-chapter li:nth-child(2) a"),
        )
        assertEquals(
            jsoupRows(detailHtml, ".description"),
            ksoupRows(detailHtml, ".description"),
        )
    }
}
