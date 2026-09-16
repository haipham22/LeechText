package dev.haipham22.leechtext.util

import dev.haipham22.leechtext.log.EngineLogger
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

/**
 * File IO extension (port từ util/FileUtils.java, Kotlin-idiomatic). P5.2b: okio.Path/
 * FileSystem thay java.io.File (KMP). Bỏ legacy static-wrapper: mkdir/add2file/copyFile/
 * cutFile/url2file — dùng FileSystem API trực tiếp.
 */

private val fs: FileSystem get() = FileSystem.SYSTEM

/** Đọc file text; null nếu không tồn tại/lỗi (thay file2string). */
fun Path.readTextOrNull(charset: String = "UTF-8"): String? = try {
    if (!fs.exists(this)) null else decodeBytes(fs.read(this) { readByteArray() }, charset)
} catch (e: Exception) {
    null
}

/** Ghi string ra file (tạo parent dir), UTF-8 mặc định (thay string2file). */
fun String.writeTo(
    path: String,
    charset: String = "UTF-8",
    log: EngineLogger,
) {
    val bytes = encodeBytes(this, charset) ?: return // charset không hợp lệ — bỏ qua như bản gốc
    bytes.writeTo(path, log)
}

/** Ghi bytes ra file (tạo parent dir) (thay byte2file). */
fun ByteArray.writeTo(
    path: String,
    log: EngineLogger,
) {
    try {
        val p = path.toPath()
        p.parent?.let { fs.createDirectories(it) }
        fs.write(p) { write(this@writeTo) }
        // DEBUG 260903 epub-0byte: xác nhận ghi xong thật + size (chỉ khi debug bật)
        log.debug("[writeTo] OK path=$path bytes=${this@writeTo.size}")
    } catch (e: Exception) {
        log
            .add("[writeTo] FAIL path=$path — ${e.message}")
    }
}

/** Xóa file, log central qua EngineLog nếu fail — không ném (SonarQube S899). */
fun Path.deleteLogged(
    tag: String,
    log: EngineLogger,
): Boolean = try {
    if (!fs.exists(this)) {
        true
    } else {
        fs.delete(this)
        true
    }
} catch (e: Exception) {
    log
        .add("[$tag] FAIL delete path=$this — ${e.message}")
    false
}

/** Đọc classpath/bundled resource UTF-8 (vd "/dark/leech/res/toc.ncx"); "" nếu thiếu
 *  (thay stream2string). JVM: classpath; Apple: "" — chưa bundle resource (P5.2c). */
expect fun readResource(path: String): String

/** String → bytes theo tên charset; null nếu charset không hỗ trợ (caller tự fallback). */
internal expect fun encodeBytes(
    text: String,
    charsetName: String,
): ByteArray?

/** Bytes → String theo tên charset; null nếu charset không hỗ trợ. */
internal expect fun decodeBytes(
    bytes: ByteArray,
    charsetName: String,
): String?
