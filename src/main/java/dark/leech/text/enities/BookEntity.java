package dark.leech.text.enities;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;


public class BookEntity implements Serializable {

    @SerializedName("book_id")
    private String id = "";

    @SerializedName("name")
    private String name;

    @SerializedName("author")
    private String author;

    @SerializedName("cover")
    private String cover;

    @SerializedName("url")
    private String url;

    @SerializedName("introduce")
    private String introduce;

    @SerializedName("web_source")
    private String webSource;

    @SerializedName("detail")
    private String detail;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getIntroduce() {
        return introduce;
    }

    public void setIntroduce(String introduce) {
        this.introduce = introduce;
    }

    public String getWebSource() {
        return webSource;
    }

    public void setWebSource(String webSource) {
        this.webSource = webSource;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

}
