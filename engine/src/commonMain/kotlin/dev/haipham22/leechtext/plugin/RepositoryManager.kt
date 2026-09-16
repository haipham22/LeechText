package dev.haipham22.leechtext.plugin

import dev.haipham22.leechtext.entities.RepositoryEntity
import dev.haipham22.leechtext.log.EngineLogger
import dev.haipham22.leechtext.util.EnginePaths
import dev.haipham22.leechtext.util.LeechJson
import dev.haipham22.leechtext.util.readTextOrNull
import dev.haipham22.leechtext.util.writeTo
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Quản lý repo plugin đã lưu local (port từ plugin/RepositoryManager.java + RepositoryUI).
 * DI class (Koin singleton), đọc tools/repository.json (format: Gson List&lt;RepositoryEntity&gt; như bản gốc).
 * P3 thêm add/remove + persist (MUST #17).
 */
@OptIn(ExperimentalUuidApi::class)
class RepositoryManager(
    private val log: EngineLogger,
) {
    private val repositoryList: MutableList<RepositoryEntity> = loadRepositories()

    private fun loadRepositories(): MutableList<RepositoryEntity> {
        // Chưa từng lưu repo là trạng thái hợp lệ — trả rỗng, không log stack
        if (!okio.FileSystem.SYSTEM.exists(repositoryFile())) return ArrayList()
        return try {
            val json = repositoryFile().readTextOrNull() ?: return ArrayList()
            LeechJson.decodeFromString<List<RepositoryEntity>>(json).toMutableList()
        } catch (e: Exception) {
            log.add(e)
            ArrayList()
        }
    }

    private fun repositoryFile(): okio.Path = EnginePaths.dataDir / "tools" / "repository.json"

    /** Có thiết lập repo chưa (file repository.json tồn tại + đọc được). */
    fun hasRepoSetting(): Boolean = try {
        repositoryFile().readTextOrNull()?.isNotEmpty() == true
    } catch (e: Exception) {
        false
    }

    fun repositoryList(): List<RepositoryEntity> = repositoryList

    /** Thêm repo theo link (enabled). Trả false nếu link rác hoặc đã có. */
    fun add(link: String): Boolean {
        val trimmed = link.trim()
        if (!trimmed.startsWith("http")) return false
        if (repositoryList.any { it.link == trimmed }) return false
        val entity =
            RepositoryEntity(
                uuid = Uuid.random().toString(),
                link = trimmed,
                isEnabled = true,
            )
        repositoryList.add(entity)
        persist()
        return true
    }

    fun remove(entity: RepositoryEntity) {
        repositoryList.removeAll { it.link == entity.link }
        persist()
    }

    fun setEnabled(
        entity: RepositoryEntity,
        enabled: Boolean,
    ) {
        repositoryList.firstOrNull { it.link == entity.link }?.isEnabled = enabled
        persist()
    }

    /** Ghi repository.json — format Gson list như bản gốc. */
    private fun persist() {
        try {
            LeechJson.encodeToString(repositoryList).writeTo(repositoryFile().toString(), log = log)
        } catch (e: Exception) {
            log.add(e)
        }
    }
}
