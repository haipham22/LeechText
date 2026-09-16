package dev.haipham22.leechtext.ui.reader
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.key.key
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.haipham22.leechtext.resources.Res
import dev.haipham22.leechtext.resources.action_close
import dev.haipham22.leechtext.resources.book_chapter_list
import dev.haipham22.leechtext.resources.book_search_chapters
import dev.haipham22.leechtext.resources.cd_add_bookmark
import dev.haipham22.leechtext.resources.cd_add_filter_rule
import dev.haipham22.leechtext.resources.cd_edit_chapter
import dev.haipham22.leechtext.resources.cd_more
import dev.haipham22.leechtext.resources.cd_next_chapter
import dev.haipham22.leechtext.resources.cd_prev_chapter
import dev.haipham22.leechtext.resources.cd_redownload_chapter
import dev.haipham22.leechtext.resources.cd_remove_bookmark
import dev.haipham22.leechtext.resources.cd_view
import dev.haipham22.leechtext.resources.chapter_default_name
import dev.haipham22.leechtext.resources.reader_auto_scroll
import dev.haipham22.leechtext.resources.reader_auto_scroll_speed
import dev.haipham22.leechtext.resources.reader_bg
import dev.haipham22.leechtext.resources.reader_bg_dark
import dev.haipham22.leechtext.resources.reader_bg_light
import dev.haipham22.leechtext.resources.reader_bg_sepia
import dev.haipham22.leechtext.resources.reader_display
import dev.haipham22.leechtext.resources.reader_fg
import dev.haipham22.leechtext.resources.reader_fg_auto
import dev.haipham22.leechtext.resources.reader_fg_black
import dev.haipham22.leechtext.resources.reader_fg_gray
import dev.haipham22.leechtext.resources.reader_fg_green
import dev.haipham22.leechtext.resources.reader_fg_sepia
import dev.haipham22.leechtext.resources.reader_fg_white
import dev.haipham22.leechtext.resources.reader_fg_yellow
import dev.haipham22.leechtext.resources.reader_font
import dev.haipham22.leechtext.resources.reader_font_size
import dev.haipham22.leechtext.resources.reader_fullscreen
import dev.haipham22.leechtext.resources.reader_line_height
import dev.haipham22.leechtext.resources.reader_mode
import dev.haipham22.leechtext.resources.reader_mode_paged
import dev.haipham22.leechtext.resources.reader_mode_vertical
import dev.haipham22.leechtext.ui.library.LibraryState
import dev.haipham22.leechtext.ui.library.TrashRulesDialog
import dev.haipham22.leechtext.ui.theme.MonoFont
import dev.haipham22.leechtext.util.removeDiacritics
import org.jetbrains.compose.resources.stringResource

private val SepiaBg = Color(0xFFF6EFDD)
private val SepiaOn = Color(0xFF4E3F30)
private val SepiaVariant = Color(0xFF7A6A57)

/**
 * Nền đọc — key persist trong setting.json (readerBg). Mỗi nền 1 scheme tường minh
 * (LIGHT = tông Teal Archivist light, Theme.kt) — chọn "Sáng" phải ra nền sáng cả
 * khi app đang Tối (dogfood 260905: trước đây LIGHT dùng theme app → vẫn tối).
 */
enum class ReaderBg(val key: String) {
    LIGHT("light"),
    SEPIA("sepia"),
    DARK("dark"),
    ;

