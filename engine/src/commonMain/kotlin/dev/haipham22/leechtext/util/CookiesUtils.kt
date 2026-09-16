package dev.haipham22.leechtext.util

/**
 * Cookie store in-memory (port từ util/CookiesUtils.java; key = host của URL).
 * P1: persist cookies ra file như bản gốc nếu cần.
 * ponytail: plain map không lock — race tệ nhất mất 1 cookie write, đủ cho Use case fetch.
 */
object CookiesUtils {
    private val COOKIES = HashMap<String, String>()

    // Host = phần giữa "//" và ký tự /?# đầu tiên — URL không slash cuối (vd "https://host") vẫn parse
    private val HOST_REGEX = Regex("https?://([^/?#]+)")

    fun getCookies(url: String): String? = monitorLock(this) { parseKey(url)?.let { COOKIES[it] } }

    fun put(
        url: String,
        cookies: String,
    ) {
        monitorLock(this) { parseKey(url)?.let { COOKIES[it] = cookies } }
    }

    fun remove(url: String) {
        monitorLock(this) { parseKey(url)?.let { COOKIES.remove(it) } }
    }

    private fun parseKey(url: String): String? = HOST_REGEX.find(url)?.groupValues?.get(1)
}
