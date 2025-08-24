package dark.leech.text.enities;

import java.io.Serializable;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
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
}
