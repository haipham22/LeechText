package dark.leech.text.action;

import dark.leech.text.models.Chapter;
import dark.leech.text.models.Properties;
import dark.leech.text.util.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class History {

    private History() {
    }

    public static History getHistory() {
        return new History();
    }

    public Properties load(String path) {
        Properties properties = new Properties();
        try {
            if (!(new File(path).exists())) return null;
            JSONObject obj = new JSONObject(FileUtils.file2string(path));
            JSONObject metadata = obj.getJSONObject("metadata");
            properties.setName(metadata.getString("name"));
            properties.setAuthor(metadata.getString("author"));
            properties.setUrl(metadata.getString("url"));
            properties.setCover(metadata.getString("cover"), "");
            properties.setSize(metadata.getInt("size"));
            properties.setSavePath(metadata.getString("path"));
            properties.setGioiThieu(metadata.getString("gioithieu"));
            JSONArray array = obj.getJSONArray("list");
            List<Chapter> list = new ArrayList<Chapter>();
            for (int i = 0; i < array.length(); i++)
                list.add(getChapter(array.getJSONObject(i)));
            properties.setChapList(list);
        } catch (Exception e) {
            Log.add(e);
        }
        return properties;
    }

    public void save(Properties properties) {
        JSONObject his = new JSONObject();
        JSONObject metadata = new JSONObject();
        metadata.put("url", properties.getUrl());
        metadata.put("name", properties.getName());
        metadata.put("author", properties.getAuthor());
        metadata.put("cover", properties.getCover());
        metadata.put("size", properties.getSize());
        metadata.put("path", properties.getSavePath());
        metadata.put("gioithieu", properties.getGioiThieu());
        his.put("metadata", metadata);
        JSONArray list = new JSONArray();

        // danh sách chương
        for (Chapter c : properties.getChapList())
            list.put(getObjectList(c));
        his.put("list", list);

        FileUtils.string2file(his.toString(), properties.getSavePath() + "/properties.json");
    }

    private JSONObject getObjectList(Chapter chapter) {
        JSONObject obj = new JSONObject();
        obj.put("id", chapter.getId());
        obj.put("error", chapter.isError());
        obj.put("part", chapter.getPartName());
        obj.put("chap", chapter.getChapName());
        obj.put("url", chapter.getUrl());
        return obj;
    }

    private Chapter getChapter(JSONObject jsonObject) {
        Chapter chapter = new Chapter();
        chapter.setId(jsonObject.getString("id"));
        chapter.setError(jsonObject.getBoolean("error"));
        chapter.setPartName(jsonObject.getString("part"));
        chapter.setChapName(jsonObject.getString("chap"));
        chapter.setUrl(jsonObject.getString("url"));
        chapter.setCompleted(true);
        return chapter;
    }
}


