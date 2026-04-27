package dark.leech.text.plugin.js.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/** Integration tests for LocalStorage API. Validates persistent key-value storage for plugins. */
public class LocalStorageTest {

    @Test
    public void testLocalStorageSetItem() {
        Context ctx = null;
        try {
            ctx = Context.enter();
            ctx.setOptimizationLevel(-1);
            ctx.setLanguageVersion(200);
            Scriptable scope = ctx.initStandardObjects();

            // Setup LocalStorage
            LocalStorage localStorageApi = new LocalStorage(ctx, scope, "test-plugin");
            ctx.getWrapFactory().setJavaPrimitiveWrap(false);
            ScriptableObject.putProperty(scope, "localStorage", localStorageApi);

            // Test setItem
            Object result =
                    ctx.evaluateString(
                            scope,
                            "localStorage.setItem('testKey', 'testValue');",
                            "test",
                            1,
                            null);

            assertNotNull("setItem should complete", result);

        } finally {
            if (ctx != null) {
                Context.exit();
            }
        }
    }

    @Test
    public void testLocalStorageGetItem() {
        Context ctx = null;
        try {
            ctx = Context.enter();
            ctx.setOptimizationLevel(-1);
            ctx.setLanguageVersion(200);
            Scriptable scope = ctx.initStandardObjects();

            // Setup LocalStorage
            LocalStorage localStorageApi = new LocalStorage(ctx, scope, "test-plugin");
            ctx.getWrapFactory().setJavaPrimitiveWrap(false);
            ScriptableObject.putProperty(scope, "localStorage", localStorageApi);

            // Test set and get
            ctx.evaluateString(scope, "localStorage.setItem('myKey', 'myValue');", "test", 1, null);

            Object result =
                    ctx.evaluateString(scope, "localStorage.getItem('myKey');", "test", 1, null);

            assertEquals("Should retrieve stored value", "myValue", result);

        } finally {
            if (ctx != null) {
                Context.exit();
            }
        }
    }

    @Test
    public void testLocalStorageRemoveItem() {
        Context ctx = null;
        try {
            ctx = Context.enter();
            ctx.setOptimizationLevel(-1);
            ctx.setLanguageVersion(200);
            Scriptable scope = ctx.initStandardObjects();

            // Setup LocalStorage
            LocalStorage localStorageApi = new LocalStorage(ctx, scope, "test-plugin");
            ctx.getWrapFactory().setJavaPrimitiveWrap(false);
            ScriptableObject.putProperty(scope, "localStorage", localStorageApi);

            // Clear any existing data
            ctx.evaluateString(scope, "localStorage.clear();", "test", 1, null);

            // Test remove
            ctx.evaluateString(
                    scope, "localStorage.setItem('tempKey', 'tempValue');", "test", 1, null);
            ctx.evaluateString(scope, "localStorage.removeItem('tempKey');", "test", 1, null);

            Object result =
                    ctx.evaluateString(scope, "localStorage.getItem('tempKey');", "test", 1, null);
            assertNull("Should return null after removal", result);

        } finally {
            if (ctx != null) {
                Context.exit();
            }
        }
    }

    @Test
    public void testLocalStorageClear() {
        Context ctx = null;
        try {
            ctx = Context.enter();
            ctx.setOptimizationLevel(-1);
            ctx.setLanguageVersion(200);
            Scriptable scope = ctx.initStandardObjects();

            // Setup LocalStorage
            LocalStorage localStorageApi = new LocalStorage(ctx, scope, "test-plugin");
            ctx.getWrapFactory().setJavaPrimitiveWrap(false);
            ScriptableObject.putProperty(scope, "localStorage", localStorageApi);

            // Test clear
            ctx.evaluateString(scope, "localStorage.setItem('key1', 'value1');", "test", 1, null);
            ctx.evaluateString(scope, "localStorage.setItem('key2', 'value2');", "test", 1, null);
            ctx.evaluateString(scope, "localStorage.clear();", "test", 1, null);

            Object length = ctx.evaluateString(scope, "localStorage.getLength();", "test", 1, null);
            assertTrue(
                    "Storage should be empty after clear",
                    length instanceof Number && ((Number) length).intValue() == 0);

        } finally {
            if (ctx != null) {
                Context.exit();
            }
        }
    }

