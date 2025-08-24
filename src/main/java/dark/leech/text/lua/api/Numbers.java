package dark.leech.text.lua.api;

public class Numbers {
    public static int toInt(String text, int def) {
        try {
            return Integer.parseInt(text);
        } catch (Exception e) {
            return def;
        }
    }

    public static double toDouble(String text, double def) {
        try {
            return Double.parseDouble(text);
        } catch (Exception e) {
            return def;
        }
    }

    public static float toFloat(String text, float def) {
        try {
            return Float.parseFloat(text);
        } catch (Exception e) {
            return def;
        }
    }

    public static long toLong(String text, long def) {
        try {
            return Long.parseLong(text);
        } catch (Exception e) {
            return def;
        }
    }
}
