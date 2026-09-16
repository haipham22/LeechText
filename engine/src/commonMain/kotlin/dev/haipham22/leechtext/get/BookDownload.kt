package dev.haipham22.leechtext.get

import dev.haipham22.leechtext.log.EngineLogger
import dev.haipham22.leechtext.models.Chapter
import dev.haipham22.leechtext.models.Properties
import dev.haipham22.leechtext.plugin.PluginManager
import dev.haipham22.leechtext.plugin.js.loader.TextLoader
import dev.haipham22.leechtext.util.AppSettings
import dev.haipham22.leechtext.util.SettingsRepository
import dev.haipham22.leechtext.util.SyntaxUtils
import dev.haipham22.leechtext.util.writeTo
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch

@OptIn(ExperimentalAtomicApi::class)
/**
 * Download toàn bộ chapter list song song theo settings.maxConn (port
 * action/Download.java + get/ChapExecute.java — SwingWorker fan-out thay bằng Semaphore,
 * mỗi chương một coroutine; parallelism thực tế còn bị kẹp bởi EngineDispatchers = 5).
 *
 * Resume (MUST #4): chương đã có file `raw/<id>.txt` khác rỗng thì skip không fetch lại —
 * tái chạy chỉ tải chương thiếu, đúng hành vi fix legacy 2026-08-23.
 *
 * Bỏ so với bản gốc: forum mode (CUT C4), pause/cancel trong phiên (NICE N1),
 * pass "checking" retry (mỗi chương được thử đúng 1 lần — ponytail: thêm vòng retry
 * khi UI cần nút "tải lại chương lỗi").
 */
suspend fun Properties.downloadChapters(
    settings: AppSettings = SettingsRepository.load(),
    onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> },
    fetch: (suspend (Chapter) -> String?)? = null,
    chapters: List<Chapter>? = null, // override — chỉ tải những chương trong list này
    log: EngineLogger,
    pluginManager: PluginManager,
): DownloadSummary {
    val list = chapters ?: chapList.orEmpty()
    val total = list.size
    // Chỉ resolve plugin khi dùng TextLoader thật — path test inject fetch không cần plugin
    val plugin =
        if (fetch == null) {
            pluginManager.get(url ?: "")
                ?: throw IllegalArgumentException("No plugin matches URL: $url")
        } else {
            null
        }
    val loader = plugin?.let { TextLoader.with(it, log) }
    val doFetch: suspend (Chapter) -> String? =
        fetch ?: { chapter -> loader!!.load(chapter.url) }

    // Per-plugin config override global (PluginSettings: plugin config → spec default → global)
    val maxConn = PluginSettings.maxConn(plugin, settings)
    val session =
        DownloadSession(
            log = log,
            semaphore = Semaphore(maxConn),
            savePath = savePath ?: "",
            delayMs = PluginSettings.delayMs(plugin, settings),
            charset = charset,
            total = total,
            onProgress = onProgress,
        )

    coroutineScope {
        list
            .map { chapter ->
                async { session.runOne(chapter, doFetch) }
            }.awaitAll()
    }

    val summary =
        DownloadSummary(
            total = total,
            resumed = session.resumed.load(),
            ok = session.ok.load(),
            empty = session.empty.load(),
            error = session.error.load(),
        )
    log.debug(
        "[downloadChapters] $url → total=$total resumed=${summary.resumed} ok=${summary.ok} empty=${summary.empty} error=${summary.error}",
    )
    return summary
}

/**
 * Trạng thái dùng chung 1 lượt downloadChapters: counters + circuit breaker
 * (site 503/rate-limit liên tục → dừng cả lô thay vì spam N chương × retry mỗi
 * chương — dogfood 260905: Cầu Ma 503 → spam tới chết).
 * ponytail: đếm N lỗi STT (không theo tỉ lệ) — đơn giản, đủ cho trang đọc truyện
 */
