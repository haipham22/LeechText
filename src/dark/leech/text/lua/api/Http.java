package dark.leech.text.lua.api;

import dark.leech.text.util.SettingUtils;
import dark.leech.text.util.TextUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Http {

    private static final String TAG = "Http";
    private String url;
    private boolean syncCookie = true;
    private boolean skipCookie = false;
    private Connection connection;
    private Connection.Response response;
    private static List<CacheResponse> cache = new ArrayList<>();
    private static List<String> cacheUrl = new ArrayList<>();

    private static CookieManager cookieManager;

    static {
        try {
            cookieManager = CookieManager.getInstance();
        } catch (Throwable ignored) {
        }
    }

    public Http() {
    }

    public Http request(Object url) {
        if (url instanceof String) {
            this.url = (String) url;
        } else this.url = url.toString();
        connection = Jsoup.connect(this.url)
                .header("User-Agent", SettingUtils.USER_AGENT)
                .followRedirects(true)
                .ignoreContentType(true)
                .ignoreHttpErrors(true)
                .timeout(SettingUtils.TIMEOUT)
                .maxBodySize(0);
        return this;
    }

    public Http headers(LuaTable headers) {
        Lua.forEach(headers, new Lua.TableAction() {
            @Override
            public void action(String key, LuaValue value) {
                connection.header(key, value.tojstring());
            }
        });
        return this;
    }

    public Http body(Object body) {
        connection.requestBody(body.toString());
        return this;
    }

    public Http params(LuaTable params) {
        Lua.forEach(params, new Lua.TableAction() {
            @Override
            public void action(String key, LuaValue value) {
                connection.data(key, value.tojstring());
            }
        });
        return this;
    }

    public LuaValue cookies() {
        if (response != null)
            return LuaValue.valueOf(response.header("Set-Cookie"));
        return LuaValue.valueOf("");
    }

    public Http cookies(Object object) {
        skipCookie = true;
        connection.header("Cookie", object.toString());
        return this;
    }

    public Http cookie(Object sync, Object skip) {
        if (sync instanceof Boolean)
            syncCookie = (boolean) sync;
        else if (sync instanceof LuaBoolean)
            syncCookie = ((LuaBoolean) sync).booleanValue();

        if (skip instanceof Boolean)
            skipCookie = (boolean) skip;
        else if (skip instanceof LuaBoolean)
            skipCookie = ((LuaBoolean) skip).booleanValue();
        return this;
    }


    public Http timeout(Object value) {
        try {
            connection.timeout(Integer.parseInt(value.toString()));
        } catch (Exception ignored) {
        }
        return this;
    }


    public Http post() {
        connection.method(Connection.Method.POST);
        return this;
    }

    public Http get() {
        connection.method(Connection.Method.GET);
        return this;
    }

    public LuaValue html() {
        try {
            return CoerceJavaToLua.coerce(call().parse());
        } catch (Exception e) {
            e.printStackTrace();
            return LuaValue.NIL;
        }
    }

    private Connection.Response call() {
        int index = cacheUrl.indexOf(this.url);
        if (index != -1)
            return cache.get(index);
        try {
            if (!skipCookie &&
                    cookieManager != null) {
                String cookie = cookieManager.getCookie(url);
                if (cookie != null) {
                    connection.header("Cookie", cookie);
                }
            }
            response = connection.execute();

            if (syncCookie) {
                String cookie = response.header("Set-Cookie");
                if (cookieManager != null && !TextUtils.isEmpty(cookie))
                    cookieManager.setCookie(url, cookie);
            }
            synchronized (Http.class) {
                if (cache.size() > 5) {
                    cacheUrl.remove(0);
                    cache.remove(0);
                }
                if (response.statusCode() == 200 && response.method() == Connection.Method.GET) {
                    cacheUrl.add(url);
                    cache.add(new CacheResponse(response));
                }
            }
            return response;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public LuaValue string() {
        try {
            String htm = call().body();
            if (!TextUtils.isEmpty(htm)) {
                if (htm.charAt(0) == '\uFEFF')
                    htm = htm.substring(1);
            } else {
                return LuaValue.NIL;
            }
            return LuaValue.valueOf(htm);
        } catch (Exception e) {
            e.printStackTrace();
            return LuaValue.NIL;
        }

    }

    public LuaValue table() {
        try {
            return Json.to_table(string());
        } catch (Exception e) {
            return LuaValue.NIL;
        }
    }

    public byte[] raw() {
        try {
            return call().bodyAsBytes();
        } catch (Exception e) {
            return null;
        }
    }


}
