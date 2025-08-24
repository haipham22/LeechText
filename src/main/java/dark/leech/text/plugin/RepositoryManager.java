package dark.leech.text.plugin;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import dark.leech.text.enities.RepositoryEntity;
import dark.leech.text.util.Http;

/** Created by Long on 1/11/2017. */
public class RepositoryManager {
    private static final Gson gson = new Gson();

    private static RepositoryManager manager;

    private static List<RepositoryEntity> repositoryList;

    private RepositoryManager() {

        new Thread(
                        () -> {
                            repositoryList = new ArrayList<>();
                        })
                .start();
    }

    public static RepositoryManager getManager() {
        if (manager == null) manager = new RepositoryManager();
        return manager;
    }

    public void add(String link) {
        repositoryList.addAll(addRepository(link));
    }

    private List<RepositoryEntity> addRepository(String link) {
        var js = Http.request(link).string();
        var type = TypeToken.getParameterized(List.class, RepositoryEntity.class).getType();
        return gson.fromJson(js, type);
    }

    public List<RepositoryEntity> repositoryList() {
        return repositoryList;
    }
}
