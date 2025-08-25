package dark.leech.text.models;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Repository {
    private MetaData metaData;
    private List<Plugin> plugins;

    @Data
    @Builder
    public static class MetaData {
        public String author;
        public String description;
    }

    @Data
    @Builder
    public static class Plugin {
        public String name;
        public String uuid;
        public String author;
        public String path;
        public int version;
        public String source;
        public String icon;
        public String description;
        public String type;
        public String locale;
        public String tag;
    }
}
