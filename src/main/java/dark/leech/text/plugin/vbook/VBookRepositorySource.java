package dark.leech.text.plugin.vbook;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import dark.leech.text.action.Log;
import dark.leech.text.enities.PluginEntity;
import dark.leech.text.plugin.PluginManager;
import dark.leech.text.plugin.api.*;
import dark.leech.text.plugin.vbook.exception.VBookPluginException;
import dark.leech.text.plugin.vbook.exception.VBookRegistryException;
import dark.leech.text.plugin.vbook.model.VBookExtensionEntity;
import dark.leech.text.plugin.vbook.model.VBookPluginEntity;
import dark.leech.text.plugin.vbook.model.VBookPluginMetadata;

/** vBook repository source implementation. Handles vBook-specific repository operations. */
public class VBookRepositorySource implements RepositorySource<VBookPluginMetadata> {

    private static final String DEFAULT_REGISTRY =
            "https://raw.githubusercontent.com/Darkrai9x/vbook-extensions/master/repository.json";

    private final String id;
    private final String name;
    private final String registryUrl;
    private final VBookRepositoryClient client;
    private final VBookToLeechTextConverter converter;
    private boolean enabled;
    private RepositoryHealth healthCache;
    private long healthCheckTime;

    public VBookRepositorySource(String id, String name, String registryUrl) {
        this.id = id;
        this.name = name;
        this.registryUrl = registryUrl;
        this.client = new VBookRepositoryClient();
        this.converter = new VBookToLeechTextConverter();
        this.enabled = true;
        this.healthCache = RepositoryHealth.unknown();
        this.healthCheckTime = 0;
    }

