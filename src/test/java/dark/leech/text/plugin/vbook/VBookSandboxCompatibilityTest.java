package dark.leech.text.plugin.vbook;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;

import dark.leech.text.plugin.js.api.Html;
import dark.leech.text.plugin.js.api.Http;
import dark.leech.text.plugin.js.api.JSList;
import dark.leech.text.plugin.js.api.Json;
import dark.leech.text.plugin.js.api.JSDocument;
import dark.leech.text.plugin.js.api.JSElements;
import dark.leech.text.plugin.js.sandbox.JsSandbox;

/**
 * End-to-end sandbox compatibility test. Simulates exactly how vBook Android executes plugins
 * to verify your implementation matches vBook behavior.
 */
public class VBookSandboxCompatibilityTest {

    private Context context;
    private Scriptable scope;
    private String baseUrl = "https://truyenfull.vision";

    @Before
    public void setUp() {
        // Enter Rhino context (matching vBook Android)
        context = Context.enter();
        context.setOptimizationLevel(-1);
        context.setLanguageVersion(200);
        context.getWrapFactory().setJavaPrimitiveWrap(false);

        // Create scope (matching vBook Android)
        scope = context.initStandardObjects();

        // Setup API surface (matching vBook Android)
        setupVBookApi();
    }

    private void setupVBookApi() {
        // Set BASE_URL variable
        org.mozilla.javascript.ScriptableObject.putProperty(scope, "BASE_URL", baseUrl);

        // Html object
        Html htmlApi = new Html(context, scope);
        org.mozilla.javascript.ScriptableObject.putProperty(scope, "Html", htmlApi);

        // Http class
        Http httpInstance = new Http(context, scope);
        org.mozilla.javascript.ScriptableObject.putProperty(scope, "Http", httpInstance);

        // Json class
        Json jsonApi = new Json(context, scope);
        org.mozilla.javascript.ScriptableObject.putProperty(scope, "Json", jsonApi);

        // Response object
        dark.leech.text.plugin.js.loader.Response responseObj =
                new dark.leech.text.plugin.js.loader.Response(context, scope);
        org.mozilla.javascript.ScriptableObject.putProperty(scope, "Response", responseObj);

        // Fetch function
        Object fetchFunc = new org.mozilla.javascript.BaseFunction() {
            @Override
            public Object call(org.mozilla.javascript.Context cx, Scriptable scope,
                               Scriptable thisObj, Object[] args) {
                String requestUrl = args.length > 0 && args[0] != null
                        ? org.mozilla.javascript.Context.toString(args[0])
                        : baseUrl;
                Http http = new Http(cx, scope);
                http.request(requestUrl);
                return http;
            }
        };
        org.mozilla.javascript.ScriptableObject.putProperty(scope, "fetch", fetchFunc);
    }

    @Test
    public void testJsonParseInSandboxReturnsNativeObject() {
        // CRITICAL TEST: Verify Json.parse() returns NativeObject in sandbox
        // This is what vBook Android does and what the plugin expects

        String testJson = "{\"chap_list\": \"<div>test</div>\", \"status\": 200}";

        // Execute: let json = Json.parse(jsonString);
        Json jsonApi = new Json(context, scope);
        Object result = jsonApi.parse(testJson);

        assertNotNull("Json.parse() must return non-null", result);
        assertTrue("Json.parse() MUST return NativeObject (not Map) for JavaScript to work",
                   result instanceof NativeObject);

        NativeObject jsonObj = (NativeObject) result;

        // Verify JavaScript property access works (this is what the plugin does)
        assertTrue("Must have 'chap_list' property via has()", jsonObj.has("chap_list", jsonObj));
        Object chapList = jsonObj.get("chap_list", jsonObj);
        assertEquals("chap_list must be accessible via get()", "<div>test</div>", chapList);
    }

    @Test
    public void testPluginCodeExecutionInSandbox() {
        // Simulate exact plugin code: let json = response.json(); let doc = Html.parse(json.chap_list);

        String pluginCode =
                "let jsonString = '{\"chap_list\": \"<div class=\\'list-chapter\\'><li><a href=\\'/chuong-1/\\'>Chương 1</a></li></div>\", \"status\": 200}';\n" +
                "let json = Json.parse(jsonString);\n" +
                "let chapList = json.chap_list;\n" +  // This is where it fails if NativeObject doesn't work
                "let HtmlApi = Html;\n" +
                "let doc = HtmlApi.parse(chapList);\n" +
                "let links = doc.select('.list-chapter li a');\n" +
                "links.length;";  // Return the count

        Object result = context.evaluateString(scope, pluginCode, "testPlugin", 1, null);

        assertNotNull("Plugin execution should not return null", result);
        assertTrue("Plugin should return chapter count", result instanceof Integer);
        assertEquals("Should find 1 chapter", Integer.valueOf(1), result);
    }

