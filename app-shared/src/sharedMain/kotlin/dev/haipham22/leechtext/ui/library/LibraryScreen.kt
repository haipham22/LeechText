package dev.haipham22.leechtext.ui.library
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import dev.haipham22.leechtext.action.export.Text
import dev.haipham22.leechtext.resources.Res
import dev.haipham22.leechtext.resources.action_cancel
import dev.haipham22.leechtext.resources.action_pause
import dev.haipham22.leechtext.resources.action_resume
import dev.haipham22.leechtext.resources.cd_close_search
import dev.haipham22.leechtext.resources.cd_open_library
import dev.haipham22.leechtext.resources.cd_refresh
import dev.haipham22.leechtext.resources.cd_search_library
import dev.haipham22.leechtext.resources.cd_sort
import dev.haipham22.leechtext.resources.lib_reading_badge
import dev.haipham22.leechtext.resources.library_empty_hint
import dev.haipham22.leechtext.resources.library_empty_title
import dev.haipham22.leechtext.resources.library_items_count
import dev.haipham22.leechtext.resources.library_search_empty
import dev.haipham22.leechtext.resources.library_search_hint
import dev.haipham22.leechtext.resources.tab_library
import dev.haipham22.leechtext.ui.components.atoms.CoverImage
import dev.haipham22.leechtext.ui.components.molecules.BookCoverTile
import dev.haipham22.leechtext.ui.model.asString
import dev.haipham22.leechtext.ui.queue.DownloadQueueState
import dev.haipham22.leechtext.ui.queue.QueueItem
import dev.haipham22.leechtext.ui.reader.ChapterReader
import dev.haipham22.leechtext.ui.theme.MonoFont
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.lazy.grid.items as gridItems

/** Màn thư viện — danh sách sách đã lưu, kiểm tra/tải chương mới. */
@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier,
    state: LibraryState,
    queueState: DownloadQueueState,
) {
    // Refresh mỗi lần vào lại tab (LaunchedEffect Unit chạy lại khi tab được compose
    // lại) — sách thêm từ tab Nguồn hiện ngay, không cần bấm ↻ (dogfood 260906)
    androidx.compose.runtime.LaunchedEffect(Unit) { state.refresh() }
    var search by remember { mutableStateOf("") }
    var searchOpen by remember { mutableStateOf(false) } // ẩn mặc định — click icon search mới hiện
    Column(modifier) {
        if (state.selected == null) {
            LibraryAppBar(
                search,
                { search = it },
                searchOpen,
                { open ->
                    searchOpen = open
                    if (!open) search = "" // đóng → bỏ filter, grid về đầy đủ
                },
                state,
            )
        }
        if (state.selected != null && state.editingIndex != null) {
            // Reader full-screen — full-bleed, NÉ padding 16dp của Column dưới
            ChapterReader(state)
        } else {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Queue inline chỉ dành cho compact (Android) — desktop có panel phải (SliceApp)
                CompactQueueIfAny(queueState) { state.refresh() }
                LibraryMainContent(state, search, queueState)
                state.message?.let { Text(it.asString()) }
            }
        }
    } // Column nội dung (trong app bar column)
}

/** Desktop/plate có queue panel cố định bên phải (SliceApp provide true) — inline
 * queue trong content phải ẩn, không thì 2 chỗ cùng điều khiển 1 job (owner 260906:
 * banner trùng lặp nhìn thừa). */
val LocalHasQueuePanel = androidx.compose.runtime.compositionLocalOf { false }

/** Wrapper queue compact — chỉ hiện khi KHÔNG có queue panel (desktop/plate provide
 * LocalHasQueuePanel) và màn hẹp (<720dp). Đo content column, không phải window —
 * window 1280 có panel thì column ~700dp, điều kiện width một mình là thiếu. */
@Composable
private fun CompactQueueIfAny(
    queueState: DownloadQueueState,
    onEnterLibrary: () -> Unit,
) {
    androidx.compose.foundation.layout.BoxWithConstraints {
        val hasPanel = LocalHasQueuePanel.current
        if (!hasPanel && maxWidth < 720.dp && queueState.items.isNotEmpty()) {
            CompactDownloadQueue(queueState, onEnterLibrary)
        }
    } // BoxWithConstraints queue compact
}

