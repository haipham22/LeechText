package dev.haipham22.leechtext.ui.sources

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.haipham22.leechtext.entities.PluginEntity
import dev.haipham22.leechtext.resources.Res
import dev.haipham22.leechtext.resources.action_search
import dev.haipham22.leechtext.resources.cd_back
import dev.haipham22.leechtext.resources.search_no_results
import dev.haipham22.leechtext.resources.search_page_title
import dev.haipham22.leechtext.resources.search_results_count
import dev.haipham22.leechtext.resources.sources_global_search_hint
import dev.haipham22.leechtext.ui.addbook.BrowseNovelTile
import dev.haipham22.leechtext.ui.addbook.GlobalSearchState
import dev.haipham22.leechtext.ui.theme.MonoFont
import org.jetbrains.compose.resources.stringResource

/**
 * Trang kết quả tìm kiếm toàn cục (owner 2026-08-26): search mở trang riêng như khi
 * duyệt truyện, kết quả chia block theo nguồn — collapse từng block được.
 */
@Composable
fun SearchResultsPage(
    query: String,
    onQuery: (String) -> Unit,
    searchState: GlobalSearchState,
    onOpenNovel: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Block nào đang thu gọn (default mở tất cả)
    var collapsed by remember { mutableStateOf(setOf<String>()) }

    Column(modifier.fillMaxSize().padding(16.dp)) {
        // Top bar: back + title
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.cd_back))
            }
            Text(
                stringResource(Res.string.search_page_title),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        // Ô search — submit lại ngay trên trang
        SearchResultsField(query, onQuery, searchState)

        val entries = searchState.results.entries.toList()
        if (!searchState.searching && entries.isEmpty()) {
            Text(
                stringResource(Res.string.search_no_results),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 32.dp),
            )
        }
        // Card layout như browse (BrowseNovelTile): grid cover 2:3 + tên,
        // header nguồn full-span (owner 2026-08-27 "layout card như discovery")
        SearchResultsGrid(
            searchState = searchState,
            collapsed = collapsed,
            onToggleCollapse = { collapsed = it },
            onOpenNovel = onOpenNovel,
        )
    }
}

/** Field search + progress row khi đang query. */
@Composable
@Suppress("MultipleEmitters") // helper emit nhiều node có chủ đích — caller đặt trong scope Column/Row
private fun SearchResultsField(
    query: String,
    onQuery: (String) -> Unit,
    searchState: GlobalSearchState,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQuery,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        placeholder = {
            Text(
                stringResource(Res.string.sources_global_search_hint),
                fontFamily = MonoFont(),
                style = MaterialTheme.typography.labelSmall,
            )
        },
        textStyle = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFont()),
        singleLine = true,
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { searchState.search(query) }, enabled = !searchState.searching) {
                    Icon(Icons.Filled.Search, contentDescription = stringResource(Res.string.action_search))
                }
            }
        },
    )
    if (searchState.searching) {
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LinearProgressIndicator(Modifier.weight(1f))
            searchState.progress?.let {
                Text(
                    "  $it",
                    fontFamily = MonoFont(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Grid kết quả chia block theo nguồn — collapse từng block được. */
@Composable
private fun SearchResultsGrid(
    searchState: GlobalSearchState,
    collapsed: Set<String>,
    onToggleCollapse: (Set<String>) -> Unit,
    onOpenNovel: (String) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(120.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        searchState.results.entries.forEach { (plugin, novels) ->
            val key = plugin.uuid ?: plugin.name ?: plugin.hashCode().toString()
            val isCollapsed = key in collapsed
            item(key = "h-$key", span = { GridItemSpan(maxLineSpan) }) {
                SourceResultHeader(plugin, novels.size, isCollapsed) {
                    onToggleCollapse(if (isCollapsed) collapsed - key else collapsed + key)
                }
            }
            if (!isCollapsed) {
                items(count = novels.size, key = { i: Int -> "n-$key-$i" }) { i ->
                    val novel = novels[i]
                    BrowseNovelTile(
                        novel,
                        onClick = {
                            val link = novel.link ?: return@BrowseNovelTile
                            onOpenNovel(if (link.startsWith("http")) link else (novel.host ?: "") + link)
                        },
                        log = searchState.log,
                    )
                }
            }
        }
    }
}

/** Header 1 block nguồn: tên mono + số kết quả, click thu gọn/mở. */
@Composable
private fun SourceResultHeader(
    plugin: PluginEntity,
    count: Int,
    collapsed: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            plugin.name ?: "?",
            fontFamily = MonoFont(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            stringResource(Res.string.search_results_count, count),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        Text(if (collapsed) "▸" else "▾", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
