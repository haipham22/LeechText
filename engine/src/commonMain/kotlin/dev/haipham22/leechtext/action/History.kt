package dev.haipham22.leechtext.action

import dev.haipham22.leechtext.log.EngineLogger
import dev.haipham22.leechtext.models.Chapter
import dev.haipham22.leechtext.models.Properties
import dev.haipham22.leechtext.util.nfc
import dev.haipham22.leechtext.util.readTextOrNull
import dev.haipham22.leechtext.util.writeTo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okio.Path.Companion.toPath

/**
 * properties.json per-book (port từ action/History.java; P5.2c: org.json → kotlinx,
 * format byte-compatible: key order insert, null field được BỎ KHÔNG ghi — như
 * JSONObject.put(key, null) remove key. FormatCompatTest + HistoryTest guard).
 */

/** Đọc properties.json; null nếu file không tồn tại. */
fun loadHistory(
    path: String,
    log: EngineLogger,
): Properties? {
    val properties = Properties()
    try {
        val text = path.toPath().readTextOrNull() ?: return null
        val obj = Json.parseToJsonElement(text).jsonObject
        obj["metadata"]?.jsonObject?.let { applyHistoryMetadata(properties, obj, it) }
    } catch (e: Exception) {
        log.add(e)
    }
    return properties
}

/** Áp metadata + danh sách chương từ JSON vào properties. */
private fun applyHistoryMetadata(
    properties: Properties,
    obj: JsonObject,
    metadata: JsonObject,
) {
    properties.name = metadata.str("name")
    properties.author = metadata.str("author")
    properties.url = metadata.str("url")
    metadata.str("cover")?.let { properties.setCover(it, "") }
    properties.size = metadata["size"]?.jsonPrimitive?.intOrNull ?: 0
    properties.savePath = metadata.str("path")
    properties.introduce = metadata.str("gioithieu")
    metadata["ongoing"]?.jsonPrimitive?.booleanOrNull?.let { properties.ongoing = it }
    obj["list"]?.jsonArray?.let { array ->
        val list = ArrayList<Chapter>()
        for (item in array) list.add(toChapter(item.jsonObject))
        properties.chapList = list
        markCompletedFromDisk(properties, list)
    }
}

/** completed flag là in-memory (không persist) — sau restart mọi flag mất,
 * export lọc theo completed → epub 0 byte dù raw/ đầy file. Root fix: đánh
 * completed theo file raw tồn tại (source of truth = đĩa). */
private fun markCompletedFromDisk(
    properties: Properties,
    list: List<Chapter>,
) {
    val savePath = properties.savePath ?: return
    val rawDir = "$savePath/raw".toPath()
    list.forEach { ch ->
        runCatching {
            val raw = rawDir / "${ch.id}.txt"
            val size = okio.FileSystem.SYSTEM.metadataOrNull(raw)?.size ?: 0L
            if (size > 0L) ch.completed = true
        }
    }
}

/** Ghi properties.json vào {savePath}/properties.json. Null field BỎ KHÔNG ghi
 * (org.json put(key, null) remove key — giữ byte-compat). */
fun saveHistory(
    properties: Properties,
    log: EngineLogger,
) {
    val metadata = LinkedHashMap<String, kotlinx.serialization.json.JsonElement>()
    properties.url?.let { metadata["url"] = JsonPrimitive(it) }
    properties.name?.let { metadata["name"] = JsonPrimitive(it) }
    properties.author?.let { metadata["author"] = JsonPrimitive(it) }
    properties.cover?.let { metadata["cover"] = JsonPrimitive(it) }
    metadata["size"] = JsonPrimitive(properties.size)
    properties.savePath?.let { metadata["path"] = JsonPrimitive(it) }
    properties.introduce?.let { metadata["gioithieu"] = JsonPrimitive(it) }
    properties.ongoing?.let { metadata["ongoing"] = JsonPrimitive(it) }
    val his = LinkedHashMap<String, kotlinx.serialization.json.JsonElement>()
    his["metadata"] = JsonObject(metadata)
    // danh sách chương
    his["list"] = JsonArray((properties.chapList ?: emptyList()).map { toJsonObject(it) })
    JsonObject(his).toString().writeTo(properties.savePath + "/properties.json", log = log)
}

private fun toJsonObject(chapter: Chapter): JsonObject = JsonObject(
    linkedMapOf(
        "id" to JsonPrimitive(chapter.id),
        "error" to JsonPrimitive(chapter.error),
        "part" to JsonPrimitive(chapter.partName),
        "chap" to JsonPrimitive(chapter.chapName),
        "url" to JsonPrimitive(chapter.url),
    ),
)

private fun toChapter(jsonObject: JsonObject): Chapter {
    val chapter = Chapter()
    chapter.id = jsonObject.str("id")
    chapter.error = jsonObject["error"]?.jsonPrimitive?.booleanOrNull ?: false
    chapter.partName = jsonObject.str("part")
    chapter.chapName = jsonObject.str("chap")?.nfc()
    chapter.url = jsonObject.str("url")
    // completed đánh theo file raw ở caller (loadHistory) — hardcode true khiến
    // chương chưa tải lọt vào export (chương rỗng) và marker "đã tải" sai
    return chapter
}

private fun JsonObject.str(name: String): String? = (this[name] as? JsonPrimitive)?.takeIf { it.contentOrNull != null }?.content
