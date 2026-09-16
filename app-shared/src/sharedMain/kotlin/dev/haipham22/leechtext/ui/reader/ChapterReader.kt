package dev.haipham22.leechtext.ui.reader
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.areAnyPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.haipham22.leechtext.resources.Res
import dev.haipham22.leechtext.resources.action_cancel
import dev.haipham22.leechtext.resources.action_save
import dev.haipham22.leechtext.resources.chapter_default_name
import dev.haipham22.leechtext.resources.lib_chapter_not_downloaded
import dev.haipham22.leechtext.resources.literata_400
import dev.haipham22.leechtext.resources.noticia_text_400
import dev.haipham22.leechtext.resources.reader_bookmark_saved
import dev.haipham22.leechtext.resources.reader_chapter_content
import dev.haipham22.leechtext.resources.reader_download_this_chapter
import dev.haipham22.leechtext.resources.reader_font_default
import dev.haipham22.leechtext.resources.roboto_400
import dev.haipham22.leechtext.resources.roboto_condensed_400
import dev.haipham22.leechtext.ui.PlatformBackHandler
import dev.haipham22.leechtext.ui.library.LibraryState
import dev.haipham22.leechtext.ui.model.UiText
import dev.haipham22.leechtext.ui.model.asString
import dev.haipham22.leechtext.ui.model.tone
import dev.haipham22.leechtext.ui.togglePlatformFullscreen
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Font đọc: generic FontFamily (serif/sans/mono) + font bundled full glyph
 * tiếng Việt (literata ≈ Bookerly, roboto, roboto_condensed, noticia).
 * Key persist setting.json (other.reader_font).
 */
val ReaderFontKeys =
    listOf("", "serif", "sans", "mono", "literata", "roboto", "roboto_condensed", "noticia")

/** Key không hợp lệ (file setting.json cũ/tay sửa) → fallback Default. */
@Composable
fun readerFontFamily(key: String): FontFamily = when {
    key.startsWith("file:") -> userFontFamily(key.removePrefix("file:")) ?: FontFamily.Default

    else ->
        when (key) {
            "serif" -> FontFamily.Serif
            "sans" -> FontFamily.SansSerif
            "mono" -> FontFamily.Monospace
            "literata" -> FontFamily(org.jetbrains.compose.resources.Font(Res.font.literata_400, FontWeight.Normal))
            "roboto" -> FontFamily(org.jetbrains.compose.resources.Font(Res.font.roboto_400, FontWeight.Normal))
            "roboto_condensed" -> FontFamily(org.jetbrains.compose.resources.Font(Res.font.roboto_condensed_400, FontWeight.Normal))
            "noticia" -> FontFamily(org.jetbrains.compose.resources.Font(Res.font.noticia_text_400, FontWeight.Normal))
            else -> FontFamily.Default
        }
}

/** Key font kế tiếp trong vòng cycle (Settings screen) — gồm cả font user drop vào fonts dir. */
fun nextReaderFont(key: String): String {
    val all = ReaderFontKeys + UserFonts.list().map { "file:$it" }
    return all[(all.indexOf(key).takeIf { it >= 0 } ?: 0).let { (it + 1) % all.size }]
}

/** Nhãn hiển thị của key font — localizable riêng chuỗi Default, tên còn lại giữ nguyên. */
@Composable
fun readerFontLabel(key: String): String = when {
    key.startsWith("file:") -> key.removePrefix("file:").substringBeforeLast('.')

    else -> when (key) {
        "" -> stringResource(Res.string.reader_font_default)
        "roboto_condensed" -> "Roboto Condensed"
        "noticia" -> "Noticia Text"
        else -> key.replaceFirstChar { it.uppercase() }
    }
}

/**
 * Raw file giữ nguyên HTML (cho EPUB export) — reader strip khi hiển thị:
 * block tags → xuống dòng 2 lần (tách đoạn), strip tag còn lại, decode entities.
 * Trash rules áp 2 lượt: trên RAW (trước strip — như export Text.kt, cho rule
 * nhắm thẳng HTML như <p style="display:none">spam) và sau strip (cho rule
 * plain-text khớp đúng text hiển thị).
 */
