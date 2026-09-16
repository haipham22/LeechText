package dev.haipham22.leechtext.ui

import android.app.Activity

/** Android: immersive fullscreen — ẩn status + nav bars, swipe mép hiện tạm. */
actual fun togglePlatformFullscreen() {
    val activity = AppContext.instance as? Activity ?: return
    val window = activity.window
    val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
    val immersive =
        androidx.core.view.WindowInsetsCompat.Type.systemBars()
    if (insetsController.systemBarsBehavior ==
        androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    ) {
        insetsController.show(immersive)
        insetsController.systemBarsBehavior =
            androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
    } else {
        insetsController.systemBarsBehavior =
            androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(immersive)
    }
}
