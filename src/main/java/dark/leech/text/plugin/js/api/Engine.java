package dark.leech.text.plugin.js.api;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * Engine factory for creating Browser instances. Provides browser automation capabilities for
 * JavaScript plugins.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * const browser = Engine.newBrowser();
 * browser.launch("https://example.com");
 * const title = browser.callJs("return document.title;");
 * browser.close();
 * }</pre>
 */
public final class Engine extends JsApiWrapper {

    public Engine(Context context, Scriptable scope) {
        super(context, scope);
    }

    /**
     * Create a new Browser instance for browser automation.
     *
     * @return New Browser instance
     */
    public static Browser newBrowser() {
        // Create browser without context for factory method
        return new Browser(null, null);
    }

    /**
     * Create a new Browser instance with execution context (internal use).
     *
     * @param context The Rhino context
     * @param scope The Rhino scope
     * @return New Browser instance
     */
    public static Browser newBrowser(Context context, Scriptable scope) {
        return new Browser(context, scope);
    }
}
