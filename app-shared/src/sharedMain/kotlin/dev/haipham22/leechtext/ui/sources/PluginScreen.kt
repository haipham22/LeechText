package dev.haipham22.leechtext.ui.sources

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import dev.haipham22.leechtext.entities.PluginEntity
import dev.haipham22.leechtext.models.Properties
import dev.haipham22.leechtext.plugin.vbook.model.VBookExtensionEntity
import dev.haipham22.leechtext.resources.Res
import dev.haipham22.leechtext.resources.action_add
import dev.haipham22.leechtext.resources.action_back
import dev.haipham22.leechtext.resources.action_cancel
import dev.haipham22.leechtext.resources.action_close
import dev.haipham22.leechtext.resources.action_delete
import dev.haipham22.leechtext.resources.action_save
import dev.haipham22.leechtext.resources.action_search
import dev.haipham22.leechtext.resources.addbook_add_to_library
import dev.haipham22.leechtext.resources.addbook_chapter_empty
import dev.haipham22.leechtext.resources.addbook_download_full
import dev.haipham22.leechtext.resources.addbook_in_library
import dev.haipham22.leechtext.resources.addbook_loading_info
import dev.haipham22.leechtext.resources.cat_audio
import dev.haipham22.leechtext.resources.cat_other
import dev.haipham22.leechtext.resources.cd_add_from_url
import dev.haipham22.leechtext.resources.cd_add_repo
import dev.haipham22.leechtext.resources.cd_global_search
import dev.haipham22.leechtext.resources.cd_next_chapter
import dev.haipham22.leechtext.resources.cd_prev_chapter
import dev.haipham22.leechtext.resources.cd_refresh
import dev.haipham22.leechtext.resources.error_prefix
import dev.haipham22.leechtext.resources.lang_name_en
import dev.haipham22.leechtext.resources.lang_name_vi
import dev.haipham22.leechtext.resources.lang_name_zh
import dev.haipham22.leechtext.resources.plugins_update_all
import dev.haipham22.leechtext.resources.sources_add_url_hint
import dev.haipham22.leechtext.resources.sources_broken_section
import dev.haipham22.leechtext.resources.sources_cfg_default
import dev.haipham22.leechtext.resources.sources_cfg_delay
import dev.haipham22.leechtext.resources.sources_cfg_threads
import dev.haipham22.leechtext.resources.sources_cfg_timeout
import dev.haipham22.leechtext.resources.sources_config
import dev.haipham22.leechtext.resources.sources_config_title
import dev.haipham22.leechtext.resources.sources_delete_confirm
import dev.haipham22.leechtext.resources.sources_delete_repo
import dev.haipham22.leechtext.resources.sources_delete_source
import dev.haipham22.leechtext.resources.sources_delete_title
import dev.haipham22.leechtext.resources.sources_domain_hint
import dev.haipham22.leechtext.resources.sources_domain_label
import dev.haipham22.leechtext.resources.sources_empty_installed
import dev.haipham22.leechtext.resources.sources_extensions_tab
import dev.haipham22.leechtext.resources.sources_global_search_hint
import dev.haipham22.leechtext.resources.sources_install
import dev.haipham22.leechtext.resources.sources_installed_chip
import dev.haipham22.leechtext.resources.sources_maybe_dead
import dev.haipham22.leechtext.resources.sources_newer_version
import dev.haipham22.leechtext.resources.sources_no_match
import dev.haipham22.leechtext.resources.sources_pin
import dev.haipham22.leechtext.resources.sources_repo_url_label
import dev.haipham22.leechtext.resources.sources_retry
import dev.haipham22.leechtext.resources.sources_search_ext_hint
import dev.haipham22.leechtext.resources.sources_section_repo
import dev.haipham22.leechtext.resources.sources_section_upgrade
import dev.haipham22.leechtext.resources.sources_test_site
import dev.haipham22.leechtext.resources.sources_unpin
import dev.haipham22.leechtext.resources.sources_upgrade
import dev.haipham22.leechtext.resources.tab_sources
import dev.haipham22.leechtext.ui.addbook.AddBookState
import dev.haipham22.leechtext.ui.addbook.ChapterPickDialog
import dev.haipham22.leechtext.ui.addbook.GlobalSearchState
import dev.haipham22.leechtext.ui.addbook.SourceBrowseScreen
import dev.haipham22.leechtext.ui.addbook.SourceBrowseState
import dev.haipham22.leechtext.ui.addbook.compressToRange
import dev.haipham22.leechtext.ui.components.atoms.CoverImage
import dev.haipham22.leechtext.ui.components.organisms.BookDetailBody
import dev.haipham22.leechtext.ui.components.organisms.BookDetailBodyOptions
import dev.haipham22.leechtext.ui.components.organisms.BookDetailBodySlots
import dev.haipham22.leechtext.ui.components.organisms.InlineChapterToc
import dev.haipham22.leechtext.ui.model.UiText
import dev.haipham22.leechtext.ui.model.asString
import dev.haipham22.leechtext.ui.model.tone
import dev.haipham22.leechtext.ui.openUrl
import dev.haipham22.leechtext.ui.queue.DownloadQueueState
import dev.haipham22.leechtext.ui.reader.toParagraphs
import dev.haipham22.leechtext.ui.theme.MonoFont
import dev.haipham22.leechtext.util.SettingsRepository
import dev.haipham22.leechtext.util.removeDiacritics
import org.jetbrains.compose.resources.stringResource

