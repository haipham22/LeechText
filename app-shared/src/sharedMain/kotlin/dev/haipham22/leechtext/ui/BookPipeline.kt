package dev.haipham22.leechtext.ui

import dev.haipham22.leechtext.action.saveHistory
import dev.haipham22.leechtext.get.DownloadSummary
import dev.haipham22.leechtext.get.downloadChapters
import dev.haipham22.leechtext.get.fetchChapterList
import dev.haipham22.leechtext.get.fetchInfo
import dev.haipham22.leechtext.get.normalizeUrl
import dev.haipham22.leechtext.log.EngineLogger
import dev.haipham22.leechtext.models.Chapter
import dev.haipham22.leechtext.models.Properties
import dev.haipham22.leechtext.plugin.PluginManager
import dev.haipham22.leechtext.plugin.vbook.VBookPluginService
import dev.haipham22.leechtext.resources.Res
import dev.haipham22.leechtext.resources.err_chapters_fetch_failed
import dev.haipham22.leechtext.resources.err_migrate_no_chapters
import dev.haipham22.leechtext.resources.err_plugin_no_match
import dev.haipham22.leechtext.resources.err_range_invalid
import dev.haipham22.leechtext.resources.err_range_no_pick
import dev.haipham22.leechtext.resources.err_range_overflow
import dev.haipham22.leechtext.ui.model.UiException
import dev.haipham22.leechtext.ui.model.UiText
import dev.haipham22.leechtext.util.EnginePaths
import dev.haipham22.leechtext.util.SettingsRepository
import dev.haipham22.leechtext.util.removeDiacritics
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

/**
 * Pipeline chung: URL → plugin (auto-cài nếu thiếu) → info + TOC → chọn range →
 * download song song → save properties.json. AddBook và queue đa sách cùng chạy qua đây.
 */
