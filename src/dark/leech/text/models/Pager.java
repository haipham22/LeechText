package dark.leech.text.models;

import java.util.List;

/**
 * Created by Long on 9/15/2016.
 */
public class Pager {
    private String url;
    private String name;
    private List<Chapter> chapter;
    private String id;
    private boolean completed;

    public Pager(String url, int id) {
        this.url = url;
        this.id = "Q" + Integer.toString(id);
    }

    public Pager(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<Chapter> getChapter() {
        return chapter;
    }

    public void setChapter(List<Chapter> chapter) {
        this.chapter = chapter;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setId(int id) {
        this.id = "P" + Integer.toString(id);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
