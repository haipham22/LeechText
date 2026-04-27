package dark.leech.text.plugin.vbook;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.Test;

import dark.leech.text.plugin.js.api.Html;
import dark.leech.text.plugin.js.api.JSDocument;
import dark.leech.text.plugin.js.api.JSElement;
import dark.leech.text.plugin.js.api.JSElements;
import dark.leech.text.plugin.js.api.JSList;
import dark.leech.text.plugin.js.api.Json;

/**
 * End-to-end test for vBook plugin JSON parsing. Validates that response.json() works correctly
 * when server returns JSON with chap_list field.
 */
public class E2EPluginJsonParsingTest {

    private static final String TEST_URL =
            "https://truyenfull.vision/my-dung-su-xuyen-qua-lam-nong-phu-lam-giau-nuoi-con";

    @Test
    public void testPluginJsonParsingWhenServerReturnsJson() {
        // Simulate what the plugin expects: server returns JSON with chap_list
        String mockJsonResponse =
                "{\"chap_list\": \"<div class='list-chapter'><li><a href='/chapter-1/'>Chapter"
                        + " 1</a></li></div>\", \"status\": 200}";

        // Test 1: Verify Json.parse() creates NativeObject
        Json jsonApi = new Json();
        Object parsed = jsonApi.parse(mockJsonResponse);

        assertNotNull("Parsed JSON should not be null", parsed);
        assertTrue(
                "Parsed JSON should be NativeObject",
                parsed instanceof org.mozilla.javascript.NativeObject);

        org.mozilla.javascript.NativeObject jsonObj = (org.mozilla.javascript.NativeObject) parsed;
        assertTrue("Should have 'chap_list' property", jsonObj.has("chap_list", jsonObj));

        Object chapList = jsonObj.get("chap_list", jsonObj);
        assertEquals(
                "chap_list should contain HTML",
                "<div class='list-chapter'><li><a href='/chapter-1/'>Chapter 1</a></li></div>",
                chapList);

        // Test 2: Verify Html.parse() can process the chap_list content
        Html htmlApi = new Html();
        JSDocument doc = htmlApi.parse((String) chapList);

        assertNotNull("Document should not be null", doc);

        JSElements links = doc.select(".list-chapter li a");
        assertEquals("Should find 1 chapter link", 1, links.size());

        JSElement link = links.get(0);
        assertEquals("Chapter name should be 'Chapter 1'", "Chapter 1", link.text());
        assertEquals("Chapter URL should be '/chapter-1/'", "/chapter-1/", link.attr("href"));
    }

    @Test
    public void testPluginJsonParsingWithRealWorldScenario() {
        // Simulate exact plugin flow from truyenfull.plugin
        String mockServerResponse =
                "{\"chap_list\": \"<div class='list-chapter'><li><span class='glyphicon"
                    + " glyphicon-certificate'></span> <a"
                    + " href='/my-dung-su-xuyen-qua-lam-nong-phu-lam-giau-nuoi-con/chuong-1/'>Chương"
                    + " 1: Chương 1</a></li></div>\", \"status\": 200}";

        // Plugin code: let json = response.json();
        Json jsonApi = new Json();
        Object json = jsonApi.parse(mockServerResponse);

        assertNotNull("json should not be null", json);
        assertTrue(
                "json should be NativeObject", json instanceof org.mozilla.javascript.NativeObject);

        org.mozilla.javascript.NativeObject jsonObj = (org.mozilla.javascript.NativeObject) json;

        // Plugin code: let doc = Html.parse(json.chap_list);
        assertTrue("Should have chap_list property", jsonObj.has("chap_list", jsonObj));
        Object chapList = jsonObj.get("chap_list", jsonObj);

        Html htmlApi = new Html();
        JSDocument doc = htmlApi.parse((String) chapList);

        assertNotNull("doc should not be null", doc);

        // Plugin code: doc.select(".list-chapter li a")
        JSElements links = doc.select(".list-chapter li a");

        assertNotNull("links should not be null", links);
        assertEquals("Should find 1 chapter link", 1, links.size());
        assertEquals("links.length should match size()", 1, links.length);

        // Verify chapter data
        JSElement chapter = links.get(0);
        String chapterName = chapter.text();
        String chapterUrl = chapter.attr("href");

        assertTrue("Chapter name should contain 'Chương 1'", chapterName.contains("Chương 1"));
        assertEquals(
                "Chapter URL should be correct",
                "/my-dung-su-xuyen-qua-lam-nong-phu-lam-giau-nuoi-con/chuong-1/",
                chapterUrl);
    }

