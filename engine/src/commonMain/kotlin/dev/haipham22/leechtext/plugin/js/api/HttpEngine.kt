package dev.haipham22.leechtext.plugin.js.api

import dev.haipham22.leechtext.log.EngineLogger

/**
 * HTTP executor seam (P5.2c) — okhttp không có artifact native (5.5.0 chỉ jvm+android).
 * JVM actual: OkHttpClient + lenient SSL + retry; Apple actual: NSURLSession.
 * Blocking — gọi trong Http, mọi caller đã sync.
 */
data class HttpSpec(
    val method: String,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val body: ByteArray? = null,
    val bodyContentType: String? = null,
)

data class HttpResult(
    val code: Int,
    val message: String,
    val headers: Map<String, String> = emptyMap(),
    val body: ByteArray = ByteArray(0),
)

/** Thực thi HTTP request — throw Exception (IO) khi lỗi mạng. */
internal expect fun httpExecute(
    spec: HttpSpec,
    log: EngineLogger,
): HttpResult

/** Fallback Browser (Playwright desktop-only) lấy HTML pass Cloudflare; null nếu không có. */
internal expect fun browserFetchHtml(
    url: String?,
    log: EngineLogger,
): String?

/** Sleep blocking ms (retry backoff). */
internal expect fun sleepMs(ms: Long)
