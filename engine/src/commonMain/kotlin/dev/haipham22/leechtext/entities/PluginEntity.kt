package dev.haipham22.leechtext.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Plugin manifest + getter scripts (port từ enities/PluginEntity.java). */
@Serializable
data class PluginEntity(
    @SerialName("uuid") var uuid: String? = null,
    @SerialName("name") var name: String? = null,
    @SerialName("version") var version: Double? = null,
    @SerialName("url") var url: String? = null,
    @SerialName("language") var language: String? = null,
    @SerialName("icon") var icon: String? = null,
    @SerialName("source") var source: String? = null,
    @SerialName("regex") var regex: String? = null,
    @SerialName("author") var author: String? = null,
    /** Tag vd "nsfw" — badge cảnh báo trong UI nguồn. */
    @SerialName("tag") var tag: String? = null,
    /** Ưu tiên khi nhiều plugin khớp 1 URL — cao thắng (vBook priority). */
    @SerialName("priority") var priority: Int = 0,
    @SerialName("describe") var describe: String? = null,
    @SerialName("group") var group: String? = null,
    @SerialName("data") var data: String? = null,
    var supportUpdate: Boolean = false,
    @SerialName("chap") var chapGetter: String? = null,
    @SerialName("toc") var tocGetter: String? = null,
    @SerialName("page") var pageGetter: String? = null,
    @SerialName("gen") var genGetter: String? = null,
    @SerialName("search") var searchGetter: String? = null,
    @SerialName("home") var homeGetter: String? = null,
    @SerialName("genre") var genreGetter: String? = null,
    @SerialName("tab") var tabGetter: String? = null,
    @SerialName("detail") var detailGetter: String? = null,
    // Toàn bộ src/*.js của plugin zip (filename -> content) — cho sandbox load() in-memory
    @SerialName("extra_scripts") var extraScripts: Map<String, String>? = null,
    @SerialName("config") var config: Map<String, String>? = null,
    /** Spec config cho UI: key → (title, subtitle, default, values, mode, format). */
    @SerialName("config_spec") var configSpec: Map<String, PluginConfigSpec>? = null,
    var checked: Boolean = false,
) {
    /** Copy toàn bộ field từ entity khác, đánh dấu checked (update metadata từ remote). */
    @OptIn(ExperimentalUuidApi::class)
    fun apply(entity: PluginEntity) {
        uuid = entity.uuid ?: Uuid.random().toString()
        name = entity.name
        version = entity.version
        url = entity.url
        language = entity.language
        icon = entity.icon
        source = entity.source
        regex = entity.regex
        author = entity.author
        describe = entity.describe
        group = entity.group
        data = entity.data
        supportUpdate = entity.supportUpdate
        chapGetter = entity.chapGetter
        tocGetter = entity.tocGetter
        pageGetter = entity.pageGetter
        genGetter = entity.genGetter
        searchGetter = entity.searchGetter
        detailGetter = entity.detailGetter
        checked = true
    }
}

/** Spec 1 entry config plugin (vBook extension-api) — dùng render UI cấu hình. */
@Serializable
data class PluginConfigSpec(
    var title: String? = null,
    var subtitle: String? = null,
    var default: String? = null,
    var values: List<String> = emptyList(),
    var mode: String? = null,
    var format: String? = null,
)
