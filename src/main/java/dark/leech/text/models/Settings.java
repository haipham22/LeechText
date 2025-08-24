package dark.leech.text.models;

import java.util.List;

import com.google.gson.annotations.SerializedName;

import lombok.Data;
import lombok.NoArgsConstructor;

import dark.leech.text.enities.RepositoryEntity;

/**
 * Settings data model for JSON serialization with Gson. Represents the complete settings structure
 * for the application.
 */
@Data
@NoArgsConstructor
public class Settings {
    @SerializedName("connection")
    private ConnectionSettings connection;

    @SerializedName("style")
    private StyleSettings style;

    @SerializedName("other")
    private OtherSettings other;

    @SerializedName("repositories")
    private RepositoriesSettings repositories;

    @Data
    @NoArgsConstructor
    public static class ConnectionSettings {
        @SerializedName("num_conn")
        private int numConn = 5;

        @SerializedName("re_conn")
        private int reConn = 3;

        @SerializedName("delay")
        private int delay = 10;

        @SerializedName("time_out")
        private int timeOut = 30000;

        @SerializedName("user_agent")
        private String userAgent =
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko)"
                        + " Chrome/54.0.2840.71 Safari/537.36";
    }

    @Data
    @NoArgsConstructor
    public static class StyleSettings {
        @SerializedName("dropcaps")
        private StyleItem dropcaps;

        @SerializedName("html")
        private StyleItem html;

        @SerializedName("txt")
        private StyleItem txt;

        @SerializedName("css")
        private StyleItem css;
    }

    @Data
    @NoArgsConstructor
    public static class StyleItem {
        @SerializedName("checked")
        private boolean checked = false;

        @SerializedName("value")
        private String value = "";
    }

    @Data
    @NoArgsConstructor
    public static class OtherSettings {
        @SerializedName("workspace")
        private String workspace = "";

        @SerializedName("calibre")
        private String calibre = "";

        @SerializedName("kindlegen")
        private String kindlegen = "";

        @SerializedName("theme_color")
        private String themeColor = "#263238";

        @SerializedName("trash")
        private List<Trash> trash;
    }

    @Data
    @NoArgsConstructor
    public static class RepositoriesSettings {
        private List<RepositoryEntity> repositories;
    }
}
