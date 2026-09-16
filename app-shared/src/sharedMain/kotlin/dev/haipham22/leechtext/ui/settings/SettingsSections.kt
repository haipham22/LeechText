package dev.haipham22.leechtext.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import dev.haipham22.leechtext.resources.Res
import dev.haipham22.leechtext.resources.conn_delay_caption
import dev.haipham22.leechtext.resources.conn_delay_title
import dev.haipham22.leechtext.resources.conn_max_caption
import dev.haipham22.leechtext.resources.conn_max_title
import dev.haipham22.leechtext.resources.conn_note
import dev.haipham22.leechtext.resources.conn_retry_caption
import dev.haipham22.leechtext.resources.conn_retry_title
import dev.haipham22.leechtext.resources.conn_timeout_caption
import dev.haipham22.leechtext.resources.conn_timeout_title
import dev.haipham22.leechtext.resources.conn_ua_caption
import dev.haipham22.leechtext.resources.conn_ua_title
import dev.haipham22.leechtext.resources.lang_name_en
import dev.haipham22.leechtext.resources.lang_name_vi
import dev.haipham22.leechtext.resources.privacy_crash_caption
import dev.haipham22.leechtext.resources.privacy_crash_title
import dev.haipham22.leechtext.resources.privacy_debuglog_caption
import dev.haipham22.leechtext.resources.privacy_debuglog_title
import dev.haipham22.leechtext.resources.reader_add_font
import dev.haipham22.leechtext.resources.reader_font
import dev.haipham22.leechtext.resources.reader_font_caption
import dev.haipham22.leechtext.resources.settings_language_field
import dev.haipham22.leechtext.resources.settings_language_restart
import dev.haipham22.leechtext.resources.settings_theme_title
import dev.haipham22.leechtext.resources.storage_pick
import dev.haipham22.leechtext.resources.storage_pick_dialog_title
import dev.haipham22.leechtext.resources.storage_workdir_caption
import dev.haipham22.leechtext.resources.storage_workdir_title
import dev.haipham22.leechtext.resources.summary_connection
import dev.haipham22.leechtext.resources.summary_off
import dev.haipham22.leechtext.resources.summary_on
import dev.haipham22.leechtext.resources.summary_syntax_on
import dev.haipham22.leechtext.resources.summary_tools_configured
import dev.haipham22.leechtext.resources.summary_trash_rules
import dev.haipham22.leechtext.resources.syntax_css_desc
import dev.haipham22.leechtext.resources.syntax_dropcaps_desc
import dev.haipham22.leechtext.resources.syntax_html_desc
import dev.haipham22.leechtext.resources.syntax_txt_desc
import dev.haipham22.leechtext.resources.theme_dark
import dev.haipham22.leechtext.resources.theme_light
import dev.haipham22.leechtext.resources.theme_system
import dev.haipham22.leechtext.ui.IS_IOS
import dev.haipham22.leechtext.ui.pickDirectory
import dev.haipham22.leechtext.ui.reader.nextReaderFont
import dev.haipham22.leechtext.ui.reader.readerFontLabel
import dev.haipham22.leechtext.ui.theme.AppThemePref
import dev.haipham22.leechtext.ui.theme.MonoFont
import dev.haipham22.leechtext.util.AppSettings
import org.jetbrains.compose.resources.stringResource

/** 6 trang con của Cài đặt — mỗi mục trong hub mở 1 trang chỉnh riêng. */

