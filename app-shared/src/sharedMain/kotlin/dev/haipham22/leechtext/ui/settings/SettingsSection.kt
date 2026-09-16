package dev.haipham22.leechtext.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.runtime.Composable
import dev.haipham22.leechtext.resources.Res
import dev.haipham22.leechtext.resources.settings_section_connection
import dev.haipham22.leechtext.resources.settings_section_language
import dev.haipham22.leechtext.resources.settings_section_privacy
import dev.haipham22.leechtext.resources.settings_section_reader
import dev.haipham22.leechtext.resources.settings_section_storage
import dev.haipham22.leechtext.resources.settings_section_syntax
import dev.haipham22.leechtext.resources.settings_section_tools
import dev.haipham22.leechtext.resources.settings_section_trash
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Các trang con của Cài đặt — click hàng trong hub để mở. */
internal enum class SettingsSection(
    val titleRes: StringResource,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    CONNECTION(Res.string.settings_section_connection, Icons.Filled.SettingsEthernet),
    STORAGE(Res.string.settings_section_storage, Icons.Filled.Folder),
    READER(Res.string.settings_section_reader, Icons.AutoMirrored.Filled.MenuBook),
    SYNTAX(Res.string.settings_section_syntax, Icons.Filled.Code),
    TOOLS(Res.string.settings_section_tools, Icons.Filled.Construction),
    TRASH(Res.string.settings_section_trash, Icons.Filled.FilterAlt),
    PRIVACY(Res.string.settings_section_privacy, Icons.Filled.Security),
    LANGUAGE(Res.string.settings_section_language, Icons.Filled.Language),
}

/** Tiêu đề section theo ngôn ngữ hiện tại. */
@Composable
internal fun SettingsSection.title(): String = stringResource(titleRes)
