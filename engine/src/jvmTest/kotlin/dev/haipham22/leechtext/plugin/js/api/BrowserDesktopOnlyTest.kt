package dev.haipham22.leechtext.plugin.js.api

import kotlin.test.Test
import kotlin.test.assertNotNull

/** Browser API — verify Engine.newBrowser() factory qua reflection. */
class BrowserDesktopOnlyTest {
    @Test
    fun newBrowserReturnsInstance() {
        val browser = Engine.newBrowser()
        assertNotNull(browser, "Engine.newBrowser() phải trả Browser instance")
    }
}
