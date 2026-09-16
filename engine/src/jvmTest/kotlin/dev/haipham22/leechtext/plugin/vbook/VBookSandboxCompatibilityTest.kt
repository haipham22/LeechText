package dev.haipham22.leechtext.plugin.vbook

import dev.haipham22.leechtext.log.platformEngineLogger
import dev.haipham22.leechtext.plugin.js.api.Html
import dev.haipham22.leechtext.plugin.js.api.Http
import dev.haipham22.leechtext.plugin.js.api.JSDocument
import dev.haipham22.leechtext.plugin.js.api.JSElements
import dev.haipham22.leechtext.plugin.js.api.Json
import dev.haipham22.leechtext.plugin.js.loader.Response
import dev.haipham22.leechtext.plugin.js.sandbox.JsSandbox
import org.junit.Before
import org.junit.Test
import org.mozilla.javascript.BaseFunction
import org.mozilla.javascript.Context
import org.mozilla.javascript.NativeObject
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private const val JS_PARSE_MOCK_JSON = "let json = Json.parse(mockJson);\n"
private const val JS_READ_CHAP_LIST = "let chapList = json.chap_list;\n"
private const val JS_ALIAS_HTML_API = "let HtmlApi = Html;\n"

/**
 * E2E sandbox compatibility (port nguyên văn từ VBookSandboxCompatibilityTest.java) —
 * mô phỏng đúng cách vBook Android thực thi plugin.
 */
class VBookSandboxCompatibilityTest {
    private lateinit var context: Context
    private lateinit var scope: Scriptable
    private val baseUrl = "https://truyenfull.vision"

    @Before
    fun setUp() {
        // Enter Rhino context (matching vBook Android) — factory JsSandbox (map-access feature)
        context = JsSandbox.FACTORY.enterContext()
        context.optimizationLevel = -1
        context.languageVersion = 200
        context.wrapFactory.isJavaPrimitiveWrap = false

        scope = context.initStandardObjects()

        setupVBookApi()
    }

    private fun setupVBookApi() {
        ScriptableObject.putProperty(scope, "BASE_URL", baseUrl)
        ScriptableObject.putProperty(scope, "Html", Html(platformEngineLogger()))
        ScriptableObject.putProperty(scope, "Http", Http(platformEngineLogger()))
        ScriptableObject.putProperty(scope, "Json", Json(platformEngineLogger()))
        ScriptableObject.putProperty(scope, "Response", Response())

        // fetch function
        val fetchFunc =
            object : BaseFunction() {
                override fun call(
                    cx: Context,
                    scope: Scriptable,
                    thisObj: Scriptable,
                    args: Array<Any?>,
                ): Any {
                    val requestUrl =
                        if (args.isNotEmpty() && args[0] != null) Context.toString(args[0]) else baseUrl
                    return Http(platformEngineLogger()).request(requestUrl)
                }
            }
        ScriptableObject.putProperty(scope, "fetch", fetchFunc)
    }

    @Test
    fun testJsonParseInSandboxReturnsNativeObject() {
        val testJson = """{"chap_list": "<div>test</div>", "status": 200}"""

        val result = Json(platformEngineLogger()).parse(testJson)
        assertNotNull(result)
        assertTrue(result is NativeObject, "Json.parse() MUST return NativeObject")

        val jsonObj = result
        assertTrue(jsonObj.has("chap_list", jsonObj))
        assertEquals("<div>test</div>", jsonObj["chap_list", jsonObj])
    }

    @Test
    fun testPluginCodeExecutionInSandbox() {
        val pluginCode =
            "let jsonString = '{\"chap_list\": \"<div class=\\'list-chapter\\'>' +\n" +
                "'<li><a href=\\'/chuong-1\\'>Chương 1</a></li></div>\", \"status\": 200}';\n" +
                "let json = Json.parse(jsonString);\n" +
                JS_READ_CHAP_LIST +
                JS_ALIAS_HTML_API +
                "let doc = HtmlApi.parse(chapList);\n" +
                "let links = doc.select('.list-chapter li a');\n" +
                "links.length;"

        val result = context.evaluateString(scope, pluginCode, "testPlugin", 1, null)

        assertNotNull(result)
        assertTrue(result is Int, "Plugin should return chapter count")
        assertEquals(1, result)
    }

    @Test
    fun testHtmlParseInSandbox() {
        val testHtml = "<div class='list-chapter'><li><a href='/chuong-1/'>Chương 1</a></li></div>"

        val doc: JSDocument = Html(platformEngineLogger()).parse(testHtml)
        assertNotNull(doc)

        val links: JSElements = doc.select(".list-chapter li a")
        assertNotNull(links)
        assertEquals(1, links.size())
        assertEquals(1, links.length)
    }

    @Test
    fun testJSElementsMapInSandbox() {
        val testCode =
            JS_ALIAS_HTML_API +
                "let doc = HtmlApi.parse('<div class=\\\"chapter\\\">Chapter 1</div><div class=\\\"chapter\\\">Chapter 2</div>');\n" +
                "let elements = doc.select('.chapter');\n" +
                "let mapped = elements.map(function(e, i) { return e.text(); });\n" +
                "mapped.length;"

        val result = context.evaluateString(scope, testCode, "testMap", 1, null)
        assertNotNull(result)
        assertTrue(result is Number)
        assertEquals(2, result.toInt())
    }