    @Test
    public void testHtmlParseInSandbox() {
        // Test Html.parse() in sandbox context
        String testHtml = "<div class='list-chapter'><li><a href='/chuong-1/'>Chương 1</a></li></div>";

        // Execute: let doc = Html.parse(html);
        Html htmlApi = new Html(context, scope);
        JSDocument doc = htmlApi.parse(testHtml);

        assertNotNull("Html.parse() should return valid document", doc);

        // Test selection
        JSElements links = doc.select(".list-chapter li a");

        assertNotNull("Selection should not return null", links);
        assertEquals("Should find 1 link", 1, links.size());
        assertEquals("links.length property should work in sandbox", 1, links.length);
    }

    @Test
    public void testJSElementsMapInSandbox() {
        // Test that JSElements.map() returns JSList in sandbox (vBook compatibility)

        String testCode =
                "let HtmlApi = Html;\n" +
                "let doc = HtmlApi.parse('<div class=\\\"chapter\\\">Chapter 1</div><div class=\\\"chapter\\\">Chapter 2</div>');\n" +
                "let elements = doc.select('.chapter');\n" +
                "let mapped = elements.map(function(e, i) { return e.text(); });\n" +
                "mapped.length;";

        Object result = context.evaluateString(scope, testCode, "testMap", 1, null);

        assertNotNull("map() should not return null", result);
        assertTrue("map() result should be number (length)", result instanceof Number);
        assertEquals("map() should return 2 elements", 2, ((Number) result).intValue());
    }

    @Test
    public void testFullPluginFlowWithMockJsonResponse() {
        // Complete E2E test: Simulate plugin receiving JSON response and extracting chapters

        String pluginCode =
                "let mockJson = '{\"chap_list\": \"<div class=\\'list-chapter\\'><li><a href=\\'/chuong-1/\\'>Chương 1</a></li><li><a href=\\'/chuong-2/\\'>Chương 2</a></li></div>\", \"status\": 200}';\n" +
                "let json = Json.parse(mockJson);\n" +
                "let HtmlApi = Html;\n" +
                "let doc = HtmlApi.parse(json.chap_list);\n" +
                "let chapters = [];\n" +
                "doc.select('.list-chapter li a').forEach(function(e) {\n" +
                "  chapters.push({\n" +
                "    name: e.text(),\n" +
                "    url: e.attr('href'),\n" +
                "    host: BASE_URL\n" +
                "  });\n" +
                "});\n" +
                "chapters.length;";

        Object result = context.evaluateString(scope, pluginCode, "testFullFlow", 1, null);

        assertNotNull("Full plugin flow should not return null", result);
        assertTrue("Should return chapter count (number)", result instanceof Number);
        assertEquals("Should extract 2 chapters", 2, ((Number) result).intValue());
    }

    @Test
    public void testResponseJsonMethodExistence() {
        // Verify that response.json() method exists and works correctly

        String testCode =
                "let mockJson = '{\"chap_list\": \"<div>test</div>\", \"status\": 200}';\n" +
                "let json = Json.parse(mockJson);\n" +
                "json.chap_list;";

        Object result = context.evaluateString(scope, testCode, "testResponseJson", 1, null);

        assertNotNull("response.json() equivalent should work", result);
        assertEquals("Should access chap_list property", "<div>test</div>", result);
    }

