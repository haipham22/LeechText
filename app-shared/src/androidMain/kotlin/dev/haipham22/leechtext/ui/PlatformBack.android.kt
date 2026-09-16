package dev.haipham22.leechtext.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) {
    // Android: BackHandler activity-compose — chặn back, pop trong app
    BackHandler(enabled = enabled, onBack = onBack)
}
