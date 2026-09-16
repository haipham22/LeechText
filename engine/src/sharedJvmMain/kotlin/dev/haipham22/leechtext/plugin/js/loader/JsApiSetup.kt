package dev.haipham22.leechtext.plugin.js.loader

import dev.haipham22.leechtext.log.EngineLogger
import dev.haipham22.leechtext.plugin.js.api.Engine
import dev.haipham22.leechtext.plugin.js.api.Html
import dev.haipham22.leechtext.plugin.js.api.Http
import dev.haipham22.leechtext.plugin.js.api.JsValueBridge
import dev.haipham22.leechtext.plugin.js.api.Json
import dev.haipham22.leechtext.plugin.js.api.LocalStorage
import dev.haipham22.leechtext.plugin.js.api.UserAgent
import dev.haipham22.leechtext.util.parseBaseUrl
import org.mozilla.javascript.BaseFunction
import org.mozilla.javascript.Context
import org.mozilla.javascript.NativeObject
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.Undefined
import java.io.File
import java.nio.file.Files

/**
 * Setup vBook API bindings vào Rhino scope (port từ loader/JsApiSetup.java; P5.2c: bind
 * instance API common — Rhino wrap NativeJavaObject như cũ).
 * Đăng ký: BASE_URL, Html, Http, Json, UserAgent, Engine, Response, fetch, load, sleep,
 * console, localStorage, localConfig.
 */
object JsApiSetup {
    @Suppress("LongParameterList") // tham số log DI + params nghiệp vụ — bundle làm code khó đọc
    fun setup(
        ctx: Context,
        scope: Scriptable,
        baseUrl: String?,
        targetUrl: String?,
        pluginSource: String?,
        log: EngineLogger,
    ) {
        setup(ctx, scope, baseUrl, targetUrl, pluginSource, null, log)
    }

    @Suppress("LongParameterList") // tham số log DI + params nghiệp vụ — bundle làm code khó đọc
    fun setup(
        ctx: Context,
        scope: Scriptable,
        baseUrl: String?,
        targetUrl: String?,
        pluginSource: String?,
        pluginConfig: Map<String, String>?,
        log: EngineLogger,
    ) = setup(ctx, scope, baseUrl, targetUrl, pluginSource, pluginConfig, null, log)

    @Suppress("LongParameterList") // tham số log DI + params nghiệp vụ — bundle làm code khó đọc
    fun setup(
        ctx: Context,
        scope: Scriptable,
        baseUrl: String?,
        targetUrl: String?,
        pluginSource: String?,
        pluginConfig: Map<String, String>?,
        extraScripts: Map<String, String>?,
        log: EngineLogger,
    ) {
        injectBaseUrl(ctx, scope, baseUrl, extraScripts)
        injectPluginConfig(ctx, scope, pluginConfig)
        registerLocalConfig(ctx, scope, pluginConfig)

        ScriptableObject.putProperty(scope, "Html", Html(log))
        ctx.wrapFactory.isJavaPrimitiveWrap = false
        ScriptableObject.putProperty(scope, "Http", Http(log))
        ctx.wrapFactory.isJavaPrimitiveWrap = false
        ScriptableObject.putProperty(scope, "Json", Json(log))
        ctx.wrapFactory.isJavaPrimitiveWrap = false
        ScriptableObject.putProperty(scope, "UserAgent", UserAgent())
        ctx.wrapFactory.isJavaPrimitiveWrap = false
        ScriptableObject.putProperty(scope, "Engine", Engine())
        ctx.wrapFactory.isJavaPrimitiveWrap = false
        ScriptableObject.putProperty(scope, "Response", Response())

        // fetch() — trả Http object cho chaining
        ScriptableObject.putProperty(scope, "fetch", createFetchFunc(targetUrl, log))
        ctx.wrapFactory.isJavaPrimitiveWrap = false

        // load() — đọc JS file từ plugin dir (có path-traversal guard, giới hạn 100KB)
        ScriptableObject.putProperty(scope, "load", createLoadFunc(pluginSource, extraScripts, log))
        ctx.wrapFactory.isJavaPrimitiveWrap = false

        // sleep(ms)
        ScriptableObject.putProperty(scope, "sleep", createSleepFunc())
        ctx.wrapFactory.isJavaPrimitiveWrap = false

        // Console API — expose cùng object cho Console + console (match vBooks)
        val consoleApi = Context.javaToJS(ConsoleApi(log), scope)
        ScriptableObject.putProperty(scope, "Console", consoleApi)
        ScriptableObject.putProperty(scope, "console", consoleApi)
        ctx.wrapFactory.isJavaPrimitiveWrap = false

        // LocalStorage
        ScriptableObject.putProperty(
            scope,
            "localStorage",
            LocalStorage(log, pluginSource ?: "default"),
        )
        ctx.wrapFactory.isJavaPrimitiveWrap = false
    }

