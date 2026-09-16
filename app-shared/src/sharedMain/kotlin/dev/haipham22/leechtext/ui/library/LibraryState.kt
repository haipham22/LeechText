package dev.haipham22.leechtext.ui.library
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.arkivanov.essenty.lifecycle.doOnDestroy
import dev.haipham22.leechtext.action.export.Ebook
import dev.haipham22.leechtext.action.export.ProgressListener
import dev.haipham22.leechtext.action.export.Text
import dev.haipham22.leechtext.action.loadHistory
import dev.haipham22.leechtext.action.saveHistory
import dev.haipham22.leechtext.get.downloadChapterImages
import dev.haipham22.leechtext.get.downloadChapters
import dev.haipham22.leechtext.get.fetchChapterList
import dev.haipham22.leechtext.get.mergeFetchedChapters
import dev.haipham22.leechtext.log.EngineLogger
import dev.haipham22.leechtext.models.Properties
import dev.haipham22.leechtext.plugin.PluginManager
import dev.haipham22.leechtext.resources.Res
import dev.haipham22.leechtext.resources.addbook_chapter_empty
import dev.haipham22.leechtext.resources.chapter_list_failed
import dev.haipham22.leechtext.resources.err_chapters_fetch_failed
import dev.haipham22.leechtext.resources.err_cover_change_failed
import dev.haipham22.leechtext.resources.err_cover_changed
import dev.haipham22.leechtext.resources.err_plugin_no_match
import dev.haipham22.leechtext.resources.error_prefix
import dev.haipham22.leechtext.resources.lib_chapter_not_downloaded
import dev.haipham22.leechtext.resources.lib_chapter_saved
import dev.haipham22.leechtext.resources.lib_checking_new
import dev.haipham22.leechtext.resources.lib_delete_failed
import dev.haipham22.leechtext.resources.lib_deleted
import dev.haipham22.leechtext.resources.lib_download_done
import dev.haipham22.leechtext.resources.lib_downloading_images
import dev.haipham22.leechtext.resources.lib_downloading_n
import dev.haipham22.leechtext.resources.lib_export_epub_done
import dev.haipham22.leechtext.resources.lib_export_epub_failed
import dev.haipham22.leechtext.resources.lib_export_epub_start
import dev.haipham22.leechtext.resources.lib_export_not_found
import dev.haipham22.leechtext.resources.lib_export_txt_done
import dev.haipham22.leechtext.resources.lib_export_txt_failed
import dev.haipham22.leechtext.resources.lib_exporting_txt
import dev.haipham22.leechtext.resources.lib_images_done
import dev.haipham22.leechtext.resources.lib_images_failed
import dev.haipham22.leechtext.resources.lib_migrate_done
import dev.haipham22.leechtext.resources.lib_migrating
import dev.haipham22.leechtext.resources.lib_names_saved
import dev.haipham22.leechtext.resources.lib_new_chapters_found
import dev.haipham22.leechtext.resources.lib_no_new_chapters
import dev.haipham22.leechtext.resources.lib_redownloaded
import dev.haipham22.leechtext.resources.lib_retry_done
import dev.haipham22.leechtext.resources.lib_retry_none_failed
import dev.haipham22.leechtext.resources.lib_retrying_failed
import dev.haipham22.leechtext.resources.lib_toc_failed
import dev.haipham22.leechtext.resources.sort_added
import dev.haipham22.leechtext.resources.sort_name
import dev.haipham22.leechtext.resources.sort_read
import dev.haipham22.leechtext.ui.BookPipeline
import dev.haipham22.leechtext.ui.IS_IOS
import dev.haipham22.leechtext.ui.IoDispatcher
import dev.haipham22.leechtext.ui.components.atoms.invalidateCoverCache
import dev.haipham22.leechtext.ui.model.UiException
import dev.haipham22.leechtext.ui.model.UiText
import dev.haipham22.leechtext.ui.model.uiTextOf
import dev.haipham22.leechtext.ui.shareFile
import dev.haipham22.leechtext.util.EnginePaths
import dev.haipham22.leechtext.util.SettingsRepository
import dev.haipham22.leechtext.util.TypeUtils
import dev.haipham22.leechtext.util.readTextOrNull
import dev.haipham22.leechtext.util.writeTo
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.jetbrains.compose.resources.StringResource

