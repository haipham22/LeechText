package dark.leech.text.plugin.api;

/** Repository metadata. Includes version, update check URL, and other repository information. */
public class RepositoryMetadata {

    private final String name;
    private final String description;
    private final String version;
    private final String updateCheckUrl;
    private final String homepageUrl;
    private final String author;
    private final long lastUpdated;
    private final int pluginCount;

    private RepositoryMetadata(Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.version = builder.version;
        this.updateCheckUrl = builder.updateCheckUrl;
        this.homepageUrl = builder.homepageUrl;
        this.author = builder.author;
        this.lastUpdated = builder.lastUpdated;
        this.pluginCount = builder.pluginCount;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getVersion() {
        return version;
    }

    public String getUpdateCheckUrl() {
        return updateCheckUrl;
    }

    public String getHomepageUrl() {
        return homepageUrl;
    }

    public String getAuthor() {
        return author;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public int getPluginCount() {
        return pluginCount;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name = "";
        private String description = "";
        private String version = "1.0.0";
        private String type = "";
        private String updateCheckUrl = "";
        private String homepageUrl = "";
        private String author = "";
        private long lastUpdated = System.currentTimeMillis();
        private int pluginCount = 0;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder updateCheckUrl(String updateCheckUrl) {
            this.updateCheckUrl = updateCheckUrl;
            return this;
        }

        public Builder homepageUrl(String homepageUrl) {
            this.homepageUrl = homepageUrl;
            return this;
        }

        public Builder author(String author) {
            this.author = author;
            return this;
        }

        public Builder lastUpdated(long lastUpdated) {
            this.lastUpdated = lastUpdated;
            return this;
        }

        public Builder pluginCount(int pluginCount) {
            this.pluginCount = pluginCount;
            return this;
        }

        public RepositoryMetadata build() {
            return new RepositoryMetadata(this);
        }
    }
}