/** Nội dung chính thư viện — ChildStack: Grid → Detail → Reader. */
@Composable
private fun LibraryMainContent(
    state: LibraryState,
    search: String,
    queueState: DownloadQueueState,
) {
    // Render trực tiếp theo stack value — không animation nên Children không cần
    // (Children trong ImageComposeScene offscreen gây detach race LayoutNode)
    val stack by state.stack.subscribeAsState()
    when (stack.active.configuration) {
        LibraryConfig.Grid -> {
            if (state.books.isEmpty()) {
                LibraryEmptyState()
            } else {
                LibraryGrid(state, search)
            }
        }

        LibraryConfig.Detail -> BookDetailScreen(state, queueState = queueState)

        // Reader full-screen thay toàn bộ detail khi đang mở chương
        LibraryConfig.Reader -> ChapterReader(state)
    }
}

/**
 * App bar master: 64dp Pure Surface, title + count chip mono trái; search mono + icons phải.
 * Mobile hẹp → search rơi xuống dòng riêng full-width, ẩn chip đếm.
 */
@Composable
private fun LibraryAppBar(
    search: String,
    onSearch: (String) -> Unit,
    searchOpen: Boolean,
    onSearchOpen: (Boolean) -> Unit,
    state: LibraryState,
) {
    androidx.compose.foundation.layout.BoxWithConstraints {
        val wide = maxWidth >= 600.dp
        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(
                        stringResource(Res.string.tab_library),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    )
                    if (wide) {
                        Spacer(Modifier.width(12.dp))
                        Text(
                            stringResource(Res.string.library_items_count, state.books.size),
                            fontFamily = MonoFont(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier =
                            Modifier
                                .background(
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                    androidx.compose.foundation.shape
                                        .RoundedCornerShape(6.dp),
                                ).padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
                if (wide) {
                    SearchAndRefresh(search, onSearch, searchOpen, onSearchOpen, state)
                } else {
                    // Nhóm icon ghim góc phải — con trực tiếp của SpaceBetween sẽ dàn giữa
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        SearchToggleButton(searchOpen, onSearchOpen)
                        SortMenuButton(state)
                        IconButton(onClick = {
                            state.closeBook()
                            state.refresh()
                        }) {
                            Icon(Icons.Filled.Refresh, contentDescription = stringResource(Res.string.cd_refresh))
                        }
                    }
                }
            }
            if (!wide && searchOpen) {
                OutlinedTextField(
                    value = search,
                    onValueChange = onSearch,
                    placeholder = {
                        Text(
                            stringResource(Res.string.library_search_hint),
                            fontFamily = MonoFont(),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    textStyle = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFont()),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                )
            }
        }
    }
}

/** Queue tải inline compact — mỗi item: tên + tiến trình + hủy / vào thư viện. */
@Composable
private fun CompactDownloadQueue(
    queueState: DownloadQueueState,
    onEnterLibrary: () -> Unit,
) {
    androidx.compose.foundation.lazy.LazyColumn(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.heightIn(max = 220.dp),
    ) {
        // KHÔNG key theo url — re-enqueue cùng URL sau khi done cho 2 item trùng url
        // (dogfood 260906: LazyColumn duplicate key crash); positional như DownloadQueuePanel
        items(queueState.items) { item ->
            Card(Modifier.fillMaxWidth()) {
                CompactQueueItem(item, queueState, onEnterLibrary)
            }
        }
    }
}

/** 1 item queue: tên + tiến trình + hủy / vào thư viện. */
@Composable
private fun CompactQueueItem(
    item: QueueItem,
    queueState: DownloadQueueState,
    onEnterLibrary: () -> Unit,
) {
    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text(
                (if (item.done) "✓ " else "") + item.name,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            if (!item.done) CompactQueueItemActions(item, queueState)
        }
        Text(item.status.asString(), style = MaterialTheme.typography.bodySmall)
        if (item.total > 0 && !item.done) {
            LinearProgressIndicator(
                progress = { item.completed.toFloat() / item.total },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (item.done && item.book != null) {
            OutlinedButton(onClick = {
                queueState.dismiss(item)
                onEnterLibrary()
            }) { Text(stringResource(Res.string.cd_open_library)) }
        }
    }
}

/** Nút Tạm dừng/Tiếp tục + Hủy của item queue đang chạy. */
@Composable
private fun CompactQueueItemActions(
    item: QueueItem,
    queueState: DownloadQueueState,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (item.paused) {
            OutlinedButton(onClick = { queueState.resume(item) }) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(Res.string.action_resume))
            }
        } else {
            OutlinedButton(onClick = { queueState.pause(item) }) {
                Icon(Icons.Filled.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(Res.string.action_pause))
            }
        }
        // Paused thì job đã null — Hủy trở thành dismiss (bỏ khỏi queue)
        OutlinedButton(onClick = {
            if (item.paused) queueState.dismiss(item) else queueState.cancel(item)
        }) { Text(stringResource(Res.string.action_cancel)) }
    }
}

