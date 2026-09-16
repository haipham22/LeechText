package dev.haipham22.leechtext.plugin.js.api

/** Browser (Playwright Chromium) — reflection để Browser.kt (desktop-only) không cần link. */
internal actual fun newBrowserPlatform(): Any = try {
    val browserClass = Class.forName("dev.haipham22.leechtext.plugin.js.api.Browser")
    browserClass.getMethod("create").invoke(null)
} catch (e: ClassNotFoundException) {
    throw UnsupportedOperationException(
        "Browser API chỉ có trên desktop (Playwright Chromium) — Android/iOS không hỗ trợ",
    )
} catch (e: Exception) {
    throw UnsupportedOperationException("Browser API lỗi: ${e.message}")
}
