package dev.haipham22.leechtext.ui.model

/** Callbacks panel queue — gom nhóm giảm số param (S107). */
data class QueuePanelCallbacks(
    val onRetry: (QueueItemUi) -> Unit = {},
    val onDismiss: (QueueItemUi) -> Unit = {},
    val onClearAll: () -> Unit = {},
    val onOpenBook: (QueueItemUi) -> Unit = {},
    val onPause: (QueueItemUi) -> Unit = {},
    val onResume: (QueueItemUi) -> Unit = {},
)
