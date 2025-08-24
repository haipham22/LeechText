package dark.leech.text.lua.api;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Regexp {
    public LuaValue find(Object str, Object regex) {
        return find(str, regex, 1);
    }

    public LuaValue find(Object source, Object regexp, Object group) {
        String str = source.toString();
        String regex = regexp.toString();
        int groupInt = 1;
        try {
            if (group instanceof Integer)
                groupInt = (int) group;
            else
                groupInt = Integer.parseInt(group.toString());
        } catch (Exception ignored) {
        }
        try {
            Pattern r = Pattern.compile(regex, Pattern.MULTILINE);
            Matcher m = r.matcher(str);
            if (m.find()) {
                return CoerceJavaToLua.coerce(m.group(groupInt));
            } else {
                return CoerceJavaToLua.coerce("");
            }
        } catch (Exception e) {
            return CoerceJavaToLua.coerce("");
        }
    }

    public LuaValue find_last(Object source, Object regexp) {
        return find_last(source, regexp, 1);
    }

    public LuaValue find_last(Object source, Object regexp, Object group) {
        String str = source.toString();
        String regex = regexp.toString();
        int groupInt = 1;
        try {
            if (group instanceof Integer)
                groupInt = (int) group;
            else
                groupInt = Integer.parseInt(group.toString());
        } catch (Exception ignored) {
        }
        try {
            Pattern r = Pattern.compile(regex, Pattern.MULTILINE);
            Matcher m = r.matcher(str);
            String result = null;
            while (m.find()) {
                result = m.group(groupInt);
            }
            return CoerceJavaToLua.coerce(result);
        } catch (Exception e) {
            return CoerceJavaToLua.coerce("");
        }
    }

    public LuaValue find_all(Object source, Object regexp, Object group) {
        String str = source.toString();
        String regex = regexp.toString();
        int groupInt = 1;
        try {
            if (group instanceof Integer)
                groupInt = (int) group;
            else
                groupInt = Integer.parseInt(group.toString());
        } catch (Exception ignored) {
        }

        LuaTable table = new LuaTable();

        try {
            Pattern r = Pattern.compile(regex, Pattern.MULTILINE);
            Matcher m = r.matcher(str);
            while (m.find()) {
                table.add(LuaValue.valueOf(m.group(groupInt)));
            }
            return table;
        } catch (Exception e) {
            return LuaValue.NIL;
        }
    }

}
