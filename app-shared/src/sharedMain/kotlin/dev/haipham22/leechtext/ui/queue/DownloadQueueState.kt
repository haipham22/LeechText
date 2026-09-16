package dev.haipham22.leechtext.ui.queue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import dev.haipham22.leechtext.get.normalizeUrl
import dev.haipham22.leechtext.log.EngineLogger
import dev.haipham22.leechtext.models.Properties
import dev.haipham22.leechtext.plugin.PluginManager
import dev.haipham22.leechtext.resources.Res
import dev.haipham22.leechtext.resources.error_prefix
import dev.haipham22.leechtext.resources.queue_status_cancelled
import dev.haipham22.leechtext.resources.queue_status_done
import dev.haipham22.leechtext.resources.queue_status_downloading
import dev.haipham22.leechtext.resources.queue_status_paused
import dev.haipham22.leechtext.resources.queue_status_waiting
import dev.haipham22.leechtext.ui.BookPipeline
import dev.haipham22.leechtext.ui.IoDispatcher
import dev.haipham22.leechtext.ui.model.UiException
import dev.haipham22.leechtext.ui.model.UiText
import dev.haipham22.leechtext.ui.startPlatformDownload
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 1 mục trong queue đa sách (MUST #13). rangeText = "1-5,8" chọn lẻ lúc enqueue (N2), null = full. */
class QueueItem(
    val url: String,
    val rangeText: String? = null,
) {
    var name by mutableStateOf(url)
    var cover by mutableStateOf<String?>(null)
    var status by mutableStateOf<UiText>(UiText.Res(Res.string.queue_status_waiting))
    var completed by mutableIntStateOf(0)
    var total by mutableIntStateOf(0)
    var done by mutableStateOf(false)
    var failed by mutableStateOf(false)
    var paused by mutableStateOf(false)

    // cancel() đặt cờ — coroutine pump đọc để không ghi đè status/done sau khi hủy
    // (race test flaky: cancel khi job chưa kịp chạy → coroutine lỗi ghi đè "Cancelled")
    internal var cancelled by mutableStateOf(false)
    internal var job: Job? = null
    internal var book: Properties? = null
}

/**
 * Queue tải đa sách (MUST #13 + #14): sách xử lý tuần tự (maxConn trong mỗi sách đã
 * song song; tuần tự giữa sách để không dội site), hủy từng mục được.
 * ponytail: song song nhiều sách khi cần — thêm Semaphore(2) quanh downloadBook.
 */
@Suppress("LongParameterList") // Decompose component — deps từ RootComponent (Koin entry)
class DownloadQueueState(
    componentContext: ComponentContext,
    val log: EngineLogger,
    val pluginManager: PluginManager,
) : ComponentContext by componentContext {
    var items by mutableStateOf<List<QueueItem>>(emptyList())
        private set
    var running by mutableStateOf(false)
        private set

    private val scope = coroutineScope()

    fun enqueue(
        urls: List<String>,
        rangeText: String? = null,
    ) {
        val fresh =
            urls.mapNotNull { u ->
                val t = u.normalizeUrl()
                if (t.isBlank()) {
                    null
                } else if (items.any { !it.done && (it.url == t || it.name == t) }) {
                    null // trùng mục CHƯA xong; mục done để clearFinished dọn (dogfood 260906)
                } else {
                    QueueItem(t, rangeText)
                }
            }
        if (fresh.isEmpty()) return
        items = items + fresh
        pump()
    }

    fun cancel(item: QueueItem) {
        item.cancelled = true
        item.job?.cancel()
        if (!item.paused) {
            item.status = UiText.Res(Res.string.queue_status_cancelled)
            item.done = true
        }
    }

    fun pause(item: QueueItem) {
        item.job?.cancel()
        item.paused = true
        item.status = UiText.Res(Res.string.queue_status_paused)
        item.job = null
        items = items.toList()
    }

    fun resume(item: QueueItem) {
        item.paused = false
        item.job = null
        items = items.toList()
        pump()
    }

    fun clearFinished() {
        items = items.filter { !it.done }
    }

    /** Ẩn 1 mục xong khỏi danh sách (sách đã vào thư viện). */
    fun dismiss(item: QueueItem) {
        items = items.filter { it !== item }
    }

    /** Mục lỗi → bỏ khỏi list và enqueue lại URL (enqueue dedup sẵn). */
    fun retry(item: QueueItem) {
        dismiss(item)
        enqueue(listOf(item.url), item.rangeText)
    }

    /** Chạy mục kế tiếp nếu chưa có gì đang chạy — tuần tự. */
    private fun pump() {
        if (running) return
        val next = items.firstOrNull { !it.done && it.job == null && !it.paused } ?: return
        running = true

        // Android: foreground service lo download — notification là UI tiến trình
        // (kèm nút Hủy), item rời queue ngay thay vì nằm lại với trạng thái stale
        if (startPlatformDownload(next.url, next.rangeText)) {
            items = items.filter { it !== next }
            running = false
            pump()
            return
        }

        next.job =
            scope.launch {
                try {
                    // pause()/cancel() trước khi coroutine kịp chạy — đừng để
                    // "Đang tải" đè lên "Tạm dừng" (race test flaky).
                    // paused-check bắt null-race: pause() đọc item.job lúc assignment
                    // chưa land → cancel no-op, job vẫn chạy
                    kotlinx.coroutines.currentCoroutineContext().ensureActive()
                    if (next.paused || next.cancelled) {
                        running = false
                        pump()
                        return@launch
                    }
                    downloadItem(next)
                } finally {
                    running = false
                    pump()
                }
            }
    }

    /** Tải 1 mục: status "Đang tải" → downloadBook (IO) → done/failed. */
    private suspend fun downloadItem(next: QueueItem) {
        try {
            next.status = UiText.Res(Res.string.queue_status_downloading)
            // downloadBook = fetch info/TOC + tải + ghi file — blocking IO, không block main
            val (book, summary) =
                withContext(IoDispatcher) {
                    BookPipeline.downloadBook(
                        next.url,
                        rangeText = next.rangeText,
                        onInfo = { info ->
                            next.name = info.name ?: next.url
                            next.cover = info.cover
                        },
                        onProgress = { c, t ->
                            next.completed = c
                            next.total = t
                        },
                        log = log,
                        pluginManager = pluginManager,
                    )
                }
            if (next.cancelled) return // cancel() đã set status/done — đừng ghi đè
            next.book = book
            next.name = book.name ?: next.url
            next.status = UiText.Res(Res.string.queue_status_done, listOf(summary.ok, summary.error), UiText.Tone.SUCCESS)
            next.done = true
        } catch (e: kotlinx.coroutines.CancellationException) {
            // cancel() đã set status
        } catch (e: UiException) {
            // Lỗi user-facing (thiếu plugin, hết chương...) — đã localize
            if (!next.cancelled) next.status = e.ui
        } catch (e: Exception) {
            if (next.cancelled) return // cancel() đã set status/done
            next.status =
                UiText.Res(
                    Res.string.error_prefix,
                    listOf(e.message ?: e::class.simpleName ?: "error"),
                    UiText.Tone.ERROR,
                )
            next.failed = true
            next.done = true
        }
    }
}
