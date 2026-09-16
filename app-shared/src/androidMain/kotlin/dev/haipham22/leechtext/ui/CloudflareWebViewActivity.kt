package dev.haipham22.leechtext.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import dev.haipham22.leechtext.EngineConfig
import dev.haipham22.leechtext.plugin.js.api.BrowserSolverBridge
import dev.haipham22.leechtext.util.CookiesUtils
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * WebView popup vượt Cloudflare (Android) — Http detect challenge → solver mở
 * activity này; challenge JS tự chạy, user chạm Turnstile nếu cần. Page load
 * sạch 2 lần liên tiếp → lấy outerHTML trả solver + sync cookie vào
 * CookiesUtils (request OkHttp sau pass luôn). Back = cancel. Timeout 120s.
 */
class CloudflareWebViewActivity : Activity() {

    private lateinit var webView: WebView
    private lateinit var url: String
    private val mainHandler = Handler(Looper.getMainLooper())
    private var cleanLoads = 0
    private var done = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        url = intent.getStringExtra(EXTRA_URL) ?: run {
            finish()
            return
        }

        webView =
            WebView(this).apply {
                @SuppressLint("SetJavaScriptEnabled")
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                // CF gắn clearance với UA — same UA với OkHttp để cookie tái sử dụng được
                settings.userAgentString = EngineConfig.USER_AGENT
                // CF challenge cần cookies — webView PHẢI tạo trước khi touch CookieManager
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                webViewClient =
                    object : WebViewClient() {
                        // Giữ navigation trong WebView — challenge reload cần nó
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?,
                        ): Boolean = false

                        // Challenge reload xong → check HTML: sạch 2 lần liên tiếp là pass
                        override fun onPageFinished(view: WebView?, pageUrl: String?) {
                            evaluateAndCheck()
                        }
                    }
            }
        val statusText =
            TextView(this).apply {
                text = "Đang vượt Cloudflare — chạm vào ô xác nhận nếu trang yêu cầu…"
                setTextColor(Color.DKGRAY)
                textSize = 14f
                setPadding(0, 0, 0, 16)
            }
        val card =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.WHITE)
                setPadding(24, 24, 24, 24)
                addView(
                    statusText,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ),
                )
                addView(
                    webView,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1f,
                    ),
                )
            }

        setContentView(
            FrameLayout(this).apply {
                setBackgroundColor(Color.TRANSPARENT)
                addView(
                    card,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        Gravity.CENTER,
                    ).apply { setMargins(24, 96, 24, 96) },
                )
            },
        )

        webView.loadUrl(url)
        mainHandler.postDelayed({ finishWith(null) }, TIMEOUT_MS)
    }

    /** Đánh giá HTML hiện tại: sạch 2 lần liên tiếp → hoàn thành, trả HTML. */
    private fun evaluateAndCheck() {
        if (done) return
        webView.evaluateJavascript("document.documentElement.outerHTML") { result ->
            if (done) return@evaluateJavascript
            val html = decodeHtml(result)
            cleanLoads = if (isChallengePage(html) || html.length < 2000) 0 else cleanLoads + 1
            if (cleanLoads >= 2) {
                finishWith(html)
            }
        }
    }

    private fun finishWith(html: String?) {
        if (done) return
        done = true
        RESULT.set(html)
        ACTIVE_LATCH.get()?.countDown()
        finish()
    }

    private fun isChallengePage(content: String): Boolean = content.contains("challenge-platform") ||
        content.contains("cf_chl_opt") ||
        content.contains("Just a moment...") ||
        content.contains("__cf_chl_jschl_tk__")

    /** evaluateJavascript trả JSON string (quote + escape) — decode gọn. */
    private fun decodeHtml(jsResult: String?): String = jsResult
        ?.trim()
        ?.removeSurrounding("\"")
        ?.replace("\\u003C", "<")
        ?.replace("\\u003c", "<")
        ?.replace("\\\"", "\"")
        ?.replace("\\\\", "\\")
        ?.replace("\\n", "\n")
        ?: ""

    companion object {
        private const val EXTRA_URL = "url"
        private const val TIMEOUT_MS = 120_000L

        /** Platform UI — resolve logger từ Koin global context (engine DI). */
        private val log: dev.haipham22.leechtext.log.EngineLogger
            get() = org.koin.mp.KoinPlatformTools.defaultContext().get().get()

        private val RESULT = AtomicReference<String?>(null)
        private val ACTIVE_LATCH = AtomicReference<CountDownLatch?>()

        /**
         * Blocking — gọi từ engine worker thread (KHÔNG phải main).
         * Single-slot: 1 solve tại 1 thời điểm (flow đọc 1 chương lúc 1 lúc).
         */
        fun solve(
            context: Context,
            url: String,
            timeoutMs: Long,
        ): String? {
            RESULT.set(null)
            val latch = CountDownLatch(1)
            ACTIVE_LATCH.set(latch)
            Handler(Looper.getMainLooper()).post {
                context.startActivity(
                    Intent(context, CloudflareWebViewActivity::class.java)
                        .putExtra(EXTRA_URL, url)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
            val ok = latch.await(timeoutMs + 5_000, TimeUnit.MILLISECONDS)
            val html = RESULT.get()
            if (ok && html != null) {
                // Clearance cookie → OkHttp request sau pass CF luôn (không spin WebView)
                CookieManager.getInstance().getCookie(url)?.let {
                    if (it.isNotEmpty()) {
                        CookiesUtils.put(url, it)
                        log.add("[CfSolver] Synced ${it.length} chars cookies")
                    }
                }
                log.add("[CfSolver] HTML: ${html.length} bytes")
                return html
            }
            log.add("[CfSolver] Timeout/cancel sau ${timeoutMs}ms")
            return null
        }

        /** Đăng ký solver vào bridge — gọi 1 lần ở MainActivity.onCreate. */
        fun register() {
            BrowserSolverBridge.solver = { url, timeoutMs ->
                AppContext.instance?.let { ctx ->
                    solve(ctx, url, timeoutMs)
                }
            }
        }
    }
}
