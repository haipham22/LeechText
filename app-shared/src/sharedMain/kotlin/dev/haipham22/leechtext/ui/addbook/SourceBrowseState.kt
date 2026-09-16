package dev.haipham22.leechtext.ui.addbook
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import dev.haipham22.leechtext.entities.PluginEntity
import dev.haipham22.leechtext.log.EngineLogger
import dev.haipham22.leechtext.plugin.js.loader.BrowseLoader
import dev.haipham22.leechtext.plugin.js.loader.BrowseNovel
import dev.haipham22.leechtext.plugin.js.loader.BrowseTab
import dev.haipham22.leechtext.resources.Res
import dev.haipham22.leechtext.resources.browse_no_catalog
import dev.haipham22.leechtext.resources.browse_no_novels
import dev.haipham22.leechtext.ui.model.UiText
import kotlinx.coroutines.launch

/**
 * State + logic browse 1 nguồn (task #12): home/genre menu → novels grid →
 * pagination. Click truyện → onOpenNovel(link) để AddBook flow enqueue URL.
 */
@Suppress("LongParameterList") // signature giữ nguyên từ khi class nằm trong SourceBrowseScreen.kt
class SourceBrowseState(
    componentContext: ComponentContext,
    val plugin: PluginEntity,
    val log: EngineLogger,
    private val onOpenNovel: (String) -> Unit,
    private val onBroken: () -> Unit = {},
) : ComponentContext by componentContext {
    var tabs by mutableStateOf<List<BrowseTab>>(emptyList())
    var novels by mutableStateOf<List<BrowseNovel>>(emptyList())
    var nextPage by mutableStateOf<String?>(null)
    var currentTab by mutableStateOf<BrowseTab?>(null)
    var loading by mutableStateOf(false)
    var message by mutableStateOf<UiText?>(null)

    // Scope lifecycle-aware (Essenty) — fix leak scope cũ không bao giờ cancel
    private val scope = coroutineScope()
    private val loader = BrowseLoader.with(plugin, log)

    /** Load home menu của nguồn. */
    fun loadHome() = scope.launch {
        loading = true
        message = null
        val result = runCatching { loader.menu("home") }.getOrNull()
        tabs = result.orEmpty()
        if (tabs.isEmpty()) {
            message = UiText.Res(Res.string.browse_no_catalog, tone = UiText.Tone.ERROR)
            onBroken() // nguồn hỏng — báo caller ẩn khỏi danh sách
        }
        loading = false
    }

    /** Chọn 1 tab menu → load list truyện trang 1. */
    fun openTab(tab: BrowseTab) = scope.launch {
        currentTab = tab
        loading = true
        message = null
        novels = emptyList()
        val script = tab.script?.removeSuffix(".js") ?: return@launch
        val result = runCatching { loader.novels(script, tab.input, "1") }.getOrNull()
        novels = result?.novels.orEmpty()
        nextPage = result?.nextPage
        if (novels.isEmpty()) message = UiText.Res(Res.string.browse_no_novels, tone = UiText.Tone.ERROR)
        loading = false
    }

    /** Load trang kế tiếp (append). */
    fun loadMore() = scope.launch {
        val page = nextPage ?: return@launch
        val tab = currentTab ?: return@launch
        loading = true
        val script = tab.script?.removeSuffix(".js") ?: return@launch
        val result = runCatching { loader.novels(script, tab.input, page) }.getOrNull()
        novels = novels + (result?.novels.orEmpty())
        nextPage = result?.nextPage
        loading = false
    }
    fun openNovel(novel: BrowseNovel) {
        val link = novel.link ?: return
        onOpenNovel(if (link.startsWith("http")) link else (novel.host ?: "") + link)
    }

    /** Back 1 tầng: đang xem list truyện → về menu chủ đề. */
    fun backToMenu() {
        novels = emptyList()
        nextPage = null
        currentTab = null
    }
}
