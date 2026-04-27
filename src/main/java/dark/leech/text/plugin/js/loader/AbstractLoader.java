package dark.leech.text.plugin.js.loader;

import dark.leech.text.action.Log;
import dark.leech.text.enities.PluginEntity;
import dark.leech.text.plugin.js.sandbox.JsSandbox;

/**
 * Abstract base loader for vBook plugins using sandbox pattern. Extracts common sandbox creation,
 * script execution, and error handling patterns.
 */
public abstract class AbstractLoader<T> {

    protected final PluginEntity plugin;

    protected AbstractLoader(PluginEntity plugin) {
        this.plugin = plugin;
    }

    /**
     * Get the script content to execute.
     *
     * @return Script string or null if not available
     */
    protected abstract String getScript();

    /**
     * Get the loader type for sandbox isolation.
     *
     * @return Loader type enum (LIST, DETAIL, TEXT)
     */
    protected abstract LoaderType getLoaderType();

    /**
     * Get the script name for logging.
     *
     * @return Script name (e.g., "tocGetter", "detailGetter")
     */
    protected String getScriptName() {
        return getLoaderType().getScriptName();
    }

    /**
     * Process the result from execute() function call.
     *
     * @param result The result object from JavaScript execution
     * @param url The target URL
     * @return Processed result or null on error
     */
    protected abstract T processResult(Object result, String url);

    /**
     * Load data from URL using the configured script in a secure sandbox.
     *
     * @param url Target URL
     * @return Processed result or null on error
     */
    public T load(String url) {
        String script = getScript();
        if (plugin == null || script == null) {
            return null;
        }

        JsSandbox sandbox = null;
        try {
            // Extract base URL
            String baseUrl = plugin.getSource();
            if (baseUrl == null || baseUrl.isEmpty()) {
                baseUrl = JsApiSetup.extractBaseUrl(url);
            }

            // Create secure sandbox for this loader type
            JsSandbox.Builder builder =
                    new JsSandbox.Builder()
                            .loaderType(getLoaderType())
                            .baseUrl(baseUrl)
                            .targetUrl(url);

            // Add plugin source if available
            if (plugin.getSource() != null && !plugin.getSource().isEmpty()) {
                builder.pluginSource(plugin.getSource());
            }

            sandbox = builder.build();

            // Execute plugin script in sandbox
            if (!sandbox.execute(script, getScriptName())) {
                return null;
            }

            // Call execute(url) function in sandbox
            Object result = sandbox.callFunction("execute", url);

            if (result == null || result == org.mozilla.javascript.Undefined.instance) {
                return null;
            }

            return processResult(result, url);

        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg == null) {
                errorMsg = e.getClass().getName();
            }
            Log.add("JavaScript execution error in " + getScriptName() + ": " + errorMsg);
            return null;
        } finally {
            if (sandbox != null) {
                sandbox.close();
            }
        }
    }
}
