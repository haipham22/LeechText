package dark.leech.text.util;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import dark.leech.text.models.Settings;
import dark.leech.text.models.Trash;
import dark.leech.text.ui.notification.Toast;

/** Created by Long on 10/3/2016. */
public class SettingUtils {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static Settings settings;

    // Kết nối
    public static int MAX_CONN;
    public static int TIMEOUT;
    public static int RE_CONN;
    public static int DELAY;
    public static String USER_AGENT;
    public static boolean IS_DROP_SELECTED;
    public static boolean IS_HTML_SELECTED;
    public static boolean IS_TXT_SELECTED;
    public static boolean IS_CSS_SELECTED;
    public static String DROP_SYNTAX;
    public static String HTML_SYNTAX;
    public static String TXT_SYNTAX;
    public static String CSS_SYNTAX;
    // Other
    public static List<Trash> TRASH;
    public static String WORKPATH;
    public static String KINDLEGEN;
    public static String CALIBRE;
    public static Color THEME_COLOR;

    private SettingUtils() {}

    public static void doLoad() {
        doDefault();
        String json = FileUtils.file2string(AppUtils.curDir + "/tools/setting.json");
        if (json != null) {
            try {
                Settings settings = gson.fromJson(json, Settings.class);
                new SettingUtils().loadFromSettings(settings, false);
            } catch (Exception e) {
                // Fallback to default settings if JSON parsing fails
                doDefault();
            }
        }
    }

    public static void doSave() {
        Settings settings = new Settings();

        // Connection settings
        Settings.ConnectionSettings connection = new Settings.ConnectionSettings();
        connection.setNumConn(MAX_CONN);
        connection.setReConn(RE_CONN);
        connection.setTimeOut(TIMEOUT);
        connection.setDelay(DELAY);
        connection.setUserAgent(USER_AGENT);
        settings.setConnection(connection);

        // Style settings
        Settings.StyleSettings style = new Settings.StyleSettings();
        style.setDropcaps(createStyleItem(IS_DROP_SELECTED, DROP_SYNTAX));
        style.setHtml(createStyleItem(IS_HTML_SELECTED, HTML_SYNTAX));
        style.setTxt(createStyleItem(IS_TXT_SELECTED, TXT_SYNTAX));
        style.setCss(createStyleItem(IS_CSS_SELECTED, CSS_SYNTAX));
        settings.setStyle(style);

        // Other settings
        Settings.OtherSettings other = new Settings.OtherSettings();
        other.setWorkspace(WORKPATH);
        other.setCalibre(CALIBRE);
        other.setKindlegen(KINDLEGEN);
        other.setThemeColor(getHexColor(THEME_COLOR));
        other.setTrash(TRASH);
        settings.setOther(other);

        String json = gson.toJson(settings);
        FileUtils.string2file(json, AppUtils.curDir + "/tools/setting.json");
        Toast.Build().font(FontUtils.TITLE_NORMAL).content("Đã lưu cài đặt!").open();
    }

    private static String getHexColor(Color color) {
        String hex = Integer.toHexString(color.getRGB() & 0xffffff);
        if (hex.length() < 6) {
            hex = "0" + hex;
        }
        hex = "#" + hex;
        return hex;
    }

    public static void doDefault() {
        try {
            String defaultJson = FileUtils.stream2string("/dark/leech/res/setting.json");
            settings = gson.fromJson(defaultJson, Settings.class);
            new SettingUtils().loadFromSettings(settings, true);
        } catch (Exception e) {
            // Fallback to hardcoded defaults if resource loading fails
            setHardcodedDefaults();
        }
    }

    private static Settings.StyleItem createStyleItem(boolean checked, String value) {
        Settings.StyleItem item = new Settings.StyleItem();
        item.setChecked(checked);
        item.setValue(value);
        return item;
    }

    private static void setHardcodedDefaults() {
        // Connection defaults
        MAX_CONN = 5;
        RE_CONN = 3;
        DELAY = 10;
        TIMEOUT = 30000;
        USER_AGENT =
                "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15"
                        + " (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1";

        // Style defaults
        IS_CSS_SELECTED = false;
        IS_HTML_SELECTED = false;
        IS_TXT_SELECTED = false;
        IS_DROP_SELECTED = false;
        CSS_SYNTAX = "";
        HTML_SYNTAX = "";
        TXT_SYNTAX = "";
        DROP_SYNTAX = "";

        // Other defaults
        WORKPATH = AppUtils.curDir;
        CALIBRE = autoDetectCalibre();
        KINDLEGEN = autoDetectKindleGen();
        THEME_COLOR = Color.decode("#263238");
        TRASH = new ArrayList<>();
    }