    /** Inject BASE_URL vào scope làm JS string primitive (match vBooks). */
    private fun injectBaseUrl(
        ctx: Context,
        scope: Scriptable,
        baseUrl: String?,
        extraScripts: Map<String, String>?,
    ) {
        // Dùng evaluateString để guarantee JS string type — putProperty với Java String
        // đôi khi bị wrap làm NativeJavaObject, stringify ra rác (": NOT_FOUND").
        // Plugin có config.js (extraScripts) thì BỎ inject — config tự const BASE_URL
        // đúng gốc (plugin.source có thể lệch path, vd /reader nhân đôi URL).
        val hasConfigScript = extraScripts?.containsKey("config.js") == true
        if (baseUrl != null && !hasConfigScript) {
            val escaped =
                baseUrl
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
            ctx.evaluateString(scope, "var BASE_URL = \"$escaped\";", "baseUrl", 1, null)
        }
        ctx.wrapFactory.isJavaPrimitiveWrap = false
    }

    /** Inject plugin config keys làm JS constants (vBook extension-api). */
    private fun injectPluginConfig(
        ctx: Context,
        scope: Scriptable,
        pluginConfig: Map<String, String>?,
    ) {
        // vd config DOMAIN default "https://x.com" → const DOMAIN = "https://x.com"
        pluginConfig?.forEach { (key, value) ->
            if (key.isNotEmpty() && key.matches(Regex("[A-Za-z_][A-Za-z0-9_]*"))) {
                ScriptableObject.putProperty(scope, key, value)
                ctx.wrapFactory.isJavaPrimitiveWrap = false
            }
        }
    }

    /** localConfig API — getItem/setItem cho dynamic access (vBook extension-api). */
    private fun registerLocalConfig(
        ctx: Context,
        scope: Scriptable,
        pluginConfig: Map<String, String>?,
    ) {
        val localConfigObj =
            object : BaseFunction() {
                override fun call(
                    cx: Context,
                    sc: Scriptable,
                    thisObj: Scriptable,
                    args: Array<Any?>,
                ): Any? {
                    return null // getItem/setItem handled qua get
                }

                override fun get(
                    name: String,
                    start: Scriptable,
                ): Any {
                    if (name == "getItem") {
                        return object : BaseFunction() {
                            override fun call(
                                cx: Context,
                                sc: Scriptable,
                                th: Scriptable,
                                args: Array<Any?>,
                            ): Any? {
                                val key = args.getOrNull(0)?.toString() ?: return null
                                return pluginConfig?.get(key)
                            }
                        }
                    }
                    if (name == "setItem") {
                        return object : BaseFunction() {
                            override fun call(
                                cx: Context,
                                sc: Scriptable,
                                th: Scriptable,
                                args: Array<Any?>,
                            ): Any? {
                                // Read-only trong engine — không persist config change từ script
                                return null
                            }
                        }
                    }
                    return super.get(name, start)
                }
            }
        ScriptableObject.putProperty(scope, "localConfig", localConfigObj)
        ctx.wrapFactory.isJavaPrimitiveWrap = false
    }