    @Test
    public void testResponseJsonReturnsNativeObject() {
        // This is the CRITICAL test - verifies response.json() returns NativeObject
        // not Map, so JavaScript property access works

        String jsonResponse =
                "{\"chap_list\": \"<div>test</div>\", \"data\": {\"status\": \"ok\"}}";

        Json jsonApi = new Json();
        Object result = jsonApi.parse(jsonResponse);

        assertNotNull("Result should not be null", result);
        assertTrue(
                "Result MUST be NativeObject for JavaScript property access to work",
                result instanceof org.mozilla.javascript.NativeObject);

        org.mozilla.javascript.NativeObject nativeObj =
                (org.mozilla.javascript.NativeObject) result;

        // Verify JavaScript property access works (this is what the plugin needs)
        assertTrue(
                "Must have 'chap_list' property via has()", nativeObj.has("chap_list", nativeObj));
        assertTrue("Must have 'data' property via has()", nativeObj.has("data", nativeObj));

        Object chapList = nativeObj.get("chap_list", nativeObj);
        assertNotNull("chap_list property should be accessible via get()", chapList);
        assertEquals("chap_list value should be correct", "<div>test</div>", chapList);

        Object data = nativeObj.get("data", nativeObj);
        assertTrue(
                "data should also be NativeObject",
                data instanceof org.mozilla.javascript.NativeObject);
    }

    @Test
    public void testHtmlParsingOfChapListContent() {
        // Test that Html.parse() correctly processes chap_list HTML content

        String chapListHtml =
                "<div class='list-chapter'>"
                        + "<li><span class='glyphicon glyphicon-certificate'></span> "
                        + "<a href='/chuong-1/'>Chương 1</a></li>"
                        + "<li><span class='glyphicon glyphicon-certificate'></span> "
                        + "<a href='/chuong-2/'>Chương 2</a></li>"
                        + "</div>";

        Html htmlApi = new Html();
        JSDocument doc = htmlApi.parse(chapListHtml);

        assertNotNull("Document should not be null", doc);

        JSElements links = doc.select(".list-chapter li a");

        assertNotNull("Links should not be null", links);
        assertEquals("Should find 2 chapter links", 2, links.size());
        assertEquals("links.length property should work", 2, links.length);

        // Verify each chapter
        JSElement chapter1 = links.get(0);
        assertEquals("Chapter 1 name should be 'Chương 1'", "Chương 1", chapter1.text());
        assertEquals("Chapter 1 URL should be '/chuong-1/'", "/chuong-1/", chapter1.attr("href"));

        JSElement chapter2 = links.get(1);
        assertEquals("Chapter 2 name should be 'Chương 2'", "Chương 2", chapter2.text());
        assertEquals("Chapter 2 URL should be '/chuong-2/'", "/chuong-2/", chapter2.attr("href"));
    }

    @Test
    public void testJSElementsMapReturnsJSList() {
        // Test that JSElements.map() returns JSList (not Object[])
        // This is critical for plugin compatibility

        String html = "<div class='chapter'>Chapter 1</div><div class='chapter'>Chapter 2</div>";
        Html htmlApi = new Html();
        JSDocument doc = htmlApi.parse(html);

        JSElements elements = doc.select(".chapter");

        assertNotNull("Elements should not be null", elements);
        assertEquals("Should find 2 elements", 2, elements.size());
        assertEquals("length property should match size()", 2, elements.length);

        // Test that map() returns JSList (vBook compatibility)
        Object mapped = elements.map(null); // null callback for type testing

        assertNotNull("Mapped result should not be null", mapped);
        assertTrue("map() MUST return JSList for vBook compatibility", mapped instanceof JSList);

        JSList jsList = (JSList) mapped;
        assertEquals(
                "JSList size should match element count",
                0,
                jsList.size()); // null callback returns empty list
    }

