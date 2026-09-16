package dev.haipham22.leechtext.ui.library
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.haipham22.leechtext.models.Chapter
import dev.haipham22.leechtext.models.Properties
import dev.haipham22.leechtext.resources.Res
import dev.haipham22.leechtext.resources.action_back
import dev.haipham22.leechtext.resources.action_cancel
import dev.haipham22.leechtext.resources.action_delete
import dev.haipham22.leechtext.resources.action_search
import dev.haipham22.leechtext.resources.book_chapter_list
import dev.haipham22.leechtext.resources.book_check_new
import dev.haipham22.leechtext.resources.book_delete_confirm
import dev.haipham22.leechtext.resources.book_delete_title
import dev.haipham22.leechtext.resources.book_download_chapters
import dev.haipham22.leechtext.resources.book_download_images
import dev.haipham22.leechtext.resources.book_edit_names
import dev.haipham22.leechtext.resources.book_export
import dev.haipham22.leechtext.resources.book_migrate_hint
import dev.haipham22.leechtext.resources.book_migrate_manual
import dev.haipham22.leechtext.resources.book_migrate_progress
import dev.haipham22.leechtext.resources.book_migrate_search_hint
import dev.haipham22.leechtext.resources.book_migrate_source
import dev.haipham22.leechtext.resources.book_migrate_title
import dev.haipham22.leechtext.resources.book_remove_from_library
import dev.haipham22.leechtext.resources.book_resume_download
import dev.haipham22.leechtext.resources.book_search_chapters
import dev.haipham22.leechtext.resources.book_share_export
import dev.haipham22.leechtext.resources.cd_bookmarked
import dev.haipham22.leechtext.resources.cd_more
import dev.haipham22.leechtext.resources.chapter_default_name
import dev.haipham22.leechtext.resources.lib_loading_toc
import dev.haipham22.leechtext.resources.pick_empty_toc
import dev.haipham22.leechtext.resources.read_resume
import dev.haipham22.leechtext.resources.sources_no_match
import dev.haipham22.leechtext.ui.PlatformBackHandler
import dev.haipham22.leechtext.ui.addbook.ChapterPickDialog
import dev.haipham22.leechtext.ui.addbook.compressToRange
import dev.haipham22.leechtext.ui.components.organisms.BookDetailBody
import dev.haipham22.leechtext.ui.components.organisms.BookDetailBodyOptions
import dev.haipham22.leechtext.ui.components.organisms.BookDetailBodySlots
import dev.haipham22.leechtext.ui.components.organisms.DownloadProgressLine
import dev.haipham22.leechtext.ui.model.UiText
import dev.haipham22.leechtext.ui.model.asString
import dev.haipham22.leechtext.ui.model.tone
import dev.haipham22.leechtext.ui.pickImageFile
import dev.haipham22.leechtext.ui.shareFile
import dev.haipham22.leechtext.ui.theme.MonoFont
import dev.haipham22.leechtext.util.removeDiacritics
import okio.FileSystem
import okio.Path.Companion.toPath
import org.jetbrains.compose.resources.stringResource

/**
 * Màn detail sách (Stitch 99ddb71e + yêu cầu owner): description ngay trên màn
 * (expandable), DS chương trang riêng (nút Xem danh sách chương), nút Tải mở popup
 * chọn range chương như app legacy — không đùng phát tải hết.
 */
