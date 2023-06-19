package dark.leech.text.plugin;

import com.google.gson.Gson;
import dark.leech.text.action.Log;
import dark.leech.text.enities.PluginEntity;
import dark.leech.text.ui.notification.Toast;
import dark.leech.text.util.AppUtils;
import dark.leech.text.util.FileUtils;
import dark.leech.text.util.Http;
import org.json.JSONObject;

import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by Dark on 2/24/2017.
 */
public class PluginUpdate {
    private static final String URL = "https://raw.githubusercontent.com/Darkrai9x/vbook-extensions/master/repository.json";
    private static PluginUpdate pluginUpdate;
    private Gson gson = new Gson();
    private static int size = 5;
    private static int numberOfThread = 5;

    private PluginUpdate() {

    }

    public static PluginUpdate getUpdate() {
        if (pluginUpdate == null) pluginUpdate = new PluginUpdate();
        return pluginUpdate;
    }


    public void checkUpdate() {
        try {

            Map<String, PluginEntity> pluginEntityMap = RepositoryManager.getInstance().getRepositories()
                    .stream().flatMap(i -> i.getPlugins().stream())
                    .collect(Collectors.toMap(PluginEntity::getPathName, Function.identity()));
            updateExistPlugin(pluginEntityMap);
            createPlugin(pluginEntityMap);

        } catch (Exception e) {
            Log.add(e);
        }

    }

    private void updateExistPlugin(Map<String, PluginEntity> pluginEntityMap) {
        for (PluginEntity pluginGetter : PluginManager.getManager().list()) {
            if (!pluginEntityMap.containsKey(pluginGetter.getPathName())) continue;
            PluginEntity plugin = pluginEntityMap.remove(pluginGetter.getPathName());
            if (plugin.getVersion() > pluginGetter.getVersion()) {
                String path = AppUtils.curDir
                        + "/tools/plugins/"
                        + plugin.getPathName()
                        + ".plugin";

                PluginEntity entity = createPluginFromTemp(plugin);
                entity.setChecked(true);
                entity.setPathName(pluginGetter.getPathName());
                FileUtils.string2file(new Gson().toJson(entity), path);
                plugin.setChecked(true);
                pluginGetter.apply(plugin);
                Toast.Build()
                        .content("Đã update plugin " + pluginGetter.getName() + " v" + pluginGetter.getVersion())
                        .time(3000)
                        .open();
            }
        }
    }

    private void createPlugin(Map<String, PluginEntity> pluginEntityMap) {
        if (pluginEntityMap.isEmpty()) return;
        Set<Map.Entry<String, PluginEntity>> entries = pluginEntityMap.entrySet();
        ExecutorService executorService = Executors.newFixedThreadPool(entries.size());

        for (Map.Entry<String, PluginEntity> entry : pluginEntityMap.entrySet()) {
            String pluginName = entry.getKey();
            PluginEntity plugin = entry.getValue();
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    String path = AppUtils.curDir
                            + "/tools/plugins/"
                            + pluginName
                            + ".plugin";

                    PluginEntity entity = createPluginFromTemp(plugin);
                    entity.setPathName(pluginName);
                    entity.setChecked(true);
                    FileUtils.string2file(new Gson().toJson(entity), path);
                    plugin.apply(entity);

                    PluginManager.getManager().add(path);
                }
            });
        }
        Toast.Build()
                .content("Đã tải xuống tất cả " + entries.size() + " plugin")
                .time(3000)
                .open();
    }

    public PluginEntity createPluginFromTemp(PluginEntity pluginTmp) {
        PluginEntity entity = null;
        File tempZipFile = null;
        try {
            byte[] bytes = Http.request(pluginTmp.getPath()).bytes();
            if (bytes == null) return null;
            String fileName = createFilePath(pluginTmp.getPathName());
            FileUtils.byte2file(bytes, fileName);
            tempZipFile = new File(fileName);
            ZipFile zipFile = new ZipFile(tempZipFile);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement();
                String content = FileUtils.input2string(zipFile.getInputStream(zipEntry));
                if (zipEntry.getName().endsWith("json")) {
                    JSONObject jsonObject = new JSONObject(content);
                    Map<String, Object> mapEntity = new HashMap<>();
                    mapEntity.putAll(jsonObject.getJSONObject("metadata").toMap());
                    mapEntity.putAll(jsonObject.getJSONObject("script").toMap());
                    entity = gson.fromJson(gson.toJson(mapEntity), PluginEntity.class);
                }
                if (entity.getChapGetter() != null && zipEntry.getName().contains(entity.getChapGetter())) {
                    entity.setChapGetter(content);
                }
                if (entity.getTocGetter() != null && zipEntry.getName().contains(entity.getTocGetter())) {
                    entity.setTocGetter(content);
                }
                if (entity.getPageGetter() != null && zipEntry.getName().contains(entity.getPageGetter())) {
                    entity.setPageGetter(content);
                }
                if (entity.getSearchGetter() != null && zipEntry.getName().contains(entity.getSearchGetter())) {
                    entity.setSearchGetter(content);
                }
                if (entity.getDetailGetter() != null && zipEntry.getName().contains(entity.getDetailGetter())) {
                    entity.setDetailGetter(content);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (tempZipFile != null && tempZipFile.exists()) tempZipFile.delete();

        }
        return entity;
    }

    private void setContent(ZipEntry zipEntry, String path, PluginEntity entity, String content) {
        if (zipEntry.getName().contains(path)) {
            entity.setChapGetter(content);
        }
    }

    private String createFilePath(String path) {
        return AppUtils.curDir + "/tools/" + path + ".zip";
    }

}
