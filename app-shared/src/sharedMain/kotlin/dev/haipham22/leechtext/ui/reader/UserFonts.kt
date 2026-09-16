package dev.haipham22.leechtext.ui.reader

import androidx.compose.ui.text.font.FontFamily
import dev.haipham22.leechtext.util.EnginePaths
import okio.Path

/**
 * Font user tự drop vào ~/.leechtext/fonts/ (hoặc filesDir/fonts trên Android).
 * Key reader: "file:<tên-file>" — resolver map tại readerFontFamily.
 */
object UserFonts {
    val dir: Path get() = EnginePaths.dataDir / "fonts"

    /** Danh sách file .ttf/.otf — tạo thư mục nếu chưa có. */
    fun list(): List<String> {
        val fs = okio.FileSystem.SYSTEM
        if (!fs.exists(dir)) fs.createDirectories(dir)
        return fs.list(dir).filter { it.name.endsWith(".ttf", true) || it.name.endsWith(".otf", true) }
            .map { it.name }
            .sorted()
    }
}

/** Mở thư mục font bằng file manager OS. */
expect fun openFontsFolder()

/** Load font từ file user — null nếu platform chưa hỗ trợ/file hỏng. */
expect fun userFontFamily(fileName: String): FontFamily?
