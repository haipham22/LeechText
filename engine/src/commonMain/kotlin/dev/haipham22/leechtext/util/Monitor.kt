package dev.haipham22.leechtext.util

/**
 * Monitor lock portable (P5.2c): JVM = synchronized intrinsic; Apple = pthread mutex
 * global (không có java monitor). ponytail: global lock đơn giản — tách per-lock nếu
 * thấy contention.
 */
internal expect fun <T> monitorLock(
    lock: Any,
    block: () -> T,
): T