/** Màn plugin — search + group Đã cài / Kho vBook collapse được. */
@Composable
fun PluginScreen(
    modifier: Modifier = Modifier,
    state: PluginState,
    queueState: DownloadQueueState? = null,
) {
    // LaunchedEffect guard — chống double-load khi composition chạy 2 lần (branch switch)
    androidx.compose.runtime.LaunchedEffect(Unit) {
        // Luôn gọi — guard fetchedOnce bên trong lo phần dedup (cache extensions
        // khôngempty thì guard cũ skip hẳn → installed không load → trắng)
        state.initialLoad()
    }

    var sourceTab by remember { mutableIntStateOf(0) }
    // Collapse từng category kho plugin — default chỉ mở "Tiếng Việt"
    var categoryExpanded by remember { mutableStateOf(mapOf<String, Boolean>()) }
    var showSearch by remember { mutableStateOf(false) }
    var globalSearch by remember { mutableStateOf("") }
    // Thêm repo mở dialog ngay (dogfood 260902: trước đây chỉ nhảy tab — form nằm đáy
    // list 175 extension, user không tìm thấy)
    var showRepoDialog by remember { mutableStateOf(false) }
    // Thêm sách bằng URL — dán link, engine auto-dò plugin (README promise; mất trong refactor)
    var showUrlDialog by remember { mutableStateOf(false) }

    // Render theo stack — không animation (như when cũ), tránh detach race offscreen
    val stack by state.stack.subscribeAsState()
    // Back hệ thống: stack có tầng con → pop (không thoát app); List = base
    dev.haipham22.leechtext.ui.PlatformBackHandler(enabled = stack.active.configuration !is SourceConfig.List) {
        state.closeTop()
    }

    when (val config = stack.active.configuration) {
        is SourceConfig.Preview -> {
            // Instance AddBookState sống theo child — pop là destroy (tốt hơn remember(url))
            val previewState =
                remember(config.url) {
                    AddBookState(stack.active.instance.context, state.log, state.pluginManager).apply { this.url = config.url }
                }
            androidx.compose.runtime.LaunchedEffect(config.url) {
                previewState.loadInfo(loadChapters = false, autoInstall = false)
            }
            NovelPreviewScreen(previewState, config.url, queueState, onBack = { state.closeTop() })
        }

        is SourceConfig.Browse -> {
            val plugin = state.installed.firstOrNull { it.uuid == config.pluginUuid }
            if (plugin == null) {
                state.closeTop()
            } else {
                SourceBrowseScreen(
                    state =
                    remember(config.pluginUuid) {
                        SourceBrowseState(
                            stack.active.instance.context,
                            plugin,
                            state.log,
                            onOpenNovel = { state.openPreview(it) },
                            onBroken = { plugin.name?.let { state.markBroken(it) } },
                        )
                    },
                    onBack = { state.closeTop() },
                )
            }
        }

        is SourceConfig.Search -> {
            SearchResultsPage(
                query = config.query,
                onQuery = { globalSearch = it },
                searchState =
                state.globalSearch
                    ?: GlobalSearchState(state.installed, state.scope, state.log).also { state.globalSearch = it },
                onOpenNovel = { state.openPreview(it) },
                onBack = { state.closeTop() },
            )
        }

        SourceConfig.List -> {
            Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                // Top bar: title trái lớn + actions phải — cố định, không nhảy khi đổi tab
                PluginTopBar(
                    state = state,
                    onAddUrl = { showUrlDialog = true },
                    onAdd = { showRepoDialog = true },
                    onSearch = {
                        showSearch = !showSearch
                    },
                )
                PluginSearchSection(
                    sourceTab = sourceTab,
                    visible = showSearch,
                    globalSearch = globalSearch,
                    onGlobalSearch = { globalSearch = it },
                    searchState =
                    state.globalSearch
                        ?: GlobalSearchState(state.installed, state.scope, state.log).also { state.globalSearch = it },
                    onOpenResults = {
                        // Tạo search state scoped theo state (scope cha — không leak)
                        state.globalSearch = state.globalSearch ?: GlobalSearchState(state.installed, state.scope, state.log)
                        state.openSearch(globalSearch)
                    },
                    state = state,
                )

                // Tab row kiểu iOS: text + gạch chân indicator, KHÔNG nền container
                PrimaryTabRow(
                    selectedTabIndex = sourceTab,
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    divider = {},
                ) {
                    Tab(selected = sourceTab == 0, onClick = { sourceTab = 0 }, text = { Text(stringResource(Res.string.tab_sources)) })
                    Tab(selected = sourceTab == 1, onClick = { sourceTab = 1 }, text = { Text(stringResource(Res.string.sources_extensions_tab)) })
                }

                PluginSourceList(
                    modifier = Modifier.weight(1f),
                    state = state,
                    sourceTab = sourceTab,
                    categoryExpanded = categoryExpanded,
                    onToggleCategory = { cat, expanded -> categoryExpanded = categoryExpanded + (cat to !expanded) },
                    onBrowse = { state.openBrowse(it.uuid ?: "") },
                )

                state.message?.let {
                    Text(
                        it.asString(),
                        style = MaterialTheme.typography.bodySmall,
                        color =
                        when (it.tone()) {
                            UiText.Tone.ERROR -> MaterialTheme.colorScheme.error
                            UiText.Tone.SUCCESS -> MaterialTheme.colorScheme.primary
                            UiText.Tone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }

    // ── Dialog quản lý kho repo (Thêm repo) ──
    if (showRepoDialog) {
        RepoDialog(state, onDismiss = { showRepoDialog = false })
    }

    // ── Dialog thêm sách bằng URL ──
    if (showUrlDialog) {
        AddUrlDialog(state, onDismiss = { showUrlDialog = false })
    }
}

/** Dialog dán URL truyện — openPreview → AddBookState tự dò plugin khớp trong repo. */
@Composable
private fun AddUrlDialog(
    state: PluginState,
    onDismiss: () -> Unit,
) {
    var url by remember { mutableStateOf("") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.cd_add_from_url)) },
        text = {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(Res.string.sources_add_url_hint)) },
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    state.openPreview(url.trim())
                    onDismiss()
                },
                enabled = url.isNotBlank(),
            ) { Text(stringResource(Res.string.action_search)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
        },
    )
}

