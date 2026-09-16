package dev.haipham22.leechtext.util

import okio.Path
import okio.Path.Companion.toPath

/** actual jvm + android — giữ nguyên semantics bản cũ (user.home, fallback "/" + warn). */
internal actual fun defaultDataDir(): Path {
    val home = System.getProperty("user.home")
    val dir = (home ?: "/").toPath() / ".leechtext"
    // user.home null (Android không init) → fallback / cho dev như bản cũ
    if (home == null) {
        EngineLogOnce.warn("EnginePaths: user.home null — Android chưa gọi init(filesDir), dùng $dir")
    }
    return dir
}

/** Log một lần — tránh spam. */
private object EngineLogOnce {
    private var warned = false

    fun warn(msg: String) {
        if (!warned) {
            dev.haipham22.leechtext.EngineLog
                .add(msg)
            warned = true
        }
    }
}
