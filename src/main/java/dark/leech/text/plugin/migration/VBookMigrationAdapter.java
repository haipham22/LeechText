package dark.leech.text.plugin.migration;

import java.util.ArrayList;
import java.util.List;

import dark.leech.text.enities.PluginEntity;
import dark.leech.text.plugin.PluginManager;
import dark.leech.text.plugin.api.PluginFormat;
import dark.leech.text.plugin.api.RepositorySource;
import dark.leech.text.plugin.vbook.VBookPluginFormat;
import dark.leech.text.plugin.vbook.VBookRepositorySource;

/**
 * Migration adapter for vBook integration. Maintains backward compatibility while using new system.
 */
public class VBookMigrationAdapter {

    private final PluginManager pluginManager;
    private final VBookRepositorySource vbookSource;
    private final VBookPluginFormat vbookFormat;

    public VBookMigrationAdapter() {
        this.pluginManager = PluginManager.getManager();
        this.vbookSource =
                new VBookRepositorySource(
                        "vbook-default",
                        "vBook Default Repository",
                        "https://raw.githubusercontent.com/Darkrai9x/vbook-extensions/master/plugin.json");
        this.vbookFormat = new VBookPluginFormat();
    }

    /** Get vBook repository source for registration with UnifiedRepositoryManager. */
    public RepositorySource<?> getRepositorySource() {
        return vbookSource;
    }

    /** Get vBook plugin format for registration. */
    public PluginFormat getPluginFormat() {
        return vbookFormat;
    }

    /** Check if plugin is from vBook source. */
    public boolean isVBookPlugin(PluginEntity plugin) {
        if (plugin == null) {
            return false;
        }
        return PluginEntity.SCRIPT_ENGINE_JAVASCRIPT.equals(plugin.getScriptEngine())
                || (plugin.getSource() != null && plugin.getSource().contains("vbook"));
    }

    /**
     * Validate existing vBook plugins.
     *
     * @return List of validation results for each vBook plugin
     */
    public List<String> validateExistingPlugins() {
        List<String> results = new ArrayList<>();
        List<PluginEntity> existingPlugins = pluginManager.list();

        for (PluginEntity plugin : existingPlugins) {
            if (isVBookPlugin(plugin)) {
                try {
                    var validation = vbookFormat.validate(plugin);
                    if (!validation.isValid()) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Plugin '").append(plugin.getName()).append("' has errors:\n");
                        for (String error : validation.getErrors()) {
                            sb.append("  - ").append(error).append("\n");
                        }
                        results.add(sb.toString());
                    } else if (validation.hasWarnings()) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Plugin '").append(plugin.getName()).append("' has warnings:\n");
                        for (String warning : validation.getWarnings()) {
                            sb.append("  - ").append(warning).append("\n");
                        }
                        results.add(sb.toString());
                    } else {
                        results.add("Plugin '" + plugin.getName() + "' is valid.");
                    }
                } catch (Exception e) {
                    results.add(
                            "Failed to validate plugin '"
                                    + plugin.getName()
                                    + "': "
                                    + e.getMessage());
                }
            }
        }

        return results;
    }

    /**
     * Get count of vBook plugins in system.
     *
     * @return Number of vBook plugins
     */
    public int getVBookPluginCount() {
        int count = 0;
        List<PluginEntity> existingPlugins = pluginManager.list();
        for (PluginEntity plugin : existingPlugins) {
            if (isVBookPlugin(plugin)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Enable or disable vBook repository source.
     *
     * @param enabled true to enable
     */
    public void setVBookSourceEnabled(boolean enabled) {
        vbookSource.setEnabled(enabled);
    }

    /**
     * Check if vBook source is enabled.
     *
     * @return true if enabled
     */
    public boolean isVBookSourceEnabled() {
        return vbookSource.isEnabled();
    }
}
