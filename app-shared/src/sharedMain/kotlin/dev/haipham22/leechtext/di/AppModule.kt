package dev.haipham22.leechtext.di

import androidx.compose.runtime.Composable
import dev.haipham22.leechtext.log.EngineLogger
import dev.haipham22.leechtext.log.platformEngineLogger
import dev.haipham22.leechtext.plugin.PluginManager
import dev.haipham22.leechtext.plugin.PluginUpdate
import dev.haipham22.leechtext.plugin.RepositoryManager
import dev.haipham22.leechtext.plugin.util.PluginPersistence
import kotlinx.coroutines.launch
import org.koin.compose.KoinApplication
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * DI app (Koin). States chính (Library/Plugin/Queue) là owner-created bởi
 * RootComponent (mỗi state cần ComponentContext riêng) — logger/dispatcher được
 * inject từ đây xuống qua constructor chain. Thêm dependency mới về sau:
 * `single { X() }`, `factory { Y() }`.
 */
val appModule: Module =
    module {
        single<EngineLogger> { platformEngineLogger() }
        single(createdAtStart = true) { PluginManager(get()) }
        single { RepositoryManager(get()) }
        single { PluginUpdate(get(), get(), get()) }
        single { PluginPersistence(get()) }
    }

/**
 * Start Koin global context — gọi từ platform entry (desktop Main,
 * MainActivity, iOS MainViewController) TRƯỚC khi tạo RootComponent, để states
 * resolve được EngineLogger từ container. Idempotent (Activity recreate an toàn).
 */
fun startAppKoin() {
    if (org.koin.mp.KoinPlatformTools.defaultContext().getOrNull() == null) {
        org.koin.core.context.startKoin { modules(appModule) }
        // Preload engine settings off-main — EngineConfig.settings first-touch là
        // đọc đĩa đồng bộ; chủ động nạp sớm để main thread không gánh I/O lúc mở app
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default)
            .launch {
                runCatching { dev.haipham22.leechtext.util.SettingsRepository.load() }
            }
        // Khởi tạo plugin list (load đĩa + dedupe) — xong mới chạy check update
        // (thay side-effect init cũ của PluginManager object). runCatching: context
        // headless/test không có engine dir không được crash app.
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default)
            .launch {
                runCatching {
                    pluginManagerFromKoin().initialize {
                        val pluginUpdate = pluginUpdateFromKoin()
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default)
                            .launch { pluginUpdate.checkUpdate() }
                    }
                }
            }
    }
}

/** Start Koin + render content — dùng ở SliceApp gốc, commonMain không glue per-platform. */
@Composable
fun WithAppKoin(content: @Composable () -> Unit) {
    KoinApplication(application = { modules(appModule) }) {
        content()
    }
}

/** Logger từ Koin global context — platform entry dùng khi tạo RootComponent
 * trước composition (pattern Decompose owner-created). */
fun engineLoggerFromKoin(): EngineLogger = org.koin.mp.KoinPlatformTools.defaultContext().get().get()

fun pluginManagerFromKoin(): PluginManager = org.koin.mp.KoinPlatformTools.defaultContext().get().get()

fun pluginUpdateFromKoin(): PluginUpdate = org.koin.mp.KoinPlatformTools.defaultContext().get().get()

fun repositoryManagerFromKoin(): RepositoryManager = org.koin.mp.KoinPlatformTools.defaultContext().get().get()
