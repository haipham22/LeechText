package dev.haipham22.leechtext.plugin.js.loader

import dev.haipham22.leechtext.plugin.js.api.JsValueBridge

/**
 * Converter giá trị JS → Kotlin (port từ JSResponse.java; P5.2c: làm việc trên giá trị
 * normalized Map/List từ JsSandbox.callFunction — engine array (NativeArray trên JVM)
 * đọc qua JsValueBridge). Contract bridge: [JsValueBridge.sizeOf] trả -1 với non-array.
 */
object JSResponse {
    fun convertObject(obj: Any?): MutableMap<String, Any?> {
        if (obj !is Map<*, *>) return HashMap()
        val map = LinkedHashMap<String, Any?>()
        for (key in obj.keys) {
            map[key.toString()] = convertValue(obj[key])
        }
        return map
    }

    fun convertArray(array: Any?): List<Any?> {
        val size = JsValueBridge.sizeOf(array)
        if (size <= 0) return emptyList()
        return List(size) { i -> convertValue(JsValueBridge.itemAt(array, i)) }
    }

    fun convertValue(value: Any?): Any? = when {
        JsValueBridge.isNullValue(value) -> null
        value is Map<*, *> -> convertObject(value)
        value is List<*> -> value.map { convertValue(it) }
        JsValueBridge.sizeOf(value) >= 0 -> convertArray(value)
        else -> value
    }

    /** String an toàn từ bất kỳ giá trị JS nào — LUÔN dùng cái này thay obj.toString(). */
    fun getString(obj: Any?): String? {
        if (JsValueBridge.isNullValue(obj)) return null
        if (obj is Map<*, *>) return null
        if (obj is List<*>) return obj.toString()
        if (JsValueBridge.isFunction(obj)) return null
        return JsValueBridge.toJsString(obj)
    }

    fun safeToString(obj: Any?): String? = getString(obj)

    fun getProperty(
        obj: Any?,
        vararg path: String,
    ): Any? {
        if (obj == null || path.isEmpty()) return null
        var current: Any? = obj
        for (key in path) {
            current =
                if (current is Map<*, *>) {
                    current[key]
                } else {
                    JsValueBridge.getProp(current, key)
                }
            if (current == null) return null
        }
        return current
    }
}
