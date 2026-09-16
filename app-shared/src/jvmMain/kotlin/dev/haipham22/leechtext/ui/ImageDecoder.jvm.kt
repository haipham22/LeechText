package dev.haipham22.leechtext.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image

/** Desktop: decode qua Skia (skiko bundled với Compose Desktop). */
actual fun decodeImageBitmap(bytes: ByteArray): ImageBitmap? = runCatching { Image.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()
