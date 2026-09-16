package dev.haipham22.leechtext.plugin.js.loader

import dev.haipham22.leechtext.EngineDispatchers
import dev.haipham22.leechtext.entities.PluginEntity
import dev.haipham22.leechtext.log.EngineLogger
import dev.haipham22.leechtext.plugin.js.sandbox.JsSandbox
import dev.haipham22.leechtext.plugin.js.sandbox.createSandbox

/** Mục menu browse (home/genre): trỏ tới script + input kế tiếp. */
data class BrowseTab(
    val title: String? = null,
    val input: String? = null,
    val script: String? = null,
)

/** 1 truyện trong catalog nguồn (tab/gen/search). */
data class BrowseNovel(
    val name: String? = null,
    val link: String? = null,
    val cover: String? = null,
    val description: String? = null,
    val host: String? = null,
)

/** Kết quả list truyện + pagination token (data2 của Response). */
data class BrowseNovels(
    val novels: List<BrowseNovel>,
    val nextPage: String? = null,
)

/**
 * Loader browse nguồn (port từ loader/BrowseLoader.java; P5.2c: kết quả normalized).
 * Format quan sát từ plugin thật (Bạch Ngọc Sách):
 *   home.js:  execute() → Response.success([{title, input, script:"tab.js"}])
 *   genre.js: execute() → Response.success([{title, input, script:"gen.js"}])
 *   tab/gen:  execute(url, page) → Response.success([{name,link,cover,description,host}], next)
 *   search:   execute(key, page) → same
 */
class BrowseLoader(
    private val plugin: PluginEntity,
    private val log: EngineLogger,
) {
    companion object {
        fun with(
            plugin: PluginEntity,
            log: EngineLogger,
        ): BrowseLoader = BrowseLoader(plugin, log)
    }

    private fun scriptFor(scriptName: String): String? = when (scriptName) {
        "home" -> plugin.homeGetter
        "genre" -> plugin.genreGetter
        "tab" -> plugin.tabGetter
        "gen" -> plugin.genGetter
        "search" -> plugin.searchGetter
        else -> null
        // Plugin vBook dùng tên script tuỳ ý (vd gen1.js, gen2.js) — menu trả về
        // script đó, phải resolve từ package plugin thay vì chỉ 5 key cố định
    } ?: plugin.extraScripts?.get("$scriptName.js")

    /** Menu (home/genre) — tabs dẫn tới script list truyện. */
    suspend fun menu(scriptName: String): List<BrowseTab>? = EngineDispatchers.withEngine {
        val result = run(scriptName, emptyList()) ?: return@withEngine null
        if (!Response.isSuccess(result)) {
            log.add("[BrowseLoader] $scriptName error: ${Response.getErrorMessage(result)}")
            return@withEngine null
        }
        parseTabs(Response.getData(result))
    }

    /** List truyện (tab/gen/search) + pagination. */
    suspend fun novels(
        scriptName: String,
        input: String?,
        page: String? = "0",
    ): BrowseNovels? = EngineDispatchers.withEngine {
        val result = run(scriptName, listOf(input, page ?: "0")) ?: return@withEngine null
        if (!Response.isSuccess(result)) {
            log.add("[BrowseLoader] $scriptName error: ${Response.getErrorMessage(result)}")
            return@withEngine null
        }
        BrowseNovels(
            novels = parseNovels(Response.getData(result)),
            nextPage = JSResponse.getString(Response.getData2(result)),
        )
    }

    /** Chạy 1 script với args trong sandbox riêng (scope LIST — fetch + html như toc). */
    @Suppress("ReturnCount") // guard-chain: từng bước fail thoát sớm, lifecycle sandbox/builder giữ nguyên shape
    private fun run(
        scriptName: String,
        args: List<Any?>,
    ): Any? {
        val script =
            scriptFor(scriptName) ?: run {
                log.add("[BrowseLoader] plugin '${plugin.name}' thiếu script $scriptName")
                return null
            }
        var sandbox: JsSandbox? = null
        try {
            val base = plugin.source?.takeIf { it.isNotEmpty() } ?: (args.firstOrNull() as? String) ?: ""
            sandbox =
                createSandbox(log) {
                    loaderType = LoaderType.LIST
                    baseUrl = base
                    targetUrl = (args.firstOrNull() as? String) ?: base
                    if (!plugin.source.isNullOrEmpty()) pluginSource = plugin.source
                    if (!plugin.config.isNullOrEmpty()) pluginConfig = plugin.config
                    if (!plugin.extraScripts.isNullOrEmpty()) extraScripts = plugin.extraScripts
                }

            if (!sandbox.execute(script, scriptName)) return null
            @Suppress("SpreadOperator") // vararg bridge vào sandbox
            val result = sandbox.callFunction("execute", *args.toTypedArray())
            return result
        } catch (e: Exception) {
            log.add("[BrowseLoader] $scriptName error: ${e.message ?: e::class.simpleName}")
            return null
        } finally {
            sandbox?.close()
        }
    }

    private fun parseTabs(data: Any?): List<BrowseTab>? {
        if (data !is List<*>) return null
        return data.filterIsInstance<Map<*, *>>().map { obj ->
            BrowseTab(
                title = JSResponse.getString(obj["title"]),
                input = JSResponse.getString(obj["input"]),
                script = JSResponse.getString(obj["script"]),
            )
        }
    }

    private fun parseNovels(data: Any?): List<BrowseNovel> {
        if (data !is List<*>) return emptyList()
        return data.filterIsInstance<Map<*, *>>().map { obj ->
            BrowseNovel(
                name = JSResponse.getString(obj["name"]),
                link = JSResponse.getString(obj["link"]),
                cover = JSResponse.getString(obj["cover"]),
                description = JSResponse.getString(obj["description"]),
                host = JSResponse.getString(obj["host"]),
            )
        }
    }
}
