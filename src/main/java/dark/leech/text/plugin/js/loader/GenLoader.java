package dark.leech.text.plugin.js.loader;

import java.util.ArrayList;
import java.util.List;

import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;

import dark.leech.text.action.Log;
import dark.leech.text.enities.PluginEntity;
import dark.leech.text.plugin.js.sandbox.JsSandbox;

/**
 * JavaScript pagination loader for vBook plugins using Rhino. Executes the 'gen' script and
 * parses result into PaginationResult with items and next page metadata.
 */
public class GenLoader extends AbstractLoader<PaginationResult<Object>> {

    private GenLoader(PluginEntity plugin) {
        super(plugin);
    }

    public static GenLoader with(PluginEntity plugin) {
        return new GenLoader(plugin);
    }

    @Override
    protected String getScript() {
        return plugin.getGenGetter();
    }

    @Override
    protected LoaderType getLoaderType() {
        return LoaderType.GEN;
    }

    @Override
    protected PaginationResult<Object> processResult(Object result, String url) {
        if (result == null || result == org.mozilla.javascript.Undefined.instance) {
            Log.add("[GenLoader] Script returned null or undefined");
            return PaginationResult.empty();
        }

        // Unwrap NativeJavaObject if present (Rhino wraps Java objects)
        if (result instanceof org.mozilla.javascript.NativeJavaObject) {
            org.mozilla.javascript.NativeJavaObject nativeJavaObj = (org.mozilla.javascript.NativeJavaObject) result;
            result = nativeJavaObj.unwrap();
        }

        // Check if result is PaginatedResponse (from Response.success(data, next))
        if (result instanceof PaginatedResponse) {
            PaginatedResponse paginated = (PaginatedResponse) result;
            Object data = paginated.getData();
            Object next = paginated.getNext();

            // Unwrap data if it's also wrapped
            if (data instanceof org.mozilla.javascript.NativeJavaObject) {
                data = ((org.mozilla.javascript.NativeJavaObject) data).unwrap();
            }

            // Convert data to List
            List<Object> items = convertToList(data);

            // Convert next to String
            String nextPage = next != null ? next.toString() : null;

            Log.add("[GenLoader] Paginated result: " + items.size() + " items, next page: " + nextPage);

            return PaginationResult.of(items, nextPage);
        }

        // Fallback: Raw data (Response.success(data) with single argument)
        Log.add("[GenLoader] Non-paginated response, converting to list");
        List<Object> items = convertToList(result);
        return PaginationResult.of(items);
    }

    /**
     * Load data from URL with specific page number.
     *
     * @param url Target URL
     * @param page Page number or identifier (can be null for first page)
     * @return Paginated result with items and next page info
     */
    public PaginationResult<Object> load(String url, String page) {
        String script = getScript();
        if (plugin == null || script == null) {
            return PaginationResult.empty();
        }

        JsSandbox sandbox = null;
        try {
            // Extract base URL
            String baseUrl = plugin.getSource();
            if (baseUrl == null || baseUrl.isEmpty()) {
                baseUrl = extractBaseUrl(url);
            }

            // Create secure sandbox for GEN loader
            sandbox = new JsSandbox.Builder()
                    .loaderType(getLoaderType())
                    .baseUrl(baseUrl)
                    .targetUrl(url)
                    .build();

            // Execute plugin script in sandbox
            if (!sandbox.execute(script, getScriptName())) {
                return null;
            }

            // Call execute(url, page) function in sandbox
            String sanitizedPage = (page != null) ? page.trim() : null;
            Object result;
            if (sanitizedPage == null || sanitizedPage.isEmpty()) {
                result = sandbox.callFunction("execute", url);
            } else {
                result = sandbox.callFunction("execute", url, sanitizedPage);
            }

            if (result == null || result == org.mozilla.javascript.Undefined.instance) {
                return PaginationResult.empty();
            }

            return processResult(result, url);

        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg == null) {
                errorMsg = e.getClass().getName();
            }
            Log.add("[GenLoader] JavaScript execution error: " + errorMsg);
            return PaginationResult.empty();
        } finally {
            if (sandbox != null) {
                sandbox.close();
            }
        }
    }

    /**
     * Convert object to List. Handles NativeArray, NativeObject, NativeJavaObject, and raw arrays.
     */
    private List<Object> convertToList(Object data) {
        if (data == null) {
            return new ArrayList<>();
        }

        if (data instanceof NativeArray) {
            NativeArray nativeArray = (NativeArray) data;
            long size = nativeArray.size();
            if (size > Integer.MAX_VALUE) {
                Log.add("[GenLoader] Array size exceeds Integer.MAX_VALUE, truncating to " + Integer.MAX_VALUE);
                size = Integer.MAX_VALUE;
            }
            List<Object> list = new ArrayList<>((int) size);
            for (int i = 0; i < size; i++) {
                list.add(nativeArray.get(i));
            }
            return list;
        }

        if (data instanceof NativeObject) {
            // Single object, wrap in list
            List<Object> list = new ArrayList<>(1);
            list.add(data);
            return list;
        }

        if (data instanceof org.mozilla.javascript.NativeJavaObject) {
            // Unwrap the Java object
            org.mozilla.javascript.NativeJavaObject nativeJavaObj = (org.mozilla.javascript.NativeJavaObject) data;
            Object unwrapped = nativeJavaObj.unwrap();
            if (unwrapped instanceof List) {
                return (List<Object>) unwrapped;
            }
            if (unwrapped != null && unwrapped.getClass().isArray()) {
                // Handle Java arrays
                Object[] array = (Object[]) unwrapped;
                List<Object> list = new ArrayList<>(array.length);
                for (Object item : array) {
                    list.add(item);
                }
                return list;
            }
            // Single object, wrap in list
            List<Object> list = new ArrayList<>(1);
            list.add(unwrapped);
            return list;
        }

        if (data instanceof List) {
            return (List<Object>) data;
        }

        // Unknown type, wrap in list
        List<Object> list = new ArrayList<>(1);
        list.add(data);
        return list;
    }

    /**
     * Extract base URL from full URL.
     */
    private String extractBaseUrl(String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }
        try {
            java.net.URL parsedUrl = new java.net.URL(url);
            String protocol = parsedUrl.getProtocol();
            String host = parsedUrl.getHost();
            int port = parsedUrl.getPort();
            if (port == -1) {
                return protocol + "://" + host;
            }
            return protocol + "://" + host + ":" + port;
        } catch (Exception e) {
            return "";
        }
    }
}
