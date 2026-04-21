package dark.leech.text.plugin.js.api;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import dark.leech.text.action.Log;
import dark.leech.text.util.SettingUtils;

/**
 * LocalStorage API for JavaScript plugins. Provides persistent key-value storage compatible with
 * browser localStorage API.
 *
 * <p>Usage example:
 * <pre>{@code
 * localStorage.setItem('key', 'value');
 * var value = localStorage.getItem('key');
 * localStorage.removeItem('key');
 * localStorage.clear();
 * }</pre>
 */
public final class LocalStorage extends JsApiWrapper {

    private static final String STORAGE_DIR = "plugin_storage";
    private static final int MAX_VALUE_SIZE = 10 * 1024; // 10KB per value
    private static final int MAX_TOTAL_SIZE = 1024 * 1024; // 1MB total storage

    private final String pluginId;
    private final File storageDir;

    /**
     * Create LocalStorage instance for a plugin.
     *
     * @param context The Rhino context
     * @param scope The Rhino scope
     * @param pluginId Unique identifier for the plugin
     */
    public LocalStorage(Context context, Scriptable scope, String pluginId) {
        super(context, scope);
        this.pluginId = pluginId != null ? pluginId : "default";

        // Create storage directory for this plugin in user data directory
        File userDataDir = new File(System.getProperty("user.home"), ".leechtext/plugin_storage");
        this.storageDir = new File(userDataDir, this.pluginId);

        if (!this.storageDir.exists()) {
            this.storageDir.mkdirs();
        }
    }

    /**
     * Set an item in storage. Creates or updates key-value pair.
     *
     * @param key The key to set
     * @param value The value to store
     * @return This LocalStorage instance for chaining
     */
    public LocalStorage setItem(String key, String value) {
        if (key == null || key.isEmpty()) {
            Log.add("[LocalStorage] Key is null or empty");
            return this;
        }

        if (value == null) {
            Log.add("[LocalStorage] Value is null, removing key: " + key);
            return removeItem(key);
        }

        try {
            // Validate key format (only alphanumeric, underscore, dash)
            if (!isValidKey(key)) {
                Log.add("[LocalStorage] Invalid key format: " + key);
                return this;
            }

            // Check value size
            byte[] valueBytes = value.getBytes("UTF-8");
            if (valueBytes.length > MAX_VALUE_SIZE) {
                Log.add("[LocalStorage] Value too large: " + valueBytes.length + " bytes (max: " + MAX_VALUE_SIZE + ")");
                return this;
            }

            // Check total storage size
            long totalSize = getCurrentStorageSize();
            if (totalSize + valueBytes.length > MAX_TOTAL_SIZE) {
                Log.add("[LocalStorage] Storage quota exceeded: " + totalSize + " + " + valueBytes.length + " > " + MAX_TOTAL_SIZE);
                return this;
            }

            // Store value
            File file = new File(storageDir, key + ".txt");
            Files.write(file.toPath(), valueBytes);

            Log.add("[LocalStorage] Set item: " + key + " (" + valueBytes.length + " bytes)");

        } catch (Exception e) {
            Log.add("[LocalStorage] Failed to set item: " + e.getMessage());
        }

        return this;
    }

    /**
     * Get an item from storage.
     *
     * @param key The key to retrieve
     * @return The value or null if not found
     */
    public String getItem(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }

        if (!isValidKey(key)) {
            Log.add("[LocalStorage] Invalid key format: " + key);
            return null;
        }

        try {
            File file = new File(storageDir, key + ".txt");
            if (!file.exists()) {
                return null;
            }

            String value = new String(Files.readAllBytes(file.toPath()), "UTF-8");
            Log.add("[LocalStorage] Get item: " + key);
            return value;

        } catch (Exception e) {
            Log.add("[LocalStorage] Failed to get item: " + e.getMessage());
            return null;
        }
    }

    /**
     * Remove an item from storage.
     *
     * @param key The key to remove
     * @return This LocalStorage instance for chaining
     */
    public LocalStorage removeItem(String key) {
        if (key == null || key.isEmpty()) {
            return this;
        }

        if (!isValidKey(key)) {
            Log.add("[LocalStorage] Invalid key format: " + key);
            return this;
        }

        try {
            File file = new File(storageDir, key + ".txt");
            if (file.exists()) {
                file.delete();
                Log.add("[LocalStorage] Removed item: " + key);
            }

        } catch (Exception e) {
            Log.add("[LocalStorage] Failed to remove item: " + e.getMessage());
        }

        return this;
    }

    /**
     * Clear all items from storage.
     */
    public void clear() {
        try {
            File[] files = storageDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        file.delete();
                    }
                }
            }
            Log.add("[LocalStorage] Cleared all items");

        } catch (Exception e) {
            Log.add("[LocalStorage] Failed to clear: " + e.getMessage());
        }
    }

    /**
     * Get the number of items in storage.
     *
     * @return Number of items
     */
    public int getLength() {
        File[] files = storageDir.listFiles();
        return files != null ? files.length : 0;
    }

    /**
     * Get the key at the specified index.
     *
     * @param index The index
     * @return The key name or null if index out of bounds
     */
    public String getKey(int index) {
        File[] files = storageDir.listFiles();
        if (files == null || index < 0 || index >= files.length) {
            return null;
        }

        File file = files[index];
        if (file.isFile()) {
            String name = file.getName();
            // Remove .txt extension
            return name.substring(0, name.length() - 4);
        }

        return null;
    }

    /**
     * Check if storage contains a key.
     *
     * @param key The key to check
     * @return true if key exists, false otherwise
     */
    public boolean hasOwnProperty(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }

        File file = new File(storageDir, key + ".txt");
        return file.exists();
    }

    /**
     * Get all keys in storage as an array.
     *
     * @return Array of keys
     */
    public String[] getKeys() {
        File[] files = storageDir.listFiles();
        if (files == null) {
            return new String[0];
        }

        String[] keys = new String[files.length];
        int count = 0;

        for (File file : files) {
            if (file.isFile()) {
                String name = file.getName();
                keys[count++] = name.substring(0, name.length() - 4);
            }
        }

        return keys;
    }

    /**
     * Validate key format. Only allows alphanumeric, underscore, and dash characters.
     *
     * @param key The key to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidKey(String key) {
        return key != null && key.matches("^[a-zA-Z0-9_-]+$");
    }

    /**
     * Calculate current storage size in bytes.
     *
     * @return Total size in bytes
     */
    private long getCurrentStorageSize() {
        long totalSize = 0;
        File[] files = storageDir.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    totalSize += file.length();
                }
            }
        }

        return totalSize;
    }
}
