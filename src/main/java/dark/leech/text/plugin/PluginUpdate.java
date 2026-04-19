package dark.leech.text.plugin;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.*;

import org.apache.commons.collections4.CollectionUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.gson.Gson;

import dark.leech.text.action.Log;
import dark.leech.text.enities.PluginEntity;
import dark.leech.text.enities.RepositoryEntity;
import dark.leech.text.models.Repository;
import dark.leech.text.ui.notification.Toast;
import dark.leech.text.util.AppUtils;
import dark.leech.text.util.FileUtils;
import dark.leech.text.util.Http;
import dark.leech.text.util.SettingUtils;

public class PluginUpdate {

    private static final Gson gson = new Gson();

    private static PluginUpdate pluginUpdate;

    private PluginUpdate() {}

    public static PluginUpdate getUpdate() {
        if (pluginUpdate == null) pluginUpdate = new PluginUpdate();
        return pluginUpdate;
    }

    public void checkUpdate() {
        var repos = RepositoryManager.getManager().repositoryList();
        if (repos == null || repos.isEmpty()) return;

        int poolSize = Math.max(1, SettingUtils.MAX_CONN);
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        for (RepositoryEntity repo : repos) {
            if (repo == null || !repo.isEnabled()) continue;
            executor.submit(
                    () -> {
                        try {
                            Log.add(
                                    "Checking update for repository: "
                                            + repo.getLink()
                                            + "  thread: "
                                            + Thread.currentThread().getName());
                            checkUpdate(repo.getLink());
                        } catch (Exception e) {
                            Log.add(e);
                        }
                    });
        }
        executor.shutdown();
    }

    public void checkUpdate(String repositoryLink) {
        Log.add("[PluginUpdate] Checking update from: " + repositoryLink);

        try {
            String js = Http.request(repositoryLink).string();
            if (js == null || js.isEmpty()) {
                Log.add("[PluginUpdate] Empty or null response from repository");
                return;
            }
            Log.add("[PluginUpdate] Repository response received, length: " + js.length());

            // Try vBook Repository format first
            Repository repository = gson.fromJson(js, Repository.class);
            if (repository != null && !CollectionUtils.isEmpty(repository.getPlugins())) {
                Log.add(
                        "[PluginUpdate] vBook Repository format detected, plugins: "
                                + repository.getPlugins().size());
                checkUpdateVBook(repository);
                return;
            }

            // Fall back to legacy array format
            Log.add("[PluginUpdate] Trying legacy array format...");
            JSONArray objArr = new JSONArray(js);
            Log.add("[PluginUpdate] Legacy format detected, plugins: " + objArr.length());
            checkUpdateLegacy(objArr);

        } catch (Exception e) {
            Log.add("[PluginUpdate] Error checking update: " + e.getMessage());
        }
    }

    /** Check updates for vBook format plugins. */
    private void checkUpdateVBook(Repository repository) {
        int updateCount = 0;
        int downloadCount = 0;

        for (Repository.Plugin pluginMeta : repository.getPlugins()) {

            try {
                var existingPlugin = PluginManager.getManager().get(pluginMeta.getPath());

                if (existingPlugin != null) {
                    // Update existing plugin
                    if (pluginMeta.getVersion() > existingPlugin.getVersion()) {
                        Log.add(
                                "[PluginUpdate] UPDATE: "
                                        + existingPlugin.getName()
                                        + " v"
                                        + existingPlugin.getVersion()
                                        + " -> v"
                                        + pluginMeta.getVersion());
                        updateCount++;
                    } else {
                        Log.add(
                                "[PluginUpdate] UP-TO-DATE: "
                                        + existingPlugin.getName()
                                        + " v"
                                        + existingPlugin.getVersion());
                    }
                }

            } catch (Exception e) {
                Log.add(
                        "[PluginUpdate] Error processing vBook plugin "
                                + pluginMeta.getName()
                                + ": "
                                + e.getMessage());
            }
        }

        Log.add(
                "[PluginUpdate] vBook update complete: "
                        + updateCount
                        + " updates, "
                        + downloadCount
                        + " new plugins");
    }

