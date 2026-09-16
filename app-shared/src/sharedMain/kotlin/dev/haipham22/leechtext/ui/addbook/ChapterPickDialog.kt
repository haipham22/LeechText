package dev.haipham22.leechtext.ui.addbook

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import dev.haipham22.leechtext.models.Chapter
import dev.haipham22.leechtext.resources.Res
import dev.haipham22.leechtext.resources.action_close
import dev.haipham22.leechtext.resources.action_download
import dev.haipham22.leechtext.resources.book_search_chapters
import dev.haipham22.leechtext.resources.chapter_default_name
import dev.haipham22.leechtext.resources.chapter_pick_all
import dev.haipham22.leechtext.resources.chapter_pick_invert
import dev.haipham22.leechtext.resources.chapter_pick_none
import dev.haipham22.leechtext.resources.chapter_pick_range_hint
import dev.haipham22.leechtext.resources.chapter_pick_select
import dev.haipham22.leechtext.resources.chapter_pick_selected
import dev.haipham22.leechtext.resources.chapter_pick_title
import dev.haipham22.leechtext.resources.pick_empty_toc
import dev.haipham22.leechtext.resources.pick_filter_all
import dev.haipham22.leechtext.resources.pick_filter_downloaded
import dev.haipham22.leechtext.resources.pick_filter_undownloaded
import dev.haipham22.leechtext.ui.theme.MonoFont
import org.jetbrains.compose.resources.stringResource

/**
 * Parse range "3-5,10" (1-based inclusive) → danh sách index 0-based, sort + dedupe,
 * clamp vào total. Token rác bỏ qua; range đảo swap. Input rỗng → null (không đổi selection).
 */
fun parseRanges(
    input: String,
    total: Int,
): List<Int>? {
    if (input.isBlank()) return null
    val picked = mutableSetOf<Int>()
    for (token in input.replace(Regex("\\s+"), "").split(",")) {
        val m = Regex("^(\\d+)(?:-(\\d+))?$").find(token) ?: continue
        // >Int.MAX_VALUE → bỏ qua token, không crash NumberFormatException
        val from = m.groupValues[1].toIntOrNull() ?: continue
        // end tràn Int → coi như "đến hết", clip bởi total bên dưới
        val to =
            m.groupValues[2].let { g2 ->
                when {
                    g2.isEmpty() -> from
                    else -> g2.toIntOrNull() ?: Int.MAX_VALUE
                }
            }
        // clip theo total TRƯỚC khi loop — chống range "1-99999999999" loop hàng tỷ lần
        val lo = minOf(from, to).coerceAtLeast(1)
        val hi = maxOf(from, to).coerceAtMost(total)
        for (n in lo..hi) picked.add(n - 1)
    }
    return picked.sorted()
}

/** Nén list index 0-based (đã sort) thành range string 1-based "1-5,8" — rỗng khi không chọn gì. */
fun compressToRange(indices: List<Int>): String {
    if (indices.isEmpty()) return ""
    val ranges = mutableListOf<String>()
    var start = indices[0]
    var prev = indices[0]
    for (k in 1 until indices.size) {
        if (indices[k] == prev + 1) {
            prev = indices[k]
            continue
        }
        ranges.add(if (start == prev) "$start" else "$start-$prev")
        start = indices[k]
        prev = indices[k]
    }
    ranges.add(if (start == prev) "$start" else "$start-$prev")
    return ranges.joinToString(",") { r ->
        r.split("-").joinToString("-") { (it.toInt() + 1).toString() }
    }
}

/**
 * Dialog chọn chương lẻ khi tải (N2): search lọc tên + checkbox LazyColumn +
 * Tất cả/Bỏ hết/Đảo + ô range "1-100,205" + đếm chọn. Confirm trả index 0-based
 * những chương được chọn — caller tự quyết tải hay chỉ lưu.
 * Danh sách trống → empty-state text (không render list chết); nút Đóng luôn
 * bấm được. busy=true → confirm khóa + hiện progress thay nút chết.
 *
 * Owner feedback: hàng confirm (Tải N + Đóng) đặt TRÊN list — luôn thấy được
 * không phải scroll. Range input 2 chiều: tick checkbox → range tự nén lại
 * (compressToRange); gõ range + Chọn → đặt lại checkbox.
 *
 * isDownloaded != null → thêm chip lọc Tất cả/Chưa tải/Đã tải (chỉ ảnh hưởng
 * hiển thị, selection độc lập); null → ẩn chip (caller không có context disk).
 */
