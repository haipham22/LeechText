package dev.haipham22.leechtext.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import dev.haipham22.leechtext.log.EngineLogger
import java.io.ByteArrayOutputStream

/** Android: BitmapFactory decode webp native (minSdk 26 đủ lossy+lossless), encode JPEG. */
actual fun imageBytesToJpeg(
    bytes: ByteArray,
    log: EngineLogger,
): ByteArray? = try {
    val img = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
    val out = ByteArrayOutputStream()
    img.compress(Bitmap.CompressFormat.JPEG, 90, out)
    out.toByteArray()
} catch (e: Exception) {
    null
}
