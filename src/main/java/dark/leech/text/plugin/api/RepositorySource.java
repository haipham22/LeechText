package dark.leech.text.plugin.api;

import java.util.concurrent.CompletableFuture;

import dark.leech.text.enities.PluginEntity;

/**
 * Abstraction for plugin repository sources. Supports multiple repository types (vBook, LeechText,
 * custom).
 *
 * @param <T> Metadata type for this repository
 */
public interface RepositorySource<T extends PluginMetadata> {

    /**
     * Unique identifier for this repository source. Format: "{type}-{id}" e.g., "vbook-default",
     * "leechtext-community"
     */
    String getId();

    /** Human-readable name for display in UI. */
    String getName();

    /** Repository type identifier. Examples: "vbook", "leechtext", "github", "custom" */
    String getType();

    /**
     * Check if this repository source is enabled. Disabled sources are not searched or downloaded
     * from.
     */
    boolean isEnabled();

    /** Enable or disable this repository source. */
    void setEnabled(boolean enabled);

    /**
     * Search for plugins matching the given query. Results are paginated to handle large
     * repositories.
     *
     * @param query Search query string
     * @param page Page number (0-indexed)
     * @param pageSize Number of results per page (recommended: 20-50)
     * @return Paginated search results
     * @throws RepositoryException if search fails
     */
    PaginatedResult<T> search(String query, int page, int pageSize) throws RepositoryException;

    /**
     * List all available plugins in this repository.
     *
     * @param page Page number (0-indexed)
     * @param pageSize Results per page
     * @return Paginated list of all plugins
     * @throws RepositoryException if listing fails
     */
    PaginatedResult<T> listAll(int page, int pageSize) throws RepositoryException;

    /**
     * Download a plugin by its identifier. Returns raw plugin entity that needs format conversion.
     *
     * @param pluginId Unique plugin identifier within this repository
     * @return Downloaded plugin entity
     * @throws RepositoryException if download fails
     */
    PluginEntity download(String pluginId) throws RepositoryException;

    /**
     * Check if this source can handle the given URL. Used for auto-discovery functionality.
     *
     * @param url URL to check
     * @return true if this source can provide a plugin for the URL
     */
    boolean canHandle(String url);

    /**
     * Find and install plugin for the given URL. Asynchronous operation - returns immediately with
     * Future.
     *
     * @param url URL to match against plugins
     * @return Future that completes with installed plugin, or null if no match
     */
    CompletableFuture<PluginEntity> findAndInstall(String url);

    /**
     * Get health status of this repository. Used for monitoring and circuit breaking.
     *
     * @return Health status
     */
    RepositoryHealth getHealth();

    /** Get repository metadata. Includes version, update check URL, etc. */
    RepositoryMetadata getMetadata();
}