    /** Scheme reader theo nền. */
    fun scheme(): ColorScheme = when (this) {
        SEPIA ->
            lightColorScheme(
                background = SepiaBg, surface = SepiaBg,
                surfaceContainerLowest = SepiaBg, surfaceContainerLow = SepiaBg,
                surfaceContainer = SepiaBg, surfaceContainerHigh = Color(0xFFE8DFC8),
                surfaceContainerHighest = Color(0xFFE8DFC8), surfaceVariant = Color(0xFFE8DFC8),
                onBackground = SepiaOn, onSurface = SepiaOn, onSurfaceVariant = SepiaVariant,
                outlineVariant = Color(0xFFD8CCB0),
                primary = Color(0xFF006A63), onPrimary = Color.White,
            )

        DARK ->
            darkColorScheme(
                primary = Color(0xFF80D5CC),
                onPrimary = Color(0xFF003733),
                background = Color(0xFF161A1A),
                onBackground = Color(0xFFDDE4E1),
                surface = Color(0xFF161A1A),
                onSurface = Color(0xFFDDE4E1),
                surfaceContainerLowest = Color(0xFF161A1A),
                surfaceContainerLow = Color(0xFF1D2222),
                surfaceContainer = Color(0xFF222727),
                surfaceContainerHigh = Color(0xFF2C3131),
                surfaceContainerHighest = Color(0xFF3A4241),
                surfaceVariant = Color(0xFF3F4947),
                onSurfaceVariant = Color(0xFFBEC9C6),
                outlineVariant = Color(0xFF3F4947),
            )

        LIGHT ->
            lightColorScheme(
                primary = Color(0xFF006A63), onPrimary = Color.White,
                background = Color(0xFFF7FAF8), onBackground = Color(0xFF181C1C),
                surface = Color(0xFFF7FAF8), onSurface = Color(0xFF181C1C),
                // Container khớp background — body column vẽ surfaceContainerLowest
                surfaceContainerLowest = Color(0xFFF7FAF8), surfaceContainerLow = Color(0xFFF7FAF8),
                surfaceContainer = Color(0xFFF7FAF8),
                surfaceContainerHigh = Color(0xFFE6E9E7), surfaceContainerHighest = Color(0xFFE0E3E2),
                surfaceVariant = Color(0xFFE0E3E2), onSurfaceVariant = Color(0xFF3E4947),
                outline = Color(0xFF6E7977), outlineVariant = Color(0xFFBDC9C6),
            )
    }

    companion object {
        /** Key từ setting.json cũ/tay sửa không khớp → LIGHT. */
        fun fromKey(key: String?): ReaderBg = entries.firstOrNull { it.key == key } ?: LIGHT
    }
}

/**
 * Màu chữ chương từ setting — rỗng = default scheme. Guard contrast so với
 * MÀU NỀN THỰC TẾ (đã tính theme hệ thống dark + scheme reader): fg cùng tông
 * nền → bỏ override, về màu scheme. State cũ lưu fg tối + nền tối tự được cứu.
 */
internal fun readerFgColor(
    fg: String,
    actualBg: Color,
): Color? {
    if (fg.isEmpty()) return null
    val c = Color(0xFF000000.toInt() or fg.toInt(16))
    return if (kotlin.math.abs(c.luminance() - actualBg.luminance()) > 0.25f) c else null
}

/** Callbacks hàng đầu reader (Aa/fullscreen/sửa/thêm rule) — gom 1 param cho header + menu. */
internal data class ReaderHeaderActions(
    val onShowDisplay: () -> Unit,
    val onToggleFullscreen: () -> Unit,
    val onToggleEdit: () -> Unit,
    val onAddRule: () -> Unit,
    // dogfood 260906: nhãn "Ch. n" mở picker + icon bookmark lưu đánh dấu
    val onPickChapter: () -> Unit = {},
    val onBookmark: () -> Unit = {},
)

/**
 * Header reader 56dp: đóng, tên sách, Ch. n mono, hiển thị (font/size/nền),
 * fullscreen, bookmark, menu ⋮ (tải lại/rule/sửa). Mobile <600dp kiểu Kindle:
 * hàng icon gọn (back…Tt/bookmark/⋮), tên sách căn giữa dòng dưới.
 */
@Composable
internal fun ReaderHeader(
    idx: Int,
    state: LibraryState,
    editMode: Boolean,
    fontKey: String,
    fullscreen: Boolean,
    actions: ReaderHeaderActions,
) {
    BoxWithConstraints {
        val compact = maxWidth < 600.dp
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { state.editingIndex = null }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.action_close))
                }
                ReaderHeaderTitles(compact, idx, state, actions.onPickChapter)
                HeaderActionsRow(compact, idx, state, editMode, fontKey, fullscreen, actions)
            }
            // Mobile: tên sách căn giữa dưới hàng icon, kiểu Kindle
            if (compact) {
                Text(
                    state.selected?.name.orEmpty(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp),
                )
            }
        }
    }
}

