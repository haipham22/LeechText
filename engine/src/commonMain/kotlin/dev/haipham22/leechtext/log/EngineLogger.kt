package dev.haipham22.leechtext.log

/**
 * Logger engine — inject qua constructor (DI, Koin) thay vì static object.
 * Backend: JVM/Android = kotlin-logging (SLF4J → logcat/stdout); Apple = println.
 *
 * errorSink: hook cho app bám vào (vd Sentry) — engine không phụ thuộc SDK báo lỗi.
 */
interface EngineLogger {
    /** App đăng ký để nhận exception (Sentry.captureException). Gọi trước khi chạy engine. */
    var errorSink: ((Throwable) -> Unit)?

    fun add(msg: String?)

    /** Log debug — chỉ hiện khi setting "debug_log" bật. */
    fun debug(msg: String?)

    fun warn(msg: String?)

    fun add(e: Exception)
}

/** Logger backend theo platform (JVM: SLF4J, Apple: println) — đăng ký một lần trong Koin. */
expect fun platformEngineLogger(): EngineLogger
