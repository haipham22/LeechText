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
import dark.leech.text.plugin.js.loader.ListLoader;
import dark.leech.text.plugin.js.loader.PageLoader;

/** Created by Dark on 1/18/2017. */
public class ListExecute extends SwingWorker {
    private ListLoader loader;
    private PageLoader pageLoader;
    private ChangeListener changeListener;
    private Properties properties;
    private boolean success;

    public ListExecute plugin(PluginEntity plugin) {
        loader = ListLoader.with(plugin);
        pageLoader = PageLoader.with(plugin);
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

            // Try page discovery first (page.js pattern)
            List<String> pageUrls = pageLoader.load(url);

            if (pageUrls != null && !pageUrls.isEmpty()) {
                // Page discovery succeeded - use discovered URLs
                Log.add("[ListExecute] Discovered " + pageUrls.size() + " page URLs");
                for (String pageUrl : pageUrls) {
                    try {
                        // Load each discovered page URL to get chapter list
                        List<ChapterEntity> chapterList = loader.load(pageUrl);
                        if (chapterList != null) {
                            for (ChapterEntity chap : chapterList) {
                                if (chap.getName() != null && chap.getUrl() != null) {
                                    chapters.add(new Chapter(chap.getUrl(), chap.getName()));
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.add(
                                "[ListExecute] Failed to load page "
                                        + pageUrl
                                        + ": "
                                        + e.getMessage());
                    }
                }
            } else {
                // No page discovery, try direct list loading (legacy toc.js pattern)
                Log.add("[ListExecute] No page URLs discovered, trying direct list loading");
                List<ChapterEntity> chapterList = loader.load(url);
                if (chapterList != null) {
                    for (ChapterEntity chap : chapterList) {
                        chapters.add(new Chapter(chap.getUrl(), chap.getName()));
                    }
                }
            }

            properties.setChapList(chapters);
            properties.setSize(chapters.size());
            success = true;
        } catch (Exception e) {
            Log.add(e);
        }
        return null;
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
