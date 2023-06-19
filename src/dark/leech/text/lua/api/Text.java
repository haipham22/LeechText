package dark.leech.text.lua.api;

import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaNumber;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

public class Text {
    public static LuaValue index_of(Object str, Object s) {
        return index_of(str, s, Integer.valueOf(0));
    }

    public static LuaValue last_index_of(Object str, Object s) {
        return last_index_of(str, s, 0);
    }

    public static LuaValue index_of(Object str, Object s, Object start) {
        if (str == null) return LuaValue.NIL;
        int index = 0;
        if (start instanceof Number) {
            index = ((Number) start).intValue();
        } else if (start instanceof LuaNumber) {
            index = ((LuaNumber) start).toint();
        } else {
            try {
                index = str.toString().indexOf(start.toString());
            } catch (Exception exception) {
            }
        }

        return LuaValue.valueOf(str.toString().indexOf(s.toString(), index));
    }

    public static LuaValue last_index_of(Object str, Object s, Object start) {
        if (str == null) return LuaValue.NIL;
        int index = 0;
        if (start instanceof Number) {
            index = ((Number) start).intValue();
        } else if (start instanceof LuaNumber) {
            index = ((LuaNumber) start).toint();
        } else {
            try {
                index = str.toString().indexOf(start.toString());
            } catch (Exception ignored) {
            }
        }
        return LuaValue.valueOf(str.toString().lastIndexOf(s.toString(), index));
    }

    public static LuaValue sub(Object str, Object s, Object e) {
        String source = str.toString();
        int start = Num.to_int(s, 0).toint();
        int end = Num.to_int(e, 0).toint();
        if (!is_empty(source)) {
            if (start != -1 && end != -1 && start < end && end <= source.length()) {
                return LuaValue.valueOf(source.substring(start, end));
            }
        }
        return LuaValue.NIL;
    }

    public static LuaValue sub_first(Object str, Object i) {
        String source = str.toString();
        int index = Num.to_int(i, 0).toint();
        if (!is_empty(source)) {
            if (index != -1 && index < source.length()) {
                return LuaValue.valueOf(source.substring(0, index));
            }
        }
        return LuaValue.NIL;
    }

    public static LuaValue sub_last(Object str, Object i) {
        String source = str.toString();
        int index = Num.to_int(i, 0).toint();
        if (!is_empty(source)) {
            if (index != -1 && index < source.length()) {
                return LuaValue.valueOf(source.substring(index, source.length()));
            }
        }
        return LuaValue.NIL;
    }


    public static LuaValue sub_between(Object str, Object och1, Object och2) {
        String source = str.toString();
        String ch1 = och1.toString();
        String ch2 = och2.toString();
        if (!is_empty(source)) {
            int index1 = source.indexOf(ch1);
            if (index1 != -1) {
                index1 += ch1.length();
                int index2 = source.indexOf(ch2, index1);
                if (index2 > index1) {
                    return LuaValue.valueOf(source.substring(index1, index2));
                }
            }
        }
        return LuaValue.NIL;
    }

    public static LuaValue split(Object text, Object regex) {
        if (is_empty(text)) return LuaValue.NIL;
        LuaTable table = new LuaTable();
        String[] spl = text.toString().split(regex.toString());
        for (int i = 0; i < spl.length; i++) {
            table.set(i + 1, LuaValue.valueOf(spl[i]));
        }

        if (table.length() == 0) return LuaValue.NIL;
        return table;
    }

    public static LuaValue trim(Object source) {
        return LuaValue.valueOf(source.toString().trim());
    }

    public static boolean is_empty(Object source) {
        if(source == null) return true;
        if (source instanceof LuaValue) {
            if(((LuaValue) source).isnil()) return true;
        }
        return source.toString().length() == 0;
    }

    public static LuaValue contains(Object obj, Object value) {
        return LuaValue.valueOf(obj.toString().contains(value.toString()));
    }

    public static LuaValue replace(Object str, Object regex, Object e) {
        if (str != null)
            return LuaValue.valueOf(str.toString().replaceAll(regex.toString(), e.toString()));
        return LuaValue.NIL;
    }

    public static LuaValue remove(Object str, Object replace) {
        return remove(str, replace, Boolean.TRUE);
    }

    public static LuaValue remove(Object str, Object replace, Object regex) {
        String text = str.toString();
        boolean usingRegex = true;
        if (regex instanceof Boolean) {
            usingRegex = ((Boolean) regex).booleanValue();
        } else if (regex instanceof LuaBoolean) {
            usingRegex = ((LuaBoolean) regex).booleanValue();
        }

        if (replace instanceof LuaTable) {
            LuaTable table = (LuaTable) replace;
            LuaValue[] keys = table.keys();
            for (LuaValue value : keys) {
                if (usingRegex) {
                    text = text.replaceAll(table.get(value).tojstring(), "");
                } else {
                    text = text.replace(table.get(value).tojstring(), "");
                }

            }
        } else if (usingRegex) {
            text = text.replaceAll(replace.toString(), "");
        } else {
            text = text.replace(replace.toString(), "");
        }
        return LuaValue.valueOf(text);
    }

    public static LuaValue json_table(Object object) {
        if (object instanceof LuaTable) {
            return (LuaValue) object;
        }
        return Json.to_table(object);
    }
}



