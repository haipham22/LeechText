package dark.leech.text.models;

/**
 * Created by Dark on 2/16/2017.
 */
public class Post {
    private String partName; // Tên quyển
    private String chapName; // Tên chương
    private boolean error; // Lỗi
    private boolean empty; //Chương trống
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

    public String getPartName() {
        return partName;
    }

    public void setPartName(String partName) {
        this.partName = partName;
    }

    public String getChapName() {
        return chapName;
    }

    public void setChapName(String chapName) {
        this.chapName = chapName;
    }

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public boolean isEmpty() {
        return empty;
    }

    public void setEmpty(boolean empty) {
        this.empty = empty;
    }

    public boolean isImageChapter() {
        return imageChapter;
    }

    public void setImageChapter(boolean imageChapter) {
        this.imageChapter = imageChapter;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
