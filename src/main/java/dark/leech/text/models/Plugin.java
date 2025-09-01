package dark.leech.text.models;

import com.google.gson.annotations.SerializedName;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Plugin {

    @SerializedName("metadata")
    private Metadata metadata;

    private Script script;

    @Data
    @Builder
    public static class Metadata {
        private String name;
        private String author;
        private int version;
        private String source;
        private String regexp;
        private String description;
        private String locale;
        private String type;
        private String language;
    }

    @Data
    @Builder
    public static class Script {
        private String home;
        private String genre;
        private String search;
        private String detail;
        private String toc;
        private String chap;
    }
}
