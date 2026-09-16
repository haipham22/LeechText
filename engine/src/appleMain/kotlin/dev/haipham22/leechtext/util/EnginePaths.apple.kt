package dev.haipham22.leechtext.util

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import okio.Path
import okio.Path.Companion.toPath
import platform.posix.getenv
import kotlin.native.Platform

/**
 * Actual apple — HOME env. macOS: ~/.leechtext (test host giữ nguyên);
 * iOS: HOME = container app → Documents/.leechtext (user thấy qua Files app).
 */
@OptIn(ExperimentalForeignApi::class, kotlin.experimental.ExperimentalNativeApi::class)
internal actual fun defaultDataDir(): Path {
    val home = getenv("HOME")?.toKString() ?: "/tmp"
    val root = if (Platform.osFamily == kotlin.native.OsFamily.IOS) "$home/Documents" else home
    return root.toPath() / ".leechtext"
}