/** Empty state khi thư viện chưa có sách. */
@Composable
private fun LibraryEmptyState() {
    Column(
        Modifier.fillMaxWidth().padding(top = 60.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(Res.string.library_empty_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(Res.string.library_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Grid cover Tachiyomi: Adaptive tự co (~7 cột desktop, 2-3 mobile), lọc theo search. */
@Composable
private fun LibraryGrid(
    state: LibraryState,
    search: String,
) {
    val visible =
        state.books.filter {
            search.isBlank() || (it.name ?: "").contains(search, ignoreCase = true)
        }
    // Search 0 kết quả → empty state thay vì grid biến mất im lặng (dogfood 260905)
    if (visible.isEmpty()) {
        Text(
            stringResource(Res.string.library_search_empty, search),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 16.dp),
        )
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.padding(horizontal = 8.dp),
    ) {
        gridItems(
            visible,
            key = { it.url ?: it.name ?: it.hashCode().toString() },
        ) { b ->
            // Badge "đang đọc chương mấy" — lastread.txt load sẵn trong refresh (IO)
            val lastRead = state.lastReadMap[b.savePath]
            BookCoverTile(
                title = b.name ?: "?",
                cover = {
                    CoverImage(
                        b.cover,
                        log = state.log,
                        modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f),
                    )
                },
                // ponytail: proxy "chưa tải xong" — chưa phải unread thật;
                // lưu newCount riêng khi cần phân biệt chương mới
                pendingCount = b.chapList.orEmpty().count { !it.completed },
                subtitle =
                lastRead?.let { n ->
                    if (b.size > 0) subtitleText(n, b.size) else null
                },
                onClick = { state.openBook(b) },
            )
        }
    }
}

@Composable
private fun subtitleText(
    n: Int,
    size: Int,
): String = stringResource(Res.string.lib_reading_badge, n + 1, size)

/** Search field mono 256dp + nút làm mới — nhánh desktop (wide). Field chỉ hiện khi mở. */
@Composable
private fun SearchAndRefresh(
    search: String,
    onSearch: (String) -> Unit,
    searchOpen: Boolean,
    onSearchOpen: (Boolean) -> Unit,
    state: LibraryState,
) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        SortMenuButton(state)
        if (searchOpen) {
            OutlinedTextField(
                value = search,
                onValueChange = onSearch,
                placeholder = {
                    Text(
                        stringResource(Res.string.library_search_hint),
                        fontFamily = MonoFont(),
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                textStyle = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFont()),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(Res.string.cd_close_search),
                        modifier = Modifier.size(16.dp).clickable { onSearchOpen(false) },
                    )
                },
                modifier = Modifier.width(256.dp).height(52.dp),
            )
        } else {
            SearchToggleButton(searchOpen, onSearchOpen)
        }
        IconButton(onClick = {
            state.closeBook()
            state.refresh()
        }) {
            Icon(Icons.Filled.Refresh, contentDescription = stringResource(Res.string.cd_refresh))
        }
    }
}

/** Icon search thu gọn — click mở field tìm kiếm (ẩn text box mặc định). */
@Composable
private fun SearchToggleButton(
    searchOpen: Boolean,
    onSearchOpen: (Boolean) -> Unit,
) {
    IconButton(onClick = { onSearchOpen(!searchOpen) }) {
        Icon(
            if (searchOpen) Icons.Filled.Close else Icons.Filled.Search,
            contentDescription = stringResource(Res.string.cd_search_library),
        )
    }
}

/** Icon sort + menu chọn kiểu sắp xếp thư viện (mới thêm / mới đọc / tên). */
@Composable
private fun SortMenuButton(state: LibraryState) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(Res.string.cd_sort))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            LibrarySort.entries.forEach { s ->
                DropdownMenuItem(
                    text = { Text(if (s == state.sortBy) "✓ ${stringResource(s.labelRes)}" else stringResource(s.labelRes)) },
                    onClick = {
                        state.sortBy = s
                        state.refresh()
                        open = false
                    },
                )
            }
        }
    }
}
