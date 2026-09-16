package dev.haipham22.leechtext.util

import dev.haipham22.leechtext.log.EngineLogger

/** Đọc classpath/bundled resource binary; rỗng nếu thiếu. JVM: classpath; apple: rỗng
 * (bundle resource ở P5.4 nếu cần — EPUB default dùng template riêng). */
expect fun readResourceBytes(
    path: String,
    log: EngineLogger,
): ByteArray

/** Kiểm tra + chạy external tool (calibre ebook-convert) — desktop JVM; apple no-op false. */
@Suppress("LongParameterList") // tham số log DI + params nghiệp vụ — bundle làm code khó đọc
expect fun runExternalTool(
    toolPath: String,
    args: List<String>,
    log: EngineLogger,
    onLine: (String) -> Unit,
): Boolean

/** Tên OS lowercase (win/mac/other) cho check .exe. */
expect fun osName(): String

/** File có executable bit không (unix check calibre tool). */
expect fun canExecute(path: String): Boolean
