package dark.leech.text.lua.api;

import org.luaj.vm2.Globals;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.JsePlatform;

public class LuaScriptEngine {

    private static LuaScriptEngine engine;
    private Globals globals;
    private static final Object mLock = new Object();

    public static LuaScriptEngine getInstance() {
//        LuaScriptEngine scriptEngine = engine;
//        if (scriptEngine == null) {
//            synchronized (mLock) {
//                scriptEngine = engine;
//                if (scriptEngine == null) {
//                    engine = scriptEngine = new LuaScriptEngine();
//                }
//            }
//        }
        return new LuaScriptEngine();
    }

    private LuaScriptEngine() {
        globals = JsePlatform.standardGlobals();
        globals.set("http", CoerceJavaToLua.coerce(new Http()));
        globals.set("regexp", CoerceJavaToLua.coerce(new Regexp()));
        globals.set("html", CoerceJavaToLua.coerce(new Html()));
        globals.set("core", CoerceJavaToLua.coerce(new Core()));
        globals.set("num", CoerceJavaToLua.coerce(new Num()));
        globals.set("text", CoerceJavaToLua.coerce(new Text()));
    }

    public Globals getGlobals() {
        return globals;
    }
}
