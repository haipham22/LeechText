package dev.haipham22.leechtext.ui

import androidx.compose.runtime.Composable

/** Desktop không có nút back hệ thống — no-op. enabled/onBack: Android BackHandler dùng. */
@Suppress("UNUSED_PARAMETER") // parity expect/actual
@Composable
actual fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) = Unit
