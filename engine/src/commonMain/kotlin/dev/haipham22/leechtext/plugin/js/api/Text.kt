package dev.haipham22.leechtext.plugin.js.api

import dev.haipham22.leechtext.log.EngineLogger

/**
 * Text utilities API cho JS plugins (port từ Text.java; P5.2c common): string
 * manipulation, encoding. Array trả về qua JsValueBridge.arrayOf (JVM: NativeArray —
 * JS array đầy đủ method; apple: List).
 */
@Suppress("TooManyFunctions") // API surface cho plugin JS — số hàm mirror đúng API vBook, không tách
class Text(
    private val log: EngineLogger,
) {
    private fun jsArray(elements: List<String>): Any = JsValueBridge.arrayOf(elements)

    /** Trim whitespace 2 đầu. Usage: text.trim(" hello ") => "hello" */
    fun trim(text: String?): String = text?.trim() ?: ""

    /** Trim các ký tự chỉ định từ đầu. */
    fun trimStart(
        text: String?,
        chars: String?,
    ): String {
        if (text == null) return ""
        if (chars.isNullOrEmpty()) return text
        var start = 0
        while (start < text.length && chars.indexOf(text[start]) >= 0) start++
        return text.substring(start)
    }

    /** Trim các ký tự chỉ định từ cuối. */
    fun trimEnd(
        text: String?,
        chars: String?,
    ): String {
        if (text == null) return ""
        if (chars.isNullOrEmpty()) return text
        var end = text.length
        while (end > 0 && chars.indexOf(text[end - 1]) >= 0) end--
        return text.substring(0, end)
    }

    fun lower(text: String?): String = text?.lowercase() ?: ""

    fun upper(text: String?): String = text?.uppercase() ?: ""

    /** Viết hoa chữ cái đầu. */
    fun capitalize(text: String?): String {
        if (text.isNullOrEmpty()) return ""
        return text.substring(0, 1).uppercase() +
            (if (text.length > 1) text.substring(1) else "")
    }

    /** Viết hoa đầu mỗi từ. */
    fun titleCase(text: String?): String {
        if (text.isNullOrEmpty()) return ""
        val result = StringBuilder()
        for (word in text.split(Regex("\\s+"))) {
            if (word.isNotEmpty()) {
                if (result.isNotEmpty()) result.append(" ")
                result.append(word.substring(0, 1).uppercase())
                if (word.length > 1) {
                    result.append(word.substring(1).lowercase())
                }
            }
        }
        return result.toString()
    }

    /** Split theo delimiter literal (giữ trailing empty như bản gốc với limit -1). */
    fun split(
        text: String?,
        delimiter: String?,
    ): Any {
        val result: List<String> =
            when {
                text == null -> emptyList()

                // Pattern.compile("").split(text): match zero-width ở đầu + giữa ký tự
                delimiter.isNullOrEmpty() -> listOf("") + text.chunked(1)

                else -> text.split(delimiter)
            }
        return jsArray(result)
    }

    /** Join mảng string với delimiter. */
    fun join(
        array: Any?,
        delimiter: String?,
    ): String {
        if (array == null) return ""
        val delim = delimiter ?: ""
        val size = JsValueBridge.sizeOf(array)
        if (size < 0) return ""
        val result = StringBuilder()
        for (i in 0 until size) {
            if (i > 0) result.append(delim)
            result.append(JsValueBridge.toJsString(JsValueBridge.itemAt(array, i)) ?: "")
        }
        return result.toString()
    }

    /** Replace tất cả occurrences (literal). */
    fun replace(
        text: String?,
        target: String?,
        replacement: String?,
    ): String {
        if (text == null) return ""
        if (target.isNullOrEmpty()) return text
        return text.replace(target, replacement ?: "")
    }

    /** Replace occurrence đầu tiên (literal). */
    fun replaceFirst(
        text: String?,
        target: String?,
        replacement: String?,
    ): String {
        if (text == null) return ""
        if (target.isNullOrEmpty()) return text
        val index = text.indexOf(target)
        if (index >= 0) {
            return text.substring(0, index) +
                (replacement ?: "") +
                text.substring(index + target.length)
        }
        return text
    }

    fun substring(
        text: String?,
        start: Int,
    ): String = substring(text, start, text?.length ?: 0)

    fun substring(
        text: String?,
        start: Int,
        end: Int,
    ): String {
        if (text == null) return ""
        var s = start
        var e = end
        return try {
            if (s < 0) s = 0
            if (e < 0) e = text.length + e
            if (e > text.length) e = text.length
            text.substring(s, e)
        } catch (err: IndexOutOfBoundsException) {
            ""
        }
    }

    fun length(text: String?): Int = text?.length ?: 0

    fun contains(
        text: String?,
        substring: String?,
    ): Boolean {
        if (text == null || substring == null) return false
        return text.contains(substring)
    }

    fun startsWith(
        text: String?,
        prefix: String?,
    ): Boolean {
        if (text == null || prefix == null) return false
        return text.startsWith(prefix)
    }

    fun endsWith(
        text: String?,
        suffix: String?,
    ): Boolean {
        if (text == null || suffix == null) return false
        return text.endsWith(suffix)
    }

    /** Lặp text n lần. */
    fun repeat(
        text: String?,
        count: Int,
    ): String {
        if (text == null || count <= 0) return ""
        val result = StringBuilder()
        repeat(count) { result.append(text) }
        return result.toString()
    }

    /** Pad trái tới length. */
    fun padLeft(
        text: String?,
        length: Int,
        padChar: String?,
    ): String {
        var t = text ?: ""
        var pad = if (padChar.isNullOrEmpty()) " " else padChar
        while (t.length < length) t = pad + t
        return t
    }

    /** Pad phải tới length. */
    fun padRight(
        text: String?,
        length: Int,
        padChar: String?,
    ): String {
        var t = text ?: ""
        var pad = if (padChar.isNullOrEmpty()) " " else padChar
        while (t.length < length) t += pad
        return t
    }

    /** Trích text giữa 2 marker. */
    fun between(
        text: String?,
        start: String?,
        end: String?,
    ): String {
        if (text == null || start == null || end == null) return ""
        var startIndex = text.indexOf(start)
        if (startIndex < 0) return ""
        startIndex += start.length
        val endIndex = text.indexOf(end, startIndex)
        if (endIndex < 0) return ""
        return text.substring(startIndex, endIndex)
    }

    /** Trích tất cả text giữa 2 marker. */
    fun betweenAll(
        text: String?,
        start: String?,
        end: String?,
    ): Any {
        val results = ArrayList<String>()
        if (text == null || start == null || end == null) {
            return jsArray(emptyList())
        }

        var searchFrom = 0
        while (true) {
            var startIndex = text.indexOf(start, searchFrom)
            if (startIndex < 0) break
            startIndex += start.length

            val endIndex = text.indexOf(end, startIndex)
            if (endIndex < 0) break

            results.add(text.substring(startIndex, endIndex))
            searchFrom = endIndex + end.length
        }
        return jsArray(results)
    }

    /** Xóa HTML tags. */
    fun stripHtml(html: String?): String {
        if (html == null) return ""
        return html.replace("<[^>]+>".toRegex(), "")
    }

    /** Trích N ký tự đầu. */
    fun truncate(
        text: String?,
        maxLength: Int,
    ): String = truncate(text, maxLength, "...")

    fun truncate(
        text: String?,
        maxLength: Int,
        suffix: String?,
    ): String {
        if (text == null) return ""
        if (text.length <= maxLength) return text
        val safeSuffix = suffix ?: ""
        return text.substring(0, maxLength - safeSuffix.length) + safeSuffix
    }

    fun reverse(text: String?): String {
        if (text == null) return ""
        return text.reversed()
    }

    /** Đếm occurrences của substring. */
    fun count(
        text: String?,
        substring: String?,
    ): Int {
        if (text == null || substring.isNullOrEmpty()) return 0
        var count = 0
        var index = 0
        while (text.indexOf(substring, index).also { index = it } >= 0) {
            count++
            index += substring.length
        }
        return count
    }

    /** Check text match toàn bộ regex pattern. */
    fun matches(
        text: String?,
        pattern: String?,
    ): Boolean {
        if (text == null || pattern == null) return false
        return try {
            Regex(pattern).matches(text)
        } catch (e: Exception) {
            log.add("Invalid regex pattern: $pattern")
            false
        }
    }

    /** Trích tất cả regex matches. */
    fun extractAll(
        text: String?,
        pattern: String?,
    ): Any {
        val results = ArrayList<String>()
        if (text == null || pattern == null) {
            return jsArray(emptyList())
        }
        try {
            Regex(pattern).findAll(text).forEach { results.add(it.value) }
        } catch (e: Exception) {
            log.add("Invalid regex pattern: $pattern")
        }
        return jsArray(results)
    }

    fun toBytes(text: String?): ByteArray = if (text == null) ByteArray(0) else text.encodeToByteArray()

    fun fromBytes(bytes: ByteArray?): String = if (bytes == null) "" else bytes.decodeToString()

    fun urlEncode(text: String?): String = if (text == null) "" else urlFormEncode(text.encodeToByteArray())

    fun urlDecode(text: String?): String = if (text == null) "" else urlFormDecode(text)
}

