package dev.haipham22.leechtext.plugin.security

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SecurityConstantsTest {
    @Test
    fun dangerousClassBlockedByPrefix() {
        listOf(
            "java.lang.Runtime",
            "java.lang.Runtime.getRuntime",
            "java.lang.ProcessBuilder",
            "java.io.File",
            "java.io.FileInputStream",
            "java.lang.System",
            "java.lang.reflect.Method",
            "java.net.Socket",
            "java.net.ServerSocket",
            "java.nio.file.Path",
        ).forEach { assertFalse(SecurityConstants.isClassAllowed(it), "phải chặn $it") }
    }

    @Test
    fun safeClassAllowed() {
        listOf(
            "java.lang.String",
            "java.lang.Integer",
            "java.lang.Long",
            "java.lang.Double",
            "java.lang.Boolean",
            "java.lang.Object",
            "java.util.ArrayList",
            "java.util.HashMap",
            "java.math.BigInteger",
        ).forEach { assertTrue(SecurityConstants.isClassAllowed(it), "phải cho $it") }
    }

    @Test
    fun classOutsideWhitelistDeniedByDefault() {
        listOf("java.awt.Frame", "javax.script.ScriptEngine", "kotlin.io.File", "com.evil.Hack")
            .forEach { assertFalse(SecurityConstants.isClassAllowed(it), "phải deny $it") }
    }

    @Test
    fun apiAllowedByExactMatchAndChildPrefix() {
        assertTrue(SecurityConstants.isApiAllowed("Http.get"))
        assertTrue(SecurityConstants.isApiAllowed("Http.post"))
        assertTrue(SecurityConstants.isApiAllowed("Http.request"))
        assertTrue(SecurityConstants.isApiAllowed("Html.select"))
        assertTrue(SecurityConstants.isApiAllowed("Text.clean"))
        assertTrue(SecurityConstants.isApiAllowed("Util.md5"))
        assertTrue(SecurityConstants.isApiAllowed("JSON.parse"))
        // Prefix + "." — method mở rộng của API hợp lệ
        assertTrue(SecurityConstants.isApiAllowed("Http.get.url"))
        assertTrue(SecurityConstants.isApiAllowed("Util.md5.hex"))
    }

    @Test
    fun unknownOrEmptyApiBlocked() {
        assertFalse(SecurityConstants.isApiAllowed("Http.delete"))
        assertFalse(SecurityConstants.isApiAllowed("eval"))
        assertFalse(SecurityConstants.isApiAllowed("Java.exit"))
        assertFalse(SecurityConstants.isApiAllowed(""))
        assertFalse(SecurityConstants.isApiAllowed(null))
        // Prefix không khớp exact — "Http.g" không phải API hợp lệ
        assertFalse(SecurityConstants.isApiAllowed("Http.g"))
    }
}
