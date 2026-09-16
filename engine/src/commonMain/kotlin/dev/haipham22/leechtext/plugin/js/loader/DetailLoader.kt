package dev.haipham22.leechtext.plugin.js.loader

import dev.haipham22.leechtext.entities.BookEntity
import dev.haipham22.leechtext.entities.PluginEntity
import dev.haipham22.leechtext.get.isValidHttpUrl
import dev.haipham22.leechtext.log.EngineLogger

/**
 * Loader book metadata (port từ loader/DetailLoader.java; P5.2c: kết quả normalized Map).
 * Extract name/author/cover/detail/description từ execute() hoặc Response wrapper.
 */
class DetailLoader private constructor(
    log: EngineLogger,
    plugin: PluginEntity,
) : AbstractLoader<BookEntity>(log, plugin) {
    companion object {
        fun with(
            plugin: PluginEntity,
            log: EngineLogger,
        ): DetailLoader = DetailLoader(log, plugin)
    }

    override fun getScript(): String? = plugin.detailGetter?.takeIf { it.isNotEmpty() }

    override fun getLoaderType(): LoaderType = LoaderType.DETAIL

    override fun processResult(
        result: Any?,
        url: String?,
    ): BookEntity {
        // Defensive: đảm bảo URL hợp lệ trước khi gọi execute
        val baseUrl = plugin.source
        val safeUrl =
            if (url == null || url.isEmpty() || url.contains("NOT_FOUND")) baseUrl else url

        // Check Response wrapper (vBooks compatibility)
        if (!Response.isSuccess(result)) {
            val errorMsg = Response.getErrorMessage(result)
            log.add("[DetailLoader] Error response: $errorMsg")
            return BookEntity()
        }

        val data = Response.getData(result)
        return extractBookEntity(data, safeUrl)
    }

    private fun extractBookEntity(
        result: Any?,
        fallbackUrl: String?,
    ): BookEntity {
        val entity = BookEntity()

        if (result is Map<*, *>) {
            // JSResponse.getString extract string an toàn
            JSResponse.getString(result["name"])?.let { entity.name = it }

            // url từ response: chỉ dùng nếu thực sự là URL http(s) — đa số plugin
            // KHÔNG trả field url (chỉ name/cover/author/host)
            val urlStr = JSResponse.getString(result["url"])
            var resultUrl = fallbackUrl
            if (urlStr.isValidHttpUrl()) resultUrl = urlStr
            entity.url = resultUrl

            JSResponse.getString(result["detail"])?.let {
                entity.detail = it.trim().replace(Regex("\n+"), "\n")
            }

            JSResponse.getString(result["description"])?.let { entity.introduce = it }
            JSResponse.getString(result["cover"])?.let { entity.cover = it }
            JSResponse.getString(result["author"])?.let { entity.author = it }
            // ongoing: true=Đang ra / false=Hoàn thành (plugin cũ không trả → null)
            (result["ongoing"] as? Boolean)?.let { entity.ongoing = it }
        } else if (result != null) {
            // Kết quả plain string
            JSResponse.getString(result)?.let { entity.detail = it }
        }

        entity.webSource = plugin.name
        return entity
    }
}
