package dev.haipham22.leechtext.plugin.js.api

import org.mozilla.javascript.Context

/**
 * Wrapper Context mượn từ pool (port từ RhinoPooledContext.java).
 */
class RhinoPooledContext(
    @JvmField val context: Context,
) {
    val timestamp: Long = System.currentTimeMillis()
}