/** Nửa phải hàng header: Aa + fullscreen (desktop) + bookmark + menu ⋮ (tách cho S3776). */
@Composable
@Suppress("MultipleEmitters") // helper emit nhiều node có chủ đích — caller đặt trong scope Column/Row
private fun HeaderActionsRow(
    compact: Boolean,
    idx: Int,
    state: LibraryState,
    editMode: Boolean,
    fontKey: String,
    fullscreen: Boolean,
    actions: ReaderHeaderActions,
) {
    // "Aa" chữ thay icon TextFields — icon cũ bị tưởng nút translate
    // (dogfood 260905); a11y giữ label Hiển thị + font đang dùng
    val displayLabel = stringResource(Res.string.reader_display) + ": " + readerFontLabel(fontKey)
    IconButton(onClick = actions.onShowDisplay) {
        Text(
            "Aa",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { contentDescription = displayLabel },
        )
    }
    if (!compact) {
        IconButton(onClick = actions.onToggleFullscreen) {
            Icon(
                if (fullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                contentDescription = stringResource(Res.string.reader_fullscreen),
            )
        }
    }
    ReaderBookmarkButton(
        bookmarked = idx in state.bookmarks,
        onToggle = {
            state.toggleBookmark(idx)
            // Vừa LƯU (không phải bỏ) → toast xác nhận
            if (idx in state.bookmarks) actions.onBookmark()
        },
    )
    var menu by remember { mutableStateOf(false) }
    IconButton(onClick = { menu = true }) {
        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(Res.string.cd_more))
    }
    ReaderHeaderMenu(
        expanded = menu,
        onDismiss = { menu = false },
        compact = compact,
        idx = idx,
        state = state,
        editMode = editMode,
        actions = actions,
    )
}

/** Tên sách + Ch. n mono giữa hàng icon — mobile chừa trống (tên xuống dòng dưới). */
@Composable
private fun RowScope.ReaderHeaderTitles(
    compact: Boolean,
    idx: Int,
    state: LibraryState,
    onPickChapter: () -> Unit,
) {
    if (compact) {
        Spacer(Modifier.weight(1f))
    } else {
        Text(
            state.selected?.name.orEmpty(),
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            "Ch. ${idx + 1}",
            fontFamily = MonoFont(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            // Nhãn trông như dropdown nhưng không phản hồi (dogfood 260906) —
            // giờ thật sự mở dialog chọn chương
            modifier =
            Modifier
                .clickable(onClick = onPickChapter)
                .padding(start = 6.dp, end = 8.dp),
        )
    }
}

/** Menu ⋮: fullscreen (mobile), tải lại chương, thêm rule lọc rác, sửa/xem chương. */
@Composable
private fun ReaderHeaderMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    compact: Boolean,
    idx: Int,
    state: LibraryState,
    editMode: Boolean,
    actions: ReaderHeaderActions,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        if (compact) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.reader_fullscreen)) },
                onClick = {
                    onDismiss()
                    actions.onToggleFullscreen()
                },
            )
        }
        DropdownMenuItem(
            text = { Text(stringResource(Res.string.cd_redownload_chapter)) },
            onClick = {
                onDismiss()
                state.redownloadChapter(idx)
            },
            enabled = !state.busy,
        )
        DropdownMenuItem(
            text = { Text(stringResource(Res.string.cd_add_filter_rule)) },
            onClick = {
                onDismiss()
                actions.onAddRule()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(if (editMode) Res.string.cd_view else Res.string.cd_edit_chapter)) },
            onClick = {
                onDismiss()
                actions.onToggleEdit()
            },
        )
    }
}

/** Giá trị + callback auto-scroll/chế độ đọc cho ReaderDisplayDialog (gom param, S107). */
internal data class ReaderDisplayControls(
    val autoScroll: Boolean,
    val autoSpeed: Int,
    val scrollMode: String,
    val onAutoScroll: (Boolean) -> Unit,
    val onAutoSpeed: (Int) -> Unit,
    val onScrollMode: (String) -> Unit,
)

/** Ảnh chụp giá trị hiển thị hiện tại (gom param cho ReaderDisplayDialog, S107). */
internal data class ReaderDisplaySnapshot(
    val fontSize: Int,
    val fontKey: String,
    val bgKey: String,
    val fgKey: String,
    val lineHeight: Float,
)

