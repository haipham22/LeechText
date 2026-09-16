package dev.haipham22.leechtext.ui.sources

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import dev.haipham22.leechtext.entities.PluginEntity
import dev.haipham22.leechtext.log.EngineLogger
import dev.haipham22.leechtext.plugin.PluginManager
import dev.haipham22.leechtext.plugin.PluginUpdate
import dev.haipham22.leechtext.plugin.RepositoryManager
import dev.haipham22.leechtext.plugin.util.PluginPersistence
import dev.haipham22.leechtext.plugin.vbook.VBookPluginService
import dev.haipham22.leechtext.plugin.vbook.exception.VBookPluginException
import dev.haipham22.leechtext.plugin.vbook.model.VBookExtensionEntity
import dev.haipham22.leechtext.resources.Res
import dev.haipham22.leechtext.resources.error_prefix
import dev.haipham22.leechtext.resources.plugins_fetch_failed
import dev.haipham22.leechtext.resources.plugins_install_dead_warn
import dev.haipham22.leechtext.resources.plugins_install_error
import dev.haipham22.leechtext.resources.plugins_install_failed
import dev.haipham22.leechtext.resources.plugins_installed
import dev.haipham22.leechtext.resources.plugins_installed_n
import dev.haipham22.leechtext.resources.plugins_installing
import dev.haipham22.leechtext.resources.plugins_installing_n
import dev.haipham22.leechtext.resources.plugins_none_to_install
import dev.haipham22.leechtext.resources.plugins_reinstall_hint
import dev.haipham22.leechtext.resources.plugins_repo_added
import dev.haipham22.leechtext.resources.plugins_repo_invalid
import dev.haipham22.leechtext.resources.plugins_repo_removed
import dev.haipham22.leechtext.resources.plugins_test_bad
import dev.haipham22.leechtext.resources.plugins_test_ok
import dev.haipham22.leechtext.resources.plugins_test_unreachable
import dev.haipham22.leechtext.resources.plugins_testing
import dev.haipham22.leechtext.resources.plugins_uninstalled
import dev.haipham22.leechtext.ui.IoDispatcher
import dev.haipham22.leechtext.ui.addbook.GlobalSearchState
import dev.haipham22.leechtext.ui.model.UiText
import dev.haipham22.leechtext.util.SettingsRepository
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * State + logic màn plugin (MUST #9): search + 2 group collapse (Đã cài / Kho vBook),
 * cài/nâng cấp/xóa. Filter áp cho cả tên / tác giả / nguồn.
 */
/** Màn trong tab Nguồn: List (kho) / Browse (source) / Preview (đọc thử) / Search. */
sealed interface SourceConfig {
    data object List : SourceConfig

    data class Browse(val pluginUuid: String) : SourceConfig

    data class Preview(val url: String) : SourceConfig

    data class Search(val query: String) : SourceConfig
}

/** Child của Sources stack — giữ context + state chung. */
class SourceChild(
    val context: ComponentContext,
    val state: PluginState,
)

@Suppress("TooManyFunctions", "LongParameterList") // Decompose component — action công khai + deps từ RootComponent (Koin)
class PluginState(
    componentContext: ComponentContext,
    val log: EngineLogger,
    val pluginManager: PluginManager,
    val pluginUpdate: PluginUpdate,
    val repositoryManager: RepositoryManager,
) : ComponentContext by componentContext {
    /** Save plugin file — PluginConfigDialog (PluginScreen) gọi trực tiếp sau khi edit. */
    val pluginPersistence = PluginPersistence(log)
    var extensions by mutableStateOf<List<VBookExtensionEntity>>(emptyList())
    var installed by mutableStateOf<List<PluginEntity>>(emptyList())
    var loading by mutableStateOf(false)
    var message by mutableStateOf<UiText?>(null)
    var search by mutableStateOf("")

    /**
     * Path zip plugin mã hóa (encrypt: true) đã xác nhận install fail — ẩn khỏi
     * NÂNG CẤP để badge không đòi update vĩnh viễn (dogfood 260906).
     * Registry không khai báo encrypt nên chỉ biết sau khi thử tải.
     * Persist setting.json (encrypted_paths) — không hiện lại mỗi phiên.
     */
    var encryptedPaths by mutableStateOf<Set<String>>(emptySet())
        private set

    /**
     * Nguồn chết (browse home rỗng — vd site đã đóng kiểu Bạch Ngọc Sách, dogfood
     * 260906): ẩn khỏi tab Nguồn, gom vào section riêng + nút Thử lại.
     * Persist setting.json (broken_sources) theo tên plugin.
     */
    var brokenSources by mutableStateOf<Set<String>>(emptySet())
        private set

    fun markBroken(name: String) {
        if (name.isEmpty() || name in brokenSources) return
        brokenSources = brokenSources + name
        persistSettings { it.copy(brokenSources = brokenSources.toList()) }
    }

    /** Nút Thử lại — bỏ khỏi broken list, probe lại browse ở lần mở nguồn kế tiếp. */
    fun retrySource(name: String) {
        brokenSources = brokenSources - name
        persistSettings { it.copy(brokenSources = brokenSources.toList()) }
    }

    private fun persistSettings(update: (dev.haipham22.leechtext.util.AppSettings) -> dev.haipham22.leechtext.util.AppSettings) = scope.launch {
        // load/save setting.json đọc + ghi file — không block main
        withContext(IoDispatcher) {
            SettingsRepository.save(update(SettingsRepository.load()))
        }
    }

    // MUST #17: quản lý repo nguồn (repository.json) — load IO trong init,
    // không đọc disk trên main lúc VM tạo (góp phần "Skipped N frames" cold start)
    var repos by mutableStateOf<List<dev.haipham22.leechtext.entities.RepositoryEntity>>(emptyList())
    var newRepoUrl by mutableStateOf("")
    var reposExpanded by mutableStateOf(false)

    // Pinned sources — persist qua setting.json (pinned_sources). Load IO trong init
    // (đọc disk trên main lúc VM tạo góp phần block cold start).
    var pinnedNames by mutableStateOf<List<String>>(emptyList())

    fun togglePin(name: String) {
        pinnedNames = if (name in pinnedNames) pinnedNames - name else pinnedNames + name
        val updated = pinnedNames
        scope.launch {
            // load/save setting.json đọc + ghi file — không block main
            withContext(IoDispatcher) {
                val s = SettingsRepository.load()
                SettingsRepository.save(s.copy(pinnedSources = updated))
            }
        }
    }

    var globalSearch by mutableStateOf<GlobalSearchState?>(null)

    val scope = coroutineScope()

    // ── Navigation (Decompose ChildStack): List → Browse/Preview/Search ──
    private val navigation = StackNavigation<SourceConfig>()

    val stack: Value<ChildStack<SourceConfig, SourceChild>> =
        childStack(
            source = navigation,
            serializer = null,
            initialStack = { listOf(SourceConfig.List) },
            handleBackButton = false,
            childFactory = { _, ctx -> SourceChild(ctx, this) },
        )

    fun openBrowse(pluginUuid: String) {
        if (pluginUuid.isNotEmpty() && stack.value.active.configuration !is SourceConfig.Browse) {
            navigation.push(SourceConfig.Browse(pluginUuid))
        }
    }

    fun openSearch(query: String) {
        if (query.isNotBlank() && stack.value.active.configuration !is SourceConfig.Search) {
            navigation.push(SourceConfig.Search(query))
        }
    }

    fun openPreview(url: String) {
        if (url.isNotEmpty() && stack.value.active.configuration !is SourceConfig.Preview) {
            navigation.push(SourceConfig.Preview(url))
        }
    }

    fun closeTop() {
        navigation.pop()
    }

    private val service = VBookPluginService(log, pluginManager = pluginManager)
    private var fetchedOnce = false

    init {
        // Stale-while-revalidate: đổ cache disk vào state trước — mở tab là có
        // dữ liệu ngay, fetch nền thay thế khi xong. Parse Gson ~200KB → IO.
        scope.launch {
            val (cached, settings, repoList) =
                withContext(IoDispatcher) {
                    Triple(
                        service.loadCachedExtensions(),
                        SettingsRepository.load(),
                        repositoryManager.repositoryList(),
                    )
                }
            if (extensions.isEmpty()) extensions = cached
            pinnedNames = settings.pinnedSources
            brokenSources = settings.brokenSources.toSet()
            encryptedPaths = settings.encryptedPluginPaths.toSet()
            repos = repoList
            // Registry rỗng mà kho còn danh sách → gợi ý cài lại ngay khi mở tab
            pluginManager.awaitInitialization()
            installed = pluginManager.list()
            maybeReinstallHint()
        }
    }

    fun initialLoad() = scope.launch {
        // Fetch 1 lần mỗi session — kể cả khi đã có cache (version check vẫn chạy,
        // hiển thị không phụ thuộc vì cache hiện sẵn)
        if (!fetchedOnce && !loading) refresh()
    }

    /** Tải lại danh sách repo + trạng thái đã cài. */
    fun refresh() = scope.launch {
        loading = true
        message = null
        try {
            pluginManager.awaitInitialization()
            // Installed = disk-local → set ngay, không chờ network (tab Nguồn khỏi trắng)
            installed = pluginManager.list()
            // Blocking HTTP (fetch repo) — chạy IO, không đứng main thread
            val exts = withContext(IoDispatcher) { service.getAvailablePlugins() }
            installed = pluginManager.list()
            extensions = exts
            if (exts.isNotEmpty()) service.saveCachedExtensions(exts)
            if (exts.isEmpty() && extensions.isEmpty()) {
                message = UiText.Res(Res.string.plugins_fetch_failed, tone = UiText.Tone.ERROR)
            }
            fetchedOnce = true
            maybeReinstallHint()
        } catch (e: Exception) {
            message = UiText.Res(Res.string.error_prefix, listOf(e.message ?: e::class.simpleName ?: "error"), UiText.Tone.ERROR)
        } finally {
            loading = false
        }
    }

    /**
     * Tìm plugin đã cài tương ứng extension kho. Match theo HOST của source —
     * mirror family (truyện cùng tên khác domain) là 2 plugin khác nhau; fallback
     * theo name từng map cả 2 row kho vào 1 plugin (bug "NÂNG CẤP (2)" 2026-08-26:
     * plugin mirror đã cài bị bản cũ xóa file, ext kia rơi vào fallback name).
     * Chỉ fallback name khi extension KHÔNG có source.
     */
    fun findInstalled(extension: VBookExtensionEntity): PluginEntity? = extension.source?.let { src ->
        installed.firstOrNull { hostOf(it.source) == hostOf(src) }
    } ?: installed.firstOrNull {
        // ext không có source mới được fallback name — ext CÓ source mà host
        // không khớp plugin nào = mirror chưa cài, hiện nút "Cài"
        extension.source == null && it.name.equals(extension.name, ignoreCase = true)
    }

    private fun hostOf(source: String?): String? = source
        ?.removePrefix("https://")
        ?.removePrefix("http://")
        ?.removePrefix("www.")
        ?.substringBefore("/")
        ?.lowercase()

    /** Registry rỗng (mất file .plugin sau crash) mà kho vẫn có → gợi ý cài lại thay vì trống trơn. */
    private fun maybeReinstallHint() {
        if (message == null && installed.isEmpty() && extensions.isNotEmpty()) {
            // Tone ERROR — mất nguồn là cảnh báo, không phải text thường (dogfood 260905)
            message = UiText.Res(Res.string.plugins_reinstall_hint, listOf(extensions.size), UiText.Tone.ERROR)
        }
    }

    /** Cài hoặc nâng cấp — download zip từ repo, convert, register. */
    fun install(extension: VBookExtensionEntity) = scope.launch {
        loading = true
        message = UiText.Res(Res.string.plugins_installing, listOf(extension.name ?: ""))
        val wasInstalled = findInstalled(extension) != null
        try {
            pluginManager.awaitInitialization()
            // installPlugin = download zip + convert — blocking network/IO, chạy IO khỏi đứng main
            val ok = withContext(IoDispatcher) { service.installPlugin(extension) }
            installed = pluginManager.list()
            message =
                if (ok) {
                    UiText.Res(Res.string.plugins_installed, listOf(extension.name ?: ""), UiText.Tone.SUCCESS)
                } else {
                    UiText.Res(Res.string.plugins_install_failed, listOf(extension.name ?: ""), UiText.Tone.ERROR)
                }
            // Nguồn MỚI cài → probe browse ngầm; rỗng = nguồn chết (260906), KHÔNG chặn install
            if (ok && !wasInstalled) probeAlive(extension)
        } catch (e: Exception) {
            onInstallError(extension, e)
        } finally {
            loading = false
        }
    }

    /** Install fail: bản mã hóa → ẩn khỏi NÂNG CẤP; message kèm root cause (mạng/zip/path). */
    private fun onInstallError(
        extension: VBookExtensionEntity,
        e: Exception,
    ) {
        // Show cả root cause — VBookPluginException bọc cause thật (mạng/zip/path)
        val root = generateSequence<Throwable>(e) { it.cause }.last()
        // Bản mã hóa không bao giờ install được — đánh dấu path để ẩn khỏi NÂNG CẤP
        if (root is VBookPluginException && root.code == "encrypted") {
            extension.path?.let {
                encryptedPaths = encryptedPaths + it
                persistSettings { s -> s.copy(encryptedPluginPaths = encryptedPaths.toList()) }
            }
            log.add("[PluginState] Plugin mã hóa (encrypt: true) — ẩn khỏi NÂNG CẤP: ${extension.name}")
        }
        message =
            UiText.Res(
                Res.string.plugins_install_error,
                listOf(
                    e.message ?: "",
                    if (root !== e && root.message != null) "${root::class.simpleName}: ${root.message}" else "",
                ),
                UiText.Tone.ERROR,
            )
    }

    /**
     * Probe browse ngầm sau khi cài mới (260906): home rỗng → nguồn chết, cảnh báo
     * + mark broken. Cùng code path SourceBrowseState (BrowseLoader.menu).
     * ponytail: withTimeout không cắt được thread Rhino đang chạy — chấp nhận 1 thread
     * rác tối đa 10s; chỉ probe plugin mới cài, không quét cả kho 216 nguồn.
     */
    private fun probeAlive(extension: VBookExtensionEntity) = scope.launch {
        val plugin = findInstalled(extension) ?: return@launch
        if (plugin.homeGetter.isNullOrEmpty()) return@launch
        val tabs =
            withContext(IoDispatcher) {
                runCatching {
                    kotlinx.coroutines.withTimeout(10_000) {
                        dev.haipham22.leechtext.plugin.js.loader.BrowseLoader
                            .with(plugin, log)
                            .menu("home")
                    }
                }.getOrNull()
            }
        if (tabs.isNullOrEmpty()) {
            plugin.name?.let { markBroken(it) }
            message =
                UiText.Res(
                    Res.string.plugins_install_dead_warn,
                    listOf(extension.name ?: ""),
                    UiText.Tone.ERROR,
                )
        }
    }

    fun uninstall(plugin: PluginEntity) = scope.launch {
        // remove xóa file .plugin/.zip — IO, không block main
        withContext(IoDispatcher) { pluginManager.remove(plugin) }
        installed = pluginManager.list()
        message = UiText.Res(Res.string.plugins_uninstalled, listOf(plugin.name ?: ""))
    }

    /** Thêm repo nguồn — lưu repository.json, list plugin lấy lại từ default + repo mới. */
    fun addRepo() = scope.launch {
        val url = newRepoUrl.trim()
        if (url.isEmpty()) return@launch
        // ponytail: getAvailablePlugins() mặc định chỉ default registry — repo mới được
        // PluginUpdate dùng ở lần check sau; mở rộng fetch theo repo khi cần duyệt ngay
        val ok =
            withContext(IoDispatcher) {
                repositoryManager.add(url)
            }
        if (ok) {
            repos =
                withContext(IoDispatcher) {
                    repositoryManager.repositoryList()
                }
            newRepoUrl = ""
            message = UiText.Res(Res.string.plugins_repo_added, listOf(url), UiText.Tone.SUCCESS)
        } else {
            message = UiText.Res(Res.string.plugins_repo_invalid, listOf(url), UiText.Tone.ERROR)
        }
    }

    fun removeRepo(repo: dev.haipham22.leechtext.entities.RepositoryEntity) = scope.launch {
        withContext(IoDispatcher) {
            repositoryManager.remove(repo)
            repos = repositoryManager.repositoryList()
        }
        message = UiText.Res(Res.string.plugins_repo_removed)
    }

    /** Kiểm tra site còn sống — HEAD request, timeout 5s. Kết quả vào message. */
    fun testSite(url: String?) = scope.launch {
        if (url.isNullOrBlank()) return@launch
        message = UiText.Res(Res.string.plugins_testing, listOf(url))
        try {
            // Blocking HTTP — chạy IO khỏi đứng main (dogfood: bấm test site đứng app vài giây)
            val code =
                withContext(IoDispatcher) {
                    dev.haipham22.leechtext.plugin.js.api
                        .Http(log)
                        .get(url)
                        .statusCode()
                }
            message =
                if (code in 200..399) {
                    UiText.Res(Res.string.plugins_test_ok, listOf(url, code), UiText.Tone.SUCCESS)
                } else {
                    UiText.Res(Res.string.plugins_test_bad, listOf(url, code), UiText.Tone.ERROR)
                }
        } catch (e: Exception) {
            message = UiText.Res(Res.string.plugins_test_unreachable, listOf(url, e.message ?: ""), UiText.Tone.ERROR)
        }
    }

    /** NÂNG CẤP mọi plugin đã cài có bản mới (260905) — tuần tự, báo tiến trình x/y. */
    fun installAll() = scope.launch {
        val pending =
            extensions.filter { ext ->
                val installed = findInstalled(ext)
                installed != null && isUpgradable(ext, installed) && ext.path !in encryptedPaths
            }
        if (pending.isEmpty()) {
            message = UiText.Res(Res.string.plugins_none_to_install)
            return@launch
        }
        loading = true
        var ok = 0
        try {
            pluginManager.awaitInitialization()
            pending.forEachIndexed { i, ext ->
                currentCoroutineContext().ensureActive()
                message = UiText.Res(Res.string.plugins_installing_n, listOf(i + 1, pending.size, ext.name ?: ""))
                if (runCatching { withContext(IoDispatcher) { service.installPlugin(ext) } }.getOrDefault(false)) ok++
                installed = pluginManager.list()
            }
            message = UiText.Res(Res.string.plugins_installed_n, listOf(ok, pending.size))
        } finally {
            loading = false
        }
    }

    /** Extension có bản mới trong kho — cùng thang version /10 như PluginScreen (260902). */
    private fun isUpgradable(
        ext: VBookExtensionEntity,
        installed: PluginEntity,
    ): Boolean {
        val repoV = ext.version?.let { it / 10.0 } ?: return false
        return repoV > (installed.version ?: 0.0)
    }
}
