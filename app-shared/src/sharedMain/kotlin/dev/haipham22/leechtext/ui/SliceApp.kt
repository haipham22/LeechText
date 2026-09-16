package dev.haipham22.leechtext.ui
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Source
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import dev.haipham22.leechtext.di.WithAppKoin
import dev.haipham22.leechtext.resources.Res
import dev.haipham22.leechtext.resources.settings_title
import dev.haipham22.leechtext.resources.tab_history
import dev.haipham22.leechtext.resources.tab_library
import dev.haipham22.leechtext.resources.tab_more
import dev.haipham22.leechtext.resources.tab_sources
import dev.haipham22.leechtext.resources.tab_updates
import dev.haipham22.leechtext.ui.components.molecules.AppBottomBar
import dev.haipham22.leechtext.ui.components.molecules.AppRail
import dev.haipham22.leechtext.ui.components.organisms.DownloadQueuePanel
import dev.haipham22.leechtext.ui.history.HistoryScreen
import dev.haipham22.leechtext.ui.library.LibraryScreen
import dev.haipham22.leechtext.ui.model.QueueItemUi
import dev.haipham22.leechtext.ui.model.QueuePanelCallbacks
import dev.haipham22.leechtext.ui.model.RailItem
import dev.haipham22.leechtext.ui.queue.DownloadQueueState
import dev.haipham22.leechtext.ui.queue.QueueItem
import dev.haipham22.leechtext.ui.settings.SettingsScreen
import dev.haipham22.leechtext.ui.sources.PluginScreen
import dev.haipham22.leechtext.ui.theme.LeechTextTheme
import org.jetbrains.compose.resources.stringResource

/**
 * Root composable của app — Decompose + Koin: state app-scope resolve qua Koin
 * (AppModule), root do entry point tạo (DefaultComponentContext/retainedComponent).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SliceApp(
    root: RootComponent,
    modifier: Modifier = Modifier,
    onCrashReportChange: (Boolean) -> Unit = {},
) {
    // N5 i18n: ngôn ngữ từ setting.json → override locale string resources cho
    // toàn app; đổi ngôn ngữ trong Cài đặt cập nhật state này → mọi stringResource
    // recompose theo ngôn ngữ mới ngay.
    var language by remember {
        mutableStateOf(
            dev.haipham22.leechtext.util.SettingsRepository
                .load()
                .language
                .takeIf { it == "en" } ?: "vi",
        )
    }
    WithAppKoin {
        ProvideAppLanguage(language) {
            SliceAppContent(root, modifier, onCrashReportChange) {
                language = if (it == "en") "en" else "vi"
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SliceAppContent(
    root: RootComponent,
    modifier: Modifier = Modifier,
    onCrashReportChange: (Boolean) -> Unit = {},
    onLanguageChange: (String) -> Unit = {},
) {
    val tab = root.tab
    // Root back: tab phụ quay về Thư viện; màn con đăng ký handler riêng phía dưới.
    PlatformBackHandler(enabled = tab != 0) { root.tab = 0 }
    val queueState = root.queue
    // Đang đọc (reader/preview) → ẩn rail/bottom bar/queue panel — thay ReaderFullscreen global
    val libStack by root.library.stack.subscribeAsState()
    val srcStack by root.plugin.stack.subscribeAsState()
    val fullscreen =
        libStack.active.configuration is dev.haipham22.leechtext.ui.library.LibraryConfig.Reader ||
            srcStack.active.configuration is dev.haipham22.leechtext.ui.sources.SourceConfig.Preview
    // Tap vùng trống → clear focus (Android: hạ bàn phím ảo)
    val focusManager = LocalFocusManager.current

    LeechTextTheme {
        // Adapter QueueItem (jvmMain) → QueueItemUi (common, không biết engine)
        fun toUi(i: QueueItem) = QueueItemUi(
            id = i.url,
            name = i.name,
            status = i.status,
            completed = i.completed,
            total = i.total,
            done = i.done,
            failed = i.failed,
            hasBook = i.book != null,
            paused = i.paused,
            coverUrl = i.cover,
        )
        val queueUi = queueState.items.map(::toUi)
        val queueItemById = queueState.items.associateBy { it.url }
        val queue = QueueUi(queueState, queueUi, queueItemById)

        // Surface gốc desktop: set contentColor theo theme (dark mode) — desktop
        // không có Scaffold → LocalContentColor default ĐEN, chữ reader (color =
        // Unspecified) thành đen-trên-đen khi nền Tối (dogfood 260905)
        androidx.compose.material3.Surface(
            modifier = modifier,
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            BoxWithConstraints(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { focusManager.clearFocus() })
                    },
            ) {
                if (maxWidth >= 720.dp) {
                    // Desktop (master "Tachidesk Light"): rail trái + content + queue panel cố định
                    SliceAppWide(
                        tab,
                        fullscreen,
                        root,
                        queue,
                        onCrashReportChange,
                        onLanguageChange,
                    ) { root.tab = it }
                } else {
                    // Compact (mobile master): bottom nav 5 tab — Thư viện / Cập nhật / Lịch sử / Nguồn / Khác
                    SliceAppCompact(
                        tab,
                        fullscreen,
                        root,
                        queue,
                        onCrashReportChange,
                        onLanguageChange,
                    ) { root.tab = it }
                }
            }
        } // Surface gốc
    }
}

/** 4 mục nav chính — dùng chung rail desktop + bottom bar mobile, cùng mapping tab index. */
@Composable
private fun navRailItems(activeCount: Int): List<RailItem> = listOf(
    RailItem(stringResource(Res.string.tab_library), Icons.AutoMirrored.Filled.MenuBook),
    RailItem(stringResource(Res.string.tab_updates), Icons.Filled.Download, badge = activeCount),
    RailItem(stringResource(Res.string.tab_history), Icons.Filled.History),
    RailItem(stringResource(Res.string.tab_sources), Icons.Filled.Source),
)

