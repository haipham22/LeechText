@file:Suppress("MatchingDeclarationName")

package dev.haipham22.leechtext.util

import dev.haipham22.leechtext.log.EngineLogger
import dev.haipham22.leechtext.util.zip.ZipAppleCodec
import okio.FileSystem
import okio.Path

/**
 * Apple actual của ZipIo (P5.4) — tự parse ZIP qua [ZipAppleCodec] (central directory +
 * platform.zlib), KHÔNG dependency mới. Behavior mirror actual JVM (zip4j):
 * - entries() lỗi → log + emptyList
 * - readEntry() lỗi/không có entry → log + null
 * - addFile()/addFolder() file/dir chưa tồn tại → bỏ qua im lặng; entry trùng tên → bản mới thắng
 *
 * Ghi = merge: đọc nguyên zip cũ (giữ nguyên raw bytes entry cũ, không nén lại) + entry mới
 * → ghi lại file qua .tmp + atomicMove. Gọi theo đợt (EPUB ~7 lần ZipIo/export) nên chi phí
 * đọc-ghi-lại chấp nhận được.
 */
@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
actual object ZipIo {
    private val fs: FileSystem get() = FileSystem.SYSTEM

    private const val METHOD_STORED = 0
    private const val METHOD_DEFLATE = 8

    actual fun entries(
        zip: Path,
        log: EngineLogger,
    ): List<ZipEntryInfo> = try {
        loadEntries(zip).map { ZipEntryInfo(it.name, it.size.toLong(), it.isDirectory) }
    } catch (e: Exception) {
        log.add("Error listing zip entries: $e")
        emptyList()
    }

    actual fun readEntry(
        zip: Path,
        entryPath: String,
        log: EngineLogger,
    ): ByteArray? = try {
        val bytes = fs.read(zip) { readByteArray() }
        val entry = ZipAppleCodec.parseCentralDirectory(bytes).firstOrNull { it.name == entryPath } ?: return null
        if (entry.isDirectory) return null
        contentOf(bytes, entry).also { checkCrc(it, entry) }
    } catch (e: Exception) {
        log.add("Error reading from zip: $e")
        null
    }

    @Suppress("LongParameterList") // zip + file + tùy chọn nén + log DI — mirror seam expect
    actual fun addFile(
        zip: Path,
        file: Path,
        rootFolder: String,
        level: Int,
        log: EngineLogger,
    ) {
        val meta = fs.metadataOrNull(file) ?: return
        if (!meta.isRegularFile) return
        val content = fs.read(file) { readByteArray() }
        val name = joinName(rootFolder, file.name)
        appendEntries(zip, listOf(ZipAppleCodec.prepare(name, content, level)), log)
    }

    @Suppress("LongParameterList") // zip + dir + tùy chọn nén + log DI — mirror seam expect
    actual fun addFolder(
        zip: Path,
        dir: Path,
        rootFolder: String,
        level: Int,
        log: EngineLogger,
    ) {
        // Thư mục chưa tồn tại = không có nội dung (vd Images/ khi chưa tải ảnh chương)
        // — bỏ qua im lặng, không phải lỗi (mirror actual JVM)
        if (fs.metadataOrNull(dir)?.isDirectory != true) return
        val prefix = joinName(rootFolder, dir.name)
        val entries = ArrayList<ZipAppleCodec.Entry>().apply { add(ZipAppleCodec.dirEntry(prefix)) }
        fs.listRecursively(dir)
            .filter { fs.metadataOrNull(it)?.isRegularFile == true }
            .sorted()
            .forEach { rel ->
                val content = fs.read(rel) { readByteArray() }
                val name = joinName(prefix, rel.relativeTo(dir).toString())
                entries.add(ZipAppleCodec.prepare(name, content, level))
            }
        appendEntries(zip, entries, log)
    }

    // ---------- nội bộ ----------

    /** Đọc zip → entry list với raw data (existing) — file chưa có/rỗng/hỏng = list rỗng. */
    private fun loadEntries(zip: Path): List<ZipAppleCodec.Entry> {
        val meta = fs.metadataOrNull(zip) ?: return emptyList()
        val size = meta.size ?: 0L
        if (size == 0L) return emptyList()
        val bytes = fs.read(zip) { readByteArray() }
        return ZipAppleCodec.parseCentralDirectory(bytes).onEach { it.data = ZipAppleCodec.readLocalData(bytes, it) }
    }

    /** Nội dung gốc của entry: STORED giữ nguyên, DEFLATE raw-inflate theo size trong CD. */
    private fun contentOf(
        bytes: ByteArray,
        entry: ZipAppleCodec.Entry,
    ): ByteArray = when (entry.method) {
        METHOD_STORED -> ZipAppleCodec.readLocalData(bytes, entry)
        METHOD_DEFLATE -> ZipAppleCodec.inflateRaw(ZipAppleCodec.readLocalData(bytes, entry), entry.size)
        else -> error("Unsupported zip method ${entry.method}: ${entry.name}")
    }

    private fun checkCrc(
        content: ByteArray,
        entry: ZipAppleCodec.Entry,
    ) {
        val crc = ZipAppleCodec.crc32(content)
        check(crc == entry.crc) { "CRC mismatch ${entry.name}: $crc != ${entry.crc}" }
    }

    /** Merge entry mới vào zip (ghi lại toàn bộ qua .tmp + atomicMove). */
    private fun appendEntries(
        zipPath: Path,
        newEntries: List<ZipAppleCodec.Entry>,
        log: EngineLogger,
    ) {
        val merged = LinkedHashMap<String, ZipAppleCodec.Entry>()
        loadEntries(zipPath).forEach { merged[it.name] = it }
        newEntries.forEach { merged[it.name] = it } // trùng tên → bản mới thắng
        val out = ZipAppleCodec.writeZip(merged.values.toList())
        zipPath.parent?.let { fs.createDirectories(it) }
        val tmp = (zipPath.parent ?: zipPath) / (zipPath.name + ".tmp")
        try {
            fs.write(tmp) { write(out) }
            fs.atomicMove(tmp, zipPath)
        } catch (e: Exception) {
            runCatching { if (fs.exists(tmp)) fs.delete(tmp) }
            log.add("Error writing zip $zipPath: $e")
        }
    }

    /** Tên entry trong zip: rootFolder + "/" + tên (separator "/" bất kể platform). */
    private fun joinName(
        rootFolder: String,
        name: String,
    ): String = if (rootFolder.isEmpty()) {
        name
    } else {
        rootFolder.trimEnd('/') + "/" + name
    }
}
