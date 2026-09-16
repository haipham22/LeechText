package dev.haipham22.leechtext.get

import dev.haipham22.leechtext.models.Chapter

/**
 * Hiệu chỉnh tên chương (port từ action/Config.java — fixName/autoFixName/Optimize/
 * splitPartName/upperFirst, bỏ TableListener: mutate trực tiếp Chapter).
 */

/** Sửa tự động: tách "Quyển X" khỏi tên chương + chuẩn hoá cả part & chap name. */
fun Chapter.autoFixName() {
    splitPartName()
    chapName = fixName(chapName) ?: chapName
    partName = fixPartName(partName) ?: partName
}

/** Tối ưu: viết hoa đầu câu + chuẩn hoá tên. */
fun Chapter.optimizeName() {
    chapName = optimize(chapName) ?: chapName
    partName = optimize(partName) ?: partName
}

fun List<Chapter>.autoFixAllNames() = forEach { it.autoFixName() }

fun List<Chapter>.optimizeAllNames() = forEach { it.optimizeName() }

/** Tách partName ("Quyển 1: ...") khỏi chapName nếu chap chứa cả hai. */
private fun Chapter.splitPartName() {
    if ((partName ?: "").length > 1) return
    val name = chapName ?: return
    if (countMatches(name, "(Quy.n |Q\\.|Q)\\d+([\\+\\.-]\\d+|)") != 1) return

    val match =
        Regex("((Quy.n |Q.|Q)\\d+\\s*[:-](.*?)*)\\s*(([Cc]h..ng|Hồi)\\s+\\d+)").find(name)
            ?: return
    var part = match.groupValues[1].replace(Regex("\\s*[:-]\\s*$"), "")
    var chap = name.replace(part, "").replace(Regex("^\\s*[:-]\\s*"), "")
    part = part.replace(Regex("Q\\.|Q(\\d+)"), "Quyển $1")
    chapName = chap
    partName = part
}

/** Chuẩn hoá tên chương — chuỗi replace của bản gốc. */
private fun fixName(name: String?): String? {
    if (name.isNullOrEmpty()) return name
    return name
        .replace(Regex("Chương \\d+\\s*[:-]\\s*(Chương \\d+.*?\$)"), "\$1")
        .replace(Regex("^([hH]ồi|[đĐ]ệ) (\\d+)"), "Chương \$1")
        .replace(Regex("(\\d+) [Cc]h..ng"), "Chương \$1")
        .replace(Regex("\\s+"), " ")
        .replace(Regex("Chương (\\d+)\\s*[-\\+:]\\s*(\\d+)"), "Chương \$1+\$2")
        .replace(Regex("(Chương \\d+)\\s*[;:-]+\\s*"), "\$1: ")
        .replace(Regex("(Chương \\d+\\+\\d+)\\s*[;:-]+\\s*"), "\$1: ")
}

/** Quay "Quyển N" về dạng chuẩn trong partName. */
private fun fixPartName(part: String?): String? = part?.replace(Regex("Q\\.|Q(\\d+)"), "Quyển \$1")

/** Viết hoa chữ đầu + sau mỗi dấu câu không phân biệt hoa thường. */
private fun upperFirst(name: String): String {
    val lower = name.lowercase()
    val chars = lower.toCharArray()
    if (chars.isNotEmpty()) chars[0] = chars[0].uppercaseChar()
    for (i in 1 until chars.size) {
        if (chars[i - 1].lowercaseChar() == chars[i - 1].uppercaseChar()) {
            chars[i] = chars[i].uppercaseChar()
        }
    }
    return chars.concatToString()
}

private fun optimize(name: String?): String? {
    if (name.isNullOrEmpty()) return name
    return upperFirst(fixName(name) ?: name)
}

private fun countMatches(
    text: String,
    pattern: String,
): Int = Regex(pattern).findAll(text).count()