/** Layout rộng (desktop/plate): rail trái + content + queue panel cố định phải. */
@Composable
private fun SliceAppWide(
    tab: Int,
    fullscreen: Boolean,
    root: RootComponent,
    queue: QueueUi,
    onCrashReportChange: (Boolean) -> Unit,
    onLanguageChange: (String) -> Unit,
    onSelectTab: (Int) -> Unit,
) {
    androidx.compose.runtime.CompositionLocalProvider(
        dev.haipham22.leechtext.ui.library.LocalHasQueuePanel provides true,
    ) {
        Row(Modifier.fillMaxSize()) {
            if (!fullscreen) {
                AppRail(
                    items = navRailItems(queue.activeCount),
                    bottomItem = RailItem(stringResource(Res.string.settings_title), Icons.Filled.Settings),
                    selected = if (tab in 0..3) tab else -1,
                    onSelect = onSelectTab,
                    onBottomClick = { onSelectTab(4) },
                )
            }
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .statusBarsPadding()
                    .background(MaterialTheme.colorScheme.background),
            ) {
                SliceTabs(tab, root, queue, onCrashReportChange, onLanguageChange) { onSelectTab(0) }
            }
            // Panel chỉ hiện khi có item — rỗng thì ẩn hẳn, content full width
            if (queue.items.isNotEmpty() && !fullscreen) {
                androidx.compose.material3.VerticalDivider()
                DownloadQueuePanel(
                    items = queue.items,
                    callbacks =
                    QueuePanelCallbacks(
                        onRetry = { ui -> queue.itemById[ui.id]?.let(queue.state::retry) },
                        onDismiss = { ui ->
                            queue.itemById[ui.id]?.let {
                                queue.state.dismiss(it)
                                root.library.refresh()
                            }
                        },
                        onClearAll = { queue.state.clearFinished() },
                        onOpenBook = { _ -> onSelectTab(0) },
                        onPause = { ui -> queue.itemById[ui.id]?.let(queue.state::pause) },
                        onResume = { ui -> queue.itemById[ui.id]?.let(queue.state::resume) },
                    ),
                    modifier = Modifier.width(320.dp),
                    coverFor = { ui -> QueueCover(ui, root.log) },
                )
            }
        }
    } // CompositionLocalProvider queue panel
}

