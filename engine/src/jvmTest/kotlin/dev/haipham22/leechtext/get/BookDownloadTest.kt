package dev.haipham22.leechtext.get

import dev.haipham22.leechtext.log.platformEngineLogger
import dev.haipham22.leechtext.models.Chapter
import dev.haipham22.leechtext.models.Properties
import dev.haipham22.leechtext.plugin.PluginManager
import dev.haipham22.leechtext.util.AppSettings
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val TEMP_DIR_PREFIX = "leech-dl"

class BookDownloadTest {
    private fun properties(
        tmp: File,
        vararg chapters: Chapter,
    ) = Properties().apply {
        url = "https://example.com/truyen"
        savePath = tmp.absolutePath
        chapList = chapters.toList()
        size = chapters.size
    }

    private val longText = "Nội dung chương. ".repeat(100)

    @Test
    fun downloadNewChapterWritesRawFileAndReportsOk() = runBlocking {
        val tmp = createTempDirectory(TEMP_DIR_PREFIX).toFile()
        val ch = Chapter(url = "u1", chapName = "Chương 1", id = "C0")
        val summary =
            properties(tmp, ch).downloadChapters(
                settings = AppSettings(maxConn = 2, delay = 0),
                fetch = { longText },
                log = platformEngineLogger(),
                pluginManager = PluginManager(platformEngineLogger()),
            )
        assertEquals(1, summary.ok)
        assertEquals(0, summary.error)
        assertTrue(File(tmp, "raw/C0.txt").length() > 0)
        assertTrue(ch.completed)
        assertFalse(ch.error)
    }

    @Test
    fun resumeSkipsFetchWhenRawFileExists() = runBlocking {
        val tmp = createTempDirectory(TEMP_DIR_PREFIX).toFile()
        File(tmp, "raw").mkdirs()
        File(tmp, "raw/C0.txt").writeText("đã có từ lần trước")

        val ch = Chapter(url = "u1", chapName = "Chương 1", id = "C0")
        val summary =
            properties(tmp, ch).downloadChapters(
                settings = AppSettings(maxConn = 2, delay = 0),
                fetch = { throw AssertionError("không được fetch chương đã resume") },
                log = platformEngineLogger(),
                pluginManager = PluginManager(platformEngineLogger()),
            )
        assertEquals(1, summary.resumed)
        assertEquals(0, summary.ok)
        assertTrue(ch.completed)
    }

    @Test
    fun shortTextMarksEmptyAndFetchExceptionMarksError() = runBlocking {
        val tmp = createTempDirectory(TEMP_DIR_PREFIX).toFile()
        val empty = Chapter(url = "u1", chapName = "C", id = "C0")
        val broken = Chapter(url = "u2", chapName = "C", id = "C1")
        val summary =
            properties(tmp, empty, broken).downloadChapters(
                settings = AppSettings(maxConn = 2, delay = 0),
                fetch = { if (it.id == "C0") "ngắn" else error("mạng chết") },
                log = platformEngineLogger(),
                pluginManager = PluginManager(platformEngineLogger()),
            )
        assertEquals(1, summary.empty)
        assertEquals(1, summary.error)
        assertTrue(empty.empty)
        assertTrue(broken.error)
        assertFalse(File(tmp, "raw/C1.txt").exists(), "chương lỗi không ghi file")
    }

    @Test
    fun progressReportsAllChaptersAndRespectsMaxConn() = runBlocking {
        val tmp = createTempDirectory(TEMP_DIR_PREFIX).toFile()
        val chapters = (0 until 6).map { Chapter(url = "u$it", chapName = "C", id = "C$it") }
        val running = AtomicInteger()
        val maxConcurrent = AtomicInteger()
        val progress = mutableListOf<Int>()

        val summary =
            properties(tmp, *chapters.toTypedArray()).downloadChapters(
                settings = AppSettings(maxConn = 2, delay = 0),
                onProgress = { completed, total ->
                    progress += completed
                    assertEquals(6, total)
                },
                fetch = {
                    val now = running.incrementAndGet()
                    maxConcurrent.accumulateAndGet(now) { old, new -> maxOf(old, new) }
                    try {
                        longText
                    } finally {
                        running.decrementAndGet()
                    }
                },
                log = platformEngineLogger(),
                pluginManager = PluginManager(platformEngineLogger()),
            )
        assertEquals(6, summary.ok)
        assertEquals(6, progress.max())
        assertTrue(maxConcurrent.get() <= 2, "không vượt maxConn, thực tế ${maxConcurrent.get()}")
    }
}
