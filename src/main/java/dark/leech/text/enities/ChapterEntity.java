package dark.leech.text.enities;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ChapterEntity {
    @SerializedName("id")
    private int id;

    @SerializedName("chapter_name")
    private String name;

    @SerializedName("chapter_url")
    private String url;
}
