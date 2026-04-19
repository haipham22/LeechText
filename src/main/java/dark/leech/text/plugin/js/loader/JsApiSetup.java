package dark.leech.text.plugin.js.loader;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import dark.leech.text.action.Log;
import dark.leech.text.plugin.js.api.Html;
import dark.leech.text.plugin.js.api.Http;

/**
 * Shared vBook API setup helper for Rhino JavaScript loaders. Extracts duplicate setup logic from
 * TextLoader, DetailLoader, and ListLoader.
 */
public final class JsApiSetup {

    private JsApiSetup() {
        // Utility class - prevent instantiation
    }

    /**
     * Setup vBook API bindings in the provided scope. Registers BASE_URL, Html, Response, fetch,
     * load, and sleep functions.
     *
     * @param ctx The Rhino context
     * @param scope The Rhino scope
     * @param baseUrl The base URL for the plugin source
     * @param targetUrl The target URL being processed
     */
    public static void setup(Context ctx, Scriptable scope, String baseUrl, String targetUrl) {
        // Set BASE_URL variable
        ScriptableObject.putProperty(scope, "BASE_URL", baseUrl);

        // Match vBooks runtime behavior: expose Java String as JS primitive string
        // so JS String methods (e.g. split('?')) follow JavaScript semantics.
        ctx.getWrapFactory().setJavaPrimitiveWrap(false);

        // Html object - Rhino automatically exposes public methods
        Html htmlApi = new Html(ctx, scope);
        ScriptableObject.putProperty(scope, "Html", htmlApi);
        ctx.getWrapFactory().setJavaPrimitiveWrap(false);

        // Http class - expose for static method calls like Http.get(), Http.post()
        Http httpInstance = new Http(ctx, scope);
        ScriptableObject.putProperty(scope, "Http", httpInstance);
        ctx.getWrapFactory().setJavaPrimitiveWrap(false);

        // Response object
        Response responseObj = new Response(ctx, scope);
        ScriptableObject.putProperty(scope, "Response", responseObj);

        // Fetch function - returns Http object for chaining
        Object fetchFunc =
                new org.mozilla.javascript.BaseFunction() {
                    @Override
                    public Object call(
                            org.mozilla.javascript.Context cx,
                            Scriptable scope,
                            Scriptable thisObj,
                            Object[] args) {
                        // Defensive: use Context.toString() to handle any Rhino object type
                        String requestUrl;
                        if (args.length > 0
                                && args[0] != null
                                && args[0] != org.mozilla.javascript.Undefined.instance) {
                            requestUrl = org.mozilla.javascript.Context.toString(args[0]);
                        } else {
                            requestUrl = targetUrl;
                        }
                        Http http = new Http(cx, scope);
                        http.request(requestUrl);
                        return http;
                    }

                    @Override
                    public Object getDefaultValue(java.lang.Class<?> typeHint) {
                        // Safe toString() implementation for plugin compatibility
                        return "function fetch() { [native code] }";
                    }
                };
        ScriptableObject.putProperty(scope, "fetch", fetchFunc);
        ctx.getWrapFactory().setJavaPrimitiveWrap(false);

        // Load function - no-op for config.js
        Object loadFunc =
                new org.mozilla.javascript.BaseFunction() {
                    @Override
                    public Object call(
                            org.mozilla.javascript.Context cx,
                            Scriptable scope,
                            Scriptable thisObj,
                            Object[] args) {
                        String fileName =
                                (args.length > 0 && args[0] != null)
                                        ? JSResponse.getString(args[0])
                                        : "";
                        Log.add("vBook load() called for: " + fileName);
                        return null;
                    }

                    @Override
                    public Object getDefaultValue(java.lang.Class<?> typeHint) {
                        // Safe toString() implementation for plugin compatibility
                        return "function load() { [native code] }";
                    }
                };
        ScriptableObject.putProperty(scope, "load", loadFunc);
        ctx.getWrapFactory().setJavaPrimitiveWrap(false);

        // Sleep function (optional - only used by ListLoader)
        Object sleepFunc =
                new org.mozilla.javascript.BaseFunction() {
                    @Override
                    public Object call(
                            org.mozilla.javascript.Context cx,
                            Scriptable scope,
                            Scriptable thisObj,
                            Object[] args) {
                        int millis =
                                (args.length > 0 && args[0] instanceof Number)
                                        ? ((Number) args[0]).intValue()
                                        : 0;
                        try {
                            Thread.sleep(millis);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        return null;
                    }

                    @Override
                    public Object getDefaultValue(java.lang.Class<?> typeHint) {
                        // Safe toString() implementation for plugin compatibility
                        return "function sleep() { [native code] }";
                    }
                };
        ScriptableObject.putProperty(scope, "sleep", sleepFunc);
        ctx.getWrapFactory().setJavaPrimitiveWrap(false);

        // Console API for plugin logging compatibility
        // Match trusted vBooks behavior: expose same logger object as both Console and console
        Object consoleApi = Context.javaToJS(new ConsoleApi(), scope);
        ScriptableObject.putProperty(scope, "Console", consoleApi);
        ScriptableObject.putProperty(scope, "console", consoleApi);
        ctx.getWrapFactory().setJavaPrimitiveWrap(false);
    }

    /**
     * Extract base URL from a full URL. Returns protocol://host[:port]
     *
     * @param url The URL to extract from
     * @return The base URL
     */
    public static String extractBaseUrl(String url) {
        try {
            java.net.URL parsed = new java.net.URL(url);
            String protocol = parsed.getProtocol();
            String host = parsed.getHost();
            int port = parsed.getPort();
            if (port == -1) {
                return protocol + "://" + host;
            }
            return protocol + "://" + host + ":" + port;
        } catch (Exception e) {
            return url;
        }
    }
}
