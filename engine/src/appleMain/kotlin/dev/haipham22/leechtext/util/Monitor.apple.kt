package dev.haipham22.leechtext.util

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Apple actual — mirror Java monitor: MỖI lock object 1 Mutex riêng. Bản cũ dùng 1 Mutex
 * toàn cục cho mọi monitorLock — lock lồng nhau cùng thread (fetchRepoJson giữ lock trong
 * lúc Http chạy → CookiesUtils.getCookies xin lock nữa) tự treo vĩnh viễn vì Mutex không
 * reentrant: tab Nguồn iOS trắng im ("KHO REPO (0)", không message — không có exception,
 * chỉ hang). JVM không gặp vì synchronized per-object + reentrant.
 *
 * ponytail: map không dọn entry — lock object là singleton/companion nên hữu hạn; lock
 * lồng CÙNG 1 object vẫn treo (chưa có call site nào) — thêm thread-id reentrancy nếu sau
 * này xuất hiện.
 */
private val LOCKS = HashMap<Any, Mutex>()
private val LOCKS_GUARD = Mutex()

internal actual fun <T> monitorLock(
    lock: Any,
    block: () -> T,
): T = runBlocking {
    val mutex = LOCKS_GUARD.withLock { LOCKS.getOrPut(lock) { Mutex() } }
    mutex.withLock { block() }
}
