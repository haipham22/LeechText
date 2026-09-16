package dev.haipham22.leechtext.util

import dev.haipham22.leechtext.log.EngineLogger

/** Apple: chưa có decoder — fallback ext sniff (cover.webp vẫn vào EPUB đúng mime). */
actual fun imageBytesToJpeg(
    bytes: ByteArray,
    log: EngineLogger,
): ByteArray? = null
