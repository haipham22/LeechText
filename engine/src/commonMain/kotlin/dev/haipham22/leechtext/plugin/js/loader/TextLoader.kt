package dev.haipham22.leechtext.plugin.js.loader

import dev.haipham22.leechtext.entities.PluginEntity
import dev.haipham22.leechtext.log.EngineLogger

/**
 * Loader nội dung chương (port từ loader/TextLoader.java; P5.2c: kết quả normalized).
 * Chạy script chapGetter, trả về text chương — hỗ trợ {body|content|text} object
 * hoặc string trực tiếp.
 */
class TextLoader private constructor(
    log: EngineLogger,
    plugin: PluginEntity,
) : AbstractLoader<String>(log, plugin) {
    companion object {
        fun with(
            plugin: PluginEntity,
            log: EngineLogger,
        ): TextLoader = TextLoader(log, plugin)
    }

    override fun getScript(): String? = plugin.chapGetter

    override fun getLoaderType(): LoaderType = LoaderType.TEXT

    override fun processResult(
        result: Any?,
        url: String?,
    ): String? {
        // Check Response wrapper (vBooks compatibility)
        if (!Response.isSuccess(result)) {
            val errorMsg = Response.getErrorMessage(result)
            log.add("[TextLoader] Error response: $errorMsg")
            return ""
        }

        val data = Response.getData(result)

        // Object với properties — lấy field đầu tiên có giá trị (body/content/text)
        val fromMap =
            (data as? Map<*, *>)
                ?.let { map -> listOf("body", "content", "text").firstNotNullOfOrNull { JSResponse.getString(map[it]) } }

        // String trực tiếp
        return fromMap ?: JSResponse.getString(data)
    }
}
