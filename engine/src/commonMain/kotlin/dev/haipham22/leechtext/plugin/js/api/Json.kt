package dev.haipham22.leechtext.plugin.js.api

import dev.haipham22.leechtext.log.EngineLogger
import dev.haipham22.leechtext.util.LeechJson
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/**
 * JSON API cho JS plugins (port từ Json.java; P5.2c common). parse() qua JsValueBridge —
 * JVM NativeJSON.parse (giữ property access `json.chap_list` cho Rhino), apple kotlinx
 * → Map/List. Fallback manual (kotlinx) như bản gốc.
 */
class Json(
    private val log: EngineLogger,
) {
    fun parse(jsonString: String?): Any? {
        if (jsonString == null || jsonString.isEmpty()) {
            log.add("[Json.parse()] Input is null or empty")
            return null
        }
        val trimmed = jsonString.trim()
        if (trimmed.isEmpty()) {
            log.add("[Json.parse()] Trimmed input is empty")
            return null
        }
        return try {
            val result = JsValueBridge.parseJson(jsonString)
            when (result) {
                is Map<*, *> -> log.add("[Json.parse()] Parsed as object with ${result.size} keys")
                is List<*> -> log.add("[Json.parse()] Parsed as array with length: ${result.size}")
                else -> log.add("[Json.parse()] Parsed as: ${result?.let { it::class.simpleName }}")
            }
            result
        } catch (e: Exception) {
            log.add("[Json.parse()] Engine parsing failed: ${e.message}, trying manual")
            parseManual(jsonString)
        }
    }

    /** Fallback manual parsing (kotlinx — Map/List/primitive). */
    private fun parseManual(jsonString: String): Any? = try {
        val trimmed = jsonString.trim()
        when {
            trimmed.startsWith("{") || trimmed.startsWith("[") -> elementToValue(LeechJson.parseToJsonElement(jsonString))
            else -> parsePrimitive(trimmed)
        }
    } catch (e: Exception) {
        log.add("[Json.parse()] Manual parsing failed: ${e.message}")
        null
    }

    /** JsonElement → Kotlin value (Map/List/primitive). */
    private fun elementToValue(element: JsonElement): Any? = when (element) {
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

    /** Convert value → JSON string. */
    fun stringify(value: Any?): String = JsValueBridge.stringify(value)

    private fun parsePrimitive(value: String): Any? {
        if (value.length >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length - 1)
        }
        if (value.equals("true", ignoreCase = true)) return true
        if (value.equals("false", ignoreCase = true)) return false
        if (value.equals("null", ignoreCase = true)) return null
        return try {
            if (value.contains(".")) value.toDouble() else value.toLong()
        } catch (e: NumberFormatException) {
            value
        }
    }

    /** Get value theo path dot-notation: json.get(obj, "user.name"). */
    fun get(
        obj: Any?,
        path: String?,
    ): Any? {
        if (obj == null || path.isNullOrEmpty()) return null
        var current: Any? = obj
        for (part in path.split(".")) {
            current = descend(current, part)
            if (current == null) return null
        }
        return current
    }

    /** Lấy 1 cấp con theo part: index cho array/array-like, key cho object. */
    private fun descend(
        current: Any?,
        part: String,
    ): Any? = when (current) {
        null -> null

        is Map<*, *> -> current[part]

        is List<*> -> part.toIntOrNull()?.let { current.getOrNull(it) }

        else ->
            if (JsValueBridge.sizeOf(current) >= 0) {
                part.toIntOrNull()?.let { JsValueBridge.itemAt(current, it) }
            } else {
                JsValueBridge.getProp(current, part)
            }
    }

    /** Set value theo path dot-notation. */
    fun set(
        obj: Any?,
        path: String?,
        value: Any?,
    ) {
        if (obj == null || path.isNullOrEmpty()) return
        val parts = path.split(".")
        var current: Any? = obj

        // Navigate tới parent
        for (i in 0 until parts.size - 1) {
            val part = parts[i]
            current =
                when (current) {
                    is Map<*, *> -> {
                        @Suppress("UNCHECKED_CAST")
                        val map = current as MutableMap<String, Any?>
                        if (!map.containsKey(part)) map[part] = LinkedHashMap<String, Any?>()
                        map[part]
                    }

                    else -> {
                        if (JsValueBridge.getProp(current, part) == null) {
                            JsValueBridge.setProp(current, part, JsValueBridge.objectOf(emptyList()))
                        }
                        JsValueBridge.getProp(current, part)
                    }
                }
        }

        val lastPart = parts.last()
        when (current) {
            is MutableMap<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                (current as MutableMap<String, Any?>)[lastPart] = value
            }

            else -> {
                JsValueBridge.setProp(current, lastPart, value)
            }
        }
    }

    fun isValid(jsonString: String?): Boolean {
        if (jsonString == null || jsonString.trim().isEmpty()) return false
        return try {
            parse(jsonString)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun pretty(value: Any?): String = stringify(value)
}