@Composable
fun BookDetailScreen(
    state: LibraryState,
    modifier: Modifier = Modifier,
    queueState: dev.haipham22.leechtext.ui.queue.DownloadQueueState? = null,
) {
    val book = state.selected ?: return
    var showChapterList by remember(book.url) { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }
    var showChapterEdit by remember { mutableStateOf(false) }
    var showDownload by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Sách thêm nhanh (metadata-only) → fetch TOC nền khi mở; chương vẫn tải thủ công
    LaunchedEffect(book.url) { state.loadToc() }

    // ── Trang con: danh sách chương ──
    if (showChapterList) {
        ChapterListPage(state) { showChapterList = false }
        return
    }

    // Đổi nguồn — dialog sống ở root, gọi từ menu ⋮ (không phải nút chính)
    var migrateOpen by remember { mutableStateOf(false) }
    // Back hệ thống: đang mở DS chương → đóng list; không thì về Thư viện
    PlatformBackHandler {
        if (showChapterList) {
            showChapterList = false
        } else {
            state.closeBook()
        }
    }

    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerLowest)) {
        BookDetailTopBar(
            state = state,
            book = book,
            onShowDownload = { showDownload = true },
            onShowExport = { showExport = true },
            onShowChapterEdit = { showChapterEdit = true },
            onShowMigrate = { migrateOpen = true },
            onDelete = { showDeleteConfirm = true },
        )

        BookDetailBodySection(
            state = state,
            book = book,
            onShowDownload = { showDownload = true },
            onShowChapterList = { showChapterList = true },
            modifier = Modifier.weight(1f),
        )
    }

    // ── Popups ──
    // Đổi nguồn (owner 2026-08-26): chuyển truyện sang site khác, giữ nội dung
    // chương khớp tên — nằm trong menu ⋮ (2026-09-02: không phải nút chính)
    if (migrateOpen) {
        MigrateDialog(state) { migrateOpen = false }
    }
    // N2: chọn chương lẻ checkbox + range (thay dialog nhập range cũ)
    if (showDownload) {
        DownloadChaptersDialog(state, book, queueState) { showDownload = false }
    }
    if (showExport) {
        ExportBookDialog(state) { showExport = false }
    }
    if (showChapterEdit) {
        ChapterEditDialog(state) { showChapterEdit = false }
    }
    if (showDeleteConfirm) {
        DeleteConfirmDialog(
            book,
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                showDeleteConfirm = false
                state.removeFromLibrary()
            },
        )
    }
}

/**
 * Thân màn detail (BookDetailBody + các slot): progress khi tải, nút DS chương,
 * khu tải chương, footer hint.
 */
@Composable
private fun BookDetailBodySection(
    state: LibraryState,
    book: Properties,
    onShowDownload: () -> Unit,
    onShowChapterList: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BookDetailBody(
        book = book,
        onRead = { state.startReading() },
        options =
        BookDetailBodyOptions(
            readEnabled = !state.busy,
            showOngoingChip = true,
            onChangeCover = {
                pickImageFile("Chọn ảnh bìa") { path ->
                    if (path != null) state.replaceCover(path)
                }
            },
            readIcon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp)) },
        ),
        modifier = modifier,
        log = state.log,
        slots =
        BookDetailBodySlots(
            headerContent = {
                // ── Progress khi đang tải + tiến trình export ──
                if (state.busy) DownloadProgressLine(state.completed, state.total)
                state.exportProgress?.let { Text(it.asString(), style = MaterialTheme.typography.bodySmall) }
            },
            secondaryActions = {
                OutlinedButton(
                    onClick = onShowChapterList,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    // maxLines 1 — hai nút cùng hàng không lệch chiều cao khi label dài (owner 260902)
                    Text(stringResource(Res.string.book_chapter_list), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            },
            belowActionsContent = {
                BookDetailDownloadSection(state, book, onDownload = onShowDownload)
                BookDetailResumeReadingSection(state, book)
            },
            trailingContent = { DetailFooterHints(state) },
        ),
    )
}

/**
 * Dialog chọn chương để tải (N2) — queue path như PluginScreen: sách vào danh sách
 * đang tải thay vì download trực tiếp blocking UI (bug: bấm Tải không thấy trong queue).
 */
