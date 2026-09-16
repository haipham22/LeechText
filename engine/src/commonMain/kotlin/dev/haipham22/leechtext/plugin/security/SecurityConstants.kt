package dev.haipham22.leechtext.plugin.security

/**
 * Constants bảo mật tập trung cho plugin validation/sandboxing (port từ
 * security/SecurityConstants.java). Single source of truth cho API/class được phép.
 */
object SecurityConstants {
    /** Class prefix nguy hiểm — blocked. */
    val BLOCKED_CLASS_PREFIXES =
        arrayOf(
            "java.lang.Runtime",
            "java.lang.ProcessBuilder",
            "java.io.File",
            "java.lang.System",
            "java.lang.reflect",
            "java.net.Socket",
            "java.net.ServerSocket",
            "java.nio.file",
        )

    /** Class prefix an toàn — allowed. */
    val ALLOWED_CLASS_PREFIXES =
        arrayOf(
            "java.lang.String",
            "java.lang.Integer",
            "java.lang.Long",
            "java.lang.Double",
            "java.lang.Boolean",
            "java.lang.Object",
            "java.util",
            "java.math",
        )

    /** API prefix an toàn cho plugin scripts. */
    val ALLOWED_API_PREFIXES =
        arrayOf(
            "Http.get",
            "Http.post",
            "Http.request",
            "Html.select",
            "Html.parse",
            "Text.clean",
            "Text.trim",
            "Text.removeTag",
            "Log.add",
            "Util.base64Encode",
            "Util.base64Decode",
            "Util.md5",
            "Util.sha256",
            "Util.sha512",
            "JSON.parse",
            "JSON.stringify",
        )

    /** Class name có được phép không (block → allow → default deny). */
    fun isClassAllowed(className: String): Boolean {
        // Block class nguy hiểm
        for (blocked in BLOCKED_CLASS_PREFIXES) {
            if (className.startsWith(blocked)) return false
        }
        // Allow class an toàn
        for (allowed in ALLOWED_CLASS_PREFIXES) {
            if (className.startsWith(allowed)) return true
        }
        // Default deny
        return false
    }

    /** API call có được phép không (match exact hoặc prefix + "."). */
    fun isApiAllowed(apiName: String?): Boolean {
        if (apiName.isNullOrEmpty()) return false
        for (allowed in ALLOWED_API_PREFIXES) {
            if (apiName == allowed || apiName.startsWith("$allowed.")) return true
        }
        return false
    }
}
