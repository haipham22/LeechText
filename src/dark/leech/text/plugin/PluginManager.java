package dark.leech.text.plugin;

import com.google.gson.Gson;
import dark.leech.text.enities.PluginEntity;
import dark.leech.text.util.AppUtils;
import dark.leech.text.util.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Long on 1/11/2017.
 */
public class PluginManager {
    private static PluginManager manager;
    private static Set<PluginEntity> pluginList;

    private PluginManager() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                pluginList = new HashSet<>();
                File[] files = new File(FileUtils.validate(AppUtils.curDir + "/tools/plugins")).listFiles();
                if (files == null) return;
                for (File f : files) {
                    if (f.getName().endsWith(".plugin"))
                        try {
                            pluginList.add(createPlugin(f.getAbsolutePath()));
                        } catch (Exception e) {
                        }
                }
                PluginUpdate.getUpdate().checkUpdate();
            }
        }).start();

    }


    public static PluginManager getManager() {
        if (manager == null) manager = new PluginManager();
        return manager;
    }

    public void add(String path) {
        pluginList.add(createPlugin(path));
    }


    private PluginEntity createPlugin(String path) {
        PluginEntity entity = new Gson().fromJson(FileUtils.file2string(path), PluginEntity.class);
        return entity;
    }

    public PluginEntity get(String url) {
        for (PluginEntity plugin : pluginList)
            if (url.matches("(https?://)?" + plugin.getRegex())) return plugin;
        return null;
    }

    public Set<PluginEntity> list() {
        return pluginList;
    }

}


