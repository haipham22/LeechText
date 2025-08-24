package dark.leech.text.enities;

import com.google.gson.annotations.SerializedName;

public class ChapterEntity {
    @SerializedName("id")
    private int id;
    @SerializedName("chapter_name")
    private String name;
    @SerializedName("chapter_url")
    private String url;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

}
