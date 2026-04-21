package dark.leech.text.plugin.js.loader;

/**
 * Wrapper for paginated responses from JavaScript plugins.
 * Used by GenLoader to capture both data and next page metadata
 * when Response.success(data, next) is called with two arguments.
 */
public class PaginatedResponse {

    private final Object data;
    private final Object next;

    public PaginatedResponse(Object data, Object next) {
        this.data = data != null ? data : new org.mozilla.javascript.NativeArray(0);
        this.next = next;
    }

    /**
     * Get the data payload (items list).
     */
    public Object getData() {
        return data;
    }

    /**
     * Get the next page identifier (page number, URL, or token).
     */
    public Object getNext() {
        return next;
    }
}
