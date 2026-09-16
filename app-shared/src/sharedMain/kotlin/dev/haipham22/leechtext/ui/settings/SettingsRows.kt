package dev.haipham22.leechtext.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.haipham22.leechtext.ui.theme.MonoFont

/** Row primitives dùng chung cho các trang con của Cài đặt. */

/** Hàng cài đặt số: title + caption trái, ô số mono compact phải. */
@Composable
internal fun NumberSettingRow(
    title: String,
    caption: String,
    value: Int,
    onSave: (Int) -> Unit,
) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                caption,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedTextField(
            value = text,
            onValueChange = { input ->
                text = input
                input.toIntOrNull()?.let(onSave)
            },
            textStyle = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFont()),
            singleLine = true,
            modifier = Modifier.width(120.dp),
        )
    }
}

/** Hàng cài đặt text: title + caption trên, field dưới. */
@Composable
internal fun SettingFieldRow(
    title: String,
    caption: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        Text(
            caption,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFont()),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
    }
}
