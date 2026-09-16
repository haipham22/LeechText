package dev.haipham22.leechtext.util

import dev.haipham22.leechtext.log.EngineLogger
import dev.haipham22.leechtext.util.zip.ZipAppleCodec
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSProcessInfo
import platform.posix.X_OK
import platform.posix.access

/**
 * Apple actual — KHÔNG resource bundle trên native. Duy nhất template EPUB cần "resource":
 * build zip tương đương untitled.epub (JVM bundle) bằng ZipAppleCodec — mimetype STORED
 * entry đầu tiên (bắt buộc theo OCF spec) + META-INF/container.xml.
 */
actual fun readResourceBytes(
    path: String,
    log: EngineLogger,
): ByteArray = if (path.endsWith("untitled.epub")) untitledEpubTemplate() else ByteArray(0)

/** EPUB template — mirror engine/src/sharedJvmMain/resources/dark/leech/res/untitled.epub. */
private fun untitledEpubTemplate(): ByteArray {
    val mimetype = ZipAppleCodec.prepare("mimetype", "application/epub+zip".encodeToByteArray(), level = 0)
    val container =
        ZipAppleCodec.prepare("META-INF/container.xml", CONTAINER_XML.encodeToByteArray(), level = 6)
    return ZipAppleCodec.writeZip(listOf(mimetype, container))
}

private const val CONTAINER_XML =
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" +
        "<container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">\r\n" +
        "    <rootfiles>\r\n" +
        "        <rootfile full-path=\"content.opf\" media-type=\"application/oebps-package+xml\"/>\r\n" +
        "   </rootfiles>\r\n" +
        "</container>\r\n"

/** Apple actual — calibre external tool không có. toolPath/args/onLine: JVM actual dùng. */
@Suppress("UNUSED_PARAMETER", "LongParameterList") // parity expect/actual + tham số log DI
actual fun runExternalTool(
    toolPath: String,
    args: List<String>,
    log: EngineLogger,
    onLine: (String) -> Unit,
): Boolean = false

actual fun osName(): String = NSProcessInfo.processInfo.operatingSystemName().lowercase()

@OptIn(ExperimentalForeignApi::class)
actual fun canExecute(path: String): Boolean = access(path, X_OK) == 0
