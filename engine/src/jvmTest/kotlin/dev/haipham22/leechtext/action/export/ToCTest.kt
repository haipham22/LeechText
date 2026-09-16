package dev.haipham22.leechtext.action.export

import dev.haipham22.leechtext.log.platformEngineLogger
import dev.haipham22.leechtext.models.Chapter
import dev.haipham22.leechtext.models.Properties
import dev.haipham22.leechtext.util.AppSettings
import dev.haipham22.leechtext.util.SettingsRepository
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Port từ src/test/java/dark/leech/text/action/export/ToCTest.java. */
class ToCTest {
    private val settings: AppSettings = SettingsRepository.defaults()

    @Test
    fun navPointClosingTagsBalanceWithIntroSection() {
        val tempDir = createTempDirectory("leech-text-test-").toFile()

        // Caller phải tạo sẵn data dirs
        File(tempDir, "data").mkdirs()
        File(tempDir, "data/Text").mkdirs()
        File(tempDir, "data/Images").mkdirs()

        val properties =
            Properties().apply {
                name = "Test Book"
                author = "Test Author"
                savePath = tempDir.absolutePath
                addGt = true
                chapList =
                    listOf(
                        Chapter(url = "url1", chapName = "Chương 1: Bắt đầu").apply { id = "C1" },
                        Chapter(url = "url2", chapName = "Chương 2: Tiếp tục").apply { id = "C2" },
                    )
            }

        ToC(platformEngineLogger(), properties, autoSplit = true, includeImg = false, settings = settings).mkToC()

        val ncxFile = File(tempDir, "data/toc.ncx")
        assertTrue(ncxFile.exists(), "toc.ncx should be created")

        val ncxContent = ncxFile.readText()
        val openCount = ncxContent.split("<navPoint").size - 1
        val closeCount = ncxContent.split("</navPoint>").size - 1

        assertEquals(openCount, closeCount, "All <navPoint> elements must be properly closed")
    }

    @Test
    fun singlePartModeWritesManifestItemsForAllChapters() {
        val tempDir = createTempDirectory("leech-toc-single-").toFile()
        File(tempDir, "data").mkdirs()
        File(tempDir, "data/Text").mkdirs()

        val properties =
            Properties().apply {
                name = "Book"
                author = "Auth"
                savePath = tempDir.absolutePath
                addGt = true
                chapList =
                    (1..3).map {
                        Chapter(url = "u$it", chapName = "Chương $it").apply { id = "C$it" }
                    }
            }

        ToC(platformEngineLogger(), properties, autoSplit = false, includeImg = false, settings = settings).mkToC()

        val opf = File(tempDir, "data/content.opf").readText()
        assertTrue(opf.contains("id=\"C0\" href=\"Text/C1.html\""))
        assertTrue(opf.contains("id=\"C2\" href=\"Text/C3.html\""))
        assertTrue(opf.contains("id=\"gioithieu\""))
        // spine: item → itemref
        assertTrue(opf.contains("<itemref idref=\"gioithieu\"/>"))
        // mucluc.html sinh ra
        assertTrue(File(tempDir, "data/Text/mucluc.html").exists())
    }

    @Test
    fun splitPartDetectsChapter1Restart() {
        val chapters =
            listOf(
                Chapter(url = "u1", chapName = "Chương 1").apply { id = "C1" },
                Chapter(url = "u2", chapName = "Chương 2").apply { id = "C2" },
                Chapter(url = "u3", chapName = "Chương 3").apply { id = "C3" },
                Chapter(url = "u4", chapName = "Chương 1").apply { id = "C4" }, // reset → split
                Chapter(url = "u5", chapName = "Chương 2").apply { id = "C5" },
            )
        val properties = Properties().apply { chapList = chapters }

        val splits = ToC(platformEngineLogger(), properties, settings = settings).splitPart()

        // Strategy 3: restart tại index 3
        assertTrue(splits.contains(3), "expected split at chapter-1 restart, got $splits")
    }
}