/** application/x-www-form-urlencoded encode (space → '+', giữ alnum .-*_ như URLEncoder). */
internal fun urlFormEncode(bytes: ByteArray): String = buildString {
    for (b in bytes) {
        val c = b.toInt() and 0xFF
        when {
            c in 'A'.code..'Z'.code || c in 'a'.code..'z'.code || c in '0'.code..'9'.code ||
                c == '.'.code || c == '-'.code || c == '*'.code || c == '_'.code -> append(c.toChar())

            c == ' '.code -> append('+')

            else -> append('%').append(HEX[c ushr 4]).append(HEX[c and 0x0F])
        }
    }
}

private val HEX = "0123456789ABCDEF".toCharArray()

/** Decode form-encoded string ('+' → space, %XX theo UTF-8 bytes). */
internal fun urlFormDecode(text: String): String = formDecodeToBytes(text).decodeToString()

/** Form-decode về raw bytes — charset handling do caller (decodeBytes seam). */
internal fun formDecodeToBytes(text: String): ByteArray {
    val bytes = ArrayList<Byte>(text.length)
    var i = 0
    while (i < text.length) {
        val c = text[i]
        when {
            c == '+' -> {
                bytes.add(' '.code.toByte())
                i++
            }

            c == '%' && i + 2 < text.length -> {
                val hex = text.substring(i + 1, i + 3)
                val byte = hex.toIntOrNull(16)
                if (byte != null && hex.length == 2) {
                    bytes.add(byte.toByte())
                    i += 3
                } else {
                    bytes.add(c.code.toByte())
                    i++
                }
            }

            else -> {
                bytes.add(c.code.toByte())
                i++
            }
        }
    }
    return bytes.toByteArray()
}
