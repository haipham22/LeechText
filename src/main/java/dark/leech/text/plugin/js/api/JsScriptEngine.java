package dark.leech.text.plugin.js.api;

import java.util.Map;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;

import dark.leech.text.action.Log;

/**
 * JavaScript script engine with resource management and context pooling. Uses Rhino engine for
 * vBook plugin compatibility.
 */
public class JsScriptEngine {

    private static final Object LOCK = new Object();
    private static JsScriptEngine engine;
    private final RhinoContextPool contextPool;

    // Resource limits
    private static final int MAX_CONTEXTS = 5;
    private static final int MAX_EXECUTION_TIME_MS = 30_000; // 30 seconds
    private static final int MAX_MEMORY_MB = 50;

    private JsScriptEngine() {
        this.contextPool = new RhinoContextPool(MAX_CONTEXTS, MAX_EXECUTION_TIME_MS, MAX_MEMORY_MB);
        Log.add("Rhino JavaScript engine initialized");
    }

    /**
     * Gets the singleton instance of JsScriptEngine.
     *
     * @return The JsScriptEngine instance
     */
    public static JsScriptEngine getInstance() {
        if (engine == null) {
            synchronized (LOCK) {
                if (engine == null) {
                    engine = new JsScriptEngine();
                }
            }
        }
        return engine;
    }

    /** Resets the singleton instance. Used primarily for testing. */
    public static void resetInstance() {
        synchronized (LOCK) {
            engine = null;
        }
    }

    /**
     * Executes a JavaScript script with resource limits and timeout.
     *
     * @param script The JavaScript code to execute
     * @return The result of the script execution
     * @throws ExecutionException if execution fails or times out
     */
    public Object execute(String script)
            throws ExecutionException, RhinoContextPool.ResourceExhaustedException {
        RhinoPooledContext pooledContext = null;
        Context ctx = null;
        try {
            // Borrow from pool (creates if needed)
            pooledContext = contextPool.borrowContext();
            ctx = pooledContext.getContext();
            ctx.setLanguageVersion(200); // ES6 support for vBook compatibility
            Scriptable scope = ctx.initStandardObjects();

            // Execute script
            Object result = ctx.evaluateString(scope, script, "script", 1, null);
            return result;

        } catch (Exception e) {
            if (pooledContext != null) {
                contextPool.invalidateContext(pooledContext);
                pooledContext = null;
            }
            throw new ExecutionException("Script execution failed: " + e.getMessage(), e);
        } finally {
            if (pooledContext != null) {
                contextPool.returnContext(pooledContext);
            } else if (ctx != null) {
                Context.exit();
            }
        }
    }

    /**
     * Executes a JavaScript function with parameters.
     *
     * @param script The script containing the function
     * @param functionName The name of the function to call
     * @param args Arguments to pass to the function
     * @return The result of the function call
     * @throws ExecutionException if execution fails
     */
    public Object executeFunction(String script, String functionName, Object... args)
            throws ExecutionException, RhinoContextPool.ResourceExhaustedException {
        RhinoPooledContext pooledContext = null;
        Context ctx = null;
        try {
            pooledContext = contextPool.borrowContext();
            ctx = pooledContext.getContext();
            ctx.setLanguageVersion(200); // ES6 support for vBook compatibility
            Scriptable scope = ctx.initStandardObjects();

            // Evaluate the script first
            ctx.evaluateString(scope, script, "script", 1, null);

            // Get the function from scope
            Object functionObj = scope.get(functionName, scope);
            if (!(functionObj instanceof org.mozilla.javascript.Function)) {
                throw new ExecutionException(
                        "Function '" + functionName + "' not found or not executable");
            }

            // Execute the function with arguments
            org.mozilla.javascript.Function function =
                    (org.mozilla.javascript.Function) functionObj;
            Object result = function.call(ctx, scope, scope, args);
            return result;

        } catch (ExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new ExecutionException("Function execution failed: " + e.getMessage(), e);
        } finally {
            if (pooledContext != null) {
                contextPool.returnContext(pooledContext);
            } else if (ctx != null) {
                Context.exit();
            }
        }
    }

    /** Creates a null JavaScript value. */
    public Object createNullValue() {
        return null;
    }

    /** Creates a JavaScript value from a Java object. */
    public Object createValue(Object obj) {
        return obj; // Rhino handles Java objects directly
    }

    /** Creates a JavaScript object from a Map. */
    public Scriptable createObject(Map<String, Object> map) {
        RhinoPooledContext pooled = null;
        Context ctx = null;
        try {
            pooled = contextPool.borrowContext();
            ctx = pooled.getContext();
            Scriptable scope = ctx.initStandardObjects();

            Scriptable result = ctx.newObject(scope);
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                result.put(entry.getKey(), result, entry.getValue());
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create object", e);
        } finally {
            if (pooled != null) {
                try {
                    contextPool.returnContext(pooled);
                } catch (Exception ex) {
                    // Ignore
                }
            }
        }
    }

    /** Creates a JavaScript array from an Object array. */
    public NativeArray createArray(Object[] array) {
        RhinoPooledContext pooled = null;
        Context ctx = null;
        try {
            pooled = contextPool.borrowContext();
            ctx = pooled.getContext();
            Scriptable scope = ctx.initStandardObjects();

            return (NativeArray) ctx.newArray(scope, array);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create array", e);
        } finally {
            if (pooled != null) {
                try {
                    contextPool.returnContext(pooled);
                } catch (Exception ex) {
                    // Ignore
                }
            }
        }
    }

    /** Exception thrown when script execution fails. */
    public static class ExecutionException extends Exception {
        public ExecutionException(String message) {
            super(message);
        }

        public ExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
