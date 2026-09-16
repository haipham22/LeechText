package dev.haipham22.leechtext.plugin.vbook

import dev.haipham22.leechtext.log.platformEngineLogger
import dev.haipham22.leechtext.plugin.js.loader.LoaderType
import dev.haipham22.leechtext.plugin.js.sandbox.JsSandbox
import dev.haipham22.leechtext.plugin.js.sandbox.createSandbox
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private const val TRUYENFULL_VISION_BASE_URL = "https://truyenfull.vision"
private const val TEST_DIV_HTML = "<div>test</div>"

/**
 * P5.2c: port VBookSandboxCompatibilityTest (Rhino/jvmTest) sang common sandbox contract
 * (QuickJS actual). Cùng scripts + cùng assertion values; type-check Rhino
 * (NativeObject/Int) map sang tương đương QuickJS (JsObject = Map, JS int → Long).
 */
@Suppress("TooManyFunctions") // test suite
class QuickJsVBookSandboxCompatTest {
    private var sandbox: JsSandbox? = null

    /** Eval qua common sandbox contract (execute + callFunction) — return expr cuối. */
    private fun eval(script: String): Any? {
        sandbox?.close()
        val s =
            createSandbox(platformEngineLogger()) {
                loaderType = LoaderType.TEXT
                baseUrl = TRUYENFULL_VISION_BASE_URL
                targetUrl = baseUrl
            }
        sandbox = s
        // JS eval() trả completion value của biểu thức cuối — giữ nguyên semantics
        // top-level eval của P5.1 với mọi dạng statement
        s.execute("globalThis.__eval = function(src){ return eval(src); };", "test")
        return s.callFunction("__eval", script)
    }

    @kotlin.test.AfterTest
    fun tearDown() {
        sandbox?.close()
        sandbox = null
    }

    @Test
    fun testJsonParseInSandboxReturnsNativeObject() {
        val result = eval("""let json = Json.parse('{"chap_list": "<div>test</div>", "status": 200}'); json;""")

        assertNotNull(result)
        assertTrue(result is Map<*, *>, "Json.parse() MUST return JS object cho property access")
        assertEquals(TEST_DIV_HTML, (result as Map<*, *>)["chap_list"])
    }

    @Test
    fun testPluginCodeExecutionInSandbox() {
        val pluginCode =
            """
            let jsonString = '{"chap_list": "<div class=\'list-chapter\'>' +
                '<li><a href=\'/chuong-1\'>Chương 1</a></li></div>", "status": 200}';
            let json = Json.parse(jsonString);
            let chapList = json.chap_list;
            let HtmlApi = Html;
            let doc = HtmlApi.parse(chapList);
            let links = doc.select('.list-chapter li a');
            links.length;
            """.trimIndent()

        assertEquals(1L, eval(pluginCode), "Plugin should return chapter count")
    }

    @Test
    fun testHtmlParseInSandbox() {
        val result =
            eval(
                """
                let doc = Html.parse("<div class='list-chapter'><li><a href='/chuong-1/'>Chương 1</a></li></div>");
                let links = doc.select('.list-chapter li a');
                JSON.stringify([links.size(), links.length]);
                """.trimIndent(),
            ) as String

        assertEquals("[1,1]", result)
    }

    @Test
    fun testJSElementsMapInSandbox() {
        val testCode =
            """
            let HtmlApi = Html;
            let doc = HtmlApi.parse('<div class="chapter">Chapter 1</div><div class="chapter">Chapter 2</div>');
            let elements = doc.select('.chapter');
            let mapped = elements.map(function(e, i) { return e.text(); });
            mapped.length;
            """.trimIndent()

        assertEquals(2L, eval(testCode))
    }

    @Test
    fun testFullPluginFlowWithMockJsonResponse() {
        val pluginCode =
            """
            let mockJson = '{"chap_list": "<div class=\'list-chapter\'>' +
                '<li><a href=\'/chuong-1\'>Chương 1</a></li>' +
                '<li><a href=\'/chuong-2\'>Chương 2</a></li></div>", "status": 200}';
            let json = Json.parse(mockJson);
            let HtmlApi = Html;
            let doc = HtmlApi.parse(json.chap_list);
            let chapters = [];
            doc.select('.list-chapter li a').forEach(function(e) {
              chapters.push({
                name: e.text(),
                url: e.attr('href'),
                host: BASE_URL
              });
            });
            chapters.length;
            """.trimIndent()

        assertEquals(2L, eval(pluginCode))
    }

    @Test
    fun testResponseJsonMethodExistence() {
        val testCode =
            """
            let mockJson = '{"chap_list": "<div>test</div>", "status": 200}';
            let json = Json.parse(mockJson);
            json.chap_list;
            """.trimIndent()

        assertEquals(TEST_DIV_HTML, eval(testCode))
    }

