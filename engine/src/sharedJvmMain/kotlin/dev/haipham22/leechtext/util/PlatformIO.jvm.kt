package dev.haipham22.leechtext.util

import dev.haipham22.leechtext.EngineLog
import dev.haipham22.leechtext.log.EngineLogger

/** JVM actual — classpath resource. */
actual fun readResourceBytes(
    path: String,
    log: EngineLogger,
): ByteArray = try {
    EngineLog::class.java.getResourceAsStream(path)?.use { it.readBytes() } ?: ByteArray(0)
} catch (e: Exception) {
    log.add("[readResourceBytes] $path — ${e.message}")
    ByteArray(0)
}

/** JVM actual — ProcessBuilder (calibre ebook-convert), đọc % progress qua stdout. */
@Suppress("LongParameterList") // tham số log DI + params nghiệp vụ — bundle làm code khó đọc
actual fun runExternalTool(
    toolPath: String,
    args: List<String>,
    log: EngineLogger,
    onLine: (String) -> Unit,
): Boolean = try {
    // ProcessBuilder thay Runtime.exec(String) deprecated — tách token bằng whitespace
    // giống StringTokenizer của exec(String), giữ nguyên behavior legacy
    val cmd = (listOf(toolPath) + args)
        .flatMap { it.split(Regex("\\s+")).filter { token -> token.isNotEmpty() } }
    val p =
        @Suppress("SpreadOperator")
        ProcessBuilder(*cmd.toTypedArray())
            .redirectErrorStream(false)
            .start()
    pumpProcessOutput(p, onLine)
    true
} catch (e: Exception) {
    log.add(e)
    false
}

/** Đọc stdout của process đến hết, bơm từng dòng vào onLine (đã thay newline bằng space). */
private fun pumpProcessOutput(
    p: Process,
    onLine: (String) -> Unit,
) {
    java.io.BufferedReader(java.io.InputStreamReader(p.inputStream, Charsets.UTF_8)).use { reader ->
        while (true) {
            val line = reader.readLine() ?: break
            regexFind(line, "(^\\d+)%", 1)?.let { /* % progress parse bởi caller */ }
            onLine(line.replace("[\n\r]".toRegex(), " "))
        }
    }
}

actual fun osName(): String = System.getProperty("os.name").lowercase()

actual fun canExecute(path: String): Boolean = java.io.File(path).canExecute()
