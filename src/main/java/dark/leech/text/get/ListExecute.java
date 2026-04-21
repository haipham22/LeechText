package dark.leech.text.get;

import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import dark.leech.text.action.Log;
import dark.leech.text.enities.ChapterEntity;
import dark.leech.text.enities.PluginEntity;
import dark.leech.text.listeners.ChangeListener;
import dark.leech.text.models.Chapter;
import dark.leech.text.models.Properties;
import dark.leech.text.plugin.js.loader.GenLoader;
import dark.leech.text.plugin.js.loader.ListLoader;

/** Created by Dark on 1/18/2017. */
public class ListExecute extends SwingWorker {
    private ListLoader loader;
    private GenLoader genLoader;
    private ChangeListener changeListener;
    private Properties properties;
    private boolean success;

    public ListExecute plugin(PluginEntity plugin) {
        loader = ListLoader.with(plugin);
        genLoader = GenLoader.with(plugin);
        return this;
    }

    public ListExecute listener(ChangeListener changeListener) {
        this.changeListener = changeListener;
        return this;
    }

    public ListExecute applyTo(Properties properties) {
        this.properties = properties;
        return this;
    }

    @Override
    protected Void doInBackground() {
        try {
            List<Chapter> chapters = new ArrayList<>();
            String url = properties.getUrl();
            String page = null;

            // Pagination loop - load all pages
            do {
                var pageResult = genLoader.load(url, page);

                if (pageResult != null && pageResult.getItems() != null && !pageResult.getItems().isEmpty()) {
                    // Convert pagination items (NativeObject from JavaScript) to chapters
                    for (Object item : pageResult.getItems()) {
                        try {
                            ChapterEntity chap = convertToChapterEntity(item);
                            if (chap != null && chap.getName() != null && chap.getUrl() != null) {
                                chapters.add(new Chapter(chap.getUrl(), chap.getName()));
                            }
                        } catch (Exception e) {
                            Log.add("[ListExecute] Failed to convert item: " + e.getMessage());
                        }
                    }

                    // Get next page identifier
                    page = pageResult.getNextPage();

                    Log.add("[ListExecute] Loaded " + pageResult.getItems().size() +
                           " items, next page: " + (page != null ? page : "none"));
                } else {
                    // No pagination result, try direct list loading (legacy mode)
                    List<ChapterEntity> chapterList = loader.load(url);
                    if (chapterList != null) {
                        for (ChapterEntity chap : chapterList) {
                            chapters.add(new Chapter(chap.getUrl(), chap.getName()));
                        }
                    }
                    break;
                }
            } while (page != null && !page.isEmpty());

            properties.setChapList(chapters);
            properties.setSize(chapters.size());
            success = true;
        } catch (Exception e) {
            Log.add(e);
        }
        return null;
    }

    /**
     * Convert NativeObject from JavaScript to ChapterEntity.
     * Handles both direct ChapterEntity and NativeObject with name/url/link properties.
     */
    private ChapterEntity convertToChapterEntity(Object item) {
        if (item instanceof ChapterEntity) {
            return (ChapterEntity) item;
        }

        if (item instanceof org.mozilla.javascript.NativeObject) {
            org.mozilla.javascript.NativeObject nativeObj = (org.mozilla.javascript.NativeObject) item;

            String name = getPropertyAsString(nativeObj, "name");
            String url = getPropertyAsString(nativeObj, "url");

            // Fallback to 'link' property if 'url' is not present
            if (url == null || url.isEmpty()) {
                url = getPropertyAsString(nativeObj, "link");
            }

            if (name != null && url != null) {
                ChapterEntity entity = new ChapterEntity();
                entity.setName(name);
                entity.setUrl(url);
                return entity;
            }
        }

        return null;
    }

    /**
     * Safely extract string property from NativeObject.
     */
    private String getPropertyAsString(org.mozilla.javascript.NativeObject obj, String key) {
        Object value = obj.get(key, obj);
        return value != null ? value.toString() : null;
    }

    @Override
    public void done() {
        if (success) {
            List<Chapter> chapList = properties.getChapList();
            for (int i = 0; i < chapList.size(); i++) chapList.get(i).setId(i);
        }
        changeListener.doChanger();
    }
}