object BookPipeline {
    /**
     * Bước 1+2 gộp. rangeText null/blank = tải toàn bộ; ngược lại parse sau khi lấy
     * TOC → chỉ TẢI subset (dogfood 260902: properties.json luôn giữ FULL TOC —
     * detail/Chapters/TOC reader/badge đếm trên danh sách đầy đủ, trạng thái đã tải
     * = file raw/<id>.txt). Báo tiến độ qua onProgress.
     */
    suspend fun downloadBook(
        rawUrl: String,
        rangeText: String? = null,
        onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> },
        onInfo: (Properties) -> Unit = {},
        log: EngineLogger,
        pluginManager: PluginManager,
    ): Pair<Properties, DownloadSummary> {
        val url = rawUrl.normalizeUrl()
        val plugin =
            pluginManager.get(url)
                ?: VBookPluginService(log, pluginManager = pluginManager).findAndInstallByUrl(url)
                ?: throw UiException(UiText.Res(Res.string.err_plugin_no_match, listOf(url), UiText.Tone.ERROR))

        val p = Properties().apply { this.url = url }
        p.fetchInfo(plugin, log)
        onInfo(p)
        p.fetchChapterList(plugin, log)
        if (p.chapList.isNullOrEmpty()) {
            throw UiException(UiText.Res(Res.string.err_chapters_fetch_failed, listOf(url), UiText.Tone.ERROR))
        }

        // Range chỉ chọn chương cần tải — chapList giữ FULL TOC khi saveHistory
        val selected = rangeText?.takeIf { it.isNotBlank() }?.let { parseRange(it, p.chapList!!) }

        val settings = SettingsRepository.load()
        val savePath = savePathFor(url, p.name, settings).toString()
        p.savePath = savePath
        fs.createDirectories(savePath.toPath() / "raw")

        val summary =
            p.downloadChapters(settings = settings, onProgress = onProgress, chapters = selected, log = log, pluginManager = pluginManager)
        saveHistory(p, log)
        return p to summary
    }

    /**
     * Thêm nhanh vào thư viện: chỉ fetch info + lưu properties.json. KHÔNG tải TOC,
     * KHÔNG tải chương — TOC lấy lazy khi mở sách (LibraryState.loadToc), chương tải
     * khi bấm Tải tất cả. Đã có sách cùng URL → trả bản cũ, không ghi đè.
     */
    suspend fun addToLibrary(
        rawUrl: String,
        log: EngineLogger,
        pluginManager: PluginManager,
    ): Properties {
        val url = rawUrl.normalizeUrl()
        val plugin =
            pluginManager.get(url)
                ?: VBookPluginService(log, pluginManager = pluginManager).findAndInstallByUrl(url)
                ?: throw UiException(UiText.Res(Res.string.err_plugin_no_match, listOf(url), UiText.Tone.ERROR))

        val p = Properties().apply { this.url = url }
        p.fetchInfo(plugin, log)

        val settings = SettingsRepository.load()
        val dir = savePathFor(url, p.name, settings)
        val existing =
            dev.haipham22.leechtext.action.loadHistory(
                (dir / "properties.json").toString(),
                log,
            )
        if (existing != null && existing.url == url) return existing

        p.savePath = dir.toString()
        fs.createDirectories(dir / "raw")
        saveHistory(p, log)
        return p
    }

    /**
     * Thư mục lưu sách: đã có sách cùng URL trong output/ → dùng lại folder đó
     * (identity = URL, chống 2 bản khi tên detail đổi), không thì slug theo tên.
     */
    fun savePathFor(
        url: String?,
        name: String?,
        settings: dev.haipham22.leechtext.util.AppSettings,
    ): Path {
        val workPath = settings.workPath.ifEmpty { EnginePaths.dataDir.toString() }
        val output = workPath.toPath() / "output"
        if (!url.isNullOrEmpty()) {
            fs.listOrNull(output)?.forEach { dir ->
                if (fs.metadataOrNull(dir)?.isDirectory != true) return@forEach
                val existing =
                    dev.haipham22.leechtext.action.loadHistory(
                        (dir / "properties.json").toString(),
                        // savePathFor giữ signature (test gọi trực tiếp, không log) — logger platform
                        dev.haipham22.leechtext.log.platformEngineLogger(),
                    )
                if (existing?.url == url) return dir
            }
        }
        val slug =
            removeDiacritics(name ?: "sach")
                .replace(Regex("[^a-zA-Z0-9_]"), "_")
                .replace("\"", "")
                .trim()
        return output / slug
    }

    /** Parse "1-5,7,9" (1-based inclusive) — port AddDialog.parseListChap. */
    @Suppress("ThrowsCount") // validation chain ở trust boundary — mỗi guard ném exception có message riêng cho UI
    fun parseRange(
        text: String,
        chapters: List<Chapter>,
    ): List<Chapter> {
        val picked = ArrayList<Chapter>()
        try {
            for (token in text.replace(Regex("\\s+"), "").split(",")) {
                if (token.isEmpty()) continue
                val parts = token.split("-")
                val from = parts[0].toInt()
                val to = if (parts.size == 1) from else parts[1].toInt()
                if (from !in 1..chapters.size || to !in from..chapters.size) {
                    throw UiException(UiText.Res(Res.string.err_range_overflow, listOf(chapters.size), UiText.Tone.ERROR))
                }
                for (i in from..to) picked.add(chapters[i - 1])
            }
        } catch (e: NumberFormatException) {
            throw UiException(UiText.Res(Res.string.err_range_invalid, listOf(text), UiText.Tone.ERROR))
        }
        if (picked.isEmpty()) {
            throw UiException(UiText.Res(Res.string.err_range_no_pick, tone = UiText.Tone.ERROR))
        }
        return picked
    }

    /** Kết quả đổi nguồn: kept = số chương khớp giữ được file raw. */
    data class MigrateResult(
        val kept: Int,
        val total: Int,
    )

    /**
     * Đổi nguồn truyện (owner 2026-08-26): fetch TOC từ URL mới qua plugin mới,
     * khớp chương theo tên chuẩn hoá để giữ file raw đã tải, còn lại tải lại sau
     * bằng nút Tải chương. Folder sách / tên / introduce giữ nguyên.
     */
    @Suppress("LongParameterList") // book + URL + log DI + pluginManager — pipeline đủ ngữ cảnh
    suspend fun migrateSource(
        book: Properties,
        rawUrl: String,
        log: EngineLogger,
        pluginManager: PluginManager,
    ): MigrateResult {
        val url = rawUrl.normalizeUrl()
        val plugin =
            pluginManager.get(url)
                ?: VBookPluginService(log, pluginManager = pluginManager).findAndInstallByUrl(url)
                ?: throw UiException(UiText.Res(Res.string.err_plugin_no_match, listOf(url), UiText.Tone.ERROR))

        val fresh =
            Properties().apply {
                this.url = url
            }
        fresh.fetchInfo(plugin, log)
        fresh.fetchChapterList(plugin, log)
        val newChapters = fresh.chapList.orEmpty()
        if (newChapters.isEmpty()) {
            throw UiException(UiText.Res(Res.string.err_migrate_no_chapters, tone = UiText.Tone.ERROR))
        }

        val kept = remapChapterFiles((book.savePath ?: "").toPath() / "raw", book.chapList.orEmpty(), newChapters)

        book.url = url
        fresh.cover?.let { book.cover = it }
        fresh.ongoing?.let { book.ongoing = it }
        book.chapList = newChapters
        saveHistory(book, log)
        return MigrateResult(kept, newChapters.size)
    }
}

