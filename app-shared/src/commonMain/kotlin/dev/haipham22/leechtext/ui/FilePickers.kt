package dev.haipham22.leechtext.ui

/**
 * File/directory picker native (P5.3 expect/actual — Kotlin/Native không có
 * javax.swing reflection).
 * Desktop (jvm): JFileChooser; Android: null (engine export tự ghi vào app
 * storage, cover dùng photo picker riêng); iOS: null — fallback user gõ tay path.
 */
internal expect fun pickSaveFile(
    title: String,
    defaultName: String,
    onResult: (String?) -> Unit,
)

internal expect fun pickDirectory(
    title: String,
    onResult: (String?) -> Unit,
)

internal expect fun pickImageFile(
    title: String,
    onResult: (String?) -> Unit,
)
