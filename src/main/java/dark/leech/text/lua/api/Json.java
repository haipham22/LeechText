package dark.leech.text.lua.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import java.util.Iterator;

public class Json {

    public static LuaValue to_table(Object object){
        if(object instanceof JSONObject)
            return json2table((JSONObject) object);
        else  if(object instanceof  JSONArray)
            return array2table((JSONArray) object);
        else if(object instanceof String)
            return string2table((String) object);
        else return string2table(object.toString());
    }

    private static LuaValue json2table(JSONObject obj) {
        LuaValue result = LuaValue.NIL;
        if (obj != null) {
            result = new LuaTable();
            if (obj.length() > 0) {
                Iterator<String> iter = obj.keys();
                while (iter.hasNext()) {
                    final String key = iter.next();
                    final Object value = obj.opt(key);
                    result.set(key, value(value));
                }
            }
        }
        return result;
    }

    private static LuaValue string2table(String jsonString) {
        LuaValue luaTable = LuaValue.NIL;
        try {
            luaTable = json2table(new JSONObject(jsonString));
        } catch (Exception e) {
            try {
                luaTable = array2table(new JSONArray(jsonString));
            } catch (JSONException ignored) {
            }
        }
        return luaTable;
    }

    private static LuaValue array2table(JSONArray obj) {
        LuaValue result = LuaValue.NIL;

        if (obj != null) {
            result = new LuaTable();//只要不空，就创建一个table
            if (obj.length() > 0) {
                for (int i = 0; i < obj.length(); i++) {
                    final int key = i + 1;
                    final Object value = obj.opt(i);
                    result.set(key, value(value));
                }
            }
        }
        return result;
    }

    /**
     * convert a object to LuaValue
     *
     * @param value
     * @return
     */
    private static LuaValue value(Object value) {
        if (value instanceof String) {
            return LuaValue.valueOf((String) value);
        } else if (value instanceof Integer) {
            return LuaValue.valueOf((Integer) value);
        } else if (value instanceof Long) {
            return LuaValue.valueOf((Long) value);
        } else if (value instanceof Double) {
            return LuaValue.valueOf((Double) value);
        } else if (value instanceof Boolean) {
            return LuaValue.valueOf((Boolean) value);
        } else if (value instanceof JSONObject) {
            return json2table((JSONObject) value);
        } else if (value instanceof JSONArray) {
            return array2table((JSONArray) value);
        } else {
            return CoerceJavaToLua.coerce(value);
        }
    }
}
