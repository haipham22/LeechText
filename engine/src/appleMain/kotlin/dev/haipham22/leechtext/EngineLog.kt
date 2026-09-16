package dev.haipham22.leechtext

import dev.haipham22.leechtext.log.platformEngineLogger

@Deprecated("Inject EngineLogger qua constructor thay vì static facade")
actual object EngineLog {
    private val impl = platformEngineLogger()

    actual var errorSink: ((Throwable) -> Unit)?
        get() = impl.errorSink
        set(value) {
            impl.errorSink = value
        }

    actual fun add(msg: String?) = impl.add(msg)

    actual fun debug(msg: String?) = impl.debug(msg)

    actual fun warn(msg: String?) = impl.warn(msg)

    actual fun add(e: Exception) = impl.add(e)
}