@Composable
private fun DownloadChaptersDialog(
    state: LibraryState,
    book: Properties,
    queueState: dev.haipham22.leechtext.ui.queue.DownloadQueueState?,
    onDismiss: () -> Unit,
) {
    ChapterPickDialog(
        chapters = book.chapList.orEmpty(),
        onConfirm = { indices ->
            val url = book.url
            if (queueState != null && !url.isNullOrEmpty()) {
                queueState.enqueue(listOf(url), compressToRange(indices))
            } else {
                state.downloadRange(compressToRange(indices))
            }
            onDismiss()
        },
        onDismiss = onDismiss,
        busy = state.busy,
        // TOC đang fetch nền (sách metadata-only) → hint "đang tải" thay vì báo lỗi
        emptyText =
        stringResource(
            if (state.loadingToc) Res.string.lib_loading_toc else Res.string.pick_empty_toc,
        ),
        // Chip lọc Chưa tải/Đã tải — check file raw/<id>.txt trên disk
        isDownloaded = { ch -> FileSystem.SYSTEM.exists((book.savePath ?: "").toPath() / "raw" / "${ch.id}.txt") },
    )
}

/** Hint dưới trang: TOC đang fetch nền (sách metadata-only) + message state (lỗi/success). */
@Composable
@Suppress("MultipleEmitters") // helper emit nhiều node có chủ đích — caller đặt trong scope Column/Row
private fun DetailFooterHints(state: LibraryState) {
    // ── Loading TOC hint ──
    if (state.loadingToc) {
        Text(
            stringResource(Res.string.lib_loading_toc),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    state.message?.let {
        Text(
            it.asString(),
            style = MaterialTheme.typography.bodySmall,
            // Lỗi (vd "Không có plugin khớp URL") phải nổi bật — cảnh báo sách mất
            // nguồn, không phải text thường (dogfood 260905)
            color =
            when (it.tone()) {
                UiText.Tone.ERROR -> MaterialTheme.colorScheme.error
                UiText.Tone.SUCCESS -> MaterialTheme.colorScheme.primary
                UiText.Tone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

/**
 * Dialog đổi nguồn: tìm tên truyện trên các nguồn đã cài plugin (GlobalSearchState
 * — search song song mọi plugin có searchGetter) → chọn kết quả → migrate giữ file
 * raw khớp tên chương (owner 260906: user không thể biết URL truyện trên site khác).
 * Fallback: dán URL thủ công cho link ngoài kết quả search.
 */
@Composable
private fun MigrateDialog(
    state: LibraryState,
    onDismiss: () -> Unit,
) {
    val book = state.selected ?: return
    var query by remember { mutableStateOf(book.name ?: "") }
    var manualUrl by remember { mutableStateOf("") }
    var showManual by remember { mutableStateOf(false) }
    val searchScope = androidx.compose.runtime.rememberCoroutineScope()
    val searchState =
        remember {
            dev.haipham22.leechtext.ui.addbook.GlobalSearchState(
                state.pluginManager.list(),
                searchScope,
                state.log,
            )
        }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(Modifier.fillMaxWidth().padding(8.dp)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.book_migrate_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(Res.string.book_migrate_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text(stringResource(Res.string.book_migrate_search_hint)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = { searchState.search(query) },
                        enabled = !searchState.searching && query.isNotBlank(),
                    ) { Text(stringResource(Res.string.action_search)) }
                }

                if (searchState.searching || searchState.progress != null) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    searchState.progress?.let {
                        Text(
                            stringResource(Res.string.book_migrate_progress, it),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Kết quả group theo nguồn — bấm 1 row = migrate sang nguồn đó
                MigrateSearchResults(searchState) { full ->
                    onDismiss()
                    state.migrateSource(full)
                }

                // Fallback URL thủ công — cho link search không trả về
                if (showManual) {
                    OutlinedTextField(
                        value = manualUrl,
                        onValueChange = { manualUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("https://…", fontFamily = MonoFont(), style = MaterialTheme.typography.labelSmall) },
                        textStyle = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFont()),
                        singleLine = true,
                    )
                    Button(
                        onClick = {
                            onDismiss()
                            state.migrateSource(manualUrl)
                        },
                        enabled = manualUrl.isNotBlank(),
                    ) { Text(stringResource(Res.string.book_migrate_source)) }
                } else {
                    androidx.compose.material3.TextButton(onClick = { showManual = true }) {
                        Text(stringResource(Res.string.book_migrate_manual))
                    }
                }

                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.action_cancel))
                }
            }
        }
    }
}

/** List kết quả migrate group theo nguồn — tách khỏi MigrateDialog cho S3776. */
@Composable
private fun MigrateSearchResults(
    searchState: dev.haipham22.leechtext.ui.addbook.GlobalSearchState,
    onPick: (String) -> Unit,
) {
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.heightIn(max = 320.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        searchState.results.forEach { (plugin, novels) ->
            item(key = "src-" + plugin.uuid) {
                Text(
                    plugin.name ?: plugin.source ?: "?",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = MonoFont(),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            items(novels.size, key = { "n$it-" + (plugin.uuid ?: "") }) { ni ->
                val novel = novels[ni]
                val link = novel.link ?: return@items
                val full = if (link.startsWith("http")) link else (novel.host ?: "") + link
                Text(
                    novel.name ?: link,
                    style = MaterialTheme.typography.bodySmall,
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onPick(full) }
                        .padding(vertical = 4.dp),
                )
            }
        }
    }
}

/** Confirm xóa sách — nút đỏ. Dismiss chỉ đóng; confirm do caller quyết (removeFromLibrary). */
@Composable
private fun DeleteConfirmDialog(
    book: Properties,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.book_delete_title), fontWeight = FontWeight.Bold) },
        text = { Text(stringResource(Res.string.book_delete_confirm, book.name ?: "?")) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) { Text(stringResource(Res.string.action_delete)) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
        },
    )
}

