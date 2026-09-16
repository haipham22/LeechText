package dev.haipham22.leechtext.util

import dev.haipham22.leechtext.log.platformEngineLogger
import okio.Path.Companion.toPath
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FileUtilsTest {
    @Test
    fun writeToThenReadTextOrNullRoundTripsUtf8() {
        val tmp = createTempDirectory("leech-file-utils").toFile()
        val path = tmp.resolve("a/b/test.txt").path

        "Trường Sa Hoàng Sa".writeTo(path, log = platformEngineLogger())

        assertEquals("Trường Sa Hoàng Sa", path.toPath().readTextOrNull())
    }

    @Test
    fun readTextOrNullMissingFileReturnsNull() {
        assertNull("/nonexistent/xyz.txt".toPath().readTextOrNull())
    }

    @Test
    fun writeToCreatesParentDirectories() {
        val tmp = createTempDirectory("leech-write").toFile()
        val f = tmp.resolve("data/Text/nested/deep.txt")
        "x".writeTo(f.path, log = platformEngineLogger())
        assertTrue(f.exists())
    }

    @Test
    fun deleteRemovesExistingFile() {
        val tmp = createTempDirectory("leech-del").toFile()
        val f = tmp.resolve("f.txt")
        "x".writeTo(f.path, log = platformEngineLogger())
        f.delete()
        assertFalse(f.exists())
    }

    @Test
    fun readResourceLoadsClasspathTemplate() {
        val ncx = readResource("/dark/leech/res/toc.ncx")
        assertTrue(ncx.contains("[NAVPOINT]"), "toc.ncx template phải có placeholder NAVPOINT")
        assertEquals("", readResource("/no/such/resource"))
    }
}
