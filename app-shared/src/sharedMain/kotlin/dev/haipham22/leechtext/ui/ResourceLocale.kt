package dev.haipham22.leechtext.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember

/**
 * N5 i18n: cung cấp ComposeEnvironment theo ngôn ngữ user chọn cho Res.string.
 * Providers từ expect/actual — JVM/Android: reflection shim CMP internal API
 * (AppLanguage.sharedJvm.kt); iOS: rỗng (locale hệ thống).
 */
@Suppress("SpreadOperator") // vararg CompositionLocalProvider — bắt buộc spread
@Composable
fun ProvideAppLanguage(
    language: String,
    content: @Composable () -> Unit,
) {
    val providers = remember(language) { appLanguageProviders(language) }
    CompositionLocalProvider(*providers) {
        content()
    }
}
