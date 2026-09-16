package dev.haipham22.leechtext.plugin.js.loader

import dev.haipham22.leechtext.entities.PluginEntity
import dev.haipham22.leechtext.log.platformEngineLogger
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private const val EXECUTE_PREFIX = "function execute(url) {"
private const val TEST_SOURCE = "https://test.com"
private const val TEST_BOOK_URL = "https://test.com/test-book"
private const val RESULT_NOT_NULL_MESSAGE = "Result should not be null"
private const val NAME_EMPTY_ON_ERROR_MESSAGE = "Name should be null or empty on error"

/**
 * Integration test Response.error() (port nguyên văn từ ResponseErrorTest.java) — error
 * handling vBooks compatibility mode. Cover cả ListLoader dual-data.
 */
class ResponseErrorTest {
    @Test
    fun testResponseErrorWithData() {
        val plugin =
            PluginEntity(
                source = TEST_SOURCE,
                detailGetter =
                EXECUTE_PREFIX +
                    "  return Response.error('Book not found');" +
                    "}",
            )

        val result = runBlocking { DetailLoader.with(plugin, platformEngineLogger()).load(TEST_BOOK_URL) }

        // Error → BookEntity rỗng
        assertNotNull(result, RESULT_NOT_NULL_MESSAGE)
        val name = result.name
        assertTrue(name == null || name.isEmpty(), NAME_EMPTY_ON_ERROR_MESSAGE)
    }

    @Test
    fun testResponseErrorWithCode() {
        val plugin =
            PluginEntity(
                source = TEST_SOURCE,
                detailGetter =
                EXECUTE_PREFIX +
                    "  return Response.error(404, 'Not found');" +
                    "}",
            )

        val result = runBlocking { DetailLoader.with(plugin, platformEngineLogger()).load(TEST_BOOK_URL) }

        assertNotNull(result, RESULT_NOT_NULL_MESSAGE)
        val name = result.name
        assertTrue(name == null || name.isEmpty(), NAME_EMPTY_ON_ERROR_MESSAGE)
    }

    @Test
    fun testResponseErrorWithCodeAndMessage() {
        val plugin =
            PluginEntity(
                source = TEST_SOURCE,
                detailGetter =
                "function execute(url) {  return Response.error(500, 'Server" +
                    " error', 'Internal error');}",
            )

        val result = runBlocking { DetailLoader.with(plugin, platformEngineLogger()).load(TEST_BOOK_URL) }

        assertNotNull(result, RESULT_NOT_NULL_MESSAGE)
        val name = result.name
        assertTrue(name == null || name.isEmpty(), NAME_EMPTY_ON_ERROR_MESSAGE)
    }

    @Test
    fun testResponseSuccessWithCodeField() {
        val plugin =
            PluginEntity(
                source = TEST_SOURCE,
                detailGetter =
                EXECUTE_PREFIX +
                    "  return Response.success({" +
                    "    name: 'Test Novel'," +
                    "    author: 'Test Author'" +
                    "  });" +
                    "}",
            )

        val result = runBlocking { DetailLoader.with(plugin, platformEngineLogger()).load("https://test.com/test-novel") }

        assertNotNull(result, RESULT_NOT_NULL_MESSAGE)
        assertEquals("Test Novel", result.name, "Name should match")
        assertEquals("Test Author", result.author, "Author should match")
    }

    @Test
    fun testResponseSuccessDualWithCodeField() {
        val plugin =
            PluginEntity(
                source = TEST_SOURCE,
                tocGetter =
                EXECUTE_PREFIX +
                    "  return Response.success([" +
                    "    {name: 'Chapter 1', url: '/chap-1'}," +
                    "    {name: 'Chapter 2', url: '/chap-2'}" +
                    "  ], 'next-page-token');" +
                    "}",
            )

        val chapters = runBlocking { ListLoader.with(plugin, platformEngineLogger()).load(TEST_SOURCE) }

        assertNotNull(chapters, "Chapters should not be null")
        assertEquals(2, chapters.size, "Should extract 2 chapters")
        assertEquals("Chapter 1", chapters[0].name, "First chapter name")
        assertEquals("Chapter 2", chapters[1].name, "Second chapter name")
    }
}