/** Trang chọn ngôn ngữ UI — dropdown; N5 đổi ngôn ngữ áp dụng ngay không cần restart. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("MultipleEmitters") // helper emit nhiều node có chủ đích — caller đặt trong scope Column/Row
internal fun LanguageSection(
    settings: AppSettings,
    save: (AppSettings) -> Unit,
) {
    // Giao diện app: system/light/dark — áp ngay qua AppThemePref, persist setting.json
    ThemeSettingRow()
    // Tên ngôn ngữ giữ nguyên dạng endonym (không dịch theo locale đang chọn)
    val options = listOf("vi" to stringResource(Res.string.lang_name_vi), "en" to stringResource(Res.string.lang_name_en))
    val current = options.firstOrNull { it.first == settings.language } ?: options.first()
    var expanded by remember { mutableStateOf(false) }

    // Dropdown full-width dưới label — không trôi lơ lửng bên phải, không trùng
    // tên màn (dogfood 260905)
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = current.second,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(Res.string.settings_language_field)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
            textStyle = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFont()),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (code, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        expanded = false
                        save(settings.copy(language = code))
                    },
                )
            }
        }
    }
    // iOS: locale override không recompose stringResource (appLanguageProviders rỗng —
    // không có reflection trên Native) → hint restart thay vì lặng lẽ không ăn (dogfood 260902).
    if (IS_IOS) {
        Text(
            stringResource(Res.string.settings_language_restart),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp),
        )
    }
}

/** Chọn theme app: mặc định (theo hệ thống) / sáng / tối — FilterChips, áp ngay khi bấm. */
@Composable
private fun ThemeSettingRow() {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(stringResource(Res.string.settings_theme_title), style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            listOf(
                "system" to Res.string.theme_system,
                "light" to Res.string.theme_light,
                "dark" to Res.string.theme_dark,
            ).forEach { (key, labelRes) ->
                val selected = AppThemePref.value == key
                androidx.compose.material3.FilterChip(
                    selected = selected,
                    onClick = { AppThemePref.set(key) },
                    // Chip selected: check + label teal — fill tối lẫn nền phải phân biệt được
                    // (dogfood 260905)
                    leadingIcon =
                    if (selected) {
                        {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    } else {
                        null
                    },
                    label = {
                        Text(
                            stringResource(labelRes),
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    },
                )
            }
        }
    }
}

@Composable
internal fun ConnectionSection(
    settings: AppSettings,
    save: (AppSettings) -> Unit,
) {
    NumberSettingRow(
        stringResource(Res.string.conn_max_title),
        stringResource(Res.string.conn_max_caption),
        settings.maxConn,
    ) { save(settings.copy(maxConn = it)) }
    NumberSettingRow(
        stringResource(Res.string.conn_retry_title),
        stringResource(Res.string.conn_retry_caption),
        settings.reConn,
    ) { save(settings.copy(reConn = it)) }
    NumberSettingRow(
        stringResource(Res.string.conn_timeout_title),
        // Hiển thị giây ("90 giây") thay vì ms thô "90000 ms" — ô nhập vẫn là ms
        stringResource(Res.string.conn_timeout_caption, settings.timeout / 1000),
        settings.timeout,
    ) { save(settings.copy(timeout = it)) }
    NumberSettingRow(
        stringResource(Res.string.conn_delay_title),
        stringResource(Res.string.conn_delay_caption),
        settings.delay,
    ) { save(settings.copy(delay = it)) }
    SettingFieldRow(
        stringResource(Res.string.conn_ua_title),
        stringResource(Res.string.conn_ua_caption),
        settings.userAgent,
    ) { save(settings.copy(userAgent = it)) }
    Text(
        stringResource(Res.string.conn_note),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
internal fun StorageSection(
    settings: AppSettings,
    save: (AppSettings) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(Res.string.storage_workdir_title), style = MaterialTheme.typography.bodyMedium)
            Text(
                settings.workPath.ifEmpty { "~/.leechtext" },
                fontFamily = MonoFont(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(Res.string.storage_workdir_caption),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val pickDialogTitle = stringResource(Res.string.storage_pick_dialog_title)
        OutlinedButton(onClick = {
            pickDirectory(pickDialogTitle) { dir -> if (dir != null) save(settings.copy(workPath = dir)) }
        }) { Text(stringResource(Res.string.storage_pick)) }
    }
}

/** Trang Đọc truyện — font-family đọc, cycle cùng nút trên reader top bar. */
@Composable
internal fun ReaderSection(
    settings: AppSettings,
    save: (AppSettings) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(Res.string.reader_font), style = MaterialTheme.typography.bodyMedium)
            Text(
                stringResource(Res.string.reader_font_caption),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            OutlinedButton(onClick = { save(settings.copy(readerFont = nextReaderFont(settings.readerFont))) }) {
                Text(readerFontLabel(settings.readerFont))
            }
            TextButton(onClick = { dev.haipham22.leechtext.ui.reader.openFontsFolder() }) {
                Text(stringResource(Res.string.reader_add_font))
            }
        }
    }
}

@Composable
internal fun SyntaxSection(
    settings: AppSettings,
    save: (AppSettings) -> Unit,
) {
    StyleSyntaxItem(
        label = "DropCaps",
        description = stringResource(Res.string.syntax_dropcaps_desc),
        checked = settings.dropcapsEnabled,
        syntax = settings.dropSyntax,
        onCheckedChange = { save(settings.copy(dropcapsEnabled = it)) },
        onSyntaxChange = { save(settings.copy(dropSyntax = it)) },
    )
    StyleSyntaxItem(
        label = "HTML SYNTAX",
        description = stringResource(Res.string.syntax_html_desc),
        checked = settings.htmlChecked,
        syntax = settings.htmlSyntax,
        onCheckedChange = { save(settings.copy(htmlChecked = it)) },
        onSyntaxChange = { save(settings.copy(htmlSyntax = it)) },
    )
    StyleSyntaxItem(
        label = "TXT SYNTAX",
        description = stringResource(Res.string.syntax_txt_desc),
        checked = settings.txtChecked,
        syntax = settings.txtSyntax,
        onCheckedChange = { save(settings.copy(txtChecked = it)) },
        onSyntaxChange = { save(settings.copy(txtSyntax = it)) },
    )
    StyleSyntaxItem(
        label = "CSS SYNTAX",
        description = stringResource(Res.string.syntax_css_desc),
        checked = settings.cssChecked,
        syntax = settings.cssSyntax,
        onCheckedChange = { save(settings.copy(cssChecked = it)) },
        onSyntaxChange = { save(settings.copy(cssSyntax = it)) },
    )
}

@Composable
internal fun ToolsSection(
    settings: AppSettings,
    save: (AppSettings) -> Unit,
) {
    ToolsSettingCard(
        calibre = settings.calibre,
        kindlegen = settings.kindlegen,
        onCalibreChange = { save(settings.copy(calibre = it)) },
        onKindlegenChange = { save(settings.copy(kindlegen = it)) },
    )
}

@Composable
internal fun TrashSection(
    settings: AppSettings,
    save: (AppSettings) -> Unit,
) {
    TrashSettingCard(
        trash = settings.trash,
        onChange = { save(settings.copy(trash = it)) },
    )
}

@Composable
@Suppress("MultipleEmitters") // helper emit nhiều node có chủ đích — caller đặt trong scope Column/Row
internal fun PrivacySection(
    settings: AppSettings,
    save: (AppSettings) -> Unit,
    onCrashReportChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(Res.string.privacy_crash_title), style = MaterialTheme.typography.bodyMedium)
            Text(
                stringResource(Res.string.privacy_crash_caption),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = settings.crashReportEnabled,
            onCheckedChange = { enabled ->
                save(settings.copy(crashReportEnabled = enabled))
                onCrashReportChange(enabled)
            },
        )
    }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(Res.string.privacy_debuglog_title), style = MaterialTheme.typography.bodyMedium)
            Text(
                stringResource(Res.string.privacy_debuglog_caption),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = settings.debugLog,
            onCheckedChange = { enabled ->
                save(settings.copy(debugLog = enabled))
                dev.haipham22.leechtext.EngineConfig
                    .reload()
            },
        )
    }
}

