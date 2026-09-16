package dev.haipham22.leechtext

import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import dev.haipham22.leechtext.ui.RootComponent
import dev.haipham22.leechtext.ui.SliceApp
import dev.haipham22.leechtext.util.SettingsRepository
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIViewController

/**
 * Entry iOS — iosApp/ContentView.swift gọi Main_iosKt.MainViewController().
 *
 * Locale override iOS (K/N không reflection như shim JVM): đặt AppleLanguages theo
 * setting TRƯỚC khi Compose đọc NSLocale. Đổi ngôn ngữ trong app → mở lại app
 * (restart) để áp dụng — behavior chuẩn iOS app tự quản ngôn ngữ.
 */
@Suppress("FunctionNaming") // entry point bắt buộc tên MainViewController cho Kotlin/Native iOS
fun MainViewController(): UIViewController {
    dev.haipham22.leechtext.di.startAppKoin()
    val lang = runCatching { SettingsRepository.load().language }.getOrNull().takeIf { it == "en" } ?: "vi"
    NSUserDefaults.standardUserDefaults.setObject(listOf(lang), "AppleLanguages")
    val root =
        RootComponent(
            DefaultComponentContext(LifecycleRegistry().apply { resume() }),
            log = dev.haipham22.leechtext.di.engineLoggerFromKoin(),
            pluginManager = dev.haipham22.leechtext.di.pluginManagerFromKoin(),
            pluginUpdate = dev.haipham22.leechtext.di.pluginUpdateFromKoin(),
            repositoryManager = dev.haipham22.leechtext.di.repositoryManagerFromKoin(),
        )
    val rootVC = ComposeUIViewController { SliceApp(root) }
    // Cloudflare challenge → WKWebView popup solver (Http detect challenge tự mở)
    dev.haipham22.leechtext.ui.CloudflareWebViewSolver.register { rootVC }
    return rootVC
}
