package dark.leech.text.models;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Properties {
    private String name; // Tên truyện
    private String author; // Tác giả
    private String url; // Đường dẩn truyện
    private String cover; // Đường dẫn Cover
    private List<Chapter> chapList; // Danh sách chương
    private List<Pager> pageList; // Danh sách trang
    private boolean forum; // Trang get có phải forum hay không
    private int size; // Số chương
    private String savePath; // Thư mục lưu
    private String gioiThieu;
    private boolean addGt;
    private String charset = "UTF-8";
    private String[] urlList;

    public void setCover(String cover) {
        this.cover = cover;
    }

    public void setCover(String cover, String page) {
        if (cover == null) return;
        if (cover.startsWith("http")) this.cover = cover;
        else this.cover = page + cover;
    }
}
