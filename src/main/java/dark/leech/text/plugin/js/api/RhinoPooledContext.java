package dark.leech.text.plugin.js.api;

import org.mozilla.javascript.Context;

/**
 * Wrapper for a Rhino Context that has been borrowed from the pool. Tracks the timestamp for
 * potential timeout management.
 */
public class RhinoPooledContext {
    private final Context context;
    private final long timestamp;

    public RhinoPooledContext(Context context) {
        this.context = context;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Gets the underlying Rhino Context.
     *
     * @return The Rhino Context
     */
    public Context getContext() {
        return context;
    }

    /**
     * Gets the timestamp when this context was borrowed.
     *
     * @return The timestamp in milliseconds
     */
    public long getTimestamp() {
        return timestamp;
    }
}
