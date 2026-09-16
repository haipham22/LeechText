package dev.haipham22.leechtext.action

import dev.haipham22.leechtext.action.export.Ebook
import dev.haipham22.leechtext.action.export.ProgressListener
import dev.haipham22.leechtext.log.platformEngineLogger
import dev.haipham22.leechtext.models.Chapter
import dev.haipham22.leechtext.models.Properties
import dev.haipham22.leechtext.util.SettingsRepository
import dev.haipham22.leechtext.util.TypeUtils
import dev.haipham22.leechtext.util.writeTo
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Repro bug epub 0 byte: export pipeline chạy xong nhưng file rỗng.
 * Chạy: ./gradlew :engine:jvmTest --tests "*EpubExportRepro*"
 */
class EpubExportReproTest {
    @Test
    fun epubExportProducesNonEmptyFile() {
        val tmp = createTempDirectory("leech-epub").toFile()
        val properties =
            Properties().apply {
                name = "Test Sách"
                author = "Tác Giả"
                url = "https://example.com/x"
                savePath = tmp.absolutePath
                size = 2
                chapList =
                    listOf(
                        Chapter(url = "u1", chapName = "Chương 1", id = "C1").apply { completed = true },
                        Chapter(url = "u2", chapName = "Chương 2", id = "C2").apply { completed = true },
                    )
            }
        val rawDir = File(tmp, "raw").apply { mkdirs() }
        val log = platformEngineLogger()
        "nội dung chương một khá dài để đủ độ dài nội dung chương kiểm tra xuất bản"
            .repeat(20)
            .writeTo(File(rawDir, "C1.txt").path, log = log)
        "nội dung chương hai khá dài để đủ độ dài nội dung chương kiểm tra xuất bản"
            .repeat(20)
            .writeTo(File(rawDir, "C2.txt").path, log = log)

        Ebook(
            platformEngineLogger(),
            properties,
            TypeUtils.EPUB,
            tool = "Mặc định",
            compressLevel = "6",
            autoSplit = false,
            includeImg = true,
            settings = SettingsRepository.load(),
            progressListener = ProgressListener { _, _ -> },
        ).export()

        val epub = File(tmp, "out/Test Sách - Tác Giả.epub")
        assertTrue(epub.exists(), "epub phải được tạo tại ${epub.path}")
        assertTrue(epub.length() > 1000, "epub phải có nội dung, được ${epub.length()} bytes (bug 0 byte)")
    }
}
