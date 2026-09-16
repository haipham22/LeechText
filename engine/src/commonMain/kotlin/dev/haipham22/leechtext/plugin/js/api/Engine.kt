package dev.haipham22.leechtext.plugin.js.api

/**
 * Engine factory cho browser automation (port từ Engine.java — Playwright Chromium).
 * P5.2c: newBrowser qua seam platform — JVM (desktop) reflection tới Browser.kt,
 * apple throw lỗi rõ ràng (như Android design D1).
 */
@Suppress("UtilityClassWithPublicConstructor") // vBook binding bind INSTANCE (JsApiSetup)
class Engine {
    companion object {
        /** Tạo Browser instance; platform không hỗ trợ throw UnsupportedOperationException. */
        fun newBrowser(): Any = newBrowserPlatform()

        @Suppress("UnusedParameter") // vBook API compat — Browser singleton không cần context/scope
        fun newBrowser(
            context: Any?,
            scope: Any?,
        ): Any = newBrowser()
    }
}

/** Browser chỉ có desktop JVM (Playwright Chromium) — actual per-platform. */
internal expect fun newBrowserPlatform(): Any
