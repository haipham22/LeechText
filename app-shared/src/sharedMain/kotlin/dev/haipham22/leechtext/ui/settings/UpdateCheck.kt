package dev.haipham22.leechtext.ui.settings

import dev.haipham22.leechtext.AppInfo
import dev.haipham22.leechtext.log.EngineLogger
import dev.haipham22.leechtext.plugin.js.api.Http
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Release mới nhất trên GitHub — tag đã bỏ prefix "v". */
data class Release(
    val version: String,
    val url: String,
)

/**
 * Kiểm tra bản cập nhật từ GitHub releases/latest của repo haipham22/leechtext2.
 * [latest] là blocking HTTP — caller tự bọc withContext(Dispatchers.IO).
 */
object UpdateCheck {
    private const val LATEST_URL = "https://api.github.com/repos/haipham22/leechtext2/releases/latest"

    /** GET releases/latest; null khi lỗi mạng/throttle/parse — im lặng, không throw. */
    fun latest(log: EngineLogger): Release? = runCatching {
        val res =
            Http(log)
                .get(LATEST_URL)
                .timeout(10_000)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "LeechText/${AppInfo.VERSION}")
        if (!res.ok()) return@runCatching null
        val obj = Json.parseToJsonElement(res.string()).jsonObject
        val tag = obj["tag_name"]?.jsonPrimitive?.content ?: return@runCatching null
        val url = obj["html_url"]?.jsonPrimitive?.content ?: return@runCatching null
        Release(tag.removePrefix("v"), url)
    }.getOrNull()

    /** So per-segment số học ("1.10.0" > "1.9.1"); segment thiếu = 0; tag rác → false. */
    fun isNewer(cur: String, tag: String): Boolean {
        val a = segments(cur) ?: return false
        val b = segments(tag) ?: return false
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x < y
        }
        return false
    }

    private fun segments(version: String): List<Int>? {
        val parts = version.trim().removePrefix("v").split('.')
        return if (parts.all { it.toIntOrNull() != null }) parts.map { it.toInt() } else null
    }
}
