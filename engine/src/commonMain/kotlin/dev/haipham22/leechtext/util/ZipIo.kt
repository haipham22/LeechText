package dev.haipham22.leechtext.util

import dev.haipham22.leechtext.log.EngineLogger
import okio.Path

/** Entry zip: tên + uncompressed size (bytes) + directory flag. */
data class ZipEntryInfo(
    val name: String,
    val size: Long,
    val isDirectory: Boolean,
)

/**
 * Zip IO seam (P5.2c) — zip4j chỉ có JVM.
 * JVM actual: zip4j (giữ nguyên behavior). Apple actual: error rõ ràng —
 * ponytail: zip reader/writer zlib cho iOS = P5.4 (plugin install + EPUB export
 * degraded trên iOS, downloads text không ảnh hưởng).
 */
expect object ZipIo {
    /** Danh sách entries (kèm size/flags) — rỗng nếu lỗi. */
    fun entries(
        zip: Path,
        log: EngineLogger,
    ): List<ZipEntryInfo>

    /** Đọc entry; null nếu không có/lỗi. */
    fun readEntry(
        zip: Path,
        entryPath: String,
        log: EngineLogger,
    ): ByteArray?

    /** Thêm 1 file vào zip (bỏ qua nếu file không tồn tại). */
    fun addFile(
        zip: Path,
        file: Path,
        rootFolder: String = "",
        level: Int = 6,
        log: EngineLogger,
    )

    /** Thêm cả thư mục vào zip (vd Text/ của EPUB) — thư mục chưa tồn tại = bỏ qua im lặng. */
    fun addFolder(
        zip: Path,
        dir: Path,
        rootFolder: String = "",
        level: Int = 6,
        log: EngineLogger,
    )
}
