package dark.leech.text.plugin.js.api;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import dark.leech.text.action.Log;

/**
 * Pool of Rhino JavaScript contexts with resource limits and lifecycle management. Simplified
 * version to avoid race conditions.
 */
public class RhinoContextPool {

    private final LinkedBlockingQueue<RhinoPooledContext> pool;
    private final AtomicInteger activeContexts;
    private final AtomicInteger createdContexts;
    private final int maxContexts;
    private final int maxMemoryMB;
    private final int timeoutMs;

    /**
     * Creates a new context pool with specified limits.
     *
     * @param maxContexts Maximum number of contexts to create
     * @param timeoutMs Execution timeout in milliseconds
     * @param maxMemoryMB Memory limit per context in MB
     */
    public RhinoContextPool(int maxContexts, int timeoutMs, int maxMemoryMB) {
        this.maxContexts = maxContexts;
        this.timeoutMs = timeoutMs;
        this.maxMemoryMB = maxMemoryMB;
        this.activeContexts = new AtomicInteger(0);
        this.createdContexts = new AtomicInteger(0);
        this.pool = new LinkedBlockingQueue<>(maxContexts);

        Log.add(
                "RhinoContextPool initialized: maxContexts="
                        + maxContexts
                        + ", timeout="
                        + timeoutMs
                        + "ms, memory="
                        + maxMemoryMB
                        + "MB");
    }

    /**
     * Borrows a context from the pool, creating a new one if needed and within limits.
     *
     * @return A pooled context wrapper
     * @throws ResourceExhaustedException if no context is available within timeout
     */
    public RhinoPooledContext borrowContext() throws ResourceExhaustedException {
        // Try to get from pool first
        RhinoPooledContext context = pool.poll();
        if (context != null) {
            return context;
        }

        // Try to create new context
        int current = activeContexts.get();
        if (current < maxContexts) {
            activeContexts.incrementAndGet();
            try {
                return createNewContext();
            } catch (RuntimeException e) {
                // Failed to create, decrement
                activeContexts.decrementAndGet();
                // Fall through to wait
            }
        }

        // Wait for available context
        try {
            context = pool.poll(5, TimeUnit.SECONDS);
            if (context == null) {
                throw new ResourceExhaustedException(
                        "Timeout waiting for available JavaScript context. "
                                + "Current active: "
                                + activeContexts.get()
                                + "/"
                                + maxContexts);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResourceExhaustedException("Interrupted while waiting for context");
        }

        return context;
    }

    /**
     * Returns a context to the pool after resetting its state.
     *
     * @param pooledContext The context to return
     */
    public void returnContext(RhinoPooledContext pooledContext) {
        if (pooledContext == null) {
            return;
        }

        try {
            Context context = pooledContext.getContext();
            if (context == null) {
                return;
            }

            // Reset context state
            try {
                Scriptable scope = context.initStandardObjects();

                // Re-initialize basic APIs
                initializeApis(context, scope);

                // Try to return to pool
                if (!pool.offer(pooledContext)) {
                    // Pool full, close context
                    context.exit();
                    activeContexts.decrementAndGet();
                    createdContexts.decrementAndGet();
                }

            } catch (Exception e) {
                Log.add("Error returning context to pool: " + e.getMessage());
                try {
                    context.exit();
                } catch (Exception ex) {
                    // Ignore exit errors
                }
                activeContexts.decrementAndGet();
                createdContexts.decrementAndGet();
            }
        } catch (Exception e) {
            Log.add("Error in returnContext: " + e.getMessage());
        }
    }

    /**
     * Invalidates a context, preventing it from being reused. Used for contexts that have
     * experienced errors or timeouts.
     *
     * @param pooledContext The context to invalidate
     */
    public void invalidateContext(RhinoPooledContext pooledContext) {
        if (pooledContext == null) {
            return;
        }

        try {
            Context context = pooledContext.getContext();
            if (context != null) {
                context.exit();
            }
        } catch (Exception e) {
            // Ignore exit errors
        } finally {
            activeContexts.decrementAndGet();
            createdContexts.decrementAndGet();
        }
    }

    /**
     * Creates a new Rhino JavaScript context with resource limits.
     *
     * @return A new pooled context wrapper
     */
    private RhinoPooledContext createNewContext() {
        try {
            Context context = Context.enter();
            context.setOptimizationLevel(-1); // Use interpretation mode for security
            context.setMaximumInterpreterStackDepth(1000);

            // Initialize standard objects
            Scriptable scope = context.initStandardObjects();

            // Initialize APIs
            initializeApis(context, scope);

            RhinoPooledContext pooled = new RhinoPooledContext(context);
            createdContexts.incrementAndGet();

            Log.add(
                    "Created new Rhino context ("
                            + createdContexts.get()
                            + "/"
                            + maxContexts
                            + ")");

            return pooled;

        } catch (Exception e) {
            activeContexts.decrementAndGet();
            Context.exit();
            throw new RuntimeException("Failed to create JavaScript context", e);
        }
    }

    /**
     * Initializes basic JavaScript APIs for the context.
     *
     * @param context The context to initialize
     * @param scope The scope to initialize
     */
    private void initializeApis(Context context, Scriptable scope) {
        // Set up console object for logging
        try {
            context.evaluateString(
                    scope,
                    "const console = { "
                            + "log: function(msg) { print(msg); }, "
                            + "error: function(msg) { print('[ERROR] ' + msg); }, "
                            + "warn: function(msg) { print('[WARN] ' + msg); } "
                            + "};",
                    "console",
                    1,
                    null);
        } catch (Exception e) {
            Log.add("Failed to initialize console API: " + e.getMessage());
        }
    }

    /**
     * Gets the current number of active contexts.
     *
     * @return The active context count
     */
    public int getActiveCount() {
        return activeContexts.get();
    }

    /**
     * Gets the total number of contexts created.
     *
     * @return The total created context count
     */
    public int getCreatedCount() {
        return createdContexts.get();
    }

    /**
     * Gets the current number of available contexts in the pool.
     *
     * @return The available context count
     */
    public int getAvailableCount() {
        return pool.size();
    }

    /** Exception thrown when resources are exhausted. */
    public static class ResourceExhaustedException extends Exception {
        public ResourceExhaustedException(String message) {
            super(message);
        }
    }
}
