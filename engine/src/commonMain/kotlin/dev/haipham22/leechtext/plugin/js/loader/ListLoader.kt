package dev.haipham22.leechtext.plugin.js.loader

import dev.haipham22.leechtext.entities.ChapterEntity
import dev.haipham22.leechtext.entities.PluginEntity
import dev.haipham22.leechtext.log.EngineLogger
import dev.haipham22.leechtext.plugin.js.api.JSList
import dev.haipham22.leechtext.plugin.js.api.JsValueBridge

/**
 * Loader mục lục (port từ loader/ListLoader.java; P5.2c: kết quả normalized —
 * List (JS array), Map (array-like length/ids), JSList, Kotlin List).
 */
class ListLoader private constructor(
    log: EngineLogger,
    plugin: PluginEntity,
) : AbstractLoader<List<ChapterEntity>>(log, plugin) {
    companion object {
        fun with(
            plugin: PluginEntity,
            log: EngineLogger,
        ): ListLoader = ListLoader(log, plugin)
    }

    override fun getScript(): String? = plugin.tocGetter

    override fun getLoaderType(): LoaderType = LoaderType.LIST

    override fun processResult(
        result: Any?,
        url: String?,
    ): List<ChapterEntity> {
        log.add("[ListLoader] Loading chapters from: $url")
        log.add("[ListLoader] Result type: ${result?.let { it::class.simpleName }}")

        // Check Response wrapper (vBooks compatibility)
        if (!Response.isSuccess(result)) {
            val errorMsg = Response.getErrorMessage(result)
            log.add("[ListLoader] Error response: $errorMsg")
            return ArrayList()
        }

        val data = Response.getData(result)
        val data2 = Response.getData2(result)

        // Dual data response (pagination token)
        if (data2 != null) {
            val nextPageToken = JSResponse.getString(data2)
            if (nextPageToken != null) {
                log.add("[ListLoader] Next page token: $nextPageToken")
            }
        }

        val chapters = extractChapterList(data)
        log.add("[ListLoader] Extracted ${chapters.size} chapters")
        return chapters
    }

    private fun extractChapterList(result: Any?): List<ChapterEntity> {
        val chapterList = ArrayList<ChapterEntity>()
        try {
            when {
                result is List<*> -> {
                    collectList(result, chapterList)
                }

                result is Map<*, *> -> {
                    collectMap(result, chapterList)
                }

                result is JSList -> {
                    collectJSList(result, chapterList)
                }

                JsValueBridge.sizeOf(result) >= 0 -> {
                    collectEngineArray(result, chapterList)
                }

                else -> {
                    log.add("[ListLoader] Unknown result type: ${result?.let { it::class.simpleName }}")
                }
            }
            log.add("[ListLoader] Extraction complete: ${chapterList.size} chapters found")
        } catch (e: Exception) {
            log.add("Error parsing chapter list: ${e.message}")
        }
        return chapterList
    }

    /** Engine array (JVM NativeArray tràn qua) — index tuần tự 0..length. */
    private fun collectEngineArray(
        result: Any?,
        out: ArrayList<ChapterEntity>,
    ) {
        val size = JsValueBridge.sizeOf(result)
        log.add("[ListLoader] Result is engine array, size: $size")
        for (i in 0 until size) {
            extractChapter(JsValueBridge.itemAt(result, i), i)?.let {
                out.add(it)
            }
        }
    }

    /** Object với custom iterator — check property 'length' (array-like), fallback numeric ids. */
    private fun collectMap(
        result: Map<*, *>,
        out: ArrayList<ChapterEntity>,
    ) {
        val lengthProp = result["length"]
        if (lengthProp is Number) {
            val size = lengthProp.toInt()
            log.add("[ListLoader] Result has 'length' property: $size")
            for (i in 0 until size) {
                extractChapter(result[i], i)?.let {
                    out.add(it)
                }
            }
        } else {
            log.add(
                "[ListLoader] Result is object without numeric length -" +
                    " checking individual properties",
            )
            // Iterate qua property numeric ids
            for (id in result.keys) {
                val index = id.toString().toIntOrNull() ?: continue
                extractChapter(result[id], index)?.let {
                    out.add(it)
                }
            }
        }
    }

    private fun collectJSList(
        result: JSList,
        out: ArrayList<ChapterEntity>,
    ) {
        val size = result.size
        log.add("[ListLoader] Result is JSList, size: $size")
        for (i in 0 until size) {
            extractChapter(result[i], i)?.let { out.add(it) }
        }
    }

    private fun collectList(
        result: List<*>,
        out: ArrayList<ChapterEntity>,
    ) {
        log.add("[ListLoader] Result is List, size: ${result.size}")
        result.forEachIndexed { i, item ->
            extractChapter(item, i)?.let { out.add(it) }
        }
    }

    private fun extractChapter(
        chapterValue: Any?,
        index: Int,
    ): ChapterEntity? {
        if (chapterValue == null) return null

        val entity = ChapterEntity(id = index)

        if (chapterValue is Map<*, *>) {
            JSResponse.getString(chapterValue["name"])?.let {
                entity.name = it
            }
            JSResponse.getString(chapterValue["url"])?.let {
                entity.url = it
            }
        } else {
            // String format — chỉ có name
            JSResponse.getString(chapterValue)?.let { entity.name = it }
        }

        return entity
    }
}
