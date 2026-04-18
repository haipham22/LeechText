package dark.leech.text.plugin.js.loader;

import org.mozilla.javascript.*;

import dark.leech.text.action.Log;
import dark.leech.text.enities.PluginEntity;

/**
 * JavaScript chapter content loader for vBook plugins using Rhino. Executes chapGetter script and
 * returns chapter text as String.
 */
public class TextLoader {

    private final PluginEntity plugin;

    private TextLoader(PluginEntity plugin) {
        this.plugin = plugin;
    }

    public static TextLoader with(PluginEntity plugin) {
        return new TextLoader(plugin);
    }

    /**
     * Load chapter content from URL.
     *
     * @param url Chapter URL
     * @return Chapter text content, or null on error
     */
    public String load(String url) {
        // 1. Validate plugin
        if (plugin == null
                || !PluginEntity.SCRIPT_ENGINE_JAVASCRIPT.equals(plugin.getScriptEngine())
                || plugin.getChapGetter() == null) {
            return null;
        }

        Context ctx = null;
        try {
            // 2. Enter Rhino Context
            ctx = Context.enter();
            ctx.setOptimizationLevel(-1); // Interpretation mode for security
            ctx.setLanguageVersion(200); // ES6 support for vBook plugins
            Scriptable scope = ctx.initStandardObjects();

            // 3. Extract base URL
            String baseUrl = plugin.getSource();
            if (baseUrl == null || baseUrl.isEmpty()) {
                baseUrl = VBookApiSetup.extractBaseUrl(url);
            }

            // 4. Setup vBook API
            VBookApiSetup.setupVBookApi(ctx, scope, baseUrl, url);

            // 5. Execute chapGetter script
            ctx.evaluateString(scope, plugin.getChapGetter(), "chapGetter", 1, null);

            // 6. Call execute(url) function
            Object functionObj = scope.get("execute", scope);
            if (!(functionObj instanceof Function function)) {
                Log.add("TextLoader: execute() function not found in script");
                return null;
            }

            Object result = function.call(ctx, scope, scope, new Object[] {url});

            // 7. Extract String from result
            if (result == null || result == org.mozilla.javascript.Undefined.instance) {
                return null;
            }

            // Handle different return types:
            // NativeObject with properties
            if (result instanceof NativeObject) {
                NativeObject obj = (NativeObject) result;

                Object body = obj.get("body", obj);
                if (body != null && body != org.mozilla.javascript.Undefined.instance) {
                    return body.toString();
                }

                Object content = obj.get("content", obj);
                if (content != null && content != org.mozilla.javascript.Undefined.instance) {
                    return content.toString();
                }

                Object text = obj.get("text", obj);
                if (text != null && text != org.mozilla.javascript.Undefined.instance) {
                    return text.toString();
                }
            }

            // Direct string result
            return Context.toString(result);

        } catch (Exception e) {
            Log.add("TextLoader: Script execution error - " + e.getMessage());
            return null;
        } finally {
            if (ctx != null) {
                Context.exit();
            }
        }
    }
}
