package dev.haipham22.leechtext.ui

/** Desktop: không có foreground service — queue tải bằng coroutine. */
@Suppress("UNUSED_PARAMETER", "FunctionOnlyReturningConstant") // parity expect/actual — queue coroutine tự mang url/range
actual fun startPlatformDownload(url: String, chapterRange: String?): Boolean = false

actual val IS_DESKTOP_PLATFORM: Boolean = true
actual val IS_IOS: Boolean = false
