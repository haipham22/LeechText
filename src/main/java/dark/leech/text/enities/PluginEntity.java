package dark.leech.text.enities;

import com.google.gson.annotations.SerializedName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing a plugin with metadata and getter classes. Used for plugin management,
 * serialization, and updates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PluginEntity {

    @SerializedName("uuid")
    private String uuid;

    @SerializedName("name")
    private String name;

    @SerializedName("version")
    private Double version;

    @SerializedName("url")
    private String url;

    @SerializedName("language")
    private String language;

    @SerializedName("icon")
    private String icon;

    @SerializedName("source")
    private String source;

    @SerializedName("regex")
    private String regex;

    @SerializedName("author")
    private String author;

    @SerializedName("describe")
    private String describe;

    @SerializedName("group")
    private String group;

    @SerializedName("data")
    private String data;

    @Builder.Default private boolean supportUpdate = false;

    @SerializedName("chap")
    private String chapGetter;

    @SerializedName("toc")
    private String tocGetter;

    @SerializedName("page")
    private String pageGetter;

    @SerializedName("gen")
    private String genGetter;

    @SerializedName("search")
    private String searchGetter;

    @SerializedName("detail")
    private String detailGetter;

    @Builder.Default private boolean checked = false;

    /**
     * Apply all fields from another entity, marking this entity as checked. Used for updating
     * plugin metadata from remote sources.
     */
    public void apply(PluginEntity entity) {
        this.uuid = entity.uuid == null ? java.util.UUID.randomUUID().toString() : entity.uuid;
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
        this.data = entity.data;
        this.supportUpdate = entity.supportUpdate;
        this.chapGetter = entity.chapGetter;
        this.tocGetter = entity.tocGetter;
        this.pageGetter = entity.pageGetter;
        this.genGetter = entity.genGetter;
        this.searchGetter = entity.searchGetter;
        this.detailGetter = entity.detailGetter;
        this.checked = true;
    }
}