/** File đánh dấu chương đang đọc trong savePath sách. */
private const val LASTREAD_FILE = "lastread.txt"
private val fs: FileSystem get() = FileSystem.SYSTEM

/** (dir nullable, child) → Path — Properties.savePath String?; null = cwd (exists=false). */
private fun fileIn(dir: String?, child: String): Path = (dir ?: "").toPath() / child

/**
 * Nơi copy file export khi user nhập path thủ công (dogfood 260902: mặc định dialog là
 * thư mục out/ → copy thẳng vào path báo "Is a directory"). Path là thư mục có sẵn
 * → copy vào BÊN TRONG giữ tên file; ngược lại coi là đường dẫn file đầy đủ.
 * Trùng src = null (bỏ qua copy) — copy(src, src) truncate chính nguồn → epub 0 byte (bug 260903).
 */
private fun copyTargetFor(
    src: String,
    outputPath: String,
): String? {
    val dst = outputPath.toPath()
    val fs = FileSystem.SYSTEM
    val target =
        if (fs.metadataOrNull(dst)?.isDirectory == true) {
            (dst / src.toPath().name).toString()
        } else {
            outputPath
        }
    return if (target.toPath() == src.toPath()) null else target
}

/** iOS: export xong mở share sheet — không bắt user lục path sandbox. */
private fun shareExportIfIos(path: String?) {
    if (IS_IOS && path != null) shareFile(path)
}
private fun Path.mtime(): Long = fs.metadataOrNull(this)?.lastModifiedAtMillis ?: 0L

/** Kiểu sắp xếp thư viện — key từ file mtime (không cần schema mới). */
enum class LibrarySort(
    val labelRes: StringResource,
) {
    /** properties.json mtime — gần đúng "mới thêm/cập nhật". */
    ADDED(Res.string.sort_added),

    /** lastread.txt mtime — chưa đọc bao giờ (mtime 0) xếp cuối. */
    READ(Res.string.sort_read),
    NAME(Res.string.sort_name),
}
/**
 * State + logic thư viện (MUST #6) và truyện đang ra (MUST #5): scan properties.json
 * trong các thư mục con của output/,
 * kiểm tra chương mới = fetch TOC → merge theo URL → download thiếu (resume bỏ qua file cũ).
 */

/** Màn trong Library: Grid (danh sách) / Detail (sách) / Reader (đọc chương). */
sealed interface LibraryConfig {
    data object Grid : LibraryConfig
    data object Detail : LibraryConfig
    data object Reader : LibraryConfig
}

/** Child của Library stack — giữ context + state chung (state hoist ở LibraryState). */
class LibraryChild(
    val context: ComponentContext,
    val state: LibraryState,
)

