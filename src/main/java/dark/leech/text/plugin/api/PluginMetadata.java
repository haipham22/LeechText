package dark.leech.text.plugin.api;

/**
 * Lightweight plugin metadata for search results and listings. Contains display information without
 * full plugin data.
 */
public class PluginMetadata {
    private final String id;
    private final String name;
    private final String author;
    private final String version;
    private final String description;
    private final String source;
    private final String iconUrl;
    private final int popularity;
    private final String[] tags;
    private final long lastUpdated;
    private final long sizeBytes;

    protected PluginMetadata(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.author = builder.author;
        this.version = builder.version;
        this.description = builder.description;
        this.source = builder.source;
        this.iconUrl = builder.iconUrl;
        this.popularity = builder.popularity;
        this.tags = builder.tags != null ? builder.tags : new String[0];
        this.lastUpdated = builder.lastUpdated;
        this.sizeBytes = builder.sizeBytes;
    }

    // Constructor for subclasses to pass individual values
    protected PluginMetadata(
            String id,
            String name,
            String author,
            String version,
            String description,
            String source,
            String iconUrl,
            int popularity,
            String[] tags,
            long lastUpdated,
            long sizeBytes) {
        this.id = id;
        this.name = name;
        this.author = author;
        this.version = version;
        this.description = description;
        this.source = source;
        this.iconUrl = iconUrl;
        this.popularity = popularity;
        this.tags = tags != null ? tags : new String[0];
        this.lastUpdated = lastUpdated;
        this.sizeBytes = sizeBytes;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAuthor() {
        return author;
    }

    public String getVersion() {
        return version;
    }

    public String getDescription() {
        return description;
    }

    public String getSource() {
        return source;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public int getPopularity() {
        return popularity;
    }

    public String[] getTags() {
        return tags;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public static Builder builder() {
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
        private int popularity;
        private String[] tags;
        private long lastUpdated;
        private long sizeBytes;

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

        public PluginMetadata build() {
            return new PluginMetadata(this);
        }
    }

    @Override
    public String toString() {
        return "PluginMetadata{"
                + "id='"
                + id
                + '\''
                + ", name='"
                + name
                + '\''
                + ", version='"
                + version
                + '\''
                + ", source='"
                + source
                + '\''
                + '}';
    }
}
