package dev.haipham22.leechtext.plugin.js.api

/** Apple actual — Browser (Playwright) không có trên iOS/macOS engine (design D1). */
internal actual fun newBrowserPlatform(): Any = throw UnsupportedOperationException(
    "Browser API chỉ có trên desktop (Playwright Chromium) — iOS/macOS không hỗ trợ",
)
