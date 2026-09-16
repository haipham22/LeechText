package dev.haipham22.leechtext.plugin.js.api

import dev.haipham22.leechtext.log.platformEngineLogger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Unit test JsScriptEngine (eval JS + gọi hàm + tạo object/array) và
 * RhinoContextPool (borrow/return reuse/invalidate/exhaust).
 * Không đụng browser/Playwright.
 */
class JsScriptEngineTest {
    private val engine = JsScriptEngine.getInstance(platformEngineLogger())

    @Test
    fun getInstanceSingleton() {
        assertSame(engine, JsScriptEngine.getInstance(platformEngineLogger()))
    }

    @Test
    fun executeEvalChuoiJsDonGian() {
        assertEquals(3.0, (engine.execute("1 + 2") as Number).toDouble())
        assertEquals("hello", engine.execute("'hello'"))
        assertEquals(6.0, (engine.execute("var x = 3; x * 2") as Number).toDouble())
    }

    @Test
    fun executeScriptLoiThrowExecutionException() {
        assertFailsWith<JsScriptEngine.ExecutionException> { engine.execute("syntax ((( loi") }
    }

    @Test
    fun executeFunctionGoiHamTheoTen() {
        val result = engine.executeFunction("function add(a, b) { return a + b; }", "add", 1, 2)
        assertEquals(3.0, (result as Number).toDouble())
    }

    @Test
    fun executeFunctionThieuHamThrowException() {
        assertFailsWith<JsScriptEngine.ExecutionException> {
            engine.executeFunction("function other() {}", "missing")
        }
        // Tên tồn tại nhưng không phải function
        assertFailsWith<JsScriptEngine.ExecutionException> {
            engine.executeFunction("var notFn = 1;", "notFn")
        }
    }

    @Test
    fun createObjectVaCreateArray() {
        val obj = engine.createObject(mapOf("name" to "sách", "chapters" to 10))
        assertEquals("sách", obj["name", obj])
        assertEquals(10, (obj["chapters", obj] as Number).toInt())

        val arr = engine.createArray(arrayOf("a", "b", "c"))
        assertEquals(3, arr.length)
        assertEquals("b", arr[1, arr])
    }

    // ---------- RhinoContextPool ----------

    @Test
    fun poolBorrowReturnReuseCungContext() {
        val pool = RhinoContextPool(platformEngineLogger(), 2, 1_000, 50)
        val c1 = pool.borrowContext()
        assertEquals(1, pool.activeCount)
        assertEquals(1, pool.createdCount)

        pool.returnContext(c1)
        assertEquals(1, pool.availableCount) // về pool, không đóng
        assertEquals(1, pool.activeCount)

        // Borrow lại → nhận CHÍNH context cũ (reuse)
        val c2 = pool.borrowContext()
        assertSame(c1, c2)
        assertEquals(0, pool.availableCount)

        // Invalidate → đóng, counters giảm
        pool.invalidateContext(c2)
        assertEquals(0, pool.activeCount)
    }

    @Test
    fun poolBorrowTaoMoiSauKhiInvalidate() {
        val pool = RhinoContextPool(platformEngineLogger(), 2, 1_000, 50)
        val c1 = pool.borrowContext()
        pool.invalidateContext(c1)
        // invalidate giảm cả createdCount (context bị đóng hẳn)
        assertEquals(0, pool.createdCount)

        // Borrow mới sau invalidate → context khác, created tăng lại
        val c2 = pool.borrowContext()
        assertTrue(c1 !== c2)
        assertEquals(1, pool.createdCount)
        pool.returnContext(c2)
    }

    @Test
    fun poolExhaustedKhiHetContext() {
        val pool = RhinoContextPool(platformEngineLogger(), 1, 1_000, 50)
        val held = pool.borrowContext() // chiếm hết limit 1
        assertFailsWith<RhinoContextPool.ResourceExhaustedException> {
            pool.borrowContext() // chờ 5s → timeout
        }
        pool.invalidateContext(held)
    }

    @Test
    fun poolReturnContextNullVaInvalidateNullAnToan() {
        val pool = RhinoContextPool(platformEngineLogger(), 1, 1_000, 50)
        pool.returnContext(null) // không throw
        pool.invalidateContext(null) // không throw
        assertEquals(0, pool.activeCount)
    }
}
