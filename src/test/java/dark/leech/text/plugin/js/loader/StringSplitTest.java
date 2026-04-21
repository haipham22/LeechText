package dark.leech.text.plugin.js.loader;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import static org.junit.Assert.*;

/**
 * Test that String.split() works correctly with regex special characters.
 * Verifies the fix for: PatternSyntaxException: Dangling meta character '?'
 */
public class StringSplitTest {

    @Test
    public void testStringSplitWithQuestionMark() {
        Context ctx = null;
        try {
            ctx = Context.enter();
            ctx.setOptimizationLevel(-1);
            ctx.setLanguageVersion(200);
            Scriptable scope = ctx.initStandardObjects();

            // Apply the fix: setJavaPrimitiveWrap(false)
            ctx.getWrapFactory().setJavaPrimitiveWrap(false);

            // Test split('?')
            Object result = ctx.evaluateString(scope,
                "var url = 'https://example.com/page?param=value'; url.split('?')[0];",
                "test", 1, null);

            assertNotNull("Result should not be null", result);
            assertEquals("Split should work correctly", "https://example.com/page", result);

        } finally {
            if (ctx != null) {
                Context.exit();
            }
        }
    }

    @Test
    public void testStringSplitWithMultipleSpecialChars() {
        Context ctx = null;
        try {
            ctx = Context.enter();
            ctx.setOptimizationLevel(-1);
            ctx.setLanguageVersion(200);
            Scriptable scope = ctx.initStandardObjects();

            // Apply the fix
            ctx.getWrapFactory().setJavaPrimitiveWrap(false);

            // Test split with various special characters
            String script = "" +
                "var results = [];\n" +
                "results.push('a.b.c'.split('.').length);\n" +
                "results.push('a*b*c'.split('*').length);\n" +
                "results.push('a+b+c'.split('+').length);\n" +
                "results.push('a?b?c'.split('?').length);\n" +
                "results.push('a$b$c'.split('$').length);\n" +
                "results.push('a|b|c'.split('|').length);\n" +
                "results.join(',');\n";

            Object result = ctx.evaluateString(scope, script, "test", 1, null);

            assertEquals("All splits should produce 3 elements", "3,3,3,3,3,3", result);

        } finally {
            if (ctx != null) {
                Context.exit();
            }
        }
    }

    @Test
    public void testStringSplitWithHash() {
        Context ctx = null;
        try {
            ctx = Context.enter();
            ctx.setOptimizationLevel(-1);
            ctx.setLanguageVersion(200);
            Scriptable scope = ctx.initStandardObjects();

            // Apply the fix
            ctx.getWrapFactory().setJavaPrimitiveWrap(false);

            // Test split('#')
            Object result = ctx.evaluateString(scope,
                "var url = 'https://example.com/page#section'; url.split('#')[0];",
                "test", 1, null);

            assertNotNull("Result should not be null", result);
            assertEquals("Split should work correctly", "https://example.com/page", result);

        } finally {
            if (ctx != null) {
                Context.exit();
            }
        }
    }

    @Test
    public void testStringSplitChaining() {
        Context ctx = null;
        try {
            ctx = Context.enter();
            ctx.setOptimizationLevel(-1);
            ctx.setLanguageVersion(200);
            Scriptable scope = ctx.initStandardObjects();

            // Apply the fix
            ctx.getWrapFactory().setJavaPrimitiveWrap(false);

            // Test the exact pattern used in plugin scripts
            Object result = ctx.evaluateString(scope,
                "var href = 'https://khotruyenchu.online/truyen/test?param=value#section';\n" +
                "var cleanUrl = href.split('?')[0].split('#')[0];\n" +
                "cleanUrl;",
                "test", 1, null);

            assertNotNull("Result should not be null", result);
            assertEquals("Chained split should work", "https://khotruyenchu.online/truyen/test", result);

        } finally {
            if (ctx != null) {
                Context.exit();
            }
        }
    }

    @Test
    public void testSplitWithFixApplied() {
        Context ctx = null;
        try {
            ctx = Context.enter();
            ctx.setOptimizationLevel(-1);
            ctx.setLanguageVersion(200);
            Scriptable scope = ctx.initStandardObjects();

            // Apply the fix: setJavaPrimitiveWrap(false)
            // This ensures Java String methods follow JavaScript semantics
            ctx.getWrapFactory().setJavaPrimitiveWrap(false);

            // Verify split works with regex special characters
            Object result = ctx.evaluateString(scope,
                "var url = 'https://example.com/page?param=value'; url.split('?')[0];",
                "test", 1, null);

            assertNotNull("Result should not be null", result);
            assertEquals("Split should work correctly", "https://example.com/page", result);

            // Test multiple special chars in chain
            Object chained = ctx.evaluateString(scope,
                "var href = 'https://example.com/page?p=v#section'; " +
                "href.split('?')[0].split('#')[0];",
                "test", 1, null);

            assertEquals("Chained split should work", "https://example.com/page", chained);

        } finally {
            if (ctx != null) {
                Context.exit();
            }
        }
    }
}
