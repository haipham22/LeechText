package dev.haipham22.leechtext.plugin.vbook

import dev.haipham22.leechtext.log.platformEngineLogger
import dev.haipham22.leechtext.plugin.js.api.Html
import dev.haipham22.leechtext.plugin.js.api.JSDocument
import dev.haipham22.leechtext.plugin.js.api.JSElement
import dev.haipham22.leechtext.plugin.js.api.JSElements
import dev.haipham22.leechtext.plugin.js.api.Json
import org.junit.Assume.assumeTrue
import org.mozilla.javascript.NativeObject
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private const val TOC_SELECTOR = ".list-chapter li a"

/**
 * E2E vBook plugin JSON parsing (port nguyên văn từ E2EPluginJsonParsingTest.java).
 * Fixture-based — không phụ thuộc site thật.
 */
class E2EPluginJsonParsingTest {
    @Test
    fun testPluginJsonParsingWhenServerReturnsJson() {
        val mockJsonResponse =
            """{"chap_list": "<div class='list-chapter'><li><a href='/chapter-1/'>Chapter 1</a></li></div>", "status": 200}"""

        // Test 1: Json.parse() tạo NativeObject
        val jsonApi = Json(platformEngineLogger())
        val parsed = jsonApi.parse(mockJsonResponse)

        assertNotNull(parsed)
        assertTrue(parsed is NativeObject, "Parsed JSON should be NativeObject")

        val jsonObj = parsed
        assertTrue(jsonObj.has("chap_list", jsonObj))

        val chapList = jsonObj["chap_list", jsonObj]
        assertEquals(
            "<div class='list-chapter'><li><a href='/chapter-1/'>Chapter 1</a></li></div>",
            chapList,
        )

        // Test 2: Html.parse() xử lý chap_list
        val htmlApi = Html(platformEngineLogger())
        val doc: JSDocument = htmlApi.parse(chapList as String)
        assertNotNull(doc)

        val links: JSElements = doc.select(TOC_SELECTOR)
        assertEquals(1, links.size())

        val link: JSElement = links.get(0)!!
        assertEquals("Chapter 1", link.text())
        assertEquals("/chapter-1/", link.attr("href"))
    }

    @Test
    fun testPluginJsonParsingWithRealWorldScenario() {
        val mockServerResponse =
            """{"chap_list": "<div class='list-chapter'><li><span class='glyphicon """ +
                """glyphicon-certificate'></span> """ +
                """<a href='/my-dung-su-xuyen-qua-lam-nong-phu-lam-giau-nuoi-con/chuong-1/'>""" +
                """Chương 1: Chương 1</a></li></div>", "status": 200}"""

        val jsonApi = Json(platformEngineLogger())
        val json = jsonApi.parse(mockServerResponse)

        assertNotNull(json)
        assertTrue(json is NativeObject)

        val jsonObj = json
        assertTrue(jsonObj.has("chap_list", jsonObj))
        val chapList = jsonObj["chap_list", jsonObj]

        val htmlApi = Html(platformEngineLogger())
        val doc = htmlApi.parse(chapList as String)
        assertNotNull(doc)

        val links = doc.select(TOC_SELECTOR)
        assertNotNull(links)
        assertEquals(1, links.size())
        assertEquals(1, links.length)

        val chapter = links.get(0)!!
        assertTrue(chapter.text().contains("Chương 1"))
        assertEquals(
            "/my-dung-su-xuyen-qua-lam-nong-phu-lam-giau-nuoi-con/chuong-1/",
            chapter.attr("href"),
        )
    }

    @Test
    fun testResponseJsonReturnsNativeObject() {
        // CRITICAL: response.json() phải trả NativeObject (không phải Map)
        val jsonResponse = """{"chap_list": "<div>test</div>", "data": {"status": "ok"}}"""

        val result = Json(platformEngineLogger()).parse(jsonResponse)

        assertNotNull(result)
        assertTrue(result is NativeObject, "Result MUST be NativeObject for JS property access")

        val nativeObj = result
        assertTrue(nativeObj.has("chap_list", nativeObj))
        assertTrue(nativeObj.has("data", nativeObj))

        assertEquals("<div>test</div>", nativeObj["chap_list", nativeObj])
        assertTrue(nativeObj["data", nativeObj] is NativeObject)
    }

