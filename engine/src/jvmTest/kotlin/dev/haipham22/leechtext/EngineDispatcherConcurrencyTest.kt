package dev.haipham22.leechtext

import dev.haipham22.leechtext.log.platformEngineLogger
import dev.haipham22.leechtext.plugin.js.loader.LoaderType
import dev.haipham22.leechtext.plugin.js.sandbox.createSandbox
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Eng review 1A: Rhino Context thread-bound — download 5 chapter song song qua
 * engineDispatcher confined phải chạy sạch, không leak context.
 */
class EngineDispatcherConcurrencyTest {
    @Test
    fun fiveConcurrentSandboxesExecuteWithoutContextLeak() = runBlocking {
        val jobs =
            (1..5).map { i ->
                async {
                    EngineDispatchers.withEngine {
                        val sandbox =
                            createSandbox(platformEngineLogger()) {
                                loaderType = LoaderType.TEXT
                                baseUrl = "https://example.com"
                                targetUrl = "https://example.com/chapter-$i"
                                pluginSource = null
                            }
                        try {
                            sandbox.execute("function getChapter() { return $i * 10; }", "chapter-$i")
                            sandbox.callFunction("getChapter")
                        } finally {
                            sandbox.close()
                        }
                    }
                }
            }
        val results = jobs.awaitAll()
        // Mỗi sandbox trả đúng kết quả — không race/leak context
        assertEquals(listOf(10, 20, 30, 40, 50), results.map { (it as Number).toInt() })
    }

    @Test
    fun parallelismCappedAtMax() {
        // limitedParallelism(5) — dispatcher không bao giờ chạy quá 5 block Rhino cùng lúc
        assertEquals(5, EngineDispatchers.MAX_PARALLELISM)
    }
}
