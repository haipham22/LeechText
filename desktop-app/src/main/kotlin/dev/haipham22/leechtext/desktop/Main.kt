package dev.haipham22.leechtext.desktop

import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.window.singleWindowApplication
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import dev.haipham22.leechtext.ui.RootComponent
import dev.haipham22.leechtext.ui.SliceApp
import dev.haipham22.leechtext.util.SettingsRepository
import io.sentry.Sentry

private const val DEFAULT_DSN =
    "https://06a648f2acfa456cab633ee8847834bd@o1036981.ingest.us.sentry.io/6129468"

/** Khởi động Sentry chỉ khi user đã đồng ý (crash_report) hoặc env set DSN (dev). */
private fun startSentry() {
    val envDsn = System.getenv("LEECHTEXT_SENTRY_DSN")
    val enabled = envDsn != null || SettingsRepository.load().crashReportEnabled
    if (!enabled) return
    Sentry.init { options ->
        options.dsn = envDsn ?: DEFAULT_DSN
        options.tracesSampleRate = 0.2
    }
    dev.haipham22.leechtext.di.engineLoggerFromKoin().errorSink = { Sentry.captureException(it) }
}

/** Toggle runtime từ SettingsScreen — tắt hẳn SDK, bật init lại. */
private fun applyCrashReport(enabled: Boolean) {
    if (enabled) {
        startSentry()
    } else {
        dev.haipham22.leechtext.di.engineLoggerFromKoin().errorSink = null
        Sentry.close()
    }
}

fun main() {
    // Title bar macOS: khởi tạo theo dark mode hệ thống — mặc định AWT ép Aqua
    // (light) dù app dark (owner 260907). Runtime đổi theo setting Giao diện
    // của app qua client property bên dưới (system property này chỉ đọc 1 lần)
    System.setProperty("apple.awt.application.appearance", "system")
    dev.haipham22.leechtext.di.startAppKoin()
    startSentry()
    singleWindowApplication(title = "LeechText") {
        // Decompose: root PHẢI tạo trên UI thread (EDT) — tạo trong content lambda,
        // ngoài ra checkMainThread của Decompose throw NotOnMainThreadException
        val root =
            RootComponent(
                DefaultComponentContext(LifecycleRegistry().apply { resume() }),
                log = dev.haipham22.leechtext.di.engineLoggerFromKoin(),
                pluginManager = dev.haipham22.leechtext.di.pluginManagerFromKoin(),
                pluginUpdate = dev.haipham22.leechtext.di.pluginUpdateFromKoin(),
                repositoryManager = dev.haipham22.leechtext.di.repositoryManagerFromKoin(),
            )
        // Desktop layout cần bề rộng cho grid ~7 cột + queue panel — như master Stitch
        window.minimumSize = java.awt.Dimension(1024, 680)
        window.size = java.awt.Dimension(1280, 800)
        // Window/dock icon — painterResource là @Composable nên dùng AWT image
        window.iconImage = java.awt.Toolkit.getDefaultToolkit().getImage(
            ComposeWindow::class.java.classLoader.getResource("icons/leechtext.png"),
        )
        // Title bar theo theme APP (không chỉ hệ thống): setting Tối/Sáng đổi
        // → client property cập nhật ngay. Key macOS-only, platform khác = no-op
        val darkTheme =
            when (dev.haipham22.leechtext.ui.theme.AppThemePref.value) {
                "dark" -> true
                "light" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
        androidx.compose.runtime.LaunchedEffect(darkTheme) {
            window.rootPane.putClientProperty(
                "apple.awt.windowTitleBarAppearance",
                if (darkTheme) "NSAppearanceNameDarkAqua" else "NSAppearanceNameAqua",
            )
        }
        SliceApp(root, onCrashReportChange = ::applyCrashReport)
    }
}
