package dev.haipham22.leechtext.plugin.js.loader

import dev.haipham22.leechtext.log.EngineLogger

/**
 * Console API cho plugin scripts (port từ ConsoleApi.java — match vBooks JSLog).
 */
class ConsoleApi(
    private val log: EngineLogger,
) {
    fun log(msg: Any?) {
        if (msg != null) {
            JSResponse.getString(msg)?.let { log.add(it) }
        }
    }
}
