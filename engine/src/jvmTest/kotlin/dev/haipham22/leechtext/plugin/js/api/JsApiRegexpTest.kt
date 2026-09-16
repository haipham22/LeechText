package dev.haipham22.leechtext.plugin.js.api

import dev.haipham22.leechtext.log.platformEngineLogger
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mozilla.javascript.Context
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val HELLO_WORLD = "Hello World"
private const val DATE_2024_05 = "2024-05"

/**
 * Unit test Regexp API (find/replace/split/groups...) — case chuẩn + biên
 * (null args, pattern invalid) theo cách plugin thật gọi.
 */
class JsApiRegexpTest {
    private lateinit var context: Context
    private lateinit var regexp: Regexp

    @Before
    fun setUp() {
        context = Context.enter()
        context.optimizationLevel = -1
        regexp = Regexp(platformEngineLogger())
    }

    @After
    fun tearDown() = Context.exit()

    @Test
    fun findTraVeMatchDauTien() {
        assertEquals("llo", regexp.find(HELLO_WORLD, "llo"))
        assertEquals("123", regexp.find("abc123def", "\\d+"))
        // Biên: không match / null args / pattern invalid → chuỗi rỗng
        assertEquals("", regexp.find("Hello", "xyz"))
        assertEquals("", regexp.find(null, "a"))
        assertEquals("", regexp.find("abc", null))
        assertEquals("", regexp.find("abc", "[invalid"))
    }

    @Test
    fun findAllVaFindLast() {
        assertEquals(listOf("l", "l"), regexp.findAll("Hello", "l").toList())
        assertEquals(listOf("1", "22"), regexp.findAll("a1b22", "\\d+").toList())
        assertEquals(emptyList<String>(), regexp.findAll(null, "a").toList())
        assertEquals(emptyList<String>(), regexp.findAll("abc", "[").toList())

        assertEquals("l", regexp.findLast("Hello", "l"))
        assertEquals("22", regexp.findLast("a1b22", "\\d+"))
        assertEquals("", regexp.findLast("abc", "xyz"))
    }

    @Test
    fun matchAllKemGroupsVaIndices() {
        val matches = regexp.matchAll("Chương 1, Chương 22", "Chương (\\d+)")
        assertEquals(2, matches.size)
        assertEquals("Chương 1", matches[0]["match"])
        assertEquals(0, matches[0]["index"])
        assertEquals(10, matches[1]["start"])
        assertEquals(19, matches[1]["end"])
        assertEquals(listOf("1"), (matches[0]["groups"] as Array<*>).toList())
        assertEquals(listOf("22"), (matches[1]["groups"] as Array<*>).toList())

        // Không groups → key "groups" vắng mặt
        assertEquals(1, regexp.matchAll("ab", "a").size)
        assertTrue(!regexp.matchAll("ab", "a")[0].containsKey("groups"))
        assertEquals(0, regexp.matchAll(null, "a").size)
    }

    @Test
    fun matchesWithPositionsTraTextStartEnd() {
        val positions = regexp.matchesWithPositions("a1b22", "\\d+")
        assertEquals(2, positions.size)
        assertEquals("1", positions[0]["text"])
        assertEquals(1, positions[0]["start"])
        assertEquals(2, positions[0]["end"])
        assertEquals("22", positions[1]["text"])
        assertEquals(0, regexp.matchesWithPositions(null, "a").size)
    }

    @Test
    fun replaceVaReplaceAll() {
        assertEquals("Hexlo World", regexp.replace(HELLO_WORLD, "l", "x"))
        assertEquals("Hexxo Worxd", regexp.replaceAll(HELLO_WORLD, "l", "x"))
        // Pattern invalid / không match → text nguyên vẹn
        assertEquals("abc", regexp.replace("abc", "[", "x"))
        assertEquals("abc", regexp.replace("abc", "xyz", "x"))
        // replacement null → thay bằng rỗng
        assertEquals("Heo Word", regexp.replaceAll(HELLO_WORLD, "l", null))
        assertEquals("", regexp.replace(null, "a", "x"))
    }

    @Test
    fun splitGiuTrailingEmpty() {
        assertEquals(listOf("a", "b", "", ""), regexp.split("a,b,,", ",").toList())
        assertEquals(listOf("abc"), regexp.split("abc", "[").toList()) // pattern invalid
        assertEquals(listOf("abc"), regexp.split("abc", null).toList())
        assertEquals(0, regexp.split(null, ",").size)
    }

    @Test
    fun testVaCountVaIsValid() {
        assertTrue(regexp.test("abc123", "\\d"))
        assertFalse(regexp.test("abc", "\\d"))
        assertFalse(regexp.test(null, "a"))
        assertFalse(regexp.test("abc", "["))

        assertEquals(3, regexp.count("a1b22c333", "\\d+"))
        assertEquals(0, regexp.count("abc", "\\d"))
        assertEquals(0, regexp.count(null, "\\d"))

        assertTrue(regexp.isValid("\\d+"))
        assertFalse(regexp.isValid("["))
        assertFalse(regexp.isValid(null))
        assertFalse(regexp.isValid(""))
    }

    @Test
    fun escapeKyTuDacBiet() {
        val escaped = regexp.escape("a.b")
        assertEquals("", regexp.escape(null))
        // Escape xong phải match literal, không còn là wildcard
        assertEquals("a.b", regexp.find("a.b", escaped))
        assertEquals("", regexp.find("axb", escaped))
    }

    @Test
    fun groupsVaNamedGroups() {
        // groups gồm cả group 0 (full match) ở đầu
        assertEquals(listOf(DATE_2024_05, "2024", "05"), regexp.groups(DATE_2024_05, "(\\d+)-(\\d+)").toList())
        // Group optional không match → null tại vị trí đó
        assertEquals(listOf("b", null), regexp.groups("ab", "(x)?b").toList())
        assertEquals(0, regexp.groups("abc", "\\d").size)
        assertEquals(0, regexp.groups(null, "a").size)

        val named: Map<String, String?> = regexp.namedGroups(DATE_2024_05, "(\\d+)-(\\d+)")
        assertEquals("2024", named["group1"])
        assertEquals("05", named["group2"])
        assertEquals(0, regexp.namedGroups(null, "a").size)
    }

    @Test
    fun matchAtLayMatchTheoIndex() {
        // "Hello" có 2 match "l" (index 0, 1)
        assertEquals("l", regexp.matchAt("Hello", "l", 1))
        // Index bằng/vượt số match, âm, pattern invalid → rỗng
        assertEquals("", regexp.matchAt("Hello", "l", 2))
        assertEquals("", regexp.matchAt("Hello", "l", 99))
        assertEquals("", regexp.matchAt("Hello", "l", -1))
        assertEquals("", regexp.matchAt("Hello", "[", 0))
    }
}
