package dev.haipham22.leechtext.ui

/**
 * Actual JVM/Android cho picker — reflection (giữ nguyên từ sharedMain cũ: Android
 * không có java.awt → Class.forName throw → onResult(null)).
 * pickSaveFile: FileDialog (native GTK/macOS/Windows — dogfood 260903 user chê
 * JFileChooser Swing style); pickDirectory/pickImageFile giữ JFileChooser —
 * FileDialog không có mode chọn thư mục cross-platform.
 */

internal actual fun pickSaveFile(
    title: String,
    defaultName: String,
    onResult: (String?) -> Unit,
) {
    try {
        val fdClass = Class.forName("java.awt.FileDialog")
        val frameClass = Class.forName("java.awt.Frame")
        val fd =
            fdClass.getConstructor(frameClass, String::class.java, Int::class.javaPrimitiveType)
                .newInstance(null as Any?, title, 1) // LOAD=0 / SAVE=1
        fdClass.getMethod("setFile", String::class.java).invoke(fd, defaultName)
        fdClass.getMethod("setVisible", Boolean::class.javaPrimitiveType).invoke(fd, true)
        val file = fdClass.getMethod("getFile").invoke(fd) as? String
        val dir = fdClass.getMethod("getDirectory").invoke(fd) as? String
        if (file != null && dir != null) {
            onResult(dir + file)
        } else {
            onResult(null)
        }
    } catch (e: Exception) {
        onResult(null)
    }
}

internal actual fun pickDirectory(
    title: String,
    onResult: (String?) -> Unit,
) {
    try {
        val jfcClass = Class.forName("javax.swing.JFileChooser")
        val jfc = jfcClass.getDeclaredConstructor().newInstance()
        jfcClass.getMethod("setFileSelectionMode", Int::class.javaPrimitiveType).invoke(jfc, 1) // DIRECTORIES_ONLY
        jfcClass.getMethod("setDialogTitle", String::class.java).invoke(jfc, title)
        val result =
            jfcClass
                .getMethod("showOpenDialog", Class.forName("java.awt.Component"))
                .invoke(jfc, null as Any?)
        if (result == 0) {
            val file = jfcClass.getMethod("getSelectedFile").invoke(jfc)
            val path = file?.let { it::class.java.getMethod("getAbsolutePath").invoke(it) as? String }
            onResult(path)
        }
    } catch (e: Exception) {
        // Android / headless — fallback: user gõ tay path
        onResult(null)
    }
}

internal actual fun pickImageFile(
    title: String,
    onResult: (String?) -> Unit,
) {
    try {
        // FileDialog native (như pickSaveFile — dogfood 260905 user chê JFileChooser
        // Swing dị + không duyệt được ~/Downloads: JFileChooser không trigger TCC
        // prompt của macOS, FileDialog native có)
        val fdClass = Class.forName("java.awt.FileDialog")
        val frameClass = Class.forName("java.awt.Frame")
        val fd =
            fdClass.getConstructor(frameClass, String::class.java, Int::class.javaPrimitiveType)
                .newInstance(null as Any?, title, 0) // LOAD=0
        val imageExts = setOf("jpg", "jpeg", "png", "webp", "gif")
        fdClass.getMethod("setFilenameFilter", Class.forName("java.io.FilenameFilter")).invoke(
            fd,
            java.io.FilenameFilter { dir, name ->
                java.io.File(dir, name).isDirectory || name.substringAfterLast('.', "").lowercase() in imageExts
            },
        )
        fdClass.getMethod("setVisible", Boolean::class.javaPrimitiveType).invoke(fd, true)
        val file = fdClass.getMethod("getFile").invoke(fd) as? String
        val dir = fdClass.getMethod("getDirectory").invoke(fd) as? String
        if (file != null && dir != null) {
            onResult(dir + file)
        } else {
            onResult(null)
        }
    } catch (e: Exception) {
        onResult(null)
    }
}