    /** Auto-detect Calibre ebook-convert executable path based on OS */
    private static String autoDetectCalibre() {
        String os = System.getProperty("os.name").toLowerCase();
        String[] paths;

        if (os.contains("win")) {
            paths =
                    new String[] {
                        "C:\\Program Files\\Calibre2\\ebook-convert.exe",
                        "C:\\Program Files\\Calibre\\ebook-convert.exe",
                        "C:\\Calibre\\ebook-convert.exe"
                    };
        } else if (os.contains("mac")) {
            paths =
                    new String[] {
                        "/opt/homebrew/bin/ebook-convert",
                        "/usr/local/bin/ebook-convert",
                        "/Applications/calibre.app/Contents/MacOS/ebook-convert"
                    };
        } else {
            paths =
                    new String[] {
                        "/usr/bin/ebook-convert",
                        "/usr/local/bin/ebook-convert",
                        "/opt/calibre/ebook-convert"
                    };
        }

        for (String path : paths) {
            if (new File(path).exists()) {
                return path;
            }
        }
        return "";
    }

    /** Auto-detect KindleGen executable path based on OS */
    private static String autoDetectKindleGen() {
        String os = System.getProperty("os.name").toLowerCase();
        String[] paths;

        if (os.contains("win")) {
            paths =
                    new String[] {
                        "C:\\Program Files\\Amazon\\KindleGen\\kindleGen.exe",
                        "C:\\KindleGen\\kindleGen.exe"
                    };
        } else if (os.contains("mac")) {
            paths = new String[] {"/usr/local/bin/kindlegen", "/Applications/KindleGen/kindlegen"};
        } else {
            paths = new String[] {"/usr/bin/kindlegen", "/usr/local/bin/kindlegen"};
        }

        for (String path : paths) {
            if (new File(path).exists()) {
                return path;
            }
        }
        return "";
    }

    public Settings getSettings() {
        return settings;
    }

    private void loadFromSettings(Settings settings, boolean isDefault) {
        if (settings == null) {
            setHardcodedDefaults();
            return;
        }

        // Load connection settings
        if (settings.getConnection() != null) {
            Settings.ConnectionSettings connection = settings.getConnection();
            MAX_CONN = connection.getNumConn();
            RE_CONN = connection.getReConn();
            DELAY = connection.getDelay();
            TIMEOUT = connection.getTimeOut();
            USER_AGENT = connection.getUserAgent();
        }

        // Load style settings
        if (settings.getStyle() != null) {
            Settings.StyleSettings style = settings.getStyle();

            if (style.getCss() != null) {
                IS_CSS_SELECTED = style.getCss().isChecked();
                if (IS_CSS_SELECTED || isDefault) {
                    CSS_SYNTAX = style.getCss().getValue();
                }
            }

            if (style.getHtml() != null) {
                IS_HTML_SELECTED = style.getHtml().isChecked();
                if (IS_HTML_SELECTED || isDefault) {
                    HTML_SYNTAX = style.getHtml().getValue();
                }
            }

            if (style.getTxt() != null) {
                IS_TXT_SELECTED = style.getTxt().isChecked();
                if (IS_TXT_SELECTED || isDefault) {
                    TXT_SYNTAX = style.getTxt().getValue();
                }
            }

            if (style.getDropcaps() != null) {
                IS_DROP_SELECTED = style.getDropcaps().isChecked();
                if (IS_DROP_SELECTED || isDefault) {
                    DROP_SYNTAX = style.getDropcaps().getValue();
                }
            }
        }

        // Load other settings
        if (settings.getOther() != null) {
            Settings.OtherSettings other = settings.getOther();
            WORKPATH = other.getWorkspace();
            if (WORKPATH == null || WORKPATH.length() == 0) {
                WORKPATH = AppUtils.curDir;
            }
            CALIBRE = other.getCalibre();
            KINDLEGEN = other.getKindlegen();

            List<Trash> trash = other.getTrash();
            TRASH = trash != null ? trash : new ArrayList<>();

            String color = other.getThemeColor();
            if (color != null && color.length() > 0) {
                try {
                    THEME_COLOR = Color.decode(color);
                } catch (NumberFormatException e) {
                    THEME_COLOR = Color.decode("#263238");
                }
            }
        }
    }
}
