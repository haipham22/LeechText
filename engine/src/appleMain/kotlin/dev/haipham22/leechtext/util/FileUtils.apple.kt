package dev.haipham22.leechtext.util

/**
 * actual apple — P5.2b ceiling: resource chưa bundle vào framework (trả "", caller
 * fallback default — SettingsRepository.defaults() đã có nhánh hardcoded); charset
 * chỉ UTF-8, tên khác → null như charset không hợp lệ. Nâng iconv/bundle ở P5.2c.
 */
// path: JVM actual đọc classpath — native chưa bundle resource (parity expect/actual)
@Suppress("UNUSED_PARAMETER")
actual fun readResource(path: String): String = ""

internal actual fun encodeBytes(
    text: String,
    charsetName: String,
): ByteArray? = if (charsetName.equals("UTF-8", ignoreCase = true)) text.encodeToByteArray() else null

internal actual fun decodeBytes(
    bytes: ByteArray,
    charsetName: String,
): String? = if (charsetName.equals("UTF-8", ignoreCase = true)) bytes.decodeToString() else null
