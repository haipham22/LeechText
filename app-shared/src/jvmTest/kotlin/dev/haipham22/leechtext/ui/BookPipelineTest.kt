package dev.haipham22.leechtext.ui

import dev.haipham22.leechtext.models.Chapter
import dev.haipham22.leechtext.util.AppSettings
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BookPipelineTest {
    private fun chapters(n: Int) = (1..n).map { Chapter(url = "u$it", chapName = "Chương $it", id = "$it") }

    @Test
    fun parseRangeSingleChapter() {
        assertEquals(listOf("2"), BookPipeline.parseRange("2", chapters(5)).map { it.id })
    }

    @Test
    fun parseRangeInterval() {
        assertEquals(listOf("1", "2", "3"), BookPipeline.parseRange("1-3", chapters(5)).map { it.id })
    }

    @Test
    fun parseRangeMixedWithSpaces() {
        assertEquals(listOf("1", "2", "4", "6", "7"), BookPipeline.parseRange("1-2, 4,6-7", chapters(9)).map { it.id })
    }

    @Test
    fun parseRangeBeyondChapterCountThrows() {
        assertFailsWith<IllegalArgumentException> { BookPipeline.parseRange("1-99", chapters(5)) }
    }

    @Test
    fun parseRangeReversedBoundsThrows() {
        assertFailsWith<IllegalArgumentException> { BookPipeline.parseRange("3-1", chapters(5)) }
    }

    @Test
    fun parseRangeEmptyInputThrows() {
        assertFailsWith<IllegalArgumentException> { BookPipeline.parseRange(",,", chapters(5)) }
    }

    @Test
    fun savePathForSlugsDiacritics() {
        val tmp = Files.createTempDirectory("leechtext-test")
        val dir = BookPipeline.savePathFor(url = null, name = "Truyện Chữ Đầy Dấu!", settings = AppSettings(workPath = tmp.toString()))
        assertTrue(dir.name.all { it.isLetterOrDigit() || it == '_' }, "slug chỉ chứa [a-zA-Z0-9_]: ${dir.name}")
        assertEquals(tmp.resolve("output").toString(), dir.parent.toString())
    }

    /**
     * strings.xml vi (default) + values-en đồng bộ key — thiếu key ở file nào
     * thì fail sớm thay vì runtime MissingResourceException.
     */
    @Test
    fun stringsViEnKeysInSync() {
        val dir = File("src/commonMain/composeResources")
        fun keys(path: String) = Regex("<string name=\"([^\"]+)\">").findAll(File(dir, path).readText()).map { it.groupValues[1] }.toSortedSet()
        val vi = keys("values/strings.xml")
        val en = keys("values-en/strings.xml")
        assertEquals(vi, en, "values-en phải có đúng bộ key như values (thiếu: ${(vi - en).joinToString()}, thừa: ${(en - vi).joinToString()})")
        assertTrue(vi.isNotEmpty())
    }
}
