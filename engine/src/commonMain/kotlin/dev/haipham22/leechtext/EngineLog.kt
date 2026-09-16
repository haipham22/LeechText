package dev.haipham22.leechtext

/**
 * [DI] Facade tạm cho giai đoạn migrate — code mới KHÔNG dùng; inject
 * dev.haipham22.leechtext.log.EngineLogger qua constructor (Koin). Xoá khi
 * mọi call site đã chuyển sang logger inject.
 */
@Deprecated("Inject EngineLogger qua constructor thay vì static facade")
expect object EngineLog {
    /** App đăng ký để nhận exception (Sentry.captureException). Gọi trước khi chạy engine. */
    var errorSink: ((Throwable) -> Unit)?

    fun add(msg: String?)

    /** Log debug — chỉ hiện khi setting "debug_log" bật. */
    fun debug(msg: String?)

    fun warn(msg: String?)

    fun add(e: Exception)
}
