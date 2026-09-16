package dev.haipham22.leechtext.util

import dev.haipham22.leechtext.log.platformEngineLogger
import dev.haipham22.leechtext.plugin.vbook.PluginZipExtractor
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSTemporaryDirectory
import platform.posix.getcwd
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private const val CHAP1_TXT = "chap1.txt"
private const val TINY_TXT = "tiny.txt"
private const val PLUGIN_JSON = "plugin.json"

/**
 * P5.4 — ZipIo Apple actual (tự parse ZIP + platform.zlib): round-trip tạo/đọc + đọc fixture
 * .plugin thật do `zip` CLI tạo (entry có extra fields unix, entry thư mục "src/") — bảo đảm
 * cài plugin + export EPUB chạy trên iOS/macOS.
 */
class ZipIoAppleTest {
    private val fs = FileSystem.SYSTEM
    private val log = platformEngineLogger()

    // ---------- round-trip seam tự tạo ----------

    @Test
    fun roundTripBytesIdentical() {
        val dir = tempDir()
        val zip = dir / "out.zip"
        val content =
            "Chương 1: Khởi đầu\nNội dung tiếng Việt có dấu — kiểm tra UTF-8.\n".repeat(50).encodeToByteArray()
        val small = "tiny".encodeToByteArray()
        fs.write(dir / CHAP1_TXT) { write(content) }
        fs.write(dir / TINY_TXT) { write(small) }

        ZipIo.addFile(zip, dir / CHAP1_TXT, level = 6, log = log)
        ZipIo.addFile(zip, dir / TINY_TXT, level = 0, log = log) // STORED

        val infos = ZipIo.entries(zip, log)
        assertEquals(listOf(CHAP1_TXT, TINY_TXT), infos.map { it.name })
        assertContentEquals(content, ZipIo.readEntry(zip, CHAP1_TXT, log))
        assertContentEquals(small, ZipIo.readEntry(zip, TINY_TXT, log))
        assertEquals(content.size.toLong(), ZipIo.entries(zip, log).first { it.name == CHAP1_TXT }.size)

        // entry nén và store phải khác nhau ở mức bytes (deflate thật, không phải store ngầm)
        val raw = fs.read(zip) { readByteArray() }
        assertNotEquals(content.size, raw.size)
    }

    @Test
    fun addFolderToExistingZipKeepsOldAndAddsNewEntries() {
        val dir = tempDir()
        val zip = dir / "book.epub"
        fs.write(dir / "mimetype") { write("application/epub+zip".encodeToByteArray()) }
        ZipIo.addFile(zip, dir / "mimetype", level = 0, log = log)

        val text = dir / "Text"
        fs.createDirectories(text / "sub")
        fs.write(text / "Chương 1.html") { write("<html>1</html>".encodeToByteArray()) }
        fs.write(text / "sub" / "Chương 2.html") { write("<html>2</html>".encodeToByteArray()) }
        ZipIo.addFolder(zip, text, level = 6, log = log)

        val names = ZipIo.entries(zip, log).map { it.name }
        assertEquals("mimetype", names.first()) // entry trước đó đứng đầu
        assertTrue("Text/" in names, "entry thư mục Text/ phải có: $names")
        assertTrue("Text/Chương 1.html" in names, "tên tiếng Việt giữ nguyên Unicode: $names")
        assertTrue("Text/sub/Chương 2.html" in names, "thư mục lồng: $names")
        assertContentEquals("<html>2</html>".encodeToByteArray(), ZipIo.readEntry(zip, "Text/sub/Chương 2.html", log))
        assertTrue(ZipIo.entries(zip, log).first { it.name == "Text/" }.isDirectory)
    }

