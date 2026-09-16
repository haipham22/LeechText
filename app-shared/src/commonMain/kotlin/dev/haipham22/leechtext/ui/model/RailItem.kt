package dev.haipham22.leechtext.ui.model

import androidx.compose.ui.graphics.vector.ImageVector

/** 1 mục navigation rail. */
data class RailItem(
    val label: String,
    val icon: ImageVector,
    val badge: Int = 0,
)
