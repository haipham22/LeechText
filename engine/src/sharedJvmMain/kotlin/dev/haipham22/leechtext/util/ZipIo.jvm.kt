package dev.haipham22.leechtext.util

import dev.haipham22.leechtext.log.EngineLogger
import dev.haipham22.leechtext.log.platformEngineLogger
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.CompressionMethod
import okio.Path
import okio.Path.Companion.toPath
import java.io.File

/**
 * JVM actual của ZipIo (P5.2c) — zip4j (port từ util/ZipUtils.java).
 */
actual object ZipIo {
    actual fun entries(
        zip: Path,
        log: EngineLogger,
    ): List<ZipEntryInfo> = try {
        ZipFile(zip.toFile()).use { zf ->
            zf.fileHeaders.map { h ->
                ZipEntryInfo(
                    name = h.fileName,
                    size = h.uncompressedSize,
                    isDirectory = h.isDirectory,
                )
            }
        }
    } catch (e: Exception) {
        log.add("Error listing zip entries: $e")
        emptyList()
    }

    actual fun readEntry(
        zip: Path,
        entryPath: String,
        log: EngineLogger,
    ): ByteArray? = try {
        val zipFile = ZipFile(zip.toFile())
        val header = zipFile.getFileHeader(entryPath) ?: return null
        if (header.isDirectory) return null
        zipFile.getInputStream(header).use { it.readBytes() }
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
        val f = file.toFile()
        if (!f.exists()) return
        try {
            ZipFile(zip.toFile()).addFile(f, parameters(rootFolder, level))
        } catch (e: Exception) {
            log.add("Error adding file to zip: $e")
        }
    }

    @Suppress("LongParameterList") // zip + dir + tùy chọn nén + log DI — mirror seam expect
    actual fun addFolder(
        zip: Path,
        dir: Path,
        rootFolder: String,
        level: Int,
        log: EngineLogger,
    ) {
        val d = dir.toFile()
        // Thư mục chưa tồn tại = không có nội dung (vd Images/ khi chưa tải ảnh chương)
        // — bỏ qua im lặng, không phải lỗi (log trước hù user 2026-08-27)
        if (!d.isDirectory) return
        try {
            ZipFile(zip.toFile()).addFolder(d, parameters(rootFolder, level))
        } catch (e: Exception) {
            log.add("Error adding folder to zip: $e")
        }
    }

    private fun parameters(
        rootFolder: String,
        level: Int,
    ): ZipParameters = ZipParameters().apply {
        compressionMethod = CompressionMethod.DEFLATE
        compressionLevel =
            when (level) {
                0 -> CompressionLevel.NO_COMPRESSION
                1 -> CompressionLevel.FASTEST
                9 -> CompressionLevel.MAXIMUM
                else -> CompressionLevel.NORMAL
            }
        if (rootFolder.isNotEmpty()) rootFolderNameInZip = rootFolder
    }
}

/** Back-compat top-level cho consumer cũ (File-based) — Ebook export. */
fun addToZip(
    zip: File,
    file: File,
    rootFolder: String = "",
    level: Int = 6,
    log: EngineLogger = platformEngineLogger(),
) = ZipIo.addFile(zip.toString().toPath(), file.toString().toPath(), rootFolder, level, log)

fun addFolderToZip(
    zip: File,
    dir: File,
    rootFolder: String = "",
    level: Int = 6,
    log: EngineLogger = platformEngineLogger(),
) = ZipIo.addFolder(zip.toString().toPath(), dir.toString().toPath(), rootFolder, level, log)

fun readZipEntry(
    zip: File,
    entryPath: String,
    log: EngineLogger = platformEngineLogger(),
): ByteArray = ZipIo.readEntry(zip.toString().toPath(), entryPath, log) ?: ByteArray(0)

fun readZipEntryAsString(
    zip: File,
    entryPath: String,
    log: EngineLogger = platformEngineLogger(),
): String = readZipEntry(zip, entryPath, log).decodeToString()
