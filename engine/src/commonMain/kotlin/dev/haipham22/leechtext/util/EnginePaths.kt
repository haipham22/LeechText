package dev.haipham22.leechtext.util

import okio.Path
import kotlin.concurrent.Volatile

/**
 * Vị trí dữ liệu của engine — thay thế AppUtils.curDir bản cũ (~/.leechtext).
 * Desktop: ~/.leechtext (mặc định). Android: app gọi [init] với filesDir sớm nhất.
 * P5.2b: okio.Path thay java.io.File (KMP).
 */
object EnginePaths {
    @Volatile
    private var override: Path? = null

    /** App Android gọi MainActivity.onCreate: EnginePaths.init(filesDir.toPath()) —
     *  root = filesDir/.leechtext. Reload EngineConfig để các settings đã cache dùng
     *  đúng path mới. */
    fun init(root: Path) {
        override = root / ".leechtext"
        // Force reload mọi config đã cache với path cũ
        dev.haipham22.leechtext.EngineConfig
            .reload()
    }

    val dataDir: Path
        get() = override ?: defaultDataDir()
}

/** Root mặc định theo platform — JVM/Android: user.home; Apple: HOME env. */
internal expect fun defaultDataDir(): Path
