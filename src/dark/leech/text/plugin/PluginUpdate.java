package dark.leech.text.plugin;

import com.google.gson.Gson;
import dark.leech.text.action.Log;
import dark.leech.text.enities.PluginEntity;
import dark.leech.text.enities.RepositoryEntity;
import dark.leech.text.repository.RepositoryManager;
import dark.leech.text.ui.notification.Toast;
import dark.leech.text.util.AppUtils;
import dark.leech.text.util.FileUtils;
import dark.leech.text.util.Http;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Dark on 2/24/2017.
 */
public class PluginUpdate {
    private static PluginUpdate pluginUpdate;

    private PluginUpdate() {

    }

    public static PluginUpdate getUpdate() {
        if (pluginUpdate == null) pluginUpdate = new PluginUpdate();
        return pluginUpdate;
    }


    public void checkUpdate() {
        try {
            Set<RepositoryEntity> repositoryList = RepositoryManager.getManager().getRepositoryList();
            for (RepositoryEntity repo : repositoryList) {
                String js = Http.request(repo.getLink()).string();
                JSONObject jsonObject = new JSONObject(js);
                JSONArray objArr = jsonObject.getJSONArray("data");
                for (int i = 0; i < objArr.length(); i++) {
                    JSONObject obj = objArr.getJSONObject(i);
                    String strUuid = obj.getString("path")
                            + obj.getString("author")
                            + obj.getString("type");
                    UUID uuid = UUID.nameUUIDFromBytes(strUuid.getBytes(StandardCharsets.UTF_8));
                    boolean have = false;
                    for (PluginEntity pluginGetter : PluginManager.getManager().list()) {
                        if (uuid.toString().equals(pluginGetter.getUuid())) {
                            have = true;
                            if (obj.getDouble("version") > pluginGetter.getVersion()) {
                                String path = AppUtils.curDir
                                        + "/tools/plugins/"
                                        + pluginGetter.getUuid()
                                        + ".plugin";
                                String json = Http.request(obj.getString("path")).string();

                                FileUtils.url2file(obj.getString("path"), path);

                                PluginEntity entity = new Gson().fromJson(FileUtils.file2string(path), PluginEntity.class);
                                entity.setChecked(true);
                                pluginGetter.apply(entity);
                                Toast.Build()
                                        .content("Đã update plugin " + pluginGetter.getName() + " v" + pluginGetter.getVersion())
                                        .time(3000)
                                        .open();
                            }
                        }
                    }
                    if (Boolean.FALSE.equals(have)) {
                        String path = AppUtils.curDir
                                + "/tools/plugins/"
                                + uuid.toString()
                                + ".plugin";
                        String json = Http.request(obj.getString("path")).string();

                        FileUtils.url2file(obj.getString("path"), path);

                        PluginEntity entity = new Gson().fromJson(FileUtils.file2string(path), PluginEntity.class);

//                        PluginEntity entity = new Gson().fromJson(obj.toString(), PluginEntity.class);
                        entity.setChecked(true);
                        entity.setUuid(uuid.toString());
                        FileUtils.string2file(new Gson().toJson(json), path);
                        PluginManager.getManager().add(path);
                        Toast.Build()
                                .content("Đã tải xuống plugin " + obj.getString("name"))
                                .time(3000)
                                .open();
                    }
                }
            }
        } catch (Exception e) {
            Log.add(e);
        }

    }


}
