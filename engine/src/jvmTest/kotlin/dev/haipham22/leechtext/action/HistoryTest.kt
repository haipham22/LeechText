package dev.haipham22.leechtext.action

import dev.haipham22.leechtext.log.platformEngineLogger
import dev.haipham22.leechtext.models.Chapter
import dev.haipham22.leechtext.models.Properties
import dev.haipham22.leechtext.util.writeTo
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val PROPERTIES_FILE = "properties.json"

class HistoryTest {
    @Test
    fun saveHistoryThenLoadHistoryRoundTripsProperties() {
        val tmp = createTempDirectory("leech-history").toFile()
        val properties =
            Properties().apply {
                name = "Truyện Test"
                author = "Tác Giả"
                url = "https://example.com/truyen"
                cover = "https://example.com/cover.jpg"
                size = 2
                savePath = tmp.absolutePath
                introduce = "Giới thiệu truyện"
                chapList =
                    listOf(
                        Chapter(url = "u1", partName = "", chapName = "Chương 1", id = "C1"),
                        Chapter(url = "u2", partName = "Quyển 1", chapName = "Chương 2", id = "C2", error = true),
                    )
            }

        saveHistory(properties, platformEngineLogger())
        val file = File(tmp, PROPERTIES_FILE)
        assertTrue(file.exists(), "properties.json phải được tạo")
        // format bản gốc: key metadata + list, compact org.json toString
        val json = file.readText()
        assertTrue(json.contains("\"metadata\""))
        assertTrue(json.contains("\"list\""))
        assertTrue(json.contains("\"gioithieu\""))

        val loaded = loadHistory(file.path, platformEngineLogger())
        assertNotNull(loaded)
        assertEquals("Truyện Test", loaded.name)
        assertEquals("Tác Giả", loaded.author)
        assertEquals(2, loaded.size)
        assertEquals(2, loaded.chapList?.size)
        assertEquals("C2", loaded.chapList?.get(1)?.id)
        assertEquals(true, loaded.chapList?.get(1)?.error)
        assertEquals("Quyển 1", loaded.chapList?.get(1)?.partName)
        // completed giờ đánh theo file raw (bug epub 0 byte) — test này không tạo raw → false
        assertEquals(false, loaded.chapList?.get(0)?.completed)
    }

    @Test
    fun loadHistoryMissingFileReturnsNull() {
        assertNull(loadHistory("/nonexistent/properties.json", platformEngineLogger()))
    }

    @Test
    fun loadHistoryMarksCompletedFromRawFileOnDisk() {
        // Bug epub 0 byte: completed in-memory mất sau restart — load phải đánh lại theo file raw
        val tmp = createTempDirectory("leech-history").toFile()
        val properties =
            Properties().apply {
                name = "X"
                savePath = tmp.absolutePath
                chapList =
                    listOf(
                        Chapter(url = "u1", chapName = "Chương 1", id = "C1"),
                        Chapter(url = "u2", chapName = "Chương 2", id = "C2"),
                    )
            }
        saveHistory(properties, platformEngineLogger())
        // Chỉ C1 có file raw — C2 không
        val rawDir = File(tmp, "raw").apply { mkdirs() }
        "nội dung chương 1".writeTo(File(rawDir, "C1.txt").path, log = platformEngineLogger())

        val loaded = loadHistory(File(tmp, PROPERTIES_FILE).path, platformEngineLogger())
        assertNotNull(loaded)
        assertTrue(loaded.chapList!![0].completed, "C1 có file raw → completed")
        assertTrue(!loaded.chapList!![1].completed, "C2 không file raw → chưa completed")
    }

    @Test
    fun loadHistoryBrokenJsonReturnsEmptyPropertiesNotCrash() {
        val tmp = createTempDirectory("leech-history-broken").toFile()
        val f = File(tmp, PROPERTIES_FILE)
        "{ broken".writeTo(f.path, log = platformEngineLogger())
        assertNotNull(loadHistory(f.path, platformEngineLogger()))
    }
}