    @Test
    fun corruptZipReturnsEmptyOrNullWithoutCrash() {
        val dir = tempDir()
        val zip = dir / "bad.zip"
        fs.write(zip) { write(ByteArray(10) { 7 }) } // không phải zip
        assertTrue(ZipIo.entries(zip, log).isEmpty())
        assertEquals(null, ZipIo.readEntry(zip, PLUGIN_JSON, log))

        // file bị cắt cụt giữa chừng (EOCD mất) → same degrade
        val ok = dir / "ok.zip"
        fs.write(dir / "x.txt") { write("abc".encodeToByteArray()) }
        ZipIo.addFile(ok, dir / "x.txt", level = 6, log = log)
        val full = fs.read(ok) { readByteArray() }
        fs.write(ok) { write(full.copyOfRange(0, full.size - 15)) }
        assertTrue(ZipIo.entries(ok, log).isEmpty())
    }

    @Test
    fun addMissingFileSkippedSilently() {
        val dir = tempDir()
        val zip = dir / "e.zip"
        ZipIo.addFile(zip, dir / "nope.txt", level = 6, log = log)
        ZipIo.addFolder(zip, dir / "nope-dir", level = 6, log = log)
        assertEquals(null, fs.metadataOrNull(zip))
    }

    // ---------- fixture .plugin thật (zip CLI) ----------

    @Test
    fun readFixturePluginZipCliEntriesAndContent() {
        val fixture = fixturePath()
        val names = ZipIo.entries(fixture, log).map { it.name }
        assertTrue(PLUGIN_JSON in names, "names=$names")
        assertTrue("src/chap.js" in names, "names=$names (zip CLI tạo entry thư mục src/)")
        val json = ZipIo.readEntry(fixture, PLUGIN_JSON, log)?.decodeToString()
        assertNotNull(json)
        assertTrue("\"Sample iOS Fixture\"" in json, "plugin.json đọc sai: $json")
        assertTrue(
            "Response.success" in ZipIo.readEntry(fixture, "src/chap.js", log)!!.decodeToString(),
            "src/chap.js đọc sai",
        )
        assertTrue(ZipIo.entries(fixture, log).first { it.name == "src/" }.isDirectory)
        // icon.png binary (1x1 PNG)
        val icon = ZipIo.readEntry(fixture, "icon.png", log)
        assertNotNull(icon)
        assertEquals(70, icon.size)
        assertTrue(icon[1] == 'P'.code.toByte() && icon[2] == 'N'.code.toByte(), "PNG signature sai")
    }

    /** Chuỗi đúng như cài plugin trên app: extractor (validate + parse + scripts + icon). */
    @Test
    fun extractorReadsFixtureIntoPluginEntity() {
        val entity = PluginZipExtractor(platformEngineLogger()).extractFromZip(fixturePath())
        assertEquals("Sample iOS Fixture", entity.name)
        assertEquals("LeechText", entity.author)
        assertTrue(entity.scriptContents.containsKey("chap"))
        assertTrue(entity.scriptContents.containsKey("toc"))
        assertTrue(entity.rawMetadata?.contains("\"Sample iOS Fixture\"") == true)
        assertTrue(entity.iconBase64?.startsWith("data:image/png;base64,") == true)
    }

    // ---------- helpers ----------

    /** Fixture native test KHÔNG trên classpath — dò từ cwd đi lên tới repo root. */
    @OptIn(ExperimentalForeignApi::class)
    private fun fixturePath(): Path {
        val cwd: String =
            memScoped {
                val buf = allocArray<ByteVar>(4096)
                getcwd(buf, 4096u)?.toKString() ?: "/"
            }
        var dir = cwd.toPath()
        repeat(8) {
            listOf(
                dir / "src/macosTest/resources/fixtures/sample-vbook-ios.plugin",
                dir / "engine/src/macosTest/resources/fixtures/sample-vbook-ios.plugin",
            ).forEach { p ->
                if (fs.exists(p)) return p
            }
            dir = dir / ".."
        }
        throw AssertionError("Không tìm thấy fixture sample-vbook-ios.plugin từ cwd=$cwd")
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun tempDir(): Path {
        val base = NSTemporaryDirectory().toPath()
        val dir = base / "lt-ziptest-${kotlin.random.Random.nextLong()}"
        fs.createDirectories(dir)
        return dir
    }
}
