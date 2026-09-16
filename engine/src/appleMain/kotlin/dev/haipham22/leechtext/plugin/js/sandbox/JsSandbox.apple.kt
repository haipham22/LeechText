@file:Suppress("MatchingDeclarationName")

package dev.haipham22.leechtext.plugin.js.sandbox

import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.JsObject
import com.dokar.quickjs.binding.function
import com.dokar.quickjs.binding.toJsObject
import com.fleeksoft.ksoup.Ksoup
import dev.haipham22.leechtext.log.EngineLogger
import dev.haipham22.leechtext.plugin.js.api.Http
import dev.haipham22.leechtext.plugin.js.api.LocalStorage
import dev.haipham22.leechtext.plugin.js.loader.LoaderType
import dev.haipham22.leechtext.util.LeechJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toPath

/**
 * QuickJS actual của JsSandbox (P5.2c) — engine mục tiêu iOS (quickjs-kt).
 *
 * Mỗi sandbox = QuickJS runtime riêng (isolation như Rhino initStandardObjects), limit
 * mirror JsScriptEngine: 30s / 50MB. API vBook install qua native functions + JS prelude
 * (giữ P5.1 slice pattern — boundary crossing chỉ primitives + JSON-ish structures;
 * Kotlin Map sang JS phải [toJsObject] cho property access).
 *
 * ponytail: QuickJS event-loop single-threaded; các op serialize qua dispatcher nội bộ
 * của quickjs-kt — nếu P5.3 thấy contention plugin song song, thêm Mutex ở đây.
 */
