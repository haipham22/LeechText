package dev.haipham22.leechtext.plugin.js.api

/**
 * List type cho vBook compat (port từ JSList.java; P5.2c: composition thay extends
 * ArrayList — ArrayList final trên Kotlin/Native). Trả về từ JSElements.map(),
 * hỗ trợ array-like access từ JavaScript (get/length qua NativeJavaObject methods).
 */
@Suppress("TooManyFunctions") // API surface cho plugin JS — số hàm mirror đúng API vBook, không tách
class JSList(
    initialCapacity: Int = 16,
) {
    private val items: MutableList<Any?> = ArrayList(initialCapacity)

    val size: Int get() = items.size

    val length: Int get() = items.size

    fun isEmpty(): Boolean = items.isEmpty()

    fun add(element: Any?): Boolean = items.add(element)

    operator fun get(index: Int): Any? = if (index in 0 until size) items[index] else null

    fun toList(): List<Any?> = items.toList()

    fun forEach(callback: Any?) {
        if (JsValueBridge.isFunction(callback)) {
            for (i in 0 until size) {
                JsValueBridge.call(callback, listOf(get(i), i))
            }
        }
    }

    fun map(callback: Any?): JSList {
        val result = JSList(size)
        if (JsValueBridge.isFunction(callback)) {
            for (i in 0 until size) {
                result.add(JsValueBridge.call(callback, listOf(get(i), i)))
            }
        }
        return result
    }

    fun filter(predicate: Any?): JSList {
        val result = JSList()
        if (JsValueBridge.isFunction(predicate)) {
            for (i in 0 until size) {
                val item = get(i)
                val testResult = JsValueBridge.call(predicate, listOf(item, i))
                if (testResult is Boolean && testResult) result.add(item)
            }
        }
        return result
    }

    fun find(predicate: Any?): Any? {
        if (JsValueBridge.isFunction(predicate)) {
            for (i in 0 until size) {
                val item = get(i)
                val testResult = JsValueBridge.call(predicate, listOf(item, i))
                if (testResult is Boolean && testResult) return item
            }
        }
        return null
    }

    fun join(separator: String): String {
        val sb = StringBuilder()
        for (i in 0 until size) {
            if (i > 0) sb.append(separator)
            get(i)?.let { sb.append(JsValueBridge.toJsString(it) ?: "null") }
        }
        return sb.toString()
    }

    fun first(): Any? = if (isEmpty()) null else get(0)

    fun last(): Any? = if (isEmpty()) null else get(size - 1)
}