internal fun toParagraphs(
    raw: String,
    trashRules: List<dev.haipham22.leechtext.models.Trash> = emptyList(),
): List<String> {
    // Lượt 1 — trên raw, cùng logic cleanTextContent Text.kt (unescape \n \r \t)
    var text = applyTrashRules(raw, trashRules)
    text =
        text
            .replace(Regex("<(p|div|br)\\b[^>]*>", RegexOption.IGNORE_CASE), "\n\n")
            .replace(Regex("<[^>]+>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
    // Lượt 2 — trên text đã strip (rule plain-text theo đúng text nhìn thấy)
    text = applyTrashRules(text, trashRules)
    return text.split("\n\n").map { it.trim() }.filter { it.isNotEmpty() }
}

private fun applyTrashRules(
    text: String,
    trashRules: List<dev.haipham22.leechtext.models.Trash>,
): String {
    var result = text
    for (rule in trashRules) {
        if (rule.replace) {
            val src =
                rule.src
                    ?.replace("\\n", "\n")
                    ?.replace("\\r", "\r")
                    ?.replace("\\t", "\t") ?: continue
            val to =
                rule.to
                    ?.replace("\\n", "\n")
                    ?.replace("\\r", "\r")
                    ?.replace("\\t", "\t") ?: ""
            result = result.replace(src.toRegex(), to)
        }
    }
    return result
}

/**
 * Reader chương theo master Stitch "Đọc truyện" (0b89d05d): header mỏng (back +
 * tên sách + Ch. n mono + nút sửa), hairline progress theo scroll, body Hanken
 * 16sp/1.6 padding 24, bottom bar ‹ n/total ›. Chế độ sửa = TextField thay body.
 */

/** Display settings reader (font/size/bg/fg/line-height) — 1 lần load, save gom 1 chỗ. */
internal class ReaderDisplay {
    var fontSize by androidx.compose.runtime.mutableIntStateOf(16)
    var fontKey by mutableStateOf("")
    var bgKey by mutableStateOf("light")
    var fgKey by mutableStateOf("")
    var lineHeight by androidx.compose.runtime.mutableFloatStateOf(1.6f)
    var scrollMode by mutableStateOf("vertical")

    fun save(
        size: Int = fontSize,
        font: String = fontKey,
        bg: String = bgKey,
        fg: String = fgKey,
        lh: Float = lineHeight,
    ) {
        fontSize = size
        fontKey = font
        bgKey = bg
        fgKey = fg
        lineHeight = lh
        dev.haipham22.leechtext.util.SettingsRepository.save(
            dev.haipham22.leechtext.util.SettingsRepository
                .load()
                .copy(readerFontSize = size, readerFont = font, readerBg = bg, readerFg = fg, readerLineHeight = lh),
        )
    }

    companion object {
        fun load(): ReaderDisplay = ReaderDisplay().apply {
            dev.haipham22.leechtext.util.SettingsRepository.load().let {
                fontSize = it.readerFontSize.coerceIn(12, 28)
                fontKey = it.readerFont
                bgKey = it.readerBg
                fgKey = it.readerFg
                // Giá trị cũ ngoài range (setting.json cũ) → thumb vẽ lệch mép track
                lineHeight = it.readerLineHeight.coerceIn(1.2f, 2.4f)
                scrollMode = it.readerScrollMode
            }
        }
    }
}

/**
 * Tap toggle chrome; di chuột (hover, không bấm — kéo/cuộn touch không tính) hiện lại
 * chrome: trước đây chỉ toggle bằng tap, ẩn rồi không có cách restore nào khác
 * (breaker 260906 kẹt reader). Move phải rời khỏi điểm ẩn (≥32px): AWT/hub sinh
 * Move trùng vị trí ngay sau release, không chặn thì tap-ẩn bị hover-restore đánh
 * bật ngay lập tức.
 */
private fun Modifier.chromeToggleGestures(
    onToggle: (Offset) -> Unit,
    onHoverRestore: () -> Unit,
    hideTapPos: Offset,
): Modifier = this
    .pointerInput(Unit) {
        awaitEachGesture {
            while (true) {
                val e = awaitPointerEvent()
                if (e.type == PointerEventType.Move && !e.buttons.areAnyPressed) {
                    val p = e.changes.firstOrNull()?.position
                    if (p != null && (p - hideTapPos).getDistance() > 32f) {
                        onHoverRestore()
                    }
                }
            }
        }
    }
    .pointerInput(Unit) {
        detectTapGestures { onToggle(it) }
    }

/** % đọc theo scroll — 1 title + số đoạn (\n\n): (vị trí item đầu + 1) / tổng. */
private fun readProgress(text: String, firstVisibleItemIndex: Int): Float {
    val items = 1 + text.split("\n\n").count()
    return if (items <= 1) 0f else (firstVisibleItemIndex + 1).toFloat() / items
}

// ModifierNotUsedAtRoot: modifier được áp ở Column trong content lambda (bọc key/Surface
// để đổi theme live) — chuyển lên root hiện tại buộc tách lại cả cây chrome, chưa đáng.
@Suppress("ModifierNotUsedAtRoot", "LongMethod")
@Composable
fun ChapterReader(
    state: LibraryState,
    modifier: Modifier = Modifier,
) {
    val book = state.selected ?: return
    val idx = state.editingIndex ?: return
    val chapter = book.chapList?.getOrNull(idx) ?: return
    var editMode by remember(idx) { mutableStateOf(false) }
    var showAddRule by remember(idx) { mutableStateOf(false) }
    // Trash rules là state — thêm rule mới (dialog bên dưới) gán lại → áp ngay
    var trashRules by remember(book.url) {
        mutableStateOf(
            dev.haipham22.leechtext.util.SettingsRepository
                .load()
                .trash,
        )
    }
    // Bookmark chương — load khi mở reader
    androidx.compose.runtime.LaunchedEffect(book.url) { state.loadBookmarks() }

    val display = remember { ReaderDisplay.load() }

    // Vào reader = ẩn chrome (kiểu Kindle) — tap màn hình để hiện/ẩn header+bottom.
    // Reader mở cũng ẩn chrome app (rail/bottom bar/queue) — không cần fullscreen OS.
    var fullscreen by remember { mutableStateOf(true) }
    // Back hệ thống = đóng reader về màn trước (không thoát app)
    PlatformBackHandler {
        state.closeChapter()
    }
    var chromeVisible by remember { mutableStateOf(false) }
    fun toggleFullscreen() {
        fullscreen = !fullscreen
        chromeVisible = !fullscreen
        togglePlatformFullscreen()
    }

    var showDisplay by remember { mutableStateOf(false) }
    var showChapterPicker by remember { mutableStateOf(false) }

    // Toast xác nhận bookmark (dogfood 260906 — bấm xong chỉ thấy icon đổ màu, mù nghĩa)
    var bookmarkSaved by remember { mutableStateOf(false) }

    // Điểm vừa ẩn chrome bằng tap — Move phải rời khỏi đây (≥32px) mới hiện lại:
    // AWT/hub sinh Move trùng vị trí ngay sau release, không chặn thì tap-ẩn bị
    // hover-restore đánh bật ngay lập tức (dogfood 260906)
    var hideTapPos by remember { mutableStateOf(Offset.Zero) }

    // Scroll đầu chương (0) — đổi chương (idx) reset về đầu
    val listState =
        remember(idx) {
            androidx.compose.foundation.lazy
                .LazyListState()
        }

    // Paged mode: hộp giữ (page, total) mức reader — phím lật qua đây, body đồng bộ
    // pager thật (animateScrollToPage) nằm trong ReaderPagedBody nơi biết số trang
    val pagedState = remember(idx) { PagedState() }
    val paged = display.scrollMode == "paged"

    /** Lật trang paged: trong chương → page ±; đầu/cuối chương → chương kề (đổi chương reset page). */
    fun turnPage(delta: Int) {
        val next = pagedState.page + delta
        if (next < 0) {
            if (idx > 0) state.changeChapter(-1)
        } else if (next >= pagedState.total) {
            if (idx < (book.chapList?.size ?: 1) - 1) state.changeChapter(1)
        } else {
            pagedState.page = next
        }
    }

    // ── Auto-scroll (chế độ dọc) ── enabled không persist; speed lưu setting.json
    val auto = remember { AutoScroller.load() }
    // Dialog mở → dừng cuộn (user đang chỉnh), đóng chạy lại theo key restart effect
    if (paged) auto.enabled = false // paged không auto-scroll — tắt khi đổi mode
    AutoScrollLoop(auto, state, listState, idx, book.chapList?.size ?: 0, paused = autoPaused(editMode, paged, showDisplay, showChapterPicker, showAddRule))

    val scheme = ReaderBg.fromKey(display.bgKey).scheme()
    // Focus cho phím tắt — reader không có node focusable nào thì onPreviewKeyEvent
    // không bao giờ nhận key (Escape/←/→ chết, breaker 260906)
    val keyFocus = remember { FocusRequester() }
    LaunchedEffect(idx) { runCatching { keyFocus.requestFocus() } }
    val scope = rememberCoroutineScope()
    val content: @Composable () -> Unit = {
        ReaderRootBox(
            modifier = modifier,
            keyFocus = keyFocus,
            // Đang sửa nội dung — Escape không thoát, mất text đang edit; phím là của TextField
            ctx = ReaderKeyContext(idx, book.chapList?.size ?: 0, !editMode, !editMode, auto.enabled, paged),
            viewport = { listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset },
            actions =
            ReaderKeyActions(
                onDelta = { state.changeChapter(it) },
                onExit = { state.editingIndex = null },
                onScroll = { dy -> scope.launch { listState.animateScrollBy(dy.toFloat()) } },
                onToggleAutoScroll = { auto.toggle() },
                onSpeedDelta = { auto.setSpeed(auto.speed + it) },
                onPageTurn = ::turnPage,
            ),
        ) {
            val ui =
                remember(display, auto, pagedState, listState, trashRules) {
                    ReaderUiState(display, auto, pagedState, listState, trashRules)
                }
            ReaderContent(
                state,
                ReaderChapterContent(
                    chapter,
                    idx,
                    paragraphs = remember(state.chapterText, trashRules) { toParagraphs(state.chapterText, trashRules) },
                ),
                ui,
                ReaderUiFlags(editMode, fullscreen, chromeVisible),
                ReaderUiCallbacks(
                    onToggleFullscreen = ::toggleFullscreen,
                    onEditChange = { editMode = it },
                    gestures =
                    ChromeGestures(
                        onToggle = {
                            chromeVisible = !chromeVisible
                            if (!chromeVisible) hideTapPos = it
                        },
                        onHover = { chromeVisible = true },
                        hideTapPos = hideTapPos,
                    ),
                ),
                readerDialogVisibility(
                    showAddRule,
                    showDisplay,
                    showChapterPicker,
                    setShowAddRule = { showAddRule = it },
                    setShowDisplay = { showDisplay = it },
                    setShowPicker = { showChapterPicker = it },
                    onBookmarkSaved = { bookmarkSaved = true },
                ),
            ) {
                ReaderBottomBar(
                    idx,
                    book.size,
                    state,
                    chapter.chapName ?: stringResource(Res.string.chapter_default_name, idx + 1),
                    onPickChapter = { showChapterPicker = true },
                )
            }
            BookmarkToast(bookmarkSaved, idx, onTimeout = { bookmarkSaved = false })
        }
    }
    // key theo lựa chọn hiển thị — MaterialTheme M3 remember scheme đầu rồi mutate tại chỗ,
    // consumer đọc LocalColorScheme không bị invalidate → nội dung giữ màu cũ tới khi mở lại
    // chương (dogfood 260902). key() vứt subtree cũ, dựng lại với scheme mới.
    key(display.bgKey to display.fgKey) {
        // Surface bọc thêm để RESET LocalContentColor — nếu không thì Text/Icon kế thừa
        // contentColor của Scaffold app (light) → dark-on-dark khi nền Tối
        androidx.compose.material3.Surface(
            color = scheme.background,
            contentColor = scheme.onSurface,
        ) {
            MaterialTheme(colorScheme = scheme, content = content)
        }
    }
}

/** Auto-scroll tạm dừng khi đang sửa/mở dialog/ở chế độ paged. */
private fun autoPaused(
    editMode: Boolean,
    paged: Boolean,
    showDisplay: Boolean,
    showChapterPicker: Boolean,
    showAddRule: Boolean,
): Boolean = editMode || paged || showDisplay || showChapterPicker || showAddRule

/** Builder visibility + callbacks dialog reader — tách khỏi ChapterReader cho S3776. */
private fun readerDialogVisibility(
    showAddRule: Boolean,
    showDisplay: Boolean,
    showChapterPicker: Boolean,
    setShowAddRule: (Boolean) -> Unit,
    setShowDisplay: (Boolean) -> Unit,
    setShowPicker: (Boolean) -> Unit,
    onBookmarkSaved: () -> Unit,
): ReaderDialogVisibility = ReaderDialogVisibility(
    showAddRule = showAddRule,
    onDismissAddRule = { setShowAddRule(false) },
    showDisplay = showDisplay,
    onDismissDisplay = { setShowDisplay(false) },
    showChapterPicker = showChapterPicker,
    onDismissPicker = { setShowPicker(false) },
    onShowDisplay = { setShowDisplay(true) },
    onAddRule = { setShowAddRule(true) },
    onPickChapter = { setShowPicker(true) },
    onBookmarkSaved = onBookmarkSaved,
)

/** Toast xác nhận bookmark — overlay đáy màn, tự tắt sau 1.8s. */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.BookmarkToast(
    visible: Boolean,
    idx: Int,
    onTimeout: () -> Unit,
) {
    if (visible) {
        val timeout = rememberUpdatedState(onTimeout)
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(1800)
            timeout.value()
        }
        Surface(
            color = Color.Black.copy(alpha = 0.72f),
            contentColor = Color.White,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 72.dp),
        ) {
            Text(
                stringResource(Res.string.reader_bookmark_saved, idx + 1),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

/** Box gốc: nền + focus + phím điều hướng — tách khỏi ChapterReader cho S3776. */
@Composable
private fun ReaderRootBox(
    keyFocus: FocusRequester,
    ctx: ReaderKeyContext,
    viewport: () -> Int,
    actions: ReaderKeyActions,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
) {
    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .focusRequester(keyFocus)
            .focusable()
            .chapterNavKeys(ctx = ctx, viewport = viewport, actions = actions),
        content = content,
    )
}

/** Callbacks tap/hover toggle chrome fullscreen + điểm tap ẩn (gom param, S107). */
internal class ChromeGestures(
    val onToggle: (Offset) -> Unit,
    val onHover: () -> Unit,
    val hideTapPos: Offset,
)

/** State UI reader dùng chung cho chrome + body (gom param cho ReaderContent/ReaderBody). */
internal class ReaderUiState(
    val display: ReaderDisplay,
    val auto: AutoScroller,
    val pagedState: PagedState,
    val listState: androidx.compose.foundation.lazy.LazyListState,
    var trashRules: List<dev.haipham22.leechtext.models.Trash>,
) {
    val paged: Boolean get() = display.scrollMode == "paged"
    val fgColor: Color? get() = readerFgColor(display.fgKey, ReaderBg.fromKey(display.bgKey).scheme().background)

    /** Progress dọc — % theo vị trí item đầu visible. */
    @Composable
    fun progress(state: LibraryState): Float {
        val progress by remember {
            derivedStateOf { readProgress(state.chapterText, listState.firstVisibleItemIndex) }
        }
        return progress
    }

    /** Progress paged — % theo trang hiện tại. */
    fun pagedProgress(): Float = if (pagedState.total > 1) (pagedState.page + 1).toFloat() / pagedState.total else 0f
}

@Composable
private fun ReaderBody(
    state: LibraryState,
    content: ReaderChapterContent,
    ui: ReaderUiState,
    showChrome: Boolean,
    modifier: Modifier = Modifier,
    gestures: ChromeGestures? = null,
) {
    // Fullscreen mới có tap-toggle chrome — non-fullscreen chrome luôn hiện
    val bodyModifier =
        if (gestures != null) {
            modifier.chromeToggleGestures(gestures.onToggle, gestures.onHover, gestures.hideTapPos)
        } else {
            modifier
        }
    if (ui.paged) {
        ReaderPagedBody(
            state,
            content,
            ui.display,
            ui.pagedState,
            showChrome,
            modifier = bodyModifier,
        )
    } else {
        ReaderReadBody(
            state,
            content,
            ui.display,
            ui.fgColor,
            ui.listState,
            showChrome,
            modifier = bodyModifier,
        )
    }
}

/** Flags hiển thị reader (gom param cho ReaderContent, S107). */
internal class ReaderUiFlags(
    val editMode: Boolean,
    val fullscreen: Boolean,
    val chromeVisible: Boolean,
)

/** Callbacks UI reader (gom param cho ReaderContent, S107). */
internal class ReaderUiCallbacks(
    val onToggleFullscreen: () -> Unit,
    val onEditChange: (Boolean) -> Unit,
    gestures: ChromeGestures,
) {
    val gestures = gestures
}

/** Toàn bộ nội dung reader: chrome + body + dialogs + bottom bar — tách cho S3776. */
@Composable
private fun ReaderContent(
    state: LibraryState,
    content: ReaderChapterContent,
    ui: ReaderUiState,
    flags: ReaderUiFlags,
    callbacks: ReaderUiCallbacks,
    visibility: ReaderDialogVisibility,
    bottomBar: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        val showChrome = !flags.fullscreen || flags.chromeVisible || flags.editMode
        if (showChrome) {
            // Header 56dp + hairline progress theo scroll (paged: theo trang)
            ReaderTopChrome(
                progress = if (ui.paged) ui.pagedProgress() else ui.progress(state),
                idx = content.idx,
                state = state,
                editMode = flags.editMode,
                fontKey = ui.display.fontKey,
                fullscreen = flags.fullscreen,
                actions =
                ReaderHeaderActions(
                    onShowDisplay = visibility.onShowDisplay,
                    onToggleFullscreen = callbacks.onToggleFullscreen,
                    onToggleEdit = { callbacks.onEditChange(!flags.editMode) },
                    onAddRule = visibility.onAddRule,
                    onPickChapter = visibility.onPickChapter,
                    onBookmark = visibility.onBookmarkSaved,
                ),
            )
        }

        // Body
        if (flags.editMode) {
            ReaderEditBody(state, { callbacks.onEditChange(false) }, Modifier.weight(1f))
        } else {
            ReaderBody(
                state,
                content,
                ui,
                showChrome,
                Modifier.weight(1f),
                callbacks.gestures,
            )
        }

        ReaderDialogs(
            state = state,
            book = state.selected ?: return@Column,
            idx = content.idx,
            display = ui.display,
            controls =
            ReaderDisplayControls(
                autoScroll = ui.auto.enabled,
                autoSpeed = ui.auto.speed,
                scrollMode = ui.display.scrollMode,
                onAutoScroll = { ui.auto.enabled = it },
                onAutoSpeed = { ui.auto.setSpeed(it) },
                onScrollMode = { mode ->
                    ui.display.scrollMode = mode
                    dev.haipham22.leechtext.util.SettingsRepository.save(
                        dev.haipham22.leechtext.util.SettingsRepository.load().copy(readerScrollMode = mode),
                    )
                },
            ),
            onRulesChange = { ui.trashRules = it },
            visibility = visibility,
        )

        // Bottom bar ‹ n/total › + tên chương (mobile) + icon chọn chương
        if (!flags.fullscreen || flags.chromeVisible) {
            bottomBar()
        }
    }
}

/** Header 56dp + hairline progress — ẩn khi fullscreen không có chrome. */
@Composable
private fun ReaderTopChrome(
    progress: Float,
    idx: Int,
    state: LibraryState,
    editMode: Boolean,
    fontKey: String,
    fullscreen: Boolean,
    actions: ReaderHeaderActions,
) {
    ReaderHeader(idx, state, editMode, fontKey, fullscreen, actions)
    LinearProgressIndicator(
        progress = { progress },
        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth().height(2.dp),
    )
}

/** Chương đang đọc: entity + vị trí + các đoạn đã strip — gom 1 param cho body (S107). */
internal data class ReaderChapterContent(
    val chapter: dev.haipham22.leechtext.models.Chapter,
    val idx: Int,
    val paragraphs: List<String>,
)

/** Body khi đọc (không edit): render các đoạn văn, bôi đen copy được.
 * modifier truyền từ caller (weight + tap-toggle chrome); paragraphs đã strip ở caller. */
@Composable
private fun ReaderReadBody(
    state: LibraryState,
    content: ReaderChapterContent,
    display: ReaderDisplay,
    fgColor: Color?,
    listState: androidx.compose.foundation.lazy.LazyListState,
    showChrome: Boolean,
    modifier: Modifier = Modifier,
) {
    ReaderParagraphs(
        state,
        content,
        display,
        fgColor,
        listState,
        modifier,
        // Chrome ẩn (fullscreen) → đệm đỉnh cao hơn status bar, chữ không chui dưới đồng hồ
        contentTopPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 24.dp,
            end = 24.dp,
            top = if (showChrome) 20.dp else 52.dp,
            bottom = 20.dp,
        ),
    )
}

/** Body chế độ sửa — TextField nội dung chương + Lưu/Hủy. */
@Composable
private fun ReaderEditBody(
    state: LibraryState,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = state.chapterText,
            onValueChange = { state.chapterText = it },
            modifier = Modifier.fillMaxWidth().weight(1f),
            label = { Text(stringResource(Res.string.reader_chapter_content)) },
        )
        Row {
            Button(onClick = {
                state.saveChapter()
                onFinish()
            }) { Text(stringResource(Res.string.action_save)) }
            Spacer(Modifier.width(8.dp))
            androidx.compose.material3.OutlinedButton(onClick = onFinish) { Text(stringResource(Res.string.action_cancel)) }
        }
    }
}

