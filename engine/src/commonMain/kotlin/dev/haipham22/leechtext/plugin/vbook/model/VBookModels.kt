package dev.haipham22.leechtext.plugin.vbook.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Extension entry trong registry plugin.json (port từ model/VBookExtensionEntity.java) —
 * match item trong data array của vBook.
 */
@Serializable
data class VBookExtensionEntity(
    @SerialName("name") var name: String? = null,
    @SerialName("author") var author: String? = null,
    /** URL download plugin.zip. */
    @SerialName("path") var path: String? = null,
    /** Version (integer format). */
    @SerialName("version") var version: Int? = null,
    /** Base URL site nguồn. */
    @SerialName("source") var source: String? = null,
    @SerialName("icon") var icon: String? = null,
    @SerialName("description") var description: String? = null,
    /** "novel" hoặc "comic". */
    @SerialName("type") var type: String? = null,
    /** Locale code vd "vi_VN". */
    @SerialName("locale") var locale: String? = null,
    /** Tag tùy chọn vd "nsfw". */
    @SerialName("tag") var tag: String? = null,
)

/**
 * Plugin.zip vBook đã extract (port từ model/VBookPluginEntity.java) — metadata từ plugin.json
 * + script contents.
 */
data class VBookPluginEntity(
    // Metadata từ plugin.json
    @SerialName("name") var name: String? = null,
    @SerialName("author") var author: String? = null,
    @SerialName("version") var version: Int? = null,
    @SerialName("source") var source: String? = null,
    @SerialName("regexp") var regexp: String? = null,
    @SerialName("description") var description: String? = null,
    @SerialName("locale") var locale: String? = null,
    @SerialName("type") var type: String? = null,
    @SerialName("language") var language: String? = null,
    @SerialName("priority") var priority: Int? = null,
    @SerialName("tag") var tag: String? = null,
    /** Script type → filename (vd "chap" -> "chap.js"). */
    var scripts: MutableMap<String, String> = HashMap(),
    /** Script type → JavaScript content đã extract. */
    var scriptContents: MutableMap<String, String> = HashMap(),
    /** Icon dạng base64 data URI. */
    var iconBase64: String? = null,
    /** Config từ plugin.json: key → (default, mode, format, values). */
    var config: MutableMap<String, VBookConfigSpec> = HashMap(),
    /** Raw plugin.json content. */
    var rawMetadata: String? = null,
)

/** Spec 1 entry config trong plugin.json (vBook extension-api: DOMAIN, thread_num...). */
data class VBookConfigSpec(
    var title: String? = null,
    var subtitle: String? = null,
    var default: String? = null,
    var values: List<String> = emptyList(),
    var mode: String? = null,
    var format: String? = null,
)

/**
 * Nguồn repo vBook trong repository.json (port từ model/VBookRepositoryEntity.java).
 */
@Serializable
data class VBookRepositoryEntity(
    var link: String? = null,
    var author: String? = null,
    var description: String? = null,
    var enabled: Boolean = true,
)
