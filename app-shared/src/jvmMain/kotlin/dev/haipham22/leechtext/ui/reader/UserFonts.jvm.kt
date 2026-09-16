package dev.haipham22.leechtext.ui.reader

import androidx.compose.ui.text.font.FontFamily
import java.io.File

/** Desktop: mở Finder/Explorer; font load từ file qua platform Font. */
actual fun openFontsFolder() {
    UserFonts.list() // ensure dir exists
    java.awt.Desktop.getDesktop().open(File(UserFonts.dir.toString()))
}

actual fun userFontFamily(fileName: String): FontFamily? = try {
    val file = File(UserFonts.dir.toString(), fileName)
    if (file.exists()) {
        FontFamily(
            androidx.compose.ui.text.platform.Font(
                identity = fileName,
                data = file.readBytes(),
            ),
        )
    } else {
        null
    }
} catch (_: Throwable) {
    null
}
