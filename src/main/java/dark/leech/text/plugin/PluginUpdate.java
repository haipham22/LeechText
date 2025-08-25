package dark.leech.text.plugin;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        try {
            var js = Http.request(repositoryLink).string();

            Repository repository = gson.fromJson(js, Repository.class);

            if (CollectionUtils.isEmpty(repository.getPlugins())) return;

            for (Repository.Plugin plugin : repository.getPlugins()) {
                var pluginGetter = PluginManager.getManager().get(plugin.getPath());
                if (pluginGetter == null) continue;

                if (plugin.getVersion() > pluginGetter.getVersion()) {
                    var path = AppUtils.curDir + "/tools/plugins/" + plugin.getUuid() + ".zip";
                    FileUtils.string2file(gson.toJson(plugin), path);
                }
            }
            JSONArray objArr = new JSONArray(js);
            for (int i = 0; i < objArr.length(); i++) {
                JSONObject obj = objArr.getJSONObject(i);
                boolean have = false;
                for (PluginEntity pluginGetter : PluginManager.getManager().list()) {
                    if (obj.getString("uuid").equals(pluginGetter.getUuid())) {
                        have = true;
                        if (obj.getDouble("version") > pluginGetter.getVersion()) {
                            String path =
                                    AppUtils.curDir
                                            + "/tools/plugins/"
                                            + pluginGetter.getUuid()
                                            + ".plugin";
                            String json = Http.request(obj.getString("url")).string();

                            PluginEntity entity = gson.fromJson(json, PluginEntity.class);
                            entity.setChecked(true);
                            FileUtils.string2file(gson.toJson(entity), path);
                            pluginGetter.apply(entity);
                            Toast.Build()
                                    .content(
                                            "Đã update plugin "
                                                    + pluginGetter.getName()
                                                    + " v"
                                                    + pluginGetter.getVersion())
                                    .time(3000)
                                    .open();
                        }
                    }
                }
                if (!have) {
                    String path =
                            AppUtils.curDir + "/tools/plugins/" + obj.getString("uuid") + ".plugin";
                    String json = Http.request(obj.getString("url")).string();

                    PluginEntity entity = gson.fromJson(json, PluginEntity.class);
                    entity.setChecked(true);
                    FileUtils.string2file(gson.toJson(entity), path);
                    PluginManager.getManager().add(path);
                    Toast.Build()
                            .content("Đã tải xuống plugin " + obj.getString("name"))
                            .time(3000)
                            .open();
                }
            }
        } catch (Exception e) {
            Log.add(e);
        }
    }
}
