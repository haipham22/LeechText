package dark.leech.text.plugin.js.loader;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * Response class for vBook JavaScript API using Rhino. Provides success() method to return data
 * from plugin scripts.
 */
public class Response {

    /**
     * Creates a new Response instance.
     *
     * @param context The Rhino context (kept for compatibility, not used)
     * @param scope The Rhino scope (kept for compatibility, not used)
     */
    public Response(Context context, Scriptable scope) {
        // Context and scope not used - kept for constructor compatibility
    }

    /** Legacy constructor - kept for compatibility. */
    public Response() {
        // No-op
    }

    /**
     * Success method - returns the provided data. Called from JavaScript as Response.success(data).
     * For vBooks compatibility, wraps in {code: 0, data: ...} structure.
     *
     * @param data The data to return
     * @return NativeObject with code and data
     */
    public Object success(Object data) {
        org.mozilla.javascript.NativeObject result = new org.mozilla.javascript.NativeObject();
        result.put("code", result, 0);
        result.put("data", result, data);
        return result;
    }

    /**
     * Success method with dual data support - returns both data values. Called from JavaScript as
     * Response.success(data, data2). Used for pagination and metadata extraction. For vBooks
     * compatibility, wraps in {code: 0, data: ..., data2: ...} structure.
     *
     * @param data The primary data
     * @param data2 The secondary data (e.g., next page token)
     * @return NativeObject containing code, data, and data2
     */
    public Object success(Object data, Object data2) {
        org.mozilla.javascript.NativeObject result = new org.mozilla.javascript.NativeObject();
        result.put("code", result, 0);
        result.put("data", result, data);
        result.put("data2", result, data2);
        return result;
    }

    /**
     * Error method - returns error with default error code 1. Called from JavaScript as
     * Response.error(data). For vBooks compatibility, wraps in {code: 1, data2: ...} structure.
     *
     * @param data The error data
     * @return NativeObject with code and data2
     */
    public Object error(Object data) {
        org.mozilla.javascript.NativeObject result = new org.mozilla.javascript.NativeObject();
        result.put("code", result, 1);
        result.put("data2", result, data);
        return result;
    }

    /**
     * Error method with custom error code. Called from JavaScript as Response.error(code, data).
     * For vBooks compatibility, wraps in {code: code, data2: ...} structure.
     *
     * @param code The error code
     * @param data The error data
     * @return NativeObject with code and data2
     */
    public Object error(int code, Object data) {
        org.mozilla.javascript.NativeObject result = new org.mozilla.javascript.NativeObject();
        result.put("code", result, code);
        result.put("data2", result, data);
        return result;
    }

    /**
     * Error method with custom error code and message. Called from JavaScript as
     * Response.error(code, data, message).
     *
     * @param code The error code
     * @param data The error data
     * @param message The error message
     * @return NativeObject with code, data2, and message
     */
    public Object error(int code, Object data, String message) {
        org.mozilla.javascript.NativeObject result = new org.mozilla.javascript.NativeObject();
        result.put("code", result, code);
        result.put("data2", result, data);
        result.put("message", result, message);
        return result;
    }

    // ========== Static Utility Methods for Response Unwrapping ==========

    /**
     * Check if a result object is a success Response. Returns true if result is null, not a
     * Response object, or is a Response with code == 0.
     *
     * @param result The result object from JavaScript execution
     * @return true if successful or not a Response, false if error Response
     */
    public static boolean isSuccess(Object result) {
        if (!(result instanceof org.mozilla.javascript.NativeObject)) {
            return true; // Not a Response object, treat as raw data
        }
        org.mozilla.javascript.NativeObject obj = (org.mozilla.javascript.NativeObject) result;
        if (!obj.has("code", obj)) {
            return true; // No code field, treat as raw data
        }
        Object codeProp = obj.get("code", obj);
        if (!(codeProp instanceof Number)) {
            return true; // Invalid code field, treat as raw data
        }
        int code = ((Number) codeProp).intValue();
        return code == 0;
    }

    /**
     * Extract data from a Response object. Returns the data field if successful Response, or the
     * original object if not a Response.
     *
     * @param result The result object from JavaScript execution
     * @return The extracted data, or null if result is null
     */
    public static Object getData(Object result) {
        if (result == null) {
            return null;
        }
        if (!(result instanceof org.mozilla.javascript.NativeObject)) {
            return result; // Not a Response object, return as-is
        }
        org.mozilla.javascript.NativeObject obj = (org.mozilla.javascript.NativeObject) result;
        if (!obj.has("code", obj)) {
            // Check for dual data format (backward compatibility)
            if (obj.has("data", obj) && obj.has("data2", obj)) {
                return obj.get("data", obj);
            }
            return result; // No code or dual data field, treat as raw data
        }
        Object codeProp = obj.get("code", obj);
        if (!(codeProp instanceof Number)) {
            return result; // Invalid code field, treat as raw data
        }
        int code = ((Number) codeProp).intValue();
        if (code != 0) {
            return null; // Error response, no data
        }
        return obj.get("data", obj); // Success response, extract data
    }

    /**
     * Extract error message from a Response object. Returns the data2 field if error Response, or
     * null if not an error.
     *
     * @param result The result object from JavaScript execution
     * @return The error message, or null if not an error Response
     */
    public static String getErrorMessage(Object result) {
        if (!(result instanceof org.mozilla.javascript.NativeObject)) {
            return null;
        }
        org.mozilla.javascript.NativeObject obj = (org.mozilla.javascript.NativeObject) result;
        if (!obj.has("code", obj)) {
            return null; // No code field, not a Response
        }
        Object codeProp = obj.get("code", obj);
        if (!(codeProp instanceof Number)) {
            return null; // Invalid code field
        }
        int code = ((Number) codeProp).intValue();
        if (code == 0) {
            return null; // Success response, no error
        }
        Object errorData = obj.get("data2", obj);
        return errorData != null ? JSResponse.getString(errorData) : "Unknown error";
    }

    /**
     * Extract data2 field from a Response object (used for pagination tokens, etc.).
     *
     * @param result The result object from JavaScript execution
     * @return The data2 value, or null if not present
     */
    public static Object getData2(Object result) {
        if (!(result instanceof org.mozilla.javascript.NativeObject)) {
            return null;
        }
        org.mozilla.javascript.NativeObject obj = (org.mozilla.javascript.NativeObject) result;
        if (!obj.has("code", obj)) {
            // Check for dual data format (backward compatibility)
            if (obj.has("data", obj) && obj.has("data2", obj)) {
                return obj.get("data2", obj);
            }
            return null;
        }
        Object codeProp = obj.get("code", obj);
        if (!(codeProp instanceof Number)) {
            return null;
        }
        int code = ((Number) codeProp).intValue();
        if (code != 0) {
            return null; // Error response, no data2
        }
        return obj.get("data2", obj); // Success response, extract data2
    }
}
