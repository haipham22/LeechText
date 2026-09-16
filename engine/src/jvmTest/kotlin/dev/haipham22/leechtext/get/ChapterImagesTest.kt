package dev.haipham22.leechtext.get

import dev.haipham22.leechtext.log.platformEngineLogger
import dev.haipham22.leechtext.models.Chapter
import dev.haipham22.leechtext.models.Properties
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

private const val TEMP_DIR_PREFIX = "leech-img"

/**
 * downloadChapterImages — chỉ nhánh không mạng: không ảnh, ảnh non-http (skip),
 * ảnh đã có file local (target.exists()). Không gọi Http thật.
 */
class ChapterImagesTest {
    private fun properties(
        tmp: File,
        vararg chapters: Chapter,
    ) = Properties().apply {
        savePath = tmp.absolutePath
        chapList = chapters.toList()
        size = chapters.size
    }

    private fun writeRaw(
        tmp: File,
        id: String,
        html: String,
    ): File {
        val raw = File(tmp, "raw/$id.txt")
        raw.parentFile.mkdirs()
        raw.writeText(html)
        return raw
    }

    @Test
    fun textWithoutImagesReturnsZeroAndSkipsRawRewrite() {
        val tmp = createTempDirectory(TEMP_DIR_PREFIX).toFile()
        val raw = writeRaw(tmp, "C0", "<p>chỉ text, không có ảnh nào</p>")
        check(raw.setLastModified(1_000L)) { "setLastModified fail" } // mtime cố định — ghi lại sẽ đổi mtime

        assertEquals(
            0,
            properties(tmp, Chapter(url = "u1", chapName = "C", id = "C0"))
                .downloadChapterImages(log = platformEngineLogger()),
        )
        assertEquals(1_000L, raw.lastModified(), "không có ảnh thì không ghi lại file raw")
        assertFalse(File(tmp, "data/Images").exists())

        // savePath null → guard trả 0 ngay
        assertEquals(
            0,
            Properties()
                .apply {
                    chapList = listOf(Chapter(url = "u", chapName = "C", id = "C0"))
                }.downloadChapterImages(log = platformEngineLogger()),
        )
    }

    @Test
    fun nonHttpImgSkippedAndContentUnchanged() {
        val tmp = createTempDirectory(TEMP_DIR_PREFIX).toFile()
        val html = "<p><img src=\"data:image/png;base64,AAA\"/></p>"
        val raw = writeRaw(tmp, "C0", html)

        assertEquals(
            0,
            properties(tmp, Chapter(url = "u1", chapName = "C", id = "C0"))
                .downloadChapterImages(log = platformEngineLogger()),
        )
        assertFalse(File(tmp, "data/Images").exists(), "URL non-http không được tải")
        assertEquals(html, raw.readText())
    }

    @Test
    fun cachedImageReusedWithRelativeSrc() {
        val tmp = createTempDirectory(TEMP_DIR_PREFIX).toFile()
        val raw = writeRaw(tmp, "C0", "<img src=\"https://cdn.example.com/pic.jpg\">")
        // target.exists() → downloadImageFile trả 0, không gọi Http
        val cached = File(tmp, "data/Images/C0_0.jpg")
        cached.parentFile.mkdirs()
        cached.writeText("cached-bytes")

        assertEquals(
            0,
            properties(tmp, Chapter(url = "u1", chapName = "C", id = "C0"))
                .downloadChapterImages(log = platformEngineLogger()),
        )
        assertEquals(
            "<img src=\"../Images/C0_0.jpg\"/>",
            raw.readText(),
            "src phải rewrite sang đường dẫn tương đối EPUB",
        )
    }

    @Test
    fun missingRawFileIncrementsDoneWithoutProgress() {
        val tmp = createTempDirectory(TEMP_DIR_PREFIX).toFile()
        writeRaw(tmp, "C1", "<p>text</p>") // C0 không có file raw
        val progress = mutableListOf<Pair<Int, Int>>()

        val downloaded =
            properties(
                tmp,
                Chapter(url = "u1", chapName = "C", id = "C0"),
                Chapter(url = "u2", chapName = "C", id = "C1"),
            ).downloadChapterImages(
                onProgress = { done, total -> progress += done to total },
                log = platformEngineLogger(),
            )

        assertEquals(0, downloaded)
        // C0 thiếu raw không báo; chỉ C1 báo (2,2) — nếu C0 báo thì (1,2) xuất hiện đầu
        assertEquals(listOf(2 to 2), progress)
    }
}
