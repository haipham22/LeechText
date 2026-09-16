package dev.haipham22.leechtext.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private const val SESSION_COOKIE = "session=abc"

/** Host parse mọi dạng URL — không slash cuối từng gây NPE Map.get(null) ở testSite. */
class CookiesUtilsTest {
    @Test
    fun parseHostNoTrailingSlash() {
        // URL này từng throw "Cannot invoke Object.hashCode() because key is null"
        CookiesUtils.put("https://truyenfull.live", SESSION_COOKIE)
        assertNotNull(CookiesUtils.getCookies("https://truyenfull.live"))
        assertEquals(SESSION_COOKIE, CookiesUtils.getCookies("https://truyenfull.live/thu-tien"))
        assertEquals(SESSION_COOKIE, CookiesUtils.getCookies("https://truyenfull.live?q=1"))
    }

    @Test
    fun differentHostIsolated() {
        CookiesUtils.put("https://truyenfull.live/", "a=1")
        CookiesUtils.put("https://bachngocsach.cc", "b=2")
        assertEquals("a=1", CookiesUtils.getCookies("https://truyenfull.live/abc"))
        assertEquals("b=2", CookiesUtils.getCookies("https://bachngocsach.cc/"))
    }
}
