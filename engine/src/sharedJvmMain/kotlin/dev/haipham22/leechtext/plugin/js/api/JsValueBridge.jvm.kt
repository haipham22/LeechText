package dev.haipham22.leechtext.plugin.js.api

import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.NativeArray
import org.mozilla.javascript.NativeJSON
import org.mozilla.javascript.NativeJavaObject
import org.mozilla.javascript.NativeObject
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.Undefined

/**
 * Rhino actual của JsValueBridge (P5.2c) — giữ contract Rhino 1.7.15: object/array trả
 * về JS là NativeObject/NativeArray (property access, Array methods). Context management:
 * dùng context của script đang chạy nếu có, không thì enter tạm.
 */
@Suppress("TooManyFunctions") // API surface cho plugin JS — số hàm mirror đúng API vBook, không tách
actual object JsValueBridge {
    /** Holder cho context tự enter — exit ở cuối operation. */
    private class Ctx(
        val context: Context,
        val owned: Boolean,
    ) {
        fun close() {
            if (owned) Context.exit()
        }

        companion object {
            fun borrow(): Ctx {
                val current = Context.getCurrentContext()
                return if (current != null) Ctx(current, false) else Ctx(Context.enter(), true)
            }
        }
    }

    actual fun arrayOf(items: List<Any?>): Any {
        val ctx = Ctx.borrow()
        return try {
            val scope = ctx.context.initStandardObjects()
            ctx.context.newArray(scope, items.toTypedArray())
        } finally {
            ctx.close()
        }
    }

    actual fun objectOf(pairs: List<Pair<String, Any?>>): Any {
        val ctx = Ctx.borrow()
        return try {
            val obj = ctx.context.newObject(ctx.context.initStandardObjects())
            for ((k, v) in pairs) obj.put(k, obj, v)
            obj
        } finally {
            ctx.close()
        }
    }

    actual fun parseJson(json: String): Any? {
        val ctx = Ctx.borrow()
        return try {
            val scope = ctx.context.initStandardObjects()
            // Reviver trả value as-is (null callable pattern của NativeJSON)
            val reviver = org.mozilla.javascript.Callable { _, _, _, args -> args[1] }
            NativeJSON.parse(ctx.context, scope, json, reviver)
        } finally {
            ctx.close()
        }
    }

    actual fun stringify(value: Any?): String = try {
        val sb = StringBuilder()
        stringifyValue(value, sb)
        sb.toString()
    } catch (e: Exception) {
        "null"
    }

    private fun stringifyValue(
        value: Any?,
        sb: StringBuilder,
    ) {
        when {
            value == null || value === Undefined.instance -> sb.append("null")

            value is Boolean || value is Number -> sb.append(value.toString())

            value is String -> sb.append(quote(value))

            value is Map<*, *> -> stringifyObject(
                sb,
                // key non-string bỏ (behavior gốc: continue)
                value.entries.mapNotNull { e -> (e.key as? String)?.let { k -> k to e.value } },
            )

            value is List<*> -> stringifyArray(sb, value)

            value is NativeObject -> stringifyObject(
                sb,
                value.ids.mapNotNull { key ->
                    (key as? String)?.let { key to RhinoValues.normalize(value[key, value]) }
                },
            )

            value is NativeArray ->
                stringifyArray(
                    sb,
                    List(value.length.toInt()) { i -> RhinoValues.normalize(value[i, value]) },
                )

            else -> sb.append("null")
        }
    }

    /** Object JSON `{...}` — key non-string đã lọc ở caller. */
    private fun stringifyObject(
        sb: StringBuilder,
        fields: List<Pair<String, Any?>>,
    ) {
        sb.append('{')
        fields.forEachIndexed { i, (k, v) ->
            if (i > 0) sb.append(',')
            sb.append(quote(k)).append(':')
            stringifyValue(v, sb)
        }
        sb.append('}')
    }

    /** Array JSON `[...]`. */
    private fun stringifyArray(
        sb: StringBuilder,
        items: List<*>,
    ) {
        sb.append('[')
        items.forEachIndexed { i, v ->
            if (i > 0) sb.append(',')
            stringifyValue(v, sb)
        }
        sb.append(']')
    }

    private fun quote(s: String): String {
        val sb = StringBuilder("\"")
        for (c in s) {
            when (c) {
                '"' -> sb.append("\\\"")

                '\\' -> sb.append("\\\\")

                '\b' -> sb.append("\\b")

                '\u000C' -> sb.append("\\f")

                '\n' -> sb.append("\\n")

                '\r' -> sb.append("\\r")

                '\t' -> sb.append("\\t")

                else ->
                    if (c.code < 32) {
                        sb.append("\\u")
                        sb.append(c.code.toString(16).padStart(4, '0'))
                    } else {
                        sb.append(c)
                    }
            }
        }
        sb.append("\"")
        return sb.toString()
    }

    actual fun getProp(
        obj: Any?,
        key: String,
    ): Any? = when (obj) {
        is NativeObject -> {
            val v = obj[key, obj]
            if (v === Undefined.instance || v === Scriptable.NOT_FOUND) null else v
        }

        is Map<*, *> -> obj[key]

        else -> null
    }

    actual fun hasProp(
        obj: Any?,
        key: String,
    ): Boolean = when (obj) {
        is NativeObject -> obj.has(key, obj)
        is Map<*, *> -> obj.containsKey(key)
        else -> false
    }

    actual fun setProp(
        obj: Any?,
        key: String,
        value: Any?,
    ) {
        when (obj) {
            is NativeObject -> obj.put(key, obj, value)

            is MutableMap<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                (obj as MutableMap<String, Any?>)[key] = value
            }
        }
    }

    actual fun sizeOf(value: Any?): Int = when (value) {
        is NativeArray -> value.length.toInt()

        is List<*> -> value.size

        is Array<*> -> value.size

        is NativeJavaObject -> {
            val unwrapped = value.unwrap()
            when (unwrapped) {
                is List<*> -> unwrapped.size
                is Array<*> -> unwrapped.size
                else -> -1
            }
        }

        else -> -1
    }

    actual fun itemAt(
        value: Any?,
        index: Int,
    ): Any? = when (value) {
        is NativeArray -> value[index, value]

        is List<*> -> value.getOrNull(index)

        is Array<*> -> value.getOrNull(index)

        is NativeJavaObject -> {
            val unwrapped = value.unwrap()
            when (unwrapped) {
                is List<*> -> unwrapped.getOrNull(index)
                is Array<*> -> unwrapped.getOrNull(index)
                else -> null
            }
        }

        else -> null
    }

    actual fun toJsString(value: Any?): String? {
        if (value == null) return null
        return try {
            if (Context.getCurrentContext() == null) return value.toString()
            Context.toString(value)
        } catch (e: Exception) {
            null
        }
    }

    actual fun isNullValue(value: Any?): Boolean = value == null || value === Undefined.instance

    actual fun isFunction(value: Any?): Boolean = value is Function

    actual fun call(
        fn: Any?,
        args: List<Any?>,
    ): Any? {
        if (fn !is Function) return null
        val ctx = Context.getCurrentContext() ?: return null
        val scope = fn.parentScope ?: fn
        return fn.call(ctx, scope, scope, args.toTypedArray())
    }
}

/**
 * Boundary normalizer Rhino → Kotlin (P5.2c internal) — JsSandbox.jvm gọi sau
 * callFunction để loaders common chỉ thấy Map/List/primitive/null/Kotlin object.
 */
internal object RhinoValues {
    fun normalize(v: Any?): Any? = when {
        v == null || v === Undefined.instance -> null

        v is NativeObject -> {
            val map = LinkedHashMap<String, Any?>()
            for (id in v.ids) {
                val key = if (id is Number) id.toInt().toString() else id.toString()
                map[key] = normalize(v[key, v])
            }
            map
        }

        v is NativeArray -> {
            val list = ArrayList<Any?>(v.length.toInt())
            for (i in 0 until v.length.toInt()) list.add(normalize(v[i, v]))
            list
        }

        v is NativeJavaObject -> {
            val unwrapped = v.unwrap()
            if (unwrapped is Scriptable) normalize(unwrapped) else unwrapped
        }

        v is Function -> null

        // JSResponse.getString(function) semantics — không string hóa
        v is Scriptable -> null

        // Symbol/other non-data Scriptable

        else -> v
    }
}
