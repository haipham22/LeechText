package dev.haipham22.leechtext.ui.components.organisms
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.haipham22.leechtext.resources.Res
import dev.haipham22.leechtext.resources.action_pause
import dev.haipham22.leechtext.resources.action_resume
import dev.haipham22.leechtext.resources.action_retry
import dev.haipham22.leechtext.resources.cd_dismiss
import dev.haipham22.leechtext.resources.cd_done
import dev.haipham22.leechtext.resources.cd_open_library
import dev.haipham22.leechtext.resources.queue_clear_finished
import dev.haipham22.leechtext.resources.queue_empty
import dev.haipham22.leechtext.resources.queue_panel_title
import dev.haipham22.leechtext.resources.queue_section_active
import dev.haipham22.leechtext.resources.queue_section_done
import dev.haipham22.leechtext.resources.queue_section_failed
import dev.haipham22.leechtext.ui.model.QueueItemUi
import dev.haipham22.leechtext.ui.model.QueuePanelCallbacks
import dev.haipham22.leechtext.ui.model.asString
import dev.haipham22.leechtext.ui.theme.MonoFont
import org.jetbrains.compose.resources.stringResource

/**
 * Panel "Hàng đợi" đúng master "Tachidesk Light": rộng 320dp Pure Surface border trái,
 * header icon download + title + nút dọn; sections label-caps mono (ĐANG TẢI / VỪA XONG /
 * LỖI); active row: title + dòng mono + progress 4dp + % mono teal phải; xong: check moss;
 * lỗi: mono clay + nút retry tròn.
 */
@Composable
fun DownloadQueuePanel(
    items: List<QueueItemUi>,
    modifier: Modifier = Modifier,
    callbacks: QueuePanelCallbacks = QueuePanelCallbacks(),
    coverFor: @Composable androidx.compose.foundation.layout.RowScope.(QueueItemUi) -> Unit = {},
) {
    val active = items.filter { !it.done }
    val finished = items.filter { it.done && !it.failed }
    val failed = items.filter { it.failed }

    Column(
        modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        // Header 64dp — icon download + title + nút dọn mục xong
        Row(
            Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Download,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.queue_panel_title), style = MaterialTheme.typography.titleSmall)
            }
            IconButton(onClick = callbacks.onClearAll, enabled = finished.isNotEmpty()) {
                Icon(
                    Icons.Filled.DeleteSweep,
                    contentDescription = stringResource(Res.string.queue_clear_finished),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        )

        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
            if (items.isEmpty()) {
                // Empty state căn giữa + icon — không mờ góc trái (dogfood 260905)
                item {
                    Column(
                        Modifier.fillParentMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            Icons.Filled.Download,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp),
                        )
                        Text(
                            stringResource(Res.string.queue_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
            }
            if (active.isNotEmpty()) {
                item(key = "s-active") { SectionHeader(stringResource(Res.string.queue_section_active, active.size)) }
                items(active.size, key = { "a$it" }) { i ->
                    ActiveRow(active[i], coverFor, callbacks.onDismiss, callbacks.onPause, callbacks.onResume)
                }
            }
            if (finished.isNotEmpty()) {
                item(key = "s-done") { SectionHeader(stringResource(Res.string.queue_section_done)) }
                items(finished.size, key = { "d$it" }) { i ->
                    FinishedRow(finished[i], callbacks.onDismiss, callbacks.onOpenBook)
                }
            }
            if (failed.isNotEmpty()) {
                item(key = "s-fail") { SectionHeader(stringResource(Res.string.queue_section_failed, failed.size)) }
                items(failed.size, key = { "f$it" }) { i -> FailedRow(failed[i], callbacks.onRetry) }
            }
        }
    }
}

/** Section header — mono caps 11px, bg paper canvas nhạt, divider. */
@Composable
private fun SectionHeader(label: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            label,
            fontFamily = MonoFont(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.08.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
@Suppress("MultipleEmitters") // helper emit nhiều node có chủ đích — caller đặt trong scope Column/Row
private fun ActiveRow(
    item: QueueItemUi,
    coverFor: @Composable androidx.compose.foundation.layout.RowScope.(QueueItemUi) -> Unit,
    onDismiss: (QueueItemUi) -> Unit,
    onPause: (QueueItemUi) -> Unit = {},
    onResume: (QueueItemUi) -> Unit = {},
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            coverFor(item)
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    item.id,
                    fontFamily = MonoFont(),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    item.status.asString(),
                    fontFamily = MonoFont(),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            // dogfood 260902: Pause và Bỏ(destructive) kề nhau 28dp — touch target 48dp
            // chồng lấn, tap lệch sang "Bỏ" làm item biến mất khỏi queue trong khi job
            // vẫn chạy (tưởng Pause mất item). Pause/play đặt TRƯỚC, nút bỏ ra mép,
            // nới 40dp + gap 12dp để không còn chồng vùng bấm.
            if (item.paused) {
                IconButton(onClick = { onResume(item) }, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = stringResource(Res.string.action_resume),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            } else {
                IconButton(onClick = { onPause(item) }, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Filled.Pause,
                        contentDescription = stringResource(Res.string.action_pause),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            IconButton(onClick = { onDismiss(item) }, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(Res.string.cd_dismiss),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        if (item.total > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                LinearProgressIndicator(
                    progress = { item.completed.toFloat() / item.total },
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(50)),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "${item.completed}/${item.total}",
                    fontFamily = MonoFont(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
    Divider()
}

@Composable
@Suppress("MultipleEmitters") // helper emit nhiều node có chủ đích — caller đặt trong scope Column/Row
private fun FinishedRow(
    item: QueueItemUi,
    onDismiss: (QueueItemUi) -> Unit,
    onOpenBook: (QueueItemUi) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = item.hasBook) { onOpenBook(item) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                item.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (item.total > 0) "Ch. ${item.total}" else item.status.asString(),
                fontFamily = MonoFont(),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = stringResource(Res.string.cd_done),
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(18.dp),
        )
        if (item.hasBook) {
            IconButton(onClick = { onDismiss(item) }, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = stringResource(Res.string.cd_open_library),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
    Divider()
}

@Composable
@Suppress("MultipleEmitters") // helper emit nhiều node có chủ đích — caller đặt trong scope Column/Row
private fun FailedRow(
    item: QueueItemUi,
    onRetry: (QueueItemUi) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                item.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                item.status.asString(),
                fontFamily = MonoFont(),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.error,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(
            onClick = { onRetry(item) },
            modifier =
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.10f)),
        ) {
            Icon(
                Icons.Filled.Refresh,
                contentDescription = stringResource(Res.string.action_retry),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp),
            )
        }
    }
    Divider()
}

@Composable
private fun Divider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
    )
}
