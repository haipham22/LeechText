package dev.haipham22.leechtext.action.export

import dev.haipham22.leechtext.log.platformEngineLogger
import dev.haipham22.leechtext.models.Chapter
import dev.haipham22.leechtext.models.Properties
import dev.haipham22.leechtext.util.SettingsRepository
import dev.haipham22.leechtext.util.TypeUtils
import dev.haipham22.leechtext.util.readZipEntryAsString
import dev.haipham22.leechtext.util.writeTo
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertTrue

class EbookTest {
    @Test
    fun defaultEpubExportProducesValidZipStructure() {
        val tmp = createTempDirectory("leech-ebook").toFile()
        "Đoạn văn".writeTo(tmp.resolve("raw/C1.txt").path, log = platformEngineLogger())

        val properties =
            Properties().apply {
                name = "Truyện Test"
                author = "Tác Giả"
                savePath = tmp.absolutePath
                size = 1
                chapList =
                    listOf(
                        Chapter(url = "u", partName = "", chapName = "Chương 1", completed = true).apply { id = "C1" },
                    )
            }

        val progress = mutableListOf<Pair<Int, String>>()
        val ebook =
            Ebook(
                platformEngineLogger(),
                properties,
                TypeUtils.EPUB,
                tool = "Mặc định",
                compressLevel = "0",
                autoSplit = false,
                includeImg = false,
                settings = SettingsRepository.defaults(),
                progressListener = { v, s -> progress.add(v to s) },
            )
        ebook.export()

        val out = File(tmp, "out/Truyện Test - Tác Giả.epub")
        assertTrue(out.exists(), "epub phải được tạo: ${out.path}")

        // cấu trúc EPUB: OPF + NCX + Text/
        val zip = net.lingala.zip4j.ZipFile(out)
        assertTrue(zip.getFileHeader("Text/C1.html") != null, "phải có Text/C1.html trong epub")
        assertTrue(zip.getFileHeader("content.opf") != null, "phải có content.opf")
        assertTrue(zip.getFileHeader("toc.ncx") != null, "phải có toc.ncx")
        val html = readZipEntryAsString(out, "Text/C1.html")
        // default dropcaps bật → chữ đầu bọc span.drop
        assertTrue(html.contains("<span class=\"drop\">Đ</span>oạn văn"), html)

        // pipeline báo progress hoàn tất
        assertTrue(progress.any { it.first == 100 }, "progress: $progress")
    }
}