@OptIn(ExperimentalAtomicApi::class)
@Suppress("LongParameterList") // tham số log DI + params nghiệp vụ — bundle làm code khó đọc
private class DownloadSession(
    private val log: EngineLogger,
    val semaphore: Semaphore,
    val savePath: String,
    val delayMs: Int,
    val charset: String?,
    val total: Int,
    val onProgress: (completed: Int, total: Int) -> Unit,
) {
    val resumed = AtomicInt(0)
    val ok = AtomicInt(0)
    val empty = AtomicInt(0)
    val error = AtomicInt(0)
    private val completedCount = AtomicInt(0)
    private val consecutiveErrors = AtomicInt(0)
    private val breaker = AtomicInt(0) // 1 = trip

    /** Body 1 coroutine chương: breaker check (trước + sau khi lấy permit) → tải → phân loại → đếm. */
    suspend fun runOne(
        chapter: Chapter,
        fetch: suspend (Chapter) -> String?,
    ) {
        if (skipWhenBreakerOpen(chapter)) return
        semaphore.withPermit {
            if (skipWhenBreakerOpen(chapter)) return@withPermit
            val outcome = downloadOne(chapter, fetch, savePath, delayMs, charset, log)
            if (outcome == Outcome.ERROR) {
                if (consecutiveErrors.incrementAndFetch() >= CONSECUTIVE_ERROR_LIMIT) {
                    breaker.compareAndSet(0, 1)
                    log
                        .add("[downloadChapters] $CONSECUTIVE_ERROR_LIMIT lỗi liên tiếp — dừng download (site chặn/rate-limit?)")
                }
            } else {
                consecutiveErrors.store(0)
            }
            when (outcome) {
                Outcome.RESUMED -> resumed.incrementAndFetch()
                Outcome.OK -> ok.incrementAndFetch()
                Outcome.EMPTY -> empty.incrementAndFetch()
                Outcome.ERROR -> error.incrementAndFetch()
            }
            log
                .debug("[downloadOne] ${chapter.id} → $outcome")
            onProgress(completedCount.incrementAndFetch(), total)
        }
    }

    /** Breaker mở → skip chương, đếm error, không tốn permit. */
    private fun skipWhenBreakerOpen(chapter: Chapter): Boolean {
        if (breaker.load() == 0) return false
        log
            .debug("[downloadChapters] breaker open — skip ${chapter.id}")
        error.incrementAndFetch()
        return true
    }
}

private enum class Outcome { RESUMED, OK, EMPTY, ERROR }

/** Số chương lỗi liên tiếp trước khi ngắt cả lô (site 503/rate-limit). */
private const val CONSECUTIVE_ERROR_LIMIT = 8

/** Tải 1 chương — thân ChapExecute: resume check → delay → fetch → phân loại → ghi file. */
@Suppress("LongParameterList") // tham số log DI + params nghiệp vụ — bundle làm code khó đọc
private suspend fun downloadOne(
    chapter: Chapter,
    fetch: suspend (Chapter) -> String?,
    savePath: String,
    delayMs: Int,
    charset: String?,
    log: EngineLogger,
): Outcome {
    val fs = FileSystem.SYSTEM
    // Resume: đã có file raw từ lần chạy trước thì không fetch lại
    val rawPath = savePath.toPath() / "raw" / "${chapter.id}.txt"
    val rawMetadata = fs.metadataOrNull(rawPath)
    if (rawMetadata != null && (rawMetadata.size ?: 0L) > 0L) {
        chapter.completed = true
        chapter.error = false
        chapter.empty = false
        return Outcome.RESUMED
    }

    if (delayMs > 0) delay(delayMs.toLong())

    return try {
        val text = fetch(chapter)
        chapter.error = false
        chapter.empty = false
        log
            .debug("[downloadOne] ${chapter.id} fetch xong ${text?.length ?: 0} chars")
        val outcome =
            when {
                text.isNullOrEmpty() -> {
                    chapter.error = true
                    Outcome.ERROR
                }

                // Chương ngắn: chứa <img> = chương ảnh, ngắn trơn = rỗng/block page.
                // Đánh error để hiện clay dot + retry được — không ghi file rác
                text.length < 1000 && !text.contains("<img ") -> {
                    chapter.empty = true
                    chapter.error = true
                    Outcome.EMPTY
                }

                text.length < 1000 -> {
                    chapter.imageChapter = true
                    Outcome.OK
                }

                else -> {
                    Outcome.OK
                }
            }
        chapter.completed = true
        // Ghi file trừ chương lỗi (bản gốc vẫn ghi chương empty/image)
        if (!chapter.error && text != null) {
            log
                .add("[downloadOne] write $rawPath (${text.length} chars)")
            (SyntaxUtils.optimize(text) ?: text).writeTo(rawPath.toString(), charset ?: "UTF-8", log)
        }
        outcome
    } catch (e: kotlinx.coroutines.CancellationException) {
        // Cancel propagation — nuốt CE khiến chương đang fetch vẫn ghi file sau khi Hủy
        throw e
    } catch (e: Exception) {
        chapter.error = true
        chapter.completed = true
        Outcome.ERROR
    }
}