    @Test
    fun testFullPluginFlowWithMockJsonResponse() {
        val pluginCode =
            "let mockJson = '{\"chap_list\": \"<div class=\\'list-chapter\\'>' +\n" +
                "'<li><a href=\\'/chuong-1\\'>Chương 1</a></li>' +\n" +
                "'<li><a href=\\'/chuong-2\\'>Chương 2</a></li></div>\", \"status\": 200}';\n" +
                JS_PARSE_MOCK_JSON +
                JS_ALIAS_HTML_API +
                "let doc = HtmlApi.parse(json.chap_list);\n" +
                "let chapters = [];\n" +
                "doc.select('.list-chapter li a').forEach(function(e) {\n" +
                "  chapters.push({\n" +
                "    name: e.text(),\n" +
                "    url: e.attr('href'),\n" +
                "    host: BASE_URL\n" +
                "  });\n" +
                "});\n" +
                "chapters.length;"

        val result = context.evaluateString(scope, pluginCode, "testFullFlow", 1, null)
        assertNotNull(result)
        assertTrue(result is Number)
        assertEquals(2, result.toInt())
    }

    @Test
    fun testResponseJsonMethodExistence() {
        val testCode =
            "let mockJson = '{\"chap_list\": \"<div>test</div>\", \"status\": 200}';\n" +
                JS_PARSE_MOCK_JSON +
                "json.chap_list;"

        val result = context.evaluateString(scope, testCode, "testResponseJson", 1, null)
        assertNotNull(result)
        assertEquals("<div>test</div>", result)
    }

    @Test
    fun testVBookAndroidBehaviorMatch() {
        // Test 1: NativeJSON.parse được dùng (không phải org.json)
        val parsed = Json(platformEngineLogger()).parse("""{"chap_list": "<div>test</div>"}""")
        assertTrue(parsed is NativeObject)

        // Test 2: Html.parse() với chap_list
        val jsonObj = parsed
        val chapList = jsonObj["chap_list", jsonObj] as String
        val doc = Html(platformEngineLogger()).parse(chapList)
        assertNotNull(doc)

        // Test 3: JSElements.length public field
        val lengthResult =
            context.evaluateString(
                scope,
                "let HtmlApi = Html; let doc = HtmlApi.parse('<div>a</div>'); let el = doc.select('div'); el.length;",
                "testLength",
                1,
                null,
            )
        assertNotNull(lengthResult)
        assertTrue(lengthResult is Int)
        assertEquals(1, lengthResult)

        // Test 4: map() trả JSList
        val mapResult =
            context.evaluateString(
                scope,
                "let HtmlApi = Html; let doc = HtmlApi.parse('<div class=\\\"test\\\">a</div><div class=\\\"test\\\">b</div>'); " +
                    "let els = doc.select('.test'); let mapped = els.map(function(e) { return e.text(); }); mapped.length;",
                "testMap",
                1,
                null,
            )
        assertNotNull(mapResult)
        assertTrue(mapResult is Number)
        assertEquals(2, mapResult.toInt())
    }

    @Test
    fun testUndefinedPropagationInSandbox() {
        val testCode =
            "let mockJson = '{}';\n" +
                JS_PARSE_MOCK_JSON +
                JS_READ_CHAP_LIST +
                "typeof chapList;"

        val result = context.evaluateString(scope, testCode, "testUndefined", 1, null)
        assertNotNull(result)
        assertEquals("undefined", result)
    }

    @Test
    fun testSandboxApiSurfaceCompleteness() {
        val htmlResult = context.evaluateString(scope, "Html.parse('<div>test</div>');", "testHtmlApi", 1, null)
        assertNotNull(htmlResult)

        val httpResult = context.evaluateString(scope, "typeof fetch;", "testHttpApi", 1, null)
        assertEquals("function", httpResult)

        val jsonResult = context.evaluateString(scope, "Json.parse('{\"test\": 1}');", "testJsonApi", 1, null)
        assertNotNull(jsonResult)

        val responseResult = context.evaluateString(scope, "Response.success([]);", "testResponseApi", 1, null)
        assertNotNull(responseResult)
    }

    @Test
    fun testPropertyAccessChain() {
        val testCode =
            "let mockJson = '{\"chap_list\": \"<div class=\\'list-chapter\\'><li><a href=\\'/chuong-1\\'>Test</a></li></div>\"}';\n" +
                JS_PARSE_MOCK_JSON +
                JS_READ_CHAP_LIST +
                JS_ALIAS_HTML_API +
                "let doc = HtmlApi.parse(chapList);\n" +
                "let links = doc.select('.list-chapter li a');\n" +
                "links.length;"

        val result = context.evaluateString(scope, testCode, "testPropertyChain", 1, null)
        assertNotNull(result)
        assertTrue(result is Int)
        assertEquals(1, result)
    }

    @Test
    fun testBaseUrlAvailability() {
        val result = context.evaluateString(scope, "typeof BASE_URL; BASE_URL;", "testBaseUrl", 1, null)
        assertNotNull(result)
        assertTrue(result is String)
        assertEquals("https://truyenfull.vision", result)
    }
}
