package dark.leech.text.get;

import dark.leech.text.action.Log;
import dark.leech.text.listeners.ChangeListener;
import dark.leech.text.models.Chapter;
import dark.leech.text.models.Pager;
import dark.leech.text.models.Post;
import dark.leech.text.util.FileUtils;
import dark.leech.text.util.SyntaxUtils;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dark on 1/21/2017.
 */
public class PageExecute extends SwingWorker<ArrayList<Post>, Void> {
    private PageGetter pageGetter;
    private ChangeListener changeListener;
    private Pager pager;
    private String chset = "utf-8";
    ;
    private String savepath;

    public PageExecute() {
    }

    @Override
    protected ArrayList<Post> doInBackground() throws Exception {
        return pageGetter.getter(pager.getUrl());
    }

    public PageExecute clazz(Class cl) {
        try {
            pageGetter = (PageGetter) cl.newInstance();
        } catch (InstantiationException e) {
            Log.add(e);
        } catch (IllegalAccessException e) {
            Log.add(e);
        }
        return this;
    }

    public PageExecute listener(ChangeListener changeListener) {
        this.changeListener = changeListener;
        return this;
    }

    public PageExecute path(String savepath) {
        this.savepath = savepath;
        return this;
    }

    public PageExecute charset(String chset) {
        this.chset = chset;
        return this;
    }

    public PageExecute applyTo(Pager pager) {
        this.pager = pager;
        return this;
    }

    private void write(String text, String ID) {
        FileUtils.string2file(Optimize(text), savepath + "/raw/" + ID + ".txt", chset);
    }

    @Override
    public void done() {
        try {
            List<Post> posts = get();
            List<Chapter> chapters = new ArrayList<>();
            for (int i = 0; i < posts.size(); i++) {
                Chapter chapter = new Chapter();
                Post post = posts.get(i);
                String text = post.getText();
                String id = pager.getId() + "C" + Integer.toString(i);
                if (text == null) {
                    chapter.setError(true);
                } else if (text.length() < 1000) {
                    if (text.split("<img ").length > 0)
                        chapter.setImageChapter(true);
                    else chapter.setEmpty(true);
                }
                chapter.setUrl(pager.getUrl());
                chapter.setChapName(post.getChapName());
                chapter.setPartName(post.getPartName());
                chapter.setCompleted(true);
                chapter.setId(id);
                chapters.add(chapter);
                if (!(chapter.isEmpty() && chapter.isError())) {
                    write(text, id);
                }
            }
            pager.setChapter(chapters);
        } catch (Exception e) {
            Log.add(e);
            pager.setChapter(new ArrayList<Chapter>());
        }
        changeListener.doChanger();
    }


    private String Optimize(String src) {
        if (SyntaxUtils.REPLACE_FROM != null)
            for (int i = 0; i < SyntaxUtils.REPLACE_FROM.length; i++)
                src = src.replaceAll(SyntaxUtils.REPLACE_FROM[i], SyntaxUtils.REPLACE_TO[i]);
        return src;
    }
}
