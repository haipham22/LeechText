package dark.leech.text.lua.loader;

import dark.leech.text.enities.PluginEntity;
import dark.leech.text.lua.api.LuaScriptEngine;
import dark.leech.text.util.TextUtils;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;

public class TextLoader {
    private PluginEntity plugin;

    public static TextLoader with(PluginEntity plugin) {
        return new TextLoader(plugin);
    }

    private TextLoader(PluginEntity plugin) {
        this.plugin = plugin;
    }

    public String load(String url) {
        if (!TextUtils.isEmpty(plugin.getChapGetter())) {
            Globals globals = LuaScriptEngine.getInstance().getGlobals();
            LuaValue chuck;
            try {
                chuck = globals.load(plugin.getChapGetter());
            } catch (LuaError error) {
                error.printStackTrace();
                return null;
            }
            try {
                LuaValue result = chuck.call(LuaValue.valueOf(url));
                if (result != null && !result.isnil()) {
                    if (result instanceof org.luaj.vm2.LuaTable) {
                        return result.get("content").tojstring();
                    }
                    return result.tojstring();
                }
            } catch (LuaError error) {
                error.printStackTrace();
                return null;
            }
        }
        return null;
    }
}
