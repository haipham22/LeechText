package dev.haipham22.leechtext.plugin.js.loader

import dev.haipham22.leechtext.entities.PluginEntity
import dev.haipham22.leechtext.log.platformEngineLogger
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private const val EXECUTE_PREFIX = "function execute(url) {"
private const val URLS_NOT_NULL_MESSAGE = "URL list should not be null"
private const val TEST_SOURCE = "https://test.com"
private const val TEST_URL = "https://test.com/test"
private const val PAGE_1_URL = "https://test.com/page-1"

/**
 * Unit + integration test PageLoader (port nguyên văn từ PageLoaderTest.java) — page
 * discovery pattern cho vBook plugins. Fixture-based.
 */
class PageLoaderTest {
    @Test
    fun testBasicPageDiscovery() {
        val plugin =
            PluginEntity(
                source = "https://truyenfull.vision",
                pageGetter =
                "function execute(url) {  return Response.success([   " +
                    " 'https://truyenfull.vision/ajax.php?type=list_chapter&page=1'," +
                    "    'https://truyenfull.vision/ajax.php?type=list_chapter&page=2'," +
                    "    'https://truyenfull.vision/ajax.php?type=list_chapter&page=3'" +
                    "  ]);}",
            )

        val urls = runBlocking { PageLoader.with(plugin, platformEngineLogger()).load("https://truyenfull.vision/test-novel") }

        assertNotNull(urls, URLS_NOT_NULL_MESSAGE)
        assertEquals(3, urls.size, "Should discover 3 URLs")
        assertEquals(
            "https://truyenfull.vision/ajax.php?type=list_chapter&page=1",
            urls[0],
            "First URL should match",
        )
    }

    @Test
    fun testEmptyResult() {
        val plugin =
            PluginEntity(
                source = TEST_SOURCE,
                pageGetter = EXECUTE_PREFIX + "  return Response.success([]);" + "}",
            )

        val urls = runBlocking { PageLoader.with(plugin, platformEngineLogger()).load(TEST_URL) }

        assertNotNull(urls, URLS_NOT_NULL_MESSAGE)
        assertTrue(urls.isEmpty(), "URL list should be empty")
    }

    @Test
    fun testNullResult() {
        val plugin =
            PluginEntity(
                source = TEST_SOURCE,
                pageGetter =
                EXECUTE_PREFIX + "  return Response.success(null);" + "}",
            )

        val urls = runBlocking { PageLoader.with(plugin, platformEngineLogger()).load(TEST_URL) }

        assertNotNull(urls, URLS_NOT_NULL_MESSAGE)
        assertTrue(urls.isEmpty(), "URL list should be empty for null result")
    }

    @Test
    fun testSingleUrlResult() {
        val plugin =
            PluginEntity(
                source = TEST_SOURCE,
                pageGetter =
                EXECUTE_PREFIX +
                    "  return Response.success('https://test.com/page-1');" +
                    "}",
            )

        val urls = runBlocking { PageLoader.with(plugin, platformEngineLogger()).load(TEST_URL) }

        assertNotNull(urls, URLS_NOT_NULL_MESSAGE)
        assertEquals(1, urls.size, "Should have 1 URL")
        assertEquals(PAGE_1_URL, urls[0], "URL should match")
    }

    @Test
    fun testTruyenFullPagePattern() {
        val plugin =
            PluginEntity(
                source = "https://truyenfull.vision",
                pageGetter =
                "function execute(url) {  var totalPages = 3;  var truyenId =" +
                    " 'test-novel';  var list = [];  for (var i = 1; i <=" +
                    " totalPages; i++) {   " +
                    " list.push('https://truyenfull.vision/ajax.php?type=list_chapter&tid='" +
                    " + truyenId + '&page=' + i);  }  return" +
                    " Response.success(list);}",
            )

        val urls = runBlocking { PageLoader.with(plugin, platformEngineLogger()).load("https://truyenfull.vision/test-novel") }

        assertNotNull(urls, URLS_NOT_NULL_MESSAGE)
        assertEquals(3, urls.size, "Should discover 3 page URLs")
        assertTrue(urls[0].contains("page=1"), "First URL should contain page=1")
        assertTrue(urls[2].contains("page=3"), "Last URL should contain page=3")
    }

