package dev.haipham22.leechtext.ui.components.organisms
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.haipham22.leechtext.models.Properties
import dev.haipham22.leechtext.resources.Res
import dev.haipham22.leechtext.resources.action_read
import dev.haipham22.leechtext.resources.addbook_chapters_title
import dev.haipham22.leechtext.resources.addbook_loading_toc
import dev.haipham22.leechtext.resources.addbook_view_toc
import dev.haipham22.leechtext.resources.book_completed
import dev.haipham22.leechtext.resources.book_ongoing
import dev.haipham22.leechtext.resources.cd_change_cover
import dev.haipham22.leechtext.resources.chapter_default_name
import dev.haipham22.leechtext.resources.chapters_count
import dev.haipham22.leechtext.ui.components.atoms.CoverImage
import dev.haipham22.leechtext.ui.reader.toParagraphs
import dev.haipham22.leechtext.ui.theme.MonoFont
import dev.haipham22.leechtext.util.SettingsRepository
import org.jetbrains.compose.resources.stringResource

/**
 * Thân chung của 2 màn detail sách — Thư viện (BookDetailScreen) và Preview từ
 * Nguồn (NovelPreviewScreen), vốn trùng layout (dogfood 260903: gộp 2 màn).
 * KHÔNG biết LibraryState/AddBookState — nhận book + slot composable.
 */
/** Cờ hiển thị tuỳ theo màn gọi (Thư viện vs Preview) — gom nhóm giảm số param (S107). */
data class BookDetailBodyOptions(
    val readEnabled: Boolean = true,
    // Chip Đang ra/Hoàn thành — Thư viện có, Preview ẩn (metadata thô)
    val showOngoingChip: Boolean = false,
    // null → ẩn nút đổi ảnh bìa (chỉ Thư viện)
    val onChangeCover: (() -> Unit)? = null,
    // Preview ẩn số chương cho tới khi TOC load xong
    val chapterCountVisible: Boolean = true,
    // Giới thiệu: Thư viện full, Preview giới hạn 4 dòng
    val introMaxLines: Int = Int.MAX_VALUE,
    // Icon trước label nút Đọc (Thư viện có, Preview không)
    val readIcon: (@Composable () -> Unit)? = null,
)

/** Slot nội dung caller inject — gom nhóm giảm số param (S107). */
data class BookDetailBodySlots(
    // ngay dưới meta (Thư viện: progress khi đang tải)
    val headerContent: @Composable (ColumnScope.() -> Unit) = {},
    // các nút cạnh "Đọc" trong 1 Row (Thư viện: "Chương"; Preview: "Thêm vào" + "Tải full")
    val secondaryActions: @Composable (RowScope.() -> Unit) = {},
    // hàng dưới action row, trên giới thiệu (Thư viện: nút Tải chương + chip tiếp-tục-tải)
    val belowActionsContent: @Composable (ColumnScope.() -> Unit) = {},
    // cuối màn, sau giới thiệu (TOC inline, message...)
    val trailingContent: @Composable (ColumnScope.() -> Unit) = {},
)

@Composable
fun BookDetailBody(
    book: Properties,
    onRead: () -> Unit,
    log: dev.haipham22.leechtext.log.EngineLogger,
    modifier: Modifier = Modifier,
    options: BookDetailBodyOptions = BookDetailBodyOptions(),
    slots: BookDetailBodySlots = BookDetailBodySlots(),
) {
    Column(
        modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BookCoverMetaRow(
            book = book,
            showOngoingChip = options.showOngoingChip,
            onChangeCover = options.onChangeCover,
            chapterCountVisible = options.chapterCountVisible,
            log = log,
        )

        slots.headerContent(this)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onRead,
                enabled = options.readEnabled,
                modifier = Modifier.weight(1f),
                colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                options.readIcon?.invoke()
                if (options.readIcon != null) Spacer(Modifier.width(4.dp))
                Text(stringResource(Res.string.action_read), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            slots.secondaryActions(this)
        }

        slots.belowActionsContent(this)

        BookIntroText(book.introduce, maxLines = options.introMaxLines)

        slots.trailingContent(this)
    }
}