/** Summary hiển thị trên hub — giá trị hiện tại của nhóm, mono. */
@Composable
internal fun summaryFor(
    s: SettingsSection,
    settings: AppSettings,
): String = when (s) {
    SettingsSection.CONNECTION -> stringResource(Res.string.summary_connection, settings.maxConn, settings.timeout / 1000)

    SettingsSection.STORAGE -> settings.workPath.ifEmpty { "~/.leechtext" }

    SettingsSection.READER -> readerFontLabel(settings.readerFont)

    SettingsSection.SYNTAX ->
        stringResource(
            Res.string.summary_syntax_on,
            listOf(
                settings.dropcapsEnabled,
                settings.htmlChecked,
                settings.txtChecked,
                settings.cssChecked,
            ).count { it },
        )

    SettingsSection.TOOLS -> stringResource(Res.string.summary_tools_configured, listOf(settings.calibre, settings.kindlegen).count { it.isNotBlank() })

    SettingsSection.TRASH -> stringResource(Res.string.summary_trash_rules, settings.trash.size)

    SettingsSection.PRIVACY -> stringResource(if (settings.crashReportEnabled) Res.string.summary_on else Res.string.summary_off)

    SettingsSection.LANGUAGE -> stringResource(if (settings.language == "en") Res.string.lang_name_en else Res.string.lang_name_vi)
}
