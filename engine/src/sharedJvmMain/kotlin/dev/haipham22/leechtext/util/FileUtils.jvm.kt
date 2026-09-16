package dev.haipham22.leechtext.util

import java.nio.charset.Charset

/** actual jvm + android — classpath resource qua classloader, charset đầy đủ qua JDK. */
actual fun readResource(path: String): String = FileUtilsJvm::class.java
    .getResourceAsStream(path)
    ?.use { it.readBytes() }
    ?.toString(Charsets.UTF_8) ?: ""

internal actual fun encodeBytes(
    text: String,
    charsetName: String,
): ByteArray? = try {
    text.toByteArray(Charset.forName(charsetName))
} catch (e: Exception) {
    null
}

internal actual fun decodeBytes(
    bytes: ByteArray,
    charsetName: String,
): String? = try {
    String(bytes, Charset.forName(charsetName))
} catch (e: Exception) {
    null
}

private object FileUtilsJvm