/** Top bar 56dp: back + title + overflow menu (tải / kiểm tra / xuất / ảnh / sửa tên / chia sẻ / xóa). */
@Composable
private fun BookDetailTopBar(
    state: LibraryState,
    book: Properties,
    onShowDownload: () -> Unit,
    onShowExport: () -> Unit,
    onShowChapterEdit: () -> Unit,
    onShowMigrate: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { state.closeBook() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.action_back))
        }
        Text(
            book.name ?: "?",
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(Res.string.cd_more))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.book_download_chapters)) },
                    onClick = {
                        menuOpen = false
                        onShowDownload()
                    },
                    enabled = !state.busy,
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.book_check_new)) },
                    onClick = {
                        menuOpen = false
                        state.checkNewChapters()
                    },
                    enabled = !state.busy,
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.book_export)) },
                    onClick = {
                        menuOpen = false
                        onShowExport()
                    },
                    enabled = !state.busy,
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.book_download_images)) },
                    onClick = {
                        menuOpen = false
                        state.downloadImages()
                    },
                    enabled = !state.busy,
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.book_edit_names)) },
                    onClick = {
                        menuOpen = false
                        onShowChapterEdit()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.book_migrate_source)) },
                    onClick = {
                        menuOpen = false
                        onShowMigrate()
                    },
                    enabled = !state.busy,
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.book_share_export)) },
                    onClick = {
                        menuOpen = false
                        state.lastExport?.let { shareFile(it) }
                    },
                    enabled = state.lastExport != null,
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.book_remove_from_library), color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        menuOpen = false
                        onDelete()
                    },
                )
            }
        }
    }
}

/** Hàng tải: chip tiếp tục tải dở (raw/ chưa đủ chapList). Tải chương mới nằm trong menu 3 chấm. */
@Composable
private fun BookDetailDownloadSection(
    state: LibraryState,
    book: Properties,
    onDownload: () -> Unit,
) {
    // ── Tiếp tục tải dở (dogfood 260902 mục 4: force-stop giữa khi tải → queue
    // in-memory mất im lặng). raw/ có file nhưng chưa đủ chapList → chip nhắc bấm
    // Tải để tiếp (engine resume bỏ qua file cũ). Đếm file qua IO, không block main.
    // ponytail: đếm theo số file .txt trong raw/ (không khớp từng chap id) — đủ cho
    // hint; khớp chính xác khi cần.
    var downloadedCount by remember(book.url) { mutableIntStateOf(0) }
    // key busy: tải xong (busy false) → đếm lại, ẩn nút khi đã đủ chương
    // (bug 260906: xong 301 chương nút "Tiếp tục tải" vẫn treo vì count stale)
    LaunchedEffect(book.url, book.chapList?.size, state.busy) {
        downloadedCount =
            runCatching {
                FileSystem.SYSTEM.list((book.savePath ?: "").toPath() / "raw").count { it.name.endsWith(".txt") }
            }.getOrDefault(0)
    }
    val totalChapters = book.chapList?.size ?: 0
    if (!state.busy && downloadedCount > 0 && downloadedCount < totalChapters) {
        OutlinedButton(
            onClick = onDownload,
            enabled = !state.busy,
            colors =
            ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.book_resume_download, totalChapters - downloadedCount))
        }
    }
}

