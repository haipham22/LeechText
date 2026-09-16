package dev.haipham22.leechtext.plugin.js.api

import dev.haipham22.leechtext.log.EngineLogger
import dev.haipham22.leechtext.util.EnginePaths
import dev.haipham22.leechtext.util.deleteLogged
import okio.FileSystem
import okio.Path

/**
 * LocalStorage API cho plugin (port từ LocalStorage.java; P5.2c: okio thay java.io).
 * File per key dưới <dataDir>/plugin_storage/<pluginId>/, quota 10KB/value, 1MB total.
 * P5.2c: dùng EnginePaths.dataDir (honor Android init) thay hardcode user.home.
 */
class LocalStorage(
    private val log: EngineLogger,
    pluginId: String?,
) {
    private val pluginId: String = pluginId ?: "default"
    private val storageDir: Path
    private val fs: FileSystem = FileSystem.SYSTEM

    init {
        storageDir = EnginePaths.dataDir / "plugin_storage" / this.pluginId
        runCatching { fs.createDirectories(storageDir) }
    }

    @Suppress("ReturnCount") // builder style — mỗi guard log + trả this
    fun setItem(
        key: String?,
        value: String?,
    ): LocalStorage {
        if (key.isNullOrEmpty()) {
            log.add("[LocalStorage] Key is null or empty")
            return this
        }
        if (value == null) {
            log.add("[LocalStorage] Value is null, removing key: $key")
            return removeItem(key)
        }
        try {
            if (!isValidKey(key)) {
                log.add("[LocalStorage] Invalid key format: $key")
                return this
            }
            val valueBytes = value.encodeToByteArray()
            if (valueBytes.size > MAX_VALUE_SIZE) {
                log.add("[LocalStorage] Value too large: ${valueBytes.size} bytes")
                return this
            }
            val totalSize = currentStorageSize()
            if (totalSize + valueBytes.size > MAX_TOTAL_SIZE) {
                log.add("[LocalStorage] Storage quota exceeded: $totalSize + ${valueBytes.size}")
                return this
            }
            fs.write(storageDir / "$key.txt") { write(valueBytes) }
        } catch (e: Exception) {
            log.add("[LocalStorage] Failed to set item: ${e.message}")
        }
        return this
    }

    fun getItem(key: String?): String? {
        if (key.isNullOrEmpty()) return null
        if (!isValidKey(key)) return null
        return try {
            val file = storageDir / "$key.txt"
            if (!fs.exists(file)) return null
            fs.read(file) { readUtf8() }
        } catch (e: Exception) {
            log.add("[LocalStorage] Failed to get item: ${e.message}")
            null
        }
    }

    fun removeItem(key: String?): LocalStorage {
        if (key.isNullOrEmpty()) return this
        if (!isValidKey(key)) return this
        return try {
            (storageDir / "$key.txt").deleteLogged("LocalStorage", log)
            this
        } catch (e: Exception) {
            log.add("[LocalStorage] Failed to remove item: ${e.message}")
            this
        }
    }

    fun clear() {
        try {
            listFiles().forEach { if (fs.metadataOrNull(it)?.isRegularFile == true) fs.delete(it) }
        } catch (e: Exception) {
            log.add("[LocalStorage] Failed to clear: ${e.message}")
        }
    }

    fun getLength(): Int = listFiles().size

    fun getKey(index: Int): String? {
        val files = listFiles()
        if (index < 0 || index >= files.size) return null
        val file = files[index]
        return if (fs.metadataOrNull(file)?.isRegularFile == true) file.name.dropLast(4) else null
    }

    fun hasOwnProperty(key: String?): Boolean {
        if (key.isNullOrEmpty()) return false
        return fs.exists(storageDir / "$key.txt")
    }

    fun getKeys(): Array<String> = listFiles()
        .filter { fs.metadataOrNull(it)?.isRegularFile == true }
        .map { it.name.dropLast(4) }
        .toTypedArray()

    private fun listFiles(): List<Path> = runCatching { fs.list(storageDir) }.getOrDefault(emptyList())

    private fun isValidKey(key: String): Boolean = key.matches(Regex("^[a-zA-Z0-9_-]+$"))

    private fun currentStorageSize(): Long = listFiles().sumOf { fs.metadataOrNull(it)?.size ?: 0L }

    companion object {
        private const val MAX_VALUE_SIZE = 10 * 1024
        private const val MAX_TOTAL_SIZE = 1024 * 1024
    }
}
