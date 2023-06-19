package dark.leech.text.util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;

/**
 * Created by Long on 1/5/2017.
 */
public class AppUtils {
    public static final String VERSION = "2019.07.22";
    public static final String TIME = "00:00";
    public static String curDir = System.getProperty("user.dir");
    public static String cacheDir = curDir;
    public static final String SEPARATOR = System.getProperty("file.separator");
    public static Point LOCATION = new Point();
    private static final GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    public static final int width = gd.getDisplayMode().getWidth();
    public static final int height = gd.getDisplayMode().getHeight();


    public static void doLoad() {
        try {
            if (curDir.endsWith(SEPARATOR)) curDir = curDir.substring(0, curDir.length() - 1);
            JSONObject json = new JSONObject(FileUtils.stream2string("/dark/leech/res/syntax.json"));
            JSONObject find = json.getJSONObject("find");
            SyntaxUtils.CHAP_NAME = find.getString("chap");
            SyntaxUtils.PART_NAME = find.getString("part");
            JSONArray optimize = json.getJSONArray("optimize");
            String[] REPLACE_FROM = new String[optimize.length()];
            String[] REPLACE_TO = new String[optimize.length()];
            for (int i = 0; i < optimize.length(); i++) {
                REPLACE_FROM[i] = optimize.getJSONObject(i).getString("replace");
                REPLACE_TO[i] = optimize.getJSONObject(i).getString("to");
            }
            SyntaxUtils.REPLACE_FROM = REPLACE_FROM;
            SyntaxUtils.REPLACE_TO = REPLACE_TO;
        } catch (Exception ignore) {
        }
    }

    public static int getX() {
        return LOCATION.x;
    }

    public static int getY() {
        return LOCATION.y;
    }

    public static Point getLocation() {
        return LOCATION;
    }

    public static void pause(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException ignore) {
        }
    }


}
