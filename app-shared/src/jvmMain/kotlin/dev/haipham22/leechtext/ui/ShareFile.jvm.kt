package dev.haipham22.leechtext.ui

import java.io.File

/** Desktop: mở Finder/Explorer tại thư mục chứa file xuất. */
actual fun shareFile(path: String) {
    runCatching {
        val file = File(path)
        java.awt.Desktop
            .getDesktop()
            .open(if (file.isFile) file.parentFile else file)
    }
}
