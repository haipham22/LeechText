package dark.leech.text.plugin;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.collections4.CollectionUtils;

import com.google.gson.Gson;

import dark.leech.text.action.Log;
import dark.leech.text.enities.PluginEntity;
import dark.leech.text.enities.RepositoryEntity;
import dark.leech.text.models.Plugin;
import dark.leech.text.models.Repository;
import dark.leech.text.ui.notification.Toast;
import dark.leech.text.util.AppUtils;
import dark.leech.text.util.FileUtils;
import dark.leech.text.util.Http;
import dark.leech.text.util.SettingUtils;
import dark.leech.text.util.ZipUtils;

public class PluginUpdate {

    private static final Gson gson = new Gson();

    private static final String PLUGINS_DIR = AppUtils.curDir + "/tools/plugins";
    private static final String TEMP_DIR = AppUtils.curDir + "/tools/temp";

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
        try {
            var js = Http.request(repositoryLink).string();

            Repository repository = gson.fromJson(js, Repository.class);

            assert repository != null;
            if (CollectionUtils.isEmpty(repository.getPlugins())) return;

            loadPlugins(repository.getPlugins());

        } catch (Exception e) {
            Log.add(e);
        }
    }

    private void loadPlugins(List<Repository.Plugin> plugins) {
        for (var plugin : plugins) {
            var localPlugin = PluginManager.getManager().getByUUID(plugin.getUuid());

            if (localPlugin == null) {
                if (installPlugin(plugin)) {
                    Toast.Build()
                            .content("Đã tải xuống plugin " + plugin.getName())
                            .time(3000)
                            .open();
                }
                continue;
            }

            if (needsToUpdate(localPlugin.getVersion(), plugin.getVersion())) {}
        }
    }

    private boolean needsToUpdate(double currentVersion, double newVersion) {
        return currentVersion < newVersion;
    }

    public boolean installPlugin(Repository.Plugin plugin) {
        try {
            String downloadPath = TEMP_DIR + "/" + plugin.getName() + ".zip";

            // Download plugin
            FileUtils.url2file(plugin.getPath(), downloadPath);

            // Load and install the plugin
            PluginEntity entity = parserPlugin(plugin, downloadPath);
            if (entity != null) {
                // Save plugin entity in the format expected by PluginManager
                String entityPath = PLUGINS_DIR + "/" + plugin.getName() + ".plugin";
                FileUtils.string2file(gson.toJson(entity), entityPath);

                // Cleanup download
                FileUtils.deleteFile(downloadPath);

                return true;
            }

        } catch (Exception e) {
            Log.add("Error installing plugin: " + e.getMessage());
        }

        return false;
    }

    private PluginEntity parserPlugin(Repository.Plugin plugin, String zipPath) {
        try {
            // Create a temp directory if it doesn't exist
            FileUtils.mkdir(TEMP_DIR);

            // Extract plugin to the temp directory
            String extractPath = TEMP_DIR + "/" + plugin.getName();

            ZipUtils.extract(zipPath, extractPath);

            // Read plugin.json
            String pluginJsonPath = extractPath + "/plugin.json";
            if (!Files.exists(Paths.get(pluginJsonPath))) {
                Log.add("Plugin JSON not found: " + pluginJsonPath);
                return null;
            }

            String pluginJson = FileUtils.file2string(pluginJsonPath);
            var repoPlugin = gson.fromJson(pluginJson, Plugin.class);

            String chapGetter =
                    FileUtils.file2string(extractPath + "/src/" + repoPlugin.getScript().getChap());
            String tocGetter =
                    FileUtils.file2string(extractPath + "/src/" + repoPlugin.getScript().getToc());
            String detailGetter =
                    FileUtils.file2string(
                            extractPath + "/src/" + repoPlugin.getScript().getDetail());

            PluginEntity entity =
                    PluginEntity.builder()
                            .uuid(plugin.getUuid())
                            .name(repoPlugin.getMetadata().getName())
                            .version(repoPlugin.getMetadata().getVersion())
                            .author(repoPlugin.getMetadata().getAuthor())
                            .source(plugin.getSource())
                            .describe(repoPlugin.getMetadata().getDescription())
                            .language("js") // Set language to JavaScript for these plugins
                            .group(repoPlugin.getMetadata().getType())
                            .regex(repoPlugin.getMetadata().getRegexp())
                            .icon(plugin.getIcon())
                            .checked(true)
                            .chapGetter(chapGetter)
                            .tocGetter(tocGetter)
                            .detailGetter(detailGetter)
                            .url(repoPlugin.getMetadata().getSource())
                            .supportUpdate(plugin.getVersion() > 0)
                            .data(repoPlugin.getMetadata().toString())
                            .build();

            // // Copy plugin to plugins directory
            // String finalPath = PLUGINS_DIR + "/" + plugin.getName() + ".plugin";
            // ZipUtils.extract(zipPath, finalPath.replace(".plugin", ""));

            // Clean up temp files
            FileUtils.deleteDirectory(extractPath);

            return entity;

        } catch (Exception e) {
            Log.add("Error loading plugin from " + zipPath + ": " + e.getMessage());
            return null;
        }
    }
}
