package dev.haipham22.leechtext.util

/**
 * Sửa HTML lỗi well-formed để EPUB validate được (port từ util/HtmlSanitizer.java).
 * Fixed heuristic — không phải HTML parser đầy đủ.
 */

private val INLINE_TAGS = arrayOf("strong", "em", "b", "i", "span", "a")

/** Sửa tag inline chưa đóng + loại tag đóng mồ côi. */
fun sanitizeHtml(html: String?): String? {
    if (html.isNullOrEmpty()) return html
    var result: String = html
    // XHTML cần `<br/>` tự đóng — raw `<br>` làm Apple Books/epubcheck báo lỗi
    result = result.replace(Regex("<br\\s*>", RegexOption.IGNORE_CASE), "<br/>")
    for (tag in INLINE_TAGS) result = fixUnclosedTags(result, tag)
    result = removeOrphanedClosingTags(result)
    return result
}

/** Tag `<p>`/`</p>` trong raw content — junk anti-scrape lồng không cân bằng
 * (truyenfull 260905: 4 mở/1 đóng → XHTML mismatch khi mở EPUB). Wrapper chuẩn
 * bọc `<p>...</p>` theo dòng ở khâu format, nên raw không cần giữ tag p nào. */
fun stripParagraphTags(content: String): String = content.replace(Regex("</?p(?:\\s[^>]*)?/?>", RegexOption.IGNORE_CASE), "")

/** `<p><strong>text` → `<p><strong>text</strong></p>` — đóng tag inline trước khi block đóng.
 * `(?=[\\s/>])` chặn `<b>` khớp nhầm `<br/>` (Cầu Ma C611 đậm lệch cuối chương, 260907). */
private fun fixUnclosedTags(
    html: String,
    tag: String,
): String {
    val pattern = Regex("<($tag)(?=[\\s/>])[^>]*>([^<]*?)</(p|div|li|h[1-6]|td)>", RegexOption.IGNORE_CASE)
    return pattern.replace(html, "<$1>$2</$1></$3>")
}

/** Bỏ tag đóng mồ côi ngay sau tag block mở. */
private fun removeOrphanedClosingTags(html: String): String {
    var result = html
    for (tag in INLINE_TAGS) {
        val pattern = Regex("<(p|div|li|h[1-6]|td)>\\s*</$tag>", RegexOption.IGNORE_CASE)
        result = pattern.replace(result, "<$1>")
    }
    return result
}

/** Kiểm tra sơ bộ tag balance. */
fun isValidHtml(html: String?): Boolean {
    if (html.isNullOrEmpty()) return true
    for (tag in arrayOf("strong", "em", "p", "div", "span", "a")) {
        if (countOccurrences(html, "<$tag") != countOccurrences(html, "</$tag>")) return false
    }
    return true
}

private fun countOccurrences(
    str: String,
    sub: String,
): Int {
    var count = 0
    var idx = 0
    while (str.indexOf(sub, idx).also { idx = it } != -1) {
        count++
        idx += sub.length
    }
    return count
}
