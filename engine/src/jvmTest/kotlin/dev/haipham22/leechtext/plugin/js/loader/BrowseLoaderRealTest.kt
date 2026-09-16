package dev.haipham22.leechtext.plugin.js.loader

import dev.haipham22.leechtext.log.platformEngineLogger
import dev.haipham22.leechtext.plugin.vbook.PluginZipExtractor
import dev.haipham22.leechtext.plugin.vbook.VBookToLeechTextConverter
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private const val SEARCH_KEYWORD = "tu tien"

/**
 * E2E BrowseLoader với plugin thật:
 * - Bạch Ngọc Sách: home menu tĩnh (site đang chặn trang dữ liệu — các test fetch
 *   chỉ assert KHÔNG crash, ghi chú 2026-08-23 mọi URL trả "Thông báo").
 * - Truyện Full (truyenfull.live): pipeline đầy đủ home → tab → list truyện.
 * Chạy: ./gradlew :engine:jvmTest --tests "*BrowseLoaderReal*"
 */
class BrowseLoaderRealTest {
    private fun convert(zipUrl: String) = VBookToLeechTextConverter().convert(
        PluginZipExtractor(platformEngineLogger()).extractFromZip(zipUrl),
    )

    private val bns = "https://raw.githubusercontent.com/Darkrai9x/vbook-extensions/master/bachngocsach/plugin.zip"
    private val truyenFull = "https://raw.githubusercontent.com/Darkrai9x/vbook-extensions/master/truyenfull/plugin.zip"

    // ── Bạch Ngọc Sách ──

    @Test
    fun bnsHomeMenu() = runBlocking<Unit> {
        val tabs = BrowseLoader.with(convert(bns), platformEngineLogger()).menu("home")
        assertNotNull(tabs, "home script phải trả tabs")
        assertTrue(tabs.isNotEmpty())
        tabs.forEach { println("BNS HOME: ${it.title} → ${it.script}") }
        assertTrue(tabs.all { !it.title.isNullOrBlank() })
    }

    /** Site BNS trả trang "Thông báo" cho mọi URL dữ liệu — loader phải không crash. */
    @Test
    fun bnsBlockedSiteGraceful() = runBlocking<Unit> {
        val loader = BrowseLoader.with(convert(bns), platformEngineLogger())
        val genres = loader.menu("genre")
        val novels = loader.novels("search", SEARCH_KEYWORD, "0")
        println("BNS blocked: genres=${genres?.size ?: "null"}, novels=${novels?.novels?.size ?: "null"}")
        // Không exception = pass — dữ liệu rỗng/null chấp nhận khi site chặn
    }

    // ── Truyện Full: E2E thật ──

    @Test
    fun tfHomeMenu() = runBlocking<Unit> {
        val tabs = BrowseLoader.with(convert(truyenFull), platformEngineLogger()).menu("home")
        assertNotNull(tabs)
        assertTrue(tabs.isNotEmpty())
        tabs.forEach { println("TF HOME: ${it.title} → ${it.script}") }
    }

    @Test
    fun tfTabNovelsFirstPage() = runBlocking<Unit> {
        val loader = BrowseLoader.with(convert(truyenFull), platformEngineLogger())
        val tabs = loader.menu("home") ?: error("home null")
        val first = tabs.first()
        val novels =
            loader.novels(
                scriptName = first.script?.removeSuffix(".js") ?: "gen",
                input = first.input,
                page = "1",
            )
        assertNotNull(novels, "gen script phải trả list truyện")
        assertTrue(novels.novels.isNotEmpty(), "trang 1 phải có truyện")
        novels.novels.take(3).forEach { println("TF NOVEL: ${it.name} — ${it.description}") }
        println("TF next page = ${novels.nextPage}")
        assertNotNull(novels.novels.first().name)
    }

    @Test
    fun tfSearchNovels() = runBlocking<Unit> {
        val result = BrowseLoader.with(convert(truyenFull), platformEngineLogger()).novels("search", SEARCH_KEYWORD, "1")
        assertNotNull(result)
        result.novels.forEach { println("TF SEARCH: ${it.name} — ${it.link}") }
        assertTrue(result.novels.isNotEmpty(), "search 'tu tien' phải có kết quả")
    }

    /** detail.js trả field ongoing (true=Đang ra) — DetailLoader phải extract. */
    @Test
    fun tfDetailOngoing() = runBlocking<Unit> {
        val plugin = convert(truyenFull)
        val search = BrowseLoader.with(plugin, platformEngineLogger()).novels("search", SEARCH_KEYWORD, "1") ?: error("search null")
        val link = search.novels.firstNotNullOfOrNull { it.link } ?: error("không có link")
        val book =
            DetailLoader.with(plugin, platformEngineLogger()).load(
                if (link.startsWith("http")) link else "https://truyenfull.live$link",
            ) ?: error("detail null")
        println("TF DETAIL: ${book.name} ongoing=${book.ongoing}")
        assertNotNull(book.ongoing, "detail.js truyenfull phải trả ongoing")
    }
}
