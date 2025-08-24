package dark.leech.text.plugin;

import java.io.File;
import java.util.ArrayList;

import com.google.gson.Gson;

import dark.leech.text.action.Log;
import dark.leech.text.enities.PluginEntity;
import dark.leech.text.util.AppUtils;
import dark.leech.text.util.FileUtils;

public class PluginManager {
    private static final Gson gson = new Gson();

    private static PluginManager manager;
    private static ArrayList<PluginEntity> pluginList;

    private PluginManager() {
        new Thread(
                        () -> {
                            pluginList = new ArrayList<>();
                            File[] files =
                                    new File(FileUtils.validate(AppUtils.curDir + "/tools/plugins"))
                                            .listFiles();
                            if (files == null) return;
                            for (File f : files) {
                                if (f.getName().endsWith(".plugin"))
                                    try {
                                        pluginList.add(createPlugin(f.getAbsolutePath()));
                                    } catch (Exception e) {
                                        Log.add(e);
                                    }
                            }
                            PluginUpdate.getUpdate().checkUpdate();
                        })
                .start();
    }

    public static PluginManager getManager() {
        if (manager == null) manager = new PluginManager();
        return manager;
    }

    public void add(String path) {
        pluginList.add(createPlugin(path));
    }

    private PluginEntity createPlugin(String path) {
        return gson.fromJson(FileUtils.file2string(path), PluginEntity.class);
    }

    public PluginEntity get(String url) {
        for (PluginEntity plugin : pluginList)
            if (url.matches("(https?://)?" + plugin.getRegex())) return plugin;
        return null;
    }

    public ArrayList<PluginEntity> list() {
        return pluginList;
    }
}