    @Test
    public void testFullPluginFlowSimulation() {
        // Simulate the complete plugin flow as it would execute in vBook

        // Step 1: Server returns JSON response
        String serverResponse =
                "{\"chap_list\": \"<div class='list-chapter'><li><a href='/chuong-1/'>Chương"
                        + " 1</a></li></div>\", \"status\": 200}";

        // Step 2: Plugin calls response.json()
        Json jsonApi = new Json();
        Object json = jsonApi.parse(serverResponse);

        assertNotNull("json should not be null", json);
        assertTrue(
                "json must be NativeObject", json instanceof org.mozilla.javascript.NativeObject);

        org.mozilla.javascript.NativeObject jsonObj = (org.mozilla.javascript.NativeObject) json;

        // Step 3: Plugin accesses json.chap_list (JavaScript property access)
        assertTrue("Must have 'chap_list' property", jsonObj.has("chap_list", jsonObj));
        Object chapList = jsonObj.get("chap_list", jsonObj);
        assertNotNull("chap_list must not be null", chapList);
        assertTrue("chap_list must be String", chapList instanceof String);

        // Step 4: Plugin calls Html.parse(json.chap_list)
        Html htmlApi = new Html();
        JSDocument doc = htmlApi.parse((String) chapList);

        assertNotNull("doc should not be null", doc);

        // Step 5: Plugin calls doc.select(".list-chapter li a")
        JSElements links = doc.select(".list-chapter li a");

        assertNotNull("links should not be null", links);
        assertEquals("Should find chapters", 1, links.size());
        assertEquals("links.length must work", 1, links.length);

        // Step 6: Plugin extracts chapter data
        JSElement chapter = links.get(0);
        String name = chapter.text();
        String url = chapter.attr("href");

        assertNotNull("Chapter name should not be null", name);
        assertNotNull("Chapter URL should not be null", url);
        assertTrue("Chapter name should contain 'Chương'", name.contains("Chương"));
        assertTrue("Chapter URL should start with /", url.startsWith("/"));
    }

    @Test
    public void testRealPluginFileCanBeParsed() throws IOException {
        // Test that we can read and parse the actual plugin file
        String pluginPath = "tools/plugins/36208149-6dae-73b0-3046-33c5438dc931.plugin";
        File pluginFile = new File(pluginPath);

        assertTrue("Plugin file should exist", pluginFile.exists());

        String content = new String(Files.readAllBytes(pluginFile.toPath()));
        assertNotNull("Plugin content should not be null", content);
        assertTrue("Plugin should contain toc function", content.contains("\"toc\":"));
        assertTrue("Plugin should contain response.json()", content.contains("response.json()"));
        assertTrue("Plugin should contain json.chap_list", content.contains("json.chap_list"));
        assertTrue(
                "Plugin should contain Html.parse", content.contains("Html.parse(json.chap_list)"));

        // Verify the plugin expects JSON format (note: plugin file uses escaped unicode)
        assertTrue(
                "Plugin expects JSON response",
                content.contains("json") && content.contains("response.json()"));
        assertTrue("Plugin expects chap_list field", content.contains("json.chap_list"));
    }

    @Test
    public void testNativeObjectPropertyAccessSimulation() {
        // Simulate how JavaScript would access properties on NativeObject
        // This is the core issue - if NativeObject doesn't work, json.chap_list fails

        String jsonString = "{\"chap_list\": \"<div>test</div>\", \"status\": 200}";

        Json jsonApi = new Json();
        Object result = jsonApi.parse(jsonString);

        assertTrue(
                "Result must be NativeObject",
                result instanceof org.mozilla.javascript.NativeObject);

        org.mozilla.javascript.NativeObject nativeObj =
                (org.mozilla.javascript.NativeObject) result;

        // Test has() method (JavaScript equivalent: "chap_list" in json)
        assertTrue("has() method must work for 'chap_list'", nativeObj.has("chap_list", nativeObj));
        assertTrue("has() method must work for 'status'", nativeObj.has("status", nativeObj));

        // Test get() method (JavaScript equivalent: json.chap_list)
        Object chapList = nativeObj.get("chap_list", nativeObj);
        assertNotNull("get() method must return value for 'chap_list'", chapList);
        assertEquals("get() method must return correct value", "<div>test</div>", chapList);

        Object status = nativeObj.get("status", nativeObj);
        assertNotNull("get() method must return value for 'status'", status);
        assertEquals("get() method must return correct value", 200, status);
    }
}
