package dark.leech.text.enities;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PluginEntity {

    @SerializedName("uuid")
    private String uuid = "";

    @SerializedName("name")
    private String name; // Tên

    @SerializedName("version")
    private double version; // Phiên bản

    @SerializedName("url")
    private String url; // Link plugin

    @SerializedName("language")
    private String language; // Ngôn ngữ (code: vi, en)

    @SerializedName("icon")
    private String icon; // Icon, base64

    @SerializedName("source")
    private String source; // Trang nguồn

    @SerializedName("regex")
    private String regex; // Chuỗi khớp Http

    @SerializedName("author")
    private String author; // Tác giả

    @SerializedName("describe")
    private String describe; // Mô tả

    @SerializedName("group")
    private String group; // Nhóm: dich, convert, truyentranh

    @SerializedName("data")
    private String data; // Base64

    private boolean supportUpdate;

    // Class
    @SerializedName("chap")
    private String chapGetter; // Nội dung chương

    @SerializedName("toc")
    private String tocGetter; // Danh sánh chương

    @SerializedName("page")
    private String pageGetter; // Dnah sách trang chương

    @SerializedName("search")
    private String searchGetter; // Tìm kiếm

    @SerializedName("detail")
    private String detailGetter; // Chi tiết

    @SerializedName("checked")
    private boolean checked;

    public void apply(PluginEntity entity) {
        this.uuid = entity.uuid;
        this.name = entity.name;
        this.version = entity.version;
        this.url = entity.url;
        this.language = entity.language;
        this.icon = entity.icon;
        this.source = entity.source;
        this.regex = entity.regex;
        this.author = entity.author;
        this.describe = entity.describe;
        this.group = entity.group;
        this.supportUpdate = entity.supportUpdate;
        this.chapGetter = entity.chapGetter;
        this.tocGetter = entity.tocGetter;
        this.pageGetter = entity.pageGetter;
        this.searchGetter = entity.searchGetter;
        this.detailGetter = entity.detailGetter;
        this.checked = true;
    }
}
