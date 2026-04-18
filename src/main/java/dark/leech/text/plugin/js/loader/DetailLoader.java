package dark.leech.text.plugin.js.loader;

import org.mozilla.javascript.*;

import dark.leech.text.action.Log;
import dark.leech.text.enities.BookEntity;
import dark.leech.text.enities.PluginEntity;
import dark.leech.text.util.TextUtils;

/**
 * JavaScript detail loader for vBook plugins using Rhino. Sets up vBook API environment (BASE_URL,
 * load, fetch, Response, Html).
 */
public class DetailLoader {

    private final PluginEntity plugin;

    private DetailLoader(PluginEntity plugin) {
        this.plugin = plugin;
    }

    public static DetailLoader with(PluginEntity plugin) {
        return new DetailLoader(plugin);
    }

    public BookEntity load(String url) {
        if (!TextUtils.isEmpty(plugin.getDetailGetter())) {
            Context ctx = null;
            try {
                // Enter Rhino context
                ctx = Context.enter();
                ctx.setOptimizationLevel(-1); // Interpretation mode for security
                ctx.setLanguageVersion(200); // ES6 support for vBook plugins
                Scriptable scope = ctx.initStandardObjects();

                // Extract base URL from plugin source
                String baseUrl = plugin.getSource();
                if (baseUrl == null || baseUrl.isEmpty()) {
                    baseUrl = VBookApiSetup.extractBaseUrl(url);
                }

                // Setup vBook API bindings
                VBookApiSetup.setupVBookApi(ctx, scope, baseUrl, url);

                // Execute the detail script
                ctx.evaluateString(scope, plugin.getDetailGetter(), "detailGetter", 1, null);

                // Call the execute function
                Object functionObj = scope.get("execute", scope);
                if (!(functionObj instanceof Function function)) {
                    return null;
                }

                // Defensive: ensure URL is valid before calling execute
                String safeUrl =
                        (url == null || url.isEmpty() || url.contains("NOT_FOUND")) ? baseUrl : url;
                Object result = function.call(ctx, scope, scope, new Object[] {safeUrl});

                if (result == null || result == org.mozilla.javascript.Undefined.instance) {
                    return null;
                }

                return extractBookEntity(result, url);

            } catch (Exception e) {
                Log.add("JavaScript execution error in detail loader: " + e.getMessage());
                return null;
            } finally {
                if (ctx != null) {
                    Context.exit();
                }
            }
        }
        return null;
    }

    private BookEntity extractBookEntity(Object result, String fallbackUrl) {
        BookEntity entity = new BookEntity();

        if (result instanceof NativeObject obj) {

            // Use JSResponse to safely extract string values from NativeObject
            Object name = obj.get("name", obj);
            String nameStr = JSResponse.getString(name);
            if (nameStr != null) {
                entity.setName(nameStr);
            }

            Object url = obj.get("url", obj);
            String urlStr = JSResponse.getString(url);
            String resultUrl = fallbackUrl;
            if (urlStr != null && !urlStr.isEmpty()) {
                resultUrl = urlStr;
            }
            entity.setUrl(resultUrl);

            Object detail = obj.get("detail", obj);
            String detailStr = JSResponse.getString(detail);
            if (detailStr != null) {
                entity.setDetail(detailStr.trim().replaceAll("\n+", "\n"));
            }

            Object description = obj.get("description", obj);
            String descStr = JSResponse.getString(description);
            if (descStr != null) {
                entity.setIntroduce(descStr);
            }

            Object cover = obj.get("cover", obj);
            String coverStr = JSResponse.getString(cover);
            if (coverStr != null) {
                entity.setCover(coverStr);
            }

            Object author = obj.get("author", obj);
            String authorStr = JSResponse.getString(author);
            if (authorStr != null) {
                entity.setAuthor(authorStr);
            }
        } else if (result != null) {
            // Handle plain string result
            entity.setDetail(result.toString());
        }

        entity.setWebSource(plugin.getName());
        return entity;
    }
}