    @Test
    public void testLocalStorageLength() {
        Context ctx = null;
        try {
            ctx = Context.enter();
            ctx.setOptimizationLevel(-1);
            ctx.setLanguageVersion(200);
            Scriptable scope = ctx.initStandardObjects();

            // Setup LocalStorage
            LocalStorage localStorageApi = new LocalStorage(ctx, scope, "test-plugin");
            ctx.getWrapFactory().setJavaPrimitiveWrap(false);
            ScriptableObject.putProperty(scope, "localStorage", localStorageApi);

            // Clear any existing data
            ctx.evaluateString(scope, "localStorage.clear();", "test", 1, null);

            // Test length
            ctx.evaluateString(scope, "localStorage.setItem('key1', 'value1');", "test", 1, null);
            ctx.evaluateString(scope, "localStorage.setItem('key2', 'value2');", "test", 1, null);
            ctx.evaluateString(scope, "localStorage.setItem('key3', 'value3');", "test", 1, null);

            Object length = ctx.evaluateString(scope, "localStorage.getLength();", "test", 1, null);
            assertTrue(
                    "Should have 3 items",
                    length instanceof Number && ((Number) length).intValue() == 3);

        } finally {
            if (ctx != null) {
                Context.exit();
            }
        }
    }

    @Test
    public void testLocalStorageKey() {
        Context ctx = null;
        try {
            ctx = Context.enter();
            ctx.setOptimizationLevel(-1);
            ctx.setLanguageVersion(200);
            Scriptable scope = ctx.initStandardObjects();

            // Setup LocalStorage
            LocalStorage localStorageApi = new LocalStorage(ctx, scope, "test-plugin");
            ctx.getWrapFactory().setJavaPrimitiveWrap(false);
            ScriptableObject.putProperty(scope, "localStorage", localStorageApi);

            // Clear any existing data
            ctx.evaluateString(scope, "localStorage.clear();", "test", 1, null);

            // Test key method
            ctx.evaluateString(scope, "localStorage.setItem('alpha', 'value1');", "test", 1, null);
            ctx.evaluateString(scope, "localStorage.setItem('beta', 'value2');", "test", 1, null);

            Object key = ctx.evaluateString(scope, "localStorage.getKey(0);", "test", 1, null);
            assertEquals("First key should be 'alpha'", "alpha", key);

        } finally {
            if (ctx != null) {
                Context.exit();
            }
        }
    }

    @Test
    public void testLocalStoragePersistenceAcrossInstances() {
        // First instance
        Context ctx1 = null;
        try {
            ctx1 = Context.enter();
            ctx1.setOptimizationLevel(-1);
            ctx1.setLanguageVersion(200);
            Scriptable scope1 = ctx1.initStandardObjects();

            LocalStorage localStorageApi1 = new LocalStorage(ctx1, scope1, "test-plugin");
            ctx1.getWrapFactory().setJavaPrimitiveWrap(false);
            ScriptableObject.putProperty(scope1, "localStorage", localStorageApi1);

            ctx1.evaluateString(
                    scope1, "localStorage.setItem('persistent', 'data');", "test", 1, null);

        } finally {
            if (ctx1 != null) {
                Context.exit();
            }
        }

        // Second instance (new context, same plugin ID)
        Context ctx2 = null;
        try {
            ctx2 = Context.enter();
            ctx2.setOptimizationLevel(-1);
            ctx2.setLanguageVersion(200);
            Scriptable scope2 = ctx2.initStandardObjects();

            LocalStorage localStorageApi2 = new LocalStorage(ctx2, scope2, "test-plugin");
            ctx2.getWrapFactory().setJavaPrimitiveWrap(false);
            ScriptableObject.putProperty(scope2, "localStorage", localStorageApi2);

            Object result =
                    ctx2.evaluateString(
                            scope2, "localStorage.getItem('persistent');", "test", 1, null);

            assertEquals("Data should persist across instances", "data", result);

        } finally {
            if (ctx2 != null) {
                Context.exit();
            }
        }
    }
}
