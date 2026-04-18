package dark.leech.text.plugin.security;

/**
 * Centralized security constants for plugin validation and sandboxing. Provides single source of
 * truth for allowed/blocked APIs and classes.
 */
public final class SecurityConstants {

    private SecurityConstants() {
        // Utility class - prevent instantiation
    }

    // Blocked dangerous class prefixes
    public static final String[] BLOCKED_CLASS_PREFIXES = {
        "java.lang.Runtime",
        "java.lang.ProcessBuilder",
        "java.io.File",
        "java.lang.System",
        "java.lang.reflect",
        "java.net.Socket",
        "java.net.ServerSocket",
        "java.nio.file"
    };

    // Allowed safe class prefixes
    public static final String[] ALLOWED_CLASS_PREFIXES = {
        "java.lang.String",
        "java.lang.Integer",
        "java.lang.Long",
        "java.lang.Double",
        "java.lang.Boolean",
        "java.lang.Object",
        "java.util",
        "java.math"
    };

    // Allowed safe API prefixes for plugin scripts
    public static final String[] ALLOWED_API_PREFIXES = {
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
        "JSON.stringify"
    };

    /**
     * Check if a class name is allowed.
     *
     * @param className Class name to check
     * @return true if allowed
     */
    public static boolean isClassAllowed(String className) {
        // Block dangerous classes
        for (String blocked : BLOCKED_CLASS_PREFIXES) {
            if (className.startsWith(blocked)) {
                return false;
            }
        }

        // Allow safe classes
        for (String allowed : ALLOWED_CLASS_PREFIXES) {
            if (className.startsWith(allowed)) {
                return true;
            }
        }

        // Default to deny
        return false;
    }

    /**
     * Check if an API call is allowed.
     *
     * @param apiName API name to check
     * @return true if allowed
     */
    public static boolean isApiAllowed(String apiName) {
        if (apiName == null || apiName.isEmpty()) {
            return false;
        }

        for (String allowed : ALLOWED_API_PREFIXES) {
            if (apiName.equals(allowed) || apiName.startsWith(allowed + ".")) {
                return true;
            }
        }
        return false;
    }
}
