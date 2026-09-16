package dev.haipham22.leechtext.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import dev.haipham22.leechtext.resources.Res
import dev.haipham22.leechtext.resources.hanken_grotesk_400
import dev.haipham22.leechtext.resources.hanken_grotesk_600
import dev.haipham22.leechtext.resources.jetbrains_mono_400
import dev.haipham22.leechtext.resources.space_grotesk_600
import dev.haipham22.leechtext.resources.space_grotesk_700
import org.jetbrains.compose.resources.Font

/**
 * Teal Archivist — design system đã duyệt trên Google Stitch
 * (project 4250987222274687179, asset "Teal Archivist" v3, light only).
 * Spec đầy đủ: docs/stitch-design.md
 */
private val TealArchivistLight: ColorScheme =
    lightColorScheme(
        primary = Color(0xFF006A63),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF27847C),
        onPrimaryContainer = Color(0xFF000606),
        secondary = Color(0xFF466460),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFC5E6E1),
        onSecondaryContainer = Color(0xFF4A6865),
        tertiary = Color(0xFF8E4C30),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFAC6446),
        onTertiaryContainer = Color(0xFFFFFFFF),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF93000A),
        background = Color(0xFFF7FAF8),
        onBackground = Color(0xFF181C1C),
        surface = Color(0xFFF7FAF8),
        onSurface = Color(0xFF181C1C),
        surfaceVariant = Color(0xFFE0E3E2),
        onSurfaceVariant = Color(0xFF3E4947),
        outline = Color(0xFF6E7977),
        outlineVariant = Color(0xFFBDC9C6),
        inverseSurface = Color(0xFF2D3131),
        inverseOnSurface = Color(0xFFEEF1F0),
        inversePrimary = Color(0xFF80D5CC),
        surfaceDim = Color(0xFFD7DBD9),
        surfaceBright = Color(0xFFF7FAF8),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF1F4F3),
        surfaceContainer = Color(0xFFEBEFED),
        surfaceContainerHigh = Color(0xFFE6E9E7),
        surfaceContainerHighest = Color(0xFFE0E3E2),
    )

/** Fonts subset tiếng Việt — subset thiếu glyph nào Skia tự fallback hệ thống. */
@Composable
fun DisplayFont() = FontFamily(
    Font(Res.font.space_grotesk_600, FontWeight.SemiBold),
    Font(Res.font.space_grotesk_700, FontWeight.Bold),
)

@Composable
private fun BodyFont() = FontFamily(
    Font(Res.font.hanken_grotesk_400, FontWeight.Normal),
    Font(Res.font.hanken_grotesk_600, FontWeight.SemiBold),
)

/** Mono cho số liệu: chương, phần trăm, URL (mọi con số đo đếm đều mono theo design). */
@Composable
fun MonoFont() = FontFamily(Font(Res.font.jetbrains_mono_400, FontWeight.Normal))

@Composable
private fun tealArchivistTypography(): Typography {
    val display = DisplayFont()
    val body = BodyFont()
    val base = Typography()
    return base.copy(
        headlineLarge = base.headlineLarge.copy(fontFamily = display, letterSpacing = (-0.02).em),
        headlineMedium = base.headlineMedium.copy(fontFamily = display, letterSpacing = (-0.02).em),
        headlineSmall = base.headlineSmall.copy(fontFamily = display, letterSpacing = (-0.02).em),
        titleLarge = base.titleLarge.copy(fontFamily = display, letterSpacing = (-0.02).em),
        titleMedium = base.titleMedium.copy(fontFamily = display),
        bodyLarge = base.bodyLarge.copy(fontFamily = body),
        bodyMedium = base.bodyMedium.copy(fontFamily = body),
        bodySmall = base.bodySmall.copy(fontFamily = body),
        labelLarge = base.labelLarge.copy(fontFamily = body),
        labelMedium = base.labelMedium.copy(fontFamily = body),
        labelSmall = base.labelSmall.copy(fontFamily = body),
    )
}

/**
 * Teal Archivist dark — cùng seed teal #006A63, tonal M3 dark. Dùng khi hệ
 * thống bật Dark Mode (iOS/Android); desktop luôn light.
 */
private val TealArchivistDark: ColorScheme =
    darkColorScheme(
        primary = Color(0xFF80D5CC),
        onPrimary = Color(0xFF003733),
        primaryContainer = Color(0xFF004F49),
        onPrimaryContainer = Color(0xFF9CF1E7),
        secondary = Color(0xFFAFCFC9),
        onSecondary = Color(0xFF1F3532),
        secondaryContainer = Color(0xFF354B48),
        onSecondaryContainer = Color(0xFFC5E6E1),
        tertiary = Color(0xFFFFB68C),
        onTertiary = Color(0xFF54220B),
        tertiaryContainer = Color(0xFF6F3817),
        onTertiaryContainer = Color(0xFFFFDCC4),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF0E1514),
        onBackground = Color(0xFFDDE4E1),
        surface = Color(0xFF0E1514),
        onSurface = Color(0xFFDDE4E1),
        surfaceVariant = Color(0xFF3F4947),
        onSurfaceVariant = Color(0xFFBEC9C6),
        outline = Color(0xFF889390),
        outlineVariant = Color(0xFF3F4947),
        inverseSurface = Color(0xFFDDE4E1),
        inverseOnSurface = Color(0xFF2B3230),
        inversePrimary = Color(0xFF006A63),
        surfaceDim = Color(0xFF0E1514),
        surfaceBright = Color(0xFF343C3A),
        surfaceContainerLowest = Color(0xFF090F0E),
        surfaceContainerLow = Color(0xFF131A19),
        surfaceContainer = Color(0xFF1A211F),
        surfaceContainerHigh = Color(0xFF1E2624),
        surfaceContainerHighest = Color(0xFF29312F),
    )

/**
 * Theme app người dùng chọn (Cài đặt → Giao diện): system | light | dark.
 * mutableState — đổi ngay lập tức, không cần restart; persist qua setting.json.
 */
object AppThemePref {
    var value by androidx.compose.runtime.mutableStateOf("system")
        private set
    private var loaded = false

    /** Đọc setting 1 lần đầu composition — các lần đổi đi qua [set]. */
    fun initFromSettings() {
        if (loaded) return
        loaded = true
        value =
            dev.haipham22.leechtext.util.SettingsRepository
                .load()
                .appTheme
                .takeIf { it in setOf("system", "light", "dark") } ?: "system"
    }

    fun set(v: String) {
        value = v
        dev.haipham22.leechtext.util.SettingsRepository.save(
            dev.haipham22.leechtext.util.SettingsRepository.load().copy(appTheme = v),
        )
    }
}

/** Theme app — theo [AppThemePref], fallback Dark Mode hệ thống. */
@Composable
fun LeechTextTheme(content: @Composable () -> Unit) {
    AppThemePref.initFromSettings()
    val dark =
        when (AppThemePref.value) {
            "dark" -> true
            "light" -> false
            else -> isSystemInDarkTheme()
        }
    MaterialTheme(
        colorScheme = if (dark) TealArchivistDark else TealArchivistLight,
        typography = tealArchivistTypography(),
        content = content,
    )
}
