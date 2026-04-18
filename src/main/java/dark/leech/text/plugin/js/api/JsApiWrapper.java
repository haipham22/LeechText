package dark.leech.text.plugin.js.api;

import java.util.HashMap;
import java.util.Map;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;

/**
 * Base class for JavaScript API wrappers using Rhino. Provides type conversion between Java and
 * JavaScript with proper null/undefined handling.
 */
public abstract class JsApiWrapper {

    private static final Object LOCK = new Object();
    private static JsScriptEngine engineInstance;

    protected final Context executionContext;
    protected final Scriptable scope;

    /**
     * Create wrapper with execution context for proper object creation (vBook compatibility).
     *
     * @param context The Rhino Context for script execution
     * @param scope The Rhino Scriptable scope
     */
    protected JsApiWrapper(Context context, Scriptable scope) {
        this.executionContext = context;
        this.scope = scope;
    }

    /** Create wrapper without context (legacy compatibility - initializes new context). */
    protected JsApiWrapper() {
        this.executionContext = Context.enter();
        this.scope = this.executionContext.initStandardObjects();
    }

    /** Get the Context for script execution. */
    protected Context getContext() {
        if (executionContext != null) {
            return executionContext;
        }
        return Context.enter();
    }

    /** Get the Scope. */
    protected Scriptable getScope() {
        if (scope != null) {
            return scope;
        }
        return getContext().initStandardObjects();
    }

    /** Gets the shared JavaScript engine instance (lazy initialization). */
    protected JsScriptEngine getEngine() {
        if (engineInstance == null) {
            synchronized (LOCK) {
                if (engineInstance == null) {
                    engineInstance = JsScriptEngine.getInstance();
                }
            }
        }
        return engineInstance;
    }

    /** Convert JavaScript value to Java object with enhanced handling. */
    protected Object toJava(Object value) {
        if (value == null) {
            return null;
        }

        // Undefined/null check
        if (value == org.mozilla.javascript.Undefined.instance) {
            return null;
        }

        // Boolean
        if (value instanceof Boolean) {
            return value;
        }

        // Number
        if (value instanceof Number) {
            return value;
        }

        // String
        if (value instanceof String) {
            return value;
        }

        // NativeArray
        if (value instanceof NativeArray) {
            return toArray((NativeArray) value);
        }

        // Scriptable object
        if (value instanceof Scriptable) {
            return toMap((Scriptable) value);
        }

        // Default: return as-is
        return value;
    }

    /** Convert JavaScript array to Java array. */
    protected Object[] toArray(NativeArray array) {
        int size = (int) array.getLength();
        Object[] result = new Object[size];
        for (int i = 0; i < size; i++) {
            result[i] = toJava(array.get(i, array));
        }
        return result;
    }

    /** Convert JavaScript object to Java Map. */
    protected Map<String, Object> toMap(Scriptable scriptable) {
        Map<String, Object> map = new HashMap<>();
        Object[] ids = scriptable.getIds();
        for (Object id : ids) {
            String key = id.toString();
            Object value = scriptable.get(key, scriptable);
            map.put(key, toJava(value));
        }
        return map;
    }

    /** Check if value is defined (not null/undefined). */
    protected boolean isDefined(Object value) {
        return value != null && value != org.mozilla.javascript.Undefined.instance;
    }

    /** Safe number conversion with default. */
    protected double toDouble(Object value, double defaultValue) {
        if (isDefined(value) && value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }

    /** Safe string conversion with default. */
    protected String toString(Object value, String defaultValue) {
        if (isDefined(value)) {
            String str = Context.toString(value);
            return str != null ? str : defaultValue;
        }
        return defaultValue;
    }

    /** Convert Java object to JavaScript value (Rhino handles this automatically). */
    protected Object toJs(Object obj) {
        if (obj == null) {
            return null;
        }

        // Rhino automatically converts primitives and simple objects
        return obj;
    }
}
