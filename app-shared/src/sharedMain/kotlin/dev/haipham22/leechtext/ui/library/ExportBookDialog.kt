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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.haipham22.leechtext.resources.Res
import dev.haipham22.leechtext.resources.action_cancel
import dev.haipham22.leechtext.resources.action_close
import dev.haipham22.leechtext.resources.action_save
import dev.haipham22.leechtext.resources.cd_pick_location
import dev.haipham22.leechtext.resources.export_action
import dev.haipham22.leechtext.resources.export_compress_level
import dev.haipham22.leechtext.resources.export_done_open_q
import dev.haipham22.leechtext.resources.export_edit_names_title
import dev.haipham22.leechtext.resources.export_epub_options
import dev.haipham22.leechtext.resources.export_format
import dev.haipham22.leechtext.resources.export_include_images
import dev.haipham22.leechtext.resources.export_location
import dev.haipham22.leechtext.resources.export_no_chapters
import dev.haipham22.leechtext.resources.export_open_folder
import dev.haipham22.leechtext.resources.export_pick_dialog_title
import dev.haipham22.leechtext.resources.export_split
import dev.haipham22.leechtext.resources.export_title
import dev.haipham22.leechtext.resources.names_optimize
import dev.haipham22.leechtext.resources.names_strip_numbers
import dev.haipham22.leechtext.resources.trash_add_rule
import dev.haipham22.leechtext.resources.trash_delete_rule
import dev.haipham22.leechtext.resources.trash_empty_hint
import dev.haipham22.leechtext.resources.trash_replace_with
import dev.haipham22.leechtext.resources.trash_rules_title
import dev.haipham22.leechtext.ui.model.UiText
import dev.haipham22.leechtext.ui.model.asString
import dev.haipham22.leechtext.ui.model.tone
import dev.haipham22.leechtext.ui.pickSaveFile
import dev.haipham22.leechtext.ui.shareFile
import dev.haipham22.leechtext.ui.theme.MonoFont
import org.jetbrains.compose.resources.stringResource

/**
 * Popup xuất ebook (Teal Archivist): chip EPUB/TXT, tùy chọn EPUB (ảnh/tách/nén),
 * chọn nơi lưu (path + browse), Hủy ghost + Xuất teal. Hiển thị tiến trình ngay
 * trong dialog — không đóng ngay khi bấm Xuất.
 */
