package dev.haipham22.leechtext.util

import dev.haipham22.leechtext.log.EngineLogger
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/** Desktop: ImageIO + TwelveMonkeys webp plugin (đăng ký SPI tự động khi vào classpath). */
actual fun imageBytesToJpeg(
    bytes: ByteArray,
    log: EngineLogger,
): ByteArray? = try {
    val img = ImageIO.read(ByteArrayInputStream(bytes)) ?: return null
    val out = ByteArrayOutputStream()
    ImageIO.write(img, "jpg", out)
    out.toByteArray()
} catch (e: Exception) {
    log.add("[imageCodec] convert fail: ${e.message}")
    null
}
