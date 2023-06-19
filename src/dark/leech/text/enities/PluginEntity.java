package dark.leech.text.enities;

import com.google.gson.annotations.SerializedName;

public class PluginEntity {

    @SerializedName("name")
    private String name; //Tên
    @SerializedName("author")
    private String author; //Tác giả
    @SerializedName("path")
    private String path; //Link plugin
    @SerializedName("version")
    private double version; //Phiên bản
    @SerializedName("source")
    private String source; //Trang nguồn
    @SerializedName("icon")
    private String icon; //Icon, base64
    @SerializedName("description")
    private String description; //Mô tả
    @SerializedName("locale")
    private String locale;
    @SerializedName("regexp")
    private String regex; //Chuỗi khớp Http
    @SerializedName("language")
    private String language; //Ngôn ngữ (code: vi, en)

    private String pathName;

    @SerializedName("type")
    private String type; //Nhóm: dich, convert, truyentranh
    @SerializedName("data")
    private String data; //Base64
    private boolean supportUpdate;

    //Class
    @SerializedName("chap")
    private String chapGetter; //Nội dung chương
    @SerializedName("toc")
    private String tocGetter; //Danh sánh chương
    @SerializedName("page")
    private String pageGetter; //Dnah sách trang chương
    @SerializedName("search")
    private String searchGetter; //Tìm kiếm
    @SerializedName("detail")
    private String detailGetter; //Chi tiết

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    @SerializedName("checked")
    private boolean checked;


    public String getPathName() {
        return pathName;
    }

    public void setPathName(String pathName) {
        this.pathName = pathName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getVersion() {
        return version;
    }

    public void setVersion(double version) {
        this.version = version;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String url) {
        this.path = url;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getChapGetter() {
        return chapGetter;
    }

    public void setChapGetter(String chapGetter) {
        this.chapGetter = chapGetter;
    }

    public String getTocGetter() {
        return tocGetter;
    }

    public void setTocGetter(String tocGetter) {
        this.tocGetter = tocGetter;
    }

    public String getPageGetter() {
        return pageGetter;
    }

    public void setPageGetter(String pageGetter) {
        this.pageGetter = pageGetter;
    }

    public String getSearchGetter() {
        return searchGetter;
    }

    public void setSearchGetter(String searchGetter) {
        this.searchGetter = searchGetter;
    }

    public String getDetailGetter() {
        return detailGetter;
    }

    public void setDetailGetter(String detailGetter) {
        this.detailGetter = detailGetter;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public boolean isSupportUpdate() {
        return supportUpdate;
    }

    public void setSupportUpdate(boolean supportUpdate) {
        this.supportUpdate = supportUpdate;
    }

    public void apply(PluginEntity entity) {
        this.pathName = entity.pathName;
        this.name = entity.name;
        this.version = entity.version;
        this.path = entity.path;
        this.locale = entity.locale;
        this.icon = entity.icon;
        this.source = entity.source;
        this.regex = entity.regex;
        this.author = entity.author;
        this.description = entity.description;
        this.type = entity.type;
        this.supportUpdate = entity.supportUpdate;
        this.chapGetter = entity.chapGetter;
        this.tocGetter = entity.tocGetter;
        this.pageGetter = entity.pageGetter;
        this.searchGetter = entity.searchGetter;
        this.detailGetter = entity.detailGetter;
        this.checked = true;
    }
}
