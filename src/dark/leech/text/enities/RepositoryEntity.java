package dark.leech.text.enities;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class RepositoryEntity {
    @SerializedName("link")
    private String path;
    @SerializedName("author")
    private String author;
    @SerializedName("description")
    private String description;
    private List<PluginEntity> plugins;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<PluginEntity> getPlugins() {
        return plugins;
    }

    public void setPlugins(List<PluginEntity> plugins) {
        this.plugins = plugins;
    }
}
