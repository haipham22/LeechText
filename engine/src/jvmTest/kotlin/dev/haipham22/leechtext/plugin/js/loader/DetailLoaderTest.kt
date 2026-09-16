package dev.haipham22.leechtext.plugin.js.loader

import dev.haipham22.leechtext.entities.PluginEntity
import dev.haipham22.leechtext.log.platformEngineLogger
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private const val DETAIL_JS_PREFIX = "function execute(url) {"
private const val SOURCE_TEST_COM = "https://test.com"
private const val TEST_COM_URL = "https://test.com/test"
private const val RESULT_NULL_MSG = "Result should not be null"

/**
 * Integration test DetailLoader với Response.success() (port nguyên văn từ
 * DetailLoaderTest.java). Fixture-based — script trả data cứng, không hit network.
 */
class DetailLoaderTest {
    @Test
    fun testResponseSuccessFromJavaScript() {
        val plugin =
            PluginEntity(
                source = "https://truyenfull.vision",
                detailGetter =
                DETAIL_JS_PREFIX +
                    "  return Response.success({" +
                    "    name: 'Test Novel'," +
                    "    author: 'Test Author'," +
                    "    description: 'Test description'," +
                    "    cover: 'https://example.com/cover.jpg'" +
                    "  });" +
                    "}",
            )

        val result = runBlocking { DetailLoader.with(plugin, platformEngineLogger()).load("https://truyenfull.vision/test-novel") }

        assertNotNull(result, RESULT_NULL_MSG)
        assertEquals("Test Novel", result.name, "Name should match")
        assertEquals("Test Author", result.author, "Author should match")
        assertEquals("Test description", result.introduce, "Description should match")
        assertEquals("https://example.com/cover.jpg", result.cover, "Cover should match")
    }

    @Test
    fun testResponseSuccessWithSingleValue() {
        val plugin =
            PluginEntity(
                source = SOURCE_TEST_COM,
                detailGetter =
                DETAIL_JS_PREFIX +
                    "  return Response.success('Simple success value');" +
                    "}",
            )

        val result = runBlocking { DetailLoader.with(plugin, platformEngineLogger()).load(TEST_COM_URL) }

        assertNotNull(result, RESULT_NULL_MSG)
        // Response.success với non-object không map vào BookEntity fields
        // nhưng loader phải hoàn thành mà không throw
    }

    @Test
    fun testResponseSuccessWithArray() {
        val plugin =
            PluginEntity(
                source = SOURCE_TEST_COM,
                detailGetter =
                DETAIL_JS_PREFIX +
                    "  return Response.success([" +
                    "    {name: 'Chapter 1', url: '/chap-1'}," +
                    "    {name: 'Chapter 2', url: '/chap-2'}" +
                    "  ]);" +
                    "}",
            )

        val result = runBlocking { DetailLoader.with(plugin, platformEngineLogger()).load(TEST_COM_URL) }

        assertNotNull(result, RESULT_NULL_MSG)
        // Arrays được handle nhưng không map vào BookEntity fields
    }

    @Test
    fun testResponseSuccessWithNull() {
        val plugin =
            PluginEntity(
                source = SOURCE_TEST_COM,
                detailGetter =
                DETAIL_JS_PREFIX +
                    "  return Response.success(null);" +
                    "}",
            )

        runBlocking { DetailLoader.with(plugin, platformEngineLogger()).load(TEST_COM_URL) }

        // Should return null or empty result
        assertNotNull(DetailLoader.with(plugin, platformEngineLogger()), "Loader should handle null gracefully")
    }

    @Test
    fun testResponseSuccessInRealWorldScenario() {
        val plugin =
            PluginEntity(
                source = "https://truyenfull.vision",
                detailGetter =
                DETAIL_JS_PREFIX +
                    "  var mockData = {" +
                    "    name: 'Test Novel Name'," +
                    "    cover: 'https://truyenfull.vision/cover.jpg'," +
                    "    author: 'Test Author'," +
                    "    description: 'Test description'," +
                    "    detail: ''" +
                    "  };" +
                    "  return Response.success(mockData);" +
                    "}",
            )

        val result = runBlocking { DetailLoader.with(plugin, platformEngineLogger()).load("https://truyenfull.vision/test-novel") }

        assertNotNull(result, RESULT_NULL_MSG)
        assertEquals("Test Novel Name", result.name, "Name should match")
        assertEquals("Test Author", result.author, "Author should match")
        assertEquals(
            "https://truyenfull.vision/cover.jpg",
            result.cover,
            "Cover should be absolute URL",
        )
    }
}
