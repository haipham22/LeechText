package dark.leech.text.repository;

import com.google.gson.Gson;
import dark.leech.text.action.Log;
import dark.leech.text.enities.RepositoryEntity;
import dark.leech.text.util.AppUtils;
import dark.leech.text.util.FileUtils;
import dark.leech.text.util.Http;
import dark.leech.text.util.SyntaxUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class RepositoryManager {
    private static final String URL = "https://raw.githubusercontent.com/Darkrai9x/vbook-extensions/master/repository.json";

    private static RepositoryManager manager;
    private static Set<RepositoryEntity> repositoryList;

    private RepositoryManager() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                repositoryList = new HashSet<>();

            }
        }).start();
    }

    public static RepositoryManager getManager() {
        if (manager == null) manager = new RepositoryManager();
        return manager;
    }

    public void doLoad() {
        doDefault();
        String repositorySettingJson = FileUtils.file2string(AppUtils.curDir + "/tools/repository.json");

        if (repositorySettingJson != null) {
            JSONArray jsonArray = new JSONArray(repositorySettingJson);

            Set<RepositoryEntity> repositoryEntities = RepositoryManager.getManager().getRepositoryList();

            for (int i = 0; i < jsonArray.length(); i++) {
                RepositoryEntity entity = new Gson().fromJson(jsonArray.getJSONObject(i).toString(), RepositoryEntity.class);
                if (repositoryEntities.contains(entity)) {
                    repositoryEntities.add(entity);
                }
                repositoryEntities.add(entity);
            }
        }
    }

    private static void doDefault() {
        try {
            String js = Http.request(URL).string();
            JSONArray jsonArray = new JSONArray(js);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject object = jsonArray.getJSONObject(i);


                String author = object.getString("author");

                RepositoryEntity entity = new Gson().fromJson(object.toString(), RepositoryEntity.class);

                if (!repositoryList.contains(entity)) {
                    entity.setChecked(true);
                    repositoryList.add(entity);
                }
            }
        } catch (Exception err) {
            Log.add(err);
        }
    }

    public Set<RepositoryEntity> getRepositoryList() {
        return repositoryList;
    }
}
