package dark.leech.text.lua.api;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

public class Num {
    public static LuaValue to_int(Object text, Object def) {
        try {
            return LuaValue.valueOf(Integer.parseInt(text.toString()));
        } catch (Exception e) {
            return CoerceJavaToLua.coerce(def);
        }
    }

    public static LuaValue to_double(Object text, Object def) {
        try {
            return LuaValue.valueOf(Double.parseDouble(text.toString()));
        } catch (Exception e) {
            return CoerceJavaToLua.coerce(def);
        }
    }

    public static LuaValue to_float(Object text, Object def) {
        try {
            return LuaValue.valueOf(Float.parseFloat(text.toString()));
        } catch (Exception e) {
            return CoerceJavaToLua.coerce(def);
        }
    }

    public static LuaValue to_long(Object text, Object def) {
        try {
            return LuaValue.valueOf(Long.parseLong(text.toString()));
        } catch (Exception e) {
            return CoerceJavaToLua.coerce(def);
        }
    }
}
