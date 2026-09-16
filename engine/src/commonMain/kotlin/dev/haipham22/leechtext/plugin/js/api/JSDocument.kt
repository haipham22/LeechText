package dev.haipham22.leechtext.plugin.js.api

/**
 * Wrapper ksoup Document cho Rhino (port từ JSDocument.java; P4: jsoup → ksoup).
 */
class JSDocument(
    val document: com.fleeksoft.ksoup.nodes.Document?,
) {
    fun select(selector: String?): JSElements {
        return try {
            if (document == null) return JSElements()
            if (selector.isNullOrEmpty()) return JSElements()
            JSElements(document.select(selector))
        } catch (e: Exception) {
            JSElements()
        }
    }

    fun text(): String = document?.text() ?: ""

    fun html(): String = document?.html() ?: ""

    fun title(): String = document?.title() ?: ""

    fun body(): JSElement? = document?.body()?.let { JSElement(it) }

    fun head(): JSElement? = document?.head()?.let { JSElement(it) }

    fun getElementById(id: String?): JSElement? {
        if (id.isNullOrEmpty()) return null
        return document?.getElementById(id)?.let { JSElement(it) }
    }
}
