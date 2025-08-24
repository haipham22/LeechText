package dark.leech.text.lua.api;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

public class Lua {

    public static void forEach(LuaTable table, TableAction action) {
        LuaValue[] keys = table.keys();
        for (LuaValue key : keys) {
            action.action(key.tojstring(), table.get(key));
        }
    }

    public interface TableAction {
        void action(String key, LuaValue value);
    }
}
