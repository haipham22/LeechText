package dark.leech.text.plugin;

import com.google.gson.Gson;
import dark.leech.text.enities.PluginEntity;
import dark.leech.text.enities.RepositoryEntity;
import dark.leech.text.util.Http;
import dark.leech.text.util.SyntaxUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class RepositoryManager {
    private static String URL = "https://raw.githubusercontent.com/Darkrai9x/vbook-extensions/master/repository.json";

    private static RepositoryManager manager;

    public List<RepositoryEntity> getRepositories() {
        return repositories;
    }

    private List<RepositoryEntity> repositories;
    private Gson gson = new Gson();


    private RepositoryManager() {
        repositories = new ArrayList<>();
        String js = Http.request(URL).string();
        JSONArray objArr = new JSONArray(js);
        for (int i = 0; i < objArr.length(); i++) {
            JSONObject obj = objArr.getJSONObject(i);
            RepositoryEntity entity = gson.fromJson(obj.toString(), RepositoryEntity.class);
            String repositoryStr = Http.request(entity.getPath()).string();
            try {
                JSONObject repository = new JSONObject(repositoryStr);
                List<PluginEntity> plugins = getListPlugin(repository.getJSONArray("data"), SyntaxUtils.xoaDau(entity.getAuthor().toLowerCase()));
                entity.setPlugins(plugins);
                repositories.add(entity);
            } catch (Exception ignore) {}
        }
    }

    private List<PluginEntity> getListPlugin(JSONArray jsonPlugins, String source) {
        List<PluginEntity> pluginEntities = new ArrayList<>();
        for (int i = 0; i < jsonPlugins.length(); i++) {
            PluginEntity entity = gson.fromJson(jsonPlugins.getJSONObject(i).toString(), PluginEntity.class);
            String pathName = SyntaxUtils.xoaDau(entity.getName()).toLowerCase() + "_" + source;
            entity.setPathName(pathName);
            pluginEntities.add(entity);
        }
        return pluginEntities;
    }

    public static RepositoryManager getInstance() {
        if (manager == null) {
            manager = new RepositoryManager();
        }
        return manager;
    }

}