@Composable
fun ChapterPickDialog(
    chapters: List<Chapter>,
    onConfirm: (indices: List<Int>) -> Unit,
    onDismiss: () -> Unit,
    confirmLabel: String = stringResource(Res.string.action_download),
    busy: Boolean = false,
    emptyText: String = stringResource(Res.string.pick_empty_toc),
    isDownloaded: ((Chapter) -> Boolean)? = null,
) {
    // Mặc định KHÔNG chọn gì (dogfood 260902: all-checked → 1 chạm tải cả 248 chương)
    var checked by remember(chapters.size) { mutableStateOf(emptySet<Int>()) }
    // Range dùng TextFieldValue — gõ là THAY THẾ, không nối (bug 260902: "7-9" + gõ
    // "11-12" → "7-911-12"). Select-all KHÔNG đặt ở onFocusChanged — platform gửi
    // cursor-placement của cú tap SAU focus qua onValueChange, đè mất selection.
    // Đặt trong onValueChange ở event đầu sau focus (event chỉ đổi selection, text
    // giữ nguyên → guard v.text == rangeInput.text) — không gì đè được nữa.
    val range = remember(chapters.size) { PickRangeState() }

    // Mọi thay đổi selection đi qua đây → range input luôn phản ánh selection
    fun setChecked(next: Set<Int>) {
        checked = next
        val compressed = compressToRange(next.sorted())
        if (compressed != range.rangeInput.text) {
            range.rangeInput = androidx.compose.ui.text.input.TextFieldValue(compressed)
        }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(Modifier.fillMaxWidth().padding(8.dp)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PickHeader(
                    confirmLabel,
                    checked.size,
                    chapters.size,
                    busy,
                    onConfirm = { onConfirm(checked.sorted()) },
                    onDismiss = onDismiss,
                )

                // TOC trống → empty-state thay các control + list chết
                if (chapters.isEmpty()) {
                    Text(
                        emptyText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    )
                } else {
                    PickBody(
                        chapters = chapters,
                        checked = checked,
                        isDownloaded = isDownloaded,
                        range = range,
                        onCheckedChange = ::setChecked,
                    )
                }
            }
        }
    }
}

/** Trạng thái dialog pick: range input, select-all, search query, filter (gom param, S107). */
private class PickRangeState {
    var rangeInput by androidx.compose.runtime.mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(""))
    var selectAllOnFocus by androidx.compose.runtime.mutableStateOf(false)
    var query by androidx.compose.runtime.mutableStateOf("")
    var filter by androidx.compose.runtime.mutableIntStateOf(0) // 0 = tất cả, 1 = chưa tải, 2 = đã tải
}

/** Search + filter chips + range + header checkbox + list chương (S3776 tách từ dialog chính). */
@Composable
@Suppress("MultipleEmitters") // helper emit nhiều node có chủ đích — caller đặt trong scope Column/Row
private fun PickBody(
    chapters: List<Chapter>,
    checked: Set<Int>,
    isDownloaded: ((Chapter) -> Boolean)?,
    range: PickRangeState,
    onCheckedChange: (Set<Int>) -> Unit,
) {
    OutlinedTextField(
        value = range.query,
        onValueChange = { range.query = it },
        label = { Text(stringResource(Res.string.book_search_chapters)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )

    // Danh sách đang HIỂN THỊ (search + filter) — batch action (Tất cả/Đảo/header
    // checkbox) áp trên đây chứ không phải toàn bộ TOC: filter "chưa tải" + Tất cả
    // phải chọn đúng chương chưa tải (bug 260906: chọn cả 5345 chương dù đang lọc)
    val visible = visibleChapters(chapters, range.query, range.filter, isDownloaded)
    val visibleIndices = visible.map { it.index }.toSet()

    PickBatchRow(visibleIndices, checked, onCheckedChange)

    if (isDownloaded != null) {
        PickFilterChips(range.filter) { range.filter = it }
    }

    RangeSelectRow(
        rangeInput = range.rangeInput,
        onRangeInput = { range.rangeInput = it },
        selectAllOnFocus = range.selectAllOnFocus,
        onSelectAllOnFocus = { range.selectAllOnFocus = it },
        total = chapters.size,
        onSelect = { parsed -> parsed?.let { onCheckedChange(it.toSet()) } },
    )

    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val allVisibleChecked = visibleIndices.isNotEmpty() && checked.containsAll(visibleIndices)
        Checkbox(
            checked = allVisibleChecked,
            onCheckedChange = { on ->
                onCheckedChange(if (on) checked + visibleIndices else checked - visibleIndices)
            },
        )
        Text(
            stringResource(Res.string.chapter_pick_all),
            style = MaterialTheme.typography.bodySmall,
        )
    }

    LazyColumn(
        modifier = Modifier.heightIn(max = 400.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items(visible.size) { vi ->
            val (i, ch) = visible[vi]
            ChapterRow(i, ch, i in checked) { on ->
                onCheckedChange(if (on) checked + i else checked - i)
            }
        }
    }
}

/**
 * Danh sách đang hiển thị theo search + filter — batch action (Tất cả/Đảo/header
 * checkbox) áp trên đây chứ không phải toàn bộ TOC: filter "chưa tải" + Tất cả
 * phải chọn đúng chương chưa tải (bug 260906: chọn cả 5345 chương dù đang lọc).
 */
private fun visibleChapters(
    chapters: List<Chapter>,
    query: String,
    filter: Int,
    isDownloaded: ((Chapter) -> Boolean)?,
): List<IndexedValue<Chapter>> = chapters
    .withIndex()
    .filter { (_, ch) ->
        (query.isBlank() || (ch.chapName ?: "").contains(query, ignoreCase = true)) &&
            (
                isDownloaded == null || filter == 0 ||
                    (filter == 1 && !isDownloaded(ch)) ||
                    (filter == 2 && isDownloaded(ch))
                )
    }

/** Hàng batch: Tất cả / Bỏ hết / Đảo — áp trên danh sách đang lọc. */
@Composable
private fun PickBatchRow(
    visibleIndices: Set<Int>,
    checked: Set<Int>,
    onCheckedChange: (Set<Int>) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { onCheckedChange(visibleIndices) }) { Text(stringResource(Res.string.chapter_pick_all)) }
        OutlinedButton(onClick = { onCheckedChange(emptySet()) }) { Text(stringResource(Res.string.chapter_pick_none)) }
        OutlinedButton(onClick = { onCheckedChange(visibleIndices - checked) }) { Text(stringResource(Res.string.chapter_pick_invert)) }
    }
}

