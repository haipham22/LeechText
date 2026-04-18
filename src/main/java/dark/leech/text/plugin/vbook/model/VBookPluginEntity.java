package dark.leech.text.plugin.vbook.model;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a fully extracted vBook plugin from plugin.zip. Contains metadata parsed from
 * plugin.json and extracted script contents.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VBookPluginEntity {

    // Metadata fields from plugin.json

    @SerializedName("name")
    private String name;

    @SerializedName("author")
    private String author;

    @SerializedName("version")
    private Integer version;

    @SerializedName("source")
    private String source;

    @SerializedName("regexp")
    private String regexp;

    @SerializedName("description")
    private String description;

    @SerializedName("locale")
    private String locale;

    @SerializedName("type")
    private String type;

    @SerializedName("language")
    private String language;

    @SerializedName("priority")
    private Integer priority;

    @SerializedName("tag")
    private String tag;

    // Script file references (from script section)

    /** Maps script type to filename (e.g., "chap" -> "chap.js") */
    @Builder.Default private Map<String, String> scripts = new HashMap<>();

    // Extracted content

    /** Maps script type to actual JavaScript content */
    @Builder.Default private Map<String, String> scriptContents = new HashMap<>();

    /** Icon as base64 data URI */
    private String iconBase64;

    /** Raw plugin.json content for reference */
    private String rawMetadata;
}