@Composable
fun ExportBookDialog(
    state: LibraryState,
    onDismiss: () -> Unit,
) {
    val book = state.selected ?: return
    var epub by remember { mutableStateOf(true) }
    // Nơi lưu — default {savePath}/out/, user đổi được qua picker hoặc gõ tay
    var outputPath by remember(book.url) {
        mutableStateOf((book.savePath ?: "") + "/out")
    }
    var picking by remember { mutableStateOf(false) }
    // Xuất xong (busy true→false + có file) → popup hỏi mở thư mục (dogfood 260907)
    var askOpenFolder by remember { mutableStateOf(false) }
    val completed = book.chapList.orEmpty().count { it.completed }
    val pickDialogTitle = stringResource(Res.string.export_pick_dialog_title)

    ExportDialogEffects(
        state,
        epub,
        pickDialogTitle,
        picking,
        onPicking = { picking = it },
        onPath = { outputPath = it },
        onAskOpen = { askOpenFolder = true },
    )

    if (askOpenFolder) {
        ExportDoneAskOpenFolder(state) { askOpenFolder = false }
    }

    AlertDialog(
        onDismissRequest = { if (!state.busy) onDismiss() },
        title = {
            Text(
                stringResource(Res.string.export_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Box {
                ExportBodyColumn(state, completed, epub, outputPath, onEpub = { epub = it }, onPath = { outputPath = it }, onPick = { picking = true })

                // Loading phủ toàn dialog khi đang xuất — chặn bấm mọi thứ, khỏi scroll (dogfood 260907)
                if (state.busy) {
                    ExportBusyOverlay(state)
                }
            }
        },
        confirmButton = {
            ExportConfirmButton(state, epub, outputPath, completed)
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !state.busy) { Text(stringResource(Res.string.action_close)) }
        },
    )
}

/** Effects dialog export: file picker nền + theo dõi busy→idle hỏi mở thư mục (tách cho S3776). */
@Composable
private fun ExportDialogEffects(
    state: LibraryState,
    epub: Boolean,
    pickDialogTitle: String,
    picking: Boolean,
    onPicking: (Boolean) -> Unit,
    onPath: (String) -> Unit,
    onAskOpen: () -> Unit,
) {
    val onPickingLatest = rememberUpdatedState(onPicking)
    val onPathLatest = rememberUpdatedState(onPath)
    val onAskOpenLatest = rememberUpdatedState(onAskOpen)
    // File picker chạy nền — JFileChooser block, chạy ngoài main
    LaunchedEffect(picking) {
        if (picking) {
            onPickingLatest.value(false)
            pickSaveFile(pickDialogTitle, "leechtext-export.${if (epub) "epub" else "txt"}") { path ->
                if (path != null) onPathLatest.value(path)
            }
        }
    }

    // Chỉ hỏi khi export chạy xong trong lần mở dialog này — không hỏi lại lastExport cũ
    LaunchedEffect(Unit) {
        var prevBusy = state.busy
        snapshotFlow { state.busy }.collect { busy ->
            if (prevBusy && !busy && state.lastExport != null) onAskOpenLatest.value()
            prevBusy = busy
        }
    }
}

/** Body dialog export: validation + format + epub options + nơi lưu + status (tách cho S3776). */
@Composable
private fun ExportBodyColumn(
    state: LibraryState,
    completed: Int,
    epub: Boolean,
    outputPath: String,
    onEpub: (Boolean) -> Unit,
    onPath: (String) -> Unit,
    onPick: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(max = 460.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── Validation: chưa tải chương nào ──
        if (completed == 0) {
            Text(
                stringResource(Res.string.export_no_chapters),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        // ── Định dạng ──
        Text(
            stringResource(Res.string.export_format),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FormatChip("EPUB", epub) { onEpub(true) }
            FormatChip("TXT", !epub) { onEpub(false) }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // ── Tùy chọn EPUB ──
        EpubOptionsSection(state, epub)

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // ── Nơi lưu ──
        Text(
            stringResource(Res.string.export_location),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SaveLocationRow(
            outputPath = outputPath,
            onPath = onPath,
            onPick = onPick,
        )

        // ── Tiến trình / kết quả ──
        ExportStatus(state)
    }
}

/** Overlay loading phủ dialog khi đang xuất (tách cho S3776). */
@Composable
private fun ExportBusyOverlay(state: LibraryState) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
            .pointerInput(Unit) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CircularProgressIndicator()
            state.exportProgress?.let {
                Text(
                    it.asString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Export xong → hỏi mở thư mục chứa file (dogfood 260907) — tách cho S3776. */
@Composable
private fun ExportDoneAskOpenFolder(
    state: LibraryState,
    onDismissAsk: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissAsk,
        title = { Text(stringResource(Res.string.export_done_open_q)) },
        confirmButton = {
            Button(onClick = {
                onDismissAsk()
                state.lastExport?.let { shareFile(it) }
            }) { Text(stringResource(Res.string.export_open_folder)) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismissAsk) { Text(stringResource(Res.string.action_close)) }
        },
    )
}

/** Nút Xuất — spinner khi busy; theo định dạng gọi exportEpub/exportTxt. */
@Composable
private fun ExportConfirmButton(
    state: LibraryState,
    epub: Boolean,
    outputPath: String,
    completed: Int,
) {
    Button(
        onClick = {
            if (epub) state.exportEpub(outputPath) else state.exportTxt(outputPath)
        },
        enabled = !state.busy && completed > 0,
    ) {
        if (state.busy) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            Text(stringResource(Res.string.export_action))
        }
    }
}

/** Section tùy chọn EPUB: kèm ảnh, tách quyển, mức nén — mờ khi chọn TXT. */
@Composable
@Suppress("MultipleEmitters") // helper emit nhiều node có chủ đích — caller đặt trong scope Column/Row
private fun EpubOptionsSection(
    state: LibraryState,
    epub: Boolean,
) {
    val alpha = if (epub) 1f else 0.4f
    Text(
        stringResource(Res.string.export_epub_options),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
    )
    SwitchRow(stringResource(Res.string.export_include_images), state.exportImages, epub) { state.exportImages = it }
    SwitchRow(stringResource(Res.string.export_split), state.exportSplit, epub) { state.exportSplit = it }
    Column {
        // Value nằm cạnh label — không bay góc phải hàng riêng (dogfood 260905)
        Text(
            stringResource(Res.string.export_compress_level) + ": ${state.exportCompress}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
        )
        Slider(
            value = state.exportCompress.toFloat(),
            onValueChange = { state.exportCompress = it.toInt() },
            valueRange = 1f..9f,
            steps = 7,
            enabled = epub,
            // Track theo accent theme — không để default lệch màu (dogfood 260905)
            colors =
            SliderDefaults.colors(
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                activeTickColor = MaterialTheme.colorScheme.onPrimary,
                inactiveTickColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
    }
}

/** Hàng nơi lưu: path mono + nút browse mở file picker. */
@Composable
private fun SaveLocationRow(
    outputPath: String,
    onPath: (String) -> Unit,
    onPick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = outputPath,
            onValueChange = onPath,
            textStyle = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFont()),
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onPick) {
            Icon(Icons.Filled.FolderOpen, contentDescription = stringResource(Res.string.cd_pick_location))
        }
    }
}

/** Tiến trình / kết quả export: indicator khi busy + dòng progress + message màu. */
@Composable
@Suppress("MultipleEmitters") // helper emit nhiều node có chủ đích — caller đặt trong scope Column/Row
private fun ExportStatus(state: LibraryState) {
    if (state.busy) {
        LinearProgressIndicator(Modifier.fillMaxWidth())
    }
    state.exportProgress?.let {
        Text(
            it.asString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    state.message?.let { ExportMessageText(it) }
    // Xuất xong — nút mở thư mục chứa file, khỏi phải mò path tay (dogfood 260903)
    if (!state.busy && state.lastExport != null) {
        OutlinedButton(onClick = { state.lastExport?.let { shareFile(it) } }) {
            Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(Res.string.export_open_folder))
        }
    }
}

/** Message export — đỏ khi lỗi, teal khi thành công, thường khi khác (tone từ UiText). */
@Composable
private fun ExportMessageText(message: UiText) {
    Text(
        message.asString(),
        style = MaterialTheme.typography.labelSmall,
        color =
        when (message.tone()) {
            UiText.Tone.ERROR -> MaterialTheme.colorScheme.error
            UiText.Tone.SUCCESS -> MaterialTheme.colorScheme.primary
            UiText.Tone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            // Track OFF đậm hơn nền dialog — mặc định gần trùng (dogfood 260905)
            colors =
            SwitchDefaults.colors(
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.4f),
            modifier = Modifier.weight(1f).align(Alignment.CenterVertically),
        )
    }
}

/** Chip định dạng — selected = teal fill. */
@Composable
private fun FormatChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors =
        if (selected) {
            FilterChipDefaults.filterChipColors(
                containerColor = MaterialTheme.colorScheme.primary,
                labelColor = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            FilterChipDefaults.filterChipColors()
        },
    )
}

/**
 * Popup sửa tên chương (port từ ChapterListDialog AddBook): autoFix + tối ưu +
 * sửa tên từng chương inline. Tên đang sửa giữ trong SnapshotStateMap (Compose
 * track được — mutate plain Chapter object không trigger recompose → TextField
 * không update khi gõ). Lưu → apply vào Chapter + properties.json.
 */
@Composable
fun ChapterEditDialog(
    state: LibraryState,
    onDismiss: () -> Unit,
) {
    val book = state.selected ?: return
    val chapters = book.chapList ?: return
    // index → tên đang sửa; đọc fallback về tên gốc trong Chapter
    val edited = remember { mutableStateMapOf<Int, String>() }

    fun current(i: Int): String = edited[i] ?: chapters[i].chapName ?: ""

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(Res.string.export_edit_names_title, chapters.size),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 460.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Nút hàng loạt — áp lên giá trị đang sửa (edited), không mutate Chapter trực tiếp
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        chapters.indices.forEach { i ->
                            edited[i] = current(i).replace(Regex("^\\s*Chương\\s+\\d+\\s*[:.\\-]?\\s*"), "")
                        }
                    }) { Text(stringResource(Res.string.names_strip_numbers)) }
                    OutlinedButton(onClick = {
                        chapters.indices.forEach { i ->
                            edited[i] = current(i).replace(Regex("\\s+"), " ").trim()
                        }
                    }) { Text(stringResource(Res.string.names_optimize)) }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Danh sách — lazy scroll, sửa inline qua edited map
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(chapters.size) { i ->
                        val ch = chapters[i]
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 1.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "${i + 1}",
                                fontFamily = MonoFont(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(32.dp),
                            )
                            OutlinedTextField(
                                value = current(i),
                                onValueChange = { edited[i] = it },
                                textStyle = MaterialTheme.typography.bodySmall,
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            if (ch.error) {
                                Box(
                                    Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.error),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                // Apply vào Chapter objects rồi lưu
                edited.forEach { (i, name) -> chapters.getOrNull(i)?.chapName = name }
                state.saveChapterNames()
                onDismiss()
            }) { Text(stringResource(Res.string.action_save)) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text(stringResource(Res.string.action_close)) }
        },
    )
}

/**
 * Popup CRUD rules lọc rác (mở từ icon FilterAlt trong reader): danh sách rule
 * (switch bật/tắt + src → to mono + xóa), bấm rule để sửa inline, nút Thêm rule.
 * Mọi thao tác persist ngay vào setting.json + callback onRulesChange để reader
 * áp lại lọc không cần restart.
 */
@Composable
fun TrashRulesDialog(
    state: LibraryState,
    onRulesChange: (List<dev.haipham22.leechtext.models.Trash>) -> Unit,
    onDismiss: () -> Unit,
) {
    var rules by remember {
        mutableStateOf(
            dev.haipham22.leechtext.util.SettingsRepository
                .load()
                .trash,
        )
    }
    // Index rule đang sửa/Thêm mới (== rules.size); null = không sửa gì
    var editIndex by remember { mutableStateOf<Int?>(null) }
    var editSrc by remember { mutableStateOf("") }
    var editTo by remember { mutableStateOf("") }

    fun persist(next: List<dev.haipham22.leechtext.models.Trash>) {
        rules = next
        state.updateTrashRules(next)
        onRulesChange(next)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(Res.string.trash_rules_title, rules.size),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (rules.isEmpty() && editIndex == null) {
                    Text(
                        stringResource(Res.string.trash_empty_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(rules.size) { i ->
                        val rule = rules[i]
                        TrashRuleItem(
                            rule = rule,
                            editing = editIndex == i,
                            edit =
                            RuleEditUi(
                                src = editSrc,
                                to = editTo,
                                onSrc = { editSrc = it },
                                onTo = { editTo = it },
                                onSave = {
                                    persist(
                                        rules.toMutableList().also {
                                            it[i] = rule.copy(src = editSrc.trim(), to = editTo.trim())
                                        },
                                    )
                                    editIndex = null
                                },
                                onCancel = { editIndex = null },
                            ),
                            onToggle = { checked ->
                                persist(rules.toMutableList().also { it[i] = rule.copy(replace = checked) })
                            },
                            onEditStart = {
                                editIndex = i
                                editSrc = rule.src.orEmpty()
                                editTo = rule.to.orEmpty()
                            },
                            onDelete = { persist(rules.toMutableList().also { it.removeAt(i) }) },
                        )
                    }
                    item {
                        TrashRulesFooter(
                            adding = editIndex == rules.size,
                            edit =
                            RuleEditUi(
                                src = editSrc,
                                to = editTo,
                                onSrc = { editSrc = it },
                                onTo = { editTo = it },
                                onSave = {
                                    if (editSrc.isNotBlank()) {
                                        persist(
                                            rules +
                                                dev.haipham22.leechtext.models.Trash(
                                                    src = editSrc.trim(),
                                                    to = editTo.trim(),
                                                ),
                                        )
                                    }
                                    editIndex = null
                                },
                                onCancel = { editIndex = null },
                            ),
                            onStartAdd = {
                                editIndex = rules.size
                                editSrc = ""
                                editTo = ""
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_close)) }
        },
    )
}

/** Value + callback của form sửa/tạo rule — gom chung để giảm param xuống item/footer. */
private class RuleEditUi(
    val src: String,
    val to: String,
    val onSrc: (String) -> Unit,
    val onTo: (String) -> Unit,
    val onSave: () -> Unit,
    val onCancel: () -> Unit,
)

/** 1 rule trong list: switch bật/tắt + src → to (bấm để sửa) + xóa; đang sửa → form ngay dưới. */
@Composable
private fun TrashRuleItem(
    rule: dev.haipham22.leechtext.models.Trash,
    editing: Boolean,
    edit: RuleEditUi,
    onToggle: (Boolean) -> Unit,
    onEditStart: () -> Unit,
    onDelete: () -> Unit,
) {
    Column {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Switch(checked = rule.replace, onCheckedChange = onToggle)
            Spacer(Modifier.width(8.dp))
            Text(
                "${rule.src.orEmpty().ifEmpty { "?" }} → ${rule.to.orEmpty().ifEmpty { "∅" }}",
                fontFamily = MonoFont(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier =
                Modifier
                    .weight(1f)
                    .clickable(onClick = onEditStart),
            )
            IconButton(onClick = {
                onDelete()
                // Đang sửa đúng rule vừa xóa → đóng form
                if (editing) edit.onCancel()
            }) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(Res.string.trash_delete_rule),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        if (editing) {
            RuleEditFields(
                src = edit.src,
                to = edit.to,
                onSrc = edit.onSrc,
                onTo = edit.onTo,
                onSave = edit.onSave,
                onCancel = edit.onCancel,
            )
        }
    }
}

/** Chân list rules: form thêm rule mới (đang thêm) hoặc nút "Thêm rule". */
@Composable
private fun TrashRulesFooter(
    adding: Boolean,
    edit: RuleEditUi,
    onStartAdd: () -> Unit,
) {
    if (adding) {
        RuleEditFields(
            src = edit.src,
            to = edit.to,
            onSrc = edit.onSrc,
            onTo = edit.onTo,
            onSave = edit.onSave,
            onCancel = edit.onCancel,
        )
    } else {
        OutlinedButton(onClick = onStartAdd) {
            Icon(Icons.Filled.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(Res.string.trash_add_rule))
        }
    }
}

/** 2 field sửa/tạo rule + Lưu/Hủy — dùng cho cả edit lẫn thêm mới. */
@Composable
private fun RuleEditFields(
    src: String,
    to: String,
    onSrc: (String) -> Unit,
    onTo: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = src,
            onValueChange = onSrc,
            label = { Text("src (regex)") },
            textStyle = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFont()),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = to,
            onValueChange = onTo,
            label = { Text(stringResource(Res.string.trash_replace_with)) },
            textStyle = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFont()),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSave, enabled = src.isNotBlank()) { Text(stringResource(Res.string.action_save)) }
            OutlinedButton(onClick = onCancel) { Text(stringResource(Res.string.action_cancel)) }
        }
    }
}