/** Nút Đọc tiếp — vị trí đọc cuối (lastread.txt), bấm = resume. */
@Composable
private fun BookDetailResumeReadingSection(
    state: LibraryState,
    book: Properties,
) {
    // Đọc file qua LaunchedEffect (IO) — không block composition trên main
    var lastRead by remember(book.url) { mutableStateOf<Int?>(null) }
    LaunchedEffect(book.url) { lastRead = state.lastReadIndex() }
    // local val — smart cast trên delegated property là lỗi
    val n = lastRead
    if (n != null) {
        val lastCh = book.chapList?.getOrNull(n)
        OutlinedButton(
            onClick = { state.resumeReading() },
            enabled = !state.busy,
            colors =
            ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                // Bỏ prefix "Chương N: " trùng với "Ch. N" phía trước (dogfood 260902)
                stringResource(Res.string.read_resume, n + 1) +
                    (
                        lastCh?.chapName
                            ?.replace(Regex("^Chương\\s*\\d+\\s*:\\s*", RegexOption.IGNORE_CASE), "")
                            ?.takeIf { it.isNotBlank() }
                            ?.let { ": $it" } ?: ""
                        ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Trang danh sách chương — full-screen, top bar (back + title + search icon +
 * edit icon). Search ẩn mặc định — bấm icon mới hiện, có khoảng cách với list.
 * Tên chương hiển thị nguyên văn (không thêm prefix "Ch. n" — tên đã có sẵn
 * "Chương N: ..."). Click chương → mở reader. Edit icon → ChapterEditDialog.
 */
@Composable
private fun ChapterListPage(
    state: LibraryState,
    onBack: () -> Unit,
) {
    val book = state.selected ?: return
    var chapterQuery by remember(book.url) { mutableStateOf("") }
    var showSearch by remember(book.url) { mutableStateOf(false) }
    var showEdit by remember(book.url) { mutableStateOf(false) }
    // Lọc chỉ xem chương đã bookmark — xem lại bookmark của truyện.
    // Load bookmarks vào state để icon hiện đúng ngay khi vào list (chưa từng mở reader).
    var bookmarkOnly by remember(book.url) { mutableStateOf(false) }
    LaunchedEffect(book.url) { state.loadBookmarks() }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerLowest)) {
        // Top bar: back + title + search + edit icons
        ChapterListTopBar(
            title = book.name ?: "?",
            onBack = onBack,
            showSearch = showSearch,
            onToggleSearch = {
                showSearch = !showSearch
                if (!showSearch) chapterQuery = ""
            },
            bookmarkOnly = bookmarkOnly,
            onToggleBookmarks = {
                bookmarkOnly = !bookmarkOnly
            },
            onEdit = { showEdit = true },
        )

        // Search field — ẩn mặc định, bấm icon mới hiện; có padding tách khỏi list
        androidx.compose.animation.AnimatedVisibility(visible = showSearch) {
            OutlinedTextField(
                value = chapterQuery,
                onValueChange = { chapterQuery = it },
                placeholder = {
                    Text(stringResource(Res.string.book_search_chapters), fontFamily = MonoFont(), style = MaterialTheme.typography.labelSmall)
                },
                textStyle = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFont()),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        state.message?.let {
            Text(
                it.asString(),
                style = MaterialTheme.typography.bodySmall,
                // Lỗi mất nguồn (plugin không khớp URL) tô đỏ trong danh sách chương,
                // không lẫn màu nội dung (dogfood 260905)
                color =
                when (it.tone()) {
                    UiText.Tone.ERROR -> MaterialTheme.colorScheme.error
                    UiText.Tone.SUCCESS -> MaterialTheme.colorScheme.primary
                    UiText.Tone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        // Chapter list dense — tên nguyên văn, không prefix; search bỏ dấu
        // ("loan" khớp "Loạn Thế", "vo dao" khớp "Võ Đạo") + lọc bookmark
        val visible =
            book.chapList
                .orEmpty()
                .withIndex()
                .filter {
                    (!bookmarkOnly || it.index in state.bookmarks) &&
                        (
                            chapterQuery.isBlank() ||
                                removeDiacritics(it.value.chapName ?: "").contains(removeDiacritics(chapterQuery), ignoreCase = true)
                            )
                }

        if (visible.isEmpty() && chapterQuery.isNotBlank()) {
            Text(
                stringResource(Res.string.sources_no_match),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
        // "Đã tải" theo file raw thật — flag completed stale sau reinstall/xóa data.
        // Liệt kê thư mục raw qua IO (LaunchedEffect), không block main trong composition
        var downloadedIds by remember(book.url) { mutableStateOf(emptySet<String>()) }
        LaunchedEffect(book.url) { downloadedIds = state.downloadedChapterIds() }
        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
            items(visible.size) { vi ->
                val (i, ch) = visible[vi]
                ChapterListRow(i, ch, state, ch.id in downloadedIds)
            }
        }
    }

    // Sửa tên chương — icon bút chì trong top bar
    if (showEdit) {
        ChapterEditDialog(state) { showEdit = false }
    }
}

/** Top bar trang danh sách chương: back + title + tìm kiếm + sửa tên chương. */
@Composable
private fun ChapterListTopBar(
    title: String,
    onBack: () -> Unit,
    showSearch: Boolean,
    onToggleSearch: () -> Unit,
    bookmarkOnly: Boolean,
    onToggleBookmarks: () -> Unit,
    onEdit: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.action_back))
        }
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onToggleSearch) {
            Icon(
                Icons.Filled.Search,
                contentDescription = stringResource(Res.string.book_search_chapters),
                tint =
                if (showSearch) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        // Lọc chỉ hiện chương đã bookmark — xem lại bookmark của truyện
        IconButton(onClick = onToggleBookmarks) {
            Icon(
                if (bookmarkOnly) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                contentDescription = stringResource(Res.string.cd_bookmarked),
                tint =
                if (bookmarkOnly) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Filled.Edit, contentDescription = stringResource(Res.string.book_edit_names))
        }
    }
}

/** 1 dòng chương (divider + row): chấm trạng thái + tên + icon lỗi/bookmark/xong. */
@Composable
private fun ChapterListRow(
    i: Int,
    ch: Chapter,
    state: LibraryState,
    downloaded: Boolean,
) {
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { state.openChapter(i) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!ch.completed) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            } else {
                Box(Modifier.size(8.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(
                ch.chapName ?: stringResource(Res.string.chapter_default_name, i + 1),
                style = MaterialTheme.typography.bodySmall,
                color =
                if (ch.completed) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            ChapterStatusIcons(i, ch, state, downloaded)
        }
    }
}

/** Icon trạng thái cuối dòng chương: lỗi / bookmark / đã tải xong. */
@Composable
@Suppress("MultipleEmitters") // helper emit nhiều node có chủ đích — caller đặt trong scope Column/Row
private fun ChapterStatusIcons(
    i: Int,
    ch: Chapter,
    state: LibraryState,
    downloaded: Boolean,
) {
    if (ch.error) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error),
        )
    }
    if (i in state.bookmarks) {
        Icon(
            Icons.Filled.Bookmark,
            contentDescription = stringResource(Res.string.cd_bookmarked),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp),
        )
    }
    if (downloaded && !ch.error) {
        Icon(
            Icons.Filled.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(14.dp),
        )
    }
}
