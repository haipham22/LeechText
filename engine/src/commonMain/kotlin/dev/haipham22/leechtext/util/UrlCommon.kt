package dev.haipham22.leechtext.util

/** URL helpers common (P5.2c) — thay java.net.URL cho loaders/VBook; regex đủ cho
 * protocol/host/port của URL web, không cần full URL parser. */

private val BASE_URL_REGEX = Regex("^(https?)://([^/?#:]+)(:(\\d+))?")

/** Extract protocol://host[:port] từ URL đầy đủ; trả nguyên url nếu không parse được. */
fun parseBaseUrl(url: String): String {
    val m = BASE_URL_REGEX.find(url) ?: return url
    val protocol = m.groupValues[1]
    val host = m.groupValues[2]
    val port = m.groupValues[4]
    return if (port.isEmpty() || isDefaultPort(protocol, port)) {
        "$protocol://$host"
    } else {
        "$protocol://$host:$port"
    }
}

/** Host của URL (bỏ scheme/port/path); null nếu không parse được. */
fun parseHost(url: String): String? = BASE_URL_REGEX.find(url)?.groupValues?.get(2)

private fun isDefaultPort(
    protocol: String,
    port: String,
): Boolean = (protocol == "https" && port == "443") || (protocol == "http" && port == "80")
