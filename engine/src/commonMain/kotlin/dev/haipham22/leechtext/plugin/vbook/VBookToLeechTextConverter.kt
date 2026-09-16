package dev.haipham22.leechtext.plugin.vbook

import dev.haipham22.leechtext.entities.PluginEntity
import dev.haipham22.leechtext.plugin.vbook.model.VBookPluginEntity
import dev.haipham22.leechtext.util.nowMillis
import dev.haipham22.leechtext.util.sha256Hex

/**
 * Converter vBook plugin → LeechText PluginEntity (port từ vbook/VBookToLeechTextConverter.java).
 * Metadata mapping, script inlining, format transformation.
 */
class VBookToLeechTextConverter {
    /**
     * Convert vBook plugin → PluginEntity LeechText format.
     *
     * @throws IllegalArgumentException nếu thiếu required fields
     */
    fun convert(vbookPlugin: VBookPluginEntity?): PluginEntity {
        requireNotNull(vbookPlugin) { "vbookPlugin cannot be null" }

        validateRequiredFields(vbookPlugin)

        val entity =
            PluginEntity(
                name = vbookPlugin.name,
                author = vbookPlugin.author ?: "vBook",
                version = convertVersion(vbookPlugin.version),
                source = vbookPlugin.source,
                regex = vbookPlugin.regexp,
                describe = vbookPlugin.description,
                language = mapLocaleToLanguage(vbookPlugin.locale),
                group = mapTypeToGroup(vbookPlugin.type),
                icon = vbookPlugin.iconBase64,
                supportUpdate = true,
                checked = true,
                uuid = generateUuid(vbookPlugin.name, vbookPlugin.version),
            )

        // Inline script contents (immutable view — converter không đụng map gốc)
        val contents: Map<String, String> = vbookPlugin.scriptContents
        contents["chap"]?.let { entity.chapGetter = it }
        contents["toc"]?.let { entity.tocGetter = it }
        contents["detail"]?.let { entity.detailGetter = it }
        contents["page"]?.let { entity.pageGetter = it }
        contents["gen"]?.let { entity.genGetter = it }
        contents["search"]?.let { entity.searchGetter = it }
        contents["home"]?.let { entity.homeGetter = it }
        contents["genre"]?.let { entity.genreGetter = it }
        contents["tab"]?.let { entity.tabGetter = it }
        contents["gen"]?.let { entity.genGetter = it }

        entity.tag = vbookPlugin.tag
        entity.priority = vbookPlugin.priority ?: 0
        // Toàn bộ src/*.js → load() trong sandbox resolve in-memory (config.js, helper...)
        entity.extraScripts = vbookPlugin.scriptContents.entries.associate { (k, v) -> "$k.js" to v }

        // Config: key → value (sandbox inject) + spec cho UI cấu hình.
        // Built-in keys (vBook extension-api): thread_num, delay, timeout — luôn có defaults
        val configMap = mutableMapOf<String, String>()
        vbookPlugin.config.forEach { (key, spec) ->
            configMap[key] = spec.default ?: ""
        }
        // Built-in connection settings — mọi plugin đều có
        configMap.putIfAbsentCompat("thread_num", "3")
        configMap.putIfAbsentCompat("delay", "0")
        configMap.putIfAbsentCompat("timeout", "30000")
        entity.config = configMap

        // Spec cho UI: declared config + built-in
        val specMap =
            vbookPlugin.config
                .mapValues { (_, spec) ->
                    dev.haipham22.leechtext.entities.PluginConfigSpec(
                        title = spec.title,
                        subtitle = spec.subtitle,
                        default = spec.default,
                        values = spec.values,
                        mode = spec.mode,
                        format = spec.format,
                    )
                }.toMutableMap()
        specMap.putIfAbsentCompat(
            "thread_num",
            dev.haipham22.leechtext.entities.PluginConfigSpec(
                title = "Số luồng tải",
                subtitle = "Max concurrent requests",
                default = "3",
                mode = "input",
                format = "number",
            ),
        )
        specMap.putIfAbsentCompat(
            "delay",
            dev.haipham22.leechtext.entities.PluginConfigSpec(
                title = "Delay giữa request (ms)",
                subtitle = "Delay between requests",
                default = "0",
                mode = "input",
                format = "number",
            ),
        )
        specMap.putIfAbsentCompat(
            "timeout",
            dev.haipham22.leechtext.entities.PluginConfigSpec(
                title = "Timeout request (ms)",
                subtitle = "Request timeout",
                default = "30000",
                mode = "input",
                format = "number",
            ),
        )
        entity.configSpec = specMap

        return entity
    }

    /** Validate required fields. */
    private fun validateRequiredFields(plugin: VBookPluginEntity) {
        require(!plugin.name.isNullOrEmpty()) { "Plugin name is required" }
        require(!plugin.regexp.isNullOrEmpty()) { "Plugin regexp is required" }
        require(plugin.scriptContents.isNotEmpty()) { "Plugin must have at least one script" }
    }

    /**
     * Version integer vBook → double LeechText (10 → 1.0). Formula: int_version / 10.0.
     */
    private fun convertVersion(vbookVersion: Int?): Double = vbookVersion?.div(10.0) ?: 1.0

    /** Locale vBook ("vi_VN") → language code LeechText ("vi"). */
    private fun mapLocaleToLanguage(locale: String?): String {
        if (locale.isNullOrEmpty()) return "en"

        // Extract language code (vi_VN → vi)
        val langCode = locale.split("_")[0].lowercase()

        return when (langCode) {
            "vi" -> "vi"
            "en" -> "en"
            "zh" -> "cn"
            else -> langCode
        }
    }

    /** Type vBook ("novel"/"comic") → group LeechText. */
    private fun mapTypeToGroup(type: String?): String {
        if (type.isNullOrEmpty()) return "dich" // default group

        return when (type.lowercase()) {
            "novel" -> "dich"
            "comic" -> "truyentranh"
            else -> "dich"
        }
    }

    /**
     * UUID nhất quán từ name + version (SHA-256 hash, 16 bytes đầu → format UUID).
     */
    private fun generateUuid(
        name: String?,
        version: Int?,
    ): String = try {
        val input = "$name-${version ?: 1}"
        val hex = sha256Hex(input.encodeToByteArray()).take(32)
        // Convert 16 bytes đầu (32 hex chars) sang UUID format
        val sb = StringBuilder()
        for (i in 0 until 16) {
            if (i in listOf(4, 6, 8, 10)) sb.append("-")
            sb.append(hex.substring(i * 2, i * 2 + 2))
        }
        sb.toString()
    } catch (e: Exception) {
        // Fallback hash-based UUID
        val ts = nowMillis() and 0xffffffffffffL
        val h = (name?.hashCode() ?: 0).toUInt().toString(16).padStart(8, '0')
        val v = (version ?: 1).toString(16).padStart(4, '0')
        "$h-$v-0000-0000-${ts.toString(16).padStart(12, '0')}"
    }
}

/** putIfAbsent common (MutableMap JVM-only). */
private fun <K, V> MutableMap<K, V>.putIfAbsentCompat(
    key: K,
    value: V,
): V? {
    val existing = this[key]
    if (existing == null) this[key] = value
    return existing
}
