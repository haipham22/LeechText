package dark.leech.text.plugin.vbook;

import java.lang.ref.SoftReference;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import dark.leech.text.action.Log;
import dark.leech.text.plugin.security.CacheCapacityException;
import dark.leech.text.plugin.vbook.exception.VBookRegistryException;
import dark.leech.text.plugin.vbook.model.VBookExtensionEntity;
import dark.leech.text.plugin.vbook.model.VBookRepositoryEntity;
import dark.leech.text.util.Http;

/**
 * Client for fetching and parsing vBook plugin registries. Handles registry JSON parsing and local
 * caching with memory-bounded eviction.
 */
public class VBookRepositoryClient {

    private static final String DEFAULT_REGISTRY =
            "https://raw.githubusercontent.com/Darkrai9x/vbook-extensions/master/repository.json";

    private static final Gson GSON = new Gson();
    private static final long CACHE_TTL_MS = 30 * 60 * 1000; // 30 minutes

    // Memory-bounded cache settings
    private static final long MAX_CACHE_MEMORY_BYTES = 50 * 1024 * 1024; // 50MB
    private static final int MAX_CACHE_SIZE = 50; // Maximum number of entries

    // Memory-aware cache
    private static final ConcurrentHashMap<String, CacheEntry> REGISTRY_CACHE =
            new ConcurrentHashMap<>();
    private static long currentCacheMemoryBytes = 0;

    /** Memory-aware cache entry with size tracking. */
    private static class CacheEntry {
        final VBookRepositoryData data;
        final long timestamp;
        final long memoryBytes;
        SoftReference<VBookRepositoryData> softReference; // For GC pressure

        CacheEntry(VBookRepositoryData data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
            this.memoryBytes = estimateSize(data);
            this.softReference = new SoftReference<>(data);
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }

        /** Estimate memory size of cached data. */
        private long estimateSize(VBookRepositoryData data) {
            // Rough estimation: base overhead + data size
            long size = 128; // Base object overhead
            if (data.getExtensions() != null) {
                for (VBookExtensionEntity ext : data.getExtensions()) {
                    size += 256; // Approximate size per extension
                }
            }
            return size;
        }

        VBookRepositoryData getData() {
            // Try soft reference first (may be cleared under memory pressure)
            VBookRepositoryData refData = softReference.get();
            if (refData != null) {
                return refData;
            }
            return data;
        }
    }

    /**
     * Fetch and parse a vBook repository from URL.
     *
     * @param url URL to plugin.json registry
     * @return VBookRepositoryEntity with metadata and extensions
     * @throws VBookRegistryException if fetch or parsing fails
     */
    public VBookRepositoryData fetchRepository(String url) throws VBookRegistryException {
        // Check cache first
        CacheEntry cached = REGISTRY_CACHE.get(url);
        if (cached != null && !cached.isExpired()) {
            return cached.getData();
        }

        // Fetch and parse with cache attempt
        try {
            String json = Http.request(url).string();
            if (json == null || json.isEmpty()) {
                throw new VBookRegistryException("Failed to fetch registry from: " + url);
            }

            VBookRepositoryData repository = parseRepository(json);

            // Try to cache - may fail if cache is full
            tryCacheRepository(url, repository);

            return repository;

        } catch (CacheCapacityException e) {
            // Cache full - log but still return data
            Log.add("Cache memory limit reached, returning uncached data for: " + url);
            try {
                return parseRepository(Http.request(url).string());
            } catch (Exception retry) {
                throw new VBookRegistryException("Failed to fetch repository from: " + url, retry);
            }
        } catch (Exception e) {
            throw new VBookRegistryException("Failed to fetch repository from: " + url, e);
        }
    }

    /**
     * Attempt to cache repository data.
     *
     * @param url Repository URL
     * @param repository Data to cache
     * @throws CacheCapacityException if cache is full
     */
    private void tryCacheRepository(String url, VBookRepositoryData repository)
            throws CacheCapacityException {
        // Check if we can add to cache (memory + size limits)
        checkCacheCapacity(repository);

        // Remove old entry if updating
        CacheEntry oldEntry = REGISTRY_CACHE.get(url);
        if (oldEntry != null) {
            currentCacheMemoryBytes -= oldEntry.memoryBytes;
        }

        // Add new entry
        CacheEntry newEntry = new CacheEntry(repository);
        REGISTRY_CACHE.put(url, newEntry);
        currentCacheMemoryBytes += newEntry.memoryBytes;
    }

    /**
     * Check if cache can accommodate new data.
     *
     * @param data Data to be cached
     * @throws CacheCapacityException if cache is full
     */
    private void checkCacheCapacity(VBookRepositoryData data) throws CacheCapacityException {
        long estimatedSize =
                128 + (data.getExtensions() != null ? data.getExtensions().size() * 256L : 0);

        // Check memory limit
        if (currentCacheMemoryBytes + estimatedSize > MAX_CACHE_MEMORY_BYTES) {
            throw new CacheCapacityException("Cache memory limit exceeded");
        }

        // Check entry count limit
        if (REGISTRY_CACHE.size() >= MAX_CACHE_SIZE) {
            throw new CacheCapacityException("Cache entry count limit exceeded");
        }
    }

