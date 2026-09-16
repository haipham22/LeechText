package dev.haipham22.leechtext.ui.library

import dev.haipham22.leechtext.ui.model.UiText
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Crash fix (dropbox 2026-08-26): sách còn trong Thư viện nhưng plugin đã gỡ →
 * bấm Đọc → ensureToc từng error() throw IllegalStateException giết app.
 * Giờ phải set message lỗi + KHÔNG throw.
 */
class StartReadingNoPluginTest {
    @Test
    fun startReadingWithoutPluginShowsMessageInsteadOfCrash() {
        val state =
            LibraryState(
                dev.haipham22.leechtext.testComponentContext(),
                log = dev.haipham22.leechtext.log.platformEngineLogger(),
                pluginManager = dev.haipham22.leechtext.plugin.PluginManager(dev.haipham22.leechtext.log.platformEngineLogger()),
                scopeDispatcher = kotlinx.coroutines.Dispatchers.Default,
            )
        val book =
            dev.haipham22.leechtext.models.Properties().apply {
                url = "https://khong-plugin.invalid/sach-nao-do"
                name = "Sách test"
                chapList = null // metadata-only → ensureToc phải chạy
            }
        state.selected = book

        val threw = try {
            state.startReading()
            false
        } catch (e: IllegalStateException) {
            true
        }
        assertTrue(!threw, "startReading không được throw khi thiếu plugin")

        // chờ coroutine scope chạy xong (viewModelScope = Main swing dispatcher)
        val deadline = System.currentTimeMillis() + 5000
        while (state.message == null && System.currentTimeMillis() < deadline) Thread.sleep(50)
        assertTrue(state.message is UiText.Raw, "phải có message 'thiếu plugin' cụ thể, được: ${state.message}")
    }
}