    @Test
    fun testHtmlParsingOfChapListContent() {
        val chapListHtml =
            "<div class='list-chapter'>" +
                "<li><span class='glyphicon glyphicon-certificate'></span> <a href='/chuong-1/'>Chương 1</a></li>" +
                "<li><span class='glyphicon glyphicon-certificate'></span> <a href='/chuong-2/'>Chương 2</a></li>" +
                "</div>"

        val doc = Html(platformEngineLogger()).parse(chapListHtml)
        assertNotNull(doc)

        val links = doc.select(TOC_SELECTOR)
        assertEquals(2, links.size())
        assertEquals(2, links.length)

        assertEquals("Chương 1", links.get(0)!!.text())
        assertEquals("/chuong-1/", links.get(0)!!.attr("href"))
        assertEquals("Chương 2", links.get(1)!!.text())
        assertEquals("/chuong-2/", links.get(1)!!.attr("href"))
    }

    @Test
    fun testJSElementsMapReturnsJSList() {
        val html = "<div class='chapter'>Chapter 1</div><div class='chapter'>Chapter 2</div>"
        val doc = Html(platformEngineLogger()).parse(html)

        val elements = doc.select(".chapter")
        assertEquals(2, elements.size())
        assertEquals(2, elements.length)

        // map() phải trả JSList (vBook compat); null callback → empty list
        val mapped = elements.map(null)
        assertNotNull(mapped)
        // mapped đã kiểu JSList tĩnh — kiểm tra nội dung thay vì is-check vô nghĩa
        assertEquals(0, mapped.size)
    }

    @Test
    fun testFullPluginFlowSimulation() {
        val serverResponse =
            """{"chap_list": "<div class='list-chapter'><li><a href='/chuong-1/'>Chương 1</a></li></div>", "status": 200}"""

        val json = Json(platformEngineLogger()).parse(serverResponse)
        assertTrue(json is NativeObject)
        val jsonObj = json

        assertTrue(jsonObj.has("chap_list", jsonObj))
        val chapList = jsonObj["chap_list", jsonObj]
        assertTrue(chapList is String)

        val doc = Html(platformEngineLogger()).parse(chapList)
        val links = doc.select(TOC_SELECTOR)
        assertEquals(1, links.size())
        assertEquals(1, links.length)

        val chapter = links.get(0)!!
        assertTrue(chapter.text().contains("Chương"))
        assertTrue(chapter.attr("href").startsWith("/"))
    }

    @Test
    fun testRealPluginFileCanBeParsed() {
        // Plugin file là artifact local (app ghi ra data dir) — chỉ test khi tồn tại
        val pluginFile = File("tools/plugins/36208149-6dae-73b0-3046-33c5438dc931.plugin")
        assumeTrue("Plugin file not present locally — skipping", pluginFile.exists())

        val content = String(Files.readAllBytes(pluginFile.toPath()))
        assertTrue(content.contains("\"toc\":"))
        assertTrue(content.contains("response.json()"))
        assertTrue(content.contains("json.chap_list"))
        assertTrue(content.contains("Html.parse(json.chap_list)"))
    }

    @Test
    fun testNativeObjectPropertyAccessSimulation() {
        val jsonString = """{"chap_list": "<div>test</div>", "status": 200}"""
        val result = Json(platformEngineLogger()).parse(jsonString)
        assertTrue(result is NativeObject)

        val nativeObj = result
        assertTrue(nativeObj.has("chap_list", nativeObj))
        assertTrue(nativeObj.has("status", nativeObj))

        assertEquals("<div>test</div>", nativeObj["chap_list", nativeObj])
        // Rhino parse số nguyên thành Integer/Double tùy version — so Number
        assertEquals(200, (nativeObj["status", nativeObj] as Number).toInt())
    }
}
