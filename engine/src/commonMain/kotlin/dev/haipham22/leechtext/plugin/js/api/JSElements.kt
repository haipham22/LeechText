package dev.haipham22.leechtext.plugin.js.api

import com.fleeksoft.ksoup.select.Elements

/**
 * Wrapper ksoup Elements (collection) (port từ JSElements.java; P5.2c common —
 * callback JS qua JsValueBridge seam). [length] là public field cho vBook compat.
 */
@Suppress("TooManyFunctions") // API surface cho plugin JS — số hàm mirror đúng API vBook, không tách
class JSElements(
    val elements: Elements,
) {
    /** Public length property cho vBook compatibility. */
    var length: Int = elements.size

    constructor() : this(Elements(arrayListOf()))

    fun select(selector: String?): JSElements {
        return try {
            if (selector.isNullOrEmpty()) return JSElements()
            JSElements(elements.select(selector))
        } catch (e: Exception) {
            JSElements()
        }
    }

    fun text(): String = elements.text()

    fun html(): String = elements.html()

    fun outerHtml(): String = elements.outerHtml()

    /** Array-like access: links.get(0). */
    fun get(index: Int): JSElement? = if (index in 0 until elements.size) JSElement(elements[index]) else null

    fun eq(index: Int): JSElement? = get(index)

    fun first(): JSElement? = elements.first()?.let { JSElement(it) }

    fun last(): JSElement? = elements.last()?.let { JSElement(it) }

    fun size(): Int = elements.size

    fun isEmpty(): Boolean = elements.isEmpty()

    fun attr(key: String?): String {
        if (key.isNullOrEmpty()) return ""
        return elements.first()?.attr(key) ?: ""
    }

    /** ForEach — callback nhận (element, index). */
    fun forEach(callback: Any?) {
        if (JsValueBridge.isFunction(callback)) {
            for (i in 0 until elements.size) {
                JsValueBridge.call(callback, listOf(JSElement(elements[i]), i))
            }
        }
    }

    /** Each — jQuery style (index, element). */
    fun each(callback: Any?) {
        if (JsValueBridge.isFunction(callback)) {
            for (i in 0 until elements.size) {
                JsValueBridge.call(callback, listOf(i, JSElement(elements[i])))
            }
        }
    }

    /** Map — trả về JSList cho vBook compat. */
    fun map(callback: Any?): JSList {
        val results = JSList()
        if (JsValueBridge.isFunction(callback)) {
            for (i in 0 until elements.size) {
                results.add(JsValueBridge.call(callback, listOf(JSElement(elements[i]), i)))
            }
        }
        return results
    }

    fun toArray(): Array<Any?> {
        val array = ArrayList<Any?>(elements.size)
        for (el in elements) array.add(JSElement(el))
        return array.toTypedArray()
    }

    fun filter(predicate: Any?): JSElements {
        if (JsValueBridge.isFunction(predicate)) {
            val filtered = Elements(arrayListOf())
            for (i in 0 until elements.size) {
                val result = JsValueBridge.call(predicate, listOf(JSElement(elements[i]), i))
                if (result is Boolean && result) filtered.add(elements[i])
            }
            return JSElements(filtered)
        }
        return JSElements()
    }

    fun find(selector: String): JSElements = select(selector)

    fun children(): JSElements {
        val allChildren = Elements(arrayListOf())
        for (el in elements) allChildren.addAll(el.children())
        return JSElements(allChildren)
    }

    fun parents(): JSElements {
        val allParents = Elements(arrayListOf())
        for (el in elements) {
            val parent = el.parent()
            if (parent != null && !allParents.contains(parent)) allParents.add(parent)
        }
        return JSElements(allParents)
    }

    fun next(): JSElements {
        val allNext = Elements(arrayListOf())
        for (el in elements) el.nextElementSibling()?.let { allNext.add(it) }
        return JSElements(allNext)
    }

    fun prev(): JSElements {
        val allPrev = Elements(arrayListOf())
        for (el in elements) el.previousElementSibling()?.let { allPrev.add(it) }
        return JSElements(allPrev)
    }

    fun addClass(className: String?): JSElements {
        if (className != null) for (el in elements) el.addClass(className)
        return this
    }

    fun removeClass(className: String?): JSElements {
        if (className != null) for (el in elements) el.removeClass(className)
        return this
    }

    fun remove(): JSElements {
        for (el in elements) el.remove()
        return this
    }

    fun remove(selector: String?): JSElements {
        if (!selector.isNullOrEmpty()) elements.select(selector).forEach { it.remove() }
        return this
    }
}
