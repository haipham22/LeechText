package dev.haipham22.leechtext.plugin.js.api

import dev.haipham22.leechtext.log.platformEngineLogger
import dev.haipham22.leechtext.plugin.js.loader.JSResponse
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.NativeArray
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val INVALID_KEY = "has space"

/**
 * Unit test các JS API thuần (không network): Html parse/select/encode/clean,
 * LocalStorage (file storage thật, pluginId test + cleanup), JSResponse converter,
 * JSList + JSElement/JSElements wrapper.
 */
class JsApiCoreTest {
    private lateinit var context: Context
    private lateinit var scope: org.mozilla.javascript.Scriptable
    private lateinit var html: Html
    private lateinit var storage: LocalStorage

    // P5.2c: LocalStorage dùng EnginePaths.dataDir — cleanup phải cùng root (test khác
    // trong suite có thể init override)
    private val storageDir = File(dev.haipham22.leechtext.util.EnginePaths.dataDir.toFile(), "plugin_storage/test-jsapi")

    @Before
    fun setUp() {
        storageDir.deleteRecursively() // dọn lần chạy trước nếu crash
        context = Context.enter()
        context.optimizationLevel = -1
        scope = context.initStandardObjects()
        html = Html(platformEngineLogger())
        storage = LocalStorage(platformEngineLogger(), "test-jsapi")
    }

    @After
    fun tearDown() {
        Context.exit()
        storageDir.deleteRecursively()
    }

    // ---------- Html (pure, không fetch) ----------

    @Test
    fun htmlParseChuoiRongTraDocumentTrong() {
        assertEquals(0, html.parse(null).select("div").size())
        assertEquals(0, html.parse("").select("div").size())
        assertEquals("", html.parse(null).title())
    }

    @Test
    fun htmlParseVaSelectChuoiTinh() {
        val doc =
            html.parse(
                "<html><head><title>Tựa sách</title></head><body><div class='chapter'><a href='/c1'>Chương 1</a></div></body></html>",
            )
        assertEquals("Tựa sách", doc.title())
        val links = doc.select(".chapter a")
        assertEquals(1, links.size())
        assertEquals(1, links.length) // public field vBook compat
        assertEquals("Chương 1", links.get(0)!!.text())
        assertEquals("/c1", links.get(0)!!.attr("href"))

        // Helper parse + select một lượt
        assertEquals(1, html.select("<ul><li>x</li></ul>", "li").size())
        // Selector không match / rỗng
        assertEquals(0, doc.select("span").size())
        assertEquals(0, doc.select(null).size())
    }

    @Test
    fun htmlUrlEncodeDecode() {
        assertEquals("hello+world", html.urlEncode("hello world"))
        assertEquals("%C3%A9", html.urlEncode("é"))
        assertEquals("hello world", html.urlDecode("hello+world"))
        // Charset sai → trả nguyên input
        assertEquals("a b", html.urlEncode("a b", "bad-charset"))
        assertEquals("%", html.urlDecode("%")) // input sai format → trả nguyên
    }

    @Test
    fun htmlCleanBoTag() {
        assertEquals("<p>a</p>", Html.clean("<p>a</p><script>bad()</script>", arrayOf("script")))
        assertNull(Html.clean(null, null))
        assertEquals("<p>x</p>", Html.clean("<p>x</p>", null)) // tags null → chỉ lấy body
        assertNull(Html.wrapElement(null))
    }

    // ---------- LocalStorage (file thật, cleanup @After) ----------

    @Test
    fun storageSetGetRemoveRoundtrip() {
        storage.setItem("k1", "value1").setItem("k2", "giá trị tiếng Việt")
        assertEquals("value1", storage.getItem("k1"))
        assertEquals("giá trị tiếng Việt", storage.getItem("k2"))
        assertNull(storage.getItem("missing")) // key chưa có → null
        assertNull(storage.getItem(null)) // key rỗng → null

        assertTrue(storage.hasOwnProperty("k1"))
        assertFalse(storage.hasOwnProperty("missing"))

        storage.removeItem("k1")
        assertNull(storage.getItem("k1"))

        // set null value → remove
        storage.setItem("k2", null)
        assertNull(storage.getItem("k2"))
    }

    @Test
    fun storageKeyKhongHopLeBiTuChoi() {
        storage.setItem(INVALID_KEY, "x") // sai format [a-zA-Z0-9_-]
        storage.setItem("", "x")
        assertNull(storage.getItem(INVALID_KEY))
        assertFalse(storage.hasOwnProperty(INVALID_KEY))
    }

