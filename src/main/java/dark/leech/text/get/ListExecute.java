package dark.leech.text.get;

import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import dark.leech.text.action.Log;
import dark.leech.text.enities.ChapterEntity;
import dark.leech.text.enities.PluginEntity;
import dark.leech.text.listeners.ChangeListener;
import dark.leech.text.models.Chapter;
import dark.leech.text.models.Pager;
import dark.leech.text.models.Properties;
import dark.leech.text.plugin.js.loader.ListLoader;

/** Created by Dark on 1/18/2017. */
public class ListExecute extends SwingWorker {
    private ListLoader loader;
    private ChangeListener changeListener;
    private Properties properties;
    private boolean success;

    public ListExecute plugin(PluginEntity plugin) {
        loader = ListLoader.with(plugin);
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
            List<ChapterEntity> chapterList = loader.load(properties.getUrl());

            List<Chapter> chapters = new ArrayList<>();

            if (chapterList != null) {
                for (ChapterEntity chap : chapterList) {
                    chapters.add(new Chapter(chap.getUrl(), chap.getName()));
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
            if (properties.isForum()) {
                List<Pager> pageList = properties.getPageList();
                for (int i = 0; i < pageList.size(); i++) {
                    Pager pager = pageList.get(i);
                    if (pager.getName() == null) pager.setName("Trang " + (i + 1));
                    pager.setId(i);
                }
            } else {
                List<Chapter> chapList = properties.getChapList();
                for (int i = 0; i < chapList.size(); i++) chapList.get(i).setId(i);
            }
        }
        changeListener.doChanger();
    }
}
