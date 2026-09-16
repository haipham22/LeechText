package dev.haipham22.leechtext.plugin.util

import dev.haipham22.leechtext.entities.PluginEntity
import dev.haipham22.leechtext.log.EngineLogger
import dev.haipham22.leechtext.util.EnginePaths
import dev.haipham22.leechtext.util.LeechJsonPretty
import okio.FileSystem
import okio.IOException
import okio.Path

/**
 * Plugin validation + persistence (port từ util/PluginPersistence.java). Atomic file ops +
 * validation. Plugin dir theo EnginePaths.dataDir — engine không phụ thuộc legacy AppUtils.
 * P5.2b: java.nio.file → okio (KMP); Pattern.compile → Kotlin Regex (cùng java.util.regex
 * bên JVM).
 */
class PluginPersistence(
    private val log: EngineLogger,
) {

    /**
     * Validate plugin trước khi install: required fields, regex compile, có ít nhất một script
     * getter.
     */
    @Suppress("ReturnCount") // validator: mỗi rule vi phạm thoát sớm với message riêng
    fun validate(plugin: PluginEntity?): Boolean {
        if (plugin == null) return false

        // Required fields
        if (plugin.name.isNullOrEmpty()) {
            log.add("Plugin validation failed: missing name")
            return false
        }

        val regexPattern = plugin.regex
        if (regexPattern.isNullOrEmpty()) {
            log.add("Plugin validation failed: missing regex pattern")
            return false
        }

        // Regex compile được
        try {
            Regex(regexPattern)
        } catch (e: Exception) {
            log.add("Plugin validation failed: invalid regex pattern - ${e.message}")
            return false
        }

        // Có ít nhất một script getter
        val hasScript =
            !plugin.chapGetter.isNullOrEmpty() || !plugin.tocGetter.isNullOrEmpty()

        if (!hasScript) {
            log.add("Plugin validation failed: no script content")
            return false
        }

        return true
    }

    /**
     * Save plugin entity vào file .plugin atomically — temp file + atomic move chống partial
     * writes.
     *
     * @return Path file .plugin đã lưu
     * @throws IOException nếu save fail
     */
    @Throws(IOException::class)
    fun saveAtomic(plugin: PluginEntity): Path {
        val fs = FileSystem.SYSTEM
        val pluginDirPath = pluginsDir()
        fs.createDirectories(pluginDirPath)

        // UUID cho filename nhất quán (match delete logic)
        val filename = "${plugin.uuid}.plugin"
        val filePath = pluginDirPath / filename
        val tempPath = pluginDirPath / "$filename.tmp"

        // Convert sang JSON
        val json = LeechJsonPretty.encodeToString(plugin)

        // Write temp file trước
        fs.write(tempPath) { write(json.encodeToByteArray()) }

        // Atomic move đến dest cuối
        try {
            fs.atomicMove(tempPath, filePath)
            log.add("Store plugin $filename to $tempPath")
        } catch (e: Exception) {
            // Cleanup temp file khi fail
            try {
                if (fs.exists(tempPath)) fs.delete(tempPath)
            } catch (_: IOException) {
                // Cleanup temp fail (file bị lock) chỉ để lại file rác .tmp — move đã fail sẵn
            }
            throw e
        }

        return filePath
    }

    /** Plugins directory path. */
    fun getPluginsDirectory(): Path = pluginsDir()

    /** Plugin dir theo EnginePaths.dataDir (ổn định, không phụ thuộc CWD). */
    private fun pluginsDir(): Path = EnginePaths.dataDir / "tools" / "plugins"
}