actual class JsSandbox internal actual constructor(
    private val log: EngineLogger,
    config: SandboxConfig,
) {
    private val loaderType: LoaderType = requireNotNull(config.loaderType)
    private val baseUrl: String? = config.baseUrl
    private val targetUrl: String? = config.targetUrl
    private val pluginConfig: Map<String, String>? = config.pluginConfig
    private val extraScripts: Map<String, String>? = config.extraScripts
    private val pluginSource: String? = config.pluginSource

    private val storage = LocalStorage(log, pluginSource ?: "default")
    private val quickJs: QuickJs = QuickJs.create(Dispatchers.Default)

    init {
        quickJs.memoryLimit = MEMORY_LIMIT_BYTES
        quickJs.evaluationTimeoutMillis = TIMEOUT_MILLIS
        runBlocking { quickJs.installVBookApi() }
        log.add("[VBookSandbox] Created ${loaderType.type} sandbox (QuickJS)")
    }

    actual fun execute(
        script: String,
        scriptName: String,
    ): Boolean = runBlocking {
        try {
            quickJs.evaluate<Any?>(script, filename = scriptName)
            true
        } catch (e: Exception) {
            val errorMsg = e.message ?: e::class.simpleName
            if (errorMsg?.contains("toString") == true) {
                log.add("[VBookSandbox] Suppressed toString() error in $scriptName")
            } else {
                log.add("[VBookSandbox] JavaScript execution error in $scriptName: $errorMsg")
            }
            false
        }
    }

    actual fun callFunction(
        functionName: String,
        vararg args: Any?,
    ): Any? = runBlocking {
        try {
            val argSrc = args.joinToString(",") { jsArgLiteral(it) }
            quickJs.evaluate<Any?>("$functionName($argSrc)", filename = "callFunction")
        } catch (e: Exception) {
            val errorMsg = e.message ?: e::class.simpleName
            log.add("[VBookSandbox] Function call error in $functionName: $errorMsg")
            null
        }
    }

    actual fun close() {
        quickJs.close()
        log.add("[VBoxSandbox] Closed ${loaderType.type} sandbox")
    }

    /** Serialize arg Kotlin → JS literal (null/String/Number/Boolean/Map/List). */
    private fun jsArgLiteral(arg: Any?): String = when (arg) {
        null -> "null"
        is String -> "\"" + arg.jsEscaped() + "\""
        is Boolean, is Int, is Long, is Double, is Float -> arg.toString()
        else -> jsValueBridgeStringify(arg)
    }

    private fun jsValueBridgeStringify(arg: Any?): String = dev.haipham22.leechtext.plugin.js.api.JsValueBridge.stringify(arg)

    private suspend fun QuickJs.installVBookApi() {
        function("__htmlSelect") { args -> htmlSelect(args) }
        function("__htmlDoc") { args -> htmlDoc(args) }
        function("__httpExecute") { args -> httpExecuteNative(args) }
        function("__consoleLog") { args ->
            log.add(args.getOrNull(0)?.toString() ?: "null")
            null
        }
        function("__lsGet") { args -> storage.getItem(args.getOrNull(0) as? String) }
        function("__lsSet") { args ->
            storage.setItem(args.getOrNull(0) as? String, args.getOrNull(1) as? String)
            null
        }
        function("__lsRemove") { args ->
            storage.removeItem(args.getOrNull(0) as? String)
            null
        }
        function("__sleepMs") { args ->
            dev.haipham22.leechtext.plugin.js.api.sleepMs((args.getOrNull(0) as? Double)?.toLong() ?: 0L)
            null
        }
        function("__loadJsFile") { args -> loadJsFile(args.getOrNull(0) as? String) }
        evaluate<Any?>(vBookPrelude(), filename = "vbook-prelude.js")
    }

    // ---------------- native impls ----------------

    /** html.select(selector) — trả list element serialized (P5.1). */
    private fun htmlSelect(args: Array<Any?>): List<JsObject> = try {
        val html = args.getOrNull(0) as? String
        val selector = args.getOrNull(1) as? String
        if (html.isNullOrEmpty() || selector.isNullOrEmpty()) {
            emptyList()
        } else {
            Ksoup.parse(html, "").select(selector).map { el ->
                mapOf(
                    "text" to el.text(),
                    "html" to el.html(),
                    "outerHtml" to el.outerHtml(),
                    "tagName" to el.tagName(),
                    "id" to el.id(),
                    "className" to el.className(),
                    "attrs" to el.attributes().associate { it.key to it.value }.toJsObject(),
                ).toJsObject()
            }
        }
    } catch (e: Exception) {
        // Selector lỗi → rỗng, như JSElements.select catch-all
        emptyList()
    }

    /** doc text/html/title cho Html.parse().text() v.v. */
    private fun htmlDoc(args: Array<Any?>): JsObject? = try {
        val html = args.getOrNull(0) as? String
        if (html.isNullOrEmpty()) {
            mapOf("text" to "", "html" to "", "title" to "").toJsObject()
        } else {
            val doc = Ksoup.parse(html, "")
            mapOf(
                "text" to doc.text(),
                "html" to doc.html(),
                "title" to doc.title(),
            ).toJsObject()
        }
    } catch (e: Exception) {
        mapOf("text" to "", "html" to "", "title" to "").toJsObject()
    }

    /** Thực thi HTTP qua common Http (UA + cookie sync + retry 429/503). */
    private fun httpExecuteNative(args: Array<Any?>): JsObject = try {
        val method = args.getOrNull(0) as? String ?: "GET"
        val url = args.getOrNull(1) as? String ?: ""
        val headersJson = args.getOrNull(2) as? String ?: "{}"
        val body = args.getOrNull(3) as? String
        val contentType = args.getOrNull(4) as? String

        val headers = HashMap<String, String>()
        runCatching {
            val obj = LeechJson.parseToJsonElement(headersJson) as? kotlinx.serialization.json.JsonObject
            obj?.forEach { (k, v) -> headers[k] = v.toString().trim('"') }
        }

        val result =
            Http.executeSpec(
                method = method,
                url = url,
                headers = headers,
                body = body?.encodeToByteArray(),
                bodyContentType = contentType,
                log = log,
            )
        mapOf(
            "code" to result.code.toDouble(),
            "message" to result.message,
            "headers" to result.headers.toJsObject(),
            "body" to result.body.decodeToString(),
        ).toJsObject()
    } catch (e: Exception) {
        log.add("[QuickJS __httpExecute] failed: ${e.message}")
        mapOf(
            "code" to 0.0,
            "message" to e.message,
            "headers" to emptyMap<String, String>().toJsObject(),
            "body" to "",
        ).toJsObject()
    }

    /** load('config.js') — extraScripts in-memory trước, disk sau (guard + 100KB). */
    private fun loadJsFile(fileName: String?): String? {
        if (fileName.isNullOrEmpty() || !isValidFileName(fileName)) return null
        extraScripts?.get(fileName)?.let { return it.toVarDeclarations() }
        if (pluginSource.isNullOrEmpty()) return null
        return readPluginSrcFile(fileName)
    }

    /** Đọc file từ pluginSource/src với guard path + giới hạn 100KB; null khi lỗi/thiếu. */
    private fun readPluginSrcFile(fileName: String): String? = try {
        if (pluginSource.isNullOrEmpty()) return null
        val file = (pluginSource.toPath() / "src" / fileName)
        val fs = FileSystem.SYSTEM
        val metadata = fs.metadataOrNull(file)
        if (metadata != null && (metadata.size ?: 0L) <= 100L * 1024) {
            fs.read(file) { readUtf8() }.toVarDeclarations()
        } else {
            null
        }
    } catch (e: Exception) {
        log.add("[QuickJS load] failed: ${e.message}")
        null
    }

    /**
     * let/const đầu dòng → var. load() eval qua `(0, eval)` — indirect eval tạo lexical
     * environment riêng, top-level let/const (vd `let BASE_URL` trong config.js) CHẾT ngay
     * sau eval nên script caller thấy "BASE_URL is not defined" (Rhino giữ binding vào
     * scope — khác biệt QuickJS vs Rhino). var bám globalThis → giữ đúng behavior Rhino.
     * ponytail ceiling: regex đầu dòng, không parse AST — const/let trong string literal
     * nhiều dòng có thể false-positive; corpus plugin vbook không có pattern đó.
     */
    private fun String.toVarDeclarations(): String = replace(letConstLineStart) { it.groupValues[1] + "var " + it.groupValues[2] }

    private fun isValidFileName(fileName: String): Boolean = !fileName.contains("..") && !fileName.contains("/") && !fileName.contains("\\") &&
        fileName.endsWith(".js") && !fileName.matches(Regex(".*[<>:\"|?*].*"))

    /** Khớp let/const đầu dòng (giữ indent) — dùng cho [String.toVarDeclarations]. */
    private val letConstLineStart = Regex("(?m)^([ \\t]*)(?:let|const)([ \\t]+)")

    /** JS prelude dựng vBook API shape như JsApiSetup (mở rộng P5.1 slice). */
    @Suppress("LongMethod") // JS template — tách file .js khi P5.3 cần iterate
    private fun vBookPrelude(): String =
        """
        (function () {
          function mkEl(m) {
            return {
              text: function () { return m.text; },
              html: function () { return m.html; },
              outerHtml: function () { return m.outerHtml; },
              tagName: function () { return m.tagName; },
              hasClass: function (c) { return m.className.split(/\s+/).indexOf(c) >= 0; },
              attr: function (k, v) {
                if (v !== undefined) { m.attrs[k] = String(v); return this; }
                return (m.attrs && m.attrs[k] !== undefined) ? m.attrs[k] : '';
              },
              select: function (sel) { return mkEls(__htmlSelect(m.outerHtml, sel == null ? '' : String(sel))); },
              first: function () { return this; },
              last: function () { return this; },
              id: function () { return m.id; },
              className: function () { return m.className; },
              parent: function () { return null; },
              children: function () { return mkEls([]); },
              remove: function () {},
              addClass: function () {},
              removeClass: function () {}
            };
          }
          function mkList(arr) {
            return {
              length: arr.length,
              size: function () { return arr.length; },
              get: function (i) { return (i >= 0 && i < arr.length) ? arr[i] : null; },
              forEach: function (cb) { if (cb) arr.forEach(function (e, i) { cb(e, i); }); },
              map: function (cb) { return mkList(cb ? arr.map(function (e, i) { return cb(e, i); }) : []); },
              filter: function (cb) { return mkList(cb ? arr.filter(function (e, i) { return cb(e, i); }) : []); },
              find: function (cb) { return cb ? arr.find(function (e, i) { return cb(e, i); }) : null; },
              join: function (sep) { return arr.join(sep); },
              first: function () { return arr.length > 0 ? arr[0] : null; },
              last: function () { return arr.length > 0 ? arr[arr.length - 1] : null; }
            };
          }
          function mkEls(list) {
            var els = list.map(mkEl);
            return {
              length: els.length,
              size: function () { return els.length; },
              isEmpty: function () { return els.length === 0; },
              get: function (i) { return (i >= 0 && i < els.length) ? els[i] : null; },
              eq: function (i) { return (i >= 0 && i < els.length) ? els[i] : null; },
              first: function () { return els.length > 0 ? els[0] : null; },
              last: function () { return els.length > 0 ? els[els.length - 1] : null; },
              text: function () { return els.map(function (e) { return e.text(); }).join(' '); },
              html: function () { return els.map(function (e) { return e.html(); }).join(' '); },
              outerHtml: function () { return els.map(function (e) { return e.outerHtml(); }).join(' '); },
              attr: function (k) { return els.length > 0 ? els[0].attr(k) : ''; },
              forEach: function (cb) { if (cb) els.forEach(function (e, i) { cb(e, i); }); },
              each: function (cb) { if (cb) els.forEach(function (e, i) { cb(i, e); }); },
              map: function (cb) { return mkList(cb ? els.map(function (e, i) { return cb(e, i); }) : []); },
              filter: function (cb) { return mkEls(cb ? els.filter(function (e, i) { return cb(e, i); }) : []); },
              select: function (sel) { return mkEls([]); },
              addClass: function () {}, removeClass: function () {}, remove: function () {}
            };
          }
          function mkHttp() {
            var h = {
              method: 'GET', url: null, headers: {}, body: null, contentType: null, _syncCookie: true, _res: null,
              request: function (u) { h.url = u; h._res = null; return h; },
              get: function (u) { return h.request(u).method('GET'); },
              post: function (u) { return h.request(u).method('POST'); },
              put: function (u) { return h.request(u).method('PUT'); },
              'delete': function (u) { return h.request(u).method('DELETE'); },
              patch: function (u) { return h.request(u).method('PATCH'); },
              head: function (u) { return h.request(u).method('HEAD'); },
              method: function (m) { h.method = (m || 'GET').toUpperCase(); h._res = null; return h; },
              header: function (k, v) { h.headers[k] = v; return h; },
              headers: function (hs) { if (hs) { for (var k in hs) h.headers[k] = hs[k]; } return h; },
              body: function (b) { h.body = (b == null) ? '' : String(b); h.contentType = 'application/json; charset=utf-8'; return h; },
              params: function (ps) {
                if (ps) {
                  var parts = [];
                  for (var k in ps) parts.push(encodeURIComponent(k) + '=' + encodeURIComponent(ps[k] == null ? '' : String(ps[k])));
                  h.body = parts.join('&'); h.contentType = 'application/x-www-form-urlencoded';
                }
                return h;
              },
              param: function (k, v) { var o = {}; o[k] = v; return h.params(o); },
              form: function (d) { return h.params(d); },
              data: function (d) { return h.params(d); },
              queries: function (qs) {
                if (qs && h.url) {
                  var parts = [];
                  for (var k in qs) parts.push(encodeURIComponent(k) + '=' + encodeURIComponent(qs[k] == null ? '' : String(qs[k])));
                  if (parts.length) h.url = h.url + (h.url.indexOf('?') >= 0 ? '&' : '?') + parts.join('&');
                }
                return h;
              },
              timeout: function (ms) { return h; },
              syncCookie: function (s) { h._syncCookie = !!s; return h; },
              _exec: function () {
                if (!h._res) {
                  h._res = __httpExecute(h.method, h.url || '', JSON.stringify(h.headers), h.body, h.contentType);
                }
                return h._res;
              },
              string: function () { return h._exec().body || ''; },
              bytes: function () { return h._exec().body || ''; },
              json: function () { return Json.parse(h.string()); },
              html: function () { return Html.parse(h.string()); },
              document: function () { return h.html(); },
              statusCode: function () { return h._exec().code; },
              ok: function () { var c = h._exec().code; return c >= 200 && c <= 399; },
              statusMessage: function () { return h._exec().message || ''; },
              responseHeaders: function () { return h._exec().headers; },
              contentType: function () { return h._exec().headers['Content-Type']; }
            };
            return h;
          }
          ${baseUrlPrelude()}
          ${configPrelude()}
          globalThis.Html = {
            parse: function (html) {
              var d = __htmlDoc(html == null ? '' : String(html));
              return {
                select: function (sel) { return mkEls(__htmlSelect(html == null ? '' : String(html), sel)); },
                text: function () { return d.text; },
                html: function () { return d.html; },
                title: function () { return d.title; }
              };
            }
          };
          globalThis.Json = {
            parse: function (s) {
              if (s === null || s === undefined || s.length === 0) return null;
              var t = String(s).trim();
              if (t.length === 0) return null;
              return JSON.parse(t);
            },
            stringify: function (v) { return JSON.stringify(v); }
          };
          globalThis.Response = {
            success: function (data, data2) {
              return data2 === undefined ? { code: 0, data: data } : { code: 0, data: data, data2: data2 };
            },
            error: function (a, b, c) {
              if (typeof a === 'number' && c !== undefined) return { code: a, data2: b, message: c };
              if (typeof a === 'number') return { code: a, data2: b };
              return { code: 1, data2: a };
            }
          };
          globalThis.Http = mkHttp();
          globalThis.fetch = function (url, opts) {
            var h = mkHttp();
            h.request(url == null ? (globalThis.BASE_URL || '') : String(url));
            if (opts && typeof opts === 'object' && opts.queries) h.queries(opts.queries);
            return h;
          };
          globalThis.UserAgent = {
            android: function () { return 'Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36'; },
            iOS: function () { return 'Mozilla/5.0 (iPhone; CPU iPhone OS 18_1_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.1 Mobile/15E148 Safari/604.1'; },
            desktop: function () { return 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36'; },
            mobile: function () { return 'Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36'; }
          };
          globalThis.Engine = {
            newBrowser: function () { throw new Error('Browser API chỉ có trên desktop (Playwright Chromium) — iOS/macOS không hỗ trợ'); }
          };
          var consoleObj = { log: function (m) { __consoleLog(m === undefined ? 'undefined' : (typeof m === 'object' ? JSON.stringify(m) : String(m))); } };
          globalThis.Console = consoleObj;
          globalThis.console = consoleObj;
          globalThis.localStorage = {
            getItem: function (k) { return __lsGet(k == null ? null : String(k)); },
            setItem: function (k, v) { __lsSet(k == null ? null : String(k), v == null ? null : String(v)); },
            removeItem: function (k) { __lsRemove(k == null ? null : String(k)); }
          };
          var pluginCfg = ${configJsonLiteral()};
          globalThis.localConfig = {
            getItem: function (k) { return (k && pluginCfg[k] !== undefined) ? pluginCfg[k] : null; },
            setItem: function () {}
          };
          globalThis.load = function (name) {
            var src = __loadJsFile(name == null ? '' : String(name));
            if (src === null || src === undefined) return null;
            // Eval trực tiếp trong global scope (load() của Rhino eval + trả giá trị cuối)
            return (0, eval)(src);
          };
          globalThis.sleep = function (ms) { __sleepMs(Number(ms) || 0); };
        })();
        """.trimIndent()

    /** BASE_URL inject — plugin có config.js thì bỏ (config tự const BASE_URL). */
    private fun baseUrlPrelude(): String {
        val hasConfigScript = extraScripts?.containsKey("config.js") == true
        if (baseUrl == null || hasConfigScript) return ""
        return """globalThis.BASE_URL = "${baseUrl.jsEscaped()}";"""
    }

    /** Plugin config keys làm JS constants (vBook extension-api). */
    private fun configPrelude(): String {
        val cfg = pluginConfig ?: return ""
        val sb = StringBuilder()
        for ((k, v) in cfg) {
            if (k.isNotEmpty() && k.matches(Regex("[A-Za-z_][A-Za-z0-9_]*"))) {
                sb.append("globalThis.").append(k).append(" = \"").append(v.jsEscaped()).append("\";\n")
            }
        }
        return sb.toString()
    }

    private fun configJsonLiteral(): String = (pluginConfig ?: emptyMap())
        .entries.joinToString(prefix = "{", postfix = "}", separator = ",") { (k, v) ->
            "\"${k.jsEscaped()}\":\"${v.jsEscaped()}\""
        }

    private fun String.jsEscaped(): String = replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")

    private companion object {
        private const val TIMEOUT_MILLIS = 30_000L
        private const val MEMORY_LIMIT_BYTES = 50L * 1024 * 1024
    }
}

/** Apple actual factory — QuickJS sandbox. */
actual fun createSandbox(
    log: EngineLogger,
    config: SandboxConfig.() -> Unit,
): JsSandbox {
    val cfg = SandboxConfig().apply(config)
    return JsSandbox(log, cfg)
}
