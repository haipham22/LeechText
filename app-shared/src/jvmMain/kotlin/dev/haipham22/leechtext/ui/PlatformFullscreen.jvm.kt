package dev.haipham22.leechtext.ui

/** Desktop: macOS native fullscreen qua EAWT; platform khác maximize. Không dispose window — chết Compose container. */
actual fun togglePlatformFullscreen() {
    val window =
        java.awt.KeyboardFocusManager
            .getCurrentKeyboardFocusManager()
            .focusedWindow as? java.awt.Frame ?: return
    if (!toggleMacFullscreen(window)) {
        window.extendedState =
            if (window.extendedState and java.awt.Frame.MAXIMIZED_BOTH != 0) {
                java.awt.Frame.NORMAL
            } else {
                java.awt.Frame.MAXIMIZED_BOTH
            }
    }
}

private fun toggleMacFullscreen(window: java.awt.Window): Boolean = try {
    val fsUtils = Class.forName("com.apple.eawt.FullScreenUtilities")
    fsUtils.getMethod("setWindowCanFullScreen", java.awt.Window::class.java, Boolean::class.java)
        .invoke(null, window, true)
    val app = Class.forName("com.apple.eawt.Application").getMethod("getApplication").invoke(null)
    Class.forName("com.apple.eawt.Application")
        .getMethod("requestToggleFullScreen", java.awt.Window::class.java)
        .invoke(app, window)
    true
} catch (_: Throwable) {
    false
}
