package dev.haipham22.leechtext.get

import dev.haipham22.leechtext.log.EngineLogger
import dev.haipham22.leechtext.models.Properties
import dev.haipham22.leechtext.plugin.js.api.Http
import dev.haipham22.leechtext.util.readTextOrNull
import dev.haipham22.leechtext.util.writeTo
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

/**
 * Tải ảnh chương ảnh (MUST #15 — port Config.downloadImg): quét <img src> trong
 * raw/<id>.txt, download về data/Images/<id>_<i>.<ext>, rewrite src tương đối
 * "../Images/..." cho đúng khi export EPUB.
 */
fun Properties.downloadChapterImages(
    onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    log: EngineLogger,
): Int {
    val chapters = chapList.orEmpty()
    val saveDir = savePath ?: return 0
    var downloaded = 0
    var done = 0

    for (chapter in chapters) {
        val rawPath = (saveDir.toPath() / "raw" / "${chapter.id}.txt")
        val text = rawPath.readTextOrNull()
        if (text == null) {
            // Chưa có file raw — bỏ qua, không báo progress (như bản gốc)
            done++
            continue
        }
        downloaded += fetchAndRewriteImages(text, chapter.id, rawPath, saveDir, log)
        done++
        onProgress(done, chapters.size)
    }
    return downloaded
}

/**
 * Tải ảnh trong text 1 chương về data/Images, rewrite src tương đối trong file raw.
 * Trả về số ảnh tải mới. Không có ảnh thì không ghi lại file.
 */
@Suppress("LongParameterList") // tham số log DI + params nghiệp vụ — bundle làm code khó đọc
private fun fetchAndRewriteImages(
    text: String,
    chapterId: String?,
    rawPath: Path,
    saveDir: String,
    log: EngineLogger,
): Int {
    val imgRegex = Regex("<img.*?src=\"(.*?)\"")
    val imgUrls = imgRegex.findAll(text).map { it.groupValues[1] }.toList()
    if (imgUrls.isEmpty()) return 0

    var downloaded = 0
    var rewritten = text
    imgUrls.forEachIndexed { i, imgUrl ->
        if (!imgUrl.startsWith("http")) return@forEachIndexed
        val ext =
            imgUrl
                .substring(imgUrl.lastIndexOf("."))
                .lowercase()
                .takeWhile { it != '?' && it != '"' }
                .ifEmpty { ".jpg" }
        val fileName = "${chapterId}_$i$ext"
        val target = saveDir.toPath() / "data" / "Images" / fileName
        downloaded += downloadImageFile(imgUrl, target, log)
        rewritten =
            rewritten
                .replace(imgUrl, "../Images/$fileName")
                .replace("\">", "\"/>")
    }
    rewritten.writeTo(rawPath.toString(), log = log)
    return downloaded
}

/** Tải 1 ảnh về target nếu chưa tồn tại. Trả về 1 nếu tải mới, 0 nếu bỏ qua/lỗi. */
private fun downloadImageFile(
    imgUrl: String,
    target: Path,
    log: EngineLogger,
): Int {
    val fs = FileSystem.SYSTEM
    if (fs.exists(target)) return 0
    return try {
        val bytes = Http(log).get(imgUrl).bytes()
        if (bytes.isNotEmpty()) {
            target.parent?.let { fs.createDirectories(it) }
            bytes.writeTo(target.toString(), log = log)
            1
        } else {
            0
        }
    } catch (e: Exception) {
        log.add("[downloadImg] fail $imgUrl: ${e.message}")
        0
    }
}