    @Test
    fun storageValueVuotQua10KBBiTuChoi() {
        storage.setItem("big", "x".repeat(10 * 1024 + 1))
        assertNull(storage.getItem("big"))
        // Giá trị đúng giới hạn thì được
        storage.setItem("ok", "x".repeat(10 * 1024))
        assertEquals(10 * 1024, storage.getItem("ok")!!.length)
    }

    @Test
    fun storageLengthGetKeyGetKeysVaClear() {
        storage.setItem("k1", "a").setItem("k2", "b")
        assertEquals(2, storage.getLength())
        assertEquals(setOf("k1", "k2"), storage.getKeys().toSet())
        // getKey trả tên không đuôi .txt; index sai → null
        assertTrue(storage.getKey(0) in setOf("k1", "k2"))
        assertNull(storage.getKey(-1))
        assertNull(storage.getKey(99))

        storage.clear()
        assertEquals(0, storage.getLength())
        assertEquals(0, storage.getKeys().size)
    }

    // ---------- JSResponse converter ----------

    private fun eval(script: String): Any? = context.evaluateString(scope, script, "test", 1, null)

    @Test
    fun jsResponseConvertObjectNested() {
        val native = RhinoValues.normalize(eval("({name: 'a', num: 42, arr: [1, 'x'], nested: {ok: true}, u: undefined})")) as Map<*, *>
        val map: Map<*, *> = JSResponse.convertObject(native)
        assertEquals("a", map["name"])
        assertEquals(42.0, (map["num"] as Number).toDouble())
        assertEquals(2, (map["arr"] as List<*>).size)
        val nested = map["nested"] as Map<*, *>
        assertEquals(true, nested["ok"])
        assertNull(map["u"]) // undefined → null

        assertEquals(0, JSResponse.convertObject(null).size) // null obj → map rỗng
    }

    @Test
    fun jsResponseConvertArrayVaValue() {
        val arr = eval("[1, 'x', {k: 1}]") as NativeArray
        val converted = JSResponse.convertArray(arr)
        assertEquals(3, converted.size)
        assertEquals("x", converted[1])
        assertTrue(converted[2] is Map<*, *>)
        assertEquals(0, JSResponse.convertArray(null).size)
        assertNull(JSResponse.convertValue(org.mozilla.javascript.Undefined.instance))
    }

    @Test
    fun jsResponseGetStringAnToan() {
        assertEquals("plain", JSResponse.getString("plain"))
        assertNull(JSResponse.getString(null))
        assertNull(JSResponse.getString(eval("({a: 1})"))) // object → null
        assertNull(JSResponse.getString(eval("(function(){})"))) // function → null
        // array → trả toString của Rhino object (không phải null như object/function)
        assertNotNull(JSResponse.getString(eval("[1, 2]")))
        assertEquals("same", JSResponse.safeToString("same")) // alias
    }

    @Test
    fun jsResponseGetPropertyTheoPath() {
        val native = RhinoValues.normalize(eval("({a: {b: {c: 'deep'}}})"))
        assertEquals("deep", JSResponse.getProperty(native, "a", "b", "c"))
        assertNull(JSResponse.getProperty(native, "a", "missing"))
        assertNull(JSResponse.getProperty(null, "a"))
        assertNull(JSResponse.getProperty(native)) // path rỗng
    }

    // ---------- JSList ----------

    @Test
    fun jsListAccessBien() {
        val list =
            JSList().apply {
                add("a")
                add("b")
            }
        assertEquals("a", list.get(0))
        assertNull(list.get(5)) // ngoài range → null (không throw)
        assertEquals("a", list.first())
        assertEquals("b", list.last())
        assertEquals("a,b", list.join(","))
        assertNull(JSList().first()) // rỗng
        assertNull(JSList().last())
    }

    @Test
    fun jsListMapFilterFindForEachVoiRhinoFunction() {
        eval(
            "var acc = []; function collect(x, i) { acc.push(x + i); } function double(x) { return x * 2; } " +
                "function gt1(x) { return x > 1; }",
        )
        val collect = scope.get("collect", scope) as Function
        val double = scope.get("double", scope) as Function
        val gt1 = scope.get("gt1", scope) as Function

        val list =
            JSList().apply {
                add(1)
                add(2)
                add(3)
            }

        list.forEach(collect)
        assertEquals(3, (eval("acc.length") as Number).toInt())
        assertEquals(1, (eval("acc[0]") as Number).toInt()) // (x + i): 1+0

        val mapped = list.map(double)
        assertEquals(3, mapped.size)
        assertEquals(4.0, (mapped.get(1) as Number).toDouble())

        assertEquals(listOf(2, 3), list.filter(gt1).toList())
        assertEquals(2, (list.find(gt1) as Number).toInt())
        // collect trả Number (không phải Boolean) → find bỏ qua, trả null
        assertNull(list.find(collect))
    }

