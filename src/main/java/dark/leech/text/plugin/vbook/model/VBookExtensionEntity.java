package dark.leech.text.plugin.vbook.model;

import com.google.gson.annotations.SerializedName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a vBook extension from the registry. Matches the data array items in vBook's
 * plugin.json.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VBookExtensionEntity {

    /** Plugin display name */
    @SerializedName("name")
    private String name;

    /** Plugin author */
    @SerializedName("author")
    private String author;

    /** URL to download the plugin.zip file */
    @SerializedName("path")
    private String path;

    /** Plugin version (integer format) */
    @SerializedName("version")
    private Integer version;

    /** Source website base URL */
    @SerializedName("source")
    private String source;

    /** Icon URL */
    @SerializedName("icon")
    private String icon;

    /** Plugin description */
    @SerializedName("description")
    private String description;

    /** Content type: "novel" or "comic" */
    @SerializedName("type")
    private String type;

    /** Locale code (e.g., "vi_VN") */
    @SerializedName("locale")
    private String locale;

    /** Optional tag (e.g., "nsfw") */
    @SerializedName("tag")
    private String tag;
}
