package dark.leech.text.models;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Pager {
    private String url;
    private String name;
    private List<Chapter> chapter;
    private String id;
    private boolean completed;

    public Pager(String url, int id) {
        this.url = url;
        this.id = "Q" + id;
    }

    public Pager(String url) {
        this.url = url;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setId(int id) {
        this.id = "P" + id;
    }
}
