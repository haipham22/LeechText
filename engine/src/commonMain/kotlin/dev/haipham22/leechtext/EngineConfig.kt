package dev.haipham22.leechtext

import dev.haipham22.leechtext.util.SettingsRepository
import kotlin.concurrent.Volatile

/**
 * Engine-wide config — đọc từ setting.json qua SettingsRepository, override được runtime.
 * Http dùng userAgent, timeout, reConn từ đây (wiring thật, không constant cứng).
 */
object EngineConfig {
    const val DEFAULT_TIMEOUT_MS: Int = 90_000
    const val DEFAULT_USER_AGENT: String =
        "Mozilla/5.0 (iPhone; CPU iPhone OS 26_0 like Mac OS X) AppleWebKit/605.1.15" +
            " (KHTML, like Gecko) Version/26.0 Mobile/15E148 Safari/604.1"

    /** Settings hiện tại — reload bằng [reload] sau khi save. */
    @Volatile
    var settings: dev.haipham22.leechtext.util.AppSettings = loadInitial()

    val USER_AGENT: String get() = settings.userAgent.ifEmpty { DEFAULT_USER_AGENT }
    val TIMEOUT_MS: Int get() = if (settings.timeout > 0) settings.timeout else DEFAULT_TIMEOUT_MS
    val RECONN: Int get() = settings.reConn.coerceIn(0, 10)

    /** Reload từ setting.json — gọi sau khi user save settings. */
    fun reload() {
        settings = SettingsRepository.load()
    }

    private fun loadInitial(): dev.haipham22.leechtext.util.AppSettings = try {
        SettingsRepository.load()
    } catch (e: Exception) {
        dev.haipham22.leechtext.util
            .AppSettings()
    }
}
