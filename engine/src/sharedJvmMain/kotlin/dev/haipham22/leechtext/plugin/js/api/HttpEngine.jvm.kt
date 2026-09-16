package dev.haipham22.leechtext.plugin.js.api

import dev.haipham22.leechtext.log.EngineLogger
import dev.haipham22.leechtext.util.SSLUtils
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * JVM actual của HTTP executor (P5.2c) — OkHttpClient (di chuyển từ Http.java companion):
 * lenient SSL match vBooks Android, timeout 90s. Retry 429/503 xử lý ở Http common.
 */
internal actual fun httpExecute(
    spec: HttpSpec,
    log: EngineLogger,
): HttpResult {
    val builder =
        try {
            Request.Builder().url(spec.url)
        } catch (e: IllegalArgumentException) {
            // URL rác từ plugin (object stringify, relative...) — fail sạch
            log.add("[Http] URL không hợp lệ: ${spec.url.take(80)}")
            throw java.io.IOException("Invalid URL: ${spec.url.take(120)}", e)
        }

    for ((k, v) in spec.headers) builder.header(k, v)

    val body =
        spec.body?.toRequestBody(
            (spec.bodyContentType ?: "application/octet-stream").toMediaType(),
        )

    val request =
        if (body != null) {
            builder.method(spec.method, body).build()
        } else {
            builder.method(spec.method, null).build()
        }

    CLIENT.newCall(request).execute().use { response: Response ->
        val headers = HashMap<String, String>()
        for (name in response.headers.names()) headers[name] = response.header(name) ?: ""
        // X-Final-Url cho Html.parseUrl baseUri (URL cuối sau redirect)
        headers["X-Final-Url"] = response.request.url.toString()
        return HttpResult(
            code = response.code,
            message = response.message,
            headers = headers,
            body = response.body.bytes(),
        )
    }
}

/** OkHttp client lenient SSL (match vBooks Android) — giải thích trong SSLUtils. */
internal val HTTP_CLIENT: OkHttpClient =
    OkHttpClient
        .Builder()
        .connectTimeout(90, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .sslSocketFactory(
            SSLUtils.createLenientSSLContext().socketFactory,
            SSLUtils.createLenientTrustManager()[0] as javax.net.ssl.X509TrustManager,
        ).hostnameVerifier(SSLUtils.createLenientHostnameVerifier())
        .build()

private val CLIENT: OkHttpClient get() = HTTP_CLIENT

/** Fallback Browser — Android/iOS: WebView solver qua bridge; desktop JVM: Playwright reflection. */
internal actual fun browserFetchHtml(
    url: String?,
    log: EngineLogger,
): String? {
    if (url == null) return null
    // Android/iOS — UI layer đăng ký WebView solver lúc khởi động
    BrowserSolverBridge.solver?.let { solver ->
        return try {
            solver(url, 120_000)
        } catch (e: Exception) {
            log.add("[Http] WebView solver failed: ${e.message}")
            null
        }
    }
    // Desktop JVM — Playwright reflection như Http.java bản gốc
    return try {
        val browserClass = Class.forName("dev.haipham22.leechtext.plugin.js.api.Browser")
        val browser = browserClass.getMethod("create").invoke(null)
        val launchMethod = browserClass.getMethod("launch", String::class.java, Int::class.javaPrimitiveType)
        // 60s — đủ thời gian user bấm Turnstile trong cửa sổ Chrome headful
        val doc = launchMethod.invoke(browser, url, 60_000)
        val closeMethod = browserClass.getMethod("close")
        try {
            (doc as? JSDocument)?.html()
        } finally {
            closeMethod.invoke(browser)
        }
    } catch (e: Exception) {
        log.add("[Http] Browser fallback failed: ${e.message}")
        null
    }
}

internal actual fun sleepMs(ms: Long) {
    Thread.sleep(ms)
}
