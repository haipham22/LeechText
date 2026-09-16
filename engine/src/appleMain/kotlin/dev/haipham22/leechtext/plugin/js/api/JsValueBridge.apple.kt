@file:Suppress("MatchingDeclarationName")

package dev.haipham22.leechtext.plugin.js.api

import dev.haipham22.leechtext.util.LeechJson
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * QuickJS/apple actual của JsValueBridge (P5.2c) — giá trị engine là Map/List thuần
 * (quickjs-kt convert List→JS Array; Map cần JsObject wrap khi đẩy sang JS — làm ở
 * sandbox binding, bridge trả Map cho Kotlin callers).
 */
@Suppress("TooManyFunctions") // API surface cho plugin JS — số hàm mirror đúng API vBook, không tách
actual object JsValueBridge {
    actual fun arrayOf(items: List<Any?>): Any = items

    actual fun objectOf(pairs: List<Pair<String, Any?>>): Any = linkedMapOf<String, Any?>().apply { pairs.forEach { put(it.first, it.second) } }

    actual fun parseJson(json: String): Any? {
        if (json.isEmpty()) return null
        return try {
            elementToValue(LeechJson.parseToJsonElement(json))
        } catch (e: Exception) {
            null
        }
    }

    private fun elementToValue(element: kotlinx.serialization.json.JsonElement): Any? = when (element) {
        is JsonObject -> {
            val map = LinkedHashMap<String, Any?>()
            for ((k, v) in element) map[k] = elementToValue(v)
            map
        }

        is JsonArray -> element.map { elementToValue(it) }

        is JsonNull -> null

        is JsonPrimitive ->
            when {
                element.isString -> element.content
                element.booleanOrNull != null -> element.booleanOrNull
                else -> element.content.toLongOrNull() ?: element.content.toDoubleOrNull() ?: element.content
            }
    }

    actual fun stringify(value: Any?): String = try {
        valueToJsonElement(value).toString()
    } catch (e: Exception) {
        "null"
    }

    private fun valueToJsonElement(value: Any?): kotlinx.serialization.json.JsonElement = when (value) {
        null -> JsonNull

        is Boolean -> JsonPrimitive(value)

        is Int -> JsonPrimitive(value)

        is Long -> JsonPrimitive(value)

        is Double -> JsonPrimitive(value)

        is Float -> JsonPrimitive(value)

        is String -> JsonPrimitive(value)

        is Map<*, *> -> buildJsonObject {
            for ((k, v) in value) {
                if (k is String) put(k, valueToJsonElement(v))
            }
        }

        is List<*> -> JsonArray(value.map { valueToJsonElement(it) })

        is Array<*> -> JsonArray(value.map { valueToJsonElement(it) })

        else -> JsonNull
    }

    actual fun getProp(
        obj: Any?,
        key: String,
    ): Any? = (obj as? Map<*, *>)?.get(key)

    actual fun hasProp(
        obj: Any?,
        key: String,
    ): Boolean = (obj as? Map<*, *>)?.containsKey(key) == true

    // Generic bị erase — runtime chỉ check được MutableMap; an toàn vì mọi map đi qua
    // bridge đều do objectOf() tạo (LinkedHashMap<String, Any?>), không có map lạ chen vào.
    @Suppress("UNCHECKED_CAST")
    actual fun setProp(
        obj: Any?,
        key: String,
        value: Any?,
    ) {
        (obj as? MutableMap<String, Any?>)?.set(key, value)
    }

    actual fun sizeOf(value: Any?): Int = when (value) {
        is List<*> -> value.size
        is Array<*> -> value.size
        else -> -1
    }

    actual fun itemAt(
        value: Any?,
        index: Int,
    ): Any? = when (value) {
        is List<*> -> value.getOrNull(index)
        is Array<*> -> value.getOrNull(index)
        else -> null
    }

    actual fun toJsString(value: Any?): String? = value?.toString()

    actual fun isNullValue(value: Any?): Boolean = value == null

    actual fun isFunction(value: Any?): Boolean = false

    actual fun call(
        fn: Any?,
        args: List<Any?>,
    ): Any? = null
}
