package dev.haipham22.leechtext.ui

import android.content.Context

/** Context app — MainActivity gán trong onCreate (app-shared không tự tạo được). */
object AppContext {
    @JvmStatic
    var instance: Context? = null
}