/** Cover + meta: tên, tác giả, chip Đang ra/Hoàn thành (tuỳ chọn), số chương, đổi ảnh bìa (tuỳ chọn). */
@Composable
private fun BookCoverMetaRow(
    book: Properties,
    showOngoingChip: Boolean,
    onChangeCover: (() -> Unit)?,
    chapterCountVisible: Boolean,
    log: dev.haipham22.leechtext.log.EngineLogger,
) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Box {
            CoverImage(
                book.cover,
                log = log,
                modifier = Modifier.width(110.dp).aspectRatio(2f / 3f).clip(RoundedCornerShape(12.dp)),
            )
            if (onChangeCover != null) {
                // Đổi ảnh bìa khi thiếu/hỏng — chọn file thay thế (owner 2026-08-27)
                IconButton(
                    onClick = onChangeCover,
                    modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(28.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f),
                            RoundedCornerShape(8.dp),
                        ),
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = stringResource(Res.string.cd_change_cover),
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
        Column(
            Modifier.padding(start = 16.dp, top = 4.dp).weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                book.name ?: "?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                book.author ?: "?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (showOngoingChip) {
                book.ongoing?.let { ongoing ->
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                    ) {
                        Text(
                            if (ongoing) stringResource(Res.string.book_ongoing) else stringResource(Res.string.book_completed),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
            if (chapterCountVisible) {
                Text(
                    stringResource(Res.string.chapters_count, book.size),
                    fontFamily = MonoFont(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Giới thiệu — hiện full/không collapse, bôi đen copy được, strip HTML + lọc rác
 * cùng pipeline reader (toParagraphs + trash rules).
 */
@Composable
private fun BookIntroText(
    intro: String?,
    maxLines: Int,
) {
    val descTrashRules =
        remember(intro) {
            SettingsRepository.load().trash
        }
    intro?.takeIf { it.isNotBlank() }?.let { text ->
        val desc =
            remember(text, descTrashRules) {
                toParagraphs(text, descTrashRules).joinToString("\n\n")
            }
        if (desc.isNotEmpty()) {
            androidx.compose.foundation.text.selection.SelectionContainer {
                Text(
                    desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = maxLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * TOC inline cho preview: chưa load → nút "Xem danh sách chương" (fetch khi bấm);
 * đã load → tiêu đề + list lazy, click chương → callback. State-free — caller
 * truyền cờ + callback (AddBookState ở preview).
 */
@Composable
@Suppress("MultipleEmitters") // helper emit nhiều node có chủ đích — caller đặt trong scope Column/Row
fun InlineChapterToc(
    chapters: List<dev.haipham22.leechtext.models.Chapter>,
    loaded: Boolean,
    loading: Boolean,
    onLoad: () -> Unit,
    onChapterClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!loaded) {
        OutlinedButton(
            onClick = onLoad,
            enabled = !loading,
            modifier = modifier.fillMaxWidth(),
        ) {
            Text(
                if (loading) {
                    stringResource(Res.string.addbook_loading_toc)
                } else {
                    stringResource(Res.string.addbook_view_toc)
                },
            )
        }
    } else {
        Text(
            stringResource(Res.string.addbook_chapters_title, chapters.size),
            style = MaterialTheme.typography.titleSmall,
        )
        LazyColumn(modifier.fillMaxWidth()) {
            items(chapters.size) { i ->
                val ch = chapters[i]
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onChapterClick(i) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Ch. ${i + 1}",
                        fontFamily = MonoFont(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        ch.chapName ?: stringResource(Res.string.chapter_default_name, i + 1),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** Progress đang tải (Thư viện) — fraction + label completed/total. */
@Composable
fun DownloadProgressLine(
    completed: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    if (total <= 0) return
    Column(modifier) {
        LinearProgressIndicator(
            progress = { completed.toFloat() / total },
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth().height(4.dp),
        )
        Text(
            "$completed/$total",
            fontFamily = MonoFont(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