/** Body đọc: tên chương + các đoạn văn (đã strip HTML + lọc rác), bôi đen copy được. */
@Composable
private fun ReaderParagraphs(
    state: LibraryState,
    content: ReaderChapterContent,
    display: ReaderDisplay,
    fgColor: Color?,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier,
    contentTopPadding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp, vertical = 20.dp),
) {
    val chapter = content.chapter
    val idx = content.idx
    val paragraphs = content.paragraphs
    val fontFamily = readerFontFamily(display.fontKey)
    // Bôi đen copy — SelectionContainer quanh body
    SelectionContainer(modifier = modifier) {
        LazyColumn(
            state = listState,
            contentPadding = contentTopPadding,
        ) {
            item {
                Text(
                    chapter.chapName ?: stringResource(Res.string.chapter_default_name, idx + 1),
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = fontFamily),
                    color = fgColor ?: Color.Unspecified,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }
            if (paragraphs.isEmpty()) {
                readerNotDownloadedItem(state, idx)
            }
            items(paragraphs.size) { i ->
                Text(
                    paragraphs[i].trim(),
                    style =
                    MaterialTheme.typography.bodyLarge.copy(
                        fontSize = display.fontSize.sp,
                        lineHeight = (display.fontSize * display.lineHeight).sp,
                        fontFamily = fontFamily,
                    ),
                    color = fgColor ?: Color.Unspecified,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
        }
    }
}

/** Chưa tải chương: hint + progress + lỗi tải (hiện ngay trong reader) + nút tải. */
private fun LazyListScope.readerNotDownloadedItem(
    state: LibraryState,
    idx: Int,
) {
    item {
        // Chưa tải → hint + đường tải ngay (dogfood 2026-08-26: trước đây
        // chỉ text trống, không có cách tải chương từ reader)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(
                stringResource(Res.string.lib_chapter_not_downloaded),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Lỗi tải phải HIỆN trong reader — không thì bấm tải im lặng
            state.message?.let {
                Text(
                    it.asString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (it.tone() == UiText.Tone.ERROR) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Button(
                onClick = { state.redownloadChapter(idx) },
                enabled = !state.busy,
            ) { Text(stringResource(Res.string.reader_download_this_chapter)) }
        }
    }
}
