package dark.leech.text.models;
/**
 * Code by Darkrai on 8/21/2016.
 */

import java.util.List;

public class Properties {
    private String name; //Tên truyện
    private String author; //Tác giả
    private String url; //Đường dẩn truyện
    private String cover; //Đường dẫn Cover
    private List<Chapter> chapList; //Danh sách chương
    private List<Pager> pageList; //Danh sách trang
    private boolean forum; //Trang get có phải forum hay không
    private int size; //Số chương
    private String savePath; //Thư mục lưu
    private String gioiThieu;
    private boolean addGt;
    private String charset = "UTF-8";
    private String[] urlList;

    public boolean isAddGt() {
        return addGt;
    }

    public void setAddGt(boolean addGt) {
        this.addGt = addGt;
    }

    public String getGioiThieu() {
        return gioiThieu;
    }

    public void setGioiThieu(String gioiThieu) {
        this.gioiThieu = gioiThieu;
    }

    public String getSavePath() {
        return savePath;
    }

    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public void setCover(String cover, String page) {
        if (cover == null) return;
        if (cover.startsWith("http"))
            this.cover = cover;
        else this.cover = page + cover;
    }

    public boolean isForum() {
        return forum;
    }

    public void setForum(boolean forum) {
        this.forum = forum;
    }

    public List<Chapter> getChapList() {
        return chapList;
    }

    public void setChapList(List<Chapter> chapList) {
        this.chapList = chapList;
    }

    public List<Pager> getPageList() {
        return pageList;
    }

    public void setPageList(List<Pager> pageList) {
        this.pageList = pageList;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public String[] getUrlList() {
        return urlList;
    }

    public void setUrlList(String[] urlList) {
        this.urlList = urlList;
    }
}
