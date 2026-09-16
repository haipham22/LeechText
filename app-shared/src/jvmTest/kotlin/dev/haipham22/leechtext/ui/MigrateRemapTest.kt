package dev.haipham22.leechtext.ui

import dev.haipham22.leechtext.models.Chapter
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val OLD_CHAPTER_FILE = "C0.txt"

/**
 * Đổi nguồn (owner 2026-08-26): matching tên chương cross-site + chuyển file raw
 * id cũ → id mới. Tách hàm pure + IO tmp dir cho test được.
 */
class MigrateRemapTest {
    private lateinit var dir: Path
    private val fs = FileSystem.SYSTEM

    @BeforeTest
    fun setUp() {
        dir = java.nio.file.Files.createTempDirectory("migrate-remap").toString().toPath()
        fs.createDirectories(dir)
    }

    @AfterTest
    fun tearDown() {
        runCatching { fs.deleteRecursively(dir) }
    }

    private fun ch(
        id: String,
        name: String,
    ) = Chapter(chapName = name).apply { this.id = id }

    @Test
    fun differentFormattingStillMatchesIgnoringPunctuationAndCase() {
        val old = listOf(ch("C0", "Chương 1: Tô Minh"), ch("C1", "Chương 2 - Về Làng!"))
        val new = listOf(ch("C5", "chương 1 tô minh"), ch("C6", "Chương 2: Về Làng"))
        fs.write(dir / OLD_CHAPTER_FILE) { writeUtf8("nội dung 0") }
        fs.write(dir / "C1.txt") { writeUtf8("nội dung 1") }

        val kept = remapChapterFiles(dir, old, new)

        assertEquals(2, kept)
        assertEquals("nội dung 0", fs.read(dir / "C5.txt") { readUtf8() })
        assertEquals("nội dung 1", fs.read(dir / "C6.txt") { readUtf8() })
        assertTrue(!fs.exists(dir / OLD_CHAPTER_FILE), "file id cũ phải bị dọn")
        assertTrue(fs.listOrNull(dir)?.none { it.name.startsWith("tmp_") } == true, "không sót tmp")
    }

    @Test
    fun unmatchedChapterLosesFileAndMustRefetch() {
        val old = listOf(ch("C0", "Chương 1: Tô Minh"))
        val new = listOf(ch("C0", "Chương alpha"))
        fs.write(dir / OLD_CHAPTER_FILE) { writeUtf8("nội dung cũ") }

        val kept = remapChapterFiles(dir, old, new)

        assertEquals(0, kept)
        assertTrue(!fs.exists(dir / OLD_CHAPTER_FILE), "không khớp → không giữ file cũ tại id trùng")
    }

    @Test
    fun missingRawDirMeansNothingDownloaded() {
        val kept = remapChapterFiles(dir / "nope", listOf(ch("C0", "A")), listOf(ch("C0", "A")))
        assertEquals(0, kept)
    }
}
