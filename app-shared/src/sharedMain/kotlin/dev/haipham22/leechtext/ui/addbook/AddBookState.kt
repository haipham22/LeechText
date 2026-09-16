package dev.haipham22.leechtext.ui.addbook
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import dev.haipham22.leechtext.action.saveHistory
import dev.haipham22.leechtext.entities.PluginEntity
import dev.haipham22.leechtext.get.autoFixAllNames
import dev.haipham22.leechtext.get.fetchChapterList
import dev.haipham22.leechtext.get.fetchInfo
import dev.haipham22.leechtext.get.normalizeUrl
import dev.haipham22.leechtext.get.optimizeAllNames
import dev.haipham22.leechtext.log.EngineLogger
import dev.haipham22.leechtext.models.Properties
import dev.haipham22.leechtext.plugin.PluginManager
import dev.haipham22.leechtext.plugin.vbook.VBookPluginService
import dev.haipham22.leechtext.resources.Res
import dev.haipham22.leechtext.resources.addbook_added_n
import dev.haipham22.leechtext.resources.addbook_added_partial
import dev.haipham22.leechtext.resources.addbook_added_to_library
import dev.haipham22.leechtext.resources.addbook_cancelled
import dev.haipham22.leechtext.resources.addbook_finding_plugin
import dev.haipham22.leechtext.resources.err_chapter_empty
import dev.haipham22.leechtext.resources.err_chapters_fetch_failed
import dev.haipham22.leechtext.resources.err_plugin_missing_hint
import dev.haipham22.leechtext.resources.err_plugin_no_match
import dev.haipham22.leechtext.ui.BookPipeline
import dev.haipham22.leechtext.ui.IoDispatcher
import dev.haipham22.leechtext.ui.model.UiException
import dev.haipham22.leechtext.ui.model.UiText
import dev.haipham22.leechtext.ui.model.uiTextOf
import dev.haipham22.leechtext.util.SettingsRepository
import dev.haipham22.leechtext.util.readTextOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.Path.Companion.toPath

/**
 * State + logic màn thêm sách (port ý AddDialog.java): URL → plugin match → info + TOC → chọn range
 * → download song song → save properties.json.
 */