/** Hàng đầu dialog: title + đếm chọn, rồi hàng confirm (Tải N + progress + Đóng) — luôn thấy không cần scroll. */
@Composable
@Suppress("MultipleEmitters") // helper emit nhiều node có chủ đích — caller đặt trong scope Column/Row
private fun PickHeader(
    confirmLabel: String,
    selectedCount: Int,
    totalCount: Int,
    busy: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(Res.string.chapter_pick_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(Res.string.chapter_pick_selected, selectedCount, totalCount),
            fontFamily = MonoFont(),
            style = MaterialTheme.typography.bodySmall,
        )
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(onClick = onConfirm, enabled = selectedCount > 0 && !busy) { Text("$confirmLabel ($selectedCount)") }
        if (busy) {
            LinearProgressIndicator(modifier = Modifier.width(64.dp))
        }
        OutlinedButton(onClick = onDismiss) { Text(stringResource(Res.string.action_close)) }
    }
}

/** Ô range "1-100,205" + nút Chọn. Gõ là THAY THẾ: event đầu sau focus đặt select-all
 * (guard text giữ nguyên → không đè selection người dùng), gõ tiếp là ghi đè toàn bộ. */
@Composable
private fun RangeSelectRow(
    rangeInput: androidx.compose.ui.text.input.TextFieldValue,
    onRangeInput: (androidx.compose.ui.text.input.TextFieldValue) -> Unit,
    selectAllOnFocus: Boolean,
    onSelectAllOnFocus: (Boolean) -> Unit,
    total: Int,
    onSelect: (List<Int>?) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = rangeInput,
            onValueChange = { v ->
                onRangeInput(
                    if (selectAllOnFocus && v.text == rangeInput.text) {
                        onSelectAllOnFocus(false)
                        v.copy(selection = androidx.compose.ui.text.TextRange(0, v.text.length))
                    } else {
                        v
                    },
                )
            },
            label = { Text(stringResource(Res.string.chapter_pick_range_hint)) },
            singleLine = true,
            modifier =
            Modifier
                .weight(1f)
                .onFocusChanged { state ->
                    if (state.isFocused) onSelectAllOnFocus(true)
                },
        )
        OutlinedButton(onClick = { onSelect(parseRanges(rangeInput.text, total)) }) {
            Text(stringResource(Res.string.chapter_pick_select))
        }
    }
}

/** Chip lọc Tất cả/Chưa tải/Đã tải — chỉ lọc hiển thị list, không đổi selection. */
@Composable
private fun PickFilterChips(
    filter: Int,
    onFilter: (Int) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = filter == 0,
            onClick = { onFilter(0) },
            label = { Text(stringResource(Res.string.pick_filter_all)) },
        )
        FilterChip(
            selected = filter == 1,
            onClick = { onFilter(1) },
            label = { Text(stringResource(Res.string.pick_filter_undownloaded)) },
        )
        FilterChip(
            selected = filter == 2,
            onClick = { onFilter(2) },
            label = { Text(stringResource(Res.string.pick_filter_downloaded)) },
        )
    }
}

/** 1 hàng chương: checkbox + số thứ tự mono + tên chương. */
@Composable
private fun ChapterRow(
    i: Int,
    ch: Chapter,
    checkedNow: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checkedNow, onCheckedChange = onToggle)
        Text(
            "${i + 1}.",
            fontFamily = MonoFont(),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(
            ch.chapName ?: stringResource(Res.string.chapter_default_name, i + 1),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
    }
}
