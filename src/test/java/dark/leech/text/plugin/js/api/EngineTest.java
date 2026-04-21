package dark.leech.text.plugin.js.api;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Unit tests for Engine factory class. Validates Browser instance creation.
 */
public class EngineTest {

    @Test
    public void testEngineNewBrowserReturnsBrowserInstance() {
        Browser browser = Engine.newBrowser();
        assertNotNull("Engine.newBrowser() should return Browser instance", browser);
    }

    @Test
    public void testEngineNewBrowserWithContext() {
        Context ctx = null;
        try {
            ctx = Context.enter();
            ctx.setOptimizationLevel(-1);
            ctx.setLanguageVersion(200);
            Scriptable scope = ctx.initStandardObjects();

            Browser browser = Engine.newBrowser(ctx, scope);
            assertNotNull("Engine.newBrowser(ctx, scope) should return Browser instance", browser);

        } finally {
            if (ctx != null) {
                Context.exit();
            }
        }
    }

    @Test
    public void testEngineNewBrowserInJavaScript() {
        Context ctx = null;
        try {
            ctx = Context.enter();
            ctx.setOptimizationLevel(-1);
            ctx.setLanguageVersion(200);
            Scriptable scope = ctx.initStandardObjects();

            // Setup Engine API
            Engine engineApi = new Engine(ctx, scope);
            ctx.getWrapFactory().setJavaPrimitiveWrap(false);
            ScriptableObject.putProperty(scope, "Engine", engineApi);

            // Test from JavaScript
            Object result = ctx.evaluateString(scope,
                "var browser = Engine.newBrowser();" +
                "typeof browser.launch;",
                "test", 1, null);

            assertNotNull("launch method should exist", result);
            assertEquals("launch should be a function", "function", result.toString());

        } finally {
            if (ctx != null) {
                Context.exit();
            }
        }
    }
}