    /** Check updates for legacy format plugins. */
    private void checkUpdateLegacy(JSONArray plugins) {
        int updateCount = 0;
        int downloadCount = 0;

        for (int i = 0; i < plugins.length(); i++) {
            JSONObject obj = plugins.getJSONObject(i);
            String pluginUuid = obj.getString("uuid");
            String pluginName = obj.getString("name");
            double remoteVersion = obj.getDouble("version");

            try {
                boolean found = false;
                for (PluginEntity existingPlugin : PluginManager.getManager().list()) {
                    if (pluginUuid.equals(existingPlugin.getUuid())) {
                        found = true;
                        if (remoteVersion > existingPlugin.getVersion()) {
                            Log.add(
                                    "[PluginUpdate] UPDATE: "
                                            + existingPlugin.getName()
                                            + " v"
                                            + existingPlugin.getVersion()
                                            + " -> v"
                                            + remoteVersion);
                            updateLegacyPlugin(obj, existingPlugin);
                            updateCount++;
                        } else {
                            Log.add(
                                    "[PluginUpdate] UP-TO-DATE: "
                                            + existingPlugin.getName()
                                            + " v"
                                            + existingPlugin.getVersion());
                        }
                        break;
                    }
                }

                if (!found) {
                    Log.add("[PluginUpdate] NEW PLUGIN: " + pluginName + " v" + remoteVersion);
                    downloadLegacyPlugin(obj, pluginName, pluginUuid);
                    downloadCount++;
                }

            } catch (Exception e) {
                Log.add(
                        "[PluginUpdate] Error processing legacy plugin "
                                + pluginName
                                + ": "
                                + e.getMessage());
            }
        }

        Log.add(
                "[PluginUpdate] Legacy update complete: "
                        + updateCount
                        + " updates, "
                        + downloadCount
                        + " new plugins");
    }

    private void updateLegacyPlugin(JSONObject obj, PluginEntity existingPlugin) {
        try {
            String path =
                    AppUtils.curDir + "/tools/plugins/" + existingPlugin.getUuid() + ".plugin";
            String json = Http.request(obj.getString("url")).string();

            PluginEntity entity = gson.fromJson(json, PluginEntity.class);
            entity.setChecked(true);
            FileUtils.string2file(gson.toJson(entity), path);
            existingPlugin.apply(entity);
            PluginManager.notifyPluginsChanged();

            Log.add(
                    "[PluginUpdate] ✓ Updated: "
                            + existingPlugin.getName()
                            + " v"
                            + existingPlugin.getVersion());
            showToast(
                    "Đã update plugin "
                            + existingPlugin.getName()
                            + " v"
                            + existingPlugin.getVersion());

        } catch (Exception e) {
            Log.add(
                    "[PluginUpdate] ✗ Update failed for "
                            + existingPlugin.getName()
                            + ": "
                            + e.getMessage());
        }
    }

    private void downloadLegacyPlugin(JSONObject obj, String pluginName, String pluginUuid) {
        try {
            String path = AppUtils.curDir + "/tools/plugins/" + pluginUuid + ".plugin";
            Log.add("[PluginUpdate] Downloading: " + pluginName + " from " + obj.getString("url"));

            String json = Http.request(obj.getString("url")).string();
            PluginEntity entity = gson.fromJson(json, PluginEntity.class);
            entity.setChecked(true);
            FileUtils.string2file(gson.toJson(entity), path);
            PluginManager.getManager().add(path);

            Log.add("[PluginUpdate] ✓ Downloaded: " + pluginName);
            showToast("Đã tải xuống plugin " + pluginName);

        } catch (Exception e) {
            Log.add("[PluginUpdate] ✗ Download failed for " + pluginName + ": " + e.getMessage());
        }
    }

    private void showToast(String message) {
        SwingUtilities.invokeLater(() -> Toast.Build().content(message).time(3000).open());
    }
}
