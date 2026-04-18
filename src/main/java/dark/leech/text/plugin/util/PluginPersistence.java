package dark.leech.text.plugin.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import dark.leech.text.action.Log;
import dark.leech.text.enities.PluginEntity;

/**
 * Utility class for plugin validation and persistence. Provides atomic file operations and
 * validation logic.
 */
public final class PluginPersistence {

    private static final String PLUGINS_DIR = "tools/plugins";

    private PluginPersistence() {
        // Utility class - prevent instantiation
    }

    /**
     * Validates a plugin before installation.
     *
     * @param plugin Plugin to validate
     * @return true if valid
     */
    public static boolean validate(PluginEntity plugin) {
        if (plugin == null) {
            return false;
        }

        // Check required fields
        if (plugin.getName() == null || plugin.getName().isEmpty()) {
            Log.add("Plugin validation failed: missing name");
            return false;
        }

        if (plugin.getRegex() == null || plugin.getRegex().isEmpty()) {
            Log.add("Plugin validation failed: missing regex pattern");
            return false;
        }

        // Validate regex compiles
        try {
            java.util.regex.Pattern.compile(plugin.getRegex());
        } catch (Exception e) {
            Log.add("Plugin validation failed: invalid regex pattern - " + e.getMessage());
            return false;
        }

        // Check for at least one script getter
        boolean hasScript =
                (plugin.getChapGetter() != null && !plugin.getChapGetter().isEmpty())
                        || (plugin.getTocGetter() != null && !plugin.getTocGetter().isEmpty());

        if (!hasScript) {
            Log.add("Plugin validation failed: no script content");
            return false;
        }

        return true;
    }

    /**
     * Save plugin entity to .plugin file atomically. Uses temp file + atomic move to prevent
     * partial writes.
     *
     * @param plugin Plugin to save
     * @return Path to saved plugin file
     * @throws IOException if save fails
     */
    public static Path saveAtomic(PluginEntity plugin) throws IOException {
        Path pluginDirPath = Paths.get(PLUGINS_DIR);
        Files.createDirectories(pluginDirPath);

        // Use UUID for consistent filename (matches delete logic)
        String filename = plugin.getUuid() + ".plugin";
        Path filePath = pluginDirPath.resolve(filename);
        Path tempPath = pluginDirPath.resolve(filename + ".tmp");

        // Convert to JSON
        com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();

        String json = gson.toJson(plugin);

        // Write to temp file first
        Files.writeString(tempPath, json);

        // Atomic move to final destination
        try {
            Files.move(
                    tempPath,
                    filePath,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            // Clean up temp file on failure
            try {
                Files.deleteIfExists(tempPath);
            } catch (IOException ignored) {
                // Ignore cleanup failures
            }
            throw e;
        }

        return filePath;
    }

    /**
     * Get the plugins directory path.
     *
     * @return Path to plugins directory
     */
    public static Path getPluginsDirectory() {
        return Paths.get(PLUGINS_DIR);
    }
}
