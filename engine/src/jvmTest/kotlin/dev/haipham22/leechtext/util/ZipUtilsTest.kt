package dev.haipham22.leechtext.util

import dev.haipham22.leechtext.log.platformEngineLogger
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ZipUtilsTest {
    @Test
    fun addToZipThenReadZipEntryAsStringRoundTrips() {
        val tmp = createTempDirectory("leech-zip").toFile()
        val src = tmp.resolve("hello.txt")
        "xin chào".writeTo(src.path, log = platformEngineLogger())
        val zip = tmp.resolve("book.zip")

        addToZip(zip, src)

        assertEquals("xin chào", readZipEntryAsString(zip, "hello.txt"))
        assertTrue(zip.length() > 0)
    }

    @Test
    fun addFolderToZipAddsContentUnderFolderName() {
        val tmp = createTempDirectory("leech-zip-dir").toFile()
        val dir = tmp.resolve("Text").apply { mkdirs() }
        "<p>nội dung</p>".writeTo(dir.resolve("C1.html").path, log = platformEngineLogger())
        val zip = tmp.resolve("book2.zip")

        // rootFolder rỗng — như cách Ebook export dùng (entry = "Text/C1.html")
        addFolderToZip(zip, dir)

        assertEquals("<p>nội dung</p>", readZipEntryAsString(zip, "Text/C1.html"))
    }

    @Test
    fun readZipEntryMissingEntryReturnsEmpty() {
        val tmp = createTempDirectory("leech-zip-miss").toFile()
        val src = tmp.resolve("a.txt")
        "a".writeTo(src.path, log = platformEngineLogger())
        val zip = tmp.resolve("book3.zip")
        addToZip(zip, src)

        assertEquals(0, readZipEntry(zip, "nope.txt").size)
    }
}