/** Dialog quản lý kho repo (Thêm repo) — tách khỏi PluginScreen cho dưới ngưỡng LongMethod. */
@Composable
private fun RepoDialog(
    state: PluginState,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.cd_add_repo)) },
        text = { RepoManagerSection(state) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_close)) }
        },
    )
}

/** Panel search: tab Nguồn → tìm toàn cầu mọi plugin; tab Phần mở rộng → filter extension. */
@Composable
private fun PluginSearchSection(
    sourceTab: Int,
    visible: Boolean,
    globalSearch: String,
    onGlobalSearch: (String) -> Unit,
    searchState: GlobalSearchState,
    state: PluginState,
    onOpenResults: () -> Unit,
) {
    AnimatedVisibility(visible = visible) {
        if (sourceTab == 0) {
            // Tab Nguồn: tìm kiếm toàn cầu qua search script mọi plugin
            GlobalSearchPanel(
                query = globalSearch,
                onQuery = onGlobalSearch,
                searchState = searchState,
                onOpenResults = onOpenResults,
            )
        } else {
            OutlinedTextField(
                value = state.search,
                onValueChange = { state.search = it },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                placeholder = {
                    Text(
                        stringResource(Res.string.sources_search_ext_hint),
                        fontFamily = MonoFont(),
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                textStyle = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFont()),
                singleLine = true,
            )
        }
    }
}

/** List chính theo tab: Nguồn (đã cài, pinned lên đầu) / Phần mở rộng (kho theo category). */
@Composable
private fun PluginSourceList(
    state: PluginState,
    sourceTab: Int,
    categoryExpanded: Map<String, Boolean>,
    onToggleCategory: (String, Boolean) -> Unit,
    onBrowse: (PluginEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val repoList =
        state.extensions.matchesSearch(
            state.search,
            { it.name },
            { it.author },
            { it.source },
        )
    // Nguồn hỏng (browse fail) + nguồn không khai báo home (không browse được) → ẩn
    val installedList =
        state.installed.matchesSearch(
            state.search,
            { it.name },
            { it.source },
        ).filter { it.name !in state.brokenSources }
    // Pinned trước — như ảnh (pin icon đưa nguồn lên đầu)
    val pinnedFirst = installedList.sortedByDescending { it.name in state.pinnedNames }
    // Nguồn chết: gom section riêng (mờ + Thử lại) thay vì ẩn hẳn — user biết
    // nguồn tồn tại nhưng site không phản hồi (dogfood 260906)
    val brokenInstalled = state.installed.filter { it.name in state.brokenSources }
    var brokenExpanded by remember { mutableStateOf(false) }

    // weight(1f) từ caller — nếu không LazyColumn chiếm trọn chỗ còn lại, message
    // (lỗi fetch, gợi ý cài lại khi mất nguồn) bị đẩy ra ngoài màn hình
    LazyColumn(modifier) {
        if (sourceTab == 0) {
            if (installedList.isEmpty()) {
                item {
                    // Empty state có icon + căn giữa — text lẻ trôi giữa list dễ đọc
                    // nhầm là trạng thái sai khi sách vẫn đang đọc từ nguồn khác
                    // (dogfood 260905; nguồn sách nằm ngoài registry hiển thị ở đây)
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp),
                        )
                        Text(
                            stringResource(Res.string.sources_empty_installed),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
            items(pinnedFirst) { plugin ->
                InstalledSourceItem(state, plugin, onBrowse)
            }
            if (brokenInstalled.isNotEmpty()) {
                item {
                    SectionHeader(
                        stringResource(Res.string.sources_broken_section, brokenInstalled.size),
                        brokenExpanded,
                    ) { brokenExpanded = !brokenExpanded }
                }
                if (brokenExpanded) {
                    items(brokenInstalled, key = { "broken-${it.uuid}" }) { plugin ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                plugin.name ?: "?",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { state.retrySource(plugin.name ?: "") }) {
                                Text(stringResource(Res.string.sources_retry))
                            }
                        }
                    }
                }
            }
        } else {
            extensionsTab(
                state = state,
                repoList = repoList,
                categoryExpanded = categoryExpanded,
                onToggleCategory = onToggleCategory,
            )
        }
    }
}

/** Top bar màn Nguồn: title trái lớn + actions phải (refresh, thêm repo, search) — không nhảy khi đổi tab. */
@Composable
private fun PluginTopBar(
    state: PluginState,
    onAddUrl: () -> Unit,
    onAdd: () -> Unit,
    onSearch: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(Res.string.tab_sources),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { state.refresh() }, enabled = !state.loading) {
            Icon(Icons.Filled.Refresh, contentDescription = stringResource(Res.string.cd_refresh))
        }
        IconButton(onClick = onAddUrl) {
            Icon(Icons.Filled.Link, contentDescription = stringResource(Res.string.cd_add_from_url))
        }
        IconButton(onClick = onAdd) {
            Icon(
                Icons.Filled.AddCircle,
                contentDescription = stringResource(Res.string.cd_add_repo),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        IconButton(onClick = onSearch) {
            Icon(Icons.Filled.Search, contentDescription = stringResource(Res.string.cd_global_search))
        }
    }
}

/** Ô tìm kiếm toàn cầu — submit mở trang kết quả riêng (SearchResultsPage). */
@Composable
private fun GlobalSearchPanel(
    query: String,
    onQuery: (String) -> Unit,
    searchState: GlobalSearchState,
    onOpenResults: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            keyboardOptions =
            androidx.compose.foundation.text.KeyboardOptions(
                imeAction = androidx.compose.ui.text.input.ImeAction.Search,
            ),
            keyboardActions =
            androidx.compose.foundation.text.KeyboardActions(
                onSearch = {
                    // Query rỗng không submit — khỏi báo "Không có kết quả" vô căn cứ (dogfood 260906)
                    if (query.isNotBlank() && !searchState.searching) {
                        searchState.search(query)
                        onOpenResults()
                    }
                },
            ),
            modifier = Modifier.fillMaxWidth(),
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
                    IconButton(
                        onClick = {
                            searchState.search(query)
                            onOpenResults()
                        },
                        enabled = !searchState.searching,
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = stringResource(Res.string.action_search))
                    }
                }
            },
        )
        if (searchState.searching) {
            androidx.compose.foundation.layout.Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                LinearProgressIndicator(Modifier.weight(1f))
                searchState.progress?.let {
                    Text(
                        " $it",
                        fontFamily = MonoFont(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** 1 nguồn đã cài trong tab Nguồn: row + dialog cấu hình + confirm xóa. */
@Composable
private fun InstalledSourceItem(
    state: PluginState,
    plugin: PluginEntity,
    onBrowse: (PluginEntity) -> Unit,
) {
    var cfgOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    SourceRowInstalled(
        state = state,
        plugin = plugin,
        pinned = plugin.name in state.pinnedNames,
        // Nguồn không có home script → không browse được, disable cả row khỏi bấm mất công
        browseEnabled = !plugin.homeGetter.isNullOrEmpty(),
        actions =
        SourceRowActions(
            onTogglePin = { state.togglePin(plugin.name ?: "") },
            onConfig = { cfgOpen = !cfgOpen },
            onOpen = { onBrowse(plugin) },
            onUninstall = { confirmDelete = true },
        ),
    )
    if (cfgOpen) PluginConfigDialog(state, plugin) { cfgOpen = false }
    if (confirmDelete) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(Res.string.sources_delete_title)) },
            text = { Text(stringResource(Res.string.sources_delete_confirm, plugin.name ?: "?")) },
            confirmButton = {
                Button(
                    onClick = {
                        state.uninstall(plugin)
                        confirmDelete = false
                    },
                    colors =
                    androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text(stringResource(Res.string.action_delete)) }
            },
            dismissButton = {
                OutlinedButton(onClick = { confirmDelete = false }) { Text(stringResource(Res.string.action_cancel)) }
            },
        )
    }
}

