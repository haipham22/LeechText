package dark.leech.text.lua.api;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

public class Core {

    public static LuaValue decode_gzip(byte[] data) {
        try {
            ByteArrayInputStream byteIn = new ByteArrayInputStream(data);
            GZIPInputStream gzIn = new GZIPInputStream(byteIn);
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();

            int res = 0;
            byte[] buf = new byte[1024];
            while (res >= 0) {
                res = gzIn.read(buf, 0, buf.length);
                if (res > 0) {
                    byteOut.write(buf, 0, res);
                }
            }
            return LuaValue.valueOf(new String(byteOut.toByteArray(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            return LuaValue.valueOf("");
        }
    }

    public static LuaValue new_chapter(Object name, Object url, Object host) {
        LuaTable value = new LuaTable();
        value.set("name", LuaValue.valueOf(name.toString()));
        value.set("url", merge_url(host, url));
        return value;
    }

    public static LuaValue new_chapter(Object name, Object url) {
        return new_chapter(name, url, "");
    }

    public static LuaValue new_search(Object name, Object url, Object cover, Object host) {
        LuaTable value = new LuaTable();
        value.set("name", LuaValue.valueOf(name.toString()));
        value.set("url", merge_url(host, url));
        value.set("cover", merge_url(host, cover));
        return value;
    }

    public static LuaValue new_search(Object name, Object url, Object cover) {
        return new_search(name, cover, url, "");
    }

    public static LuaValue new_search(Object name, Object url) {
        return new_search(name, url);
    }


    public static LuaValue merge_url(Object host, Object url) {
        String newUrl = url.toString().trim();
        if (newUrl.length() == 0) return LuaValue.NIL;
        String hostName = host.toString().trim();
        if (!newUrl.startsWith("http") && !hostName.isEmpty()) {
            if (newUrl.startsWith("/"))
                newUrl = newUrl.substring(1);
            if (!hostName.endsWith("/"))
                hostName = hostName + "/";
            newUrl = hostName + newUrl;
        }
        return LuaValue.valueOf(newUrl);
    }

    public static LuaValue create_login(Object url) {
        return LuaValue.valueOf(0 + "0#" + url);

    }

    public static LuaValue create_confirm(Object url) {
        return LuaValue.valueOf("1#" + url.toString());
    }

    public static LuaValue create_capcha(Object url) {
        return LuaValue.valueOf("2#" + url.toString());
    }

    public static LuaValue create_purchase(Object url) {
        return LuaValue.valueOf("3#" + url.toString());
    }
}
