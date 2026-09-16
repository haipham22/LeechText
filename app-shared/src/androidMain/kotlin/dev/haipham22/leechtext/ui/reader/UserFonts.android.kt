package dev.haipham22.leechtext.ui.reader

import androidx.compose.ui.text.font.FontFamily
import java.io.File

/** Android: không có file manager chung cho folder app-private — no-op, user copy qua MTP/SAF. */
// ponytail: mở DocumentsUI SAF picker khi cần import in-app — thêm Intent ACTION_OPEN_DOCUMENT.
actual fun openFontsFolder() = Unit

actual fun userFontFamily(fileName: String): FontFamily? = try {
    val file = File(UserFonts.dir.toString(), fileName)
    if (file.exists()) FontFamily(android.graphics.Typeface.createFromFile(file)) else null
} catch (_: Throwable) {
    null
}
