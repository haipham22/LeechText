package dev.haipham22.leechtext.util

import dev.haipham22.leechtext.log.platformEngineLogger
import dev.haipham22.leechtext.plugin.PluginManager
import dev.haipham22.leechtext.plugin.vbook.VBookPluginService
import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.usleep
import kotlin.concurrent.atomics.AtomicInt
import kotlin.native.concurrent.Worker
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Regression "KHO REPO (0)" trên iOS: Monitor.apple cũ dùng 1 Mutex toàn cục cho mọi
 * monitorLock — lock lồng nhau cùng thread (fetchRepoJson giữ lock trong lúc Http chạy →
 * CookiesUtils.getCookies xin lock nữa) tự treo vĩnh viễn vì Mutex không reentrant.
 * JVM không gặp (synchronized per-object + reentrant) nên chỉ Apple runtime chết.
 *
 * Probe chạy trong Worker + poll cờ done với deadline — nếu regression quay lại, test
 * FAIL rõ thay vì treo cả suite (thread bị deadlock không cancel được, chỉ chờ tới giờ).
 */
class MonitorNestedLockTest {
    @OptIn(ExperimentalForeignApi::class, kotlin.concurrent.atomics.ExperimentalAtomicApi::class)
    private fun runBounded(timeoutMs: Long, block: () -> Unit): Boolean {
        val done = AtomicInt(0)
        val worker = Worker.start()
        worker.executeAfter(0L) {
            try {
                block()
            } finally {
                done.store(1)
            }
        }
        val start = nowMillis()
        while (done.load() == 0 && nowMillis() - start < timeoutMs) usleep(50_000u)
        return done.load() == 1
    }

    @Test
    fun monitorLockLongNhauKhacObjectKhongTreo() {
        val ok =
            runBounded(10_000L) {
                val inner = monitorLock(Any()) { 42 }
                monitorLock(Any()) { inner }
            }
        assertTrue(
            ok,
            "monitorLock lồng nhau (khác object) treo — Apple Monitor dùng 1 Mutex toàn cục " +
                "không reentrant (root cause tab Nguồn iOS rỗng)",
        )
    }

    /** Chuỗi đúng như app: fetch registry + parse + cache (cần mạng — local only, CI không chạy macosArm64Test). */
    @Test
    fun layPluginKhoQuaChuoiApp() {
        val ok =
            runBounded(90_000L) {
                val plugins = VBookPluginService(platformEngineLogger(), pluginManager = PluginManager(platformEngineLogger())).getAvailablePlugins()
                check(plugins.isNotEmpty()) { "registry rỗng — fetch/parse fail trên Apple" }
            }
        assertTrue(ok, "Chuỗi getAvailablePlugins() (monitorLock → Http → parse) treo/rỗng trên Apple")
    }
}
