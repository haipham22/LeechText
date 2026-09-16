package dev.haipham22.leechtext.ui

/** Mở URL ngoài app (link release...). Desktop: browser mặc định; Android: Intent ACTION_VIEW. */
expect fun openUrl(url: String)