@Suppress("TooManyFunctions", "LongParameterList") // Decompose component — action công khai + deps từ Koin
class LibraryState(
    componentContext: ComponentContext,
    val log: EngineLogger,
    val pluginManager: PluginManager,
    // Test JVM không có Main dispatcher — truyền Dispatchers.Default khi test
    scopeDispatcher: kotlinx.coroutines.CoroutineDispatcher = kotlinx.coroutines.Dispatchers.Main.immediate,
) : ComponentContext by componentContext {
    var books by mutableStateOf<List<Properties>>(emptyList())
    var sortBy by mutableStateOf(LibrarySort.ADDED)
    var selected by mutableStateOf<Properties?>(null)
    var busy by mutableStateOf(false)
    var completed by mutableIntStateOf(0)
    var total by mutableIntStateOf(0)
    var message by mutableStateOf<UiText?>(null)

    /** Vị trí đọc cuối theo savePath (lastread.txt) — badge "đang đọc chương mấy" ở grid. */
    var lastReadMap by mutableStateOf<Map<String, Int>>(emptyMap())
        private set

    // Tùy chọn export EPUB (mặc định = legacy: nén 9, có ảnh, không tách quyển)
    var exportCompress by mutableIntStateOf(9)
    var exportSplit by mutableStateOf(false)
    var exportImages by mutableStateOf(true)
    var exportProgress by mutableStateOf<UiText?>(null)

    /** File xuất gần nhất trong out/ (epub, txt) — cho nút Chia sẻ. */
    var lastExport by mutableStateOf<String?>(null)
    private fun latestExport(
        book: Properties,
        ext: String,
    ): String? = fs.listOrNull(fileIn(book.savePath, "out"))
        ?.filter { it.name.substringAfterLast('.', "").equals(ext, ignoreCase = true) }
        ?.maxByOrNull { fs.metadataOrNull(it)?.lastModifiedAtMillis ?: 0L }
        ?.toString()

    // Editor nội dung chương (yêu cầu owner 2026-08-23) — index trong chapList
    var editingIndex by mutableStateOf<Int?>(null)
    var chapterText by mutableStateOf("")
    private val scope = coroutineScope(scopeDispatcher)

    // ── Navigation (Decompose ChildStack): Grid → Detail → Reader ──
    private val navigation = StackNavigation<LibraryConfig>()
    val stack: Value<ChildStack<LibraryConfig, LibraryChild>> =
        childStack(
            source = navigation,
            serializer = null, // không process-death restore — như ViewModel cũ
            initialStack = { listOf(LibraryConfig.Grid) },
            handleBackButton = false, // PlatformBackHandler phía UI giữ semantics cũ
            childFactory = ::child,
        )
    private fun child(
        config: LibraryConfig,
        context: ComponentContext,
    ): LibraryChild = when (config) {
        LibraryConfig.Grid -> LibraryChild(context, this)

        LibraryConfig.Detail ->
            LibraryChild(context, this).apply { lifecycle.doOnDestroy { selected = null } }

        LibraryConfig.Reader ->
            LibraryChild(context, this).apply { lifecycle.doOnDestroy { editingIndex = null } }
    }

    /** Mở sách (grid/history) — Detail đã top thì không re-push. */
    fun openBook(b: Properties) {
        selected = b
        if (stack.value.active.configuration !is LibraryConfig.Detail) {
            navigation.push(LibraryConfig.Detail)
        }
    }
    fun closeBook() {
        navigation.pop()
    }
    fun closeChapter() {
        navigation.pop()
    }
    private fun outputDir(): Path {
        val workPath = SettingsRepository.load().workPath.ifEmpty { EnginePaths.dataDir.toString() }
        return workPath.toPath() / "output"
    }
    fun refresh() = scope.launch {
        // Đọc + parse properties.json mỗi sách (1000+ chương) — phải chạy IO,
        // để main là "Skipped N frames" khi mở app
        val sorted =
            withContext(IoDispatcher) {
                val dir = outputDir()
                val list =
                    fs.listOrNull(dir)
                        ?.filter { fs.metadataOrNull(it)?.isDirectory == true }
                        ?.mapNotNull { loadHistory((it / "properties.json").toString(), log) }
                        .orEmpty()
                        .let { l ->
                            when (sortBy) {
                                LibrarySort.ADDED -> l.sortedByDescending { fileIn(it.savePath, "properties.json").mtime() }

                                LibrarySort.READ -> l.sortedByDescending { fileIn(it.savePath, LASTREAD_FILE).mtime() }

                                LibrarySort.NAME -> {
                                    l.sortedBy { it.name ?: "" }
                                }
                            }
                        }
                // Badge "đang đọc chương mấy" — đọc lastread.txt từng sách cùng lượt IO
                lastReadMap =
                    list.associate { book ->
                        book.savePath.orEmpty() to
                            (fileIn(book.savePath, LASTREAD_FILE).readTextOrNull()?.trim()?.toIntOrNull() ?: -1)
                    }.filterValues { it >= 0 }
                list
            }
        books = sorted
        if (selected != null) selected = books.firstOrNull { it.url == selected?.url }
        // Thư viện trống → empty state UI tự lo (không hiện path workdir)
        message = null
    }

    /** Chương đọc cuối (lastread.txt) — null nếu chưa đọc lần nào. Đọc file qua IO. */
    suspend fun lastReadIndex(): Int? = selected?.let { book ->
        withContext(IoDispatcher) {
            runCatching { fileIn(book.savePath, LASTREAD_FILE).readTextOrNull()?.trim()?.toIntOrNull() }.getOrNull()
        }
    }

    /** MUST #5: re-fetch TOC → merge → chỉ tải chương mới. */
    fun checkNewChapters() = scope.launch {
        val book = selected ?: return@launch
        val url = book.url ?: return@launch
        busy = true
        message = UiText.Res(Res.string.lib_checking_new)
        try {
            message =
                withContext(IoDispatcher) {
                    // fetchChapterList (network) + merge + saveHistory (ghi file) — blocking
                    val plugin =
                        pluginManager.get(url)
                            ?: throw UiException(UiText.Res(Res.string.err_plugin_no_match, listOf(url), UiText.Tone.ERROR))
                    val fetched = Properties().apply { this.url = url }
                    fetched.fetchChapterList(plugin, log)
                    val fresh = fetched.chapList.orEmpty()
                    if (fresh.isEmpty()) {
                        throw UiException(UiText.Res(Res.string.err_chapters_fetch_failed, listOf(url), UiText.Tone.ERROR))
                    }
                    val newCount = book.mergeFetchedChapters(fresh)
                    saveHistory(book, log)
                    if (newCount == 0) {
                        UiText.Res(Res.string.lib_no_new_chapters, listOf(book.size))
                    } else {
                        UiText.Res(Res.string.lib_new_chapters_found, listOf(newCount, book.size), UiText.Tone.SUCCESS)
                    }
                }
        } catch (e: Exception) {
            message = uiTextOf(e)
        } finally {
            busy = false
        }
    }

    /** MUST #8: xuất TXT gộp 1 file (tach=1 → out/text.txt). */
    fun exportTxt(outputPath: String? = null) = scope.launch {
        val book = selected ?: return@launch
        busy = true
        message = UiText.Res(Res.string.lib_exporting_txt)
        try {
            // export đọc toàn bộ raw/ + ghi file — blocking IO
            withContext(IoDispatcher) {
                Text(
                    log,
                    book,
                    TypeUtils.TXT,
                    makeToc = false,
                    includeCss = false,
                    tach = 1,
                    settings = SettingsRepository.load(),
                ).export()
            }
            if (outputPath != null) {
                // Custom path — copy file sau khi export vào out/
                val src = latestExport(book, "txt")
                if (src != null) {
                    val target = copyTargetFor(src, outputPath)
                    if (target != null) {
                        FileSystem.SYSTEM.copy(src.toPath(), target.toPath())
                        message = UiText.Res(Res.string.lib_export_txt_done, listOf(target), UiText.Tone.SUCCESS)
                        lastExport = target
                        shareExportIfIos(target)
                    } else {
                        // Nơi lưu = chính out/ → file đã ở đúng chỗ, khỏi copy
                        message = UiText.Res(Res.string.lib_export_txt_done, listOf(src), UiText.Tone.SUCCESS)
                        lastExport = src
                        shareExportIfIos(src)
                    }
                } else {
                    message = UiText.Res(Res.string.lib_export_not_found, tone = UiText.Tone.ERROR)
                }
            } else {
                message = UiText.Res(Res.string.lib_export_txt_done, listOf(book.savePath ?: ""), UiText.Tone.SUCCESS)
                val src = latestExport(book, "txt")
                lastExport = src
                shareExportIfIos(src)
            }
        } catch (e: Exception) {
            message = UiText.Res(Res.string.lib_export_txt_failed, listOf(e.message ?: e::class.simpleName ?: "error"), UiText.Tone.ERROR)
        } finally {
            busy = false
        }
    }

    /** MUST #16: tải lại chương lỗi/thiếu — resume skip file cũ, chỉ fetch phần thiếu. */
    fun retryFailed() = scope.launch {
        val book = selected ?: return@launch
        busy = true
        try {
            // Sách thêm metadata-only → lấy TOC trước khi tải
            if (!ensureToc(book)) {
                if (message == null) message = UiText.Res(Res.string.chapter_list_failed, tone = UiText.Tone.ERROR)
                return@launch
            }
            val failed = book.chapList.orEmpty().count { it.error }
            completed = 0
            total = book.size
            message =
                if (failed == 0) {
                    UiText.Res(Res.string.lib_retry_none_failed)
                } else {
                    UiText.Res(Res.string.lib_retrying_failed, listOf(failed))
                }
            // downloadChapters = network + ghi raw/ + saveHistory ghi properties.json — blocking IO
            val summary =
                withContext(IoDispatcher) {
                    val s =
                        book.downloadChapters(
                            settings = SettingsRepository.load(),
                            onProgress = { c, t ->
                                completed = c
                                total = t
                            },
                            log = log,
                            pluginManager = pluginManager,
                        )
                    saveHistory(book, log)
                    s
                }
            message = UiText.Res(Res.string.lib_retry_done, listOf(summary.ok, summary.resumed, summary.error), UiText.Tone.SUCCESS)
        } catch (e: Exception) {
            message = UiText.Res(Res.string.error_prefix, listOf(e.message ?: e::class.simpleName ?: "error"), UiText.Tone.ERROR)
        } finally {
            busy = false
        }
    }

    /** MUST #15: tải ảnh chương ảnh — download <img> về data/Images, rewrite src. */
    fun downloadImages() = scope.launch {
        val book = selected ?: return@launch
        busy = true
        message = UiText.Res(Res.string.lib_downloading_images)
        try {
            // Network + ghi ảnh — blocking IO
            val n =
                withContext(IoDispatcher) {
                    book.downloadChapterImages(
                        log = log,
                        onProgress = { d, t ->
                            completed = d
                            total = t
                        },
                    )
                }
            message = UiText.Res(Res.string.lib_images_done, listOf(n, fileIn(book.savePath, "data/Images").toString()), UiText.Tone.SUCCESS)
        } catch (e: Exception) {
            message = UiText.Res(Res.string.lib_images_failed, listOf(e.message ?: e::class.simpleName ?: "error"), UiText.Tone.ERROR)
        } finally {
            busy = false
        }
    }

    /** Đổi nguồn (owner 2026-08-26): fetch TOC nguồn mới, khớp tên giữ file raw. */
    fun migrateSource(rawUrl: String) = scope.launch {
        val book = selected ?: return@launch
        busy = true
        message = UiText.Res(Res.string.lib_migrating)
        try {
            val r =
                withContext(IoDispatcher) {
                    BookPipeline.migrateSource(book, rawUrl, log, pluginManager)
                }
            message =
                UiText.Res(
                    Res.string.lib_migrate_done,
                    listOf(r.kept, r.total),
                    UiText.Tone.SUCCESS,
                )
            refresh()
        } catch (e: Exception) {
            message = uiTextOf(e)
        } finally {
            busy = false
        }
    }

    /** Set id chương đã có file raw — "đã tải" tin cậy theo file (flag completed có thể stale). */
    suspend fun downloadedChapterIds(): Set<String> {
        val book = selected ?: return emptySet()
        return withContext(IoDispatcher) {
            runCatching {
                fs.list(fileIn(book.savePath, "raw")).map { it.name.removeSuffix(".txt") }.toSet()
            }.getOrDefault(emptySet())
        }
    }

    /** Mở nội dung chương (raw/<id>.txt) để xem/sửa. Ghi vị trí đọc cuối. */
    fun openChapter(index: Int) {
        val book = selected ?: return
        val chapter = book.chapList?.getOrNull(index) ?: return
        editingIndex = index
        // changeChapter đi qua đây — Reader đã top thì KHÔNG re-push (reset page)
        if (stack.value.active.configuration !is LibraryConfig.Reader) {
            navigation.push(LibraryConfig.Reader)
        }
        scope.launch {
            // Đọc/ghi file raw — blocking IO (dogfood: mở chương đứng app)
            val text =
                withContext(IoDispatcher) {
                    runCatching { fs.write(fileIn(book.savePath, LASTREAD_FILE)) { writeUtf8(index.toString()) } }
                    fileIn(book.savePath, "raw/${chapter.id}.txt").readTextOrNull() ?: ""
                }
            chapterText = text
            if (text.isEmpty()) message = UiText.Res(Res.string.lib_chapter_not_downloaded)
        }
    }

    /** Tải theo range (popup chọn chương): parse "1-50,55" → chỉ tải những chương đó. */
    fun downloadRange(rangeText: String) = scope.launch {
        val book = selected ?: return@launch
        busy = true
        try {
            if (!ensureToc(book)) {
                if (message == null) message = UiText.Res(Res.string.chapter_list_failed, tone = UiText.Tone.ERROR)
                return@launch
            }
            val selected = BookPipeline.parseRange(rangeText, book.chapList.orEmpty())
            completed = 0
            total = selected.size
            message = UiText.Res(Res.string.lib_downloading_n, listOf(selected.size))
            val summary =
                withContext(IoDispatcher) {
                    val s =
                        book.downloadChapters(
                            settings = SettingsRepository.load(),
                            onProgress = { c, t ->
                                completed = c
                                total = t
                            },
                            chapters = selected,
                            log = log,
                            pluginManager = pluginManager,
                        )
                    saveHistory(book, log)
                    s
                }
            message = UiText.Res(Res.string.lib_download_done, listOf(summary.ok, summary.resumed, summary.error), UiText.Tone.SUCCESS)
        } catch (e: Exception) {
            message = uiTextOf(e)
        } finally {
            busy = false
        }
    }

    /** Tải lại 1 chương (nút trong reader): fetch → ghi đè raw/<id>.txt → cập nhật text đang đọc. */
    fun redownloadChapter(index: Int) = scope.launch {
        val book = selected ?: return@launch
        val chapter = book.chapList?.getOrNull(index) ?: return@launch
        val url = book.url ?: return@launch
        busy = true
        try {
            // TextLoader.load (network + Rhino) + Optimize + ghi file — blocking IO
            val optimized =
                withContext(IoDispatcher) {
                    val plugin =
                        pluginManager.get(url)
                            ?: throw UiException(UiText.Res(Res.string.err_plugin_no_match, listOf(url), UiText.Tone.ERROR))
                    val text =
                        dev.haipham22.leechtext.plugin.js.loader.TextLoader
                            .with(plugin, log)
                            .load(chapter.url)
                    if (text.isNullOrEmpty()) throw UiException(UiText.Res(Res.string.addbook_chapter_empty, tone = UiText.Tone.ERROR))
                    chapter.error = false
                    chapter.completed = true
                    val clean =
                        dev.haipham22.leechtext.util.SyntaxUtils
                            .optimize(text) ?: text
                    clean.writeTo(fileIn(book.savePath, "raw/${chapter.id}.txt").toString(), log = log)
                    saveHistory(book, log)
                    clean
                }
            if (editingIndex == index) chapterText = optimized
            message = UiText.Res(Res.string.lib_redownloaded, listOf(chapter.chapName ?: ""), UiText.Tone.SUCCESS)
        } catch (e: Exception) {
            message = uiTextOf(e)
        } finally {
            busy = false
        }
    }

    /** Nút Đọc tiếp: đúng vị trí đọc cuối (lastread.txt), fallback chương chưa đọc đầu. */
    fun resumeReading() {
        val book = selected ?: return
        fun open() {
            val last = runCatching { fileIn(book.savePath, LASTREAD_FILE).readTextOrNull()?.trim()?.toIntOrNull() }.getOrNull()
            val firstUnread =
                book.chapList
                    .orEmpty()
                    .indexOfFirst { !it.completed }
                    .takeIf { it >= 0 } ?: 0
            openChapter((last ?: firstUnread).coerceIn(0, (book.chapList?.size ?: 1) - 1))
        }
        // Sách thêm metadata-only → lấy TOC trước khi đọc
        if (book.chapList.isNullOrEmpty()) {
            scope.launch {
                busy = true
                try {
                    if (ensureToc(book)) {
                        open()
                    } else if (message == null) {
                        // ensureToc có thể đã set lỗi cụ thể hơn (thiếu plugin)
                        message = UiText.Res(Res.string.chapter_list_failed, tone = UiText.Tone.ERROR)
                    }
                } catch (e: Exception) {
                    // fetchChapterList throw (mạng/plugin) — không catch thì coroutine nuốt
                    // lỗi ngầm: nút Đọc nháy rồi im, user không biết sai gì
                    if (message == null) message = UiText.Raw(e.message ?: e::class.simpleName ?: "error", tone = UiText.Tone.ERROR)
                } finally {
                    busy = false
                }
            }
        } else {
            open()
        }
    }

    /** Nút Đọc: có vị trí đọc → đọc tiếp (dogfood 260902 — mất vị trí là cú bẫy);
     * chưa từng đọc → chương chưa đọc đầu tiên. */
    fun startReading() = resumeReading()

    /**
     * Thay ảnh bìa bằng file user chọn: copy vào savePath/data/cover-custom.jpg,
     * book.cover trỏ tới đó (CoverImage đọc file path) + persist.
     */
    fun replaceCover(sourcePath: String) {
        val book = selected ?: return
        val oldCover = book.cover
        scope.launch {
            try {
                withContext(IoDispatcher) {
                    val destDir = (book.savePath ?: "").toPath() / "data"
                    FileSystem.SYSTEM.createDirectories(destDir)
                    val dest = destDir / "cover-custom.jpg"
                    FileSystem.SYSTEM.copy(sourcePath.toPath(), dest)
                    book.cover = dest.toString()
                    saveHistory(book, log)
                }
                // Bust cache cover + reload selected — dest path GIỐNG lần đổi trước,
                // cache bitmap theo url trả ảnh cũ (bug 260906: phải thoát vào lại)
                invalidateCoverCache(oldCover)
                invalidateCoverCache(book.cover)
                refresh()
                message = UiText.Res(Res.string.err_cover_changed, tone = UiText.Tone.SUCCESS)
            } catch (e: Exception) {
                message = UiText.Res(Res.string.err_cover_change_failed, listOf(e.message ?: ""), UiText.Tone.ERROR)
            }
        }
    }

    /** TOC đang fetch nền (sách metadata-only) — hint UI. */
    var loadingToc by mutableStateOf(false)
        private set

    /**
     * Sách thêm nhanh (metadata-only) không có chapList → fetch TOC + lưu khi mở sách.
     * Chương vẫn chỉ tải khi bấm Tải tất cả.
     */
    fun loadToc() {
        val book = selected ?: return
        if (loadingToc || !book.chapList.isNullOrEmpty()) return
        loadingToc = true
        scope.launch {
            try {
                if (!ensureToc(book)) message = UiText.Res(Res.string.chapter_list_failed, tone = UiText.Tone.ERROR)
            } catch (e: Exception) {
                message = UiText.Res(Res.string.lib_toc_failed, listOf(e.message ?: e::class.simpleName ?: "error"), UiText.Tone.ERROR)
            } finally {
                loadingToc = false
            }
        }
    }

    /**
     * Lấy TOC nếu sách chưa có (metadata-only). False = không lấy được.
     * KHÔNG throw — caller startReading không catch, error() từng crash app
     * khi sách còn trong Thư viện nhưng plugin đã gỡ (dropbox 2026-08-26).
     */
    private suspend fun ensureToc(book: Properties): Boolean {
        if (!book.chapList.isNullOrEmpty()) return true
        val url = book.url ?: return false
        val plugin =
            pluginManager.get(url)
                ?: run {
                    // Sách còn trong Thư viện nhưng plugin đã gỡ — trả false thay vì
                    // throw (startReading không catch → crash app, dropbox 2026-08-26)
                    message = UiText.Raw("Thiếu plugin cho nguồn này — vào tab Nguồn → Phần mở rộng để cài lại", UiText.Tone.ERROR)
                    return false
                }
        // fetchChapterList (network/Rhino) + merge + saveHistory — blocking IO
        return withContext(IoDispatcher) {
            val fetched = Properties().apply { this.url = url }
            fetched.fetchChapterList(plugin, log)
            val fresh = fetched.chapList.orEmpty()
            if (fresh.isEmpty()) {
                false
            } else {
                book.mergeFetchedChapters(fresh)
                saveHistory(book, log)
                true
            }
        }
    }
    fun changeChapter(delta: Int) {
        val book = selected ?: return
        val next = ((editingIndex ?: 0) + delta).coerceIn(0, (book.chapList?.size ?: 1) - 1)
        openChapter(next)
    }
    fun saveChapter() {
        val book = selected ?: return
        val chapter = book.chapList?.getOrNull(editingIndex ?: return) ?: return
        val text = chapterText
        closeChapter() // pop Reader — doOnDestroy nulls editingIndex
        scope.launch {
            withContext(IoDispatcher) {
                text.writeTo(fileIn(book.savePath, "raw/${chapter.id}.txt").toString(), log = log)
            }
            message = UiText.Res(Res.string.lib_chapter_saved, listOf(chapter.chapName ?: ""), UiText.Tone.SUCCESS)
        }
    }

    /** MUST #7: xuất EPUB theo tùy chọn (Calibre/PDF defer — owner chưa cần). */
    fun exportEpub(outputPath: String? = null) = scope.launch {
        val book = selected ?: return@launch
        busy = true
        exportProgress = UiText.Res(Res.string.lib_export_epub_start)
        try {
            // Ebook.export nén zip toàn bộ raw/ — blocking IO
            withContext(IoDispatcher) {
                Ebook(
                    log,
                    book,
                    TypeUtils.EPUB,
                    tool = "Mặc định",
                    compressLevel = exportCompress.toString(),
                    autoSplit = exportSplit,
                    includeImg = exportImages,
                    settings = SettingsRepository.load(),
                    progressListener = ProgressListener { _, s -> exportProgress = UiText.Raw(s) },
                ).export()
                val src = latestExport(book, "epub")
                if (outputPath != null && src != null) {
                    val target = copyTargetFor(src, outputPath)
                    if (target != null) {
                        FileSystem.SYSTEM.copy(src.toPath(), target.toPath())
                        lastExport = target
                        message = UiText.Res(Res.string.lib_export_epub_done, listOf(target), UiText.Tone.SUCCESS)
                        shareExportIfIos(target)
                    } else {
                        // Nơi lưu = chính out/ → epub đã ở đúng chỗ, khỏi copy
                        // (copy(src, src) truncate nguồn → 0 byte — bug 260903)
                        lastExport = src
                        message = UiText.Res(Res.string.lib_export_epub_done, listOf(src), UiText.Tone.SUCCESS)
                        shareExportIfIos(src)
                    }
                } else {
                    lastExport = src
                    message = UiText.Res(Res.string.lib_export_epub_done, listOf(book.savePath ?: ""), UiText.Tone.SUCCESS)
                    shareExportIfIos(src)
                }
            }
        } catch (e: Exception) {
            // Known engine codes → i18n string; unknown → prefix + message (uiTextOf map)
            message =
                when (val ui = uiTextOf(e)) {
                    is UiText.Raw -> UiText.Res(Res.string.lib_export_epub_failed, listOf(ui.text), UiText.Tone.ERROR)
                    else -> ui
                }
        } finally {
            busy = false
            exportProgress = null
        }
    }

    /** CRUD rules lọc rác (từ reader) — thay cả list, persist setting.json. */
    fun updateTrashRules(rules: List<dev.haipham22.leechtext.models.Trash>) {
        scope.launch {
            withContext(IoDispatcher) {
                SettingsRepository.save(SettingsRepository.load().copy(trash = rules))
            }
        }
    }

    /** Lưu tên chương đã sửa (từ ChapterEditDialog) vào properties.json. */
    fun saveChapterNames() {
        val book = selected ?: return
        scope.launch {
            withContext(IoDispatcher) { saveHistory(book, log) }
            message = UiText.Res(Res.string.lib_names_saved, listOf(book.size), UiText.Tone.SUCCESS)
        }
    }

    /** Bookmark chương — file {savePath}/bookmarks.txt, mỗi dòng 1 index. */
    var bookmarks by mutableStateOf<Set<Int>>(emptySet())
        private set
    fun loadBookmarks() {
        val book = selected ?: return
        scope.launch {
            bookmarks =
                withContext(IoDispatcher) {
                    runCatching {
                        fileIn(book.savePath, "bookmarks.txt").readTextOrNull()?.lines()?.mapNotNull { it.trim().toIntOrNull() }?.toSet() ?: emptySet()
                    }.getOrDefault(emptySet())
                }
        }
    }
    fun toggleBookmark(index: Int) {
        val book = selected ?: return
        bookmarks = if (index in bookmarks) bookmarks - index else bookmarks + index
        val updated = bookmarks
        scope.launch {
            withContext(IoDispatcher) {
                runCatching { fs.write(fileIn(book.savePath, "bookmarks.txt")) { writeUtf8(updated.sorted().joinToString("\n")) } }
            }
        }
    }

    /** Xóa sách khỏi thư viện — xóa thư mục savePath + refresh list. */
    fun removeFromLibrary() {
        val book = selected ?: return
        scope.launch {
            try {
                withContext(IoDispatcher) { fs.deleteRecursively((book.savePath ?: "").toPath()) }
                closeBook()
                refresh()
                message = UiText.Res(Res.string.lib_deleted, listOf(book.name ?: ""), UiText.Tone.SUCCESS)
            } catch (e: Exception) {
                message = UiText.Res(Res.string.lib_delete_failed, listOf(e.message ?: e::class.simpleName ?: "error"), UiText.Tone.ERROR)
            }
        }
    }
}
