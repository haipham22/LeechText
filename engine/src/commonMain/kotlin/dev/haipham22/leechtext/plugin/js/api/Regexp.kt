package dev.haipham22.leechtext.plugin.js.api

import dev.haipham22.leechtext.log.EngineLogger

/** Regex API cho JS plugins (port từ Regexp.java; P5.2c: kotlin.text.Regex thay
 * java.util.regex — semantic match: find/findAll/replace/split theo pattern). */
@Suppress("TooManyFunctions") // API surface cho plugin JS — số hàm mirror đúng API vBook, không tách
class Regexp(
    private val log: EngineLogger,
) {
    /** Compile pattern; log + null khi pattern invalid (giống bản gốc). */
    private fun compiled(pattern: String): Regex? = try {
        Regex(pattern)
    } catch (e: Exception) {
        log.add("Invalid regex pattern: $pattern")
        null
    }

    /** Find match đầu tiên. Usage: regexp.find("Hello World", "llo") => "llo" */
    fun find(
        text: String?,
        pattern: String?,
    ): String {
        if (text == null || pattern == null) return ""
        val m = compiled(pattern) ?: return ""
        return m.find(text)?.value ?: ""
    }

    /** Find tất cả matches. Usage: regexp.findAll("Hello World", "l") => ["l", "l", "l"] */
    fun findAll(
        text: String?,
        pattern: String?,
    ): Array<String> {
        val results = ArrayList<String>()
        if (text != null && pattern != null) {
            val m = compiled(pattern)
            if (m != null) m.findAll(text).forEach { results.add(it.value) }
        }
        return results.toTypedArray()
    }

    /** Match tất cả kèm groups và indices. Usage: regexp.matchAll("test123", "\\d+") */
    fun matchAll(
        text: String?,
        pattern: String?,
    ): Array<HashMap<String, Any?>> {
        if (text == null || pattern == null) return arrayOf()

        val results = ArrayList<HashMap<String, Any?>>()
        val regex = compiled(pattern) ?: return arrayOf()
        var matchCount = 0
        for (match in regex.findAll(text)) {
            val matchInfo = HashMap<String, Any?>()
            matchInfo["match"] = match.value
            matchInfo["start"] = match.range.first
            matchInfo["end"] = match.range.last + 1
            matchInfo["index"] = matchCount++

            if (match.groups.size > 1) {
                val groups = ArrayList<String?>()
                for (i in 1 until match.groups.size) groups.add(match.groups[i]?.value)
                matchInfo["groups"] = groups.toTypedArray()
            }

            results.add(matchInfo)
        }
        return results.toTypedArray()
    }

    /** Replace match đầu tiên theo pattern (tên gốc vBook — alias replaceFirst). */
    fun replace(
        text: String?,
        pattern: String?,
        replacement: String?,
    ): String = replaceFirst(text, pattern, replacement)

    /** Replace match đầu tiên theo pattern. */
    fun replaceFirst(
        text: String?,
        pattern: String?,
        replacement: String?,
    ): String {
        if (text == null || pattern == null) return text ?: ""
        val m = compiled(pattern) ?: return text
        return m.replaceFirst(text, replacement ?: "")
    }

    /** Replace tất cả occurrences theo pattern. */
    fun replaceAll(
        text: String?,
        pattern: String?,
        replacement: String?,
    ): String {
        if (text == null || pattern == null) return text ?: ""
        val m = compiled(pattern) ?: return text
        return m.replace(text, replacement ?: "")
    }

    /** Split text theo pattern (limit -1, giữ trailing empty). */
    fun split(
        text: String?,
        pattern: String?,
    ): Array<String> {
        if (text == null || pattern == null) {
            return if (text != null) arrayOf(text) else arrayOf()
        }
        return try {
            // limit lớn dương = giữ trailing empty (Java Pattern.split limit âm semantics —
            // Kotlin stdlib split không nhận limit âm)
            Regex(pattern).split(text, Int.MAX_VALUE).toTypedArray()
        } catch (e: Exception) {
            log.add("Invalid regex pattern: $pattern")
            arrayOf(text)
        }
    }

    /** Test pattern có match không. */
    fun test(
        text: String?,
        pattern: String?,
    ): Boolean {
        if (text == null || pattern == null) return false
        val m = compiled(pattern) ?: return false
        return m.containsMatchIn(text)
    }

    /** Escape regex special characters. */
    fun escape(text: String?): String = if (text == null) "" else Regex.escape(text)

    /** Find match cuối cùng. */
    fun findLast(
        text: String?,
        pattern: String?,
    ): String {
        if (text == null || pattern == null) return ""
        val m = compiled(pattern) ?: return ""
        return m.findAll(text).lastOrNull()?.value ?: ""
    }

    /** Đếm occurrences theo pattern. */
    fun count(
        text: String?,
        pattern: String?,
    ): Int {
        if (text == null || pattern == null) return 0
        val m = compiled(pattern) ?: return 0
        return m.findAll(text).count()
    }

    /** Lấy tất cả groups từ match đầu tiên. */
    fun groups(
        text: String?,
        pattern: String?,
    ): Array<String?> {
        if (text == null || pattern == null) return arrayOf()

        val groupList = ArrayList<String?>()
        val m = compiled(pattern)
        val match = m?.find(text)
        if (match != null) {
            for (i in 0..match.groups.size - 1) groupList.add(match.groups[i]?.value)
        }
        return groupList.toTypedArray()
    }

    /** Trích groups dạng "group1", "group2", ... từ match đầu tiên. */
    fun namedGroups(
        text: String?,
        pattern: String?,
    ): MutableMap<String, String?> {
        val result = HashMap<String, String?>()
        if (text == null || pattern == null) return result

        val match = compiled(pattern)?.find(text)
        if (match != null) {
            for (i in 1..match.groups.size - 1) result["group$i"] = match.groups[i]?.value
        }
        return result
    }

    /** Check pattern hợp lệ. */
    fun isValid(pattern: String?): Boolean {
        if (pattern.isNullOrEmpty()) return false
        return try {
            Regex(pattern)
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Lấy match tại index cụ thể. */
    fun matchAt(
        text: String?,
        pattern: String?,
        index: Int,
    ): String {
        if (text == null || pattern == null || index < 0) return ""

        val m = compiled(pattern) ?: return ""
        return m.findAll(text).elementAtOrNull(index)?.value ?: ""
    }

    /** Tất cả matches kèm vị trí. */
    fun matchesWithPositions(
        text: String?,
        pattern: String?,
    ): Array<HashMap<String, Any?>> {
        if (text == null || pattern == null) return arrayOf()

        val results = ArrayList<HashMap<String, Any?>>()
        val m = compiled(pattern)
        if (m != null) {
            m.findAll(text).forEach { match ->
                val m2 = HashMap<String, Any?>()
                m2["text"] = match.value
                m2["start"] = match.range.first
                m2["end"] = match.range.last + 1
                results.add(m2)
            }
        }
        return results.toTypedArray()
    }
}
