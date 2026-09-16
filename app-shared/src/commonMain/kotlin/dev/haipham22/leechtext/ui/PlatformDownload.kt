package dev.haipham22.leechtext.ui

/**
 * Seam platform cho download (cùng pattern ImageDecoder/ShareFile):
 * desktop tải qua queue coroutine, Android giao foreground service.
 */
expect fun startPlatformDownload(
    url: String,
    chapterRange: String? = null,
): Boolean

/** Desktop marker — ẩn cài đặt chỉ desktop (Công cụ: Calibre/Kindlegen). */
expect val IS_DESKTOP_PLATFORM: Boolean

/** iOS marker — UI khác biệt theo nền (bottom bar nổi kiểu iOS 26 vs chuẩn Android). */
expect val IS_IOS: Boolean
