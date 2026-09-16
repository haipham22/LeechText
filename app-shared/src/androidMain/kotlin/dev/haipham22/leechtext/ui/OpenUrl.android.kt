package dev.haipham22.leechtext.ui

import android.content.Intent
import android.net.Uri

/** Android: Intent ACTION_VIEW — browser/app xử lý URL mở ra. */
actual fun openUrl(url: String) {
    val ctx = AppContext.instance ?: return
    runCatching {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
