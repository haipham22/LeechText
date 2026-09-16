package dev.haipham22.leechtext.plugin.js.loader

import dev.haipham22.leechtext.entities.PluginEntity
import dev.haipham22.leechtext.log.platformEngineLogger
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

private const val TEST_CHAP_URL = "https://test.com/chap-1"

/**
 * Test TextLoader — body/content/text props + plain string + error response (không có bản
 * legacy riêng, viết mới theo pattern DetailLoaderTest — script fixture, không network).
 * Fallback chain body → content → text chỉ được exercise khi prop trước là null (prop
 * ABSENT trả UniqueTag.NOT_FOUND — hành vi legacy, contract đóng băng).
 */
class TextLoaderTest {
    @Test
    fun testObjectWithBodyProperty() {
        val plugin = textPlugin("  return Response.success({body: 'Chapter body text'});")

        val text = runBlocking { TextLoader.with(plugin, platformEngineLogger()).load(TEST_CHAP_URL) }

        assertEquals("Chapter body text", text, "Should extract body")
    }

    @Test
    fun testObjectWithContentFallsBackFromNullBody() {
        val plugin =
            textPlugin("  return Response.success({body: null, content: 'Chapter content'});")

        val text = runBlocking { TextLoader.with(plugin, platformEngineLogger()).load(TEST_CHAP_URL) }

        assertEquals("Chapter content", text, "Should fall back to content when body null")
    }

    @Test
    fun testObjectWithTextFallsBackFromNullBodyAndContent() {
        val plugin =
            textPlugin("  return Response.success({body: null, content: null, text: 'Chapter text'});")

        val text = runBlocking { TextLoader.with(plugin, platformEngineLogger()).load(TEST_CHAP_URL) }

        assertEquals("Chapter text", text, "Should fall back to text when body/content null")
    }

    @Test
    fun testDirectStringResult() {
        val plugin = textPlugin("  return Response.success('Plain text');")

        val text = runBlocking { TextLoader.with(plugin, platformEngineLogger()).load(TEST_CHAP_URL) }

        assertEquals("Plain text", text, "Should return direct string")
    }

    @Test
    fun testErrorResponseReturnsEmpty() {
        val plugin = textPlugin("  return Response.error('Not found');")

        val text = runBlocking { TextLoader.with(plugin, platformEngineLogger()).load(TEST_CHAP_URL) }

        assertNotNull(text, "Should not be null on error")
        assertEquals("", text, "Should be empty on error")
    }

    @Test
    fun testNoScriptReturnsNull() {
        val plugin = PluginEntity(source = "https://test.com")

        val text = runBlocking { TextLoader.with(plugin, platformEngineLogger()).load(TEST_CHAP_URL) }

        assertNull(text, "Should be null when no script")
    }

    private fun textPlugin(script: String): PluginEntity = PluginEntity(
        source = "https://test.com",
        chapGetter = "function execute(url) {\n$script\n}",
    )
}
