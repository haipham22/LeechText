package dev.haipham22.leechtext.get

import dev.haipham22.leechtext.entities.PluginEntity
import dev.haipham22.leechtext.util.AppSettings

/**
 * Resolve setting theo priority: plugin config (user set) → plugin default (spec) → global.
 * Dùng cho thread_num, delay, timeout — built-in vBook extension-api.
 */
object PluginSettings {
    /** String value: plugin.config[key] → plugin.configSpec[key].default → globalDefault */
    fun resolve(
        plugin: PluginEntity?,
        key: String,
        globalDefault: String,
    ): String {
        val fromConfig = plugin?.config?.get(key)
        if (!fromConfig.isNullOrBlank()) return fromConfig
        val fromSpec = plugin?.configSpec?.get(key)?.default
        if (!fromSpec.isNullOrBlank()) return fromSpec
        return globalDefault
    }

    /** Int value — coerce vào range nếu cho. */
    fun resolveInt(
        plugin: PluginEntity?,
        key: String,
        globalDefault: Int,
        min: Int = 1,
        max: Int = Int.MAX_VALUE,
    ): Int {
        val raw = resolve(plugin, key, globalDefault.toString())
        return raw.toIntOrNull()?.coerceIn(min, max) ?: globalDefault
    }

    /** maxConn / thread_num — dùng trong downloadChapters. */
    fun maxConn(
        plugin: PluginEntity?,
        settings: AppSettings,
    ): Int = resolveInt(plugin, "thread_num", settings.maxConn, min = 1, max = 20)

    /** Delay ms — dùng trong downloadChapters. */
    fun delayMs(
        plugin: PluginEntity?,
        settings: AppSettings,
    ): Int = resolveInt(plugin, "delay", settings.delay, min = 0, max = 60_000)

    /** Timeout ms — dùng trong Http. */
    fun timeoutMs(
        plugin: PluginEntity?,
        settings: AppSettings,
    ): Int = resolveInt(plugin, "timeout", settings.timeout, min = 1_000, max = 300_000)
}
