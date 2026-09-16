package dev.haipham22.leechtext.log

import dev.haipham22.leechtext.EngineConfig

/**
 * Logger engine Apple — println ra console Xcode. Backend thật (os_log/kermit/kslog)
 * ở P5.2c; engine apple hiện chỉ là compile gate, chưa chạy runtime.
 */
class AppleEngineLogger : EngineLogger {
    override var errorSink: ((Throwable) -> Unit)? = null

    override fun add(msg: String?) {
        if (msg != null) println("[Engine] $msg")
    }

    override fun debug(msg: String?) {
        if (msg != null && EngineConfig.settings.debugLog) println("[Engine][D] $msg")
    }

    override fun warn(msg: String?) {
        if (msg != null) println("[Engine][WARN] $msg")
    }

    override fun add(e: Exception) {
        println("[Engine][ERROR] engine error")
        e.printStackTrace()
        errorSink?.invoke(e)
    }
}

/** Singleton — facade EngineLog (deprecated) và Koin phải chia sẻ cùng instance,
 * để errorSink (Sentry) đăng ký một chỗ có tác dụng với mọi logger được inject. */
private val loggerInstance: EngineLogger = AppleEngineLogger()

actual fun platformEngineLogger(): EngineLogger = loggerInstance