/** Tab Phần mở rộng: NÂNG CẤP (có bản mới) + CÀI ĐẶT theo category + quản lý kho repo. */
private fun LazyListScope.extensionsTab(
    state: PluginState,
    repoList: List<VBookExtensionEntity>,
    categoryExpanded: Map<String, Boolean>,
    onToggleCategory: (String, Boolean) -> Unit,
) {
    // Group trùng (vd "Truyện Full" / "truyenfull" cùng host) — giữ bản version cao
    val deduped = dedupeExtensions(repoList)
    // 2 khu theo yêu cầu owner: NÂNG CẤP (có bản mới) + CÀI ĐẶT (chưa có)
    val upgradable =
        deduped.filter { ext ->
            val installed = state.findInstalled(ext)
            // Cùng thang với PluginRow (version repo int 17 = 1.7) — so raw int
            // (17 > 1.3) sinh row NÂNG CẤP ma, bấm không có tác động (bug 260902)
            val repoV = ext.version?.let { it / 10.0 }
            val curV = installed?.version
            installed != null && repoV != null && (curV == null || repoV > curV) && ext.path !in state.encryptedPaths
        }
    val fresh = deduped.filter { state.findInstalled(it) == null }
    if (upgradable.isNotEmpty()) {
        item { SectionHeader(categoryLabel("upgrade", upgradable.size), true) { } }
        // "Cập nhật tất cả" — 260905 owner: cài batch mọi bản mới, không tap từng row
        item { UpdateAllButton(state, upgradable.size) }
        items(upgradable) { ext -> PluginRow(state, ext) }
    }
    // Kho chia theo category: ngôn ngữ (Tiếng Việt/Trung/Anh) + Audio, "Khác" gom phần còn lại
    val byCategory =
        fresh
            .groupBy { extCategory(it) }
            .toList()
            .sortedBy { (cat, _) -> CATEGORY_ORDER.indexOf(cat).let { if (it < 0) CATEGORY_ORDER.size else it } }
    byCategory.forEach { (cat, list) ->
        val expanded = categoryExpanded[cat] ?: (cat == "vi")
        item {
            SectionHeader(categoryLabel(cat, list.size), expanded) { onToggleCategory(cat, expanded) }
        }
        if (expanded) items(list) { ext -> PluginRow(state, ext) }
    }
    if (repoList.isEmpty() && !state.loading) {
        item { Text(state.message?.asString() ?: stringResource(Res.string.sources_no_match), style = MaterialTheme.typography.bodySmall) }
    }
    // Quản lý kho repo — theo yêu cầu: thêm được repo ở Phần mở rộng
    item { SectionHeader(categoryLabel("repo", state.repos.size), state.reposExpanded) { state.reposExpanded = !state.reposExpanded } }
    if (state.reposExpanded) {
        item { RepoManagerSection(state) }
    }
}

