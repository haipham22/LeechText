package dark.leech.text.lua.api;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;


public class Html {

    public LuaValue remove(Object input, LuaTable tags) {
        LuaValue[] keys = tags.keys();
        if (input instanceof Element) {
            for (LuaValue key : keys) {
                ((Element) input).select(tags.get(key).tojstring()).remove();
            }
        } else if (input instanceof Elements) {
            for (LuaValue key : keys) {
                ((Elements) input).select(tags.get(key).tojstring()).remove();
            }
        }
        return CoerceJavaToLua.coerce(input);
    }

    public LuaValue url_encode(Object url, Object charset) {
        try {
            return LuaValue.valueOf(URLEncoder.encode(url.toString(), charset.toString()));
        } catch (UnsupportedEncodingException e) {
            return LuaValue.NIL;
        }
    }


    public LuaValue parse(Object object) {
        return CoerceJavaToLua.coerce(Jsoup.parse(object.toString()));
    }

    public LuaValue url_encode(Object url) {
        return url_encode(url, "UTF-8");
    }

}
