package dev.haipham22.leechtext.plugin.api

/**
 * Metadata nhẹ cho search result/listing (port từ api/PluginMetadata.java) — thông tin hiển
 * thị, không chứa full plugin data. VBookPluginMetadata mở rộng với field vBook-specific.
 */
open class PluginMetadata(
    val id: String? = null,
    val name: String? = null,
    val author: String? = null,
    val version: String? = null,
    val description: String? = null,
    val source: String? = null,
    val iconUrl: String? = null,
    val popularity: Int = 0,
    val tags: Array<String> = emptyArray(),
    val lastUpdated: Long = 0,
    val sizeBytes: Long = 0,
) {
    override fun toString(): String = "PluginMetadata{id='$id', name='$name', version='$version', source='$source'}"
}