    /**
     * Evict entries to free up cache capacity. Prioritizes: 1) expired entries, 2) oldest entries.
     */
    private void evictForCapacity() {
        // First pass: remove expired entries
        REGISTRY_CACHE
                .entrySet()
                .removeIf(
                        entry -> {
                            if (entry.getValue().isExpired()) {
                                currentCacheMemoryBytes -= entry.getValue().memoryBytes;
                                return true;
                            }
                            return false;
                        });

        // Second pass: remove oldest entries until under limits
        while (REGISTRY_CACHE.size() >= MAX_CACHE_SIZE
                || currentCacheMemoryBytes > MAX_CACHE_MEMORY_BYTES * 0.8) {

            String oldestKey = null;
            long oldestTimestamp = Long.MAX_VALUE;

            for (var entry : REGISTRY_CACHE.entrySet()) {
                if (entry.getValue().timestamp < oldestTimestamp) {
                    oldestTimestamp = entry.getValue().timestamp;
                    oldestKey = entry.getKey();
                }
            }

            if (oldestKey != null) {
                CacheEntry removed = REGISTRY_CACHE.remove(oldestKey);
                if (removed != null) {
                    currentCacheMemoryBytes -= removed.memoryBytes;
                }
            } else {
                break;
            }
        }
    }

    /**
     * Evict the oldest cache entry.
     *
     * @deprecated Use evictForCapacity() instead
     */
    @Deprecated
    private void evictOldestEntry() {
        evictForCapacity();
    }

    /**
     * Fetch list of extensions from a repository URL. Handles both direct plugin.json and
     * repository.json (array of sources).
     *
     * @param url URL to plugin.json registry or repository.json
     * @return List of VBookExtensionEntity
     * @throws VBookRegistryException if fetch or parsing fails
     */
    public List<VBookExtensionEntity> fetchExtensions(String url) throws VBookRegistryException {
        // Try to parse as repository.json first (array of repository sources)
        try {
            List<VBookRepositoryEntity> repositories = fetchRepositorySources(url);
            if (!repositories.isEmpty()) {
                return fetchAllExtensionsFromRepositories(repositories);
            }
        } catch (Exception e) {
            // Not a repository.json, fall through to direct plugin.json parsing
        }

        // Fallback: direct plugin.json parsing
        VBookRepositoryData repository = fetchRepository(url);
        return repository.getExtensions();
    }

    /**
     * Fetch repository sources from repository.json URL. repository.json contains an array of
     * repository metadata.
     *
     * @param url URL to repository.json
     * @return List of repository sources
     * @throws VBookRegistryException if fetch or parsing fails
     */
    public List<VBookRepositoryEntity> fetchRepositorySources(String url)
            throws VBookRegistryException {
        try {
            String json = Http.request(url).string();
            if (json == null || json.isEmpty()) {
                throw new VBookRegistryException("Failed to fetch repository sources from: " + url);
            }

            // Parse as array of repository sources
            Type listType = new TypeToken<List<VBookRepositoryEntity>>() {}.getType();
            List<VBookRepositoryEntity> repositories = GSON.fromJson(json, listType);

            return repositories != null ? repositories : new ArrayList<>();

        } catch (Exception e) {
            throw new VBookRegistryException("Failed to parse repository sources from: " + url, e);
        }
    }

    /**
     * Fetch all extensions from multiple repository sources. Aggregates plugins from all
     * repositories.
     *
     * @param repositories List of repository sources
     * @return Combined list of all extensions
     */
    public List<VBookExtensionEntity> fetchAllExtensionsFromRepositories(
            List<VBookRepositoryEntity> repositories) {
        List<VBookExtensionEntity> allExtensions = new ArrayList<>();

        for (VBookRepositoryEntity repo : repositories) {
            if (!repo.isEnabled()) {
                continue;
            }

            try {
                List<VBookExtensionEntity> extensions = fetchExtensions(repo.getLink());
                allExtensions.addAll(extensions);
            } catch (Exception e) {
                Log.add(
                        "Failed to fetch from repository: "
                                + repo.getLink()
                                + " - "
                                + e.getMessage());
                // Continue with other repositories
            }
        }

        return allExtensions;
    }

    /**
     * Parse vBook plugin.json format.
     *
     * @param json Raw JSON string
     * @return VBookRepositoryData with parsed data
     * @throws VBookRegistryException if JSON is malformed
     */
    private VBookRepositoryData parseRepository(String json) throws VBookRegistryException {
        try {
            // Parse the root object
            com.google.gson.JsonObject root = GSON.fromJson(json, com.google.gson.JsonObject.class);

            // Extract metadata
            com.google.gson.JsonObject metadata = root.getAsJsonObject("metadata");
            String author = metadata != null ? metadata.get("author").getAsString() : "Unknown";
            String description = metadata != null ? metadata.get("description").getAsString() : "";

            // Extract data array (list of extensions)
            com.google.gson.JsonArray dataArray = root.getAsJsonArray("data");
            Type listType = new TypeToken<List<VBookExtensionEntity>>() {}.getType();
            List<VBookExtensionEntity> extensions = GSON.fromJson(dataArray, listType);

            return new VBookRepositoryData(author, description, extensions);

        } catch (Exception e) {
            throw new VBookRegistryException("Failed to parse registry JSON", e);
        }
    }

    /** Clear the registry cache. */
    public static void clearCache() {
        REGISTRY_CACHE.clear();
    }

    /**
     * Get the default vBook repository URL.
     *
     * @return Default registry URL
     */
    public static String getDefaultRegistryUrl() {
        return DEFAULT_REGISTRY;
    }

    /** Internal data class for parsed repository. */
    public static class VBookRepositoryData {
        private final String author;
        private final String description;
        private final List<VBookExtensionEntity> extensions;

        public VBookRepositoryData(
                String author, String description, List<VBookExtensionEntity> extensions) {
            this.author = author;
            this.description = description;
            this.extensions = extensions != null ? extensions : new ArrayList<>();
        }

        public String getAuthor() {
            return author;
        }

        public String getDescription() {
            return description;
        }

        public List<VBookExtensionEntity> getExtensions() {
            return extensions;
        }
    }
}
