package dev.haipham22.leechtext.log

import dev.haipham22.leechtext.EngineConfig
import mu.KotlinLogging

/**
 * Logger JVM/Android — kotlin-logging (SLF4J). Backend slf4j-simple in ra stdout
 * (desktop) / logcat (Android). Đổi file log: cấu hình simplelogger.properties.
 */
class JvmEngineLogger : EngineLogger {
    private val logger = KotlinLogging.logger("Engine")

    override var errorSink: ((Throwable) -> Unit)? = null

    override fun add(msg: String?) {
        if (msg != null) logger.info { msg }
    }

    /** Log debug — chỉ hiện khi setting "debug_log" bật (level INFO kèm prefix [D]
     * vì slf4j-simple mặc định lọc DEBUG). */
    override fun debug(msg: String?) {
        if (msg != null && EngineConfig.settings.debugLog) logger.info { "[D] $msg" }
    }

    override fun warn(msg: String?) {
        if (msg != null) logger.warn { msg }
    }

    override fun add(e: Exception) {
        logger.error(e) { "engine error" }
        errorSink?.invoke(e)
    }
}

/** Singleton — facade EngineLog (deprecated) và Koin phải chia sẻ cùng instance,
 * để errorSink (Sentry) đăng ký một chỗ có tác dụng với mọi logger được inject. */
private val loggerInstance: EngineLogger = JvmEngineLogger()

actual fun platformEngineLogger(): EngineLogger = loggerInstance