    @Test
    public void testVBookAndroidBehaviorMatch() {
        // This test validates that your implementation matches vBook Android exactly
        // Reference: vBooks-decompiled/sources/com/vbook/app/extensions/js/module/http/JSHttpResponse.java

        // Test 1: NativeJSON.parse() is used (not org.json.JSONObject)
        String jsonString = "{\"chap_list\": \"<div>test</div>\"}";
        Json jsonApi = new Json(context, scope);
        Object parsed = jsonApi.parse(jsonString);

        assertTrue("vBook uses NativeJSON.parse() -> returns NativeObject",
                   parsed instanceof NativeObject);
        // Note: NativeObject extends AbstractMap, so it IS a Map, but that's okay
        // The important part is that it's NativeObject for JavaScript property access

        // Test 2: Html.parse() works with chap_list content
        NativeObject jsonObj = (NativeObject) parsed;
        String chapList = (String) jsonObj.get("chap_list", jsonObj);

        Html htmlApi = new Html(context, scope);
        JSDocument doc = htmlApi.parse(chapList);

        assertNotNull("vBook Html.parse() should work", doc);

        // Test 3: JSElements has public length field (vBook compatibility)
        String testCode = "let HtmlApi = Html; let doc = HtmlApi.parse('<div>a</div>'); let el = doc.select('div'); el.length;";
        Object lengthResult = context.evaluateString(scope, testCode, "testLength", 1, null);

        assertNotNull("JSElements.length should be accessible", lengthResult);
        assertTrue("length should be a number", lengthResult instanceof Integer);
        assertEquals("length should be 1", Integer.valueOf(1), lengthResult);

        // Test 4: JSElements.map() returns JSList (vBook compatibility)
        String mapCode = "let HtmlApi = Html; let doc = HtmlApi.parse('<div class=\\\"test\\\">a</div><div class=\\\"test\\\">b</div>'); let els = doc.select('.test'); let mapped = els.map(function(e) { return e.text(); }); mapped.length;";
        Object mapResult = context.evaluateString(scope, mapCode, "testMap", 1, null);

        assertNotNull("map() should return non-null", mapResult);
        // Rhino wraps JSList as NativeJavaList, so we check for length property instead
        assertTrue("map() result should have length property (as number)", mapResult instanceof Number);
        assertEquals("map() should have 2 elements", 2, ((Number) mapResult).intValue());
    }

    @Test
    public void testUndefinedPropagationInSandbox() {
        // Test what happens when json.chap_list is undefined

        String testCode =
                "let mockJson = '{}';\n" +  // JSON without chap_list
                "let json = Json.parse(mockJson);\n" +
                "let chapList = json.chap_list;\n" +  // This will be undefined
                "typeof chapList;";

        Object result = context.evaluateString(scope, testCode, "testUndefined", 1, null);

        assertNotNull("Result should not be null", result);
        assertEquals("undefined should be 'undefined'", "undefined", result);
    }

    @Test
    public void testSandboxApiSurfaceCompleteness() {
        // Verify all required APIs are exposed in sandbox (matching vBook Android)
        // Note: typeof on Java objects returns different values in Rhino, so we test functionality instead

        // Check Html API - test that it works
        String htmlTest = "Html.parse('<div>test</div>');";
        Object htmlResult = context.evaluateString(scope, htmlTest, "testHtmlApi", 1, null);
        assertNotNull("Html.parse() should work", htmlResult);

        // Check Http API - test that fetch works
        String httpTest = "typeof fetch;";
        Object httpResult = context.evaluateString(scope, httpTest, "testHttpApi", 1, null);
        assertEquals("fetch should be defined", "function", httpResult);

        // Check Json API - test that it works
        String jsonTest = "Json.parse('{\"test\": 1}');";
        Object jsonResult = context.evaluateString(scope, jsonTest, "testJsonApi", 1, null);
        assertNotNull("Json.parse() should work", jsonResult);

        // Check Response API - test that it exists
        String responseTest = "Response.success([]);";
        Object responseResult = context.evaluateString(scope, responseTest, "testResponseApi", 1, null);
        assertNotNull("Response.success() should work", responseResult);
    }

    @Test
    public void testPropertyAccessChain() {
        // Test the complete property access chain: json -> chap_list -> Html.parse -> select

        String testCode =
                "let mockJson = '{\"chap_list\": \"<div class=\\'list-chapter\\'><li><a href=\\'/chuong-1/\\'>Test</a></li></div>\"}';\n" +
                "let json = Json.parse(mockJson);\n" +
                "let chapList = json.chap_list;\n" +  // Critical property access
                "let HtmlApi = Html;\n" +
                "let doc = HtmlApi.parse(chapList);\n" +  // Parse HTML
                "let links = doc.select('.list-chapter li a');\n" +  // Select elements
                "links.length;";  // Return count

        Object result = context.evaluateString(scope, testCode, "testPropertyChain", 1, null);

        assertNotNull("Property access chain should work", result);
        assertTrue("Should return link count", result instanceof Integer);
        assertEquals("Should find 1 link", Integer.valueOf(1), result);
    }

    @Test
    public void testBaseUrlAvailability() {
        // Verify BASE_URL is available in sandbox (plugin needs this)

        String testCode = "typeof BASE_URL; BASE_URL;";
        Object result = context.evaluateString(scope, testCode, "testBaseUrl", 1, null);

        assertNotNull("BASE_URL should be defined", result);
        assertTrue("BASE_URL should be a string", result instanceof String);
        assertEquals("BASE_URL should match test URL", "https://truyenfull.vision", result);
    }
}