/** Nội dung khu KHO REPO: list repo hiện có + field thêm repo mới. */
@Composable
private fun RepoManagerSection(state: PluginState) {
    Column(Modifier.padding(vertical = 6.dp)) {
        state.repos.forEach { repo ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    repo.link ?: "?",
                    fontFamily = MonoFont(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { state.removeRepo(repo) }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(Res.string.sources_delete_repo),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.newRepoUrl,
                onValueChange = { state.newRepoUrl = it },
                label = { Text(stringResource(Res.string.sources_repo_url_label)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = { state.addRepo() }) { Text(stringResource(Res.string.action_add)) }
        }
    }
}

/** Icon plugin: đã cài = iconBase64 data URI; chưa cài = URL icon resolve theo repo. */
private fun pluginIconUrl(ext: VBookExtensionEntity): String? {
    val i = ext.icon ?: return null
    if (i.startsWith("http")) return i
    val base = ext.source?.substringBefore("plugin.json")?.trimEnd('/') ?: return null
    return "$base/${i.trimStart('/')}"
}

/** Ô icon 40dp: cover nếu có, chữ cái đầu nếu không. */
@Composable
private fun PluginIconBox(
    iconUrl: String?,
    name: String?,
    log: dev.haipham22.leechtext.log.EngineLogger,
) {
    Box(
        Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        if (iconUrl != null) {
            CoverImage(iconUrl, modifier = Modifier.size(40.dp), log = log)
        } else {
            Text(
                (name ?: "?").take(1).uppercase(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Callbacks hàng nguồn đã cài — gom nhóm giảm số param (S107). */
private data class SourceRowActions(
    val onTogglePin: () -> Unit = {},
    val onConfig: () -> Unit = {},
    val onOpen: () -> Unit = {},
    val onUninstall: () -> Unit = {},
)

/** Row nguồn đã cài — y hệt ảnh: icon + tên + "Tiếng Việt" + 3 icons ⚙/pin/! */
@Composable
private fun SourceRowInstalled(
    state: PluginState,
    plugin: PluginEntity,
    pinned: Boolean,
    browseEnabled: Boolean = true,
    actions: SourceRowActions = SourceRowActions(),
) {
    Column {
        androidx.compose.material3.Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().clickable(enabled = browseEnabled, onClick = actions.onOpen).padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PluginIconBox(plugin.icon, plugin.name, log = state.log)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(plugin.name ?: "?", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    // Ngôn ngữ + domain nguồn — biết plugin này phục vụ site nào
                    Text(
                        buildString {
                            append(languageLabel(plugin.language))
                            plugin.source?.let { append(" · ", it.removePrefix(HTTPS_PREFIX).removePrefix(HTTP_PREFIX).substringBefore("/")) }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
                // Action icons: cấu hình / pin / test / xóa
                IconButton(onClick = actions.onConfig, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = stringResource(Res.string.sources_config),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = actions.onTogglePin, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Filled.PushPin,
                        contentDescription = stringResource(if (pinned) Res.string.sources_unpin else Res.string.sources_pin),
                        tint =
                        if (pinned) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(
                    onClick = { state.testSite(plugin.source) },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = stringResource(Res.string.sources_test_site),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = actions.onUninstall, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(Res.string.sources_delete_source),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

/** Language code ("vi"/"en"/"zh" từ PluginEntity.language) → label hiển thị. */
@Composable
private fun languageLabel(language: String?): String = when (language?.lowercase()) {
    "vi" -> stringResource(Res.string.lang_name_vi)
    "zh" -> stringResource(Res.string.lang_name_zh)
    "en" -> stringResource(Res.string.lang_name_en)
    null, "" -> stringResource(Res.string.lang_name_vi)
    else -> language
}

/** Locale code → label hiển thị (ảnh Tachiyomi: "Tiếng Việt"). */
@Composable
private fun localeLabel(locale: String?): String = when {
    locale == null -> ""
    locale.startsWith("vi", ignoreCase = true) -> stringResource(Res.string.lang_name_vi)
    locale.startsWith("zh", ignoreCase = true) -> stringResource(Res.string.lang_name_zh)
    locale.startsWith("en", ignoreCase = true) -> stringResource(Res.string.lang_name_en)
    else -> locale
}

/**
 * Category key ổn định theo ngôn ngữ hiển thị (group ở LazyListScope ngoài
 * composition không gọi stringResource được) — label resolve ở SectionHeader.
 */
private const val HTTPS_PREFIX = "https://"
private const val HTTP_PREFIX = "http://"

private val CATEGORY_ORDER = listOf("vi", "zh", "en", "audio", "other")

/** Label section theo category key (mã ổn định) + số lượng. */
@Composable
private fun categoryLabel(
    key: String,
    count: Int,
): String = when (key) {
    "upgrade" -> stringResource(Res.string.sources_section_upgrade, count)
    "repo" -> stringResource(Res.string.sources_section_repo, count)
    "vi" -> stringResource(Res.string.lang_name_vi) + " ($count)"
    "zh" -> stringResource(Res.string.lang_name_zh) + " ($count)"
    "en" -> stringResource(Res.string.lang_name_en) + " ($count)"
    "audio" -> stringResource(Res.string.cat_audio) + " ($count)"
    else -> stringResource(Res.string.cat_other) + " ($count)"
}

/**
 * Group extension trùng theo dedupe key như PluginManager (tên chuẩn hoá + host) —
 * repo vBook có thể trả 2 entry khác name nhưng cùng nguồn ("Truyện Full" /
 * "truyenfull") → NÂNG CẤP hiện 2 dòng; giữ bản version cao nhất.
 */
private fun dedupeExtensions(exts: List<VBookExtensionEntity>): List<VBookExtensionEntity> = exts
    .groupBy { ext ->
        val host = ext.source?.removePrefix(HTTPS_PREFIX)?.removePrefix(HTTP_PREFIX)?.substringBefore("/")?.lowercase().orEmpty()
        val name = dev.haipham22.leechtext.util
            .removeDiacritics(ext.name.orEmpty().replace("(vBook)", "", ignoreCase = true))
            .lowercase()
            .replace(Regex("[\\s\\p{Punct}]+"), "")
        "$name|$host"
    }.map { (_, group) -> group.maxBy { it.version ?: 0 } }

/** Category plugin: audio/tts tách riêng, còn lại theo ngôn ngữ (không biết → other). */
private fun extCategory(ext: VBookExtensionEntity): String = when (ext.type?.lowercase()) {
    "audio", "tts" -> "audio"

    else ->
        when {
            ext.locale?.startsWith("vi", true) == true -> "vi"
            ext.locale?.startsWith("zh", true) == true -> "zh"
            ext.locale?.startsWith("en", true) == true -> "en"
            else -> "other"
        }
}

/** Nút "Cập nhật tất cả" — 260905 owner: cài batch mọi bản mới, không tap từng row. */
@Composable
private fun UpdateAllButton(
    state: PluginState,
    count: Int,
) {
    OutlinedButton(
        onClick = { state.installAll() },
        enabled = !state.loading,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(Res.string.plugins_update_all, count))
    }
}

/** Section header mono caps theo design — không Card. */
@Composable
private fun SectionHeader(
    label: String,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            fontFamily = MonoFont(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.08.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(
            if (expanded) Icons.Filled.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
    }
}

/** Source row theo design 2911c97d: icon tile chữ cái + name + meta mono + chip/actions. */
@Composable
private fun SourceRow(
    title: String,
    meta: String,
    installed: Boolean,
    onConfig: () -> Unit = {},
    onRemove: () -> Unit = {},
) {
    Column {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Icon tile 40dp — chữ cái đầu trên Soft Fill
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    title.take(1).uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodySmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                Text(
                    meta,
                    fontFamily = MonoFont(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
            if (installed) {
                // Chip moss "ĐÃ CÀI" theo design
                Text(
                    stringResource(Res.string.sources_installed_chip),
                    fontFamily = MonoFont(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier =
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
            IconButton(onClick = onConfig, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Settings, contentDescription = stringResource(Res.string.sources_config), modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.action_delete), modifier = Modifier.size(16.dp))
            }
        }
        // Divider
        Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)))
    }
}

@Composable
private fun PluginRow(
    state: PluginState,
    ext: VBookExtensionEntity,
) {
    val installedEntity = state.findInstalled(ext)
    // vBook repo version là int (13 = 1.3) — đưa về cùng thang plugin, không là
    // 13 > 1.3 luôn "có bản mới" dù không có gì (bug 2026-08-27)
    val repoVersion: Double? = ext.version?.let { it / 10.0 }
    val installedVersion: Double? = installedEntity?.version
    val hasNewer =
        repoVersion != null &&
            (installedVersion == null || repoVersion > installedVersion)
    val upgradable = installedEntity != null && hasNewer

    Column {
        Row(
            Modifier
                .fillMaxWidth()
                // NÂNG CẤP: bấm cả row — icon nhỏ khó trúng (dogfood 260902)
                .clickable(enabled = upgradable && !state.loading) { state.install(ext) }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PluginIconBox(pluginIconUrl(ext), ext.name, log = state.log)
            Spacer(Modifier.width(12.dp))
            PluginRowInfo(state, ext, installedEntity, upgradable, repoVersion, installedVersion)
            IconButton(
                onClick = { state.testSite(ext.source) },
                enabled = !state.loading,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    Icons.Filled.Public,
                    contentDescription = stringResource(Res.string.sources_test_site),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            PluginRowAction(state, ext, installedEntity, upgradable)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)))
    }
}

/** Cột giữa PluginRow: tên + meta (locale · tác giả · host) + badge bản mới/nguồn chết. */
@Composable
private fun androidx.compose.foundation.layout.RowScope.PluginRowInfo(
    state: PluginState,
    ext: VBookExtensionEntity,
    installedEntity: PluginEntity?,
    upgradable: Boolean,
    repoVersion: Double?,
    installedVersion: Double?,
) {
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            ext.name ?: "?",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        )
        Text(
            listOfNotNull(
                localeLabel(ext.locale).ifEmpty { null },
                ext.author ?: "?",
                // Bỏ scheme — "https://t…" cắt mất host, hiện host đầy đủ hơn
                ext.source?.removePrefix(HTTPS_PREFIX)?.removePrefix(HTTP_PREFIX) ?: "",
            ).joinToString(" · "),
            fontFamily = MonoFont(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.clickable { openInBrowser(ext.source) },
        )
        if (upgradable) {
            Text(
                stringResource(Res.string.sources_newer_version, fmt(repoVersion), fmt(installedVersion)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        // Đã cài nhưng nguồn chết (browse rỗng) — cảnh báo, KHÔNG chặn nút update
        // (chết là chuyện site, plugin vẫn update được — dogfood 260906)
        if (installedEntity != null && installedEntity.name in state.brokenSources) {
            Text(
                stringResource(Res.string.sources_maybe_dead),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/** Nút cuối row: chưa cài → Download, có bản mới → Update, đã cài mới nhất → không nút. */
@Composable
private fun PluginRowAction(
    state: PluginState,
    ext: VBookExtensionEntity,
    installedEntity: PluginEntity?,
    upgradable: Boolean,
) {
    when {
        installedEntity == null -> {
            IconButton(
                onClick = { state.install(ext) },
                enabled = !state.loading,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    Icons.Filled.Download,
                    contentDescription = stringResource(Res.string.sources_install),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        upgradable -> {
            IconButton(
                onClick = { state.install(ext) },
                enabled = !state.loading,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    Icons.Filled.Update,
                    contentDescription = stringResource(Res.string.sources_upgrade),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/** Editor cấu hình plugin — luôn có domain override + declared config nếu có. */
@Composable
private fun PluginConfigDialog(
    state: PluginState,
    plugin: dev.haipham22.leechtext.entities.PluginEntity,
    onDismiss: () -> Unit,
) {
    val spec = plugin.configSpec ?: emptyMap()

    // Local edits — KHÔNG đụng plugin trực tiếp, áp 1 lần khi bấm Lưu
    var domain by remember { mutableStateOf(plugin.source ?: "") }
    val config =
        remember {
            mutableStateMapOf<String, String>().apply { putAll(plugin.config ?: emptyMap()) }
        }
    val globalSettings = SettingsRepository.load()

    fun current(
        key: String,
        def: String? = null,
    ): String = config[key] ?: def ?: ""

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(Res.string.sources_config_title, plugin.name ?: "?"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            )
        },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Domain override — mọi plugin đều có
                Text(stringResource(Res.string.sources_domain_label), style = MaterialTheme.typography.bodySmall)
                Text(
                    stringResource(Res.string.sources_domain_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = domain,
                    onValueChange = { domain = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Built-in connection settings (vBook extension-api)
                NumberConfigField(stringResource(Res.string.sources_cfg_threads), globalSettings.maxConn, current("thread_num")) {
                    config["thread_num"] = it
                }
                NumberConfigField(stringResource(Res.string.sources_cfg_delay), globalSettings.delay, current("delay")) {
                    config["delay"] = it
                }
                NumberConfigField(stringResource(Res.string.sources_cfg_timeout), globalSettings.timeout, current("timeout")) {
                    config["timeout"] = it
                }

                // Declared config từ plugin.json (nếu có)
                DeclaredConfigFields(spec, config)
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(onClick = {
                // Áp 1 lần — bỏ key số rỗng (= dùng global default)
                plugin.source = domain.trim().ifEmpty { null }
                val numberKeys = setOf("thread_num", "delay", "timeout")
                plugin.config =
                    config
                        .toMap()
                        .filterNot { (k, v) -> k in numberKeys && v.isBlank() }
                        .let { if (it.isEmpty()) null else it }
                state.pluginPersistence.saveAtomic(plugin)
                state.pluginManager.add(plugin) // re-add → update entity trong list
                state.installed = state.pluginManager.list()
                onDismiss()
            }) { Text(stringResource(Res.string.action_save)) }
        },
        dismissButton = {
            androidx.compose.material3.OutlinedButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
        },
    )
}

/** Declared config từ plugin.json (config_spec) — render field theo mode select/toggle/text. */
@Composable
@Suppress("MutableParams") // edit-in-place config plugin kiểu vBook — map share với caller
private fun DeclaredConfigFields(
    spec: Map<String, dev.haipham22.leechtext.entities.PluginConfigSpec>,
    config: MutableMap<String, String>,
) {
    spec.forEach { (key, cfg) ->
        val value = config[key] ?: cfg.default.orEmpty()
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(cfg.title ?: key, style = MaterialTheme.typography.bodySmall)
            cfg.subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            when (cfg.mode) {
                "select" -> {
                    cfg.values.forEach { option ->
                        Row(
                            Modifier.fillMaxWidth().clickable { config[key] = option },
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            androidx.compose.material3.RadioButton(
                                selected = value == option,
                                onClick = { config[key] = option },
                            )
                            Text(option, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                "toggle" -> {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(key, style = MaterialTheme.typography.bodySmall)
                        androidx.compose.material3.Switch(
                            checked = value == "true",
                            onCheckedChange = { config[key] = it.toString() },
                        )
                    }
                }

                else -> {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { config[key] = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/** Field số per-plugin — chỉ ghi local state, rỗng = dùng global default. */
@Composable
private fun NumberConfigField(
    label: String,
    globalDefault: Int,
    value: String,
    onChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(
            stringResource(Res.string.sources_cfg_default, globalDefault),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = value,
            onValueChange = { input -> onChange(input.filter { it.isDigit() }) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Mở URL ngoài app — qua expect openUrl (desktop browser / Android Intent / iOS UIApplication). */
private fun openInBrowser(url: String?) {
    if (url.isNullOrBlank() || !url.startsWith("http")) return
    openUrl(url)
}

/** 0.1 -> "v0.1"; null -> "" (version Double như format .plugin gốc). */
private fun fmt(v: Double?): String = v?.let { "v" + it.toString().trimEnd('0').trimEnd('.') } ?: ""

/** Filter theo search text (tên + các field phụ), case-insensitive + không dấu (vd "truyen" khớp "Truyện"). */
private fun <T> List<T>.matchesSearch(
    query: String,
    name: (T) -> String?,
    vararg extra: (T) -> String?,
): List<T> {
    val q = removeDiacritics(query.trim().lowercase())
    if (q.isEmpty()) return this

    fun norm(s: String?) = removeDiacritics((s ?: "").lowercase())
    return filter { candidate ->
        norm(name(candidate)).contains(q) ||
            extra.any { norm(it(candidate)).contains(q) }
    }
}

/**
 * Màn detail truyện CHƯA thêm thư viện — giao diện như BookDetailScreen: top bar,
 * cover + meta, action row, danh sách chương. Click chương → đọc luôn (fetch on demand,
 * không lưu). Nút: Đọc (chương 1) / Thêm vào thư viện / Tải full.
 */
@Composable
fun NovelPreviewScreen(
    state: AddBookState,
    url: String,
    queueState: DownloadQueueState?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = state.properties
    Column(
        modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        // Top bar 56dp như BookDetailScreen
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(Res.string.action_back),
                )
            }
            Text(
                p?.name ?: url,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.action_close))
            }
        }

        PreviewStatus(state)

        if (p != null) {
            // Sách đã trong Thư viện? (check properties.json theo URL, qua IO)
            androidx.compose.runtime.LaunchedEffect(p.url) { state.checkInLibrary() }
            PreviewBookContent(p, state, url, queueState, onBack, Modifier.weight(1f))
        }
    }

    // Reader full-screen khi đang đọc
    if (state.previewIndex != null) PreviewChapterReader(state)
}

/** Trạng thái tải preview: LoadingInfo → spinner; Failed → lỗi đỏ; khác → trống. */
@Composable
private fun PreviewStatus(state: AddBookState) {
    when (val s = state.status) {
        is AddBookState.Status.LoadingInfo -> {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(Res.string.addbook_loading_info))
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 8.dp))
            }
        }

        is AddBookState.Status.Failed -> {
            Text(
                stringResource(Res.string.error_prefix, s.error.asString()),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp),
            )
        }

        else -> Unit // nhánh không render gì
    }
}

/**
 * Nội dung preview khi đã có properties — wrapper mỏng trên BookDetailBody
 * (component dùng chung với BookDetailScreen, dogfood 260903: 2 layout trùng nhau).
 * Preview-mode ẩn các action chỉ-của-thư-viện (menu ⋮, đổi bìa, export, migrate,
 * xóa) cho tới khi sách vào thư viện; giữ "✓ Đã trong" + nút Thêm + Tải full.
 */
@Composable
private fun PreviewBookContent(
    p: Properties,
    state: AddBookState,
    url: String,
    queueState: DownloadQueueState?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // "Đọc": sách đã trong thư viện → mở đúng vị trí lastread.txt như màn Thư viện;
    // chưa → fetch-on-demand chương đầu như cũ.
    var lastRead by remember(p.url) { mutableStateOf<Int?>(null) }
    androidx.compose.runtime.LaunchedEffect(p.url, state.inLibrary) { lastRead = state.lastReadIndex() }

    var showPick by remember { mutableStateOf(false) }
    BookDetailBody(
        book = p,
        onRead = { state.openPreviewChapter(lastRead ?: 0) },
        options =
        BookDetailBodyOptions(
            readEnabled = !state.fetchingChapter,
            chapterCountVisible = state.chaptersLoaded,
            introMaxLines = 4,
        ),
        modifier = modifier,
        log = state.log,
        slots =
        BookDetailBodySlots(
            secondaryActions = {
                OutlinedButton(
                    onClick = { state.addToLibrary() },
                    // Đã có sách (theo URL) → nút disabled "✓ Đã trong thư viện"
                    enabled = !state.inLibrary && state.status !is AddBookState.Status.Downloading,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        if (state.inLibrary) {
                            "✓ " + stringResource(Res.string.addbook_in_library)
                        } else {
                            stringResource(Res.string.addbook_add_to_library)
                        },
                        maxLines = 1,
                    )
                }
                OutlinedButton(
                    onClick = {
                        // TOC đã có → dialog chọn chương lẻ (N2, default all); chưa → fetch TOC
                        // rồi mở dialog (dialog tự hiện khi chapList populate)
                        if (state.chaptersLoaded && !p.chapList.isNullOrEmpty()) {
                            showPick = true
                        } else {
                            state.loadChapterList()
                            showPick = true
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(Res.string.addbook_download_full)) }
            },
            trailingContent = {
                state.message?.let {
                    Text(it.asString(), style = MaterialTheme.typography.bodySmall)
                }

                // ── Danh sách chương — lấy TOC khi bấm, render lazy theo scroll ──
                InlineChapterToc(
                    chapters = p.chapList.orEmpty(),
                    loaded = state.chaptersLoaded,
                    loading = state.loadingChapters,
                    onLoad = { state.loadChapterList() },
                    onChapterClick = { state.openPreviewChapter(it) },
                )
            },
        ),
    )

    // N2: chọn chương lẻ → enqueue kèm rangeText, pump/Android service cùng parse
    if (showPick && p.chapList != null) {
        ChapterPickDialog(
            chapters = p.chapList!!,
            onConfirm = { indices ->
                queueState?.enqueue(listOf(url), compressToRange(indices))
                showPick = false
                onBack()
            },
            onDismiss = { showPick = false },
        )
    }
}

/** Reader: top bar prev/next + nội dung scroll. Không lưu lastread/raw. */
@Composable
private fun PreviewChapterReader(state: AddBookState) {
    val idx = state.previewIndex ?: return
    val chapters = state.properties?.chapList.orEmpty()
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { state.closePreviewChapter() }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(Res.string.action_back),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "Ch. ${idx + 1}/${chapters.size}",
                    fontFamily = MonoFont(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    chapters.getOrNull(idx)?.chapName ?: "",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = { state.openPreviewChapter((idx - 1).coerceAtLeast(0)) },
                enabled = idx > 0,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(Res.string.cd_prev_chapter),
                )
            }
            IconButton(
                onClick = { state.openPreviewChapter((idx + 1).coerceAtMost(chapters.size - 1)) },
                enabled = idx < chapters.size - 1,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(Res.string.cd_next_chapter),
                )
            }
        }
        if (state.fetchingChapter) LinearProgressIndicator(Modifier.fillMaxWidth())
        // Strip HTML như ChapterReader — nội dung TextLoader là HTML thô
        val paragraphs = remember(state.previewText) { toParagraphs(state.previewText) }
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (paragraphs.isEmpty() && !state.fetchingChapter) {
                Text(
                    stringResource(Res.string.addbook_chapter_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Lỗi fetch chương (đã localize qua UiText) — hiện đỏ ngay trong reader
            state.previewError?.let {
                Text(
                    it.asString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            paragraphs.forEach { p ->
                Text(p, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
