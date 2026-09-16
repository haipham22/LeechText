package dev.haipham22.leechtext.ui

import dev.haipham22.leechtext.EngineConfig
import dev.haipham22.leechtext.log.EngineLogger
import dev.haipham22.leechtext.plugin.js.api.BrowserSolverBridge
import dev.haipham22.leechtext.util.CookiesUtils
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSHTTPCookie
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.UIKit.UIColor
import platform.UIKit.UIFont
import platform.UIKit.UILabel
import platform.UIKit.UIViewController
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.WKWebsiteDataStore
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.usleep
import kotlin.concurrent.AtomicReference
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/**
 * WebView popup vượt Cloudflare (iOS) — Http detect challenge → solver present
 * 1 VC chứa WKWebView; challenge JS tự chạy, user chạm Turnstile nếu cần.
 * Page sạch 2 lần liên tiếp → lấy outerHTML + sync cookie cf_clearance vào
 * CookiesUtils (OkHttp sau pass luôn). Timeout tự dismiss.
 */
object CloudflareWebViewSolver {

    private val RESULT = AtomicReference<String?>(null)
    private var popupVC: UIViewController? = null

    /** Platform UI — resolve logger từ Koin global context (engine DI). */
    private val log: EngineLogger
        get() = org.koin.mp.KoinPlatformTools.defaultContext().get().get()

    /** Đăng ký solver vào bridge — gọi 1 lần ở MainViewController. */
    fun register(rootProvider: () -> UIViewController?) {
        BrowserSolverBridge.solver = { url, timeoutMs ->
            solve(rootProvider(), url, timeoutMs)
        }
    }

    /** Blocking — gọi từ engine worker thread (KHÔNG phải main). */
    private fun solve(
        root: UIViewController?,
        url: String,
        timeoutMs: Long,
    ): String? {
        if (root == null) return null
        RESULT.value = null

        dispatch_async(dispatch_get_main_queue()) {
            val popup = CloudflarePopupViewController(url) { html ->
                RESULT.value = html
            }
            popupVC = popup
            root.presentViewController(popup, animated = true, completion = null)
        }

        // Poll trên worker thread — main chạy WKWebView độc lập
        val deadline = TimeSource.Monotonic.markNow()
        var html: String? = null
        while (deadline.elapsedNow() < timeoutMs.milliseconds) {
            usleep(500_000u)
            RESULT.value?.let {
                html = it
                break
            }
        }

        // Timeout chưa xong → dismiss popup
        if (html == null) {
            dispatch_async(dispatch_get_main_queue()) {
                popupVC?.dismissViewControllerAnimated(true, completion = null)
            }
            log.add("[CfSolver] Timeout sau ${timeoutMs}ms")
            return null
        }

        // Sync clearance cookies → OkHttp request sau pass CF luôn
        dispatch_async(dispatch_get_main_queue()) {
            popupVC?.dismissViewControllerAnimated(true, completion = null)
        }
        WKWebsiteDataStore
            .defaultDataStore()
            .httpCookieStore
            .getAllCookies { cookies ->
                val header =
                    (cookies ?: emptyList<Any?>())
                        .filterIsInstance<NSHTTPCookie>()
                        .joinToString("; ") { "${it.name}=${it.value}" }
                if (header.isNotEmpty()) {
                    CookiesUtils.put(url, header)
                    log.add("[CfSolver] Synced ${header.length} chars cookies")
                }
            }
        log.add("[CfSolver] HTML: ${html.length} bytes")
        return html
    }
}

/** VC popup: label trạng thái + WKWebView toàn phần dưới label. */
@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
private class CloudflarePopupViewController(
    private val url: String,
    private val onDone: (String?) -> Unit,
) : UIViewController(nibName = null, bundle = null) {

    private var cleanPolls = 0
    private var done = false

    override fun viewDidLoad() {
        super.viewDidLoad()
        view.setBackgroundColor(UIColor.whiteColor)

        val label =
            UILabel(frame = CGRectMake(16.0, 60.0, 280.0, 40.0)).apply {
                text = "Đang vượt Cloudflare — chạm vào ô xác nhận nếu trang yêu cầu…"
                numberOfLines = 2
                font = UIFont.systemFontOfSize(13.0)
            }
        view.addSubview(label)

        val config = WKWebViewConfiguration()
        config.websiteDataStore = WKWebsiteDataStore.defaultDataStore()
        val web =
            WKWebView(
                frame = CGRectMake(16.0, 108.0, 280.0, 500.0),
                configuration = config,
            )
        // Co giãn theo container (rotation, form sheet) — khỏi đọc screen bounds
        web.autoresizingMask =
            platform.UIKit.UIViewAutoresizingFlexibleWidth or platform.UIKit.UIViewAutoresizingFlexibleHeight
        web.setBackgroundColor(UIColor.whiteColor)
        // CF gắn clearance với UA — same UA với OkHttp để cookie tái sử dụng được
        web.customUserAgent = EngineConfig.USER_AGENT
        web.navigationDelegate =
            object : NSObject(), WKNavigationDelegateProtocol {
                override fun webView(
                    webView: WKWebView,
                    didFinishNavigation: WKNavigation?,
                ) {
                    evaluateAndCheck(webView)
                }
            }
        view.addSubview(web)
        web.loadRequest(NSURLRequest(NSURL(string = url)))
    }

    /** Đánh giá HTML hiện tại: sạch 2 lần liên tiếp → hoàn thành, trả HTML. */
    private fun evaluateAndCheck(web: WKWebView) {
        if (done) return
        web.evaluateJavaScript("document.documentElement.outerHTML") { result, _ ->
            if (done) return@evaluateJavaScript
            val html = result as? String ?: return@evaluateJavaScript
            cleanPolls = if (isChallengePage(html) || html.length < 2000) 0 else cleanPolls + 1
            if (cleanPolls >= 2) {
                done = true
                onDone(html)
                dismissViewControllerAnimated(true, completion = null)
            }
        }
    }

    private fun isChallengePage(content: String): Boolean = content.contains("challenge-platform") ||
        content.contains("cf_chl_opt") ||
        content.contains("Just a moment...") ||
        content.contains("__cf_chl_jschl_tk__")
}
