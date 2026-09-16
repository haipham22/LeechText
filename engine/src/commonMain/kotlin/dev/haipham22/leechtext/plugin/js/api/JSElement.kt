package dev.haipham22.leechtext.plugin.js.api

import com.fleeksoft.ksoup.select.Elements

/**
 * Wrapper ksoup Element cho Rhino (port từ JSElement.java; P4: jsoup → ksoup).
 */
@Suppress("TooManyFunctions") // API surface cho plugin JS — số hàm mirror đúng API vBook, không tách
class JSElement(
    val element: com.fleeksoft.ksoup.nodes.Element?,
) {
    fun select(selector: String?): JSElements {
        return try {
            if (element == null) {
                return JSElements(Elements(arrayListOf()))
            }
            if (selector.isNullOrEmpty()) {
                return JSElements(Elements(arrayListOf()))
            }
            JSElements(element.select(selector))
        } catch (e: Exception) {
            JSElements(Elements(arrayListOf()))
        }
    }

    fun text(): String = element?.text() ?: ""

    fun ownText(): String = element?.ownText() ?: ""

    fun html(): String = element?.html() ?: ""

    fun outerHtml(): String = element?.outerHtml() ?: ""

    fun attr(key: String?): String {
        if (key.isNullOrEmpty()) return ""
        return element?.attr(key) ?: ""
    }

    fun attr(
        key: String?,
        value: String?,
    ): JSElement {
        if (element != null && !key.isNullOrEmpty()) element.attr(key, value ?: "")
        return this
    }

    fun hasClass(className: String?): Boolean {
        if (className.isNullOrEmpty()) return false
        return element?.hasClass(className) == true
    }

    fun tagName(): String = element?.tagName() ?: ""

    fun id(): String = element?.id() ?: ""

    fun className(): String = element?.className() ?: ""

    fun first(): JSElement? = element?.firstElementChild()?.let { JSElement(it) }

    fun last(): JSElement? = element?.lastElementChild()?.let { JSElement(it) }

    fun children(): JSElements = if (element == null) {
        JSElements(Elements(arrayListOf()))
    } else {
        JSElements(Elements(element.children()))
    }

    fun parent(): JSElement? = element?.parent()?.let { JSElement(it) }

    fun next(): JSElement? = element?.nextElementSibling()?.let { JSElement(it) }

    fun prev(): JSElement? = element?.previousElementSibling()?.let { JSElement(it) }

    fun isEmpty(): Boolean = element == null || element.children().isEmpty()

    fun childrenSize(): Int = element?.children()?.size ?: 0

    fun remove() {
        element?.remove()
    }

    fun remove(selector: String?): JSElement {
        // ksoup Elements không có remove() no-arg như jsoup — detach từng element
        if (element != null && !selector.isNullOrEmpty()) element.select(selector).forEach { it.remove() }
        return this
    }

    fun addClass(className: String?): JSElement {
        if (element != null && !className.isNullOrEmpty()) element.addClass(className)
        return this
    }

    fun removeClass(className: String?): JSElement {
        if (element != null && !className.isNullOrEmpty()) element.removeClass(className)
        return this
    }

    fun toggleClass(className: String?) {
        if (element != null && !className.isNullOrEmpty()) element.toggleClass(className)
    }
}