    /** fetch() — trả Http object cho chaining, hỗ trợ option {method, body, headers, queries}. */
    private fun createFetchFunc(
        targetUrl: String?,
        log: EngineLogger,
    ): BaseFunction = object : BaseFunction() {
        override fun call(
            cx: Context,
            scope: Scriptable,
            thisObj: Scriptable,
            args: Array<Any?>,
        ): Any {
            var requestUrl = resolveRequestUrl(args, targetUrl)
            val opts = args.getOrNull(1) as? NativeObject
            val hasOptions = opts != null && listOf("method", "body", "headers", "queries").any { opts.has(it, opts) }
            if (opts != null && !hasOptions) {
                // Hành vi cũ: object trần (không key option) = query string
                requestUrl = appendQueryParams(requestUrl, opts)
            }
            log.add("[fetch()] $requestUrl")
            val http = Http(log).request(requestUrl)
            if (opts != null && hasOptions) {
                applyFetchOptions(http, opts)
            }
            return http
        }

        override fun getDefaultValue(typeHint: Class<*>?): Any = "function fetch() { [native code] }"
    }

    /** vBook extension-api: fetch(url, {method, body, headers, queries}) — apply option lên Http. */
    private fun applyFetchOptions(
        http: Http,
        opts: NativeObject,
    ) {
        val queries = opts.get("queries", opts)
        if (queries is Map<*, *>) http.queries(queries)
        val method = opts.get("method", opts)
        if (method is String) http.method(method)
        val headers = opts.get("headers", opts)
        if (headers is Map<*, *>) {
            for ((k, v) in headers) {
                if (k is String) http.header(k, v?.toString() ?: "")
            }
        }
        val body = opts.get("body", opts)
        when (body) {
            // object → form-encoded (vd ajax POST)
            is Map<*, *> -> http.form(body)

            is String -> http.body(body)
        }
    }

    /** Xác định URL request: arg đầu tiên hoặc fallback targetUrl. */
    private fun resolveRequestUrl(
        args: Array<Any?>,
        targetUrl: String?,
    ): String = if (args.isNotEmpty() && args[0] != null && args[0] !== Undefined.instance) {
        Context.toString(args[0])
    } else {
        targetUrl ?: ""
    }

    /** Append query string từ option {queries: {k: v}} vào URL. */
    private fun appendQueryParams(
        requestUrl: String,
        opts: NativeObject,
    ): String {
        val queries = opts.get("queries", opts)
        if (queries is NativeObject) {
            val pairs =
                queries.keys.mapNotNull { key ->
                    val k = key as? String ?: return@mapNotNull null
                    val v = queries.get(k, queries) ?: return@mapNotNull null
                    "$k=" +
                        java.net.URLEncoder.encode(
                            Context.toString(v),
                            Charsets.UTF_8.name(),
                        )
                }
            if (pairs.isNotEmpty()) {
                return requestUrl + (if (requestUrl.contains("?")) "&" else "?") + pairs.joinToString("&")
            }
        }
        return requestUrl
    }

    /** load() — đọc JS file từ plugin dir (có path-traversal guard, giới hạn 100KB). */
    private fun createLoadFunc(
        pluginSource: String?,
        extraScripts: Map<String, String>?,
        log: EngineLogger,
    ): BaseFunction = object : BaseFunction() {
        @Suppress("ReturnCount") // guard-chain validation trong BaseFunction override
        override fun call(
            cx: Context,
            scope: Scriptable,
            thisObj: Scriptable,
            args: Array<Any?>,
        ): Any? {
            val fileName = resolveLoadFileName(args)

            if (fileName.isEmpty()) {
                log.add("[load()] File name is empty")
                return null
            }
            if (!isValidFileName(fileName)) {
                log.add("[load()] Invalid file name: $fileName")
                return null
            }
            // In-memory trước — script src/*.js đã extract trong plugin zip
            // (load('config.js') resolve từ extraScripts, không cần disk)
            extraScripts?.get(fileName)?.let { content ->
                return evalExtraScript(cx, scope, fileName, content, log)
            }
            if (pluginSource.isNullOrEmpty()) {
                log.add("[load()] Plugin source directory not set")
                return null
            }

            return loadFromDisk(cx, scope, pluginSource, fileName, log)
        }

        override fun getDefaultValue(typeHint: Class<*>?): Any = "function load() { [native code] }"
    }