/** Popup hiển thị reader: chọn font, cỡ chữ (slider 12–28), dòng, màu nền — áp + persist ngay. */
@Composable
private fun ReaderDisplayDialog(
    snapshot: ReaderDisplaySnapshot,
    controls: ReaderDisplayControls,
    onChange: (size: Int, font: String, bg: String, fg: String, lineHeight: Float) -> Unit,
    onDismiss: () -> Unit,
) {
    var size by remember { androidx.compose.runtime.mutableIntStateOf(snapshot.fontSize) }
    var font by remember { mutableStateOf(snapshot.fontKey) }
    var bg by remember { mutableStateOf(snapshot.bgKey) }
    var fg by remember { mutableStateOf(snapshot.fgKey) }
    var lh by remember { androidx.compose.runtime.mutableFloatStateOf(snapshot.lineHeight) }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        androidx.compose.material3.Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(Res.string.reader_display), style = MaterialTheme.typography.titleMedium)

                Text(stringResource(Res.string.reader_font), style = MaterialTheme.typography.labelMedium)
                var showFontPicker by remember { mutableStateOf(false) }
                OutlinedButton(onClick = { showFontPicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(readerFontLabel(font))
                }
                if (showFontPicker) {
                    ReaderFontPicker(
                        font = font,
                        onPick = { key ->
                            font = key
                            onChange(size, font, bg, fg, lh)
                            showFontPicker = false
                        },
                        onDismiss = { showFontPicker = false },
                    )
                }

                Text(
                    stringResource(Res.string.reader_font_size) + ": $size",
                    style = MaterialTheme.typography.labelMedium,
                )
                androidx.compose.material3.Slider(
                    value = size.toFloat(),
                    onValueChange = { size = it.toInt().coerceIn(12, 28) },
                    onValueChangeFinished = { onChange(size, font, bg, fg, lh) },
                    valueRange = 12f..28f,
                )

                Text(
                    stringResource(Res.string.reader_line_height) + ": ${(kotlin.math.floor(lh * 10) / 10)}",
                    style = MaterialTheme.typography.labelMedium,
                )
                androidx.compose.material3.Slider(
                    value = lh,
                    onValueChange = { lh = (it * 10).toInt() / 10f },
                    onValueChangeFinished = { onChange(size, font, bg, fg, lh) },
                    valueRange = 1.2f..2.4f,
                )

                // Auto-scroll + chế độ đọc (tách section cho S3776)
                AutoScrollSection(controls.autoScroll, controls.autoSpeed, controls.onAutoScroll, controls.onAutoSpeed)
                ScrollModeSection(controls.scrollMode, controls.onScrollMode)

                Text(stringResource(Res.string.reader_bg), style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReaderBg.entries.forEach { option ->
                        androidx.compose.material3.FilterChip(
                            selected = bg == option.key,
                            onClick = {
                                bg = option.key
                                fg = "" // đổi nền → reset chữ về auto theo scheme, tránh fg cũ cùng tông nền
                                onChange(size, font, bg, fg, lh)
                            },
                            label = {
                                Text(
                                    stringResource(
                                        when (option) {
                                            ReaderBg.LIGHT -> Res.string.reader_bg_light
                                            ReaderBg.SEPIA -> Res.string.reader_bg_sepia
                                            ReaderBg.DARK -> Res.string.reader_bg_dark
                                        },
                                    ),
                                )
                            },
                        )
                    }
                }

                Text(stringResource(Res.string.reader_fg), style = MaterialTheme.typography.labelMedium)
                ReaderFgSwatches(fg) { hex ->
                    fg = hex
                    onChange(size, font, bg, fg, lh)
                }

                OutlinedButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(Res.string.action_close))
                }
            }
        }
    }
}

