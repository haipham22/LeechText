package dev.haipham22.leechtext.plugin.js.loader

import dev.haipham22.leechtext.EngineDispatchers
import dev.haipham22.leechtext.entities.PluginEntity
import dev.haipham22.leechtext.log.EngineLogger
import dev.haipham22.leechtext.plugin.js.sandbox.JsSandbox
import dev.haipham22.leechtext.plugin.js.sandbox.createSandbox
import dev.haipham22.leechtext.util.parseBaseUrl

/**
 * Base loader cho vBook plugin (port từ loader/AbstractLoader.java; P5.2c common —
 * engine qua JsSandbox expect/actual, Rhino context thread-bound nên load() vẫn chạy
 * trong EngineDispatchers.withEngine).
 */
abstract class AbstractLoader<T>(
    protected val log: EngineLogger,
    protected val plugin: PluginEntity,
) {
    /** Script content sẽ execute, null nếu plugin không có. */
    protected abstract fun getScript(): String?

    /** Loại loader cho sandbox isolation. */
    protected abstract fun getLoaderType(): LoaderType

    /** Tên script cho log (vd "tocGetter", "detailGetter"). */
    protected open fun getScriptName(): String = getLoaderType().scriptName

    /** Xử lý kết quả từ call execute() function. */
    protected abstract fun processResult(
        result: Any?,
        url: String?,
    ): T?

    /** Load data từ URL bằng script của plugin trong sandbox bảo mật. Null trên error. */
    open suspend fun load(url: String?): T? = EngineDispatchers.withEngine { loadInSandbox(url) }

    /** Thân legacy AbstractLoader.load — chạy trên engine dispatcher. */
    @Suppress("ReturnCount") // guard-chain: từng bước fail thoát sớm, lifecycle sandbox/builder giữ nguyên shape
    protected open fun loadInSandbox(url: String?): T? {
        val script = getScript() ?: return null

        var sandbox: JsSandbox? = null
        try {
            // Extract base URL
            val baseUrl =
                plugin.source?.takeIf { it.isNotEmpty() } ?: parseBaseUrl(url ?: "")

            // Create secure sandbox cho loader type này
            sandbox =
                createSandbox(log) {
                    loaderType = getLoaderType()
                    this.baseUrl = baseUrl
                    targetUrl = url
                    if (!plugin.source.isNullOrEmpty()) pluginSource = plugin.source
                    // Plugin config (vBook extension-api) — inject làm JS constants
                    if (!plugin.config.isNullOrEmpty()) pluginConfig = plugin.config
                    if (!plugin.extraScripts.isNullOrEmpty()) extraScripts = plugin.extraScripts
                }

            // Execute plugin script trong sandbox
            if (!sandbox.execute(script, getScriptName())) return null

            // Call execute(url) trong sandbox — kết quả đã normalize
            val result = sandbox.callFunction("execute", url)
            if (result == null) return null

            return processResult(result, url)
        } catch (e: Exception) {
            val errorMsg = e.message ?: e::class.simpleName
            log.add("JavaScript execution error in ${getScriptName()}: $errorMsg")
            return null
        } finally {
            sandbox?.close()
        }
    }
}
