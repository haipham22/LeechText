package dev.haipham22.leechtext.plugin

import dev.haipham22.leechtext.entities.PluginEntity
import dev.haipham22.leechtext.log.EngineLogger
import dev.haipham22.leechtext.models.Repository
import dev.haipham22.leechtext.plugin.js.api.Http
import dev.haipham22.leechtext.plugin.vbook.VBookRepositoryClient
import dev.haipham22.leechtext.util.EnginePaths
import dev.haipham22.leechtext.util.LeechJson
import dev.haipham22.leechtext.util.writeTo
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/**
 * Check update plugin từ repository (port từ plugin/PluginUpdate.java; P5.2c common:
 * org.json → kotlinx.serialization, File → okio path string).
 */
@Suppress("LongParameterList") // deps engine DI — gộp thành config object chỉ thêm lớp vô ích
class PluginUpdate(
    private val log: EngineLogger,
    private val pluginManager: PluginManager,
    private val repositoryManager: RepositoryManager,
) {
    /**
     * Check update mọi repo enabled — chạy song song (thay legacy
     * Executors.newFixedThreadPool(MAX_CONN)).
     */
    suspend fun checkUpdate() {
        val repos = repositoryManager.repositoryList()
        if (repos.isEmpty()) return

        coroutineScope {
            repos
                .filter { it.isEnabled }
                .map { repo ->
                    async {
                        try {
                            checkUpdate(repo.link ?: return@async)
                        } catch (e: Exception) {
                            log.add(e)
                        }
                    }
                }.awaitAll()
        }
    }

    fun checkUpdate(repositoryLink: String?) {
        try {
            val js = VBookRepositoryClient.fetchRepoJson(repositoryLink ?: return, log)
            if (js.isEmpty()) {
                log.add("[PluginUpdate] Empty or null response from repository")
                return
            }

            // Try vBook Repository format trước — Gson trả null khi response là array
            // (legacy format); kotlinx throw nên bọc runCatching giữ fallback
            val repository = runCatching { LeechJson.decodeFromString<Repository>(js) }.getOrNull()
            val repoPlugins = repository?.plugins
            if (!repoPlugins.isNullOrEmpty()) {
                log.add(
                    "[PluginUpdate] vBook Repository format detected, plugins: " +
                        repoPlugins.size,
                )
                checkUpdateVBook(repository)
                return
            }

            // Fallback legacy array format
            log.add("[PluginUpdate] Trying legacy array format...")
            val objArr = LeechJson.parseToJsonElement(js).jsonArray
            log.add("[PluginUpdate] Legacy format detected, plugins: ${objArr.size}")
            checkUpdateLegacy(objArr)
        } catch (e: Exception) {
            log.add("[PluginUpdate] Error checking update: ${e.message}")
        }
    }

    /** Check update cho plugin vBook format. */
    private fun checkUpdateVBook(repository: Repository) {
        for (pluginMeta in repository.plugins ?: return) {
            try {
                val existingPlugin = pluginManager.get(pluginMeta.path ?: continue) ?: continue

                // pluginMeta.version là thang int repo (13 = 1.3) — chuẩn hóa /10
                // như PluginScreen; raw 13 > 1.3 → báo UPDATE ma vĩnh viễn (260905)
                val repoVersion = pluginMeta.version / 10.0
                if (repoVersion > (existingPlugin.version ?: 0.0)) {
                    log.add(
                        "[PluginUpdate] UPDATE: ${existingPlugin.name} " +
                            "v${existingPlugin.version} -> v$repoVersion",
                    )
                } else {
                    log.add(
                        "[PluginUpdate] UP-TO-DATE: ${existingPlugin.name} " +
                            "v${existingPlugin.version}",
                    )
                }
            } catch (e: Exception) {
                log.add(
                    "[PluginUpdate] Error processing vBook plugin ${pluginMeta.name}: " +
                        e.message,
                )
            }
        }
    }

    /** Check update cho plugin legacy format. */
    private fun checkUpdateLegacy(plugins: JsonArray) {
        var updateCount = 0
        var downloadCount = 0

        for (pluginJson in plugins) {
            val meta = parseLegacyPluginMeta(pluginJson) ?: continue
            try {
                val existingPlugin = pluginManager.list().firstOrNull { meta.uuid == it.uuid }
                if (existingPlugin == null) {
                    log.add("[PluginUpdate] NEW PLUGIN: ${meta.name} v${meta.remoteVersion}")
                    downloadLegacyPlugin(meta.obj, meta.name, meta.uuid)
                    downloadCount++
                } else {
                    updateCount += updateLegacyIfNewer(meta.obj, existingPlugin, meta.remoteVersion)
                }
            } catch (e: Exception) {
                log.add("[PluginUpdate] Error processing legacy plugin ${meta.name}: ${e.message}")
            }
        }

        log.add("[PluginUpdate] Legacy update complete: $updateCount updates, $downloadCount new plugins")
    }

    private data class LegacyPluginMeta(
        val obj: JsonObject,
        val uuid: String,
        val name: String,
        val remoteVersion: Double,
    )

    /** Parse metadata 1 plugin legacy; null khi JSON hỏng hoặc thiếu uuid/name/version. */
    private fun parseLegacyPluginMeta(pluginJson: JsonElement): LegacyPluginMeta? {
        val obj = runCatching { pluginJson.jsonObject }.getOrNull() ?: return null
        val uuid = obj.str("uuid")
        val name = obj.str("name")
        val remoteVersion = (obj["version"] as? JsonPrimitive)?.doubleOrNull
        return if (uuid != null && name != null && remoteVersion != null) {
            LegacyPluginMeta(obj, uuid, name, remoteVersion)
        } else {
            null
        }
    }

    /** UPDATE log + ghi đè file khi remote mới hơn; UP-TO-DATE log khi không. Trả 1 nếu đã update. */
    private fun updateLegacyIfNewer(
        obj: JsonObject,
        existingPlugin: PluginEntity,
        remoteVersion: Double,
    ): Int {
        if (remoteVersion <= (existingPlugin.version ?: 0.0)) {
            log.add(
                "[PluginUpdate] UP-TO-DATE: ${existingPlugin.name} " +
                    "v${existingPlugin.version}",
            )
            return 0
        }
        log.add(
            "[PluginUpdate] UPDATE: ${existingPlugin.name} " +
                "v${existingPlugin.version} -> v$remoteVersion",
        )
        updateLegacyPlugin(obj, existingPlugin)
        return 1
    }

    private fun updateLegacyPlugin(
        obj: JsonObject,
        existingPlugin: PluginEntity,
    ) {
        try {
            val path = "${pluginsDir()}/${existingPlugin.uuid}.plugin"
            val json = Http(log).request(obj.str("url") ?: return).string()

            val entity = LeechJson.decodeFromString<PluginEntity>(json)
            entity.checked = true
            LeechJson.encodeToString(entity).writeTo(path, log = log)
            existingPlugin.apply(entity)
            pluginManager.notifyPluginsChanged()

            log.add("[PluginUpdate] ✓ Updated: ${existingPlugin.name} v${existingPlugin.version}")
            // ponytail: Toast Swing bỏ — thay bằng log; thêm notification khi có UI layer
            log.add("Đã update plugin ${existingPlugin.name} v${existingPlugin.version}")
        } catch (e: Exception) {
            log.add("[PluginUpdate] ✗ Update failed for ${existingPlugin.name}: ${e.message}")
        }
    }

    private fun downloadLegacyPlugin(
        obj: JsonObject,
        pluginName: String,
        pluginUuid: String,
    ) {
        try {
            val path = "${pluginsDir()}/$pluginUuid.plugin"
            log.add("[PluginUpdate] Downloading: $pluginName from ${obj.str("url")}")

            val json = Http(log).request(obj.str("url") ?: return).string()
            val entity = LeechJson.decodeFromString<PluginEntity>(json)
            entity.checked = true
            LeechJson.encodeToString(entity).writeTo(path, log = log)
            pluginManager.add(path)

            log.add("[PluginUpdate] ✓ Downloaded: $pluginName")
            log.add("Đã tải xuống plugin $pluginName")
        } catch (e: Exception) {
            log.add("[PluginUpdate] ✗ Download failed for $pluginName: ${e.message}")
        }
    }

    private fun pluginsDir(): String = (EnginePaths.dataDir / "tools" / "plugins").toString()
}

private fun JsonObject.str(name: String): String? = (this[name] as? JsonPrimitive)?.takeIf { it !is kotlinx.serialization.json.JsonNull }?.content