    @Test
    fun testVBookAndroidBehaviorMatch() {
        // Test 1: Json.parse trả JS object (Rhino: NativeObject)
        val parsed = eval("""Json.parse('{"chap_list": "<div>test</div>"}');""")
        assertTrue(parsed is Map<*, *>)
        assertEquals(TEST_DIV_HTML, (parsed as Map<*, *>)["chap_list"])

        // Test 2: Html.parse() với chap_list
        assertNotNull(eval("""Html.parse('<div>a</div>');"""))

        // Test 3: JSElements.length property
        assertEquals(
            1L,
            eval(
                """
                let HtmlApi = Html; let doc = HtmlApi.parse('<div>a</div>'); let el = doc.select('div'); el.length;
                """.trimIndent(),
            ),
        )

        // Test 4: map() trả list-like có length
        assertEquals(
            2L,
            eval(
                """
                let HtmlApi = Html; let doc = HtmlApi.parse('<div class="test">a</div><div class="test">b</div>');
                let els = doc.select('.test'); let mapped = els.map(function(e) { return e.text(); }); mapped.length;
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun testUndefinedPropagationInSandbox() {
        val testCode =
            """
            let mockJson = '{}';
            let json = Json.parse(mockJson);
            let chapList = json.chap_list;
            typeof chapList;
            """.trimIndent()

        assertEquals("undefined", eval(testCode))
    }

    @Test
    fun testSandboxApiSurfaceCompleteness() {
        assertNotNull(eval("Html.parse('<div>test</div>');"))
        assertEquals("function", eval("typeof fetch;"))
        assertNotNull(eval("""Json.parse('{"test": 1}');"""))
        assertNotNull(eval("Response.success([]);"))
    }

    @Test
    fun testPropertyAccessChain() {
        val testCode =
            """
            let mockJson = '{"chap_list": "<div class=\'list-chapter\'><li><a href=\'/chuong-1\'>Test</a></li></div>"}';
            let json = Json.parse(mockJson);
            let chapList = json.chap_list;
            let HtmlApi = Html;
            let doc = HtmlApi.parse(chapList);
            let links = doc.select('.list-chapter li a');
            links.length;
            """.trimIndent()

        assertEquals(1L, eval(testCode))
    }

    @Test
    fun testBaseUrlAvailability() {
        assertEquals(TRUYENFULL_VISION_BASE_URL, eval("typeof BASE_URL; BASE_URL;"))
    }

    /**
     * P5.4 regression: load('config.js') với top-level `let/const` phải persist ra global —
     * `(0, eval)` của QuickJS tạo lexical env riêng nên `let BASE_URL` chết ngay sau eval
     * ("BASE_URL is not defined", kho plugin thật die toàn bộ home/search/detail trên iOS).
     * sandbox loadJsFile đổi let/const đầu dòng → var (var bám globalThis như Rhino scope).
     */
    @Test
    fun testLoadTopLevelLetConfigPersistRaGlobal() {
        sandbox?.close()
        val s =
            createSandbox(platformEngineLogger()) {
                loaderType = LoaderType.TEXT
                baseUrl = TRUYENFULL_VISION_BASE_URL
                targetUrl = baseUrl
                extraScripts = mapOf("config.js" to "let BASE_URL = \"https://example.org\"\ntry { if (CONFIG_URL) BASE_URL = CONFIG_URL } catch (e) {}")
            }
        sandbox = s
        assertTrue(s.execute("load('config.js'); function execute() { return BASE_URL; }", "test"))
        assertEquals("https://example.org", s.callFunction("execute"))
    }

    /** P5.4: e.select(...).first().attr(...) trên 1 element (gen.js dùng) — mkEl phải có select. */
    @Test
    fun testElementSelectChainTrenMotElement() {
        sandbox?.close()
        val s =
            createSandbox(platformEngineLogger()) {
                loaderType = LoaderType.TEXT
                baseUrl = TRUYENFULL_VISION_BASE_URL
                targetUrl = baseUrl
            }
        sandbox = s
        s.execute(
            "function execute() {" +
                "  var doc = Html.parse('<div class=\"novel\"><div class=\"truyen-title\"><a href=\"/trang-chu/abc\">Ten Truyen</a></div></div>');" +
                "  var e = doc.select('.novel').first();" +
                "  return e.select('.truyen-title > a').first().attr('href') + '|' + e.select('.truyen-title > a').text();" +
                "}",
            "test",
        )
        assertEquals("/trang-chu/abc|Ten Truyen", s.callFunction("execute"))
    }
}
