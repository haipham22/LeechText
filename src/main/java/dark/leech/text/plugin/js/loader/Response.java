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
     *
     * @param data The data to return
     * @return The same data object
     */
    public Object success(Object data) {
        return data;
    }

    /**
     * Overloaded success method for two arguments (data, next). Used for pagination scenarios where
     * both data and next page token are returned. Wraps both values in PaginatedResponse.
     *
     * @param data The data to return (items list)
     * @param next The next page token (page number, URL, or token)
     * @return PaginatedResponse wrapper containing both data and next
     */
    public Object success(Object data, Object next) {
        return new PaginatedResponse(data, next);
    }
}
