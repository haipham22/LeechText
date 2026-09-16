package dev.haipham22.leechtext.plugin.vbook

import dev.haipham22.leechtext.log.EngineLogger
import dev.haipham22.leechtext.plugin.js.api.Http
import dev.haipham22.leechtext.plugin.security.CacheCapacityException
import dev.haipham22.leechtext.plugin.vbook.exception.VBookRegistryException
import dev.haipham22.leechtext.plugin.vbook.model.VBookExtensionEntity
import dev.haipham22.leechtext.plugin.vbook.model.VBookRepositoryEntity
import dev.haipham22.leechtext.util.LeechJson
import dev.haipham22.leechtext.util.monitorLock
import dev.haipham22.leechtext.util.nowMillis
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

/**
 * Client fetch + parse vBook plugin registry (port từ vbook/VBookRepositoryClient.java).
 * Registry JSON parsing + local cache memory-bounded.
 */
class VBookRepositoryClient(
    private val log: EngineLogger,
) {
    companion object {
        private const val DEFAULT_REGISTRY =
            "https://raw.githubusercontent.com/Darkrai9x/vbook-extensions/master/repository.json"

        private const val CACHE_TTL_MS = 30 * 60 * 1000L // 30 minutes

        // Memory-bounded cache
        private const val MAX_CACHE_MEMORY_BYTES = 50L * 1024 * 1024 // 50MB
        private const val MAX_CACHE_SIZE = 50

        // ponytail: synchronized thay ConcurrentHashMap — registry fetch là low-frequency
        private val REGISTRY_CACHE = HashMap<String, CacheEntry>()
        private var currentCacheMemoryBytes = 0L

        /**
         * Cache JSON thô per URL — checkUpdate (PluginUpdate) và fetchExtensions
         * (getAvailablePlugins) cùng đụng plugin.json mỗi repo lúc mở app; không
         * cache chung là double request toàn bộ repo (thấy trong log 2026-08-26).
         */
        private val rawJsonCache = HashMap<String, Pair<String, Long>>()
        private const val RAW_TTL_MS = 10 * 60 * 1000L

        /** Fetch repo JSON qua cache dùng chung mọi caller. */
        fun fetchRepoJson(
            url: String,
            log: EngineLogger,
        ): String = monitorLock(rawJsonCache) {
            val now = nowMillis()
            rawJsonCache[url]?.let { (json, ts) -> if (now - ts < RAW_TTL_MS) return@monitorLock json }
            val json = Http(log).request(url).string()
            if (json.isNotEmpty()) rawJsonCache[url] = json to now
            json
        }

        /** Clear registry cache. */
        fun clearCache() {
            monitorLock(REGISTRY_CACHE) {
                REGISTRY_CACHE.clear()
                currentCacheMemoryBytes = 0
            }
        }

        fun getDefaultRegistryUrl(): String = DEFAULT_REGISTRY
    }

    /** Cache entry có size tracking (SoftReference JVM-only bỏ — entry bounded theo
     * MAX_CACHE_SIZE + TTL, GC pressure không phải vấn đề với 50 entries). */
    private class CacheEntry(
        val data: VBookRepositoryData,
    ) {
        val timestamp: Long = nowMillis()
        val memoryBytes: Long = estimateSize(data)

        fun isExpired(): Boolean = nowMillis() - timestamp > CACHE_TTL_MS

        fun resolve(): VBookRepositoryData = data

        companion object {
            /** Ước lượng memory — base overhead + size per extension. */
            fun estimateSize(data: VBookRepositoryData): Long {
                var size = 128L // base object overhead
                size += data.extensions.size * 256L
                return size
            }
        }
    }

    /** Parsed repository — metadata + extensions. */
    data class VBookRepositoryData(
        val author: String,
        val description: String,
        val extensions: List<VBookExtensionEntity> = ArrayList(),
    )

    /**
     * Fetch + parse vBook repository từ URL.
     *
     * @throws VBookRegistryException nếu fetch hoặc parse fail
     */
    @Throws(VBookRegistryException::class)
    @Suppress("ThrowsCount") // validation chain ở trust boundary — mỗi guard ném exception có message riêng cho UI
    fun fetchRepository(url: String): VBookRepositoryData {
        // Check cache trước
        val cachedResult = monitorLock(REGISTRY_CACHE) {
            REGISTRY_CACHE[url]?.takeIf { !it.isExpired() }?.resolve()
        }
        cachedResult?.let { return it }

        return try {
            val json = fetchRepoJson(url, log)
            if (json.isEmpty()) {
                throw VBookRegistryException("Failed to fetch registry from: $url")
            }

            val repository = parseRepository(json)

            // Try cache — có thể fail khi cache full
            tryCacheRepository(url, repository)

            repository
        } catch (e: CacheCapacityException) {
            // Cache full — log nhưng vẫn trả data
            log.add("Cache memory limit reached, returning uncached data for: $url")
            try {
                parseRepository(fetchRepoJson(url, log))
            } catch (retry: Exception) {
                throw VBookRegistryException("Failed to fetch repository from: $url", retry)
            }
        } catch (e: VBookRegistryException) {
            throw e
        } catch (e: Exception) {
            throw VBookRegistryException("Failed to fetch repository from: $url", e)
        }
    }

    /** Cache repository data — throw khi vượt limit. */
    @Throws(CacheCapacityException::class)
    private fun tryCacheRepository(
        url: String,
        repository: VBookRepositoryData,
    ) {
        // Check capacity (memory + entry count)
        checkCacheCapacity(repository)

        monitorLock(REGISTRY_CACHE) {
            // Remove entry cũ nếu update
            REGISTRY_CACHE[url]?.let { oldEntry ->
                currentCacheMemoryBytes -= oldEntry.memoryBytes
            }

            // Add entry mới
            val newEntry = CacheEntry(repository)
            REGISTRY_CACHE[url] = newEntry
            currentCacheMemoryBytes += newEntry.memoryBytes
        }
    }

    /** Check cache còn chỗ cho data mới không. */
    @Throws(CacheCapacityException::class)
    private fun checkCacheCapacity(data: VBookRepositoryData) {
        val estimatedSize =
            128L + data.extensions.size * 256L

        monitorLock(REGISTRY_CACHE) {
            // Memory limit
            if (currentCacheMemoryBytes + estimatedSize > MAX_CACHE_MEMORY_BYTES) {
                throw CacheCapacityException("Cache memory limit exceeded")
            }

            // Entry count limit
            if (REGISTRY_CACHE.size >= MAX_CACHE_SIZE) {
                throw CacheCapacityException("Cache entry count limit exceeded")
            }
        }
    }

    /**
     * Fetch extensions từ URL — handle cả plugin.json trực tiếp lẫn repository.json (array
     * sources).
     *
     * @throws VBookRegistryException nếu fetch hoặc parse fail
     */
    @Throws(VBookRegistryException::class)
    fun fetchExtensions(url: String): List<VBookExtensionEntity> {
        // Try parse repository.json trước (array of repository sources)
        try {
            val repositories = fetchRepositorySources(url)
            if (repositories.isNotEmpty()) {
                return fetchAllExtensionsFromRepositories(repositories)
            }
        } catch (_: Exception) {
            // Không phải repository.json → fall through parse plugin.json trực tiếp
        }

        // Fallback: direct plugin.json parsing
        return fetchRepository(url).extensions
    }

    /**
     * Fetch repository sources từ repository.json URL.
     *
     * @throws VBookRegistryException nếu fetch hoặc parse fail
     */
    @Throws(VBookRegistryException::class)
    fun fetchRepositorySources(url: String): List<VBookRepositoryEntity> = try {
        val json = fetchRepoJson(url, log)
        if (json.isEmpty()) {
            throw VBookRegistryException("Failed to fetch repository sources from: $url")
        }

        // Parse as array of repository sources — Gson trả null cho input "null"/empty,
        // kotlinx throw nên bọc runCatching giữ semantics (empty = không phải repo format)
        runCatching { LeechJson.decodeFromString<List<VBookRepositoryEntity>>(json) }.getOrNull() ?: ArrayList()
    } catch (e: VBookRegistryException) {
        throw e
    } catch (e: Exception) {
        throw VBookRegistryException("Failed to parse repository sources from: $url", e)
    }

    /** Fetch extensions từ nhiều repository sources — aggregate plugins từ mọi repo. */
    fun fetchAllExtensionsFromRepositories(repositories: List<VBookRepositoryEntity>): List<VBookExtensionEntity> {
        val allExtensions = ArrayList<VBookExtensionEntity>()

        for (repo in repositories) {
            if (!repo.enabled) continue

            try {
                val extensions = fetchExtensions(repo.link ?: continue)
                allExtensions.addAll(extensions)
            } catch (e: Exception) {
                log.add("Failed to fetch from repository: ${repo.link} - ${e.message}")
                // Continue với repo khác
            }
        }

        return allExtensions
    }

    /** Parse vBook plugin.json format. */
    @Throws(VBookRegistryException::class)
    private fun parseRepository(json: String): VBookRepositoryData = try {
        val root = Json.parseToJsonElement(json).jsonObject

        // Metadata
        val metadata = root["metadata"] as? JsonObject
        val author =
            (metadata?.get("author") as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content ?: "Unknown"
        val description =
            (metadata?.get("description") as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content ?: ""

        // Data array (extensions) — absent → rỗng (không throw như Gson behavior)
        val dataArray = root["data"] as? JsonArray
        val extensions: List<VBookExtensionEntity> =
            dataArray?.let { LeechJson.decodeFromJsonElement<List<VBookExtensionEntity>>(it) } ?: ArrayList()

        VBookRepositoryData(author, description, extensions)
    } catch (e: Exception) {
        throw VBookRegistryException("Failed to parse registry JSON", e)
    }
}