/**
 * Chuẩn hoá tên chương để khớp cross-site: lowercase, bỏ hết khoảng trắng + dấu câu
 * ("Chương 1: Tô Minh" == "Chương 1 - Tô Minh"). Giữ nguyên diacritics — cùng ngôn ngữ.
 */
fun normalizeChapterName(name: String): String = name.lowercase().replace(Regex("[\\s\\p{Punct}]+"), "")

/**
 * Chuyển file raw chương từ id nguồn cũ sang id nguồn mới theo tên khớp.
 * Id mới dùng cùng không gian tên C<n> với id cũ nên phải dời tạm TOÀN BỘ file cũ
 * ra tên tmp trước, rồi copy khớp về id mới, cuối cùng dọn tmp — không làm vậy
 * copy sẽ đè nhầm file cũ trùng số.
 *
 * @return số chương giữ được nội dung
 */
fun remapChapterFiles(
    rawDir: Path,
    oldChapters: List<Chapter>,
    newChapters: List<Chapter>,
): Int {
    if (!fs.exists(rawDir)) return 0
    // B1: dời tạm toàn bộ file cũ, dựng map tên chuẩn → tmp file
    val tmpByName = HashMap<String, Path>()
    oldChapters.forEach { ch ->
        val f = rawDir / "${ch.id}.txt"
        if (fs.exists(f)) {
            val tmp = rawDir / "tmp_${ch.id}.txt"
            if (runCatching { fs.atomicMove(f, tmp) }.isSuccess) {
                tmpByName[normalizeChapterName(ch.chapName ?: "")] = tmp
            }
        }
    }
    // B2: copy khớp về id mới
    var kept = 0
    newChapters.forEach { ch ->
        val src = tmpByName[normalizeChapterName(ch.chapName ?: "")] ?: return@forEach
        val dst = rawDir / "${ch.id}.txt"
        if (!fs.exists(dst)) {
            fs.copy(src, dst)
            kept++
        }
    }
    // B3: dọn tmp
    tmpByName.values.forEach { runCatching { fs.delete(it) } }
    return kept
}

private val fs: FileSystem get() = FileSystem.SYSTEM
