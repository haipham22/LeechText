package dark.leech.text.models;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Post {
    private String partName; // Tên quyển
    private String chapName; // Tên chương
    private boolean error; // Lỗi
    private boolean empty; // Chương trống
    private boolean imageChapter;
    private String text;

    public Post() {
        this.partName = "";
        this.chapName = "";
        this.text = "";
    }

    public Post(String partName, String chapName, String text) {
        this.partName = partName;
        this.chapName = chapName;
        this.text = text;
    }

    public Post(String chapName, String text) {
        this.chapName = chapName;
        this.text = text;
    }
}
