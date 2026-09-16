package dev.haipham22.leechtext.ui

import androidx.compose.ui.graphics.ImageBitmap

/** Decode ảnh từ bytes — mỗi platform một cách (desktop Skia, Android BitmapFactory). */
expect fun decodeImageBitmap(bytes: ByteArray): ImageBitmap?
