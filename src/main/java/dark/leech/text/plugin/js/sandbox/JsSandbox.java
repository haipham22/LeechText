package dark.leech.text.plugin.js.sandbox;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import lombok.Getter;

import dark.leech.text.action.Log;
import dark.leech.text.plugin.js.loader.JsApiSetup;
import dark.leech.text.plugin.js.loader.LoaderType;

/**
 * Secure sandbox for vBook plugin JavaScript execution. Provides isolated execution environment
 * with controlled API access and security constraints.
 */
public class JsSandbox {

    /** -- GETTER -- Get the Rhino Context for this sandbox. */
    @Getter private final Context context;

    /** -- GETTER -- Get the JavaScript scope for this sandbox. */
    @Getter private final Scriptable scope;

    private final LoaderType loaderType;
    private final String baseUrl;
    private final String targetUrl;

    private JsSandbox(Builder builder) {
        this.loaderType = builder.loaderType;
        this.baseUrl = builder.baseUrl;
        this.targetUrl = builder.targetUrl;

        // Enter Rhino Context
        this.context = Context.enter();
        context.setOptimizationLevel(-1); // Interpretation mode for security
        context.setLanguageVersion(200); // ES6 support for vBook plugins
        context.getWrapFactory().setJavaPrimitiveWrap(false); // Match vBooks behavior

        // Create isolated scope
        this.scope = context.initStandardObjects();

        // Setup secure API surface
        setupSandbox();

        Log.add("[VBookSandbox] Created " + loaderType.getType() + " sandbox");
    }

    /**
     * Execute JavaScript code in this sandbox.
     *
     * @param script JavaScript code to execute
     * @param scriptName Name for logging/error reporting
     * @return true if execution succeeded, false otherwise
     */
    public boolean execute(String script, String scriptName) {
        try {
            context.evaluateString(scope, script, scriptName, 1, null);
            return true;
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg == null) {
                errorMsg = e.getClass().getName();
            }

            // Suppress toString() errors
            if (errorMsg.contains("toString")) {
                Log.add("[VBookSandbox] Suppressed toString() error in " + scriptName);
                return false;
            }

            Log.add("[VBookSandbox] JavaScript execution error in " + scriptName + ": " + errorMsg);
            return false;
        }
    }

    /**
     * Call a JavaScript function in this sandbox.
     *
     * @param functionName Name of the function to call
     * @param args Arguments to pass to the function
     * @return Result of the function call, or null if failed
     */
    public Object callFunction(String functionName, Object... args) {
        try {
            Object functionObj = scope.get(functionName, scope);
            if (!(functionObj instanceof org.mozilla.javascript.Function function)) {
                Log.add("[VBookSandbox] Function not found: " + functionName);
                return null;
            }

            return function.call(context, scope, scope, args);
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg == null) {
                errorMsg = e.getClass().getName();
            }

            // Suppress toString() errors
            if (errorMsg.contains("toString")) {
                Log.add("[VBookSandbox] Suppressed toString() error in " + functionName);
                return null;
            }

            Log.add("[VBookSandbox] Function call error in " + functionName + ": " + errorMsg);
            return null;
        }
    }

    /** Exit the sandbox and clean up resources. */
    public void close() {
        if (context != null) {
            Context.exit();
            Log.add("[VBoxSandbox] Closed " + loaderType.getType() + " sandbox");
        }
    }

    /** Setup secure sandbox environment with controlled API access. */
    private void setupSandbox() {
        // Setup vBook API (Http, Html, Response, etc.)
        JsApiSetup.setup(context, scope, baseUrl, targetUrl);

        // Add safe toString wrapper to prevent Function.toString() errors
        addSafeToStringWrapper();
    }

    /** Add safe toString wrapper to prevent Function.toString() errors. */
    private void addSafeToStringWrapper() {
        String safeToStringScript =
                "(function() {"
                        + "const originalFunctionToString = Function.prototype.toString;"
                        + "Function.prototype.toString = function() {"
                        + "try {"
                        + "return originalFunctionToString.call(this);"
                        + "} catch(e) {"
                        + "return 'function () { [native code] }';"
                        + "}"
                        + "};"
                        + "const originalObjectToString = Object.prototype.toString;"
                        + "Object.prototype.toString = function() {"
                        + "try {"
                        + "return originalObjectToString.call(this);"
                        + "} catch(e) {"
                        + "return '[object Object]';"
                        + "}"
                        + "};"
                        + "})();";
        try {
            context.evaluateString(scope, safeToStringScript, "safeToString", 1, null);
        } catch (Exception e) {
            Log.add("[VBookSandbox] Failed to add toString wrapper: " + e.getMessage());
        }
    }

    /** Builder for creating VBookSandbox instances. */
    public static class Builder {
        private LoaderType loaderType;
        private String baseUrl;
        private String targetUrl;

        public Builder loaderType(LoaderType loaderType) {
            this.loaderType = loaderType;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder targetUrl(String targetUrl) {
            this.targetUrl = targetUrl;
            return this;
        }

        public JsSandbox build() {
            if (loaderType == null) {
                throw new IllegalStateException("loaderType is required");
            }
            if (baseUrl == null) {
                throw new IllegalStateException("baseUrl is required");
            }
            if (targetUrl == null) {
                throw new IllegalStateException("targetUrl is required");
            }
            return new JsSandbox(this);
        }
    }
}
