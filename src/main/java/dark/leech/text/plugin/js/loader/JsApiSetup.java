package dark.leech.text.plugin.js.loader;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import dark.leech.text.action.Log;
import dark.leech.text.plugin.js.api.Engine;
import dark.leech.text.plugin.js.api.Html;
import dark.leech.text.plugin.js.api.Http;
import dark.leech.text.plugin.js.api.Json;
import dark.leech.text.plugin.js.api.LocalStorage;
import dark.leech.text.plugin.js.api.UserAgent;

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
     * @param pluginSource The plugin source directory (optional, for load() function)
     */
    public static void setup(
            Context ctx, Scriptable scope, String baseUrl, String targetUrl, String pluginSource) {
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

        // Json class - expose for JSON parsing (vBook compatibility)
        Json jsonApi = new Json(ctx, scope);
        ScriptableObject.putProperty(scope, "Json", jsonApi);
        ctx.getWrapFactory().setJavaPrimitiveWrap(false);

        // UserAgent class - expose for platform-specific user agents
        UserAgent userAgentApi = new UserAgent(ctx, scope);
        ScriptableObject.putProperty(scope, "UserAgent", userAgentApi);
        ctx.getWrapFactory().setJavaPrimitiveWrap(false);

        // Engine class - expose for browser automation
        Engine engineApi = new Engine(ctx, scope);
        ScriptableObject.putProperty(scope, "Engine", engineApi);
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

        // Load function - load JavaScript files from plugin directory
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

                        if (fileName.isEmpty()) {
                            Log.add("[load()] File name is empty");
                            return null;
                        }

                        // Security: Validate file path
                        if (!isValidFileName(fileName)) {
                            Log.add("[load()] Invalid file name: " + fileName);
                            return null;
                        }

                        // Load file from plugin directory
                        if (pluginSource == null || pluginSource.isEmpty()) {
                            Log.add("[load()] Plugin source directory not set");
                            return null;
                        }

                        try {
                            java.io.File file = new java.io.File(pluginSource, "src/" + fileName);

                            // Security: Check if file exists and is within plugin directory
                            if (!file.exists() || !file.isFile()) {
                                Log.add("[load()] File not found: " + file.getPath());
                                return null;
                            }

                            // Security: Check file path doesn't escape plugin directory
                            java.io.File pluginDir = new java.io.File(pluginSource, "src");
                            if (!file.getCanonicalPath().startsWith(pluginDir.getCanonicalPath())) {
                                Log.add("[load()] Path traversal detected: " + fileName);
                                return null;
                            }

                            // Security: Check file size (limit to 100KB)
                            long fileSize = file.length();
                            if (fileSize > 100 * 1024) {
                                Log.add("[load()] File too large: " + fileSize + " bytes");
                                return null;
                            }

                            // Read file content
                            String content =
                                    new String(java.nio.file.Files.readAllBytes(file.toPath()));

                            // Execute script in current context
                            return cx.evaluateString(scope, content, fileName, 1, null);

                        } catch (Exception e) {
                            Log.add("[load()] Failed to load file: " + e.getMessage());
                            return null;
                        }
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

        // LocalStorage API for plugin persistence
        LocalStorage localStorageApi =
                new LocalStorage(ctx, scope, pluginSource != null ? pluginSource : "default");
        ScriptableObject.putProperty(scope, "localStorage", localStorageApi);
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

    /**
     * Validate file name for security. Prevents path traversal and ensures only .js files are
     * allowed.
     *
     * @param fileName The file name to validate
     * @return true if valid, false otherwise
     */
    private static boolean isValidFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }

        // No path traversal
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            return false;
        }

        // No absolute paths
        if (fileName.startsWith("/") || fileName.startsWith("\\")) {
            return false;
        }

        // Must end with .js
        if (!fileName.endsWith(".js")) {
            return false;
        }

        // No special characters that could cause issues
        if (fileName.matches(".*[<>:\"|?*].*")) {
            return false;
        }

        return true;
    }
}