/** Section auto-scroll: switch bật/tắt + slider tốc độ 1..10 (commit ngay qua setAutoSpeed). */
@Composable
@Suppress("MultipleEmitters") // helper emit nhiều node có chủ đích — caller đặt trong scope Column/Row
private fun AutoScrollSection(
    autoScroll: Boolean,
    autoSpeed: Int,
    onAutoScroll: (Boolean) -> Unit,
    onAutoSpeed: (Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(Res.string.reader_auto_scroll),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.weight(1f),
        )
        androidx.compose.material3.Switch(checked = autoScroll, onCheckedChange = onAutoScroll)
    }
    Text(
        stringResource(Res.string.reader_auto_scroll_speed) + ": $autoSpeed",
        style = MaterialTheme.typography.labelMedium,
    )
    androidx.compose.material3.Slider(
        value = autoSpeed.toFloat(),
        onValueChange = { onAutoSpeed(it.toInt().coerceIn(1, 10)) },
        valueRange = 1f..10f,
    )
}

/** Section chế độ đọc: chip cuộn dọc / theo trang. */
@Composable
@Suppress("MultipleEmitters") // helper emit nhiều node có chủ đích — caller đặt trong scope Column/Row
private fun ScrollModeSection(
    scrollMode: String,
    onScrollMode: (String) -> Unit,
) {
    Text(stringResource(Res.string.reader_mode), style = MaterialTheme.typography.labelMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        androidx.compose.material3.FilterChip(
            selected = scrollMode != "paged",
            onClick = { onScrollMode("vertical") },
            label = { Text(stringResource(Res.string.reader_mode_vertical)) },
        )
        androidx.compose.material3.FilterChip(
            selected = scrollMode == "paged",
            onClick = { onScrollMode("paged") },
            label = { Text(stringResource(Res.string.reader_mode_paged)) },
        )
    }
}

/** Dialog con: chọn font chữ (radio list, preview đúng font). */
@Composable
private fun ReaderFontPicker(
    font: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        androidx.compose.material3.Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(Res.string.reader_font), style = MaterialTheme.typography.titleMedium)
                ReaderFontKeys.forEach { key ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(key == font) { onPick(key) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = font == key,
                            onClick = null,
                        )
                        Text(
                            readerFontLabel(key),
                            fontFamily = readerFontFamily(key),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
                OutlinedButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(Res.string.action_close))
                }
            }
        }
    }
}

/** Hàng ô màu chữ: chọn màu trắng/đen/sepia... — a11y label theo tên màu (dogfood 260902). */
@Composable
private fun ReaderFgSwatches(
    fg: String,
    onPick: (String) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            "" to Res.string.reader_fg_auto,
            "1a1a1a" to Res.string.reader_fg_black,
            "4e3f30" to Res.string.reader_fg_sepia,
            "8a8a8a" to Res.string.reader_fg_gray,
            "81c784" to Res.string.reader_fg_green,
            "f5f5f0" to Res.string.reader_fg_white,
            "ffd54f" to Res.string.reader_fg_yellow,
        ).forEach { (hex, labelRes) ->
            val selected = fg == hex
            val label = stringResource(labelRes)
            val c = if (hex.isEmpty()) MaterialTheme.colorScheme.onSurface else Color(0xFF000000.toInt() or hex.toInt(16))
            androidx.compose.material3.Surface(
                onClick = { onPick(hex) },
                selected = selected,
                shape = androidx.compose.foundation.shape.CircleShape,
                color = c,
                border = androidx.compose.foundation.BorderStroke(
                    if (selected) 2.dp else 1.dp,
                    // outline (không phải outlineVariant) — swatch đen/nâu phải thấy
                    // được trên nền dialog Tối (dogfood 260905)
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                ),
                modifier =
                Modifier
                    .size(32.dp)
                    .semantics { contentDescription = label },
            ) {}
        }
    }
}

