package dev.haipham22.leechtext.plugin.js.api

import com.fleeksoft.ksoup.Ksoup
import dev.haipham22.leechtext.EngineConfig
import dev.haipham22.leechtext.log.EngineLogger
import dev.haipham22.leechtext.util.CookiesUtils
import dev.haipham22.leechtext.util.EnginePaths
import dev.haipham22.leechtext.util.LeechJson
import kotlinx.serialization.Serializable

/**
 * Browser API (vBook extension-api) — Playwright headless Chromium.
 * Hỗ trợ: launch (sync/async), waitUrl (network intercept), block, urls, callJs,
 * getVariable, html, setUserAgent, close. Pass Cloudflare 5s challenge.
 *
 * Desktop JVM only — Android/web sẽ thấy lỗi rõ ràng (D1 trong checklist).
 */
@Suppress("TooManyFunctions") // API surface cho plugin JS — số hàm mirror đúng API vBook, không tách
class Browser private constructor(
    private val log: EngineLogger,
) {
    private var playwright: com.microsoft.playwright.Playwright? = null
    private var browserInstance: com.microsoft.playwright.Browser? = null
    private var context: com.microsoft.playwright.BrowserContext? = null
    private var page: com.microsoft.playwright.Page? = null
    private val blockedUrls = mutableListOf<String>()
    private val interceptedUrls = mutableListOf<String>()
    private var userAgent: String? = null

    companion object {
        /** Playwright instance dùng chung — đắt (tải browser lần đầu). */
        @Volatile
        private var sharedPlaywright: com.microsoft.playwright.Playwright? = null

        private fun playwright(log: EngineLogger): com.microsoft.playwright.Playwright {
            if (sharedPlaywright == null) {
                synchronized(this) {
                    if (sharedPlaywright == null) {
                        log.add("[Browser] Starting Playwright (first run downloads Chromium)...")
                        sharedPlaywright =
                            com.microsoft.playwright.Playwright
                                .create()
                    }
                }
            }
            return sharedPlaywright!!
        }

        /** Logger-less entry cho reflection (Engine.newBrowser / Http fallback Browser) —
         * dùng backend platform y hệt facade EngineLog cũ. */
        @JvmStatic
        fun create(): Browser = create(dev.haipham22.leechtext.log.platformEngineLogger())

        @JvmStatic
        fun create(log: EngineLogger): Browser = Browser(log)

        /** Xóa automation artifact mà Cloudflare check (webdriver, plugins, chrome.runtime). */
        private const val STEALTH_JS = """
            Object.defineProperty(navigator, 'webdriver', {get: () => undefined});
            Object.defineProperty(navigator, 'plugins', {get: () => [1, 2, 3, 4, 5]});
            Object.defineProperty(navigator, 'languages', {get: () => ['vi-VN', 'vi', 'en-US', 'en']});
            window.chrome = window.chrome || {runtime: {}};
        """

        private fun cookieFile() = java.io.File(EnginePaths.dataDir.toFile(), "tools/browser-cookies.json")

        /** Persist cookie browser xuống disk — clearance CF còn hạn thì lần sau
         * challenge tự clear, user không phải bấm lại. */
        private fun saveCookies(cookies: List<com.microsoft.playwright.options.Cookie>) {
            try {
                val f = cookieFile()
                f.parentFile?.mkdirs()
                f.writeText(LeechJson.encodeToString(cookies.map { it.toDto() }))
            } catch (_: Exception) {
                // Persist cookie là best-effort — fail (disk đầy, permission) không làm fail flow chính
            }
        }

        /** Nạp lại cookie đã persist vào context mới. */
        private fun restoreCookies(
            context: com.microsoft.playwright.BrowserContext,
            log: EngineLogger,
        ) {
            try {
                val f = cookieFile()
                if (!f.exists()) return
                val cookies = LeechJson.decodeFromString<List<CookieDto>>(f.readText()).map { it.toCookie() }
                if (cookies.isNotEmpty()) {
                    context.addCookies(cookies)
                    log.add("[Browser] Restored ${cookies.size} cookies từ disk")
                }
            } catch (_: Exception) {
                // Cookie cache hỏng/không parse được thì bỏ qua — mở context mới không cookie
            }
        }
    }

    private fun ensurePage(): com.microsoft.playwright.Page {
        if (page == null) {
            val pw = playwright(log)
            val launchOpts =
                com.microsoft.playwright.BrowserType
                    .LaunchOptions()
                    // Headful — CF managed challenge loop vô hạn với headless (dù Chrome thật
                    // + stealth). Cửa sổ hiện ~5-10s/lần cho tới khi cookie clearance được sync.
                    .setHeadless(false)
            // Chrome thật (channel) pass CF tốt hơn Chromium của Playwright.
            // ponytail: detect theo macOS path; thêm Linux/Windows khi port platform khác.
            if (java.io.File("/Applications/Google Chrome.app").exists()) {
                launchOpts.setChannel("chrome")
            }
            browserInstance = pw.chromium().launch(launchOpts)
            val ctxOptions =
                com.microsoft.playwright.Browser
                    .NewContextOptions()
            // CF gắn clearance cookie với UA — dùng cùng UA với OkHttp để cookie tái sử dụng được
            ctxOptions.setUserAgent(userAgent ?: EngineConfig.USER_AGENT)
            context = browserInstance!!.newContext(ctxOptions)
            restoreCookies(context!!, log)
            // Stealth tối thiểu — CF check các navigator artifact của automation
            context!!.addInitScript(STEALTH_JS)
            // Network intercept — track mọi request + abort blocked
            context!!.route("**/*") { route ->
                val url = route.request().url()
                interceptedUrls.add(url)
                if (blockedUrls.any { url.contains(it) }) {
                    route.abort()
                } else {
                    route.resume()
                }
            }
            page = context!!.newPage()
            log.add("[Browser] Page ready (Chrome headful)")
        }
        return page!!
    }

    /** Navigate + đợi load. timeout ms (default 10s). Trả JSDocument. */
    fun launch(
        url: String,
        timeout: Int = 10_000,
    ): Any? {
        try {
            val p = ensurePage()
            p.navigate(
                url,
                com.microsoft.playwright.Page
                    .NavigateOptions()
                    .setWaitUntil(com.microsoft.playwright.options.WaitUntilState.NETWORKIDLE),
            )
            log.add("[Browser] Launched: $url")
            waitForChallengeClear(timeout.toLong())
            return html(timeout)
        } catch (e: Exception) {
            log.add("[Browser] Launch failed: ${e.message}")
            return null
        }
    }

    /**
     * Đợi Cloudflare challenge tự clear — trang challenge cũng NETWORKIDLE (script load
     * xong rồi chờ ~5s reload), nên lấy content ngay là lấy nhầm "Just a moment...".
     * Cửa sổ headful hiện ra cho user bấm Turnstile nếu cần. Poll tới khi hết marker
     * challenge (2 lần sạch liên tiếp — tránh bắt trang transition) hoặc hết timeout.
     */
    private fun waitForChallengeClear(timeoutMs: Long) {
        val deadline = System.currentTimeMillis() + timeoutMs
        var cleanPolls = 0
        while (System.currentTimeMillis() < deadline) {
            val content =
                try {
                    page?.content() ?: return
                } catch (_: Exception) {
                    // Challenge đang reload page (Playwright: "Execution context was
                    // destroyed") — chưa clean, poll tiếp thay vì return nhầm challenge HTML
                    cleanPolls = 0
                    Thread.sleep(500)
                    continue
                }
            // Trang transition (<2KB) chưa phải content — tính là chưa clean
            // ponytail: heuristic theo size; nếu có site page thật <2KB thì đổi sang wait selector
            cleanPolls = if (isChallengePage(content) || content.length < 2000) 0 else cleanPolls + 1
            if (cleanPolls >= 2) return
            Thread.sleep(500)
        }
        log.add("[Browser] Challenge chưa clear sau ${timeoutMs}ms")
    }

    private fun isChallengePage(content: String): Boolean = content.contains("challenge-platform") || content.contains("cf_chl_opt") ||
        content.contains("Just a moment...") || content.contains("__cf_chl_jschl_tk__")

    /** Navigate async — không đợi load, dùng kèm waitUrl + html. */
    fun launchAsync(url: String) {
        try {
            val p = ensurePage()
            p.navigate(
                url,
                com.microsoft.playwright.Page
                    .NavigateOptions()
                    .setWaitUntil(com.microsoft.playwright.options.WaitUntilState.COMMIT),
            )
            log.add("[Browser] LaunchAsync: $url")
        } catch (e: Exception) {
            log.add("[Browser] LaunchAsync failed: ${e.message}")
        }
    }

    /** Đợi network request match 1 trong patterns. timeout ms. Trả true nếu match. */
    fun waitUrl(
        patterns: Array<String>,
        timeout: Int = 15_000,
    ): Boolean = try {
        val p = ensurePage()
        val patternRegex = patterns.joinToString("|") { Regex.escape(it).replace("\\*", ".*") }
        val javaPattern =
            java.util.regex.Pattern
                .compile(patternRegex)
        p.waitForRequest(
            javaPattern,
            com.microsoft.playwright.Page
                .WaitForRequestOptions()
                .setTimeout(timeout.toDouble()),
            null,
        )
        true
    } catch (e: Exception) {
        log.add("[Browser] waitUrl timeout: ${e.message}")
        false
    }

    /** Lấy HTML hiện tại. timeout ms. Trả JSDocument. */
    @Suppress("UnusedParameter") // vBook API compat — timeout toàn cục lo
    fun html(timeout: Int = 5_000): Any? = try {
        val p = ensurePage()
        val content = p.content()
        log.add("[Browser] HTML: ${content.length} bytes")
        JSDocument(Ksoup.parse(content, p.url()))
    } catch (e: Exception) {
        log.add("[Browser] html failed: ${e.message}")
        JSDocument(Ksoup.parse("", ""))
    }

    /** Thực thi JS trong page, trả kết quả. */
    @Suppress("UnusedParameter") // vBook API compat
    fun callJs(
        script: String,
        timeout: Int = 5_000,
    ): Any? = try {
        val p = ensurePage()
        val result = p.evaluate(script)
        log.add("[Browser] callJs OK: ${result?.toString()?.take(80)}")
        result
    } catch (e: Exception) {
        log.add("[Browser] callJs failed: ${e.message}")
        null
    }

    /** Lấy giá trị JS variable (vd "window.__TOKEN__"). */
    fun getVariable(
        name: String,
        timeout: Int = 5_000,
    ): Any? = callJs("() => { try { return $name; } catch(e) { return null; } }", timeout)

    /** Chặn URLs chứa pattern (ads, tracker...). Áp cho request sau này. */
    fun block(urls: Array<String>) {
        blockedUrls.addAll(urls)
        log.add("[Browser] Blocked: ${urls.joinToString()}")
    }

    /** URLs đã intercept (sau khi block áp dụng). */
    fun urls(): Array<String> = interceptedUrls.toTypedArray()

    /** Set User-Agent cho request tiếp theo (phải gọi trước launch). */
    fun setUserAgent(ua: String): Browser {
        userAgent = ua
        return this
    }

    /** URL hiện tại của page. */
    fun url(): String = try {
        page?.url() ?: ""
    } catch (e: Exception) {
        ""
    }

    /** Title hiện tại. */
    fun title(): String = try {
        page?.title() ?: ""
    } catch (e: Exception) {
        ""
    }

    /** Đóng browser (giữ Playwright shared). Sync + persist cookie trước khi đóng. */
    fun close() {
        syncCookies()
        try {
            context?.cookies()?.let { saveCookies(it) }
        } catch (_: Exception) {
            // Browser có thể đã chết — lấy cookie lúc close fail thì bỏ qua, không cứu được nữa
        }
        try {
            context?.close()
            browserInstance?.close()
        } catch (e: Exception) {
            // ignore
        }
        page = null
        context = null
        browserInstance = null
        log.add("[Browser] Closed")
    }

    fun isActive(): Boolean = page != null

    /**
     * Đưa cookie browser (cf_clearance, __cf_bm...) về CookiesUtils — request OkHttp sau
     * đó pass Cloudflare luôn, không phải spin Chromium mỗi request (fetch cả trăm chương).
     * ponytail: filter đơn giản theo host suffix; bỏ qua path/expiry nếu CF đổi chính sách.
     */
    private fun syncCookies() {
        try {
            val pageUrl = page?.url() ?: return
            if (pageUrl == "about:blank") return
            val host = java.net.URI(pageUrl).host ?: return
            val header =
                (context?.cookies() ?: return)
                    .filter { c ->
                        val d = c.domain.removePrefix(".")
                        d == host || host.endsWith(".$d")
                    }.joinToString("; ") { "${it.name}=${it.value}" }
            if (header.isNotEmpty()) {
                CookiesUtils.put(pageUrl, header)
                log.add("[Browser] Synced ${header.length} chars cookies → $host")
            }
        } catch (_: Exception) {
            // Sync cookie best-effort — fail thì flow chính vẫn chạy, request sau tự challenge lại
        }
    }
}

/** DTO serialize cookie — Playwright Cookie là class bên thứ 3, không annotate được. */
@Serializable
private data class CookieDto(
    val name: String = "",
    val value: String = "",
    val domain: String = "",
    val path: String = "",
    val expires: Double = -1.0,
    val httpOnly: Boolean = false,
    val secure: Boolean = false,
    val sameSite: String? = null,
)

private fun com.microsoft.playwright.options.Cookie.toDto() = CookieDto(
    name = name,
    value = value,
    domain = domain,
    path = path,
    expires = expires ?: -1.0,
    httpOnly = httpOnly ?: false,
    secure = secure ?: false,
    sameSite = sameSite?.name,
)

private fun CookieDto.toCookie() = com.microsoft.playwright.options
    .Cookie(name, value)
    .setDomain(domain)
    .setPath(path)
    .setExpires(expires)
    .setHttpOnly(httpOnly)
    .setSecure(secure)
    .setSameSite(
        sameSite?.let {
            runCatching { com.microsoft.playwright.options.SameSiteAttribute.valueOf(it) }.getOrNull()
        },
    )
