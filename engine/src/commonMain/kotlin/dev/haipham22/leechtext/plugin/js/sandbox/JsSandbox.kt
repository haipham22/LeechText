package dev.haipham22.leechtext.plugin.js.sandbox

import dev.haipham22.leechtext.log.EngineLogger
import dev.haipham22.leechtext.plugin.js.loader.LoaderType

/** Cấu hình tạo sandbox (thay Builder — expect class lồng nhau khóportable). */
class SandboxConfig(
    var loaderType: LoaderType? = null,
    var baseUrl: String? = null,
    var targetUrl: String? = null,
    var pluginSource: String? = null,
    var pluginConfig: Map<String, String>? = null,
    var extraScripts: Map<String, String>? = null,
)

/**
 * Sandbox bảo mật cho vBook plugin JS (P5.2c expect/actual — contract giữ nguyên bản Rhino).
 *
 * Tạo qua [createSandbox] (expect fun). callFunction trả về giá trị ĐÃ NORMALIZE
 * (Map/List/primitive/null/Kotlin object như JSList) — loaders common không thấy type
 * của engine.
 *
 * - JVM actual: Rhino (Context enter/exit theo lifecycle, optimizationLevel -1).
 * - Apple actual: QuickJS (quickjs-kt, runtime fresh mỗi sandbox, limit 30s/50MB).
 */
expect class JsSandbox internal constructor(
    log: EngineLogger,
    config: SandboxConfig,
) {
    /**
     * Execute script trong sandbox.
     *
     * @param script source JS
     * @param scriptName tên cho log/error
     * @return true nếu không có lỗi
     */
    fun execute(
        script: String,
        scriptName: String,
    ): Boolean

    /**
     * Gọi function theo tên trong sandbox với args.
     *
     * @return kết quả normalized; null nếu không tìm thấy/lỗi
     */
    fun callFunction(
        functionName: String,
        vararg args: Any?,
    ): Any?

    /** Đóng sandbox, giải phóng tài nguyên engine. */
    fun close()
}

/** Factory sandbox per-engine. */
expect fun createSandbox(
    log: EngineLogger,
    config: SandboxConfig.() -> Unit = {},
): JsSandbox
