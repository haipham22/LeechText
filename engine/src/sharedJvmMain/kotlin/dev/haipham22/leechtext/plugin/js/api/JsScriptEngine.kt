package dev.haipham22.leechtext.plugin.js.api

import dev.haipham22.leechtext.log.EngineLogger
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.NativeArray
import org.mozilla.javascript.Scriptable

/**
 * Script engine với pooling + resource limits (port từ JsScriptEngine.java).
 */
class JsScriptEngine private constructor(
    log: EngineLogger,
) {
    private val contextPool = RhinoContextPool(log, MAX_CONTEXTS, MAX_EXECUTION_TIME_MS, MAX_MEMORY_MB)

    fun execute(script: String): Any? {
        var pooledContext: RhinoPooledContext? = null
        var ctx: Context? = null
        try {
            pooledContext = contextPool.borrowContext()
            ctx = pooledContext.context
            ctx.languageVersion = 200
            val scope = ctx.initStandardObjects()
            return ctx.evaluateString(scope, script, "script", 1, null)
        } catch (e: Exception) {
            pooledContext?.let { contextPool.invalidateContext(it) }
            // Đặt null để finally không return lại context vừa invalidate mà exit Context
            pooledContext = null
            // invalidateContext đã Context.exit() — null luôn ctx để finally không exit lần 2
            ctx = null
            throw ExecutionException("Script execution failed: ${e.message}", e)
        } finally {
            if (pooledContext != null) {
                contextPool.returnContext(pooledContext)
            } else {
                ctx?.let { Context.exit() }
            }
        }
    }

    @Suppress("ThrowsCount") // validation chain ở trust boundary — mỗi guard ném exception có message riêng cho UI
    fun executeFunction(
        script: String,
        functionName: String,
        vararg args: Any?,
    ): Any? {
        var pooledContext: RhinoPooledContext? = null
        var ctx: Context? = null
        try {
            pooledContext = contextPool.borrowContext()
            ctx = pooledContext.context
            ctx.languageVersion = 200
            val scope = ctx.initStandardObjects()

            ctx.evaluateString(scope, script, "script", 1, null)

            val functionObj = scope[functionName, scope]
            if (functionObj !is Function) {
                throw ExecutionException("Function '$functionName' not found or not executable")
            }
            return functionObj.call(ctx, scope, scope, args)
        } catch (e: ExecutionException) {
            throw e
        } catch (e: Exception) {
            throw ExecutionException("Function execution failed: ${e.message}", e)
        } finally {
            if (pooledContext != null) {
                contextPool.returnContext(pooledContext)
            } else {
                ctx?.let { Context.exit() }
            }
        }
    }

    fun createObject(map: Map<String, Any?>): Scriptable {
        var pooled: RhinoPooledContext? = null
        return try {
            pooled = contextPool.borrowContext()
            val ctx = pooled.context
            val scope = ctx.initStandardObjects()
            val result = ctx.newObject(scope)
            for ((key, value) in map) result.put(key, result, value)
            result
        } catch (e: Exception) {
            throw IllegalStateException("Failed to create object", e)
        } finally {
            pooled?.let {
                try {
                    contextPool.returnContext(it)
                } catch (ex: Exception) {
                    // ignore
                }
            }
        }
    }

    fun createArray(array: Array<Any?>): NativeArray {
        var pooled: RhinoPooledContext? = null
        return try {
            pooled = contextPool.borrowContext()
            val ctx = pooled.context
            val scope = ctx.initStandardObjects()
            ctx.newArray(scope, array) as NativeArray
        } catch (e: Exception) {
            throw IllegalStateException("Failed to create array", e)
        } finally {
            pooled?.let {
                try {
                    contextPool.returnContext(it)
                } catch (ex: Exception) {
                    // ignore
                }
            }
        }
    }

    class ExecutionException(
        message: String,
        cause: Throwable? = null,
    ) : Exception(message, cause)

    companion object {
        private const val MAX_CONTEXTS = 5
        private const val MAX_EXECUTION_TIME_MS = 30_000
        private const val MAX_MEMORY_MB = 50

        private val LOCK = Any()
        private var engine: JsScriptEngine? = null

        @JvmStatic
        fun getInstance(log: EngineLogger): JsScriptEngine {
            if (engine == null) {
                synchronized(LOCK) {
                    if (engine == null) {
                        engine = JsScriptEngine(log)
                        log.add("Rhino JavaScript engine initialized")
                    }
                }
            }
            return engine!!
        }

        @JvmStatic
        fun resetInstance() {
            synchronized(LOCK) { engine = null }
        }
    }
}
