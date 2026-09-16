package dev.haipham22.leechtext.plugin.js.api

import dev.haipham22.leechtext.log.EngineLogger
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import platform.posix.usleep

/**
 * Apple actual của HTTP executor (P5.2c) — ktor Darwin engine (okhttp không có artifact
 * native). Timeout 90s match JVM; redirect theo default engine.
 * ponytail: redirect cuối không expose X-Final-Url — Html.parseUrl baseUri fallback
 * URL gốc; thêm khi plugin cần abs: URL sau redirect.
 */
internal actual fun httpExecute(
    spec: HttpSpec,
    log: EngineLogger,
): HttpResult = runBlocking {
    val response: HttpResponse =
        CLIENT.request(spec.url) {
            method = HttpMethod.parse(spec.method)
            for ((k, v) in spec.headers) header(k, v)
            spec.bodyContentType?.let { contentType(ContentType.parse(it)) }
            spec.body?.let { setBody(it) }
        }
    val headers = HashMap<String, String>()
    for (name in response.headers.names()) {
        headers[name] = response.headers[name] ?: ""
    }
    HttpResult(
        code = response.status.value,
        message = response.status.description,
        headers = headers,
        body = response.bodyAsBytes(),
    )
}

/** Client dùng chung (per-call client leak tài nguyên). */
private val CLIENT: HttpClient =
    HttpClient(Darwin) {
        expectSuccess = false
        followRedirects = true
    }

// url: desktop JVM dùng Playwright — iOS/macOS: WebView solver qua bridge (iOS đăng
// ký lúc khởi động; macOS không đăng ký → null như cũ)
internal actual fun browserFetchHtml(
    url: String?,
    log: EngineLogger,
): String? {
    if (url == null) return null
    val solver = BrowserSolverBridge.solver ?: return null
    return try {
        solver(url, 120_000)
    } catch (e: Exception) {
        log.add("[Http] WebView solver failed: ${e.message}")
        null
    }
}

internal actual fun sleepMs(ms: Long) {
    usleep((ms * 1000).toUInt())
}
