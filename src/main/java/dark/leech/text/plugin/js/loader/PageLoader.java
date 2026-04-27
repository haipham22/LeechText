package dark.leech.text.plugin.js.loader;

import java.util.ArrayList;
import java.util.List;

import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;

import dark.leech.text.action.Log;
import dark.leech.text.enities.PluginEntity;
import dark.leech.text.plugin.js.sandbox.JsSandbox;

/**
 * JavaScript page discovery loader for vBook plugins using Rhino. Executes the 'page' script and
 * parses result into a list of page URLs. Used for discovering all chapter page URLs for a novel
 * from HTML metadata (e.g., total-page input field).
 */
public class PageLoader extends AbstractLoader<List<String>> {

    private PageLoader(PluginEntity plugin) {
        super(plugin);
    }

    public static PageLoader with(PluginEntity plugin) {
        return new PageLoader(plugin);
    }

    @Override
    protected String getScript() {
        return plugin.getPageGetter();
    }

    @Override
    protected LoaderType getLoaderType() {
        return LoaderType.PAGE;
    }

    @Override
    protected List<String> processResult(Object result, String url) {
        if (result == null || result == org.mozilla.javascript.Undefined.instance) {
            Log.add("[PageLoader] Script returned null or undefined");
            return new ArrayList<>();
        }

        // Unwrap NativeJavaObject if present (Rhino wraps Java objects)
        if (result instanceof org.mozilla.javascript.NativeJavaObject nativeJavaObj) {
            result = nativeJavaObj.unwrap();
        }

        // Check for Response wrapper (vBooks compatibility)
        if (!Response.isSuccess(result)) {
            String errorMsg = Response.getErrorMessage(result);
            Log.add("[PageLoader] Error response: " + errorMsg);
            return new ArrayList<>();
        }

        Object data = Response.getData(result);

        Log.add("[PageLoader] Processing result, type: " + result.getClass().getName());
        List<String> urlList = convertToStringList(data);
        Log.add("[PageLoader] Discovered " + urlList.size() + " page URLs");
        return urlList;
    }

    /**
     * Load page URLs from the given URL using the configured script. This is a one-shot discovery
     * operation that returns all available page URLs at once.
     *
     * @param url Target URL (novel detail page)
     * @return List of discovered page URLs, or empty list on error
     */
    public List<String> load(String url) {
        String script = getScript();
        if (plugin == null || script == null) {
            Log.add("[PageLoader] No script available");
            return new ArrayList<>();
        }

        JsSandbox sandbox = null;
        try {
            // Extract base URL
            String baseUrl = plugin.getSource();
            if (baseUrl == null || baseUrl.isEmpty()) {
                baseUrl = extractBaseUrl(url);
            }

            // Create secure sandbox for PAGE loader
            sandbox =
                    new JsSandbox.Builder()
                            .loaderType(getLoaderType())
                            .baseUrl(baseUrl)
                            .targetUrl(url)
                            .build();

            // Execute plugin script in sandbox
            if (!sandbox.execute(script, getScriptName())) {
                return new ArrayList<>();
            }

            // Call execute(url) function in sandbox (no page parameter)
            Object result = sandbox.callFunction("execute", url);

            if (result == null || result == org.mozilla.javascript.Undefined.instance) {
                return new ArrayList<>();
            }

            return processResult(result, url);

        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg == null) {
                errorMsg = e.getClass().getName();
            }
            Log.add("[PageLoader] JavaScript execution error: " + errorMsg);
            return new ArrayList<>();
        } finally {
            if (sandbox != null) {
                sandbox.close();
            }
        }
    }

    /**
     * Convert object to List of String URLs. Handles NativeArray, NativeObject, NativeJavaObject,
     * and raw arrays.
     */
    private List<String> convertToStringList(Object data) {
        if (data == null) {
            return new ArrayList<>();
        }

        if (data instanceof NativeArray) {
            NativeArray nativeArray = (NativeArray) data;
            long size = nativeArray.size();
            if (size > Integer.MAX_VALUE) {
                Log.add(
                        "[PageLoader] Array size exceeds Integer.MAX_VALUE, truncating to "
                                + Integer.MAX_VALUE);
                size = Integer.MAX_VALUE;
            }
            List<String> list = new ArrayList<>((int) size);
            for (int i = 0; i < size; i++) {
                Object item = nativeArray.get(i);
                String str = convertToString(item);
                if (str != null) {
                    list.add(str);
                }
            }
            return list;
        }

        if (data instanceof NativeObject) {
            // Single object - try to extract URL property
            String url = extractUrlFromObject((NativeObject) data);
            if (url != null) {
                List<String> list = new ArrayList<>(1);
                list.add(url);
                return list;
            }
            return new ArrayList<>();
        }

        if (data instanceof org.mozilla.javascript.NativeJavaObject) {
            // Unwrap the Java object
            org.mozilla.javascript.NativeJavaObject nativeJavaObj =
                    (org.mozilla.javascript.NativeJavaObject) data;
            Object unwrapped = nativeJavaObj.unwrap();
            if (unwrapped instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> objList = (List<Object>) unwrapped;
                List<String> list = new ArrayList<>(objList.size());
                for (Object item : objList) {
                    String str = convertToString(item);
                    if (str != null) {
                        list.add(str);
                    }
                }
                return list;
            }
            if (unwrapped != null && unwrapped.getClass().isArray()) {
                // Handle Java arrays
                Object[] array = (Object[]) unwrapped;
                List<String> list = new ArrayList<>(array.length);
                for (Object item : array) {
                    String str = convertToString(item);
                    if (str != null) {
                        list.add(str);
                    }
                }
                return list;
            }
            // Single object
            String url = convertToString(unwrapped);
            if (url != null) {
                List<String> list = new ArrayList<>(1);
                list.add(url);
                return list;
            }
            return new ArrayList<>();
        }

        if (data instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> objList = (List<Object>) data;
            List<String> list = new ArrayList<>(objList.size());
            for (Object item : objList) {
                String str = convertToString(item);
                if (str != null) {
                    list.add(str);
                }
            }
            return list;
        }

        // Single item - convert to string
        String str = convertToString(data);
        if (str != null) {
            List<String> list = new ArrayList<>(1);
            list.add(str);
            return list;
        }

        return new ArrayList<>();
    }

    /** Convert object to String URL. */
    private String convertToString(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof org.mozilla.javascript.Undefined) {
            return null;
        }
        String str = obj.toString();
        return (str != null && !str.isEmpty()) ? str : null;
    }

    /** Extract URL from NativeObject (checks common property names). */
    private String extractUrlFromObject(NativeObject obj) {
        String[] urlProps = {"url", "link", "href", "page", "pageUrl"};
        for (String prop : urlProps) {
            Object value = obj.get(prop, obj);
            // Check for NOT_FOUND (property doesn't exist)
            if (value == org.mozilla.javascript.UniqueTag.NOT_FOUND) {
                continue;
            }
            if (value != null && value != org.mozilla.javascript.Undefined.instance) {
                String url = convertToString(value);
                if (url != null && !url.isEmpty()) {
                    return url;
                }
            }
        }
        return null;
    }

    /** Extract base URL from full URL. */
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
