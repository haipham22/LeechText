package dev.haipham22.leechtext.plugin.js.api

import com.fleeksoft.ksoup.Ksoup
import dev.haipham22.leechtext.EngineConfig
import dev.haipham22.leechtext.log.EngineLogger
import dev.haipham22.leechtext.util.CookiesUtils

/**
 * HTTP API cho JS plugins (port từ Http.java; P5.2c common — network qua HttpEngine seam:
 * JVM=OkHttp, apple=NSURLSession). Fluent: http.get(url).string() / .html() / .json().
 * 1 Http instance = 1 request — response + body cache sau execute() đầu tiên.
 */
@Suppress("TooManyFunctions") // API surface cho plugin JS — số hàm mirror đúng API vBook, không tách
class Http(
    private val log: EngineLogger,
) {
    private var specUrl: String? = null
    private var url: String? = null
    private var method = "GET"
    private var headers = LinkedHashMap<String, String>()
    private var requestBody: ByteArray? = null
    private var bodyContentType: String? = null
    private var syncCookie = true

    // Cache result: 1 Http instance = 1 request — ok().html() không re-execute (chống double
    // network call + góp phần tránh 503 burst)
    private var resultCache: HttpResult? = null

    /** Request thay đổi → bỏ cache. */
    private fun invalidate() {
        resultCache = null
    }

    fun request(url: String): Http {
        this.url = url
        this.specUrl = url
        this.headers = LinkedHashMap()
        invalidate()
        return this
    }

    fun get(url: String): Http = request(url).method("GET")

    fun post(url: String): Http = request(url).method("POST")

    fun put(url: String): Http = request(url).method("PUT")

    fun delete(url: String): Http = request(url).method("DELETE")

    fun patch(url: String): Http = request(url).method("PATCH")

    fun head(url: String): Http = request(url).method("HEAD")

    fun method(method: String): Http {
        this.method = method.uppercase()
        invalidate()
        return this
    }

    fun headers(headers: Any?): Http {
        if (headers is Map<*, *>) {
            for ((k, v) in headers) {
                if (k is String && v is String) this.headers[k] = v
            }
        }
        return this
    }

    fun header(
        key: String,
        value: String,
    ): Http {
        headers[key] = value
        return this
    }

    fun body(body: String?): Http {
        requestBody = (body ?: "").encodeToByteArray()
        bodyContentType = "application/json; charset=utf-8"
        return this
    }

    fun params(params: Any?): Http {
        if (params is Map<*, *>) {
            val form = ArrayList<Pair<String, String>>()
            for ((k, v) in params) {
                if (k is String) form.add(k to (v?.toString() ?: ""))
            }
            applyForm(form)
        }
        return this
    }

    fun param(
        key: String,
        value: String,
    ): Http {
        applyForm(listOf(key to value))
        return this
    }

    /** Form body: key=value&... (application/x-www-form-urlencoded). */
    private fun applyForm(pairs: List<Pair<String, String>>) {
        val encoded = pairs.joinToString("&") { (k, v) ->
            urlFormEncode(k.encodeToByteArray()) + "=" + urlFormEncode(v.encodeToByteArray())
        }
        requestBody = encoded.encodeToByteArray()
        bodyContentType = "application/x-www-form-urlencoded"
        invalidate()
    }

    fun form(data: Any?): Http = params(data)

    fun queries(params: Any?): Http {
        if (params is Map<*, *>) {
            val current = url ?: return this
            val pairs =
                params.entries.mapNotNull { (k, v) ->
                    if (k is String) "$k=${urlFormEncode((v?.toString() ?: "").encodeToByteArray())}" else null
                }
            if (pairs.isNotEmpty()) {
                url = current + (if (current.contains("?")) "&" else "?") + pairs.joinToString("&")
                specUrl = url
                invalidate()
            }
        }
        return this
    }

    fun data(params: Any?): Http = params(params)

    @Suppress("UnusedParameter") // vBook API compat — timeout toàn cục lo
    fun timeout(ms: Int): Http {
        // Timeout toàn cục từ EngineConfig.TIMEOUT_MS (setting.json time_out)
        return this
    }

    fun syncCookie(sync: Boolean): Http {
        this.syncCookie = sync
        return this
    }

    /** Fetch + parse HTML. Usage: http.get(url).html(). Cloudflare challenge → Browser pass
     * (desktop) + sync clearance cookie → retry. Ưu tiên server HTML: plugin selector viết
     * theo server markup, DOM browser render có thể khác (SPA hydrate đổi class). */
    fun html(): JSDocument = try {
        var bodyText = cachedBody().decodeToString()
        if (isCloudflareChallenge(bodyText)) {
            log.add("[Http.html()] Cloudflare challenge detected — fallback Browser")
            bodyText = browserFetchHtml(url, log) ?: bodyText
            invalidate()
            val retried = runCatching { cachedBody().decodeToString() }.getOrNull()
            when {
                // <500B = response rỗng/lỗi — không tính là content (CF 403 rỗng body)
                retried != null && retried.length >= 500 && !isCloudflareChallenge(retried) -> {
                    bodyText = retried
                }

                isCloudflareChallenge(bodyText) -> {
                    log.add("[Http.html()] Vẫn challenge sau Browser fallback + retry")
                }
            }
        }
        log.add("[Http.html()] Response length: ${bodyText.length} bytes")
        JSDocument(Ksoup.parse(bodyText, url ?: ""))
    } catch (e: Exception) {
        log.add("[Http.html()] Failed to get HTML document: ${e.message}")
        JSDocument(Ksoup.parse("", ""))
    }

    /** Detect Cloudflare challenge page — marker mạnh. "cf-turnstile" KHÔNG dùng:
     * widget turnstile nhúng cả ở footer trang thật → false positive. */
    private fun isCloudflareChallenge(body: String): Boolean {
        if (body.length < 500) return false
        return body.contains("challenge-platform") ||
            body.contains("cf_chl_opt") ||
            body.contains("Just a moment...") ||
            body.contains("__cf_chl_jschl_tk__")
    }

    /** Result 1 lần rồi cache — các lần gọi sau (ok, html, json, statusCode) dùng lại. */
    private fun executedResult(): HttpResult {
        resultCache?.let { return it }
        return execute().also { resultCache = it }
    }

    private fun cachedBody(): ByteArray = executedResult().body

    fun document(): JSDocument = html()

    /** Fetch body as string, strip UTF-8 BOM. */
    fun string(): String = try {
        var text = cachedBody().decodeToString()
        if (text.startsWith("\uFEFF")) text = text.substring(1)
        text
    } catch (e: Exception) {
        log.add("Failed to get response body: ${e.message}")
        ""
    }

    fun bytes(): ByteArray = try {
        cachedBody()
    } catch (e: Exception) {
        log.add("Failed to get response bytes: ${e.message}")
        ByteArray(0)
    }

    /** Fetch + parse JSON; HTML response được wrap {chap_list, status} (giữ bản gốc). */
    fun json(): Any? {
        return try {
            val text = string()
            if (text.isEmpty()) return null
            val parsed = Json(log).parse(text)

            if (parsed is String) {
                val trimmed = text.trim()
                if (trimmed.startsWith("<!DOCTYPE html>") ||
                    trimmed.startsWith("<html") ||
                    trimmed.startsWith("<HTML")
                ) {
                    log.add("[Http.json()] Wrapped HTML with chap_list")
                    return JsValueBridge.objectOf(listOf("chap_list" to parsed, "status" to 200))
                }
            }
            parsed
        } catch (e: Exception) {
            log.add("[Http.json()] Failed to parse JSON: ${e.message}")
            null
        }
    }

    fun statusCode(): Int = try {
        executedResult().code
    } catch (e: Exception) {
        -1
    }

    fun ok(): Boolean = statusCode() in 200..399

    fun statusMessage(): String = try {
        executedResult().message
    } catch (e: Exception) {
        ""
    }

    fun responseHeaders(): Map<String, String> = try {
        executedResult().headers
    } catch (e: Exception) {
        emptyMap()
    }

    fun header(name: String): String? = try {
        executedResult().headers[name]
    } catch (e: Exception) {
        null
    }

    fun contentType(): String? = header("Content-Type")

    private fun execute(): HttpResult = executeSpec(
        method,
        checkNotNull(url) { "request() chưa được gọi" },
        headers,
        requestBody,
        bodyContentType,
        syncCookie,
        log,
    )

    internal companion object {
        /**
         * Thực thi 1 spec: default UA + cookie sync + retry 429/503 (backoff).
         * Dùng chung Http instance (common) và sandbox binding QuickJS (apple).
         */
        @Suppress("LongParameterList") // tham số log DI + params nghiệp vụ — bundle làm code khó đọc
        internal fun executeSpec(
            method: String,
            url: String,
            headers: Map<String, String>,
            body: ByteArray?,
            bodyContentType: String?,
            syncCookie: Boolean = true,
            log: EngineLogger,
        ): HttpResult {
            var result = executeOnce(method, url, headers, body, bodyContentType, syncCookie, log)

            // Retry 429/503 — site truyện (Cloudflare) rate-limit khi fetch burst
            // ponytail: RECONN retry × backoff 500ms/1s đủ cho burst TOC
            var attempt = 0
            val maxRetry = EngineConfig.RECONN
            while (result.code == 429 || result.code == 503) {
                if (attempt++ >= maxRetry) break
                sleepMs(500L * attempt)
                log.add("[Http] ${result.code} — retry $attempt cho $url")
                result = executeOnce(method, url, headers, body, bodyContentType, syncCookie, log)
            }
            return result
        }

        @Suppress("LongParameterList") // tham số log DI + params nghiệp vụ — bundle làm code khó đọc
        private fun executeOnce(
            method: String,
            url: String,
            headers: Map<String, String>,
            body: ByteArray?,
            bodyContentType: String?,
            syncCookie: Boolean,
            log: EngineLogger,
        ): HttpResult {
            log.add("[Http.execute()] Executing request: $method $url")

            val allHeaders = LinkedHashMap(headers)
            if (!allHeaders.containsKey("User-Agent")) {
                allHeaders["User-Agent"] = EngineConfig.USER_AGENT
            }
            if (syncCookie) {
                val cookies = CookiesUtils.getCookies(url)
                if (!cookies.isNullOrEmpty()) allHeaders["Cookie"] = cookies
            }

            val result =
                httpExecute(
                    HttpSpec(
                        method = method,
                        url = url,
                        headers = allHeaders,
                        body = body,
                        bodyContentType = bodyContentType,
                    ),
                    log,
                )

            if (syncCookie) {
                val setCookie = result.headers["Set-Cookie"]
                if (!setCookie.isNullOrEmpty()) {
                    CookiesUtils.put(url, setCookie)
                }
            }
            return result
        }
    }
}
