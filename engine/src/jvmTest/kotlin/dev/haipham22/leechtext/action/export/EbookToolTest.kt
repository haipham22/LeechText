package dev.haipham22.leechtext.action.export

import dev.haipham22.leechtext.log.platformEngineLogger
import dev.haipham22.leechtext.models.Chapter
import dev.haipham22.leechtext.models.Properties
import dev.haipham22.leechtext.util.SettingsRepository
import dev.haipham22.leechtext.util.TypeUtils
import dev.haipham22.leechtext.util.writeTo
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val TEMP_DIR_PREFIX = "leech-tool"

/**
 * Ebook: isTool/tinyCmd/exportDefaultEpub qua reflection (private) + các nhánh
 * export không chạy calibre thật (tool rỗng → exec fail nhanh, calibre path rác →
 * dừng sớm).
 */
class EbookToolTest {
    private fun ebook(
        tmp: File,
        tool: String = "",
        includeImg: Boolean = false,
        settings: dev.haipham22.leechtext.util.AppSettings = SettingsRepository.defaults(),
        withChapters: Boolean = false,
    ) = Ebook(
        platformEngineLogger(),
        Properties().apply {
            name = "B"
            author = "A"
            savePath = tmp.absolutePath
            if (withChapters) {
                size = 1
                chapList =
                    listOf(
                        Chapter(url = "u", partName = "", chapName = "Chương 1", completed = true)
                            .apply { id = "C1" },
                    )
            }
        },
        TypeUtils.EPUB,
        tool,
        "0",
        false,
        includeImg,
        settings,
    )

    private fun isTool(
        e: Ebook,
        x: String?,
    ): Boolean = Ebook::class.java
        .getDeclaredMethod("isTool", String::class.java)
        .apply { isAccessible = true }
        .invoke(e, x) as Boolean

    private fun tinyCmd(
        e: Ebook,
        x: String,
    ): String = Ebook::class.java
        .getDeclaredMethod("tinyCmd", String::class.java)
        .apply { isAccessible = true }
        .invoke(e, x) as String

    @Test
    fun isToolRejectsNullShortMissingAndDirectory() {
        val tmp = createTempDirectory(TEMP_DIR_PREFIX).toFile()
        val e = ebook(tmp)
        assertFalse(isTool(e, null))
        assertFalse(isTool(e, "a"), "path 1 ký tự không hợp lệ")
        assertFalse(isTool(e, "/no/such/tool"))
        assertFalse(isTool(e, tmp.absolutePath), "thư mục không phải tool")
    }

    @Test
    fun isToolOnUnixRequiresExecutableFile() {
        val tmp = createTempDirectory(TEMP_DIR_PREFIX).toFile()
        val f = tmp.resolve("ebook-convert")
        f.writeText("#!/bin/sh\n")
        val e = ebook(tmp)
        f.setExecutable(true)
        assertTrue(isTool(e, f.absolutePath))
        f.setExecutable(false)
        assertFalse(isTool(e, f.absolutePath), "file không executable bị chặn trên unix")
    }

    @Test
    fun tinyCmdQuotesPathWithSpace() {
        val e = ebook(createTempDirectory(TEMP_DIR_PREFIX).toFile())
        assertEquals("plain.txt", tinyCmd(e, "plain.txt"))
        assertEquals("\"a b/c.txt\"", tinyCmd(e, "a b/c.txt"))
    }

    @Test
    fun exportDefaultEpubZipsDataIntoUntitledTemplate() {
        val tmp = createTempDirectory("leech-epub-default").toFile()
        "opf".writeTo(tmp.resolve("data/content.opf").path, log = platformEngineLogger())
        "ncx".writeTo(tmp.resolve("data/toc.ncx").path, log = platformEngineLogger())
        "css".writeTo(tmp.resolve("data/stylesheet.css").path, log = platformEngineLogger())
        "html".writeTo(tmp.resolve("data/Text/C0.html").path, log = platformEngineLogger())
        "img".writeTo(tmp.resolve("data/Images/C0_0.jpg").path, log = platformEngineLogger())

        val e = ebook(tmp, tool = "Mặc định", includeImg = true)
        Ebook::class.java
            .getDeclaredMethod("exportDefaultEpub")
            .apply { isAccessible = true }
            .invoke(e)

        val out = File(tmp, "out/B - A.epub")
        assertTrue(out.exists(), "phải tạo epub từ template: ${out.path}")
        val zip = net.lingala.zip4j.ZipFile(out)
        assertTrue(zip.getFileHeader("Text/C0.html") != null)
        assertTrue(zip.getFileHeader("content.opf") != null)
        assertTrue(zip.getFileHeader("Images/C0_0.jpg") != null, "includeImg phải nén Images/")
    }

    @Test
    fun emptyToolRunCmdFailsFastNoEpubCreated() {
        val tmp = createTempDirectory("leech-epub").toFile()
        "Đoạn văn".writeTo(tmp.resolve("raw/C1.txt").path, log = platformEngineLogger())

        ebook(tmp, tool = "", withChapters = true).export()

        assertFalse(
            File(tmp, "out/B - A.epub").exists(),
            "exec content.opf fail → không có epub output",
        )
    }

    @Test
    fun invalidCalibrePathStopsBeforeWritingData() {
        val tmp = createTempDirectory("leech-epub").toFile()
        val settings = SettingsRepository.defaults().copy(calibre = "/no/such/ebook-convert")

        ebook(tmp, tool = "Calibre", settings = settings, withChapters = true).export()

        assertFalse(
            File(tmp, "data/stylesheet.css").exists(),
            "checkTool fail phải return trước bước ghi stylesheet",
        )
    }
}
