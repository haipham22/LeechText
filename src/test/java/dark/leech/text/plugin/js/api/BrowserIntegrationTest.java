package dark.leech.text.plugin.js.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 * Integration tests for Browser automation API. Validates Selenium WebDriver integration with Rhino
 * JavaScript engine.
 */
public class BrowserIntegrationTest {

    @Test
    public void testEngineNewBrowser() {
        Context ctx = null;
        try {
            ctx = Context.enter();
            ctx.setOptimizationLevel(-1);
            ctx.setLanguageVersion(200);
            Scriptable scope = ctx.initStandardObjects();

            // Setup API
            Engine engineApi = new Engine(ctx, scope);
            ctx.getWrapFactory().setJavaPrimitiveWrap(false);
            ScriptableObject.putProperty(scope, "Engine", engineApi);

            // Test Engine.newBrowser()
            Object result =
                    ctx.evaluateString(
                            scope,
                            "var browser = Engine.newBrowser(); browser != null;",
                            "test",
                            1,
                            null);

            assertNotNull("Engine.newBrowser() should return Browser instance", result);
            assertTrue("Result should be true", result instanceof Boolean && (Boolean) result);

        } finally {
            if (ctx != null) {
                Context.exit();
            }
        }
    }

    @Test
    public void testBrowserLaunchAndClose() {
        Context ctx = null;
        try {
            ctx = Context.enter();
            ctx.setOptimizationLevel(-1);
            ctx.setLanguageVersion(200);
            Scriptable scope = ctx.initStandardObjects();

            // Setup API
            Engine engineApi = new Engine(ctx, scope);
            ctx.getWrapFactory().setJavaPrimitiveWrap(false);
            ScriptableObject.putProperty(scope, "Engine", engineApi);

            // Test browser launch with a simple URL
            Object result =
                    ctx.evaluateString(
                            scope,
                            "var browser = Engine.newBrowser();"
                                    + "browser.launch('https://example.com');"
                                    + "browser.title();",
                            "test",
                            1,
                            null);

            assertNotNull("Browser should return title", result);
            assertEquals("Title should be 'Example Domain'", "Example Domain", result);

        } finally {
            if (ctx != null) {
                Context.exit();
            }
        }
    }

    @Test
    public void testBrowserCallJs() {
        Context ctx = null;
        try {
            ctx = Context.enter();
            ctx.setOptimizationLevel(-1);
            ctx.setLanguageVersion(200);
            Scriptable scope = ctx.initStandardObjects();

            // Setup API
            Engine engineApi = new Engine(ctx, scope);
            ctx.getWrapFactory().setJavaPrimitiveWrap(false);
            ScriptableObject.putProperty(scope, "Engine", engineApi);

            // Test JavaScript execution
            Object result =
                    ctx.evaluateString(
                            scope,
                            "var browser = Engine.newBrowser();"
                                    + "browser.launch('about:blank');"
                                    + "browser.sleep(100);"
                                    + // Wait for page to load
                                    "browser.callJs('return document.title;');",
                            "test",
                            1,
                            null);

            assertNotNull("callJs should return result", result);
            // Title might be empty string or "about:blank"
            assertTrue("Title should be string", result instanceof String);

        } finally {
            if (ctx != null) {
                Context.exit();
            }
        }
    }

    @Test
    public void testBrowserHtml() {
        Context ctx = null;
        try {
            ctx = Context.enter();
            ctx.setOptimizationLevel(-1);
            ctx.setLanguageVersion(200);
            Scriptable scope = ctx.initStandardObjects();

            // Setup API
            Engine engineApi = new Engine(ctx, scope);
            Html htmlApi = new Html(ctx, scope);
            ctx.getWrapFactory().setJavaPrimitiveWrap(false);
            ScriptableObject.putProperty(scope, "Engine", engineApi);
            ScriptableObject.putProperty(scope, "Html", htmlApi);

            // Test HTML extraction
            Object result =
                    ctx.evaluateString(
                            scope,
                            "var browser = Engine.newBrowser();"
                                    + "browser.launch('about:blank');"
                                    + "var doc = browser.html();"
                                    + "doc != null;",
                            "test",
                            1,
                            null);

            assertNotNull("html() should return JSDocument", result);
            assertTrue("Result should be true", result instanceof Boolean && (Boolean) result);

        } finally {
            if (ctx != null) {
                Context.exit();
            }
        }
    }

    @Test
    public void testBrowserChaining() {
        Context ctx = null;
        try {
            ctx = Context.enter();
            ctx.setOptimizationLevel(-1);
            ctx.setLanguageVersion(200);
            Scriptable scope = ctx.initStandardObjects();

            // Setup API
            Engine engineApi = new Engine(ctx, scope);
            ctx.getWrapFactory().setJavaPrimitiveWrap(false);
            ScriptableObject.putProperty(scope, "Engine", engineApi);

            // Test method chaining
            Object result =
                    ctx.evaluateString(
                            scope,
                            "var browser = Engine.newBrowser();"
                                    + "browser.launch('https://example.com')"
                                    + "       .sleep(100)"
                                    + "       .title();",
                            "test",
                            1,
                            null);

            assertNotNull("Chaining should work", result);
            assertEquals("Title should be available", "Example Domain", result);

        } finally {
            if (ctx != null) {
                Context.exit();
            }
        }
    }

    @Test
    public void testBrowserClose() {
        Context ctx = null;
        try {
            ctx = Context.enter();
            ctx.setOptimizationLevel(-1);
            ctx.setLanguageVersion(200);
            Scriptable scope = ctx.initStandardObjects();

            // Setup API
            Engine engineApi = new Engine(ctx, scope);
            ctx.getWrapFactory().setJavaPrimitiveWrap(false);
            ScriptableObject.putProperty(scope, "Engine", engineApi);

            // Test browser close
            Object result =
                    ctx.evaluateString(
                            scope,
                            "var browser = Engine.newBrowser();"
                                    + "browser.launch('about:blank');"
                                    + "browser.close();"
                                    + "browser.isActive();",
                            "test",
                            1,
                            null);

            assertNotNull("isActive() should return false", result);
            assertTrue(
                    "Browser should not be active after close",
                    result instanceof Boolean && !(Boolean) result);

        } finally {
            if (ctx != null) {
                Context.exit();
            }
        }
    }
}
