package dev.haipham22.leechtext.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.haipham22.leechtext.models.Trash
import dev.haipham22.leechtext.resources.Res
import dev.haipham22.leechtext.resources.action_close
import dev.haipham22.leechtext.resources.action_delete
import dev.haipham22.leechtext.resources.action_edit
import dev.haipham22.leechtext.resources.syntax_template_label
import dev.haipham22.leechtext.resources.tools_abs_path
import dev.haipham22.leechtext.resources.tools_not_configured
import dev.haipham22.leechtext.resources.trash_add_rule
import dev.haipham22.leechtext.resources.trash_delete_rule
import dev.haipham22.leechtext.resources.trash_hint
import dev.haipham22.leechtext.resources.trash_rule_title
import dev.haipham22.leechtext.resources.trash_to_label
import dev.haipham22.leechtext.ui.theme.MonoFont
import org.jetbrains.compose.resources.stringResource

/**
 * Các mục cài đặt bổ sung (port từ SettingUI bản gốc): syntax template lưu
 * (DropCaps/HTML/TXT/CSS), đường dẫn công cụ (Calibre/Kindlegen), lọc rác Trash.
 * Engine đã consume toàn bộ (Text.kt/Ebook.kt) — đây chỉ là UI sửa.
 * Pattern chung: hàng gọn (title/summary + switch + bút chì), sửa trong popup.
 */

/** Item "Cấu trúc lưu": switch bật/tắt + icon pencil mở popup sửa syntax template. */
@Composable
fun StyleSyntaxItem(
    label: String,
    description: String,
    checked: Boolean,
    syntax: String,
    onCheckedChange: (Boolean) -> Unit,
    onSyntaxChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showEditor by remember { mutableStateOf(false) }

    Row(
        modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PencilButton { showEditor = true }
        Switch(checked, onCheckedChange)
    }

    if (showEditor) {
        Dialog(onDismissRequest = { showEditor = false }) {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(label, style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = syntax,
                        onValueChange = onSyntaxChange,
                        label = { Text(stringResource(Res.string.syntax_template_label), fontFamily = MonoFont()) },
                        textStyle = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFont()),
                        minLines = 6,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.End) {
                        OutlinedButton(onClick = { showEditor = false }) { Text(stringResource(Res.string.action_close)) }
                    }
                }
            }
        }
    }
}

/** "Công cụ" — 2 hàng path mono + bút chì popup sửa đường dẫn. */
@Composable
fun ToolsSettingCard(
    calibre: String,
    kindlegen: String,
    onCalibreChange: (String) -> Unit,
    onKindlegenChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        ToolPathRow("Calibre (ebook-convert)", calibre, onCalibreChange)
        ToolPathRow("Kindlegen", kindlegen, onKindlegenChange)
    }
}

@Composable
private fun ToolPathRow(
    title: String,
    path: String,
    onChange: (String) -> Unit,
) {
    var showEditor by remember { mutableStateOf(false) }

    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                path.ifEmpty { stringResource(Res.string.tools_not_configured) },
                fontFamily = MonoFont(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        PencilButton { showEditor = true }
    }

    if (showEditor) {
        Dialog(onDismissRequest = { showEditor = false }) {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = path,
                        onValueChange = onChange,
                        label = { Text(stringResource(Res.string.tools_abs_path), fontFamily = MonoFont()) },
                        textStyle = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFont()),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.End) {
                        OutlinedButton(onClick = { showEditor = false }) { Text(stringResource(Res.string.action_close)) }
                    }
                }
            }
        }
    }
}

/**
 * "Lọc rác": rule regex src→to áp khi export (Text.kt). Mỗi rule 1 hàng gọn —
 * switch bật + summary mono + bút chì popup sửa + nút xóa; sửa trong Dialog.
 */
@Composable
fun TrashSettingCard(
    trash: List<Trash>,
    onChange: (List<Trash>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            stringResource(Res.string.trash_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        trash.forEachIndexed { i, rule ->
            // KHÔNG key remember theo rule — key đổi khi sửa src/to → showEditor reset
            // false → popup tự đóng ngay chữ đầu tiên gõ vào
            var showEditor by remember { mutableStateOf(false) }

            Row(
                Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Switch(
                    checked = rule.replace,
                    onCheckedChange = { checked ->
                        onChange(trash.toMutableList().also { it[i] = rule.copy(replace = checked) })
                    },
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "${rule.src.orEmpty().ifEmpty { "?" }} → ${rule.to.orEmpty().ifEmpty { "∅" }}",
                    fontFamily = MonoFont(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                PencilButton { showEditor = true }
                IconButton(onClick = { onChange(trash.toMutableList().also { it.removeAt(i) }) }) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.trash_delete_rule), modifier = Modifier.size(16.dp))
                }
            }

            if (showEditor) {
                Dialog(onDismissRequest = { showEditor = false }) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(stringResource(Res.string.trash_rule_title, i + 1), style = MaterialTheme.typography.titleSmall)
                            OutlinedTextField(
                                value = rule.src.orEmpty(),
                                onValueChange = { src ->
                                    onChange(trash.toMutableList().also { it[i] = rule.copy(src = src) })
                                },
                                label = { Text("src (regex)", fontFamily = MonoFont()) },
                                textStyle = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFont()),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = rule.to.orEmpty(),
                                onValueChange = { to ->
                                    onChange(trash.toMutableList().also { it[i] = rule.copy(to = to) })
                                },
                                label = { Text(stringResource(Res.string.trash_to_label), fontFamily = MonoFont()) },
                                textStyle = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFont()),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                TextButton(onClick = {
                                    onChange(trash.toMutableList().also { it.removeAt(i) })
                                    showEditor = false
                                }) { Text(stringResource(Res.string.action_delete), color = MaterialTheme.colorScheme.error) }
                                OutlinedButton(onClick = { showEditor = false }) { Text(stringResource(Res.string.action_close)) }
                            }
                        }
                    }
                }
            }
        }
        OutlinedButton(onClick = { onChange(trash + Trash(src = "", to = "")) }) {
            Icon(Icons.Filled.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(Res.string.trash_add_rule))
        }
    }
}

/** Icon bút chì mở popup sửa — dùng chung cho syntax/tools/trash. */
@Composable
private fun PencilButton(onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Icon(
            Icons.Filled.Edit,
            contentDescription = stringResource(Res.string.action_edit),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
    }
}
