package dark.leech.text.plugin.vbook.model;

import dark.leech.text.plugin.api.PluginMetadata;

/** vBook-specific plugin metadata. Extends generic PluginMetadata with vBook-specific fields. */
@SuppressWarnings("unchecked")
public class VBookPluginMetadata extends PluginMetadata {

    private final String type;
    private final String group;
    private final String language;

    private VBookPluginMetadata(Builder builder) {
        super(
                builder.id,
                builder.name,
                builder.author,
                builder.version,
                builder.description,
                builder.source,
                builder.iconUrl,
                builder.popularity,
                builder.tags,
                builder.lastUpdated,
                builder.sizeBytes);
        this.type = builder.type;
        this.group = builder.group;
        this.language = builder.language;
    }

    public String getType() {
        return type;
    }

    public String getGroup() {
        return group;
    }

    public String getLanguage() {
        return language;
    }

    public static Builder vbookBuilder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String name;
        private String author;
        private String version;
        private String description;
        private String source;
        private String iconUrl;
        private int popularity = 0;
        private String[] tags = {};
        private long lastUpdated = 0;
        private long sizeBytes = 0;
        private String type = "vbook";
        private String group = "";
        private String language = "vi";

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder author(String author) {
            this.author = author;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public Builder iconUrl(String iconUrl) {
            this.iconUrl = iconUrl;
            return this;
        }

        public Builder popularity(int popularity) {
            this.popularity = popularity;
            return this;
        }

        public Builder tags(String[] tags) {
            this.tags = tags;
            return this;
        }

        public Builder lastUpdated(long lastUpdated) {
            this.lastUpdated = lastUpdated;
            return this;
        }

        public Builder sizeBytes(long sizeBytes) {
            this.sizeBytes = sizeBytes;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder group(String group) {
            this.group = group;
            return this;
        }

        public Builder language(String language) {
            this.language = language;
            return this;
        }

        public VBookPluginMetadata build() {
            return new VBookPluginMetadata(this);
        }
    }
}
