package dark.leech.text.lua.loader;

import dark.leech.text.enities.BookEntity;
import dark.leech.text.enities.PluginEntity;
import dark.leech.text.lua.api.LuaScriptEngine;
import dark.leech.text.util.TextUtils;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;

public class DetailLoader {

    private PluginEntity plugin;

    public static DetailLoader with(PluginEntity plugin) {
        return new DetailLoader(plugin);
    }

    private DetailLoader(PluginEntity plugin) {
        this.plugin = plugin;
    }

    public BookEntity load(String url) {
        if (!TextUtils.isEmpty(plugin.getDetailGetter())) {
            BookEntity entity = new BookEntity();
            Globals globals = LuaScriptEngine.getInstance().getGlobals();
            LuaValue chuck;
            try {
                chuck = globals.load(plugin.getDetailGetter());
            } catch (LuaError e) {
                e.printStackTrace();
                return null;
            }
            LuaValue value;
            try {
                value = chuck.call(LuaValue.valueOf(url));
            } catch (LuaError error) {
                error.printStackTrace();
                return null;
            }
            if (value == null || value.isnil()) return null;
            if (!value.get("name").isnil())
                entity.setName(value.get("name").tojstring());
            if (!value.get("url").isnil() && !TextUtils.isEmpty(value.get("url").tojstring())) {
                entity.setUrl(value.get("url").tojstring());
            } else {
                entity.setUrl(url);
            }
            if (!value.get("detail").isnil()) {
                String html = value.get("detail").tojstring();
                entity.setDetail(html.trim().replaceAll("\n+", "\n"));
            }
            if (!value.get("description").isnil())
                entity.setIntroduce(value.get("description").tojstring());
            if (!value.get("cover").isnil())
                entity.setCover(value.get("cover").tojstring());
            if (!value.get("author").isnil())
                entity.setAuthor(value.get("author").tojstring());
            entity.setWebSource(this.plugin.getName());
            return entity;
        }
        return null;
    }

}

