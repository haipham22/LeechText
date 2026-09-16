package dev.haipham22.leechtext.plugin.js.loader

import dev.haipham22.leechtext.entities.PluginEntity
import dev.haipham22.leechtext.log.platformEngineLogger
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * BrowseLoader phải resolve script TÊN TUỲ Ý từ package plugin (vd gen1.js —
 * menu của plugin vBook trả script đó, trước đây chỉ map 5 key cố định →
 * "thiếu script gen1" → browse chết). Offline: script execute không gọi network.
 */
class BrowseLoaderArbitraryScriptTest {
    @Test
    fun novelsResolvesArbitraryScriptFromExtraScripts() {
        val plugin =
            PluginEntity(
                name = "test",
                source = "https://example.com",
                regex = "example\\.com",
            ).apply {
                extraScripts =
                    mapOf(
                        "gen1.js" to
                            """
                            function execute(url, page) {
                                return Response.success(
                                    [{name: "Truyện A", link: "/a", cover: "", description: "", host: BASE_URL}],
                                    "2",
                                );
                            }
                            """.trimIndent(),
                    )
            }
        val result = runBlocking { BrowseLoader.with(plugin, platformEngineLogger()).novels("gen1", "slug", "1") }
        assertNotNull(result, "novels() phải chạy được script gen1 từ extraScripts")
        assertEquals(1, result.novels.size)
        assertEquals("Truyện A", result.novels[0].name)
        assertEquals("2", result.nextPage)
    }

    private fun assertEquals(
        expected: Any?,
        actual: Any?,
    ) {
        assertTrue(expected == actual, "expected=$expected actual=$actual")
    }
}