    @Test
    fun testNoPageGetterScript() {
        val plugin = PluginEntity(source = TEST_SOURCE)

        val urls = runBlocking { PageLoader.with(plugin, platformEngineLogger()).load(TEST_URL) }

        assertNotNull(urls, URLS_NOT_NULL_MESSAGE)
        assertTrue(urls.isEmpty(), "URL list should be empty when no script")
    }

    @Test
    fun testWithMixedUrlTypes() {
        val plugin =
            PluginEntity(
                source = TEST_SOURCE,
                pageGetter =
                EXECUTE_PREFIX +
                    "  return Response.success([" +
                    "    'https://test.com/page-1'," +
                    "    '/relative-url'," +
                    "    'http://old-domain.com/page'," +
                    "    'test.com/path'" +
                    "  ]);" +
                    "}",
            )

        val urls = runBlocking { PageLoader.with(plugin, platformEngineLogger()).load(TEST_URL) }

        assertNotNull(urls, URLS_NOT_NULL_MESSAGE)
        assertEquals(4, urls.size, "Should handle all URL types")
    }

    @Test
    fun testLargePageDiscovery() {
        val plugin =
            PluginEntity(
                source = TEST_SOURCE,
                pageGetter =
                EXECUTE_PREFIX +
                    "  var list = [];" +
                    "  for (var i = 1; i <= 100; i++) {" +
                    "    list.push('https://test.com/page-' + i);" +
                    "  }" +
                    "  return Response.success(list);" +
                    "}",
            )

        val urls = runBlocking { PageLoader.with(plugin, platformEngineLogger()).load(TEST_URL) }

        assertNotNull(urls, URLS_NOT_NULL_MESSAGE)
        assertEquals(100, urls.size, "Should discover 100 URLs")
        assertEquals(PAGE_1_URL, urls[0], "First URL should be page-1")
        assertEquals("https://test.com/page-100", urls[99], "Last URL should be page-100")
    }

    @Test
    fun testScriptErrorHandling() {
        val plugin =
            PluginEntity(
                source = TEST_SOURCE,
                pageGetter = EXECUTE_PREFIX + "  invalid javascript here" + "}",
            )

        val urls = runBlocking { PageLoader.with(plugin, platformEngineLogger()).load(TEST_URL) }

        // Handle error graceful, trả empty list
        assertNotNull(urls, "URL list should not be null on error")
        assertTrue(urls.isEmpty(), "URL list should be empty on error")
    }

    @Test
    fun testObjectWithUrlProperty() {
        val plugin =
            PluginEntity(
                source = TEST_SOURCE,
                pageGetter =
                "function execute(url) {  return Response.success({url:" +
                    " 'https://test.com/page-1'});}",
            )

        val urls = runBlocking { PageLoader.with(plugin, platformEngineLogger()).load(TEST_URL) }

        assertNotNull(urls, URLS_NOT_NULL_MESSAGE)
        assertEquals(1, urls.size, "Should extract URL from object")
        assertEquals(PAGE_1_URL, urls[0], "URL should match")
    }

    @Test
    fun testObjectWithLinkProperty() {
        val plugin =
            PluginEntity(
                source = TEST_SOURCE,
                pageGetter =
                "function execute(url) {  return Response.success({link:" +
                    " 'https://test.com/page-1'});}",
            )

        val urls = runBlocking { PageLoader.with(plugin, platformEngineLogger()).load(TEST_URL) }

        assertNotNull(urls, URLS_NOT_NULL_MESSAGE)
        assertEquals(1, urls.size, "Should extract link from object")
        assertEquals(PAGE_1_URL, urls[0], "Link should match")
    }
}