    // ---------- JSElement / JSElements ----------

    @Test
    fun jsElementNullElementAnToan() {
        val el = JSElement(null)
        assertEquals("", el.text())
        assertEquals("", el.html())
        assertEquals("", el.outerHtml())
        assertEquals("", el.attr("href"))
        assertEquals("", el.tagName())
        assertEquals("", el.id())
        assertEquals("", el.className())
        assertFalse(el.hasClass("c"))
        assertTrue(el.isEmpty())
        assertEquals(0, el.childrenSize())
        assertNull(el.first())
        assertNull(el.parent())
        assertEquals(0, el.select("a").size())
    }

    @Test
    fun jsElementTraversalVaMutate() {
        val doc = html.parse("<div id='wrap'><p class='a'>1</p><p class='b'>2</p><span>3</span></div>")
        val wrap = doc.getElementById("wrap")!!

        assertEquals("wrap", wrap.id())
        assertEquals("div", wrap.tagName())
        assertEquals(3, wrap.childrenSize()) // p, p, span
        val firstP = wrap.first()!!
        assertEquals("1", firstP.text())
        assertEquals("3", wrap.last()!!.text()) // last element child là span
        assertEquals(firstP.parent()!!.id(), "wrap")
        assertEquals("span", firstP.next()?.next()?.tagName()) // p → p → span
        assertNull(firstP.prev())

        // attr set + get, class mutate
        firstP.attr("data-x", "1")
        assertEquals("1", firstP.attr("data-x"))
        firstP.addClass("extra")
        assertTrue(firstP.hasClass("extra"))
        firstP.removeClass("extra")
        assertFalse(firstP.hasClass("extra"))

        // remove(selector) — xóa span rồi element rỗng con
        wrap.remove("span")
        assertEquals(2, wrap.childrenSize())
    }

    @Test
    fun jsElementsCollectionOps() {
        val doc = html.parse("<ul><li class='keep'>1</li><li class='drop'>2</li></ul>")
        val els = doc.select("li")
        assertEquals(2, els.size())
        assertEquals("1 2", els.text())
        assertEquals("1", els.first()!!.text())
        assertEquals("2", els.last()!!.text())
        assertEquals("2", els.eq(1)!!.text())
        assertNull(els.get(99)) // ngoài range
        assertEquals(2, els.toArray().size)

        // filter bằng Rhino predicate
        eval("function isKeep(e) { return e.hasClass('keep'); }")
        val filtered = els.filter(scope.get("isKeep", scope))
        assertEquals(1, filtered.size())
        assertEquals("1", filtered.first()!!.text())

        // select con + each (index, element) jQuery style
        val ul = html.parse("<ul><li>1</li><li>2</li></ul>").select("ul")
        eval("var seen = []; function eachFn(i, e) { seen.push(i + ':' + e.text()); }")
        ul.select("li").each(scope.get("eachFn", scope))
        assertEquals("0:1,1:2", eval("seen.join(',')"))

        // addClass/removeClass/remove(selector) — remove tác dụng lên DOM (re-select để verify)
        els.addClass("mark")
        assertTrue(els.get(0)!!.hasClass("mark"))
        els.removeClass("mark")
        assertFalse(els.get(0)!!.hasClass("mark"))
        els.remove(".drop")
        assertEquals(1, doc.select("li").size())
    }

    @Test
    fun jsElementsTraversalChildrenParentsNextPrev() {
        val els = html.parse("<div id='p'><a id='a1'>1</a><a id='a2'>2</a></div>").select("a")

        assertEquals(0, els.children().size()) // <a> không có element con
        assertEquals(1, els.parents().size()) // chung 1 parent div
        assertEquals(1, els.next().size()) // chỉ a1 có next sibling
        assertEquals(1, els.prev().size()) // chỉ a2 có prev sibling

        // select với selector rỗng / invalid → collection rỗng, không throw
        assertEquals(0, els.select("").size())
        assertEquals(0, els.select(null).size())
    }

    @Test
    fun jsDocumentBien() {
        val doc = html.parse("<html><body><div id='only'>x</div></body></html>")
        assertEquals("x", doc.getElementById("only")!!.text())
        assertNull(doc.getElementById(null))
        assertNull(doc.getElementById("missing"))
        assertNotNull(doc.body())
        assertNotNull(doc.head())
        assertTrue(doc.html().contains("only"))
        assertTrue(doc.text().isNotEmpty())
    }
}
