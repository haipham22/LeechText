package dev.haipham22.leechtext.action.export

import dev.haipham22.leechtext.log.EngineLogger
import dev.haipham22.leechtext.models.Properties
import dev.haipham22.leechtext.util.AppSettings
import dev.haipham22.leechtext.util.TypeUtils
import dev.haipham22.leechtext.util.ZipIo
import dev.haipham22.leechtext.util.canExecute
import dev.haipham22.leechtext.util.imageBytesToJpeg
import dev.haipham22.leechtext.util.osName
import dev.haipham22.leechtext.util.readResourceBytes
import dev.haipham22.leechtext.util.regexFind
import dev.haipham22.leechtext.util.runExternalTool
import dev.haipham22.leechtext.util.writeTo
import okio.Path
import okio.Path.Companion.toPath

/** Ký tự cấm trong tên file EPUB; OPF path trong savePath (format bản gốc). */
private const val FILENAME_ILLEGAL_CHARS = "[:/?*]"
private const val CONTENT_OPF_PATH = "/data/content.opf"

/** Ext cover có thể có sau sniff magic bytes (ensureCoverFile). */
private val COVER_EXTENSIONS = listOf("jpg", "png", "webp", "gif")

/**
 * Export EPUB/PDF (port từ action/export/Ebook.java — Alert UI thay bằng EngineLog).
 */
