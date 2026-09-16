package dev.haipham22.leechtext.util

/** URL(String) deprecated (JDK 20+) — parse qua URI; IAE đổi thành MalformedURLException giữ contract caller (S1874).
 *  P5.2b: tách khỏi TypeUtils (common) vì java.net URL là JVM-only. */
fun String.toUrl(): java.net.URL = try {
    java.net.URI(this).toURL()
} catch (e: IllegalArgumentException) {
    throw java.net.MalformedURLException(e.message)
}
