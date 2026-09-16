package dev.haipham22.leechtext.util

/** JVM actual — java monitor (synchronized). */
internal actual fun <T> monitorLock(
    lock: Any,
    block: () -> T,
): T = synchronized(lock, block)
