package dev.haipham22.leechtext.ui.addbook
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.haipham22.leechtext.entities.PluginEntity
import dev.haipham22.leechtext.log.EngineLogger
import dev.haipham22.leechtext.plugin.js.loader.BrowseLoader
import dev.haipham22.leechtext.plugin.js.loader.BrowseNovel
import dev.haipham22.leechtext.resources.Res
import dev.haipham22.leechtext.resources.action_back
import dev.haipham22.leechtext.resources.browse_back_to_menu
import dev.haipham22.leechtext.resources.browse_load_more
import dev.haipham22.leechtext.resources.cd_refresh
import dev.haipham22.leechtext.resources.loading_short
import dev.haipham22.leechtext.resources.tab_sources
import dev.haipham22.leechtext.ui.components.atoms.CoverImage
import dev.haipham22.leechtext.ui.model.asString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import androidx.compose.foundation.lazy.grid.items as gridItems

/**
 * Màn browse 1 nguồn: top bar (back + tên nguồn + refresh), hàng tabs menu
 * (home) horizontal scroll, grid truyện cover 2:3, nút "Tải thêm" khi có next.
 */
@Composable
fun SourceBrowseScreen(
    state: SourceBrowseState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) { if (state.tabs.isEmpty() && state.novels.isEmpty()) state.loadHome() }
    // Back trong browse: đang xem list → về menu; ở menu → đóng nguồn (PluginScreen)
    dev.haipham22.leechtext.ui.PlatformBackHandler(enabled = state.novels.isNotEmpty()) {
        state.backToMenu()
    }
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerLowest)) {
        SourceBrowseTopBar(state, onBack)
        if (state.loading && state.novels.isEmpty()) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        state.message?.let {
            Text(
                it.asString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        // Hàng tabs menu — chỉ hiện khi đang ở mức menu (chưa chọn tab)
        if (state.novels.isEmpty() && state.tabs.isNotEmpty()) {
            SourceBrowseMenu(state)
        }
        // Grid truyện khi đã chọn tab
        if (state.novels.isNotEmpty()) {
            SourceBrowseNovels(state)
        }
    }
}

/** Top bar browse: back + tên/mô tả nguồn + refresh. */
@Composable
private fun SourceBrowseTopBar(
    state: SourceBrowseState,
    onBack: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.action_back))
        }
        Column(Modifier.weight(1f)) {
            Text(
                state.plugin.name ?: stringResource(Res.string.tab_sources),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // Mô tả nguồn — 1 dòng mờ
            state.plugin.describe?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = { state.loadHome() }) {
            Icon(Icons.Filled.Refresh, contentDescription = stringResource(Res.string.cd_refresh))
        }
    }
}

/** Danh sách tabs menu (home) của nguồn — click mở list truyện. */
@Composable
private fun SourceBrowseMenu(state: SourceBrowseState) {
    LazyColumn(Modifier.fillMaxWidth()) {
        items(state.tabs.size) { i ->
            val tab = state.tabs[i]
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { state.openTab(tab) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    tab.title ?: "?",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    // Quy ước mở: sang phải (đi vào list) — trước đây mũi tên trái gây hiểu "đóng"
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .padding(horizontal = 16.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            )
        }
    }
}

/** Grid truyện của tab đang chọn + nút Tải thêm khi còn trang kế. */
@Composable
private fun ColumnScope.SourceBrowseNovels(state: SourceBrowseState) {
    val tabTitle = state.currentTab?.title
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = {
            state.novels = emptyList()
            state.currentTab = null
        }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.browse_back_to_menu), modifier = Modifier.size(18.dp))
        }
        Text(tabTitle ?: "", style = MaterialTheme.typography.titleSmall)
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(120.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding =
        androidx.compose.foundation.layout
            .PaddingValues(16.dp),
        modifier = Modifier.fillMaxWidth().weight(1f),
    ) {
        gridItems(state.novels, key = { it.link ?: it.name ?: it.hashCode().toString() }) { novel ->
            BrowseNovelTile(novel, onClick = { state.openNovel(novel) }, log = state.log)
        }
        if (state.nextPage != null) {
            item(key = "load-more") {
                Box(
                    Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.OutlinedButton(onClick = { state.loadMore() }, enabled = !state.loading) {
                        Text(if (state.loading) stringResource(Res.string.loading_short) else stringResource(Res.string.browse_load_more))
                    }
                }
            }
        }
    }
}

/** Tile truyện browse: cover 2:3 + tên 2 dòng + description 1 dòng mờ. */
@Composable
fun BrowseNovelTile(
    novel: BrowseNovel,
    onClick: () -> Unit,
    log: EngineLogger,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            CoverImage(novel.cover, log = log, modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f))
        }
        Text(
            novel.name ?: "?",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
        novel.description?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Tìm kiếm toàn cầu (Nguồn tab): query → search song song trên mọi plugin
 * đã cài có searchGetter → ghép kết quả group theo nguồn.
 */
@Suppress("LongParameterList") // state Decompose — deps bắt buộc
class GlobalSearchState(
    private val plugins: List<PluginEntity>,
    parentScope: CoroutineScope,
    val log: EngineLogger,
) {
    var results by mutableStateOf<Map<PluginEntity, List<BrowseNovel>>>(emptyMap())
    var searching by mutableStateOf(false)

    /** "N/T nguồn đã trả lời" — feedback trong lúc chờ nguồn chậm */
    var progress by mutableStateOf<String?>(null)

    // Scope cha sở hữu (PluginState.scope) — fix leak scope cũ không bao giờ cancel
    private val scope: CoroutineScope = CoroutineScope(parentScope.coroutineContext)

    @OptIn(ExperimentalAtomicApi::class)
    fun search(query: String) = scope.launch {
        if (query.isBlank()) return@launch
        searching = true
        results = emptyMap()
        val searchable = plugins.filter { !it.searchGetter.isNullOrEmpty() }
        val found = HashMap<PluginEntity, List<BrowseNovel>>()
        val done = kotlin.concurrent.atomics.AtomicInt(0)
        searchable
            .map { plugin ->
                async {
                    val res =
                        runCatching {
                            BrowseLoader.with(plugin, log).novels("search", query, "1")
                        }.getOrNull()
                    // Chỉ giữ link mở được bằng chính plugin nguồn (regex khớp như
                    // PluginManager.get) — search trả lẫn link chương web, click vào
                    // đó preview báo "Thiếu plugin" (dogfood 260902)
                    res?.novels?.filter { it.openableBy(plugin) }?.takeIf { it.isNotEmpty() }?.let { found[plugin] = it }
                    // Stream từng nguồn xong — không đợi cả bể mới có gì hiển thị
                    results = found.toMap()
                    progress = "${done.incrementAndFetch()}/${searchable.size}"
                }
            }.awaitAll()
        progress = null
        searching = false
    }

    /** Link đầy đủ (prepend host khi tương đối — cùng rule lúc click) khớp regex plugin. */
    private fun BrowseNovel.openableBy(plugin: PluginEntity): Boolean {
        val pattern = plugin.regex ?: return true
        val link = link ?: return false
        val full = if (link.startsWith("http")) link else (host ?: "") + link
        return runCatching { Regex("(https?://)?$pattern").matches(full) }.getOrDefault(false)
    }
}
