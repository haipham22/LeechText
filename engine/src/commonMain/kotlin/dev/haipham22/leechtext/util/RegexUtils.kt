package dev.haipham22.leechtext.util

/** Tìm match đầu tiên, trả về group (0 = cả match), null nếu không match (port RegexUtils.java). */
fun regexFind(
    src: String?,
    regex: String,
    group: Int = 1,
): String? {
    return try {
        Regex(regex, RegexOption.MULTILINE).find(src ?: return null)?.groupValues?.get(group)
    } catch (e: Exception) {
        null
    }
}
