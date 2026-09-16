package dev.haipham22.leechtext.action.export

import dev.haipham22.leechtext.log.platformEngineLogger
import dev.haipham22.leechtext.models.Chapter
import dev.haipham22.leechtext.models.Properties
import dev.haipham22.leechtext.models.Trash
import dev.haipham22.leechtext.util.AppSettings
import dev.haipham22.leechtext.util.SettingsRepository
import dev.haipham22.leechtext.util.TypeUtils
import dev.haipham22.leechtext.util.writeTo
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertTrue

private const val RAW_C1_TXT = "raw/C1.txt"
private const val DATA_TEXT_C1_TXT = "data/Text/C1.txt"
private const val BOOK_NAME = "Truyện"
private const val BOOK_AUTHOR = "Tác Giả"
private const val CHAPTER_1_NAME = "Chương 1"

class TextTest {
    private val settings: AppSettings = SettingsRepository.defaults() // syntax template thật từ resource

    private fun chapter(
        id: String,
        name: String,
        part: String = "",
    ) = Chapter(url = "u", partName = part, chapName = name, completed = true).apply { this.id = id }

    @Test
    fun splitHtmlModeWritesPerChapterFilesWithParagraphWrapping() {
        val tmp = createTempDirectory("leech-text-split").toFile()
        "Đoạn 1\nĐoạn 2".writeTo(tmp.resolve(RAW_C1_TXT).path, log = platformEngineLogger())

        val properties =
            Properties().apply {
                name = BOOK_NAME
                author = BOOK_AUTHOR
                savePath = tmp.absolutePath
                size = 1
                chapList = listOf(chapter("C1", "Chương 1: Bắt đầu"))
            }

        Text(platformEngineLogger(), properties, TypeUtils.HTML, makeToc = false, includeCss = false, tach = 0, settings = settings).export()

        val html = File(tmp, "data/Text/C1.html").readText()
        assertTrue(html.contains("<h4 id=\"C1\">Chương 1: Bắt đầu</h4>"), html)
        // default dropcaps bật → chữ đầu đoạn bọc span.drop
        assertTrue(html.contains("<p><span class=\"drop\">Đ</span>oạn 1</p>"), html)
        assertTrue(html.contains("<p>Đoạn 2</p>"), html)
        // template thay title theo tên chương
        assertTrue(html.contains("<title>Chương 1: Bắt đầu</title>"), html)
    }

    @Test
    fun splitTxtModeUsesTxtTemplate() {
        val tmp = createTempDirectory("leech-text-txt").toFile()
        "Nội dung chương".writeTo(tmp.resolve(RAW_C1_TXT).path, log = platformEngineLogger())

        val properties =
            Properties().apply {
                name = BOOK_NAME
                author = BOOK_AUTHOR
                savePath = tmp.absolutePath
                size = 1
                chapList = listOf(chapter("C1", CHAPTER_1_NAME))
            }

        Text(platformEngineLogger(), properties, TypeUtils.TXT, makeToc = false, includeCss = false, tach = 0, settings = settings).export()

        val txt = File(tmp, DATA_TEXT_C1_TXT).readText()
        assertTrue(txt.contains(CHAPTER_1_NAME), txt)
        assertTrue(txt.contains("Nội dung chương"), txt)
        assertTrue(!txt.contains("<p>"), txt)
    }

    @Test
    fun txtModeConvertsBrAndHtmlTagsToNewlines() {
        val tmp = createTempDirectory("leech-text-br").toFile()
        "Đoạn 1 <br>Đoạn 2 <br/>Đoạn 3 <i>nghiêng</i> &amp; &nbsp;kết".writeTo(tmp.resolve(RAW_C1_TXT).path, log = platformEngineLogger())

        val properties =
            Properties().apply {
                name = BOOK_NAME
                author = BOOK_AUTHOR
                savePath = tmp.absolutePath
                size = 1
                chapList = listOf(chapter("C1", CHAPTER_1_NAME))
            }

        Text(platformEngineLogger(), properties, TypeUtils.TXT, makeToc = false, includeCss = false, tach = 0, settings = settings).export()

        val txt = File(tmp, DATA_TEXT_C1_TXT).readText()
        assertTrue(!txt.contains("<br>"), txt)
        assertTrue(!txt.contains("<br/>"), txt)
        assertTrue(!txt.contains("<i>"), txt)
        assertTrue(txt.contains("Đoạn 1 \nĐoạn 2 \nĐoạn 3 nghiêng &  kết"), txt)
    }

    @Test
    fun combinedModeWritesSingleOutFile() {
        val tmp = createTempDirectory("leech-text-combined").toFile()
        "nội dung 1".writeTo(tmp.resolve(RAW_C1_TXT).path, log = platformEngineLogger())
        "nội dung 2".writeTo(tmp.resolve("raw/C2.txt").path, log = platformEngineLogger())

        val properties =
            Properties().apply {
                name = BOOK_NAME
                author = BOOK_AUTHOR
                savePath = tmp.absolutePath
                size = 2
                chapList = listOf(chapter("C1", CHAPTER_1_NAME), chapter("C2", "Chương 2"))
            }

        Text(platformEngineLogger(), properties, TypeUtils.HTML, makeToc = false, includeCss = false, tach = 1, settings = settings).export()

        val out = File(tmp, "out/text.html")
        assertTrue(out.exists())
        val content = out.readText()
        assertTrue(content.contains("nội dung 1"), content)
        assertTrue(content.contains("nội dung 2"), content)
        assertTrue(content.contains("</body>"), content)
    }

    @Test
    fun makeTocHtmlModeWritesTocHtmlWithLinks() {
        val tmp = createTempDirectory("leech-text-toc").toFile()
        "x".writeTo(tmp.resolve(RAW_C1_TXT).path, log = platformEngineLogger())

        val properties =
            Properties().apply {
                name = BOOK_NAME
                author = BOOK_AUTHOR
                savePath = tmp.absolutePath
                size = 1
                chapList = listOf(chapter("C1", CHAPTER_1_NAME, part = "Quyển 1"))
            }

        Text(platformEngineLogger(), properties, TypeUtils.HTML, makeToc = true, includeCss = false, tach = 0, settings = settings).export()

        val toc = File(tmp, "data/Text/mucluc.html").readText()
        assertTrue(toc.contains("<a href=\"../Text/C1.html\">Quyển 1 - Chương 1</a>"), toc)
    }

    @Test
    fun trashReplaceRulesAppliedDuringExport() {
        val tmp = createTempDirectory("leech-text-trash").toFile()
        "ADS-CRAP\nnội dung".writeTo(tmp.resolve(RAW_C1_TXT).path, log = platformEngineLogger())
        val trashSettings =
            settings.copy(
                trash = listOf(Trash(src = "^ADS-CRAP\\n", to = "", replace = true)),
            )

        val properties =
            Properties().apply {
                name = BOOK_NAME
                author = BOOK_AUTHOR
                savePath = tmp.absolutePath
                size = 1
                chapList = listOf(chapter("C1", CHAPTER_1_NAME))
            }

        Text(platformEngineLogger(), properties, TypeUtils.TXT, makeToc = false, includeCss = false, tach = 0, settings = trashSettings).export()

        val txt = File(tmp, DATA_TEXT_C1_TXT).readText()
        assertTrue(!txt.contains("ADS-CRAP"), txt)
        assertTrue(txt.contains("nội dung"), txt)
    }
}
