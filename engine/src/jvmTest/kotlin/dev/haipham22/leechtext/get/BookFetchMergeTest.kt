package dev.haipham22.leechtext.get

import dev.haipham22.leechtext.models.Chapter
import dev.haipham22.leechtext.models.Properties
import kotlin.test.Test
import kotlin.test.assertEquals

class BookFetchMergeTest {
    @Test
    fun newChapterByUrlAppendsNextIdWhileOldChapterKeepsId() {
        val p =
            Properties().apply {
                chapList =
                    listOf(
                        Chapter(url = "u1", chapName = "C1", id = "C0"),
                        Chapter(url = "u3", chapName = "C3", id = "C2"),
                    )
                size = 2
            }
        val fetched =
            listOf(
                Chapter(url = "u1", chapName = "C1"),
                Chapter(url = "u2", chapName = "C2 mới"),
                Chapter(url = "u3", chapName = "C3"),
                Chapter(url = "u4", chapName = "C4 mới"),
            )

        val newCount = p.mergeFetchedChapters(fetched)

        assertEquals(2, newCount)
        assertEquals(4, p.size)
        val ids = p.chapList!!.map { it.id }
        // cũ giữ id; mới nhận max(0,2)+1=3 trở đi — không đụng C2 cũ
        assertEquals(listOf("C0", "C2", "C3", "C4"), ids)
        assertEquals(listOf("u1", "u3", "u2", "u4"), p.chapList!!.map { it.url })
    }

    @Test
    fun unchangedTocYieldsNoNewChapters() {
        val p =
            Properties().apply {
                chapList = listOf(Chapter(url = "u1", chapName = "C1", id = "C0"))
                size = 1
            }
        assertEquals(0, p.mergeFetchedChapters(listOf(Chapter(url = "u1", chapName = "C1"))))
        assertEquals(1, p.size)
    }
}
