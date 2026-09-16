package dev.haipham22.leechtext.plugin.vbook

import dev.haipham22.leechtext.log.EngineLogger
import dev.haipham22.leechtext.plugin.js.api.HttpSpec
import dev.haipham22.leechtext.plugin.js.api.httpExecute
import dev.haipham22.leechtext.plugin.security.NetworkSecurityValidator
import dev.haipham22.leechtext.plugin.security.RegexSecurityValidator
import dev.haipham22.leechtext.plugin.security.SecurityValidationException
import dev.haipham22.leechtext.plugin.security.ZipSecurityValidator
import dev.haipham22.leechtext.plugin.vbook.exception.VBookPluginException
import dev.haipham22.leechtext.plugin.vbook.model.VBookPluginEntity
import dev.haipham22.leechtext.util.EnginePaths
import dev.haipham22.leechtext.util.ZipIo
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okio.FileSystem
import okio.Path
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Extract + parse vBook plugin.zip (port từ PluginZipExtractor.java; P5.2c common —
 * zip qua ZipIo seam, download qua HttpEngine). ZIP Slip + ReDoS protection qua
 * security validators.
 */
class PluginZipExtractor(
    private val log: EngineLogger,
) {
    private val fs: FileSystem get() = FileSystem.SYSTEM

    /** Extract plugin từ local ZIP file. */
    @Suppress("ThrowsCount") // validation chain ở trust boundary — mỗi guard ném exception có message riêng cho UI
    fun extractFromZip(zipPath: Path): VBookPluginEntity {
        if (!fs.exists(zipPath)) {
            throw VBookPluginException("ZIP file not found: $zipPath")
        }

        // Validate ZIP security threats (sau existence check)
        try {
            ZipSecurityValidator.validateZipFile(zipPath, log)
        } catch (e: SecurityValidationException) {
            throw VBookPluginException("ZIP security validation failed: ${e.message}", e)
        }

        return try {
            extractZipInternal(zipPath)
        } catch (e: VBookPluginException) {
            throw e
        } catch (e: Exception) {
            throw VBookPluginException("Failed to extract plugin from: $zipPath", e)
        }
    }

    /** Download + extract plugin từ URL. */
    fun extractFromZip(zipUrl: String): VBookPluginEntity = try {
        // Download vào cache
        val cacheDir = EnginePaths.dataDir / "tools" / "plugins" / "vbook-cache"
        fs.createDirectories(cacheDir)

        val filename = extractFilenameFromUrl(zipUrl)
        val localZipPath = cacheDir / filename

        // Remove file cũ nếu có (forces fresh download)
        runCatching { if (fs.exists(localZipPath)) fs.delete(localZipPath) }

        downloadFile(zipUrl, localZipPath)
        log.debug("[extractFromZip] downloaded ${fs.metadataOrNull(localZipPath)?.size ?: 0} bytes → $localZipPath")
        extractFromZip(localZipPath)
    } catch (e: Exception) {
        throw VBookPluginException("Failed to download plugin from: $zipUrl", e)
    }

    /** Extraction logic nội bộ (ZipIo) — có ZIP Slip protection. */
    @Suppress("ThrowsCount") // validation chain ở trust boundary — mỗi guard ném exception có message riêng cho UI
    private fun extractZipInternal(zipFile: Path): VBookPluginEntity {
        // Validate entries chống path traversal (ZIP Slip)
        validateZipEntries(zipFile)

        // Extract plugin.json (metadata)
        val pluginJson =
            extractEntryAsString(zipFile, "plugin.json")
                ?: throw VBookPluginException("plugin.json not found in ZIP")

        // Parse plugin.json → entity
        val entity = parsePluginMetadata(pluginJson)

        // Plugin mã hóa (repo bên thứ 3 flag metadata "encrypt": true) — script là
        // ciphertext, execute làm JS chỉ ra ReferenceError rác. Chặn tại install.
        val encryptFlag =
            runCatching {
                Json.parseToJsonElement(pluginJson).jsonObject["metadata"]
                    ?.jsonObject?.get("encrypt")?.jsonPrimitive?.booleanOrNull
            }.getOrNull()
        if (encryptFlag == true) {
            throw VBookPluginException("Encrypted plugin (encrypt: true) — decrypt not supported", code = "encrypted")
        }

        // Validate regex pattern từ metadata
        entity.regexp?.let { validateRegexPattern(it) }

        // Extract scripts từ src/
        val scriptContents = extractScripts(zipFile)
        entity.scriptContents = scriptContents

        // Extract icon.png
        extractIconAsBase64(zipFile)?.let { entity.iconBase64 = it }

        // Set raw metadata
        entity.rawMetadata = pluginJson

        // Validate required scripts
        if (!scriptContents.containsKey("chap") &&
            !scriptContents.containsKey("toc") &&
            !scriptContents.containsKey("gen")
        ) {
            throw VBookPluginException("Plugin must have at least chap.js or toc.js")
        }

        return entity
    }

    /** Validate ZIP entries chống ZIP Slip. */
    private fun validateZipEntries(zip: Path) {
        try {
            for (entry in ZipIo.entries(zip, log)) {
                ZipSecurityValidator.validateEntryName(entry.name)
            }
        } catch (e: SecurityValidationException) {
            throw VBookPluginException("ZIP entry validation failed", e)
        } catch (e: Exception) {
            throw VBookPluginException("Failed to validate ZIP entries", e)
        }
    }

    /** Validate regex pattern chống ReDoS. */
    private fun validateRegexPattern(regex: String) {
        try {
            RegexSecurityValidator.validateRegex(regex)
        } catch (e: SecurityValidationException) {
            throw VBookPluginException("Regex validation failed", e)
        }
    }

    /** Parse plugin.json metadata → entity. */
    private fun parsePluginMetadata(json: String): VBookPluginEntity = try {
        val root = Json.parseToJsonElement(json).jsonObject
        val entity =
            VBookPluginEntity(
                scripts = HashMap(),
                scriptContents = HashMap(),
            )

        // Metadata section
        (root["metadata"] as? JsonObject)?.let { metadata ->
            entity.name = getString(metadata, "name")
            entity.author = getString(metadata, "author")
            entity.version = getInt(metadata, "version")
            entity.source = getString(metadata, "source")
            entity.regexp = getString(metadata, "regexp")
            entity.description = getString(metadata, "description")
            entity.locale = getString(metadata, "locale")
            entity.type = getString(metadata, "type")
            entity.language = getString(metadata, "language")
            entity.priority = getInt(metadata, "priority")
            entity.tag = getString(metadata, "tag")
        }

        // Script section (file references)
        (root["script"] as? JsonObject)?.let { scripts ->
            val scriptMap = HashMap<String, String>()
            for ((key, value) in scripts) {
                scriptMap[key] = value.jsonPrimitive.content
            }
            entity.scripts = scriptMap
        }

        // Config section (vBook extension-api) — key → spec cho UI + JS injection
        (root["config"] as? JsonObject)?.let { config ->
            for ((key, specJson) in config) {
                val spec = specJson as? JsonObject ?: continue
                entity.config[key] =
                    dev.haipham22.leechtext.plugin.vbook.model.VBookConfigSpec(
                        title = getString(spec, "title"),
                        subtitle = getString(spec, "subtitle"),
                        default = getString(spec, "default"),
                        values = (spec["values"] as? JsonArray)?.map { it.jsonPrimitive.content } ?: emptyList(),
                        mode = getString(spec, "mode"),
                        format = getString(spec, "format"),
                    )
            }
        }

        entity
    } catch (e: SerializationException) {
        throw VBookPluginException("Failed to parse plugin.json", e)
    }

    /** Extract mọi file .js trong src/. */
    private fun extractScripts(zip: Path): MutableMap<String, String> {
        val scripts = HashMap<String, String>()
        val scriptPattern = Regex("^src/([^/]+)\\.js$")

        for (entry in ZipIo.entries(zip, log)) {
            // Skip directories
            if (entry.isDirectory) continue

            // Match src/*.js
            val matcher = scriptPattern.matchEntire(entry.name) ?: continue
            val scriptName = matcher.groupValues[1]
            val content = extractEntryAsString(zip, entry.name)
            if (content != null) {
                scripts[scriptName] = content
            }
        }

        return scripts
    }

    /** Extract icon.png → base64 data URI. */
    @OptIn(ExperimentalEncodingApi::class)
    private fun extractIconAsBase64(zip: Path): String? {
        return try {
            if (ZipIo.entries(zip, log).any { it.name == "icon.png" && !it.isDirectory }) {
                val bytes = ZipIo.readEntry(zip, "icon.png", log) ?: return null
                val base64 = Base64.encode(bytes)
                "data:image/png;base64,$base64"
            } else {
                null
            }
        } catch (e: Exception) {
            // Icon optional — log nhưng không fail
            log.add("Failed to extract icon (non-critical): ${e.message}")
            null
        }
    }

    /** Extract một entry từ ZIP thành string. */
    private fun extractEntryAsString(
        zip: Path,
        entryName: String,
    ): String? = ZipIo.readEntry(zip, entryName, log)?.decodeToString()

    /** Download file từ URL xuống local path với security validation. */
    @Suppress("ThrowsCount") // validation chain ở trust boundary — mỗi guard ném exception có message riêng cho UI
    private fun downloadFile(
        url: String,
        destPath: Path,
    ) {
        // Validate URL trước khi download
        try {
            NetworkSecurityValidator.validateUrl(url)
        } catch (e: SecurityValidationException) {
            throw VBookPluginException("URL validation failed: ${e.message}", e)
        }

        val result =
            try {
                httpExecute(HttpSpec(method = "GET", url = url), log)
            } catch (e: Exception) {
                throw VBookPluginException("Download failed: ${url.take(120)}", e)
            }

        // Validate response headers
        try {
            NetworkSecurityValidator.validateResponse(result.headers["Content-Type"], result.body.size.toLong())
        } catch (e: SecurityValidationException) {
            throw VBookPluginException("Response validation failed: ${e.message}", e)
        }

        // Download vào temp file trước, atomic move từ temp → dest
        val tempPath = destPath.parent?.resolve(destPath.name + ".tmp") ?: destPath
        try {
            tempPath.parent?.let { fs.createDirectories(it) }
            fs.write(tempPath) { write(result.body) }
            fs.atomicMove(tempPath, destPath)
        } catch (e: Exception) {
            // Cleanup temp file khi fail
            runCatching { if (fs.exists(tempPath)) fs.delete(tempPath) }
            throw e
        }
    }

    /** Filename từ URL. */
    private fun extractFilenameFromUrl(url: String): String = url.split("/").last()
}

// Helper JSON parsing

private fun getString(
    obj: JsonObject,
    name: String,
): String? = (obj[name] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content

private fun getInt(
    obj: JsonObject,
    name: String,
): Int? = (obj[name] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content?.toIntOrNull()
