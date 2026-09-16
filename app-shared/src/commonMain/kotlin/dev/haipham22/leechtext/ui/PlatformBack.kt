package dev.haipham22.leechtext.ui

import androidx.compose.runtime.Composable

/**
 * Bắt nút Back hệ thống (Android) — pop màn hình trong app thay vì thoát app.
 * Nền không có back (iOS/desktop) = no-op.
 */
@Composable
expect fun PlatformBackHandler(
    enabled: Boolean = true,
    onBack: () -> Unit,
)