    public VBookRepositorySource() {
        this("vbook-default", "vBook Default Repository", DEFAULT_REGISTRY);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getType() {
        return "vbook";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public PaginatedResult<VBookPluginMetadata> search(String query, int page, int pageSize)
            throws RepositoryException {

        try {
            // Fetch all extensions (vBook doesn't support server-side search)
            List<VBookExtensionEntity> allExtensions = client.fetchExtensions(registryUrl);

            // Filter locally
            List<VBookExtensionEntity> filtered = new ArrayList<>();
            if (query != null && !query.isEmpty()) {
                String lowerQuery = query.toLowerCase();
                for (VBookExtensionEntity ext : allExtensions) {
                    if ((ext.getName() != null && ext.getName().toLowerCase().contains(lowerQuery))
                            || (ext.getAuthor() != null
                                    && ext.getAuthor().toLowerCase().contains(lowerQuery))
                            || (ext.getDescription() != null
                                    && ext.getDescription().toLowerCase().contains(lowerQuery))) {
                        filtered.add(ext);
                    }
                }
            } else {
                filtered = allExtensions;
            }

            // Paginate
            int start = page * pageSize;
            int end = Math.min(start + pageSize, filtered.size());

            if (start >= filtered.size()) {
                return PaginatedResult.empty();
            }

            List<VBookExtensionEntity> pageItems = filtered.subList(start, end);

            // Convert to metadata
            List<VBookPluginMetadata> metadata = new ArrayList<>();
            for (VBookExtensionEntity ext : pageItems) {
                metadata.add(convertToMetadata(ext));
            }

            return PaginatedResult.of(metadata, filtered.size(), page, pageSize);

        } catch (VBookRegistryException e) {
            healthCache = RepositoryHealth.unhealthy(e.getMessage());
            throw new RepositoryException(
                    "Failed to search vBook repository: " + e.getMessage(),
                    RepositoryException.ErrorType.NETWORK_ERROR,
                    e);
        }
    }

    @Override
    public PaginatedResult<VBookPluginMetadata> listAll(int page, int pageSize)
            throws RepositoryException {
        return search("", page, pageSize); // Empty query returns all
    }

    @Override
    public PluginEntity download(String pluginId) throws RepositoryException {
        try {
            // Fetch all extensions
            List<VBookExtensionEntity> extensions = client.fetchExtensions(registryUrl);

            // Find target extension by name
            VBookExtensionEntity target = null;
            for (VBookExtensionEntity ext : extensions) {
                if (ext.getName().equals(pluginId) || ext.getName().equalsIgnoreCase(pluginId)) {
                    target = ext;
                    break;
                }
            }

            if (target == null) {
                throw new RepositoryException(
                        "Plugin not found: " + pluginId, RepositoryException.ErrorType.NOT_FOUND);
            }

            // Download and convert using extractor
            PluginZipExtractor extractor = new PluginZipExtractor();
            VBookPluginEntity vbookPlugin =
                    extractor.extractFromZip(new java.net.URL(target.getPath()));

            return converter.convert(vbookPlugin);

        } catch (VBookPluginException e) {
            throw new RepositoryException(
                    "Failed to download plugin: " + e.getMessage(),
                    RepositoryException.ErrorType.PARSE_ERROR,
                    e);
        } catch (java.io.IOException e) {
            throw new RepositoryException(
                    "Failed to download plugin: " + e.getMessage(),
                    RepositoryException.ErrorType.NETWORK_ERROR,
                    e);
        }
    }

    @Override
    public boolean canHandle(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }

        try {
            // vBook registry doesn't contain regex - need to download plugin to check
            // This is inefficient but necessary given vBook architecture
            List<VBookExtensionEntity> extensions = client.fetchExtensions(registryUrl);
            PluginZipExtractor extractor = new PluginZipExtractor();

            for (VBookExtensionEntity ext : extensions) {
                try {
                    // Download plugin to get its regex
                    VBookPluginEntity plugin =
                            extractor.extractFromZip(new java.net.URL(ext.getPath()));

                    if (plugin.getRegexp() != null && !plugin.getRegexp().isEmpty()) {
                        try {
                            if (url.matches(plugin.getRegexp())) {
                                return true;
                            }
                        } catch (Exception e) {
                            // Invalid regex, skip this plugin
                        }
                    }
                } catch (Exception e) {
                    // Failed to download/parse this plugin, skip it
                }
            }
        } catch (Exception e) {
            // If we can't check, assume we can't handle it
        }
        return false;
    }

    @Override
    public CompletableFuture<PluginEntity> findAndInstall(String url) {
        return CompletableFuture.supplyAsync(
                () -> {
                    if (!isEnabled()) {
                        return null;
                    }

                    try {
                        // Use VBookPluginService for find and install
                        VBookPluginService service = new VBookPluginService();

                        // The service handles downloading, matching, and installing
                        List<VBookExtensionEntity> extensions = client.fetchExtensions(registryUrl);
                        PluginZipExtractor extractor = new PluginZipExtractor();

                        for (VBookExtensionEntity ext : extensions) {
                            try {
                                // Download plugin to get its regex
                                VBookPluginEntity plugin =
                                        extractor.extractFromZip(new java.net.URL(ext.getPath()));

                                if (plugin.getRegexp() != null && url.matches(plugin.getRegexp())) {
                                    // Convert and install
                                    PluginEntity pluginEntity = converter.convert(plugin);

                                    // Add to plugin manager
                                    PluginManager.getManager().add(pluginEntity);
                                    Log.add(
                                            "Auto-discovery: installed vBook plugin "
                                                    + pluginEntity.getName()
                                                    + " for URL: "
                                                    + url);
                                    return pluginEntity;
                                }
                            } catch (Exception e) {
                                Log.add(
                                        "Auto-discovery: failed to match plugin "
                                                + ext.getName()
                                                + ": "
                                                + e.getMessage());
                            }
                        }
                        return null;

                    } catch (Exception e) {
                        Log.add("Auto-discovery failed: " + e.getMessage());
                        throw new RuntimeException(
                                "Auto-discovery failed: " + e.getMessage(),
                                new RepositoryException(
                                        "Auto-discovery failed: " + e.getMessage(),
                                        RepositoryException.ErrorType.UNKNOWN,
                                        e));
                    }
                });
    }

    @Override
    public RepositoryHealth getHealth() {
        try {
            client.fetchExtensions(registryUrl);
            healthCache = RepositoryHealth.healthy();
        } catch (Exception e) {
            healthCache = RepositoryHealth.unhealthy(e.getMessage());
        }
        return healthCache;
    }

    @Override
    public RepositoryMetadata getMetadata() {
        return RepositoryMetadata.builder()
                .name(name)
                .description("vBook community plugins repository")
                .version("1.0")
                .homepageUrl("https://github.com/Darkrai9x/vbook-extensions")
                .author("Darkrai9x")
                .pluginCount(0) // Unknown until fetched
                .build();
    }

    private VBookPluginMetadata convertToMetadata(VBookExtensionEntity ext) {
        return VBookPluginMetadata.vbookBuilder()
                .id(ext.getName())
                .name(ext.getName())
                .author(ext.getAuthor())
                .version(String.valueOf(ext.getVersion()))
                .description(ext.getDescription())
                .source(registryUrl)
                .iconUrl(ext.getIcon())
                .popularity(0) // vBook doesn't provide this
                .tags(new String[] {"vbook"})
                .lastUpdated(0) // vBook doesn't provide this
                .sizeBytes(0) // Unknown until download
                .type("vbook")
                .group("") // Not available in registry
                .language("") // Not available in registry
                .build();
    }
}
