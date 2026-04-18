package dark.leech.text.plugin.vbook;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import dark.leech.text.action.Log;
import dark.leech.text.enities.PluginEntity;
import dark.leech.text.plugin.PluginManager;
import dark.leech.text.plugin.util.PluginPersistence;
import dark.leech.text.plugin.vbook.exception.VBookPluginException;
import dark.leech.text.plugin.vbook.exception.VBookRegistryException;
import dark.leech.text.plugin.vbook.model.VBookExtensionEntity;
import dark.leech.text.plugin.vbook.model.VBookPluginEntity;
import dark.leech.text.util.FileUtils;

/**
 * Service for orchestrating vBook plugin download, conversion, and installation. Provides
 * high-level operations for working with vBook extensions. Thread-safe with atomic installation
 * support.
 */
public class VBookPluginService {

    private static final String PLUGINS_DIR = "tools/plugins";
    private static final String VBOOK_CACHE_DIR = "tools/plugins/vbook-cache";

    private final VBookRepositoryClient repositoryClient;
    private final PluginZipExtractor extractor;
    private final VBookToLeechTextConverter converter;
    private final PluginManager pluginManager;

    // Lock for atomic installation operations
    private static final Lock installationLock = new ReentrantLock();

    // Track in-progress installations to prevent duplicates
    private static final Set<String> installingPlugins = new HashSet<>();

    public VBookPluginService() {
        this.repositoryClient = new VBookRepositoryClient();
        this.extractor = new PluginZipExtractor();
        this.converter = new VBookToLeechTextConverter();
        this.pluginManager = PluginManager.getManager();
    }

    /**
     * Get list of available plugins from a repository.
     *
     * @param repositoryUrl URL to plugin.json registry
     * @return List of available extensions
     * @throws VBookRegistryException if fetch fails
     */
    public List<VBookExtensionEntity> getAvailablePlugins(String repositoryUrl)
            throws VBookRegistryException {
        return repositoryClient.fetchExtensions(repositoryUrl);
    }

    /**
     * Get list of available plugins from default vBook repository.
     *
     * @return List of available extensions
     * @throws VBookRegistryException if fetch fails
     */
    public List<VBookExtensionEntity> getAvailablePlugins() throws VBookRegistryException {
        return getAvailablePlugins(VBookRepositoryClient.getDefaultRegistryUrl());
    }

    /**
     * Download and convert a vBook plugin from URL.
     *
     * @param pluginZipUrl URL to plugin.zip file
     * @return Converted PluginEntity
     * @throws VBookPluginException if download or conversion fails
     */
    public PluginEntity downloadAndConvert(String pluginZipUrl) throws VBookPluginException {
        try {
            // Extract from URL
            VBookPluginEntity vbookPlugin =
                    extractor.extractFromZip(new java.net.URL(pluginZipUrl));

            // Convert to LeechText format
            return converter.convert(vbookPlugin);

        } catch (Exception e) {
            throw new VBookPluginException("Failed to download plugin from: " + pluginZipUrl, e);
        }
    }

    /**
     * Install a vBook extension from repository with atomic operation.
     *
     * @param extension The extension to install
     * @return true if installation succeeded
     * @throws VBookPluginException if installation fails
     */
    public boolean installPlugin(VBookExtensionEntity extension) throws VBookPluginException {
        String pluginKey = extension.getName() + ":" + extension.getVersion();

        installationLock.lock();
        try {
            // Check if already installing (prevent duplicate installations)
            if (installingPlugins.contains(pluginKey)) {
                Log.add("Plugin installation already in progress: " + extension.getName());
                return false;
            }

            installingPlugins.add(pluginKey);

            try {
                // Download and convert
                PluginEntity pluginEntity = downloadAndConvert(extension.getPath());

                // Validate plugin before installation
                if (!PluginPersistence.validate(pluginEntity)) {
                    throw new VBookPluginException(
                            "Plugin validation failed: " + extension.getName());
                }

                // Check for name conflicts
                if (hasNameConflict(pluginEntity.getName())) {
                    pluginEntity.setName(pluginEntity.getName() + " (vBook)");
                }

                // Atomic save to plugins directory (uses temp file + atomic move)
                PluginPersistence.saveAtomic(pluginEntity);

                // Add to PluginManager
                pluginManager.add(pluginEntity);

                Log.add("Successfully installed plugin: " + pluginEntity.getName());
                return true;

            } finally {
                installingPlugins.remove(pluginKey);
            }

        } catch (Exception e) {
            Log.add("Failed to install plugin: " + extension.getName() + " - " + e.getMessage());
            throw new VBookPluginException("Failed to install plugin: " + extension.getName(), e);
        } finally {
            installationLock.unlock();
        }
    }

    /**
     * Find and install plugin by matching URL against available extensions. Errors are logged for
     * observability (not silent).
     *
     * @param targetUrl URL to match against plugin regex patterns
     * @return Installed PluginEntity, or null if no match found
     */
    public PluginEntity findAndInstallByUrl(String targetUrl) {
        if (targetUrl == null || targetUrl.isEmpty()) {
            Log.add("Auto-discovery: empty URL provided");
            return null;
        }

        try {
            // Get available plugins from default repository
            List<VBookExtensionEntity> extensions = getAvailablePlugins();

            // Find matching extension
            for (VBookExtensionEntity extension : extensions) {
                try {
                    // Download plugin to get its regex
                    VBookPluginEntity vbookPlugin =
                            extractor.extractFromZip(new java.net.URL(extension.getPath()));

                    // Check if URL matches plugin's regex
                    if (vbookPlugin.getRegexp() != null
                            && targetUrl.matches(vbookPlugin.getRegexp())) {
                        // Convert and install
                        PluginEntity pluginEntity = converter.convert(vbookPlugin);

                        if (hasNameConflict(pluginEntity.getName())) {
                            pluginEntity.setName(pluginEntity.getName() + " (vBook)");
                        }

                        // Use atomic installation
                        Path pluginPath = PluginPersistence.saveAtomic(pluginEntity);
                        pluginManager.add(pluginEntity);

                        Log.add(
                                "Auto-discovery: installed plugin "
                                        + pluginEntity.getName()
                                        + " for URL: "
                                        + targetUrl);
                        return pluginEntity;
                    }
                } catch (Exception e) {
                    // Log error but continue trying other plugins
                    Log.add(
                            "Auto-discovery: failed to process plugin "
                                    + extension.getName()
                                    + " - "
                                    + e.getMessage());
                }
            }

            Log.add("Auto-discovery: no matching plugin found for URL: " + targetUrl);
            return null;

        } catch (Exception e) {
            // Log error with context
            Log.add(
                    "Auto-discovery: error processing repository for URL: "
                            + targetUrl
                            + " - "
                            + e.getMessage());
            return null;
        }
    }

    /** Check if a plugin with the given name already exists. */
    private boolean hasNameConflict(String name) {
        if (name == null) return false;

        for (PluginEntity existing : pluginManager.list()) {
            if (name.equals(existing.getName())) {
                return true;
            }
        }
        return false;
    }

    /** Clear cached plugin files. */
    public void clearCache() {
        try {
            Path cacheDir = Paths.get(FileUtils.validate(VBOOK_CACHE_DIR));
            if (Files.exists(cacheDir)) {
                Files.walk(cacheDir)
                        .filter(p -> !p.equals(cacheDir))
                        .forEach(
                                p -> {
                                    try {
                                        Files.deleteIfExists(p);
                                    } catch (IOException e) {
                                        // Ignore
                                    }
                                });
            }
        } catch (IOException e) {
            // Ignore
        }
    }
}
