package dev.haipham22.leechtext.plugin.vbook

import dev.haipham22.leechtext.entities.PluginEntity
import dev.haipham22.leechtext.log.EngineLogger
import dev.haipham22.leechtext.plugin.PluginManager
import dev.haipham22.leechtext.plugin.util.PluginPersistence
import dev.haipham22.leechtext.plugin.vbook.exception.VBookPluginException
import dev.haipham22.leechtext.plugin.vbook.exception.VBookRegistryException
import dev.haipham22.leechtext.plugin.vbook.model.VBookExtensionEntity
import dev.haipham22.leechtext.util.EnginePaths
import dev.haipham22.leechtext.util.LeechJson
import dev.haipham22.leechtext.util.monitorLock
import dev.haipham22.leechtext.util.parseHost
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import okio.FileSystem

/**
 * Orchestrator download + convert + install vBook plugin (port từ
 * vbook/VBookPluginService.java). Thread-safe, atomic installation.
 */
class VBookPluginService(
    private val log: EngineLogger,
    private val repositoryClient: VBookRepositoryClient = VBookRepositoryClient(log),
    private val extractor: PluginZipExtractor = PluginZipExtractor(log),
    private val converter: VBookToLeechTextConverter = VBookToLeechTextConverter(),
    private val pluginPersistence: PluginPersistence = PluginPersistence(log),
    private val pluginManager: PluginManager,
) {
    companion object {
        // Lock cho atomic installation + track đang chạy — chống duplicate
        private val installationLock = Any()
        private val installingPlugins = HashSet<String>()
    }

    /** List plugin có sẵn từ repository. */
    @Throws(VBookRegistryException::class)
    fun getAvailablePlugins(repositoryUrl: String): List<VBookExtensionEntity> = repositoryClient.fetchExtensions(repositoryUrl)

    /** List plugin từ default vBook repository. */
    @Throws(VBookRegistryException::class)
    fun getAvailablePlugins(): List<VBookExtensionEntity> = getAvailablePlugins(VBookRepositoryClient.getDefaultRegistryUrl())

    private val fs: FileSystem get() = FileSystem.SYSTEM
    private val cacheFile: okio.Path get() = EnginePaths.dataDir / "tools" / "extensions-cache.json"

    /** Cache disk extension list — UI đổ vào state trước khi fetch (stale-while-revalidate). */
    fun loadCachedExtensions(): List<VBookExtensionEntity> = try {
        if (!fs.exists(cacheFile)) {
            emptyList()
        } else {
            runCatching { LeechJson.decodeFromString<List<VBookExtensionEntity>>(fs.read(cacheFile) { readUtf8() }) }.getOrNull() ?: emptyList()
        }
    } catch (e: Exception) {
        emptyList()
    }

    /** Ghi cache disk extension list sau fetch thành công. */
    fun saveCachedExtensions(exts: List<VBookExtensionEntity>) {
        try {
            cacheFile.parent?.let { fs.createDirectories(it) }
            fs.write(cacheFile) { writeUtf8(LeechJson.encodeToString(exts)) }
        } catch (_: Exception) {
            // Cache disk chỉ là tối ưu (stale-while-revalidate) — ghi fail không làm fail caller
        }
    }

    /** Download + convert vBook plugin từ URL. */
    @Throws(VBookPluginException::class)
    fun downloadAndConvert(pluginZipUrl: String): PluginEntity = try {
        val vbookPlugin = extractor.extractFromZip(pluginZipUrl)
        converter.convert(vbookPlugin)
    } catch (e: Exception) {
        throw VBookPluginException("Failed to download plugin from: $pluginZipUrl", e)
    }

    /**
     * Install vBook extension từ repository — atomic operation.
     *
     * @return true nếu install thành công
     */
    @Throws(VBookPluginException::class)
    fun installPlugin(extension: VBookExtensionEntity): Boolean {
        val pluginKey = "${extension.name}:${extension.version}"

        // Đảm bảo working directory tồn tại
        ensureWorkingDirectory()

        return monitorLock(installationLock) {
            // Chống duplicate installation
            if (pluginKey in installingPlugins) {
                log.add("Plugin installation already in progress: ${extension.name}")
                return@monitorLock false
            }

            installingPlugins.add(pluginKey)

            try {
                // Download + convert
                val pluginEntity = downloadAndConvert(extension.path ?: "")

                // Validate trước khi install
                if (!pluginPersistence.validate(pluginEntity)) {
                    throw VBookPluginException("Plugin validation failed: ${extension.name}")
                }

                // Check name conflict
                if (hasNameConflict(pluginEntity.name)) {
                    pluginEntity.name = pluginEntity.name + " (vBook)"
                }

                // Atomic save vào plugins dir (temp file + atomic move)
                pluginPersistence.saveAtomic(pluginEntity)

                // Add vào PluginManager
                pluginManager.add(pluginEntity)

                log.add("Successfully installed plugin: ${pluginEntity.name}")
                true
            } catch (e: Exception) {
                log.add("Failed to install plugin: ${extension.name} - ${e.message}")
                // Log full exception — add(String) chỉ in message, cause chain mất → không debug được
                log.add(e)
                throw VBookPluginException("Failed to install plugin: ${extension.name}", e)
            } finally {
                installingPlugins.remove(pluginKey)
            }
        }
    }

    /**
     * Tìm + install plugin match URL với extensions có sẵn. Error được log (không silent).
     *
     * Pre-filter theo host của extension.source — chỉ tải zip ứng viên khớp host, không
     * quét cả registry (17+ zip × repo = chờ vô ích). Hủy được giữa các zip qua
     * cancellation của coroutine gọi.
     *
     * @return PluginEntity đã install, null nếu không match
     */
    suspend fun findAndInstallByUrl(targetUrl: String?): PluginEntity? { // NOSONAR — cognitive 16/15: chuỗi try auto-discovery, tách hàm làm mẻ flow
        if (targetUrl.isNullOrEmpty()) {
            log.add("Auto-discovery: empty URL provided")
            return null
        }

        return try {
            val targetHost = parseHost(targetUrl)
            val extensions = getAvailablePlugins()

            val candidates =
                if (targetHost != null) {
                    val byHost =
                        extensions.filter { ext ->
                            val host = parseHost(ext.source.orEmpty())
                            host != null && (
                                host == targetHost ||
                                    host.removePrefix("www.") == targetHost.removePrefix("www.")
                                )
                        }
                    if (byHost.isEmpty()) {
                        log.add("Auto-discovery: no plugin source matches host $targetHost")
                        return null
                    }
                    byHost
                } else {
                    extensions
                }

            candidates.forEachIndexed { index, extension ->
                currentCoroutineContext().ensureActive()
                log.add("Auto-discovery: checking ${extension.name} (${index + 1}/${candidates.size})")
                try {
                    // Download plugin để lấy regex
                    val vbookPlugin = extractor.extractFromZip(extension.path.orEmpty())

                    // Check URL match regex — prefix scheme optional như PluginManager.get
                    // (không có thì URL https:// không bao giờ full-match)
                    if (vbookPlugin.regexp != null &&
                        targetUrl.matches(Regex("(https?://)?${vbookPlugin.regexp!!}"))
                    ) {
                        // Convert + install
                        val pluginEntity = converter.convert(vbookPlugin)

                        if (hasNameConflict(pluginEntity.name)) {
                            pluginEntity.name = pluginEntity.name + " (vBook)"
                        }

                        // Atomic installation
                        pluginPersistence.saveAtomic(pluginEntity)
                        pluginManager.add(pluginEntity)

                        log.add(
                            "Auto-discovery: installed plugin ${pluginEntity.name}" +
                                " for URL: $targetUrl",
                        )
                        return pluginEntity
                    }
                } catch (e: Exception) {
                    // Log nhưng thử plugin khác
                    log.add(
                        "Auto-discovery: failed to process plugin ${extension.name}" +
                            " - ${e.message}",
                    )
                }
            }

            log.add("Auto-discovery: no matching plugin found for URL: $targetUrl")
            null
        } catch (e: Exception) {
            log.add(
                "Auto-discovery: error processing repository for URL: $targetUrl" +
                    " - ${e.message}",
            )
            null
        }
    }

    /** Plugin với name này đã tồn tại chưa. */
    private fun hasNameConflict(name: String?): Boolean {
        if (name == null) return false
        return pluginManager.list().any { name == it.name }
    }

    /**
     * Đảm bảo working directory tồn tại + writable.
     *
     * @throws VBookPluginException nếu không tạo/ghi được
     */
    @Throws(VBookPluginException::class)
    private fun ensureWorkingDirectory() {
        try {
            val workingDir = EnginePaths.dataDir / "tools" / "plugins"
            fs.createDirectories(workingDir)
        } catch (e: Exception) {
            throw VBookPluginException("Failed to ensure working directory exists", e)
        }
    }

    /** Clear cache plugin files. */
    fun clearCache() {
        try {
            val cacheDir = EnginePaths.dataDir / "tools" / "plugins" / "vbook-cache"
            if (fs.exists(cacheDir)) {
                // Xóa file trước, dir sau (list không đệ quy — cache phẳng 1 cấp zip files)
                fs.list(cacheDir).forEach { file ->
                    runCatching {
                        if (fs.metadataOrNull(file)?.isDirectory == true) {
                            fs.deleteRecursively(file)
                        } else {
                            fs.delete(file)
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Clear cache không bao giờ ném — caller chỉ dọn dẹp, lỗi không có gì để xử lý
        }
    }
}
