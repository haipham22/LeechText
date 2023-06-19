package dark.leech.text.util;

import dark.leech.text.models.Trash;
import dark.leech.text.ui.notification.Toast;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Long on 10/3/2016.
 */
public class SettingUtils {
    private SettingUtils() {
    }

    //Kết nối
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

    //Other
    public static List<Trash> TRASH;
    public static String WORKPATH;
    public static String KINDLEGEN;
    public static String CALIBRE;
    public static Color THEME_COLOR;

    public static void doLoad() {
        doDefault();
        String json = FileUtils.file2string(AppUtils.curDir + "/tools/setting.json");
        if (json != null)
            new SettingUtils().doLoad(new JSONObject(json), false);
    }

    private void doLoad(JSONObject object, boolean IS_DEFAULT) {
        JSONObject connection = object.getJSONObject("connection");
        MAX_CONN = connection.getInt("num_conn");
        RE_CONN = connection.getInt("re_conn");
        DELAY = connection.getInt("delay");
        TIMEOUT = connection.getInt("time_out");
        USER_AGENT = connection.getString("user_agent");

        JSONObject style = object.getJSONObject("style");
        IS_CSS_SELECTED = style.getJSONObject("css").getBoolean("checked");
        if (IS_CSS_SELECTED || IS_DEFAULT)
            CSS_SYNTAX = style.getJSONObject("css").getString("value");
        IS_HTML_SELECTED = style.getJSONObject("html").getBoolean("checked");
        if (IS_HTML_SELECTED || IS_DEFAULT)
            HTML_SYNTAX = style.getJSONObject("html").getString("value");
        IS_TXT_SELECTED = style.getJSONObject("txt").getBoolean("checked");
        if (IS_TXT_SELECTED || IS_DEFAULT)
            TXT_SYNTAX = style.getJSONObject("txt").getString("value");
        IS_DROP_SELECTED = style.getJSONObject("dropcaps").getBoolean("checked");
        if (IS_DROP_SELECTED || IS_DEFAULT)
            DROP_SYNTAX = style.getJSONObject("dropcaps").getString("value");
        //
        JSONObject other = object.getJSONObject("other");
        WORKPATH = other.getString("workspace");
        if (WORKPATH.length() == 0) WORKPATH = AppUtils.curDir;
        CALIBRE = other.getString("calibre");
        KINDLEGEN = other.getString("kindlegen");
        List<Trash> trash = new ArrayList<>();
        JSONArray trashArr = other.getJSONArray("trash");
        for (int i = 0; i < trashArr.length(); i++) {
            JSONObject obj = trashArr.getJSONObject(i);
            trash.add(new Trash(obj.getString("src"), obj.getString("to"), obj.getString("tip"), obj.getBoolean("replace")));
        }
        TRASH = trash;
        String color = other.getString("theme_color");
        if (color != null)
            if (color.length() > 0)
                THEME_COLOR = Color.decode(color);
    }

    public static void doSave() {
        JSONObject stt = new JSONObject();
        JSONObject connection = new JSONObject();
        connection.put("num_conn", MAX_CONN);
        connection.put("re_conn", RE_CONN);
        connection.put("time_out", TIMEOUT);
        connection.put("delay", DELAY);
        connection.put("user_agent", USER_AGENT);
        stt.put("connection", connection);

        JSONObject style = new JSONObject();
        style.put("dropcaps", getObject(IS_DROP_SELECTED, DROP_SYNTAX));
        style.put("html", getObject(IS_HTML_SELECTED, HTML_SYNTAX));
        style.put("txt", getObject(IS_TXT_SELECTED, TXT_SYNTAX));
        style.put("css", getObject(IS_CSS_SELECTED, CSS_SYNTAX));
        stt.put("style", style);

        JSONObject other = new JSONObject();
        other.put("workspace", WORKPATH);
        other.put("calibre", CALIBRE);
        other.put("kindlegen", KINDLEGEN);
        other.put("theme_color", getHexColor(THEME_COLOR));
        other.put("trash", new JSONArray(TRASH));
        stt.put("other", other);

        FileUtils.string2file(stt.toString(), AppUtils.curDir + "/tools/setting.json");
        Toast.Build()
                .font(FontUtils.TITLE_NORMAL)
                .content("Đã lưu cài đặt!")
                .open();
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
        new SettingUtils().doLoad(new JSONObject(FileUtils.stream2string("/dark/leech/res/setting.json")), true);
    }

    private static JSONObject getObject(boolean b, String value) {
        JSONObject object = new JSONObject();
        object.put("checked", Boolean.valueOf(b));
        object.put("value", value);
        return object;
    }
}
