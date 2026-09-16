package dev.haipham22.leechtext.plugin

import dev.haipham22.leechtext.entities.PluginEntity
import dev.haipham22.leechtext.log.EngineLogger
import dev.haipham22.leechtext.util.EnginePaths
import dev.haipham22.leechtext.util.LeechJson
import dev.haipham22.leechtext.util.deleteLogged
import dev.haipham22.leechtext.util.monitorLock
import dev.haipham22.leechtext.util.readTextOrNull
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import kotlin.concurrent.Volatile
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * Quản lý plugin load/install/retrieve (port từ plugin/PluginManager.java; P5.2c common:
 * okio thay java.io, list thread-safe qua lock — CopyOnWriteArrayList là JVM-only).
 * DI class (Koin singleton); legacy background Thread → coroutine; listener gọi trực tiếp.
 */
@OptIn(ExperimentalAtomicApi::class)
@Suppress("TooManyFunctions") // API surface cho plugin JS — số hàm mirror đúng API vBook, không tách
class PluginManager(
    private val log: EngineLogger,
) {
    /** Listener cho plugin list changes. */
    fun interface PluginListListener {
        fun onPluginsChanged()
    }

    private val lock = Any()
    private val pluginList = ArrayList<PluginEntity>()
    private val listeners = ArrayList<PluginListListener>()

    @Volatile
    private var initialized = false
    private val initializedSignal = CompletableDeferred<Unit>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val initOnce = AtomicInt(0)

    private val fs: FileSystem get() = FileSystem.SYSTEM

    /**
     * Init một lần (idempotent) — load plugin từ đĩa + dedupe; onLoaded hook cho caller
     * orchestration (vd chạy check update sau khi list sẵn sàng) — thay side-effect init cũ.
     */
    fun initialize(onLoaded: () -> Unit = {}) {
        if (initOnce.compareAndSet(0, 1)) {
            scope.launch {
                loadFromDisk()
                dedupeByName()
                initialized = true
                initializedSignal.complete(Unit)
                onLoaded()
            }
        }
    }

    private fun pluginsDir(): Path = EnginePaths.dataDir / "tools" / "plugins"

    private fun loadFromDisk() {
        val dir = pluginsDir()
        val files =
            try {
                fs.list(dir)
            } catch (e: Exception) {
                return
            }
        for (f in files) {
            if (f.name.endsWith(".plugin")) {
                try {
                    val plugin = createPlugin(f.toString())
                    // loadFromDisk chạy coroutine nền (Default) — add phải qua lock như
                    // list()/remove(), không thì get() đang duyệt ArrayList có thể crash.
                    // Guard uuid: init-coroutine + reloadFromDisk có thể chạy chồng nhau →
                    // cùng file load 2 lần → dedupe xóa "bản trùng" = XÓA NHẦM FILE THẬT
                    // (mất toàn bộ plugin — bug 2026-09-02)
                    monitorLock(lock) {
                        if (pluginList.none { it.uuid == plugin.uuid }) pluginList.add(plugin)
                    }
                } catch (e: Exception) {
                    log.add(e)
                }
            }
        }
    }

    /** Tên chuẩn hoá nhận diện cùng nguồn — bỏ "(vBook)", lowercase, trim. */
    private fun normalizeName(name: String?): String = name?.replace("(vBook)", "", ignoreCase = true)?.trim()?.lowercase() ?: ""

    /**
     * Dedupe key: name chuẩn hoá + host của source. Mirror family vBook (vd
     * truyenfull.today v0.3 / truyenfull.live v1.3) cùng tên nhưng KHÁC source là 2
     * plugin thật — key theo name không sẽ xóa nhầm file bản thấp (bug 2026-08-25).
     * Key name+source vẫn gộp được case cũ: "X (vBook)" v0.3 + "X" v1.3 cùng site.
     */
    private fun dedupeKey(p: PluginEntity): String {
        val host = p.source?.removePrefix("https://")?.removePrefix("http://")?.substringBefore("/")?.lowercase() ?: ""
        return "${normalizeName(p.name)}|$host"
    }

    /**
     * Cùng dedupe key → giữ version cao, xóa hẳn bản thấp (kể cả file).
     * Fix: plugin cũ "X (vBook)" v0.3 + mới "X" v1.3 — regex khác nhau nên dedup theo
     * regex không bắt được, thành 2 mục trong tab Nguồn.
     */
    private fun dedupeByName() {
        val winners = HashMap<String, PluginEntity>()
        val losers = ArrayList<PluginEntity>()
        monitorLock(lock) {
            for (p in pluginList) {
                val key = dedupeKey(p)
                val winner = winners[key]
                when {
                    winner == null -> winners[key] = p

                    // Cùng uuid = cùng 1 file load 2 lần (init + reload chồng) — bỏ qua,
                    // KHÔNG xóa file (bug mất plugin 2026-09-02)
                    winner.uuid == p.uuid -> Unit

                    (p.version ?: 0.0) > (winner.version ?: 0.0) -> {
                        losers.add(winner)
                        winners[key] = p
                    }

                    else -> losers.add(p)
                }
            }
        }
        losers.forEach {
            log.add("[dedupeByName] remove duplicate: ${it.name} v${it.version} (giữ ${winners[dedupeKey(it)]?.name})")
            remove(it) // xóa cả file .plugin
        }
    }

    /** Đăng ký listener nhận notify khi plugin add/remove. */
    fun addListener(listener: PluginListListener) {
        monitorLock(lock) { listeners.add(listener) }
    }

    /** Hủy đăng ký listener. */
    fun removeListener(listener: PluginListListener) {
        monitorLock(lock) { listeners.remove(listener) }
    }

    /** Notify mọi listener rằng plugin list đã đổi (thay legacy SwingUtilities). */
    private fun notifyListeners() {
        for (listener in listeners.toList()) {
            try {
                listener.onPluginsChanged()
            } catch (e: Exception) {
                log.add(e)
            }
        }
    }

    /** Trigger listener notification public — gọi sau khi update plugins. */
    fun notifyPluginsChanged() {
        notifyListeners()
    }

    /**
     * Reload plugins từ disk — Android gọi sau EnginePaths.init(filesDir) vì
     * static initializer chạy trước khi path đúng, load từ sai dir.
     * Chờ load đầu tiên xong rồi mới clear+load lại — 2 coroutine chồng nhau từng
     * làm dedupe xóa nhầm file plugin thật (bug mất nguồn 2026-09-02).
     */
    fun reloadFromDisk() {
        scope.launch {
            awaitInitialization()
            monitorLock(lock) { pluginList.clear() }
            loadFromDisk()
            dedupeByName()
            notifyListeners()
        }
    }

    fun add(path: String) {
        add(createPlugin(path))
    }

    /**
     * Add plugin entity trực tiếp — thread-safe. Plugin trùng regex thì update existing.
     * Sau add/update luôn dedupe name+host: nâng cấp cùng nguồn mà regex đổi giữa 2
     * version (truyenfull v1.5→v1.7) không match regex → để lại 2 entry, UI vẫn thấy
     * bản cũ ("Nâng cấp" no-op — bug 260902). Giữ version cao, xóa file bản thấp —
     * cùng logic khi load từ disk.
     */
    fun add(plugin: PluginEntity?) {
        if (plugin == null || plugin.regex == null) return

        // Check duplicate + update nếu tồn tại
        monitorLock(lock) {
            for (existing in pluginList) {
                if (existing.regex == plugin.regex) {
                    // Update existing plugin
                    existing.apply(plugin)
                    return@monitorLock
                }
            }
            // Add plugin mới
            pluginList.add(plugin)
            notifyListeners()
        }
        dedupeByName()
    }

    private fun createPlugin(path: String): PluginEntity = try {
        val text = path.toPath().readTextOrNull() ?: error("Plugin file unreadable: $path")
        LeechJson.decodeFromString<PluginEntity>(text)
    } catch (e: Exception) {
        log.add(e)
        throw e
    }

    /**
     * Plugin match URL — chỉ trả plugin đã cài local.
     *
     * @return Plugin match, null nếu không tìm thấy
     */
    fun get(url: String): PluginEntity? = monitorLock(lock) { pluginList.toList() }
        .filter { url.matches(Regex("(https?://)?${it.regex ?: return@filter false}")) }
        .maxByOrNull { it.priority } // vBook priority — cao thắng khi trùng regex
        ?.also { log.debug("[plugin.get] $url → ${it.name}") }

    fun list(): List<PluginEntity> = monitorLock(lock) { pluginList.toList() }

    /**
     * Remove plugin — thread-safe, xóa file .plugin/.zip tương ứng.
     *
     * @return true nếu tìm thấy + removed
     */
    fun remove(plugin: PluginEntity?): Boolean {
        if (plugin == null) return false

        return monitorLock(lock) {
            val removed = pluginList.remove(plugin)
            if (removed) {
                val dir = pluginsDir()

                // Vẫn còn entity khác cùng uuid (file load 2 lần do init+reload chồng) —
                // chỉ bỏ khỏi list, KHÔNG xóa file (nguồn duy nhất trên đĩa)
                val stillReferenced = pluginList.any { it.uuid == plugin.uuid }
                if (!stillReferenced) {
                    // Delete file .plugin
                    (dir / "${plugin.uuid}.plugin").deleteLogged("PluginManager", log)

                    // Delete file .zip nếu có
                    (dir / "${plugin.uuid}.zip").deleteLogged("PluginManager", log)
                }

                notifyListeners()
            }
            removed
        }
    }

    /** Đã load xong plugin từ disk chưa. */
    fun isInitialized(): Boolean = initialized

    /**
     * Wờ initialization xong (max 5s) — gọi trước list() để chắc chắn plugin đã load.
     */
    suspend fun awaitInitialization() {
        withTimeoutOrNull(5000) { initializedSignal.await() }
            ?: log.add("PluginManager initialization timeout")
    }
}