    /** Lấy file name từ arg đầu tiên của load(). */
    private fun resolveLoadFileName(args: Array<Any?>): String {
        if (args.isEmpty() || args[0] == null || args[0] === Undefined.instance) return ""
        return JsValueBridge.toJsString(args[0]) ?: ""
    }

    /** Eval script in-memory từ extraScripts; null khi lỗi eval. */
    @Suppress("LongParameterList") // tham số log DI + params nghiệp vụ Rhino
    private fun evalExtraScript(
        cx: Context,
        scope: Scriptable,
        fileName: String,
        content: String,
        log: EngineLogger,
    ): Any? = try {
        cx.evaluateString(scope, content, fileName, 1, null)
    } catch (e: Exception) {
        log.add("[load()] In-memory eval error $fileName: ${e.message}")
        null
    }

    /** Đọc + eval file từ đĩa (path-traversal guard, giới hạn 100KB); null khi lỗi. */
    @Suppress("ReturnCount", "LongParameterList") // security guards + tham số log DI — chain guard cần thoát sớm
    private fun loadFromDisk(
        cx: Context,
        scope: Scriptable,
        pluginSource: String,
        fileName: String,
        log: EngineLogger,
    ): Any? {
        return try {
            val file = File(pluginSource, "src/$fileName")
            if (!file.exists() || !file.isFile) {
                log.add("[load()] File not found: ${file.path}")
                return null
            }

            // Security: không thoát khỏi plugin dir
            val pluginDir = File(pluginSource, "src")
            if (!file.canonicalPath.startsWith(pluginDir.canonicalPath)) {
                log.add("[load()] Path traversal detected: $fileName")
                return null
            }

            // Security: giới hạn kích thước file
            if (file.length() > 100L * 1024) {
                log.add("[load()] File too large: ${file.length()} bytes")
                return null
            }

            val content = String(Files.readAllBytes(file.toPath()))
            cx.evaluateString(scope, content, fileName, 1, null)
        } catch (e: Exception) {
            log.add("[load()] Failed to load file: ${e.message}")
            null
        }
    }

    /** sleep(ms) — block thread hiện tại, interrupt-safe. */
    private fun createSleepFunc(): BaseFunction = object : BaseFunction() {
        override fun call(
            cx: Context,
            scope: Scriptable,
            thisObj: Scriptable,
            args: Array<Any?>,
        ): Any? {
            val millis = if (args.isNotEmpty() && args[0] is Number) (args[0] as Number).toInt() else 0
            return try {
                Thread.sleep(millis.toLong())
                null
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                null
            }
        }

        override fun getDefaultValue(typeHint: Class<*>?): Any = "function sleep() { [native code] }"
    }

    /** Extract protocol://host[:port] từ URL đầy đủ. */
    fun extractBaseUrl(url: String): String = parseBaseUrl(url)

    /** Validate file name: không path traversal, chỉ .js, không ký tự lạ. */
    private fun isValidFileName(fileName: String): Boolean {
        if (fileName.isEmpty()) return false
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) return false
        if (fileName.startsWith("/") || fileName.startsWith("\\")) return false
        if (!fileName.endsWith(".js")) return false
        if (fileName.matches(Regex(".*[<>:\"|?*].*"))) return false
        return true
    }
}
