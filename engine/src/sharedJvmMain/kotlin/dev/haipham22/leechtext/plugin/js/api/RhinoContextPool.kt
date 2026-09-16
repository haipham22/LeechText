package dev.haipham22.leechtext.plugin.js.api

import dev.haipham22.leechtext.log.EngineLogger
import org.mozilla.javascript.Context
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Pool Rhino Context với resource limits (port từ RhinoContextPool.java).
 * Lưu ý thread-affinity: Context.enter/exit phải xảy ra trên CÙNG thread —
 * dùng trong coroutines qua dispatcher confined (EngineDispatchers, T3).
 */
@Suppress("LongParameterList") // tham số log DI + params nghiệp vụ — bundle làm code khó đọc
class RhinoContextPool(
    private val log: EngineLogger,
    private val maxContexts: Int,
    @Suppress("unused") private val timeoutMs: Int,
    @Suppress("unused") private val maxMemoryMB: Int,
) {
    private val pool = LinkedBlockingQueue<RhinoPooledContext>(maxContexts)
    private val activeContexts = AtomicInteger(0)
    private val createdContexts = AtomicInteger(0)

    @Throws(ResourceExhaustedException::class)
    fun borrowContext(): RhinoPooledContext {
        // Lấy từ pool trước
        pool.poll()?.let { return it }

        // Tạo mới nếu còn trong limit
        if (activeContexts.get() < maxContexts) {
            activeContexts.incrementAndGet()
            try {
                return createNewContext()
            } catch (e: RuntimeException) {
                activeContexts.decrementAndGet()
                // fall through để đợi
            }
        }

        // Đợi context trả về
        return try {
            pool.poll(5, TimeUnit.SECONDS)
                ?: throw ResourceExhaustedException(
                    "Timeout waiting for available JavaScript context. " +
                        "Current active: ${activeContexts.get()}/$maxContexts",
                )
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw ResourceExhaustedException("Interrupted while waiting for context")
        }
    }

    fun returnContext(pooledContext: RhinoPooledContext?) {
        if (pooledContext == null) return
        try {
            val context = pooledContext.context

            // Reset state scope + APIs
            try {
                val scope = context.initStandardObjects()
                initializeApis(context, scope)
                if (!pool.offer(pooledContext)) {
                    // Pool đầy — đóng context
                    Context.exit()
                    activeContexts.decrementAndGet()
                    createdContexts.decrementAndGet()
                }
            } catch (e: Exception) {
                log.add("Error returning context to pool: ${e.message}")
                try {
                    Context.exit()
                } catch (ex: Exception) {
                    // ignore
                }
                activeContexts.decrementAndGet()
                createdContexts.decrementAndGet()
            }
        } catch (e: Exception) {
            log.add("Error in returnContext: ${e.message}")
        }
    }

    fun invalidateContext(pooledContext: RhinoPooledContext?) {
        if (pooledContext == null) return
        try {
            pooledContext.context.let { Context.exit() }
        } catch (e: Exception) {
            // ignore exit errors
        } finally {
            activeContexts.decrementAndGet()
            createdContexts.decrementAndGet()
        }
    }

    private fun createNewContext(): RhinoPooledContext = try {
        val context = Context.enter()
        context.optimizationLevel = -1
        context.setMaximumInterpreterStackDepth(1000)

        val scope = context.initStandardObjects()
        initializeApis(context, scope)

        val pooled = RhinoPooledContext(context)
        createdContexts.incrementAndGet()
        log.add("Created new Rhino context (${createdContexts.get()}/$maxContexts)")
        pooled
    } catch (e: Exception) {
        activeContexts.decrementAndGet()
        Context.exit()
        throw IllegalStateException("Failed to create JavaScript context", e)
    }

    private fun initializeApis(
        context: Context,
        scope: org.mozilla.javascript.Scriptable,
    ) {
        try {
            context.evaluateString(
                scope,
                "const console = { " +
                    "log: function(msg) { print(msg); }, " +
                    "error: function(msg) { print('[ERROR] ' + msg); }, " +
                    "warn: function(msg) { print('[WARN] ' + msg); } " +
                    "};",
                "console",
                1,
                null,
            )
        } catch (e: Exception) {
            log.add("Failed to initialize console API: ${e.message}")
        }
    }

    val activeCount: Int get() = activeContexts.get()
    val createdCount: Int get() = createdContexts.get()
    val availableCount: Int get() = pool.size

    class ResourceExhaustedException(
        message: String,
    ) : Exception(message)
}
