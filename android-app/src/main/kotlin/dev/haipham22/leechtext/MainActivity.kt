package dev.haipham22.leechtext

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.arkivanov.decompose.retainedComponent
import dev.haipham22.leechtext.ui.RootComponent
import dev.haipham22.leechtext.ui.SliceApp
import dev.haipham22.leechtext.ui.theme.AppThemePref
import okio.Path.Companion.toPath

/** Entry Android (P3) — share toàn bộ UI + engine với desktop. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // SplashScreen compat — phải gọi trước super.onCreate
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Engine data dir = app files dir. init() cũng reload EngineConfig.
        // DI (plugin scan đọc đĩa lúc initialize) PHẢI chạy SAU khi path đúng.
        dev.haipham22.leechtext.util.EnginePaths
            .init(filesDir.path.toPath())
        // DI container — trước RootComponent để states resolve EngineLogger
        dev.haipham22.leechtext.di.startAppKoin()
        org.koin.mp.KoinPlatformTools.defaultContext().get().get<dev.haipham22.leechtext.plugin.PluginManager>()
            .reloadFromDisk()
        dev.haipham22.leechtext.ui.AppContext.instance = this
        // Cloudflare challenge → WebView popup solver (Http detect challenge tự mở)
        dev.haipham22.leechtext.ui.CloudflareWebViewActivity.register()

        // A1: download nền qua foreground service — notification hiện tiến trình
        // (Android 13+ cần POST_NOTIFICATIONS runtime permission)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        setContent {
            // Nav bar hệ thống màu theo theme app — không còn band trắng trên dark
            // (dogfood 260905). Không edge-to-edge: Scaffold insets=0, bottom bar
            // không tự pad navigation bar. Màu = surfaceContainer của NavigationBar.
            AppThemePref.initFromSettings()
            val dark =
                when (AppThemePref.value) {
                    "dark" -> true
                    "light" -> false
                    else -> isSystemInDarkTheme()
                }
            SideEffect {
                window.navigationBarColor =
                    if (dark) {
                        0xFF1A211F.toInt() // surfaceContainer dark
                    } else {
                        0xFFEBEFED.toInt() // surfaceContainer light
                    }
                WindowCompat.getInsetsController(window, window.decorView)
                    .isAppearanceLightNavigationBars = !dark
            }
            // Decompose: retainedComponent — root sống qua rotation (như ViewModel cũ)
            val root =
                retainedComponent {
                    RootComponent(
                        it,
                        log = dev.haipham22.leechtext.di.engineLoggerFromKoin(),
                        pluginManager = dev.haipham22.leechtext.di.pluginManagerFromKoin(),
                        pluginUpdate = dev.haipham22.leechtext.di.pluginUpdateFromKoin(),
                        repositoryManager = dev.haipham22.leechtext.di.repositoryManagerFromKoin(),
                    )
                }
            SliceApp(root)
        }
    }
}
