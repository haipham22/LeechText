package dark.leech.text.plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.*;

import com.google.gson.Gson;

import dark.leech.text.action.Log;
import dark.leech.text.enities.PluginEntity;
import dark.leech.text.util.AppUtils;
import dark.leech.text.util.FileUtils;

/**
 * Manages plugin loading, installation, and retrieval. Extended to support vBook plugin format.
 * Thread-safe using CopyOnWriteArrayList for plugin list.
 */
public class PluginManager {
    private static final Gson gson = new Gson();
    private static final Object lock = new Object();

    private static PluginManager manager;
    private static List<PluginEntity> pluginList;
    private static boolean initialized = false;

    /** Listener interface for plugin list changes. */
    public interface PluginListListener {
        void onPluginsChanged();
    }

    private static final List<PluginListListener> listeners = new ArrayList<>();

    /** Register a listener to be notified when plugins are added or removed. */
    public static void addListener(PluginListListener listener) {
        synchronized (lock) {
            listeners.add(listener);
        }
    }

    /** Unregister a listener. */
    public static void removeListener(PluginListListener listener) {
        synchronized (lock) {
            listeners.remove(listener);
        }
    }

    /** Notify all listeners that the plugin list has changed. */
    private static void notifyListeners() {
        SwingUtilities.invokeLater(
                () -> {
                    synchronized (lock) {
                        for (PluginListListener listener : listeners) {
                            try {
                                listener.onPluginsChanged();
                            } catch (Exception e) {
                                Log.add(e);
                            }
                        }
                    }
                });
    }

    /**
     * Public method to trigger listener notification. Call this after updating plugins to refresh
     * the UI.
     */
    public static void notifyPluginsChanged() {
        notifyListeners();
    }

    private PluginManager() {
        synchronized (lock) {
            pluginList = new CopyOnWriteArrayList<>();
        }
        new Thread(
                        () -> {
                            synchronized (lock) {
                                File[] files =
                                        new File(
                                                        FileUtils.validate(
                                                                AppUtils.curDir + "/tools/plugins"))
                                                .listFiles();
                                if (files == null) {
                                    initialized = true;
                                    return;
                                }
                                for (File f : files) {
                                    if (f.getName().endsWith(".plugin"))
                                        try {
                                            pluginList.add(createPlugin(f.getAbsolutePath()));
                                        } catch (Exception e) {
                                            Log.add(e);
                                        }
                                }
                                initialized = true;
                                lock.notifyAll();
                            }
                            PluginUpdate.getUpdate().checkUpdate();
                        })
                .start();
    }

    public static PluginManager getManager() {
        if (manager == null) {
            synchronized (lock) {
                if (manager == null) {
                    manager = new PluginManager();
                }
            }
        }
        return manager;
    }

    public void add(String path) {
        PluginEntity plugin = createPlugin(path);
        add(plugin);
    }

    /**
     * Add a plugin entity directly to the manager. Thread-safe operation using
     * CopyOnWriteArrayList.
     *
     * @param plugin The plugin entity to add
     */
    public void add(PluginEntity plugin) {
        if (plugin != null && plugin.getRegex() != null) {
            // Check for duplicates and update if exists
            synchronized (lock) {
                for (PluginEntity existing : pluginList) {
                    if (existing.getRegex().equals(plugin.getRegex())) {
                        // Update existing plugin
                        existing.apply(plugin);
                        return;
                    }
                }
                // Add new plugin
                pluginList.add(plugin);
                notifyListeners();
            }
        }
    }

    private PluginEntity createPlugin(String path) {
        return gson.fromJson(FileUtils.file2string(path), PluginEntity.class);
    }

    /**
     * Get plugin that matches the given URL. Only returns existing locally installed plugins.
     *
     * @param url URL to match against plugin regex patterns
     * @return Matching plugin, or null if not found
     */
    public PluginEntity get(String url) {
        // Check existing plugins only
        for (PluginEntity plugin : pluginList)
            if (url.matches("(https?://)?" + plugin.getRegex())) return plugin;

        return null;
    }

    public List<PluginEntity> list() {
        return pluginList;
    }

    /**
     * Remove a plugin from the manager. Thread-safe operation using CopyOnWriteArrayList.
     *
     * @param plugin The plugin entity to remove
     * @return true if plugin was found and removed
     */
    public boolean remove(PluginEntity plugin) {
        if (plugin == null) {
            return false;
        }

        synchronized (lock) {
            boolean removed = pluginList.remove(plugin);
            if (removed) {
                // Delete plugin file
                String fileName = plugin.getUuid() + ".plugin";
                String filePath = AppUtils.curDir + "/tools/plugins/" + fileName;
                File pluginFile = new File(FileUtils.validate(filePath));
                if (pluginFile.exists()) {
                    pluginFile.delete();
                }

                // Also delete zip file if exists
                String zipFileName = plugin.getUuid() + ".zip";
                String zipFilePath = AppUtils.curDir + "/tools/plugins/" + zipFileName;
                File zipFile = new File(FileUtils.validate(zipFilePath));
                if (zipFile.exists()) {
                    zipFile.delete();
                }

                notifyListeners();
            }
            return removed;
        }
    }

    /**
     * Check if plugin manager has finished loading plugins from disk.
     *
     * @return true if initialization complete
     */
    public static boolean isInitialized() {
        synchronized (lock) {
            return initialized;
        }
    }

    /**
     * Wait for initialization to complete (max 5 seconds). Use this before calling list() to ensure
     * plugins are loaded.
     */
    public static void waitForInitialization() {
        long timeoutMs = 5000; // 5 seconds
        long deadline = System.currentTimeMillis() + timeoutMs;

        synchronized (lock) {
            while (!initialized) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) {
                    Log.add("PluginManager initialization timeout");
                    return;
                }
                try {
                    lock.wait(remaining);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
}