/** Layout hẹp (mobile): bottom nav 5 tab — Thư viện / Cập nhật / Lịch sử / Nguồn / Khác. */
@Composable
private fun SliceAppCompact(
    tab: Int,
    fullscreen: Boolean,
    root: RootComponent,
    queue: QueueUi,
    onCrashReportChange: (Boolean) -> Unit,
    onLanguageChange: (String) -> Unit,
    onSelectTab: (Int) -> Unit,
) {
    Scaffold(
        // iOS edge-to-edge: đồng bộ màu Scaffold với header màn (surfaceContainerLowest)
        // để status bar không ra band 2 tông — status bar pad 1 LẦN ở Box content dưới
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            // Đang đọc truyện → ẩn bottom nav (reader tự có chrome)
            if (!fullscreen) {
                AppBottomBar(
                    items =
                    navRailItems(queue.activeCount) +
                        RailItem(stringResource(Res.string.tab_more), Icons.Filled.Settings),
                    selected = tab,
                    onSelect = onSelectTab,
                )
            }
        },
    ) { padding ->
        // statusBarsPadding DUY NHẤT ở đây — màn con không tự pad nữa
        Box(Modifier.fillMaxSize().padding(padding).statusBarsPadding()) {
            SliceTabs(tab, root, queue, onCrashReportChange, onLanguageChange) { onSelectTab(0) }
        }
    }
}

/** Queue hiển thị — state + view data gom chung để giảm param truyền xuống SliceTabs. */

/** Cover 2:3 trong queue row — placeholder khi chưa fetch xong info. */
@Composable
private fun QueueCover(
    item: QueueItemUi,
    log: dev.haipham22.leechtext.log.EngineLogger,
) {
    dev.haipham22.leechtext.ui.components.atoms.CoverImage(
        item.coverUrl,
        log = log,
        modifier = Modifier.width(28.dp).height(42.dp),
    )
}

private class QueueUi(
    val state: DownloadQueueState,
    val items: List<QueueItemUi>,
    val itemById: Map<String, QueueItem>,
) {
    /** Số item chưa xong — badge nav cập nhật. */
    val activeCount: Int get() = state.items.count { !it.done }
}

/** Nội dung theo tab — dùng chung desktop/mobile, mapping index thống nhất 0..4. */
@Composable
private fun SliceTabs(
    tab: Int,
    root: RootComponent,
    queue: QueueUi,
    onCrashReportChange: (Boolean) -> Unit,
    onLanguageChange: (String) -> Unit,
    onOpenBook: () -> Unit,
) {
    when (tab) {
        0 -> {
            LibraryScreen(state = root.library, queueState = queue.state)
        }

        1 -> {
            DownloadQueuePanel(
                items = queue.items,
                callbacks =
                QueuePanelCallbacks(
                    onRetry = { ui -> queue.itemById[ui.id]?.let(queue.state::retry) },
                    onDismiss = { ui ->
                        queue.itemById[ui.id]?.let {
                            queue.state.dismiss(it)
                            root.library.refresh()
                        }
                    },
                    onClearAll = { queue.state.clearFinished() },
                    onOpenBook = { onOpenBook() },
                    onPause = { ui -> queue.itemById[ui.id]?.let(queue.state::pause) },
                    onResume = { ui -> queue.itemById[ui.id]?.let(queue.state::resume) },
                ),
                modifier = Modifier.fillMaxSize(),
                coverFor = { ui -> QueueCover(ui, root.log) },
            )
        }

        2 -> {
            HistoryScreen(
                state = root.library,
                onOpenBook = { b ->
                    root.library.openBook(b)
                    root.library.resumeReading()
                    onOpenBook()
                },
            )
        }

        3 -> {
            PluginScreen(state = root.plugin, queueState = queue.state)
        }

        else -> {
            SettingsScreen(onCrashReportChange = onCrashReportChange, onLanguageChange = onLanguageChange, log = root.log)
        }
    }
}
