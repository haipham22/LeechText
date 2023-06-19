package dark.leech.text.get;

import dark.leech.text.enities.PluginEntity;
import dark.leech.text.listeners.ChangeListener;
import dark.leech.text.lua.loader.TextLoader;
import dark.leech.text.models.Chapter;
import dark.leech.text.util.AppUtils;
import dark.leech.text.util.FileUtils;
import dark.leech.text.util.SettingUtils;
import dark.leech.text.util.SyntaxUtils;

import javax.swing.*;

/**
 * Created by Dark on 1/18/2017.
 */
public class ChapExecute extends SwingWorker<String, Void> {
    private TextLoader chapGetter;
    private Chapter chapter;
    private ChangeListener changeListener;
    private String text;
    private String savepath;
    private String chset = "utf-8";

    @Override
    protected String doInBackground() throws Exception {
        AppUtils.pause(SettingUtils.DELAY);
        return chapGetter.load(chapter.getUrl());
    }

    @Override
    protected void done() {
        try {
            text = get();
            chapter.setError(false);
            chapter.setEmpty(false);
            if (text == null || text.length() == 0)
                chapter.setError(true);
            else if (text.length() < 1000)
                if (text.split("<img ").length > 1)
                    chapter.setImageChapter(true);
                else chapter.setEmpty(true);
            chapter.setCompleted(true);
            if (!(chapter.isEmpty() && chapter.isError()))
                write();
        } catch (Exception e) {
            chapter.setError(true);
        }
        changeListener.doChanger();

    }

    public ChapExecute plugin(PluginEntity pluginEntity) {
        chapGetter = TextLoader.with(pluginEntity);
        return this;
    }

    public ChapExecute listener(ChangeListener changeListener) {
        this.changeListener = changeListener;
        return this;
    }

    public ChapExecute path(String savepath) {
        this.savepath = savepath;
        return this;
    }

    public ChapExecute charset(String chset) {
        this.chset = chset;
        return this;
    }

    public ChapExecute applyTo(Chapter chapter) {
        this.chapter = chapter;
        return this;
    }

    private void write() {
        FileUtils.string2file(Optimize(text), savepath + "/raw/" + chapter.getId() + ".txt", chset);
    }


    private String Optimize(String src) {
        if (SyntaxUtils.REPLACE_FROM != null)
            for (int i = 0; i < SyntaxUtils.REPLACE_FROM.length; i++)
                src = src.replaceAll(SyntaxUtils.REPLACE_FROM[i], SyntaxUtils.REPLACE_TO[i]);
        return src;
    }
}
