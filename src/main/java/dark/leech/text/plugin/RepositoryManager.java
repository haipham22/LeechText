package dark.leech.text.plugin;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import dark.leech.text.action.Log;
import dark.leech.text.enities.RepositoryEntity;
import dark.leech.text.util.AppUtils;
import dark.leech.text.util.FileUtils;

public class RepositoryManager {
    private static final Gson gson = new Gson();

    private static RepositoryManager manager;

    private static List<RepositoryEntity> repositoryList;

    private RepositoryManager() {

        repositoryList = new ArrayList<>();
        try {
            var json = FileUtils.file2string(AppUtils.curDir + "/tools/repository.json");
            var type = TypeToken.getParameterized(List.class, RepositoryEntity.class).getType();
            repositoryList = gson.fromJson(json, type);
        } catch (Exception e) {
            Log.add(e);
        }
    }

    public static RepositoryManager getManager() {
        if (manager == null) manager = new RepositoryManager();
        return manager;
    }

    public boolean hasRepoSetting() {
        return FileUtils.file2string(AppUtils.curDir + "/tools/repository.json") != null;
    }

    public List<RepositoryEntity> repositoryList() {
        return repositoryList;
    }
}
