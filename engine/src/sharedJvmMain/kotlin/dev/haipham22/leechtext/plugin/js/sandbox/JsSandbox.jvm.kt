package dev.haipham22.leechtext.plugin.js.sandbox

import dev.haipham22.leechtext.log.EngineLogger
import dev.haipham22.leechtext.plugin.js.api.RhinoValues
import dev.haipham22.leechtext.plugin.js.loader.JsApiSetup
import dev.haipham22.leechtext.plugin.js.loader.LoaderType
import org.mozilla.javascript.Context
import org.mozilla.javascript.ContextFactory
import org.mozilla.javascript.Function
import org.mozilla.javascript.Scriptable

/**
 * Rhino actual của JsSandbox (P5.2c) — behavior giữ nguyên bản JsSandbox.java port:
 * Context.enter/exit theo lifecycle, interpretation mode, FEATURE_ENABLE_JAVA_MAP_ACCESS
 * (Map trả từ Kotlin API xuống JS access được property), kết quả callFunction normalize
 * qua RhinoValues để loaders common không thấy type Rhino.
 */
actual class JsSandbox internal actual constructor(
    private val log: dev.haipham22.leechtext.log.EngineLogger,
    config: dev.haipham22.leechtext.plugin.js.sandbox.SandboxConfig,
) {
    @JvmField
    val context: Context

    @JvmField
    val scope: Scriptable

    companion object {
        /**
         * Factory bật FEATURE_ENABLE_JAVA_MAP_ACCESS — Map trả từ Kotlin common API xuống
         * JS (Response.success, Http.json wrap...) access được property dot (`res.data`).
         */
        val FACTORY: ContextFactory =
            object : ContextFactory() {
                override fun hasFeature(
                    cx: Context,
                    featureIndex: Int,
                ): Boolean = featureIndex == Context.FEATURE_ENABLE_JAVA_MAP_ACCESS || super.hasFeature(cx, featureIndex)
            }
    }

    private val loaderType: LoaderType = requireNotNull(config.loaderType)
    private val baseUrl: String? = config.baseUrl
    private val targetUrl: String? = config.targetUrl
    private val pluginConfig: Map<String, String>? = config.pluginConfig
    private val extraScripts: Map<String, String>? = config.extraScripts

    init {
        // Enter Rhino Context (factory bật map-access feature)
        context = FACTORY.enterContext()
        context.optimizationLevel = -1 // interpretation mode cho security
        context.languageVersion = 200 // ES6 cho vBook plugins
        context.wrapFactory.isJavaPrimitiveWrap = false // match vBooks

        // Isolated scope
        scope = context.initStandardObjects()

        // Secure API surface
        JsApiSetup.setup(context, scope, baseUrl, targetUrl, config.pluginSource, pluginConfig, extraScripts, log)
        addSafeToStringWrapper()

        log.add("[VBookSandbox] Created ${loaderType.type} sandbox")
    }

    actual fun execute(
        script: String,
        scriptName: String,
    ): Boolean {
        return try {
            context.evaluateString(scope, script, scriptName, 1, null)
            true
        } catch (e: Exception) {
            val errorMsg = e.message ?: e.javaClass.name
            if (errorMsg.contains("toString")) {
                log.add("[VBookSandbox] Suppressed toString() error in $scriptName")
                return false
            }
            log.add("[VBookSandbox] JavaScript execution error in $scriptName: $errorMsg")
            false
        }
    }

    actual fun callFunction(
        functionName: String,
        vararg args: Any?,
    ): Any? {
        return try {
            val functionObj = scope[functionName, scope]
            if (functionObj !is Function) {
                log.add("[VBookSandbox] Function not found: $functionName")
                return null
            }
            RhinoValues.normalize(functionObj.call(context, scope, scope, args))
        } catch (e: Exception) {
            val errorMsg = e.message ?: e.javaClass.name
            if (errorMsg.contains("toString")) {
                log.add("[VBookSandbox] Suppressed toString() error in $functionName")
                return null
            }
            log.add("[VBookSandbox] Function call error in $functionName: $errorMsg")
            null
        }
    }

    actual fun close() {
        Context.exit()
        log.add("[VBoxSandbox] Closed ${loaderType.type} sandbox")
    }

    private fun addSafeToStringWrapper() {
        val safeToStringScript =
            "(function(){" +
                "const originalFunctionToString = Function.prototype.toString;" +
                "Function.prototype.toString = function(){" +
                "try{return originalFunctionToString.call(this);}catch(e){return 'function () { [native code] }';}" +
                "};" +
                "const originalObjectToString = Object.prototype.toString;" +
                "Object.prototype.toString = function(){" +
                "try{return originalObjectToString.call(this);}catch(e){return '[object Object]';}" +
                "};" +
                "})();"
        try {
            context.evaluateString(scope, safeToStringScript, "safeToString", 1, null)
        } catch (e: Exception) {
            log.add("[VBookSandbox] Failed to add toString wrapper: ${e.message}")
        }
    }
}

/** JVM actual factory — Rhino sandbox. */
actual fun createSandbox(
    log: EngineLogger,
    config: SandboxConfig.() -> Unit,
): JsSandbox {
    val cfg = SandboxConfig().apply(config)
    return JsSandbox(log, cfg)
}
