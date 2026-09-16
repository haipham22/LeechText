package dev.haipham22.leechtext.plugin.js.api

import com.fleeksoft.ksoup.Ksoup
import dev.haipham22.leechtext.EngineConfig
import dev.haipham22.leechtext.log.EngineLogger
import dev.haipham22.leechtext.util.decodeBytes
import dev.haipham22.leechtext.util.encodeBytes

/**
 * HTML API cho JS plugins (port từ Html.java; P5.2c common). parse() trả JSDocument;
 * input null/empty → empty document (vBook compat). parseUrl fetch qua HttpEngine seam.
 */
class Html(
    private val log: EngineLogger,
) {
    fun parse(html: String?): JSDocument {
        if (html.isNullOrEmpty()) return JSDocument(EMPTY_DOC)
        return JSDocument(Ksoup.parse(html, ""))
    }

    fun parseUrl(url: String?): JSDocument {
        if (url.isNullOrEmpty()) return JSDocument(EMPTY_DOC)
        return try {
            // Fetch (share HttpEngine) → ksoup parse. baseUri = URL cuối sau redirect cho
            // selector abs:. Non-2xx → EMPTY_DOC như jsoup HttpStatusException cũ
            val result =
                httpExecute(
                    HttpSpec(
                        method = "GET",
                        url = url,
                        headers = mapOf("User-Agent" to EngineConfig.USER_AGENT),
                    ),
                    log,
                )
            if (result.code !in 200..399) {
                error("HTTP ${result.code} từ $url")
            }
            val finalUrl = result.headers["X-Final-Url"] ?: url
            JSDocument(Ksoup.parse(result.body.decodeToString(), finalUrl))
        } catch (e: Exception) {
            log.add("Failed to parse HTML from URL: ${e.message}")
            JSDocument(EMPTY_DOC)
        }
    }

    fun urlEncode(url: String): String = urlEncode(url, "UTF-8")

    fun urlEncode(
        url: String,
        charset: String,
    ): String = try {
        encodeWithCharset(url, charset)
    } catch (e: Exception) {
        log.add("URL encoding failed: ${e.message}")
        url
    }

    fun urlDecode(url: String): String = urlDecode(url, "UTF-8")

    fun urlDecode(
        url: String,
        charset: String,
    ): String = try {
        decodeWithCharset(url, charset)
    } catch (e: Exception) {
        log.add("URL decoding failed: ${e.message}")
        url
    }

    /** Parse + select một lượt: html.select("<html>...</html>", "div.class"). */
    fun select(
        html: String?,
        selector: String,
    ): JSElements = parse(html).select(selector)

    /** Encode form-style theo tên charset (charset không hợp lệ → nguyên input). */
    private fun encodeWithCharset(
        text: String,
        charset: String,
    ): String {
        val bytes = encodeBytes(text, charset) ?: return text
        return urlFormEncode(bytes)
    }

    private fun decodeWithCharset(
        text: String,
        charset: String,
    ): String {
        val bytes = formDecodeToBytes(text)
        return decodeBytes(bytes, charset) ?: text
    }

    companion object {
        private val EMPTY_DOC: com.fleeksoft.ksoup.nodes.Document = Ksoup.parse("<html><body></body></html>", "")

        /** Clean HTML bỏ các tag: Html.clean("<html>...</html>", ["script", ".ads"]). */
        fun clean(
            html: String?,
            tags: Array<Any?>?,
        ): String? {
            if (html.isNullOrEmpty()) return html
            val doc = Ksoup.parse(html, "")
            tags?.asSequence()
                ?.mapNotNull { JsValueBridge.toJsString(it) }
                ?.filter { it.isNotEmpty() }
                ?.forEach { doc.select(it).remove() }
            return doc.body().html()
        }

        fun wrapElement(element: com.fleeksoft.ksoup.nodes.Element?): JSElement? = element?.let { JSElement(it) }

        fun wrapElements(elements: com.fleeksoft.ksoup.select.Elements): JSElements = JSElements(elements)
    }
}
