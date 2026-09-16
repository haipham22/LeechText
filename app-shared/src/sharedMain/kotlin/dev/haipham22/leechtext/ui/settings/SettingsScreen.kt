package dev.haipham22.leechtext.ui.settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.haipham22.leechtext.AppInfo
import dev.haipham22.leechtext.resources.Res
import dev.haipham22.leechtext.resources.action_back
import dev.haipham22.leechtext.resources.action_close
import dev.haipham22.leechtext.resources.settings_reset
import dev.haipham22.leechtext.resources.settings_title
import dev.haipham22.leechtext.resources.update_available_title
import dev.haipham22.leechtext.resources.update_check_failed
import dev.haipham22.leechtext.resources.update_check_row
import dev.haipham22.leechtext.resources.update_checking
import dev.haipham22.leechtext.resources.update_up_to_date
import dev.haipham22.leechtext.resources.update_version_summary
import dev.haipham22.leechtext.resources.update_view_release
import dev.haipham22.leechtext.ui.IS_DESKTOP_PLATFORM
import dev.haipham22.leechtext.ui.IoDispatcher
import dev.haipham22.leechtext.ui.model.UiText
import dev.haipham22.leechtext.ui.model.asString
import dev.haipham22.leechtext.ui.openUrl
import dev.haipham22.leechtext.ui.theme.MonoFont
import dev.haipham22.leechtext.util.AppSettings
import dev.haipham22.leechtext.util.SettingsRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

/**
 * Màn cài đặt dạng drill-down (kiểu iOS Settings): hub là danh sách mục,
 * click từng mục → trang chỉnh riêng với top bar back. Teal Archivist.
 * Nội dung trang con: SettingsSections.kt; row primitives: SettingsRows.kt.
 */
@Composable
fun SettingsScreen(
    log: dev.haipham22.leechtext.log.EngineLogger,
    modifier: Modifier = Modifier,
    onCrashReportChange: (Boolean) -> Unit = {},
    onLanguageChange: (String) -> Unit = {},
) {
    var settings by remember { mutableStateOf(SettingsRepository.load()) }

    fun save(new: AppSettings) {
        settings = new
        SettingsRepository.save(new)
        onLanguageChange(new.language)
        // Reload engine config để userAgent/reConn/timeout áp dụng ngay
        dev.haipham22.leechtext.EngineConfig
            .reload()
    }

    var opened by remember { mutableStateOf<SettingsSection?>(null) }
    // Back hệ thống từ trang con → về hub Cài đặt, không thoát app (dogfood 260905)
    dev.haipham22.leechtext.ui.PlatformBackHandler(enabled = opened != null) { opened = null }

    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerLowest)) {
        val section = opened
        if (section == null) {
            SettingsHub(settings, onOpen = { opened = it }, onReset = {
                val defaults = SettingsRepository.defaults()
                save(defaults)
                onCrashReportChange(defaults.crashReportEnabled)
            }, log = log)
        } else {
            SettingsDetail(section, settings, ::save, onCrashReportChange, onBack = { opened = null })
        }
    }
}

/** Hub cài đặt — danh sách mục drill-down + nút khôi phục mặc định. */
@Composable
private fun ColumnScope.SettingsHub(
    settings: AppSettings,
    onOpen: (SettingsSection) -> Unit,
    onReset: () -> Unit,
    log: dev.haipham22.leechtext.log.EngineLogger,
) {
    // Hub — top bar 64dp như Library
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(Res.string.settings_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
    Column(
        Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
    ) {
        // Công cụ (Calibre/Kindlegen) + Storage (đổi thư mục dữ liệu) chỉ desktop —
        // mobile không chạy binary ngoài / không picker thư mục (owner 2026-08-29)
        val sections =
            if (IS_DESKTOP_PLATFORM) {
                SettingsSection.entries
            } else {
                SettingsSection.entries.filter { it != SettingsSection.TOOLS && it != SettingsSection.STORAGE }
            }
        sections.forEachIndexed { i, s ->
            SettingsHubRow(
                icon = s.icon,
                title = s.title(),
                summary = summaryFor(s, settings),
                onClick = { onOpen(s) },
            )
            if (i < sections.size - 1) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    modifier = Modifier.padding(start = 68.dp),
                )
            }
        }

        // N4: kiểm tra bản cập nhật thủ công từ GitHub release — KHÔNG auto-check khi start
        val scope = rememberCoroutineScope()
        var checking by remember { mutableStateOf(false) }
        var updateMsg by remember { mutableStateOf<UiText?>(null) }
        var newRelease by remember { mutableStateOf<Release?>(null) }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
            modifier = Modifier.padding(start = 68.dp),
        )
        SettingsHubRow(
            icon = Icons.Filled.Update,
            title = stringResource(Res.string.update_check_row),
            summary = updateMsg?.asString() ?: stringResource(Res.string.update_version_summary, AppInfo.VERSION),
            onClick = {
                if (checking) return@SettingsHubRow
                checking = true
                updateMsg = UiText.Res(Res.string.update_checking)
                scope.launch {
                    val rel = withContext(IoDispatcher) { UpdateCheck.latest(log) }
                    newRelease = rel?.takeIf { UpdateCheck.isNewer(AppInfo.VERSION, it.version) }
                    updateMsg = when {
                        newRelease != null -> null
                        rel == null -> UiText.Res(Res.string.update_check_failed)
                        else -> UiText.Res(Res.string.update_up_to_date, listOf(AppInfo.VERSION))
                    }
                    checking = false
                }
            },
        )
        newRelease?.let { rel ->
            AlertDialog(
                onDismissRequest = { newRelease = null },
                title = { Text(stringResource(Res.string.update_available_title)) },
                text = { Text("${AppInfo.VERSION} → ${rel.version}") },
                confirmButton = {
                    TextButton(onClick = {
                        openUrl(rel.url)
                        newRelease = null
                    }) { Text(stringResource(Res.string.update_view_release)) }
                },
                dismissButton = {
                    TextButton(onClick = { newRelease = null }) { Text(stringResource(Res.string.action_close)) }
                },
            )
        }

        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
        ) { Text(stringResource(Res.string.settings_reset), color = MaterialTheme.colorScheme.error) }
    }
}

/** Trang con cài đặt — top bar back + nội dung theo section (SettingsSections.kt). */
@Composable
private fun ColumnScope.SettingsDetail(
    section: SettingsSection,
    settings: AppSettings,
    save: (AppSettings) -> Unit,
    onCrashReportChange: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    // Trang con — top bar 56dp back như BookDetailScreen
    Row(
        Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.action_back))
        }
        Text(section.title(), style = MaterialTheme.typography.titleMedium)
    }
    Column(
        Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        when (section) {
            SettingsSection.CONNECTION -> ConnectionSection(settings, save)
            SettingsSection.STORAGE -> StorageSection(settings, save)
            SettingsSection.READER -> ReaderSection(settings, save)
            SettingsSection.SYNTAX -> SyntaxSection(settings, save)
            SettingsSection.TOOLS -> ToolsSection(settings, save)
            SettingsSection.TRASH -> TrashSection(settings, save)
            SettingsSection.PRIVACY -> PrivacySection(settings, save, onCrashReportChange)
            SettingsSection.LANGUAGE -> LanguageSection(settings, save)
        }
    }
}

/** Hàng hub: icon tròn teal + title + summary mono + chevron. */
@Composable
private fun SettingsHubRow(
    icon: ImageVector,
    title: String,
    summary: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier =
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                .padding(7.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                summary,
                fontFamily = MonoFont(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
