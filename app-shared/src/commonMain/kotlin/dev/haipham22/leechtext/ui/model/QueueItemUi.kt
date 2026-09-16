package dev.haipham22.leechtext.ui.model

/**
 * Dữ liệu hiển thị 1 mục queue — thuần UI, tách khỏi QueueItem (jvmMain) qua adapter
 * ở SliceApp. Ad-hoc migration note: khi DownloadQueueState chuyển common thì gộp lại.
 */
data class QueueItemUi(
    val id: String,
    val name: String,
    val status: UiText,
    val completed: Int,
    val total: Int,
    val done: Boolean,
    val failed: Boolean,
    val hasBook: Boolean,
    val paused: Boolean,
    val coverUrl: String? = null,
)
