package dev.haipham22.leechtext.ui

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/** Android: decode qua BitmapFactory. */
actual fun decodeImageBitmap(bytes: ByteArray): ImageBitmap? = runCatching {
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
}.getOrNull()
