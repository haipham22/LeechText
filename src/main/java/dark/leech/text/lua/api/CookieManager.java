package dark.leech.text.lua.api;

import dark.leech.text.util.CookiesUtils;

public class CookieManager {
    public static CookieManager getInstance() {
        return new CookieManager();
    }

    public void setCookie(String url, String cookie) {
        CookiesUtils.put(url, cookie);
    }

    public String getCookie(String url) {
        return CookiesUtils.getCookies(url);
    }
}
