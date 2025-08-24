package dark.leech.text.util;

import java.util.HashMap;
import java.util.Map;

public class CookiesUtils {
    private static Map<String, String> COOKIES = new HashMap<>();


    public static String getCookies(String url) {
        return COOKIES.get(parseKey(url));
    }

    public static void put(String url, String cookies) {
        COOKIES.put(parseKey(url), cookies);
    }

    public static void remove(String url) {
        COOKIES.remove(parseKey(url));
    }

    private static String parseKey(String url) {
        return RegexUtils.find(url, "https{0,1}://(.*?)/", 1);
    }
}
