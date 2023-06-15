package dark.leech.text.enities;

import com.google.gson.annotations.SerializedName;

public class PluginEntity {

    @SerializedName("uuid")
    private String uuid = "";
    @SerializedName("name")
    private String name; //Tên
    @SerializedName("version")
    private double version; //Phiên bản
    @SerializedName("path")
    private String path; //Link plugin
    @SerializedName("locale")
    private String locale; //Ngôn ngữ (code: vi, en)
    @SerializedName("icon")
    private String icon; //Icon, base64
    @SerializedName("source")
    private String source; //Trang nguồn
    @SerializedName("regex")
    private String regex; //Chuỗi khớp Http
    @SerializedName("author")
    private String author; //Tác giả
    @SerializedName("description")
    private String description; //Mô tả
    @SerializedName("group")
    private String group; //Nhóm: dich, convert, truyentranh
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


    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    public void setPath(String path) {
        this.path = path;
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

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
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
        this.uuid = entity.uuid;
        this.name = entity.name;
        this.version = entity.version;
        this.path = entity.path;
        this.locale = entity.locale;
        this.icon = entity.icon;
        this.source = entity.source;
        this.regex = entity.regex;
        this.author = entity.author;
        this.description = entity.description;
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