@Suppress("LongParameterList") // tham số log DI + params nghiệp vụ — bundle làm code khó đọc
class Ebook(
    private val log: EngineLogger,
    private val properties: Properties,
    private val type: Int,
    private val tool: String,
    private val compressLevel: String,
    private val autoSplit: Boolean,
    private val includeImg: Boolean,
    private val settings: AppSettings,
    private val progressListener: ProgressListener? = null,
) {
    fun export() {
        var tool = this.tool
        when (type) {
            TypeUtils.EPUB -> {
                if (tool == "Calibre") {
                    tool = settings.calibre
                    if (!checkTool(tool)) {
                        error()
                        return
                    }
                }
            }

            TypeUtils.PDF -> {
                tool = settings.calibre
                if (!checkTool(tool)) {
                    error()
                    return
                }
            }

            else -> Unit // format khác EPUB/PDF — TXT xử lý ở Text.kt
        }
        settings.cssSyntax.writeTo(properties.savePath + "/data/stylesheet.css", log = log)
        ensureCoverFile()
        progressListener?.setProgress(0, "[1/3]Exporting text...")
        Text(
            log,
            properties,
            TypeUtils.HTML,
            makeToc = false,
            includeCss = false,
            tach = 0,
            settings = settings,
            progressListener = progressListener,
        ).export()
        progressListener?.setProgress(10, "[2/3]Building TOC...")
        createToc()
        progressListener?.setProgress(14, "[3/3]Creating ebook...")
        when (type) {
            TypeUtils.EPUB -> {
                exportEpub(tool)
            }

            TypeUtils.PDF -> {
                exportPdf()
            }

            else -> Unit // format khác EPUB/PDF — TXT xử lý ở Text.kt
        }
    }

    private fun error() {
        log.add(
            "Invalid Calibre path (ebook-convert)!\n" +
                "Check settings!",
        )
    }

    private fun exportEpub(tool: String) {
        if (tool == "Mặc định") {
            exportDefaultEpub() // lỗi ném lên UI — không nuốt im lặng (bug epub 0 byte 260903)
        } else {
            var fileName = properties.name + " - " + properties.author + ".epub"
            fileName = fileName.replace(FILENAME_ILLEGAL_CHARS.toRegex(), "")
            val cmd =
                tinyCmd(tool) +
                    " " + tinyCmd(properties.savePath + CONTENT_OPF_PATH) +
                    " " + tinyCmd(properties.savePath + "/out/" + fileName)
            runCmd(cmd)
        }
    }

    /**
     * Đảm bảo data/cover.jpg (hoặc cover.<ext> gốc khi không convert được) tồn tại
     * trước khi zip. Ưu tiên cover-custom.jpg (ảnh user thay qua replaceCover),
     * thiếu thì tải book.cover 1 lần. WebP/PNG/GIF → convert JPEG (imageBytesToJpeg
     * — TwelveMonkeys desktop / BitmapFactory Android); convert fail (apple) →
     * fallback ghi bytes gốc với ext sniff từ magic bytes. Thiếu hẳn → bỏ qua
     * (ZipIo.addFile no-op) — EPUB không bìa vẫn đọc được.
     */
    private fun ensureCoverFile() {
        val dataDir = (properties.savePath + "/data").toPath()
        val fs = okio.FileSystem.SYSTEM
        // cover cũ (đã zip từ lần export trước) — build lại từ custom/download
        COVER_EXTENSIONS.forEach { ext -> runCatching { fs.delete(dataDir / "cover.$ext") } }
        val bytes = loadCoverBytes(dataDir) ?: return
        // JPEG luôn, mọi nguồn — reader nào cũng decode được
        val jpeg = imageBytesToJpeg(bytes, log)
        if (jpeg != null) {
            runCatching { fs.write(dataDir / "cover.jpg") { write(jpeg) } }
        } else {
            writeRawCover(bytes, dataDir)
        }
    }

    /** Ưu tiên cover custom trong data/, không có thì tải book.cover 1 lần. */
    private fun loadCoverBytes(dataDir: Path): ByteArray? {
        val fs = okio.FileSystem.SYSTEM
        val custom = dataDir / "cover-custom.jpg"
        if (fs.exists(custom)) {
            return runCatching { fs.read(custom) { readByteArray() } }.getOrNull()
        }
        val url = properties.cover ?: return null
        return runCatching {
            fs.createDirectories(dataDir)
            val b = dev.haipham22.leechtext.plugin.js.api.Http(log).get(url).bytes()
            if (b.isNotEmpty()) b else error("empty cover")
        }.getOrNull()
    }

    /** ponytail: apple chưa có decoder — giữ bytes gốc + ext sniff; add lib iOS khi cần */
    private fun writeRawCover(
        bytes: ByteArray,
        dataDir: Path,
    ) {
        val fs = okio.FileSystem.SYSTEM
        val ext = imageExtension(bytes) ?: return
        runCatching { fs.write(dataDir / "cover.$ext") { write(bytes) } }
    }

    private fun imageExtension(bytes: ByteArray): String? = when {
        bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() -> "jpg"

        bytes.size >= 12 && bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() -> "png"

        bytes.size >= 12 &&
            bytes[0] == 0x52.toByte() && bytes[1] == 0x49.toByte() &&
            bytes[8] == 0x57.toByte() && bytes[9] == 0x45.toByte() -> "webp"

        bytes.size >= 6 && bytes[0] == 0x47.toByte() && bytes[1] == 0x49.toByte() -> "gif"

        else -> null
    }

    private fun exportDefaultEpub() {
        var fileName = properties.name + " - " + properties.author + ".epub"
        fileName = fileName.replace(FILENAME_ILLEGAL_CHARS.toRegex(), "")
        val outPath = properties.savePath + "/out/" + fileName
        // Atomic: ghi file .tmp rồi move — app kill giữa chừng (continuous restart,
        // force-stop) không để lại epub 0 byte mà copy đi Downloads (bug 260903).
        val tmpPath = outPath + ".tmp"
        val fs = okio.FileSystem.SYSTEM
        readResourceBytes("/dark/leech/res/untitled.epub", log).writeTo(tmpPath, log = log)
        val level = compressLevel.toIntOrNull() ?: 6
        ZipIo.addFolder(tmpPath.toPath(), (properties.savePath + "/data/Text").toPath(), level = level, log = log)
        progressListener?.setProgress(75, "(3/3)Creating ebook...")
        ZipIo.addFile(tmpPath.toPath(), (properties.savePath + CONTENT_OPF_PATH).toPath(), level = level, log = log)
        ZipIo.addFile(tmpPath.toPath(), (properties.savePath + "/data/toc.ncx").toPath(), level = level, log = log)
        ZipIo.addFile(tmpPath.toPath(), (properties.savePath + "/data/stylesheet.css").toPath(), level = level, log = log)
        // cover.<ext> — ext sniff từ magic bytes ở ensureCoverFile
        COVER_EXTENSIONS
            .map { (properties.savePath + "/data/cover.$it").toPath() }
            .firstOrNull { fs.exists(it) }
            ?.let { ZipIo.addFile(tmpPath.toPath(), it, level = level, log = log) }
        if (includeImg) {
            ZipIo.addFolder(tmpPath.toPath(), (properties.savePath + "/data/Images").toPath(), level = level, log = log)
        }
        // Kết quả cuối phải có nội dung — template rỗng + zip fail im lặng = 425B rác
        val size = fs.metadataOrNull(tmpPath.toPath())?.size ?: 0L
        log.debug("[exportDefaultEpub] tmp=$tmpPath size=$size")
        if (size < 1_000L) {
            runCatching { fs.delete(tmpPath.toPath()) }
            // code "empty" — UI map stringResource theo locale (engine không có resources)
            throw ExportException("EPUB empty after compression ($size bytes)", code = "empty", detail = "$size")
        }
        fs.createDirectories(outPath.toPath().parent!!)
        fs.atomicMove(tmpPath.toPath(), outPath.toPath())
        log.debug("[exportDefaultEpub] DONE out=$outPath size=${fs.metadataOrNull(outPath.toPath())?.size}")
        progressListener?.setProgress(100, "Done!")
    }

    private fun exportPdf() {
        val tool = settings.calibre
        var fileName = properties.name + " - " + properties.author + ".pdf"
        fileName = fileName.replace(FILENAME_ILLEGAL_CHARS.toRegex(), "")
        val cmd =
            tinyCmd(tool) +
                " " + tinyCmd(properties.savePath + CONTENT_OPF_PATH) +
                " " + tinyCmd(properties.savePath + "/out/" + fileName) +
                " --paper-size=" + compressLevel
        runCmd(cmd)
    }

    fun createToc() {
        ToC(log, properties, autoSplit, includeImg, settings).mkToC()
    }

    private fun checkTool(tool: String): Boolean {
        val b = isTool(tool)
        if (b) createToc()
        return b
    }

    private fun isTool(tool: String?): Boolean {
        if (tool == null) return false
        if (tool.length < 2) return false
        val fs = okio.FileSystem.SYSTEM
        val path = tool.toPath()
        if (!fs.exists(path)) return false
        if (fs.metadata(path).isDirectory) return false
        // Cross-platform: .exe trên Windows, executable trên Unix/macOS
        val os = osName()
        return if (os.contains("win")) tool.endsWith(".exe") else canExecute(tool)
    }

    private fun runCmd(cmd: String) {
        val tokens = cmd.split(Regex("\\s+")).filter { it.isNotEmpty() }
        val tool = tokens.firstOrNull() ?: return
        val args = tokens.drop(1)
        var ok = false
        var percent = 0
        try {
            ok =
                runExternalTool(tool, args, log) { line ->
                    val pe = regexFind(line, "(^\\d+)%", 1)
                    if (pe != null) pe.toIntOrNull()?.let { percent = it }
                    progressListener?.setProgress(percent, "[3/3]" + line)
                }
        } catch (e: Exception) {
            log.add(e)
        }
        if (ok) progressListener?.setProgress(100, "Done!")
    }

    private fun tinyCmd(cmdIn: String): String {
        var cmd = cmdIn
        if (cmd.contains(" ")) cmd = "\"" + cmd + "\""
        return cmd
    }
}
