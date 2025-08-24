package dark.leech.text.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Chapter implements Cloneable {

    private String url; // Đường dẩn
    private String partName; // Tên quyển
    private String chapName; // Tên chương
    private boolean completed; // Đã tải hoàn tất
    private boolean error; // Lỗi
    private boolean empty;
    private boolean imageChapter;
    private String id; // ID chương
    private boolean purchase;

    public Chapter() {
        this("");
    }

    public Chapter(String url) {
        this(url, "");
    }

    public Chapter(String url, String name) {
        this(url, -1, "", name);
    }

    public Chapter(String url, int id, String partName, String chapName) {
        this(url, id, partName, chapName, false, false);
    }

    public Chapter(
            String url,
            int id,
            String partName,
            String chapName,
            boolean completed,
            boolean error) {
        this.url = url;
        this.id = "C" + id;
        this.partName = partName;
        this.chapName = chapName;
        this.error = error;
        this.completed = completed;
    }

    public Chapter(String url, int id, String chapName) {
        this(url, id, null, chapName);
    }

    public Chapter(String url, String id, String partName, String chapName) {
        this.url = url;
        this.partName = partName;
        this.chapName = chapName;
        this.id = id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setId(int id) {
        this.id = "C" + id;
    }

    public String getChapName() {
        if (chapName == null) chapName = "";
        return chapName;
    }

    public String getPartName() {
        if (partName == null) partName = "";
        return partName;
    }
}
