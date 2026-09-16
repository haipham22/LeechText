package dev.haipham22.leechtext.plugin.js.loader

import dev.haipham22.leechtext.entities.PluginEntity
import dev.haipham22.leechtext.log.EngineLogger
import dev.haipham22.leechtext.plugin.js.api.JsValueBridge
import dev.haipham22.leechtext.plugin.js.sandbox.JsSandbox
import dev.haipham22.leechtext.plugin.js.sandbox.createSandbox
import dev.haipham22.leechtext.util.parseBaseUrl

/**
 * Loader page discovery (port từ loader/PageLoader.java; P5.2c: kết quả normalized,
 * NativeJavaObject unwrap đã làm ở boundary JsSandbox actual). Chạy script 'page',
 * trả về list URL các trang chapter — dùng cho site phân trang.
 */
class PageLoader private constructor(
    log: EngineLogger,
    plugin: PluginEntity,
) : AbstractLoader<List<String>>(log, plugin) {
    companion object {
        private val URL_PROPS = arrayOf("url", "link", "href", "page", "pageUrl")

        fun with(
            plugin: PluginEntity,
            log: EngineLogger,
        ): PageLoader = PageLoader(log, plugin)
    }

    override fun getScript(): String? = plugin.pageGetter

    override fun getLoaderType(): LoaderType = LoaderType.PAGE

    /**
     * One-shot discovery: trả về mọi page URL có sẵn. Empty list trên error (khác null của
     * AbstractLoader). Không set pluginSource (theo bản gốc — page script không cần load()).
     */
    override suspend fun load(url: String?): List<String> {
        if (getScript() == null) {
            log.add("[PageLoader] No script available")
            return ArrayList()
        }
        return super.load(url) ?: ArrayList()
    }

    @Suppress("ReturnCount") // guard-chain: từng bước fail thoát sớm, lifecycle sandbox/builder giữ nguyên shape
    override fun loadInSandbox(url: String?): List<String>? {
        val script = getScript() ?: return ArrayList()

        var sandbox: JsSandbox? = null
        try {
            // Extract base URL
            val baseUrl =
                plugin.source?.takeIf { it.isNotEmpty() } ?: parseBaseUrl(url ?: "")

            // Secure sandbox cho PAGE loader
            sandbox =
                createSandbox(log) {
                    loaderType = getLoaderType()
                    this.baseUrl = baseUrl
                    targetUrl = url
                }

            // Execute plugin script trong sandbox
            if (!sandbox.execute(script, getScriptName())) return ArrayList()

            // Call execute(url) (không có page parameter)
            val result = sandbox.callFunction("execute", url)
            if (result == null) return ArrayList()

            return processResult(result, url)
        } catch (e: Exception) {
            val errorMsg = e.message ?: e::class.simpleName
            log.add("[PageLoader] JavaScript execution error: $errorMsg")
            return ArrayList()
        } finally {
            sandbox?.close()
        }
    }

    override fun processResult(
        result: Any?,
        url: String?,
    ): List<String> {
        if (result == null) {
            log.add("[PageLoader] Script returned null or undefined")
            return ArrayList()
        }

        // Check Response wrapper (vBooks compatibility)
        if (!Response.isSuccess(result)) {
            val errorMsg = Response.getErrorMessage(result)
            log.add("[PageLoader] Error response: $errorMsg")
            return ArrayList()
        }

        val data = Response.getData(result)

        log.add("[PageLoader] Processing result, type: ${result::class.simpleName}")
        val urlList = convertToStringList(data)
        log.add("[PageLoader] Discovered ${urlList.size} page URLs")
        return urlList
    }

    /** Convert giá trị → List<String> URLs (List/Map single object/array-like). */
    private fun convertToStringList(data: Any?): List<String> {
        if (data == null) return ArrayList()

        if (data is List<*>) return itemsToStrings(data)

        if (data is Map<*, *>) {
            // Single object — thử extract property URL
            val url = extractUrlFromObject(data)
            return if (url != null) arrayListOf(url) else ArrayList()
        }

        if (JsValueBridge.sizeOf(data) >= 0) return itemsToStrings(JSResponse.convertArray(data))

        // Single item — convert sang string
        val str = convertToString(data)
        return if (str != null) arrayListOf(str) else ArrayList()
    }

    /** Iterable items → List<String> (bỏ item không stringify được). */
    private fun itemsToStrings(items: List<*>): List<String> {
        val list = ArrayList<String>(items.size)
        for (item in items) {
            convertToString(item)?.let { list.add(it) }
        }
        return list
    }

    /** Object → String URL. Giữ cả relative (contract PageLoaderTest) — chỉ loại rác
     * stringify của object gây "Expected URL scheme" xa hơn. */
    private fun convertToString(obj: Any?): String? {
        if (obj == null) return null
        if (obj is Map<*, *>) return null
        val str = JSResponse.getString(obj)?.trim() ?: return null
        if (str.isEmpty()) return null
        if (" " in str || str.contains("@") || str.contains("org.mozilla") || str.contains("com.dokar")) return null
        return str
    }

    /** Extract URL từ object (check các property name phổ biến). */
    private fun extractUrlFromObject(obj: Map<*, *>): String? {
        for (prop in URL_PROPS) {
            val value = obj[prop] ?: continue
            val url = convertToString(value)
            if (!url.isNullOrEmpty()) return url
        }
        return null
    }
}
