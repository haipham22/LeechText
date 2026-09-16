package dev.haipham22.leechtext.get

/**
 * Tổng kết sau khi tải xong toàn bộ danh sách chương — thay DownloadListener status machine
 * của bản gốc (caller đọc flags trên Chapter để biết chi tiết từng chương).
 */
data class DownloadSummary(
    val total: Int,
    val resumed: Int,
    val ok: Int,
    val empty: Int,
    val error: Int,
) {
    val completed: Int get() = resumed + ok + empty + error
}
