package dev.haipham22.leechtext.get

import dev.haipham22.leechtext.models.Chapter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChapterNameTest {
    @Test
    fun autoFixNameSplitsVolumeFromChapterName() {
        val ch = Chapter(chapName = "Quyển 1: Chương 5 - Bí Mật", partName = "")
        ch.autoFixName()
        assertEquals("Quyển 1", ch.partName)
        assertTrue(ch.chapName!!.startsWith("Chương"), "chapName=$${ch.chapName}")
    }

    @Test
    fun optimizeNameCapitalizesSentenceStart() {
        val ch = Chapter(chapName = "chương 12: khởi đầu", partName = "")
        ch.optimizeName()
        assertEquals("Chương 12: Khởi Đầu", ch.chapName)
    }

    @Test
    fun fixNameMergesRepeatedChapter() {
        val ch = Chapter(chapName = "Chương 5 - Chương 5: Bí Mật", partName = "")
        ch.autoFixName()
        assertEquals("Chương 5: Bí Mật", ch.chapName)
    }
}
