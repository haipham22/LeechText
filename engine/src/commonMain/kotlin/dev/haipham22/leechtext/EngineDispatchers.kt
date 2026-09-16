package dev.haipham22.leechtext

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Confined dispatcher cho Rhino script execution (eng review 1A).
 *
 * Rhino Context thread-bound: Context.enter()/exit() phải xảy ra trên cùng thread.
 * Mọi code chạm Rhino context (JsSandbox, RhinoContextPool, JsScriptEngine) phải chạy
 * trong [engine] — limitedParallelism = MAX_CONTEXTS (5) khớp kích thước pool, nên
 * không coroutine nào nhảy thread giữa enter/exit và không vượt limit context.
 */
object EngineDispatchers {
    /** Khớp JsScriptEngine.MAX_CONTEXTS — đổi thì đổi cả 2. */
    const val MAX_PARALLELISM = 5

    val engine: CoroutineDispatcher = Dispatchers.Default.limitedParallelism(MAX_PARALLELISM)

    /** Chạy block touching Rhino trên dispatcher confined. */
    suspend fun <T> withEngine(block: () -> T): T = withContext(engine) { block() }
}
