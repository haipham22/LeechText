package dev.haipham22.leechtext.plugin.vbook

import dev.haipham22.leechtext.entities.PluginEntity
import dev.haipham22.leechtext.log.platformEngineLogger
import dev.haipham22.leechtext.plugin.js.loader.ListLoader
import dev.haipham22.leechtext.plugin.js.loader.LoaderType
import dev.haipham22.leechtext.plugin.js.sandbox.JsSandbox
import dev.haipham22.leechtext.plugin.js.sandbox.createSandbox
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * P5.2c: port E2EPluginJsonParsingTest sang common loader path trên macOS (QuickJS).
 * Fixture-based, không phụ thuộc site thật; assertion values giữ nguyên bản P5.1.
 * Entry giờ đi qua common JsSandbox contract + ListLoader (commonMain) — không còn
 * gọi QuickJsSandbox slice trực tiếp.
 */
class QuickJsE2ePluginJsonParsingTest {
    /** Eval qua sandbox contract chung (execute + callFunction) — return expr cuối. */
    private fun eval(script: String): Any? {
        val sandbox = testSandbox()
        try {
            // JS eval() trả completion value của biểu thức cuối (semantics P5.1)
            sandbox.execute("globalThis.__eval = function(src){ return eval(src); };", "test")
            return sandbox.callFunction("__eval", script)
        } finally {
            sandbox.close()
        }
    }

    private fun testSandbox(): JsSandbox = createSandbox(platformEngineLogger()) {
        loaderType = LoaderType.TEXT
        baseUrl = "https://truyenfull.vision"
        targetUrl = baseUrl
    }

    @Test
    fun testPluginJsonParsingWhenServerReturnsJson() {
        val mockJsonResponse =
            """{"chap_list": "<div class='list-chapter'><li><a href='/chapter-1/'>Chapter 1</a></li></div>", "status": 200}"""

        val result =
            eval(
                """
                let json = Json.parse('${mockJsonResponse.replace("\\", "\\\\").replace("'", "\\'")}');
                let links = Html.parse(json.chap_list).select('.list-chapter li a');
                JSON.stringify([json.chap_list, links.size(), links.length, links.get(0).text(), links.get(0).attr('href')]);
                """.trimIndent(),
            ) as String

        assertEquals(
            "[\"<div class='list-chapter'><li><a href='/chapter-1/'>Chapter 1</a></li></div>\",1,1,\"Chapter 1\",\"/chapter-1/\"]",
            result,
        )
    }

    @Test
    fun testHtmlParsingOfChapListContent() {
        val chapListHtml =
            "<div class='list-chapter'>" +
                "<li><span class='glyphicon glyphicon-certificate'></span> <a href='/chuong-1/'>Chương 1</a></li>" +
                "<li><span class='glyphicon glyphicon-certificate'></span> <a href='/chuong-2/'>Chương 2</a></li>" +
                "</div>"

        val result =
            eval(
                """
                let links = Html.parse('${chapListHtml.replace("'", "\\'")}').select('.list-chapter li a');
                JSON.stringify([
                  links.size(), links.length,
                  links.get(0).text(), links.get(0).attr('href'),
                  links.get(1).text(), links.get(1).attr('href')
                ]);
                """.trimIndent(),
            ) as String

        assertEquals("[2,2,\"Chương 1\",\"/chuong-1/\",\"Chương 2\",\"/chuong-2/\"]", result)
    }

    @Test
    fun testJSElementsMapReturnsJSList() {
        val html = "<div class='chapter'>Chapter 1</div><div class='chapter'>Chapter 2</div>"

        // map() phải trả list-like JSList compat; null callback → empty list
        val result =
            eval(
                """
                let elements = Html.parse('${html.replace("'", "\\'")}').select('.chapter');
                let mapped = elements.map(null);
                JSON.stringify([elements.size(), elements.length, mapped.length]);
                """.trimIndent(),
            ) as String

        assertEquals("[2,2,0]", result)
    }

    /** FULL common-loader path: ListLoader (commonMain) chạy toc script trên QuickJS. */
    @Test
    fun testListLoaderExtractsChaptersViaCommonLoader() {
        val plugin =
            PluginEntity(
                name = "fixture",
                tocGetter =
                """
                    function execute(url) {
                      let json = Json.parse('{"chap_list": "<div class=\'list-chapter\'><li><a href=\'/chuong-1/\'>Chương 1</a></li><li><a href=\'/chuong-2/\'>Chương 2</a></li></div>"}');
                      let links = Html.parse(json.chap_list).select('.list-chapter li a');
                      let chapters = [];
                      links.forEach(function(e) { chapters.push({name: e.text(), url: e.attr('href')}); });
                      return Response.success(chapters);
                    }
                """.trimIndent(),
            )

        val chapters = kotlinx.coroutines.runBlocking { ListLoader.with(plugin, platformEngineLogger()).load(null) }
        assertNotNull(chapters)
        assertEquals(2, chapters!!.size)
        assertEquals("Chương 1", chapters[0].name)
        assertEquals("/chuong-1/", chapters[0].url)
        assertEquals("Chương 2", chapters[1].name)
        assertEquals("/chuong-2/", chapters[1].url)
        assertTrue(chapters.all { it.id in 0..1 })
    }
}