/** Nút bookmark chương — icon/màu đổi theo trạng thái bookmarked. */
@Composable
private fun ReaderBookmarkButton(
    bookmarked: Boolean,
    onToggle: () -> Unit,
) {
    IconButton(onClick = onToggle) {
        Icon(
            if (bookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
            contentDescription = stringResource(if (bookmarked) Res.string.cd_remove_bookmark else Res.string.cd_add_bookmark),
            tint =
            if (bookmarked) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

/** Bottom bar: mobile kiểu Kindle — tên chương dòng trên, ‹ n/total › ☰ dòng dưới; desktop chỉ pager. */
@Composable
internal fun ReaderBottomBar(
    idx: Int,
    total: Int,
    state: LibraryState,
    chapterTitle: String,
    onPickChapter: () -> Unit,
) {
    BoxWithConstraints {
        val compact = maxWidth < 600.dp
        Column(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)) {
            if (compact) {
                Text(
                    chapterTitle,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp).padding(top = 8.dp),
                )
            }
            Row(
                Modifier.fillMaxWidth().height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { state.changeChapter(-1) }, enabled = idx > 0) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(Res.string.cd_prev_chapter))
                }
                Text(
                    "${idx + 1}/$total",
                    fontFamily = MonoFont(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                IconButton(onClick = onPickChapter) {
                    Icon(
                        Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = stringResource(Res.string.book_chapter_list),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { state.changeChapter(1) }, enabled = idx < total - 1) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(Res.string.cd_next_chapter))
                }
            }
        }
    }
}

/** Dialog chọn chương kiểu TOC — tap chương để nhảy tới, chương hiện tại bôi teal. */
@Composable
private fun ChapterPickerDialog(
    book: dev.haipham22.leechtext.models.Properties,
    idx: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val chapters = book.chapList.orEmpty()
    // Search trong TOC — truyện 2000 chương vuốt tới bao giờ; lọc bỏ dấu như detail
    var query by remember { mutableStateOf("") }
    val visible =
        chapters
            .withIndex()
            .filter {
                query.isBlank() ||
                    removeDiacritics(it.value.chapName ?: "").contains(removeDiacritics(query), ignoreCase = true)
            }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.book_chapter_list)) },
        text = {
            androidx.compose.foundation.layout.Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(Res.string.book_search_chapters)) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(420.dp),
                ) {
                    items(visible.size) { vi ->
                        val i = visible[vi].index
                        val current = i == idx
                        Text(
                            chapters[i].chapName
                                ?: stringResource(Res.string.chapter_default_name, i + 1),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (current) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (current) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { onPick(i) }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_close))
            }
        },
    )
}

/** Popup đang mở + callback tắt tương ứng — gom 1 param cho ReaderDialogs (S107). */

/** Visibility + open/close callbacks cho dialogs & header actions reader (gom param, S107). */
internal data class ReaderDialogVisibility(
    val showAddRule: Boolean,
    val onDismissAddRule: () -> Unit,
    val showDisplay: Boolean,
    val onDismissDisplay: () -> Unit,
    val showChapterPicker: Boolean,
    val onDismissPicker: () -> Unit,
    val onShowDisplay: () -> Unit = {},
    val onAddRule: () -> Unit = {},
    val onPickChapter: () -> Unit = {},
    val onBookmarkSaved: () -> Unit = {},
)

/** Cụm popup của reader: rules lọc rác + Hiển thị + chọn chương. */
@Composable
internal fun ReaderDialogs(
    state: LibraryState,
    book: dev.haipham22.leechtext.models.Properties,
    idx: Int,
    display: ReaderDisplay,
    controls: ReaderDisplayControls,
    onRulesChange: (List<dev.haipham22.leechtext.models.Trash>) -> Unit,
    visibility: ReaderDialogVisibility,
) {
    // ── Popup CRUD rules lọc rác — thêm/sửa/xóa/bật-tắt, áp ngay ──
    if (visibility.showAddRule) {
        TrashRulesDialog(
            state = state,
            onRulesChange = onRulesChange,
            onDismiss = visibility.onDismissAddRule,
        )
    }

    if (visibility.showDisplay) {
        ReaderDisplayDialog(
            snapshot =
            ReaderDisplaySnapshot(
                display.fontSize,
                display.fontKey,
                display.bgKey,
                display.fgKey,
                display.lineHeight,
            ),
            controls = controls,
            onChange = { s, f, b, fg, lh -> display.save(s, f, b, fg, lh) },
            onDismiss = visibility.onDismissDisplay,
        )
    }

    // ── Dialog chọn chương kiểu Kindle TOC ──
    if (visibility.showChapterPicker) {
        ChapterPickerDialog(
            book = book,
            idx = idx,
            onPick = {
                visibility.onDismissPicker()
                state.openChapter(it)
            },
            onDismiss = visibility.onDismissPicker,
        )
    }
}
