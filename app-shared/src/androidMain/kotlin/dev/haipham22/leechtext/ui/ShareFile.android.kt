package dev.haipham22.leechtext.ui

import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** Android: share sheet qua FileProvider — gửi tới app đọc sách/Drive/Zalo... */
actual fun shareFile(path: String) {
    val ctx = AppContext.instance ?: return
    val file = File(path)
    if (!file.isFile) return
    val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
    val intent =
        Intent(Intent.ACTION_SEND).apply {
            type =
                when (file.extension.lowercase()) {
                    "epub" -> "application/epub+zip"
                    else -> "text/plain"
                }
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    ctx.startActivity(
        Intent.createChooser(intent, "Chia sẻ").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}
