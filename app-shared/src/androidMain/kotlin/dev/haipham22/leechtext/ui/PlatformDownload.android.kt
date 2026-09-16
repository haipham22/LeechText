package dev.haipham22.leechtext.ui

/**
 * Queue chạy trong ViewModel để giữ progress/error. Foreground service chưa có
 * callback hoàn tất nên không nhận item ở đây.
 * ponytail: dùng service lại khi có callback/persistent worker.
 */
@Suppress("UNUSED_PARAMETER") // parity expect/actual — url/chapterRange cho service sau này
actual fun startPlatformDownload(
    url: String,
    chapterRange: String?,
): Boolean = false

/** Desktop marker — ẩn cài đặt chỉ desktop (Công cụ: Calibre/Kindlegen). */
actual val IS_DESKTOP_PLATFORM: Boolean = false

/** Android: bottom bar chuẩn M3, không floating pill. */
actual val IS_IOS: Boolean = false
