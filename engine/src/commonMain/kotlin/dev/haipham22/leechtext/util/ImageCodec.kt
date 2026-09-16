package dev.haipham22.leechtext.util

import dev.haipham22.leechtext.log.EngineLogger

/**
 * Decode ảnh (WebP/PNG/GIF/…) → JPEG bytes cho EPUB cover (bìa Cầu Ma là WebP
 * mà reader không render — 260907). Không convert được → null, caller fallback
 * ghi bytes gốc theo ext sniff.
 */
expect fun imageBytesToJpeg(
    bytes: ByteArray,
    log: EngineLogger,
): ByteArray?
