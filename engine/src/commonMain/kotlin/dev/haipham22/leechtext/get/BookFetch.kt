package dev.haipham22.leechtext.get

import dev.haipham22.leechtext.entities.ChapterEntity
import dev.haipham22.leechtext.entities.PluginEntity
import dev.haipham22.leechtext.log.EngineLogger
import dev.haipham22.leechtext.models.Chapter
import dev.haipham22.leechtext.models.Properties
import dev.haipham22.leechtext.plugin.js.loader.DetailLoader
import dev.haipham22.leechtext.plugin.js.loader.ListLoader
import dev.haipham22.leechtext.plugin.js.loader.PageLoader
import dev.haipham22.leechtext.util.SyntaxUtils
import dev.haipham22.leechtext.util.nfc

/**
 * Fetch logic book info + chapter list (port từ get/InfoExecute.java + get/ListExecute.java).
 * SwingWorker wrapper bỏ — loaders là logic thật, chạy suspend trên EngineDispatchers (bên
 * trong loader). Listener ChangeListener bỏ — caller tự observe Properties.
 */

/**
 * Chuẩn hoá URL sách: trim + bỏ slash cuối. Plugin (vd truyenfull) parse URL theo segment —
 * slash thừa khiến parse host/port ra rác ("Invalid URL port: NOT_FOUND").
 */
fun String?.normalizeUrl(): String = this?.trim()?.trimEnd('/') ?: ""

/** URL HTTP(S) hợp lệ — dùng lọc URL từ plugin response (UniqueTag stringify ra rác). */
fun String?.isValidHttpUrl(): Boolean = this != null && (startsWith("http://") || startsWith("https://"))

/**
 * Load book info qua DetailLoader, apply vào properties (thay InfoExecute.doInBackground +
 * done). Wrap introduce trong thẻ <p> khi thành công — như bản gốc.
 */
suspend fun Properties.fetchInfo(
    plugin: PluginEntity,
    log: EngineLogger,
): Properties {
    var success = false
    try {
        val book = DetailLoader.with(plugin, log).load(url.normalizeUrl())

        if (book != null) {
            log.debug("[fetchInfo] $url → name=${book.name} ongoing=${book.ongoing}")
            author = book.author
            introduce = book.introduce
            // Chỉ ghi đè URL nếu response trả URL thật — đa số plugin không có field url
            if (book.url.isValidHttpUrl()) url = book.url
            cover = book.cover
            name = book.name
            ongoing = book.ongoing
        }
        success = true
    } catch (e: Exception) {
        log.add("InfoExecute error: ${e.message}")
    }

    // done(): format giới thiệu sau khi fetch thành công
    if (success && introduce != null) {
        introduce =
            "<p>" + SyntaxUtils.optimize(introduce)!!.replace("\n", "</p>\n<p>") + "</p>"
    }
    return this
}

/**
 * Load chapter list qua PageLoader (page discovery) + ListLoader (thay
 * ListExecute.doInBackground + done). Gán id C0..Cn-1 cho mỗi chapter — như bản gốc.
 */
suspend fun Properties.fetchChapterList(
    plugin: PluginEntity,
    log: EngineLogger,
): Properties {
    var success = false
    val targetUrl = url.normalizeUrl()

    try {
        val loader = ListLoader.with(plugin, log)
        val pageLoader = PageLoader.with(plugin, log)

        // Try page discovery trước (page.js pattern)
        val pageUrls = pageLoader.load(targetUrl)

        val chapters =
            if (pageUrls.isNotEmpty()) {
                // Page discovery thành công — load từng page URL
                log.add("[ListExecute] Discovered ${pageUrls.size} page URLs")
                collectPageChapters(loader, pageUrls, log)
            } else {
                // Không có page discovery — load trực tiếp (legacy toc.js pattern)
                log.add("[ListExecute] No page URLs discovered, trying direct list loading")
                collectDirectChapters(loader, targetUrl)
            }

        chapList = chapters
        size = chapters.size
        success = true
        log.debug("[fetchChapterList] $targetUrl → ${chapters.size} chương (${pageUrls.size} pages)")
    } catch (e: Exception) {
        log.add(e)
    }

    // done(): gán id sau khi fetch thành công
    if (success) {
        chapList?.forEachIndexed { i, chapter -> chapter.setId(i) }
    }
    return this
}

/** Load chương từ từng page URL (page discovery) — bỏ qua page lỗi, lọc chương thiếu name/url. */
private suspend fun collectPageChapters(
    loader: ListLoader,
    pageUrls: List<String>,
    log: EngineLogger,
): ArrayList<Chapter> {
    val chapters = ArrayList<Chapter>()
    for (pageUrl in pageUrls) {
        try {
            val chapterList = loader.load(pageUrl) ?: continue
            chapters.addAll(validChapters(chapterList))
        } catch (e: Exception) {
            log.add("[ListExecute] Failed to load page $pageUrl: ${e.message}")
        }
    }
    return chapters
}

/** Chỉ giữ chương có đủ name + url. */
private fun validChapters(chapterList: List<ChapterEntity>): List<Chapter> = chapterList.mapNotNull { chap ->
    val chapName = chap.name
    if (chapName != null && chap.url != null) Chapter(url = chap.url, chapName = chapName.nfc()) else null
}

/** Load chương trực tiếp từ URL (legacy toc.js pattern) — không lọc name/url null. */
private suspend fun collectDirectChapters(
    loader: ListLoader,
    targetUrl: String,
): List<Chapter> = loader.load(targetUrl).orEmpty().map {
    Chapter(url = it.url, chapName = it.name?.nfc())
}

/**
 * Gộp TOC vừa fetch vào sách đang có (MUST #5 — truyện đang ra, re-open tải chương mới).
 * Chương cũ khớp URL giữ nguyên id → file raw/<id>.txt cũ khớp, resume không tải lại.
 * Chương mới (URL chưa có) nhận id tiếp theo theo số thứ tự. Trả về số chương mới.
 *
 * ponytail: không theo dõi chương user từng bỏ chọn khi Add — nếu sách được thêm bằng
 * range con, chương chưa chọn trong TOC sẽ bị coi là mới khi re-open. Sửa khi cần
 * lưu selected-range trong properties.json.
 */
fun Properties.mergeFetchedChapters(fetched: List<Chapter>): Int {
    val existingUrls = HashSet<String>()
    chapList?.forEach { ch -> ch.url?.let { existingUrls.add(it) } }

    // Id tiếp theo = max id hiện có + 1 (sách thêm bằng range con có id nhảy C9,C20 —
    // nếu tính từ size sẽ đụng id cũ)
    var nextIndex =
        chapList
            ?.mapNotNull { it.id?.removePrefix("C")?.toIntOrNull() }
            ?.maxOrNull()
            ?.plus(1) ?: 0
    val merged = ArrayList<Chapter>(chapList.orEmpty())
    var newCount = 0
    for (ch in fetched) {
        val url = ch.url ?: continue
        if (existingUrls.contains(url)) continue
        val copy =
            Chapter(
                url = url,
                partName = ch.partName,
                chapName = ch.chapName,
            )
        copy.setId(nextIndex++)
        merged.add(copy)
        newCount++
    }

    chapList = merged
    size = merged.size
    return newCount
}