@Suppress("TooManyFunctions", "LongParameterList") // Decompose component — action công khai + deps từ Koin
class AddBookState(
    componentContext: ComponentContext,
    val log: EngineLogger,
    val pluginManager: PluginManager,
) : ComponentContext by componentContext {
    sealed interface Status {
        data object Idle : Status

        data object LoadingInfo : Status

        data object Ready : Status

        data object Downloading : Status

        data object Done : Status

        data class Failed(
            val error: UiText,
        ) : Status
    }

    var url by mutableStateOf("")
    var rangeText by mutableStateOf("")
    var status by mutableStateOf<Status>(Status.Idle)
    var properties by mutableStateOf<Properties?>(null)
    var completed by mutableIntStateOf(0)
    var total by mutableIntStateOf(0)
    var message by mutableStateOf<UiText?>(null)

    // Bảng hiệu chỉnh tên chương (N2) — version bump để recompose khi mutate Chapter
    var showChapters by mutableStateOf(false)
    var chaptersVersion by mutableIntStateOf(0)

    private val scope = coroutineScope()
    private var job: kotlinx.coroutines.Job? = null

    /** N2: sửa tên chương hàng loạt — port Config.autoFixName / Optimize. */
    fun autoFixNames() {
        val list = properties?.chapList ?: return
        scope.launch {
            // Regex hàng nghìn chương — chạy IO khỏi đứng main
            withContext(IoDispatcher) { list.autoFixAllNames() }
            chaptersVersion++
        }
    }

    fun optimizeNames() {
        val list = properties?.chapList ?: return
        scope.launch {
            withContext(IoDispatcher) { list.optimizeAllNames() }
            chaptersVersion++
        }
    }

    /** Dán nhiều URL → thêm nhanh từng sách (metadata-only) vào thư viện, không tải. */
    fun addUrlsToLibrary(urls: List<String>) {
        job?.cancel()
        job =
            scope.launch {
                var ok = 0
                var lastErr: String? = null
                for (u in urls) {
                    try {
                        // fetchInfo network + ghi properties.json — blocking IO
                        withContext(IoDispatcher) { BookPipeline.addToLibrary(u, log, pluginManager) }
                        ok++
                    } catch (e: Exception) {
                        lastErr = e.message ?: e::class.simpleName
                    }
                }
                message =
                    if (ok == urls.size) {
                        UiText.Res(Res.string.addbook_added_n, listOf(ok), UiText.Tone.SUCCESS)
                    } else {
                        UiText.Res(Res.string.addbook_added_partial, listOf(ok, urls.size, lastErr ?: ""), UiText.Tone.ERROR)
                    }
            }
    }

    /** Hủy fetch/dò đang chạy — quay về Idle. */
    fun cancel() {
        job?.cancel()
        job = null
        status = Status.Idle
        message = UiText.Res(Res.string.addbook_cancelled)
    }

    /** Đọc 1 chương (fetch on demand, KHÔNG ghi raw/, KHÔNG thêm thư viện). */
    var previewIndex by mutableStateOf<Int?>(null)
    var previewText by mutableStateOf("")
    var previewError by mutableStateOf<UiText?>(null)
    var fetchingChapter by mutableStateOf(false)

    fun openPreviewChapter(index: Int) {
        val p = properties ?: return
        if (previewIndex == null) previewIndex = index // hiện reader ngay, chữ tới sau
        previewText = ""
        previewError = null
        fetchingChapter = true
        scope.launch {
            try {
                ensureChapters(p)
                val chapter = p.chapList?.getOrNull(index) ?: throw UiException(UiText.Res(Res.string.err_chapter_empty, listOf(index), UiText.Tone.ERROR))
                previewIndex = index
                val plugin =
                    pluginManager.get(p.url ?: "")
                        ?: throw UiException(UiText.Res(Res.string.err_plugin_no_match, listOf(p.url ?: ""), UiText.Tone.ERROR))
                // TextLoader.load (network + Rhino) + Optimize — blocking IO
                previewText =
                    withContext(IoDispatcher) {
                        val text =
                            dev.haipham22.leechtext.plugin.js.loader.TextLoader
                                .with(plugin, log)
                                .load(chapter.url)
                        (
                            dev.haipham22.leechtext.util.SyntaxUtils
                                .optimize(text) ?: text ?: ""
                            ).ifEmpty { "Chương trống hoặc không tải được" } // ponytail: luồng nội dung động, chưa UiText hóa
                    }
            } catch (e: Exception) {
                previewError = uiTextOf(e) // hiện trong PreviewChapterReader (đã localize)
            } finally {
                fetchingChapter = false
            }
        }
    }

    fun closePreviewChapter() {
        previewIndex = null
    }

    /**
     * Bước 1: fetch info theo URL. loadChapters=false (preview từ Nguồn) → chỉ info, TOC lấy sau
     * khi cần (nút Xem danh sách chương / Đọc / Thêm). Không có plugin → auto-dò + cài từ repo.
     */
    fun loadInfo(
        loadChapters: Boolean = true,
        autoInstall: Boolean = true,
    ) {
        job?.cancel()
        job =
            scope.launch {
                status = Status.LoadingInfo
                message = null
                try {
                    val trimmed = url.normalizeUrl()
                    val plugin = resolvePlugin(trimmed, autoInstall)
                    val p = fetchBookInfo(trimmed, plugin, loadChapters)
                    if (loadChapters) {
                        rangeText = "1-${p.size}"
                        chaptersLoaded = true
                    }
                    properties = p
                    status = Status.Ready
                } catch (e: kotlinx.coroutines.CancellationException) {
                    // đã xử lý trong cancel() — status Idle
                } catch (e: Exception) {
                    status = Status.Failed(uiTextOf(e))
                }
            }
    }

    /**
     * Tìm plugin khớp URL; không có + autoInstall → dò repo, cài plugin khớp rồi
     * chạy luôn (download zip + convert — blocking IO). Flow Nguồn (autoInstall=false)
     * không tự cài — user yêu cầu 2026-08-27: chỉ đọc nguồn đã cài.
     */
    private suspend fun resolvePlugin(
        trimmed: String,
        autoInstall: Boolean,
    ): PluginEntity {
        val found = pluginManager.get(trimmed)
        if (found != null) return found
        if (!autoInstall) {
            throw UiException(UiText.Res(Res.string.err_plugin_missing_hint, tone = UiText.Tone.ERROR))
        }
        message = UiText.Res(Res.string.addbook_finding_plugin)
        val installed =
            withContext(IoDispatcher) { VBookPluginService(log, pluginManager = pluginManager).findAndInstallByUrl(trimmed) }
                ?: throw UiException(UiText.Res(Res.string.err_plugin_no_match, listOf(trimmed), UiText.Tone.ERROR))
        message = null
        return installed
    }

    /** fetchInfo/fetchChapterList = network + Rhino — blocking IO. */
    private suspend fun fetchBookInfo(
        trimmed: String,
        plugin: PluginEntity,
        loadChapters: Boolean,
    ): Properties = withContext(IoDispatcher) {
        val props = Properties().apply { url = trimmed }
        props.fetchInfo(plugin, log)
        if (loadChapters) {
            props.fetchChapterList(plugin, log)
            if (props.chapList.isNullOrEmpty()) {
                throw UiException(UiText.Res(Res.string.err_chapters_fetch_failed, listOf(props.url ?: trimmed), UiText.Tone.ERROR))
            }
        }
        props
    }

    /** TOC đã fetch chưa — preview mở detail không load sẵn chương. */
    var chaptersLoaded by mutableStateOf(false)
    var loadingChapters by mutableStateOf(false)

    /** Fetch TOC khi người dùng yêu cầu (nút Xem danh sách chương). */
    fun loadChapterList() {
        val p = properties ?: return
        if (chaptersLoaded || loadingChapters) return
        loadingChapters = true
        scope.launch {
            try {
                val plugin =
                    pluginManager.get(p.url ?: "")
                        ?: throw UiException(UiText.Res(Res.string.err_plugin_no_match, listOf(p.url ?: ""), UiText.Tone.ERROR))
                // Network + Rhino — blocking IO
                withContext(IoDispatcher) { p.fetchChapterList(plugin, log) }
                if (p.chapList.isNullOrEmpty()) {
                    throw UiException(UiText.Res(Res.string.err_chapters_fetch_failed, listOf(p.url ?: ""), UiText.Tone.ERROR))
                }
                rangeText = "1-${p.size}"
                chaptersLoaded = true
            } catch (e: Exception) {
                message = uiTextOf(e)
            } finally {
                loadingChapters = false
            }
        }
    }

    /** Đảm bảo có TOC trước khi đọc/thêm — fetch nếu chưa (lazy). */
    private suspend fun ensureChapters(p: Properties) {
        if (chaptersLoaded) return
        val plugin =
            pluginManager.get(p.url ?: "")
                ?: throw UiException(UiText.Res(Res.string.err_plugin_no_match, listOf(p.url ?: ""), UiText.Tone.ERROR))
        withContext(IoDispatcher) { p.fetchChapterList(plugin, log) }
        if (p.chapList.isNullOrEmpty()) {
            throw UiException(UiText.Res(Res.string.err_chapters_fetch_failed, listOf(p.url ?: ""), UiText.Tone.ERROR))
        }
        rangeText = "1-${p.size}"
        chaptersLoaded = true
    }

    /** Vị trí đọc cuối (lastread.txt) khi sách đã trong thư viện — "Đọc" ở preview resume theo nó. */
    suspend fun lastReadIndex(): Int? {
        if (!inLibrary) return null
        val p = properties ?: return null
        val bookUrl = p.url ?: return null
        return withContext(IoDispatcher) {
            runCatching {
                val dir = BookPipeline.savePathFor(bookUrl, p.name, SettingsRepository.load())
                (dir / "lastread.txt").readTextOrNull()?.trim()?.toIntOrNull()
            }.getOrNull()
        }
    }

    /**
     * Bước 2: tạo thư mục + lưu properties.json. KHÔNG tải chương — tải sau ở tab Thư viện. TOC đã
     * fetch (tab Thêm sách) → lưu FULL TOC (dogfood 260902: không subset theo range — range chỉ
     * dùng lúc tải); chưa fetch (preview từ Nguồn) → thêm metadata-only, TOC lấy lazy khi mở sách.
     */
    fun addToLibrary() {
        job?.cancel()
        job =
            scope.launch {
                val p = properties ?: return@launch
                try {
                    // savePathFor quét output/ + parse properties.json, saveHistory ghi file — IO
                    withContext(IoDispatcher) {
                        val settings = SettingsRepository.load()
                        val savePath = BookPipeline.savePathFor(p.url, p.name, settings).toString()
                        p.savePath = savePath
                        val rawDir = savePath.toPath() / "raw"
                        val dirOk = runCatching { okio.FileSystem.SYSTEM.createDirectories(rawDir) }.isSuccess
                        log.add(
                            "[addToLibrary] savePath=${p.savePath} rawDir=$dirOk workPath=${settings.workPath}",
                        )
                        saveHistory(p, log)
                    }

                    inLibrary = true
                    message = UiText.Res(Res.string.addbook_added_to_library, tone = UiText.Tone.SUCCESS)
                    status = Status.Done
                } catch (e: kotlinx.coroutines.CancellationException) {
                    status = Status.Idle
                } catch (e: Exception) {
                    status = Status.Failed(uiTextOf(e))
                }
            }
    }

    /** Sách đã có trong Thư viện (properties.json theo URL) — nút Thêm disabled "✓ Đã trong thư viện". */
    var inLibrary by mutableStateOf(false)
        private set

    /** Check thư viện theo URL (scan output/ + parse) — gọi sau khi loadInfo xong, qua IO. */
    fun checkInLibrary() {
        val p = properties ?: return
        val bookUrl = p.url ?: return
        scope.launch {
            inLibrary =
                withContext(IoDispatcher) {
                    val dir = BookPipeline.savePathFor(bookUrl, p.name, SettingsRepository.load())
                    dev.haipham22.leechtext.action
                        .loadHistory((dir / "properties.json").toString(), log)
                        ?.url == bookUrl
                }
        }
    }
}
