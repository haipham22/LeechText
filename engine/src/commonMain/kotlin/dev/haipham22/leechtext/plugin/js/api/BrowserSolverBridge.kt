package dev.haipham22.leechtext.plugin.js.api

import kotlin.concurrent.Volatile

/**
 * Cầu nối Browser solver cho platform có WebView (Android/iOS) — UI layer
 * (app-shared) đăng ký solver lúc khởi động; engine không phụ thuộc UI.
 *
 * Solver nhận url + timeout, mở WebView/WebKit popup hiện challenge cho user
 * tương tác (hoặc tự pass), trả về HTML cuối khi thoát challenge (hoặc null
 * khi timeout/cancel). Desktop không đăng ký — dùng Playwright như cũ.
 */
object BrowserSolverBridge {
    @Volatile
    var solver: ((url: String, timeoutMs: Long) -> String?)? = null
}
